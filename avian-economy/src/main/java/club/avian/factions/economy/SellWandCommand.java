package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * {@code /sellwand give <player> <tier> [amount]}: how admins, crates and shops hand out sell wands.
 * Tiers come from {@code sellwand.conf}.
 */
final class SellWandCommand {

    static final String ADMIN_PERMISSION = "avian.sellwand.admin";

    private final ConfigHandle<SellWandConfig> config;

    SellWandCommand(ConfigHandle<SellWandConfig> config) {
        this.config = config;
    }

    LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("sellwand")
                .requires(src -> src.getSender().hasPermission(ADMIN_PERMISSION))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("tier", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            config.get().tiers().keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> give(ctx, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))))
                .build();
    }

    private int give(CommandContext<CommandSourceStack> ctx, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
        var tierId = StringArgumentType.getString(ctx, "tier").toLowerCase(java.util.Locale.ROOT);
        var tier = config.get().tier(tierId);
        var sender = ctx.getSource().getSender();
        if (tier == null) {
            sender.sendMessage(Component.text("No sell wand tier '" + tierId + "'. Tiers: "
                    + String.join(", ", config.get().tiers().keySet()), NamedTextColor.RED));
            return 0;
        }
        for (int i = 0; i < amount; i++) {
            HoeMenu.give(target, SellWand.create(tierId, tier));   // inventory, or at their feet if full
        }
        sender.sendMessage(Component.text("Gave " + target.getName() + " " + amount + " " + tierId + " sell wand"
                + (amount == 1 ? "." : "s."), NamedTextColor.GOLD));
        return 1;
    }
}
