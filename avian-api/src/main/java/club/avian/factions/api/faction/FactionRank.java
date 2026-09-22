package club.avian.factions.api.faction;

/** Ranks within a Faction, ordered weakest to strongest (spec §7). */
public enum FactionRank {
    RECRUIT,
    MEMBER,
    OFFICER,
    CO_LEADER,
    LEADER;

    /** True when this rank is {@code other} or above — the form every permission check uses. */
    public boolean isAtLeast(FactionRank other) {
        return ordinal() >= other.ordinal();
    }
}
