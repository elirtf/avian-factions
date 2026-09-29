package club.avian.factions.economy;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;

import java.util.function.BiConsumer;

/**
 * {@code /cratesback <player> <crate>}: where a crate preview's "Menu" button goes. CrazyCrates has one
 * menu button for every preview and it always opened /crates, even from the Enchanter's book rolls
 * (owner, 2026-09-29). Its override runs this with the player and the crate's file name
 * ({@code crazycrates config.yml → gui.inventory.buttons.menu.override}): a book roll
 * ({@code Enchant*}) goes back to /enchanter, every other crate to /crates as before.
 */
final class CratesBackCommand {

    static final String PERMISSION = "avian.admin.economy";   // the console runs it

    /** Runs a command as the console (the real one dispatches to Bukkit). */
    private final BiConsumer<String, String> openFor;

    /** {@code openFor} is called with (player name, menu to open: "enchanter" or "crates"). */
    CratesBackCommand(BiConsumer<String, String> openFor) {
        this.openFor = openFor;
    }

    static String menuFor(String crate) {
        return crate.startsWith("Enchant") ? "enchanter" : "crates";
    }

    LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("cratesback")
                .requires(src -> src.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .executes(ctx -> {
                                    openFor.accept(StringArgumentType.getString(ctx, "player"),
                                            menuFor(StringArgumentType.getString(ctx, "crate")));
                                    return 1;
                                })))
                .build();
    }

    /** Opens the menu the way a player would: /enchanter is a DeluxeMenus menu, /crates is CrazyCrates'. */
    static void open(String playerName, String menu) {
        var player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return;
        }
        if (menu.equals("enchanter")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open enchanter " + playerName);
        } else {
            player.performCommand("crates");
        }
    }
}
