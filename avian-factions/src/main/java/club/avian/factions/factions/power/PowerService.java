package club.avian.factions.factions.power;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.faction.Faction;
import club.avian.factions.factions.FactionsConfig;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Power for every player in the Season, held in memory and written through on change (spec §8).
 *
 * <p>Nothing is scheduled. {@link #powerOf} evaluates regeneration or decay from the stored
 * timestamp at the moment it is asked, so the cost is one subtraction per call rather than a task
 * that walks every player (#4: the plugins that scan are the plugins that lag).
 */
public final class PowerService {

    private final Map<UUID, PlayerPower> power = new ConcurrentHashMap<>();
    private final PowerRepository repository;
    private final ConfigHandle<FactionsConfig> config;
    private final Clock clock;
    private final int seasonId;

    public PowerService(PowerRepository repository, ConfigHandle<FactionsConfig> config, Clock clock, int seasonId) {
        this.repository = repository;
        this.config = config;
        this.clock = clock;
        this.seasonId = seasonId;
    }

    /** Loads the Season's rows. Boot-time; see {@code Database#queryDuringBoot}. */
    public int load() {
        var rows = repository.loadSeason(seasonId);
        rows.forEach(row -> power.put(row.player(), row));
        return rows.size();
    }

    private PowerConfig rules() {
        return config.get().power();
    }

    /** This player's power right now, regenerated or decayed from what is stored. */
    public double powerOf(UUID player) {
        var stored = power.get(player);
        if (stored == null) {
            return rules().starting();
        }
        return PowerMath.currentValue(stored, rules(), clock.instant());
    }

    /** The most this player may hold, including any admin boost. */
    public double maximumOf(UUID player) {
        var stored = power.get(player);
        return stored == null ? rules().maximum() : PowerMath.maximumFor(stored, rules());
    }

    /** A Faction's power: the sum over its members, evaluated on demand (spec §8). */
    public double powerOf(Faction faction) {
        double total = 0;
        for (var member : faction.members().keySet()) {
            total += powerOf(member);
        }
        return total;
    }

    /** How many chunks this Faction may hold: {@code floor(power / per-claim)}. */
    public int claimCapacity(Faction faction) {
        return PowerMath.claimCapacity(powerOf(faction), rules());
    }

    /** True when the Faction holds more claims than its power supports (CONTEXT.md "Raidable"). */
    public boolean isRaidable(Faction faction, int claimCount) {
        return claimCount > claimCapacity(faction);
    }

    /** Login: settle what accrued while offline, then start accruing regeneration. */
    public CompletableFuture<Void> handleLogin(UUID player) {
        var now = clock.instant();
        var stored = power.computeIfAbsent(player, p -> PlayerPower.starting(p, rules(), now));
        var settled = PowerMath.settle(stored, rules(), now).withOnlineSince(now);
        power.put(player, settled);
        return repository.save(settled, seasonId);
    }

    /** Logout: settle what regenerated while online, then start accruing decay. */
    public CompletableFuture<Void> handleQuit(UUID player) {
        var stored = power.get(player);
        if (stored == null) {
            return CompletableFuture.completedFuture(null);
        }
        var settled = PowerMath.settle(stored, rules(), clock.instant()).withOnlineSince(null);
        power.put(player, settled);
        return repository.save(settled, seasonId);
    }

    /** Death: apply the penalty to the value current right now. */
    public CompletableFuture<Void> handleDeath(UUID player) {
        var now = clock.instant();
        var stored = power.computeIfAbsent(player, p -> PlayerPower.starting(p, rules(), now));
        var after = PowerMath.onDeath(stored, rules(), now);
        power.put(player, after);
        return repository.save(after, seasonId);
    }

    /**
     * Admin adjustment of the maximum.
     *
     * @throws IllegalArgumentException if {@code boost} is not finite (#4: validate numeric admin
     *     input — NaN or infinity stored here would poison every later comparison)
     */
    public CompletableFuture<Void> setBoost(UUID player, double boost) {
        if (!Double.isFinite(boost)) {
            throw new IllegalArgumentException("power boost must be a finite number, got " + boost);
        }
        var now = clock.instant();
        var stored = power.computeIfAbsent(player, p -> PlayerPower.starting(p, rules(), now));
        var settled = PowerMath.settle(stored, rules(), now).withBoost(boost, now);
        power.put(player, settled);
        return repository.save(settled, seasonId);
    }

    public int trackedPlayers() {
        return power.size();
    }
}
