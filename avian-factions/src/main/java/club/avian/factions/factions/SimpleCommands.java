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
        if (parts.length < 2 || !ROOTS.contains(parts[0].toLowerCase(Locale.ROOT))
                || Arrays.stream(parts).anyMatch(p -> p.startsWith("--"))) {
            return Optional.empty();
        }
        String sub = parts[1].toLowerCase(Locale.ROOT);
        List<String> args = Arrays.asList(parts).subList(2, parts.length);
        List<String> out = switch (sub) {
            case "claim", "unclaim" -> claim(sub, args);
            case "autoclaim" -> args.isEmpty() ? List.of("claim", "--auto") : null;
            case "unclaimall" -> args.isEmpty() ? List.of("unclaim", "--all-territory") : null;
            case "map" -> map(args);
            case "fly" -> args.size() == 1 && args.getFirst().equalsIgnoreCase("auto")
                    ? List.of("fly", "--auto") : null;
            case "warp", "setwarp" -> args.size() == 2
                    ? List.of(sub, args.get(0), "--password", args.get(1)) : null;
            case "delwarp" -> args.size() == 1 ? List.of("setwarp", args.getFirst(), "--delete") : null;
            case "deinvite" -> args.size() == 1 ? List.of("invite", args.getFirst(), "--delete") : null;
            case "delhome" -> args.isEmpty() ? List.of("sethome", "--delete") : null;
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
