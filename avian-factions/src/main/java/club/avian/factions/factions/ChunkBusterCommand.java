package club.avian.factions.factions;

import club.avian.factions.api.text.Brand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;

/** {@code /chunkbuster confirm} (the chat button) and {@code /chunkbuster give <player> [amount]}. */
final class ChunkBusterCommand {

    static final String ADMIN_PERMISSION = "avian.admin.chunkbuster";

    private final ChunkBusters busters;

    ChunkBusterCommand(ChunkBusters busters) {
        this.busters = busters;
    }

    LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("chunkbuster")
                .then(Commands.literal("confirm")
                        .executes(ctx -> {
                            if (ctx.getSource().getExecutor() instanceof Player player) {
                                busters.confirm(player);
                            }
                            return 1;
                        }))
                .then(Commands.literal("give")
                        .requires(src -> src.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> give(ctx, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))))
                .build();
    }

    private int give(CommandContext<CommandSourceStack> ctx, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
        target.getInventory().addItem(ChunkBusters.create(amount)).values()
                .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        ctx.getSource().getSender().sendMessage(Brand.mm("<cane>Gave " + target.getName() + " " + amount + " chunk buster(s).</cane>"));
        return 1;
    }
}
