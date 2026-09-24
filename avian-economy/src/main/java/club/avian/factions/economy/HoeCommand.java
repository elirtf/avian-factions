package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/** {@code /hoe} opens the Harvester Hoe menu; {@code /hoe give <player>} is for admins. */
final class HoeCommand {

    static final String ADMIN_PERMISSION = "avian.hoe.admin";

    private final HoeMenu menu;
    private final ConfigHandle<HoeConfig> config;

    HoeCommand(HoeMenu menu, ConfigHandle<HoeConfig> config) {
        this.menu = menu;
        this.config = config;
    }

    LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("hoe")
                .executes(ctx -> {
                    if (ctx.getSource().getExecutor() instanceof Player player) {
                        menu.open(player);
                        return 1;
                    }
                    ctx.getSource().getSender().sendMessage(Component.text("Players only; use /hoe give <player>.", NamedTextColor.RED));
                    return 0;
                })
                .then(Commands.literal("give")
                        .requires(src -> src.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> {
                                    var target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    HoeMenu.give(target, HarvesterHoe.create(config.get()));
                                    ctx.getSource().getSender().sendMessage(Component.text(
                                            "Gave " + target.getName() + " a Harvester Hoe.", NamedTextColor.GOLD));
                                    return 1;
                                })))
                .build();
    }
}
