package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.config.RequiresRestart;
import org.bukkit.entity.EntityType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** {@code economy.conf} — currencies, starting balances and the sell table (spec §12, §30). */
@ConfigSerializable
public final class EconomyConfig {

    public static final ConfigSpec<EconomyConfig> SPEC = ConfigSpec.of("economy.conf", EconomyConfig.class)
            .version(1)
            .validate(EconomyConfig::validate)
            .freeForm("sell-values", "spawner-drops")
            .build();

    @RequiresRestart
    @Comment("Season these balances belong to. Must match factions.conf's season-id.")
    private int seasonId = 1;

    @Comment("Money a player starts with.")
    private long startingMoney = 0;

    @Comment("Tokens a player starts with.")
    private long startingTokens = 0;

    @Comment("Gems a player starts with.")
    private long startingGems = 0;

    @Comment("Register our money as Vault's economy provider, so EssentialsX and any other\n"
            + "Vault-aware plugin reads and writes the same balance. Turn this off only if\n"
            + "something else must own the economy.")
    private boolean provideVault = true;

    @Comment("""
            What items sell for, in whole dollars per item. Anything not listed cannot be sold.
            This one table is read by /shop sell, sell-all and the Harvester Hoe's autosell, so a
            price can never disagree with itself between features.""")
    private Map<String, Long> sellValues = defaultSellValues();

    @Comment("""
            Chance, per sugar cane block a player breaks by hand, of earning tokens (0.02 = 1 in 50).
            Cane the player placed never pays, so place-and-break earns nothing; only grown cane
            does. Auto-farms (pistons, observers) earn money but no tokens.""")
    private double sugarCaneTokenChance = 0.02;

    @Comment("Tokens earned each time that chance hits.")
    private long sugarCaneTokens = 1;

    @Comment("""
            Whether villagers may summon iron golems. false stops vanilla iron farms, which would
            otherwise produce free what iron golem spawners (the top spawner tier) are sold for.""")
    private boolean villagerIronGolems = false;

    @Comment("""
            Which mobs RoseStacker may stack (owner, 2026-10-01). Stacking turns many mobs into one,
            so it decides how much a spawner farm earns per kill.
              only-player-placed-spawners: only mobs from spawners players placed stack; wild mobs
                and mobs from dungeon, fortress or stronghold spawners never do.
              never-stack: entity types that never stack at all, whatever spawned them. Villagers,
                iron golems and silverfish feed the emerald economy, so each one is a single kill.""")
    private Stacking stacking = new Stacking();

    @Comment("""
            Extra drops for mobs from spawners players placed, by entity type: item and amount per death,
            however it dies. The emerald economy (owner, 2026-10-01): a villager spawner (the top tier)
            pays one emerald a villager. Wild, bred or dungeon-spawner mobs never drop these.""")
    private Map<String, Map<String, Integer>> spawnerDrops = new LinkedHashMap<>(Map.of("VILLAGER", new LinkedHashMap<>(Map.of("EMERALD", 1))));

    @Comment("""
            Whether villagers may pay emeralds in trades. false: they still sell things for emeralds,
            but never buy crops, paper or meat with them, since emeralds sell for money and those trades
            would make any farm free money. Emeralds come from villager spawners and mining.""")
    private boolean emeraldTrades = false;

    @Comment("""
            What EliteMobs' elites and bosses pay (owner, 2026-10-02: our rewards, not EliteMobs' own coins
            and gear). The player who dealt the most damage is paid.
              money-per-level: money for an elite, times its level (elite levels follow nearby players'
                gear: about 1-20 in vanilla gear).
              boss-tiers: what a custom boss pays, by its level; the highest tier the boss reaches applies.
                Commands run from the console, %player% is the player's name.""")
    private EliteRewards eliteRewards = new EliteRewards();

    @ConfigSerializable
    public static final class EliteRewards {
        private long moneyPerLevel = 25;
        private List<BossTier> bossTiers = List.of(
                new BossTier(0, 25, List.of("crates give virtual Rare 1 %player% -s")),
                new BossTier(20, 75, List.of("crates give virtual Epic 1 %player% -s")));

        public long moneyPerLevel() {
            return moneyPerLevel;
        }

        public List<BossTier> bossTiers() {
            return bossTiers;
        }
    }

    @ConfigSerializable
    public static final class BossTier {
        private int minLevel;
        private long tokens;
        private List<String> commands = List.of();

        public BossTier() {
        }

        BossTier(int minLevel, long tokens, List<String> commands) {
            this.minLevel = minLevel;
            this.tokens = tokens;
            this.commands = commands;
        }

        public int minLevel() {
            return minLevel;
        }

        public long tokens() {
            return tokens;
        }

        public List<String> commands() {
            return commands;
        }
    }

    @ConfigSerializable
    public static final class Stacking {
        private boolean onlyPlayerPlacedSpawners = true;
        private List<String> neverStack = List.of("VILLAGER", "IRON_GOLEM", "SILVERFISH");

        public boolean onlyPlayerPlacedSpawners() {
            return onlyPlayerPlacedSpawners;
        }

