package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.economy.TransactionResult;
import club.avian.factions.api.text.Brand;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * {@code /tokens} and {@code /gems} (#26). Bare, they show your own balance. The admin forms are how
 * crates, votes, events and menus pay out, since every reward plugin integrates by running a
 * console command:
 *
 * <pre>
 * /tokens|gems give|take|set &lt;player&gt; &lt;amount&gt; [reason]
 * /tokens|gems &lt;player&gt;
 * </pre>
 *
 * <p>Money has no command here: it is Vault's economy (see {@link VaultEconomyBridge}), so
 * EssentialsX's {@code /bal} and {@code /eco give|take|set} already read and write it.
 *
 * <p>Offline players work (a crate may fire while they are away), but only ones the server has
 * seen: names resolve from the profile cache, never a blocking Mojang lookup. The reason lands in the
 * audit trail, e.g. {@code crate:talon}, so "where did my tokens come from" has an answer.
 */
final class EconomyCommand {

    static final String PERMISSION = "avian.admin.economy";

    private final Economy economy;
    private final Executor mainThread;

    /** {@code mainThread} runs replies on the server thread; transactions complete on the DB thread. */
    EconomyCommand(Economy economy, Executor mainThread) {
        this.mainThread = mainThread;
        this.economy = economy;
    }

    /** One root command per currency players carry besides money: {@code /tokens}, {@code /gems}. */
    List<LiteralCommandNode<CommandSourceStack>> build() {
        return List.of(currencyNode(Currency.TOKENS).build(), currencyNode(Currency.GEMS).build());
    }

    private LiteralArgumentBuilder<CommandSourceStack> currencyNode(Currency currency) {
        var node = Commands.literal(currency.name().toLowerCase(Locale.ROOT))
                .executes(ctx -> own(ctx, currency));
        node.then(Commands.argument("player", StringArgumentType.word())
                .requires(src -> src.getSender().hasPermission(PERMISSION))
                .executes(ctx -> balance(ctx, currency)));
        for (var action : Action.values()) {
            node.then(Commands.literal(action.name().toLowerCase(Locale.ROOT))
                    .requires(src -> src.getSender().hasPermission(PERMISSION))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .then(Commands.argument("amount", LongArgumentType.longArg(action == Action.SET ? 0 : 1))
                                    .executes(ctx -> run(ctx, currency, action, "admin:" + ctx.getSource().getSender().getName()))
                                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                                            .executes(ctx -> run(ctx, currency, action, StringArgumentType.getString(ctx, "reason")))))));
        }
        return node;
    }

    enum Action { GIVE, TAKE, SET }

    private int run(CommandContext<CommandSourceStack> ctx, Currency currency, Action action, String reason) {
        var sender = ctx.getSource().getSender();
        var player = resolve(sender, StringArgumentType.getString(ctx, "player"));
        if (player == null) {
            return 0;
        }
        long amount = LongArgumentType.getLong(ctx, "amount");
        var uuid = player.getUniqueId();
        CompletableFuture<TransactionResult> result = switch (action) {
            case GIVE -> economy.deposit(uuid, currency, amount, reason);
            case TAKE -> economy.withdraw(uuid, currency, amount, reason);
            case SET -> {
                long current = economy.balance(uuid, currency);
                if (amount == current) {
                    yield CompletableFuture.completedFuture(new TransactionResult(TransactionResult.Status.OK, null, current));
                }
                yield amount > current
                        ? economy.deposit(uuid, currency, amount - current, reason)
                        : economy.withdraw(uuid, currency, current - amount, reason);
            }
        };
        result.whenCompleteAsync((r, error) -> {
            if (error != null) {
                sender.sendMessage(Component.text("Failed: " + error.getMessage(), NamedTextColor.RED));
            } else if (!r.ok()) {
                sender.sendMessage(Component.text("Refused (" + r.status() + "). " + player.getName() + " has "
                        + economy.format(currency, r.balanceAfter()) + ".", NamedTextColor.RED));
            } else {
                sender.sendMessage(Component.text(player.getName() + " now has "
                        + economy.format(currency, r.balanceAfter()) + ".", NamedTextColor.GOLD));
            }
        }, mainThread);
        return 1;
    }

    private int own(CommandContext<CommandSourceStack> ctx, Currency currency) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text(
                    "Players only; use /" + currency.name().toLowerCase(Locale.ROOT) + " <player>.", NamedTextColor.RED));
            return 0;
        }
        player.sendMessage(Brand.mm("<soft>You have " + tagged(currency, economy.balance(player.getUniqueId(), currency)) + ".</soft>"));
        return 1;
    }

    private int balance(CommandContext<CommandSourceStack> ctx, Currency currency) {
        var sender = ctx.getSource().getSender();
        var player = resolve(sender, StringArgumentType.getString(ctx, "player"));
        if (player == null) {
            return 0;
        }
        sender.sendMessage(Brand.mm("<soft>" + player.getName() + " has "
                + tagged(currency, economy.balance(player.getUniqueId(), currency)) + ".</soft>"));
        return 1;
    }

    /** The amount in its currency's colour: {@code <token>1,200 tokens</token>}. */
    private String tagged(Currency currency, long amount) {
        String tag = currency == Currency.GEMS ? "gem" : "token";
        return "<" + tag + ">" + economy.format(currency, amount) + "</" + tag + ">";
    }

    /** An online or previously seen player; never a blocking lookup. Tells the sender if unknown. */
    private static OfflinePlayer resolve(CommandSender sender, String name) {
        OfflinePlayer player = Bukkit.getPlayerExact(name);
        if (player == null) {
            player = Bukkit.getOfflinePlayerIfCached(name);
        }
        if (player == null) {
            sender.sendMessage(Component.text("No player named " + name + " has joined this server.", NamedTextColor.RED));
        }
        return player;
    }
}
