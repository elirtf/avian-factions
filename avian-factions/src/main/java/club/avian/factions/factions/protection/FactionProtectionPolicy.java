package club.avian.factions.factions.protection;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.faction.Faction;
import club.avian.factions.api.faction.Interaction;
import club.avian.factions.api.faction.ProtectionDecision;
import club.avian.factions.api.faction.ProtectionPolicy;
import club.avian.factions.api.faction.Territory;
import club.avian.factions.factions.FactionsConfig;

import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * The claim rules (spec §9), as one pure function.
 *
 * <p>Order is fixed and every branch returns, so there is no way to fall through to a later rule
 * by accident — the shape of bug that produced UID #42 and #55.
 *
 * <ol>
 *   <li>unprotected land (wilderness, warzone, disabled world) → allow
 *   <li>safezone → deny outright, staff bypass aside
 *   <li>staff bypass → allow
 *   <li>member of the owning Faction → allow
 *   <li>owning Faction is raidable → allow
 *   <li>otherwise → deny, unless the interaction is in the non-member allow list
 * </ol>
 */
public final class FactionProtectionPolicy implements ProtectionPolicy {

    private final ConfigHandle<FactionsConfig> config;
    private final Predicate<UUID> hasBypass;
    private final BiPredicate<UUID, Faction> isMember;
    private final Predicate<Faction> isRaidable;

    public FactionProtectionPolicy(ConfigHandle<FactionsConfig> config, Predicate<UUID> hasBypass,
                                   BiPredicate<UUID, Faction> isMember, Predicate<Faction> isRaidable) {
        this.config = config;
        this.hasBypass = hasBypass;
        this.isMember = isMember;
        this.isRaidable = isRaidable;
    }

    @Override
    public ProtectionDecision check(UUID player, Territory territory, Interaction interaction) {
        // 1. Land nobody owns. Wilderness and warzone are free-for-all by definition.
        if (territory.kind() == Territory.Kind.WILDERNESS || territory.kind() == Territory.Kind.WARZONE) {
            return ProtectionDecision.ALLOW_UNPROTECTED;
        }
        // 2. Staff bypass, checked before safezone so admins can build Spawn.
        if (hasBypass.test(player)) {
            return ProtectionDecision.ALLOW_BYPASS;
        }
        // 3. Safezone: nobody but staff, ever.
        if (territory.kind() == Territory.Kind.SAFEZONE) {
            return ProtectionDecision.DENY_SAFEZONE;
        }
        var owner = territory.faction().orElse(null);
        if (owner == null) {
            // A FACTION territory with no faction is a bug upstream; fail safe, not open.
            return ProtectionDecision.DENY_FOREIGN_TERRITORY;
        }
        // 4. Members do as they like in their own land.
        if (isMember.test(player, owner)) {
            return ProtectionDecision.ALLOW_MEMBER;
        }
        // 5. Overclaimed factions lose protection — this is the whole point of Power.
        if (isRaidable.test(owner)) {
            return ProtectionDecision.ALLOW_RAIDABLE;
        }
        // 6. Anything explicitly opened to outsiders.
        if (config.get().protection().nonMemberAllowed().contains(interaction)) {
            return ProtectionDecision.ALLOW_UNPROTECTED;
        }
        return ProtectionDecision.DENY_FOREIGN_TERRITORY;
    }

    @Override
    public boolean denyExplosion(Territory territory) {
        if (territory.kind() == Territory.Kind.WILDERNESS || territory.kind() == Territory.Kind.WARZONE) {
            return false;
        }
        if (territory.kind() == Territory.Kind.SAFEZONE) {
            return true;
        }
        if (config.get().protection().explosionsInClaims()) {
            return false;
        }
        // A raidable faction's claims explode like wilderness; that is how raids start.
        return territory.faction().map(owner -> !isRaidable.test(owner)).orElse(true);
    }

    @Override
    public ProtectionDecision canDamagePlayer(UUID attacker, UUID victim, Territory attackerTerritory,
                                              Territory victimTerritory) {
        // PvP is denied only where the *victim* stands: a safezone protects whoever is inside it,
        // and shooting out of one must not become a free shot.
        if (victimTerritory.kind() == Territory.Kind.SAFEZONE
                || attackerTerritory.kind() == Territory.Kind.SAFEZONE) {
            return hasBypass.test(attacker) ? ProtectionDecision.ALLOW_BYPASS : ProtectionDecision.DENY_SAFEZONE;
        }
        // Everywhere else PvP is open. Relations (ally/truce friendly fire) plug in here.
        return ProtectionDecision.ALLOW_UNPROTECTED;
    }
}
