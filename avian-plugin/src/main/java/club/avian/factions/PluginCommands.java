package club.avian.factions;

import club.avian.factions.api.command.Commands;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the command trees modules register during enable and hands them to Paper when it fires
 * the {@code COMMANDS} lifecycle event, which happens after {@code onEnable} returns.
 */
final class PluginCommands implements Commands {

    private record Registration(LiteralCommandNode<CommandSourceStack> node, String description, List<String> aliases) {
    }

    private final List<Registration> pending = new ArrayList<>();

    PluginCommands(JavaPlugin plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var registrar = event.registrar();
            for (var registration : pending) {
                registrar.register(registration.node(), registration.description(), registration.aliases());
            }
        });
    }

    @Override
    public void register(LiteralCommandNode<CommandSourceStack> node, String description, List<String> aliases) {
        pending.add(new Registration(node, description, List.copyOf(aliases)));
    }
}
