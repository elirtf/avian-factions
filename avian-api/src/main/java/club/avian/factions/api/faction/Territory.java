package club.avian.factions.api.faction;

import java.util.Optional;

/**
 * What occupies one chunk. Never null: unclaimed land is {@link #wilderness()}, so callers branch
 * on the kind rather than null-checking (#4 prior art — the null checks are where protection bugs
 * hide).
 */
public interface Territory {

    enum Kind {
        /** Unclaimed. Anyone may build. */
        WILDERNESS,
        /** Claimed by a Faction. */
        FACTION,
        /** Admin-protected (Spawn). Held as a WorldGuard region; claims are disallowed. */
        SAFEZONE,
        /** Public PvP area. Claims are disallowed, protection is off. */
        WARZONE
    }

    Kind kind();

    /** The owning Faction, present only when {@link #kind()} is {@link Kind#FACTION}. */
    Optional<Faction> faction();

    default boolean isWilderness() {
        return kind() == Kind.WILDERNESS;
    }

    /** True when a Faction may claim here — false for safezone and warzone. */
    default boolean claimable() {
        return kind() == Kind.WILDERNESS || kind() == Kind.FACTION;
    }

    /** The shared wilderness instance; there is exactly one. */
    static Territory wilderness() {
        return Wilderness.INSTANCE;
    }

    /** Package-private singleton so every wilderness lookup returns the same object. */
    final class Wilderness implements Territory {
        private static final Wilderness INSTANCE = new Wilderness();

        private Wilderness() {
        }

        @Override
        public Kind kind() {
            return Kind.WILDERNESS;
        }

        @Override
        public Optional<Faction> faction() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "Wilderness";
        }
    }
}
