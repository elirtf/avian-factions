package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.economy.TransactionResult;
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

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Admin and console control of the three currencies (#26). This is how crates, votes, events and
 * menus pay out, since every reward plugin integrates by running a console command:
 *
 * <pre>
 * /avian tokens|gems|money give|take|set &lt;player&gt; &lt;amount&gt; [reason]
 * /avian balance &lt;player&gt;
 * </pre>
 *
 * <p>Offline players work (a crate may fire while they are away), but only ones the server has
 * seen: names resolve from the profile cache, never a blocking Mojang lookup. The reason lands in the
 * audit trail, e.g. {@code crate:talon}, so "where did my tokens come from" has an answer.
 */
final class EconomyCommand {

    static final String PERMISSION = "avian.admin.economy";

    private final Economy economy;

    EconomyCommand(Economy economy) {
        this.economy = economy;
    }

    LiteralCommandNode<CommandSourceStack> build() {
        var root = Commands.literal("avian")
                .requires(src -> src.getSender().hasPermission(PERMISSION));
        for (var currency : Currency.values()) {
            root.then(currencyNode(currency));
        }
        root.then(Commands.literal("balance")
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(this::balance)));
        return root.build();
    }

    private LiteralArgumentBuilder<CommandSourceStack> currencyNode(Currency currency) {
        var node = Commands.literal(currency.name().toLowerCase(Locale.ROOT));
        for (var action : Action.values()) {
            node.then(Commands.literal(action.name().toLowerCase(Locale.ROOT))
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
        result.whenComplete((r, error) -> {
            if (error != null) {
                sender.sendMessage(Component.text("Failed: " + error.getMessage(), NamedTextColor.RED));
            } else if (!r.ok()) {
                sender.sendMessage(Component.text("Refused (" + r.status() + "). " + player.getName() + " has "
                        + economy.format(currency, r.balanceAfter()) + ".", NamedTextColor.RED));
            } else {
                sender.sendMessage(Component.text(player.getName() + " now has "
                        + economy.format(currency, r.balanceAfter()) + ".", NamedTextColor.GOLD));
            }
        });
        return 1;
    }

    private int balance(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = resolve(sender, StringArgumentType.getString(ctx, "player"));
        if (player == null) {
            return 0;
        }
        var uuid = player.getUniqueId();
        sender.sendMessage(Component.text(player.getName() + ": "
                + economy.format(Currency.MONEY, economy.balance(uuid, Currency.MONEY)) + ", "
                + economy.format(Currency.TOKENS, economy.balance(uuid, Currency.TOKENS)) + ", "
                + economy.format(Currency.GEMS, economy.balance(uuid, Currency.GEMS)), NamedTextColor.GOLD));
        return 1;
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
