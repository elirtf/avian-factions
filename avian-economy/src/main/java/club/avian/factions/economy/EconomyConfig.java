package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.config.RequiresRestart;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;

/** {@code economy.conf} — currencies, starting balances and the sell table (spec §12, §30). */
@ConfigSerializable
public final class EconomyConfig {

    public static final ConfigSpec<EconomyConfig> SPEC = ConfigSpec.of("economy.conf", EconomyConfig.class)
            .version(1)
            .validate(EconomyConfig::validate)
            .freeForm("sell-values")
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

    static void validate(EconomyConfig cfg, ConfigErrors e) {
        e.check(cfg.seasonId >= 1, "season-id", "must be >= 1");
        e.check(cfg.startingMoney >= 0, "starting-money", "must be >= 0");
        e.check(cfg.startingTokens >= 0, "starting-tokens", "must be >= 0");
        e.check(cfg.startingGems >= 0, "starting-gems", "must be >= 0");
        e.check(cfg.sugarCaneTokenChance >= 0 && cfg.sugarCaneTokenChance <= 1, "sugar-cane-token-chance", "must be 0-1");
        e.check(cfg.sugarCaneTokens >= 0, "sugar-cane-tokens", "must be >= 0");
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
