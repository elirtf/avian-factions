package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

/**
 * {@code factions.conf}. Factions, claims, power and protection belong to FactionsUUID (ADR-0007)
 * and are configured in its own {@code config/main.conf}; this file holds only what we add on top.
 */
@ConfigSerializable
public final class FactionsConfig {

    public static final ConfigSpec<FactionsConfig> SPEC = ConfigSpec.of("factions.conf", FactionsConfig.class)
            .version(1)
            .validate(FactionsConfig::validate)
            .build();

    @Comment("Flat power every new faction gets on top of its members' power, so a solo player can\n"
            + "hold a real base. With FactionsUUID one power is one chunk: base 5 + a full player's 20\n"
            + "is 25, a 5x5 square. Applied once, when the faction is created; changing it does not\n"
            + "touch existing factions (use /f admin power boost for those). Not lost on death.")
    private double factionBasePower = 5.0;

    @Comment("FactionsUUID upgrades factions can buy, by name. Every other upgrade is switched off.\n"
            + "Costs and levels are FactionsUUID's own, in its data/universe.json.\n"
            + "Launch set: claim_boost (ours), spawner_rate, crop_yield + growth (farming), mob_exp, tnt_bank,\n"
            + "and warps: FactionsUUID sets a faction's warp limit through it (5, free at level 1), so without\n"
            + "it no faction can set a warp. vaults: /f vault, through PlayerVaultsX (#30).")
    private List<String> enabledUpgrades = List.of("claim_boost", "spawner_rate", "crop_yield", "growth", "mob_exp", "tnt_bank",
            "warps", "vaults");

    private ChunkBuster chunkBuster = new ChunkBuster();

    private ClaimLoading claimLoading = new ClaimLoading();

    private NetheriteAlert netheriteAlert = new NetheriteAlert();

    public double factionBasePower() {
        return factionBasePower;
    }

    public List<String> enabledUpgrades() {
        return enabledUpgrades;
    }

    public NetheriteAlert netheriteAlert() {
        return netheriteAlert;
    }

    public ChunkBuster chunkBuster() {
        return chunkBuster;
    }

    public ClaimLoading claimLoading() {
        return claimLoading;
    }

    static void validate(FactionsConfig cfg, ConfigErrors e) {
        e.check(Double.isFinite(cfg.factionBasePower) && cfg.factionBasePower >= 0, "faction-base-power",
                "must be a finite number >= 0 (got %s)", cfg.factionBasePower);
        e.check(cfg.chunkBuster.layersPerTick >= 1 && cfg.chunkBuster.layersPerTick <= 16,
                "chunk-buster.layers-per-tick", "must be 1-16");
        e.check(cfg.netheriteAlert.cooldownSeconds >= 0, "netherite-alert.cooldown-seconds", "must be >= 0");
        e.check(cfg.chunkBuster.minDistanceFromSpawn >= 0, "chunk-buster.min-distance-from-spawn", "must be >= 0");
        e.check(cfg.chunkBuster.grantOnJoin >= 0 && cfg.chunkBuster.grantOnJoin <= 64, "chunk-buster.grant-on-join", "must be 0-64");
        e.check(cfg.claimLoading.maxChunksPerFaction >= 0, "claim-loading.max-chunks-per-faction", "must be >= 0");
        e.check(cfg.claimLoading.reconcileSeconds >= 5, "claim-loading.reconcile-seconds", "must be >= 5");
        for (var env : cfg.chunkBuster.environments) {
            e.check(java.util.Arrays.stream(org.bukkit.World.Environment.values()).anyMatch(v -> v.name().equals(env)),
                    "chunk-buster.environments", "%s is not NORMAL, NETHER or THE_END", env);
        }
        for (var name : cfg.chunkBuster.keep) {
            e.check(org.bukkit.Material.matchMaterial(name) != null, "chunk-buster.keep", "%s is not a block", name);
        }
    }

    /** The chunk buster: clears a chunk in the player's own claim, from where it is placed down. */
    @ConfigSerializable
    public static final class ChunkBuster {

        @Comment("Whether chunk busters work at all.")
        private boolean enabled = true;

        @Comment("Layers cleared per tick (16x16 blocks each). 1 is gentle; higher is faster and heavier.")
        private int layersPerTick = 1;

        @Comment("""
                Blocks a chunk buster never removes. Containers (chests, hoppers, barrels, shulker boxes,
                furnaces…) are always kept too, so nothing is lost, as are bedrock and spawners.""")
        private List<String> keep = List.of("BEDROCK", "SPAWNER", "END_PORTAL_FRAME", "END_PORTAL", "BEACON");

        @Comment("Seconds a player has to click [BUST CHUNK] after placing one.")
        private int confirmSeconds = 15;

        @Comment("""
                Worlds busters work in, by type: NORMAL (overworld), NETHER, THE_END. In the Nether they
                never reach the roof, bedrock is never removed, and edges touching lava or water are
                sealed with netherrack (stone in the overworld) so nothing floods in.""")
        private List<String> environments = List.of("NORMAL", "NETHER");

        @Comment("""
                Busters do nothing within this many blocks of the world's spawn, measured flat (X and Z).
                Owner, 2026-09-29: at least 1,000.""")
        private int minDistanceFromSpawn = 1000;

        @Comment("""
                Busters a player is given the first time they found or join a faction. Once per player,
                ever: leaving, disbanding and joining again gives nothing, so busters can't be farmed.
                0 turns it off.""")
        private int grantOnJoin = 5;

        public boolean enabled() {
            return enabled;
        }

        public int layersPerTick() {
            return layersPerTick;
        }

        public List<String> keep() {
            return keep;
        }

        public int confirmSeconds() {
            return confirmSeconds;
        }

        public List<String> environments() {
            return environments;
        }

        public int minDistanceFromSpawn() {
            return minDistanceFromSpawn;
        }

        public int grantOnJoin() {
            return grantOnJoin;
        }
    }

    /** Telling the server when someone mines ancient debris, so players go looking for each other. */
    @ConfigSerializable
    public static final class NetheriteAlert {

        @Comment("Announce in chat when a player mines ancient debris (owner, 2026-09-29).")
        private boolean enabled = true;

        @Comment("Seconds before the same player is announced again, so mining a vein is one message.")
        private int cooldownSeconds = 60;

        public boolean enabled() {
            return enabled;
        }

        public int cooldownSeconds() {
            return cooldownSeconds;
        }
    }

    /** Keeping a faction's land loaded while a member is online, so crops grow while they are away. */
    @ConfigSerializable
    public static final class ClaimLoading {

        @Comment("""
                Keep a faction's claimed chunks loaded while any of its members is online, so crops,
                cane and farms keep growing while they are elsewhere. Unloaded when the last one leaves.""")
        private boolean enabled = true;

        @Comment("At most this many of one faction's chunks are held (the lag safety valve).")
        private int maxChunksPerFaction = 128;

        @Comment("How often, in seconds, held chunks are brought in line with claims and who is online.")
        private int reconcileSeconds = 30;

        public boolean enabled() {
            return enabled;
        }

        public int maxChunksPerFaction() {
            return maxChunksPerFaction;
        }

        public int reconcileSeconds() {
            return reconcileSeconds;
        }
    }
}
