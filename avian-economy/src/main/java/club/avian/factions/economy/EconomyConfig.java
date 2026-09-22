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

    private static Map<String, Long> defaultSellValues() {
        var values = new LinkedHashMap<String, Long>();
        // Farming (spec §30's main crops) — sugar cane is the headline farming economy.
        values.put("SUGAR_CANE", 5L);
        values.put("CACTUS", 5L);
        values.put("WHEAT", 4L);
        values.put("CARROT", 4L);
        values.put("POTATO", 4L);
        values.put("MELON_SLICE", 2L);
        values.put("PUMPKIN", 6L);
        // Mob drops (§29).
        values.put("ROTTEN_FLESH", 2L);
        values.put("BONE", 4L);
        values.put("STRING", 4L);
        values.put("SPIDER_EYE", 6L);
        values.put("GUNPOWDER", 10L);
        values.put("ENDER_PEARL", 25L);
        values.put("BLAZE_ROD", 30L);
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

    static void validate(EconomyConfig cfg, ConfigErrors e) {
        e.check(cfg.seasonId >= 1, "season-id", "must be >= 1");
        e.check(cfg.startingMoney >= 0, "starting-money", "must be >= 0");
        e.check(cfg.startingTokens >= 0, "starting-tokens", "must be >= 0");
        e.check(cfg.startingGems >= 0, "starting-gems", "must be >= 0");
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
