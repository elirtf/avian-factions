package club.avian.factions.api.faction;

/**
 * The outcome of a protection check, carrying <em>why</em> so listeners can show the right message
 * and rate-limit it per reason (#4: PHYSICAL interact and hopper events fire every tick).
 */
public enum ProtectionDecision {

    /** No claim rules apply here — disabled world, wilderness, or the warzone. */
    ALLOW_UNPROTECTED,
    /** The actor is a member of the owning Faction. */
    ALLOW_MEMBER,
    /** Staff bypass. */
    ALLOW_BYPASS,
    /** The owning Faction holds more claims than its Power supports (CONTEXT.md "Raidable"). */
    ALLOW_RAIDABLE,
    /** Claimed by another Faction and none of the above applies. */
    DENY_FOREIGN_TERRITORY,
    /** Admin-protected land: nobody builds in Spawn. */
    DENY_SAFEZONE;

    public boolean allowed() {
        return ordinal() <= ALLOW_RAIDABLE.ordinal();
    }

    public boolean denied() {
        return !allowed();
    }
}
