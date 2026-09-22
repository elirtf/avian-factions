package club.avian.factions.factions.protection;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.faction.Faction;
import club.avian.factions.api.faction.FactionRank;
import club.avian.factions.api.faction.Interaction;
import club.avian.factions.api.faction.ProtectionDecision;
import club.avian.factions.api.faction.Territory;
import club.avian.factions.factions.FactionRecord;
import club.avian.factions.factions.FactionsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec §9. Every branch of the decision function, for every interaction kind — the regression
 * suite #4 asks for (UID #42 and #55 were both missing cases, not wrong logic).
 */
class FactionProtectionPolicyTest {

    static final UUID MEMBER = UUID.randomUUID();
    static final UUID OUTSIDER = UUID.randomUUID();
    static final UUID STAFF = UUID.randomUUID();

    static final class Handle implements ConfigHandle<FactionsConfig> {
        final FactionsConfig config = new FactionsConfig();
        @Override public FactionsConfig get() { return config; }
        @Override public void onReload(Consumer<FactionsConfig> callback) { }
    }

    final Handle config = new Handle();
    final Set<UUID> bypassing = new HashSet<>(Set.of(STAFF));
    final Set<UUID> raidableFactions = new HashSet<>();

    final Faction ravens = new FactionRecord(UUID.randomUUID(), "Ravens", MEMBER,
            Map.of(MEMBER, FactionRank.LEADER), 1, Instant.EPOCH);

    final FactionProtectionPolicy policy = new FactionProtectionPolicy(config,
            bypassing::contains,
            (player, faction) -> faction.hasMember(player),
            faction -> raidableFactions.contains(faction.id()));

    private Territory claimed() {
        return territory(Territory.Kind.FACTION, ravens);
    }

    private static Territory territory(Territory.Kind kind, Faction faction) {
        return new Territory() {
            @Override public Kind kind() { return kind; }
            @Override public Optional<Faction> faction() { return Optional.ofNullable(faction); }
        };
    }

    // --- the six branches, in order ----------------------------------------------------------

    @ParameterizedTest
    @EnumSource(Interaction.class)
    void wildernessIsAlwaysOpen(Interaction interaction) {
        assertEquals(ProtectionDecision.ALLOW_UNPROTECTED,
                policy.check(OUTSIDER, Territory.wilderness(), interaction));
    }

    @ParameterizedTest
    @EnumSource(Interaction.class)
    void warzoneIsAlwaysOpen(Interaction interaction) {
        assertEquals(ProtectionDecision.ALLOW_UNPROTECTED,
                policy.check(OUTSIDER, territory(Territory.Kind.WARZONE, null), interaction));
    }

    @ParameterizedTest
    @EnumSource(Interaction.class)
    void safezoneIsClosedToEveryoneButStaff(Interaction interaction) {
        var safezone = territory(Territory.Kind.SAFEZONE, null);
        assertEquals(ProtectionDecision.DENY_SAFEZONE, policy.check(OUTSIDER, safezone, interaction));
        assertEquals(ProtectionDecision.DENY_SAFEZONE, policy.check(MEMBER, safezone, interaction));
        assertEquals(ProtectionDecision.ALLOW_BYPASS, policy.check(STAFF, safezone, interaction),
                "staff must be able to build Spawn");
    }

    @ParameterizedTest
    @EnumSource(Interaction.class)
    void membersMayDoAnythingInTheirOwnClaims(Interaction interaction) {
        assertEquals(ProtectionDecision.ALLOW_MEMBER, policy.check(MEMBER, claimed(), interaction));
    }

    @ParameterizedTest
    @EnumSource(Interaction.class)
    void outsidersAreDeniedEverythingByDefault(Interaction interaction) {
        assertEquals(ProtectionDecision.DENY_FOREIGN_TERRITORY, policy.check(OUTSIDER, claimed(), interaction),
                "spec §9 default is deny for every listed interaction");
    }

    @ParameterizedTest
    @EnumSource(Interaction.class)
    void staffBypassEveryClaim(Interaction interaction) {
        assertEquals(ProtectionDecision.ALLOW_BYPASS, policy.check(STAFF, claimed(), interaction));
    }

    @ParameterizedTest
    @EnumSource(Interaction.class)
    void raidableClaimsLoseProtectionForOutsiders(Interaction interaction) {
        raidableFactions.add(ravens.id());
        assertEquals(ProtectionDecision.ALLOW_RAIDABLE, policy.check(OUTSIDER, claimed(), interaction),
                "overclaimed factions are raidable — the entire point of Power");
        assertEquals(ProtectionDecision.ALLOW_MEMBER, policy.check(MEMBER, claimed(), interaction),
                "members are still members");
    }

