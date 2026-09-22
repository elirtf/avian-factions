package club.avian.factions.api.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.List;

/** Command registration for modules; wraps Paper's Brigadier lifecycle event (ADR-0004). */
public interface Commands {

    /** Registers a command tree with optional aliases. Call during {@code enable}. */
    void register(LiteralCommandNode<CommandSourceStack> node, String description, List<String> aliases);

    default void register(LiteralCommandNode<CommandSourceStack> node, String description) {
        register(node, description, List.of());
    }
}
