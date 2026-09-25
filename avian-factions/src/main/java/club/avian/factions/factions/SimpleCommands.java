package club.avian.factions.factions;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Lets players type the plain faction commands every factions server uses ({@code /f claim 5},
 * {@code /f unclaim all}, {@code /f map on}) instead of the {@code --flag} syntax FactionsUUID 4.7
 * requires. Without this, Paper answers the plain form with a bare "Incorrect argument" and no hint.
 * Anything already using flags, or not listed here, passes through untouched.
 */
public final class SimpleCommands implements Listener {

    private static final Set<String> ROOTS = Set.of("f", "faction", "factions", "factionsuuid:f",
            "factionsuuid:faction", "factionsuuid:factions");
    private static final Set<String> MENU = Set.of("menu", "gui");
    static final String MENU_COMMAND = "fmenu";

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        rewrite(event.getMessage()).ifPresent(event::setMessage);
    }

    /** The flag form of a plain {@code /f} command, or empty when it needs no change. */
    static Optional<String> rewrite(String message) {
        if (!message.startsWith("/")) {
            return Optional.empty();
        }
        String[] parts = message.substring(1).trim().split("\\s+");
        if (!ROOTS.contains(parts[0].toLowerCase(Locale.ROOT))
                || Arrays.stream(parts).anyMatch(p -> p.startsWith("--"))) {
            return Optional.empty();
        }
        // Bare /f, /f menu and /f gui open the faction menu (DeluxeMenus, /fmenu). /f help still lists commands.
        if (parts.length == 1 || (parts.length == 2 && MENU.contains(parts[1].toLowerCase(Locale.ROOT)))) {
            return Optional.of("/" + MENU_COMMAND);
        }
        String sub = parts[1].toLowerCase(Locale.ROOT);
        // F-Top is ours (/ftop: a menu, by spawner and block value); FactionsUUID's /f top ranks by money.
        if (sub.equals("top") && (parts.length == 2 || (parts.length == 3 && parts[2].chars().allMatch(Character::isDigit)))) {
            return Optional.of("/ftop" + (parts.length == 3 ? " " + parts[2] : ""));
        }
        List<String> args = Arrays.asList(parts).subList(2, parts.length);
        List<String> out = switch (sub) {
            case "claim", "unclaim" -> claim(sub, args);
            case "autoclaim" -> args.isEmpty() ? List.of("claim", "--auto") : null;
            case "unclaimall" -> args.isEmpty() ? List.of("unclaim", "--all-territory") : null;
            case "map" -> map(args);
            case "fly" -> args.size() == 1 && args.getFirst().equalsIgnoreCase("auto")
                    ? List.of("fly", "--auto") : null;
            case "warp" -> args.size() == 2 ? List.of("warp", args.get(0), "--password", args.get(1)) : null;
            // Setting a home or warp lives under /f set in FactionsUUID 4.7: /f set home, /f set warp.
            case "sethome" -> args.isEmpty() ? List.of("set", "home") : null;
            case "delhome" -> args.isEmpty() ? List.of("set", "home", "--delete") : null;
            case "setwarp" -> switch (args.size()) {
                case 1 -> List.of("set", "warp", args.getFirst());
                case 2 -> List.of("set", "warp", args.get(0), "--password", args.get(1));
                default -> null;
            };
            case "delwarp" -> args.size() == 1 ? List.of("set", "warp", args.getFirst(), "--delete") : null;
            case "deinvite" -> args.size() == 1 ? List.of("invite", args.getFirst(), "--delete") : null;
            case "desc" -> args.isEmpty() ? null : concat(List.of("set", "description"), args);
            case "tag", "rename" -> args.size() == 1 ? List.of("set", "tag", args.getFirst()) : null;
            // Older plugins: /f ally <faction>. FactionsUUID: /f relation <faction> ally.
            case "ally", "enemy", "neutral", "truce" -> args.size() == 1 ? List.of("relation", args.getFirst(), sub) : null;
            case "who", "f", "info" -> args.size() <= 1 ? concat(List.of("show"), args) : null;
            default -> null;
        };
        if (out == null) {
            return Optional.empty();
        }
        var joined = new ArrayList<String>();
        joined.add("/" + parts[0]);
        joined.addAll(out);
        return Optional.of(String.join(" ", joined));
    }

    private static List<String> concat(List<String> head, List<String> tail) {
        var all = new ArrayList<>(head);
        all.addAll(tail);
        return all;
    }

    /** {@code claim 5}, {@code claim auto}, {@code claim fill}; unclaim also takes {@code all}. */
    private static List<String> claim(String sub, List<String> args) {
        if (args.size() != 1) {
            return null;
        }
        String arg = args.getFirst().toLowerCase(Locale.ROOT);
        if (arg.chars().allMatch(Character::isDigit)) {
            return List.of(sub, "--radius", arg);
        }
        return switch (arg) {
            case "auto" -> List.of(sub, "--auto");
            case "fill" -> List.of(sub, "--fill");
            case "all" -> sub.equals("unclaim") ? List.of(sub, "--all-territory") : null;
            default -> null;
        };
    }

    /** {@code map on}, {@code map off}, {@code map height 10}. */
    private static List<String> map(List<String> args) {
        if (args.isEmpty()) {
            return null;
        }
        String arg = args.getFirst().toLowerCase(Locale.ROOT);
        if (args.size() == 1) {
            return switch (arg) {
                case "on", "auto", "auto-on" -> List.of("map", "--auto-on");
                case "off", "auto-off" -> List.of("map", "--auto-off");
                default -> null;
            };
        }
        if (args.size() == 2 && (arg.equals("height") || arg.equals("set-height"))) {
            return List.of("map", "--set-height", args.get(1));
        }
        return null;
    }
}