        public List<String> neverStack() {
            return neverStack;
        }
    }

    @Comment("""
            Fishing skill XP (AuraSkills) for custom-enchant catches. A Double Catch that doubles the fish
            earns the catch's XP again when true; Seasoned Angler adds this fraction per level
            (0.10 = +10 % a level). Vanilla XP orbs are the enchant's own business.""")
    private boolean doubleCatchSkillXp = true;
    private double seasonedAnglerSkillXpPerLevel = 0.10;

    private static Map<String, Long> defaultSellValues() {
        var values = new LinkedHashMap<String, Long>();
        // Fallback only: with EconomyShopGUI installed its /shop prices win (docs/ECONOMY.md).
        // Farming (spec §30): sugar cane is the headline farming economy.
        values.put("SUGAR_CANE", 16L);
        values.put("CACTUS", 4L);
        values.put("PUMPKIN", 3L);
        values.put("WHEAT", 2L);
        values.put("CARROT", 2L);
        values.put("POTATO", 2L);
        values.put("MELON_SLICE", 1L);
        // Mob drops (§29), by spawner tier.
        values.put("ROTTEN_FLESH", 12L);
        values.put("BONE", 10L);
        values.put("STRING", 10L);
        values.put("SPIDER_EYE", 8L);
        values.put("GUNPOWDER", 30L);
        values.put("ENDER_PEARL", 60L);
        values.put("BLAZE_ROD", 120L);
        values.put("IRON_INGOT", 40L);
        values.put("EMERALD", 250L);   // the emerald economy (owner, 2026-10-01)
        return values;
    }

    public int seasonId() {
        return seasonId;
    }

    public long startingMoney() {
        return startingMoney;
    }

    public long startingTokens() {
        return startingTokens;
    }

    public long startingGems() {
        return startingGems;
    }

    public boolean provideVault() {
        return provideVault;
    }

    public Map<String, Long> sellValues() {
        return sellValues;
    }

    public double sugarCaneTokenChance() {
        return sugarCaneTokenChance;
    }

    public long sugarCaneTokens() {
        return sugarCaneTokens;
    }

    public boolean villagerIronGolems() {
        return villagerIronGolems;
    }

    public Stacking stacking() {
        return stacking;
    }

    public Map<String, Map<String, Integer>> spawnerDrops() {
        return spawnerDrops;
    }

    public boolean emeraldTrades() {
        return emeraldTrades;
    }

    public EliteRewards eliteRewards() {
        return eliteRewards;
    }

    public boolean doubleCatchSkillXp() {
        return doubleCatchSkillXp;
    }

    public double seasonedAnglerSkillXpPerLevel() {
        return seasonedAnglerSkillXpPerLevel;
    }

    static void validate(EconomyConfig cfg, ConfigErrors e) {
        e.check(cfg.seasonId >= 1, "season-id", "must be >= 1");
        e.check(cfg.startingMoney >= 0, "starting-money", "must be >= 0");
        e.check(cfg.startingTokens >= 0, "starting-tokens", "must be >= 0");
        e.check(cfg.startingGems >= 0, "starting-gems", "must be >= 0");
        e.check(cfg.sugarCaneTokenChance >= 0 && cfg.sugarCaneTokenChance <= 1, "sugar-cane-token-chance", "must be 0-1");
        e.check(cfg.sugarCaneTokens >= 0, "sugar-cane-tokens", "must be >= 0");
        cfg.spawnerDrops.forEach((type, drops) -> {
            e.check(Arrays.stream(EntityType.values()).anyMatch(t -> t.name().equals(type)),
                    "spawner-drops", "\"%s\" is not an entity type (e.g. VILLAGER)", type);
            drops.forEach((item, amount) -> {
                e.check(org.bukkit.Material.matchMaterial(item) != null, "spawner-drops." + type, "\"%s\" is not an item", item);
                e.check(amount >= 0, "spawner-drops." + type, "amounts must be >= 0");
            });
        });
        e.check(cfg.eliteRewards.moneyPerLevel >= 0, "elite-rewards.money-per-level", "must be >= 0");
        for (var tier : cfg.eliteRewards.bossTiers) {
            e.check(tier.minLevel >= 0 && tier.tokens >= 0, "elite-rewards.boss-tiers", "min-level and tokens must be >= 0");
        }
        for (var type : cfg.stacking.neverStack) {
            e.check(Arrays.stream(EntityType.values()).anyMatch(t -> t.name().equals(type)),
                    "stacking.never-stack", "\"%s\" is not an entity type (e.g. VILLAGER)", type);
        }
        e.check(cfg.seasonedAnglerSkillXpPerLevel >= 0, "seasoned-angler-skill-xp-per-level", "must be >= 0");
        for (var entry : cfg.sellValues.entrySet()) {
            if (org.bukkit.Material.matchMaterial(entry.getKey()) == null) {
                e.add("sell-values." + entry.getKey(), "is not a Minecraft material");
            }
            if (entry.getValue() < 0) {
                e.add("sell-values." + entry.getKey(), "must be >= 0 (got " + entry.getValue() + ")");
            }
        }
    }
}
