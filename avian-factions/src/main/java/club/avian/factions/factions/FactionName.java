package club.avian.factions.factions;

import java.util.Locale;

/**
 * Faction name validation (spec §7). Pure and configurable, so it is unit-testable and every
 * caller — command, admin tool, future API — rejects the same names for the same reasons.
 */
public final class FactionName {

    /** Why a name was rejected; the command maps these to messages. */
    public enum Problem {
        TOO_SHORT, TOO_LONG, ILLEGAL_CHARACTERS, RESERVED, BLOCKED, TAKEN
    }

    private FactionName() {
    }

    /** Lower-cased comparison key; what uniqueness and reserved checks use. */
    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * Validates everything decidable without the database.
     *
     * @return the problem, or null when the name is acceptable so far ({@link Problem#TAKEN}
     *     is decided by the caller against the Faction index)
     */
    public static Problem validate(String name, FactionsConfig.Names rules) {
        if (name.length() < rules.minLength()) {
            return Problem.TOO_SHORT;
        }
        if (name.length() > rules.maxLength()) {
            return Problem.TOO_LONG;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok) {
                return Problem.ILLEGAL_CHARACTERS;
            }
        }
        var key = key(name);
        for (var reserved : rules.reserved()) {
            if (key.equals(key(reserved))) {
                return Problem.RESERVED;
            }
        }
        for (var blocked : rules.blocked()) {
            if (!blocked.isEmpty() && key.contains(key(blocked))) {
                return Problem.BLOCKED;
            }
        }
        return null;
    }
}
