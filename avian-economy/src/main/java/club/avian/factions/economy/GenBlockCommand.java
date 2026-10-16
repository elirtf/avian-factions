package club.avian.factions.economy;

import club.avian.factions.api.text.Brand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;

/** {@code /genblock give <player> <type> [amount]}: gen blocks for crates, events and tests (admins). */
final class GenBlockCommand {

    static final String PERMISSION = "avian.admin.genblock";

    private final GenBlocks gens;
    private final club.avian.factions.api.config.ConfigHandle<GenBlocksConfig> config;

    GenBlockCommand(GenBlocks gens, club.avian.factions.api.config.ConfigHandle<GenBlocksConfig> config) {
        this.gens = gens;
        this.config = config;
    }

    LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("genblock")
                .requires(src -> src.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, b) -> {
                                            config.get().types().keySet().forEach(b::suggest);
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> give(ctx, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))))
                .build();
    }

    private int give(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        var target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
        var type = StringArgumentType.getString(ctx, "type");
        var item = gens.create(type, amount);
        if (item == null) {
            ctx.getSource().getSender().sendMessage(Brand.mm("<bad>No gen block called " + type + " (gens.conf).</bad>"));
            return 0;
        }
        target.getInventory().addItem(item).values().forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        ctx.getSource().getSender().sendMessage(Brand.mm("<cane>Gave " + target.getName() + " " + amount + " " + type + ".</cane>"));
        return 1;
    }
}
