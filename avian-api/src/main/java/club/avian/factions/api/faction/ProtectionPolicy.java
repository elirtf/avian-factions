package club.avian.factions.api.faction;

import java.util.UUID;

/**
 * The single decision function every protection listener consults (#4 prior art).
 *
 * <p>Listeners translate events into these calls and do nothing else. Keeping the branching in one
 * pure place is what makes the rules testable and stops the "enemy can break blocks in a claim"
 * class of regression that every surveyed plugin shipped at least once.
 *
 * <p>Evaluation order is fixed: world enabled → bypass → territory kind → raid state → membership.
 */
public interface ProtectionPolicy {

    /** May {@code player} perform {@code interaction} in the chunk at this territory? */
    ProtectionDecision check(UUID player, Territory territory, Interaction interaction);

    /** Convenience for the commonest case. */
    default ProtectionDecision canBuild(UUID player, Territory territory) {
        return check(player, territory, Interaction.BUILD);
    }

    /**
     * Should an explosion be stopped from damaging blocks in this territory?
     *
     * <p>Evaluated once per distinct chunk, never per block (#4: the plugins that decide per block
     * allocate a key and do a lookup for every block of every TNT stack).
     */
    boolean denyExplosion(Territory territory);

    /** May {@code attacker} damage {@code victim}? Player-versus-player only. */
    ProtectionDecision canDamagePlayer(UUID attacker, UUID victim, Territory attackerTerritory,
                                       Territory victimTerritory);
}
