package club.avian.factions.cosmetics;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.CoreModule;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * Cosmetics we build ourselves. Tags (DeluxeTags) and pets (SimplePets) are plugins; trails are ours
 * because the usual trails plugin, PlayerParticles, is licensed non-commercial (owner, 2026-09-29).
 */
public final class CosmeticsModule implements AvianModule {

    @Override
    public String id() {
        return "cosmetics";
    }

    @Override
    public Set<Class<? extends AvianModule>> dependsOn() {
        return Set.of(CoreModule.class);
    }

    @Override
    public List<ConfigSpec<?>> configs() {
        return List.of(TrailsConfig.SPEC);
    }

    @Override
    public void enable(ModuleContext ctx) {
        var config = ctx.config(TrailsConfig.SPEC);
        var trails = new Trails(config);
        ctx.registerListener(trails);
        var menu = new TrailsMenu(config, trails);
        ctx.registerListener(menu);
        long interval = config.get().intervalTicks();
        Bukkit.getScheduler().runTaskTimer(ctx.plugin(), () -> trails.tick(Bukkit.getOnlinePlayers()), interval, interval);
        ctx.commands().register(Commands.literal("trails")
                .executes(c -> {
                    if (c.getSource().getSender() instanceof Player player) {
                        menu.open(player);
                    }
                    return 1;
                })
                .build(), "Pick a particle trail you've won");
        ctx.logger().info(config.get().trails().size() + " trail(s) loaded");
    }
}
