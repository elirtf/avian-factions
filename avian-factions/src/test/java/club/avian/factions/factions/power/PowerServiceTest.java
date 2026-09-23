package club.avian.factions.factions.power;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.faction.FactionRank;
import club.avian.factions.factions.FactionRecord;
import club.avian.factions.factions.FactionsConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerServiceTest {

    static final Instant T0 = Instant.parse("2026-09-22T12:00:00Z");

    /** A clock the test moves by hand, so elapsed-time behaviour is deterministic. */
    static final class TestClock extends java.time.Clock {
        Instant now = T0;

        @Override public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        @Override public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }

        void advance(Duration d) {
            now = now.plus(d);
        }
    }

    static final class MemoryRepository implements PowerRepository {
        final Map<UUID, PlayerPower> rows = new HashMap<>();
        final List<UUID> saves = new ArrayList<>();
        List<PlayerPower> season = List.of();

        @Override public List<PlayerPower> loadSeason(int seasonId) {
            return season;
        }

        @Override public CompletableFuture<Void> save(PlayerPower power, int seasonId) {
            rows.put(power.player(), power);
            saves.add(power.player());
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<PlayerPower> findOrCreate(UUID p, int s, PowerConfig c, Instant now) {
            return CompletableFuture.completedFuture(
                    rows.computeIfAbsent(p, k -> PlayerPower.starting(k, c, now)));
        }
    }

    static final class FixedHandle implements ConfigHandle<FactionsConfig> {
        final FactionsConfig config = new FactionsConfig();
        @Override public FactionsConfig get() { return config; }
        @Override public void onReload(Consumer<FactionsConfig> callback) { }
    }

    final TestClock clock = new TestClock();
    final MemoryRepository repo = new MemoryRepository();
    final FixedHandle config = new FixedHandle();
    final PowerService service = new PowerService(repo, config, clock, 1);
    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();

    private FactionRecord faction(UUID... members) {
        Map<UUID, FactionRank> map = new HashMap<>();
        for (var m : members) {
            map.put(m, m == members[0] ? FactionRank.LEADER : FactionRank.MEMBER);
        }
        return new FactionRecord(UUID.randomUUID(), "Ravens", members[0], map, 1, T0);
    }

    @Test
    void anUnknownPlayerIsWorthTheStartingPower() {
        assertEquals(20.0, service.powerOf(alice), 1e-9);
        assertEquals(0, service.trackedPlayers(), "reading must not create a row");
    }

    @Test
    void loginSettlesThenRegeneratesWhileOnline() {
        service.handleLogin(alice).join();
        service.handleDeath(alice).join();              // 20 → 18
        assertEquals(18.0, service.powerOf(alice), 1e-9);
        clock.advance(Duration.ofHours(1));
        assertEquals(19.0, service.powerOf(alice), 1e-9, "regenerates while online");
    }

    @Test
    void quitFreezesPowerSoOfflineTimeDoesNotRegenerate() {
        service.handleLogin(alice).join();
        service.handleDeath(alice).join();              // 18
        service.handleQuit(alice).join();
        clock.advance(Duration.ofDays(7));
        assertEquals(18.0, service.powerOf(alice), 1e-9, "no regeneration while offline");
    }

    @Test
    void deathIsAppliedToTheRegeneratedValueAndPersistedOnce() {
        service.handleLogin(alice).join();
        service.handleDeath(alice).join();              // 20 → 18
        clock.advance(Duration.ofHours(1));             // → 19
        service.handleDeath(alice).join();              // → 17
        assertEquals(17.0, service.powerOf(alice), 1e-9);
        assertEquals(3, repo.saves.size(), "login + two deaths, one write each");
    }

    @Test
    void factionPowerIsTheBasePlusTheSumOverMembersAndDrivesCapacity() {
        var ravens = faction(alice, bob);
        assertEquals(140.0, service.powerOf(ravens), 1e-9, "100 base + 2 × 20");
        assertEquals(28, service.claimCapacity(ravens), "140 / 5 per claim");

        service.handleLogin(alice).join();
        service.handleDeath(alice).join();              // alice 18, faction 138
        assertEquals(138.0, service.powerOf(ravens), 1e-9);
        assertEquals(27, service.claimCapacity(ravens), "138 / 5 = 27.6 → 27");
    }

    @Test
    void aSoloFactionStillHoldsARealBase() {
        assertEquals(24, service.claimCapacity(faction(alice)), "(100 + 20) / 5");
    }

    @Test
    void raidableWhenClaimsExceedCapacity() {
        var ravens = faction(alice, bob);               // capacity 28
        assertFalse(service.isRaidable(ravens, 28));
        assertTrue(service.isRaidable(ravens, 29), "one claim over capacity is raidable");
    }

    @Test
    void loadRestoresTheSeasonIntoMemory() {
        repo.season = List.of(new PlayerPower(alice, 3.5, 0, T0, null));
        assertEquals(1, service.load());
        assertEquals(3.5, service.powerOf(alice), 1e-9);
    }

    @Test
    void nonFiniteBoostIsRefusedBeforeItCanPoisonComparisons() {
        assertThrows(IllegalArgumentException.class, () -> service.setBoost(alice, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> service.setBoost(alice, Double.POSITIVE_INFINITY));
        assertEquals(0, repo.saves.size(), "a refused boost must not be written");
    }

    @Test
    void boostRaisesTheCeilingForRegeneration() {
        service.setBoost(alice, 5).join();
        service.handleLogin(alice).join();
        clock.advance(Duration.ofDays(1));
        assertEquals(25.0, service.powerOf(alice), 1e-9);
        assertEquals(25.0, service.maximumOf(alice), 1e-9);
    }
}