    @Test
    void theNonMemberAllowListOpensOnlyWhatItNames() {
        setNonMemberAllowed("DOOR");
        assertEquals(ProtectionDecision.ALLOW_UNPROTECTED, policy.check(OUTSIDER, claimed(), Interaction.DOOR));
        assertEquals(ProtectionDecision.DENY_FOREIGN_TERRITORY, policy.check(OUTSIDER, claimed(), Interaction.CONTAINER));
        assertEquals(ProtectionDecision.DENY_FOREIGN_TERRITORY, policy.check(OUTSIDER, claimed(), Interaction.BUILD));
    }

    @Test
    void aFactionTerritoryWithNoFactionFailsClosed() {
        // Should not happen; if the index ever hands us one, protecting is the safe answer.
        assertEquals(ProtectionDecision.DENY_FOREIGN_TERRITORY,
                policy.check(OUTSIDER, territory(Territory.Kind.FACTION, null), Interaction.BUILD));
    }

    @Test
    void decisionsClassifyThemselvesCorrectly() {
        assertTrue(ProtectionDecision.ALLOW_MEMBER.allowed());
        assertTrue(ProtectionDecision.ALLOW_RAIDABLE.allowed());
        assertTrue(ProtectionDecision.ALLOW_BYPASS.allowed());
        assertTrue(ProtectionDecision.ALLOW_UNPROTECTED.allowed());
        assertTrue(ProtectionDecision.DENY_FOREIGN_TERRITORY.denied());
        assertTrue(ProtectionDecision.DENY_SAFEZONE.denied());
    }

    // --- explosions ---------------------------------------------------------------------------

    @Test
    void explosionsAreStoppedInClaimsButNotInWilderness() {
        assertFalse(policy.denyExplosion(Territory.wilderness()));
        assertFalse(policy.denyExplosion(territory(Territory.Kind.WARZONE, null)));
        assertTrue(policy.denyExplosion(claimed()));
        assertTrue(policy.denyExplosion(territory(Territory.Kind.SAFEZONE, null)));
    }

    @Test
    void raidableClaimsExplodeLikeWilderness() {
        raidableFactions.add(ravens.id());
        assertFalse(policy.denyExplosion(claimed()), "raids have to be able to breach walls");
    }

    @Test
    void explosionsInClaimsCanBeTurnedOnGlobally() {
        setExplosionsInClaims(true);
        assertFalse(policy.denyExplosion(claimed()));
        assertTrue(policy.denyExplosion(territory(Territory.Kind.SAFEZONE, null)),
                "the safezone is never negotiable");
    }

    // --- pvp -----------------------------------------------------------------------------------

    @Test
    void pvpIsOpenOutsideSafezones() {
        assertEquals(ProtectionDecision.ALLOW_UNPROTECTED,
                policy.canDamagePlayer(OUTSIDER, MEMBER, Territory.wilderness(), claimed()));
        assertEquals(ProtectionDecision.ALLOW_UNPROTECTED,
                policy.canDamagePlayer(OUTSIDER, MEMBER, claimed(), Territory.wilderness()));
    }

    @Test
    void safezoneProtectsTheVictimAndAlsoBlocksShootingOutOfOne() {
        var safezone = territory(Territory.Kind.SAFEZONE, null);
        assertEquals(ProtectionDecision.DENY_SAFEZONE,
                policy.canDamagePlayer(OUTSIDER, MEMBER, Territory.wilderness(), safezone),
                "a player standing in spawn cannot be hit");
        assertEquals(ProtectionDecision.DENY_SAFEZONE,
                policy.canDamagePlayer(OUTSIDER, MEMBER, safezone, Territory.wilderness()),
                "and spawn is not a free firing position");
    }

    // --- config helpers -------------------------------------------------------------------------

    private void setNonMemberAllowed(String... kinds) {
        set("nonMemberAllowed", java.util.List.of(kinds));
    }

    private void setExplosionsInClaims(boolean value) {
        set("explosionsInClaims", value);
    }

    private void set(String field, Object value) {
        try {
            var f = ProtectionConfig.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(config.get().protection(), value);
            var parsed = ProtectionConfig.class.getDeclaredField("parsed");
            parsed.setAccessible(true);
            parsed.set(config.get().protection(), null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ProtectionConfig field renamed: " + field, e);
        }
    }
}
