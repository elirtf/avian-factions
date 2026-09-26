package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.bukkit.Material;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@code sellwand.conf}: sell wands. Right-click a container with one to sell everything in it at
 * {@code /shop} prices, times the wand's multiplier. Tiers are data: add, remove or retune them here.
 */
@ConfigSerializable
public final class SellWandConfig {

    public static final ConfigSpec<SellWandConfig> SPEC = ConfigSpec.of("sellwand.conf", SellWandConfig.class)
            .version(1)
            .freeForm("tiers")
            .validate(SellWandConfig::validate)
            .build();

    @Comment("Milliseconds a player waits between two uses of any sell wand.")
    private long cooldownMillis = 1_000;

    @Comment("""
            The wand tiers, by id (used in /sellwand give <player> <tier>, crate prizes and shops).
              name       — shown on the item, MiniMessage with the Brand colours
              material   — the item it is made of
              uses       — how many sales before it breaks; -1 = never breaks
              multiplier — percent of the shop price paid: 100 = the shop price, 125 = 25 % more""")
    private Map<String, Tier> tiers = defaultTiers();

    private static Map<String, Tier> defaultTiers() {
        var tiers = new LinkedHashMap<String, Tier>();
        tiers.put("basic", new Tier("<soft>Sell Wand</soft>", Material.STICK, 100, 100));
        tiers.put("gilded", new Tier("<sun>Gilded Sell Wand</sun>", Material.BLAZE_ROD, 500, 110));
        tiers.put("eternal", new Tier("<gem>Eternal Sell Wand</gem>", Material.END_ROD, -1, 125));
        return tiers;
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }

    public Map<String, Tier> tiers() {
        return tiers;
    }

    /** The tier with this id, ignoring case; null if there is none. */
    public Tier tier(String id) {
        return id == null ? null : tiers.get(id.toLowerCase(Locale.ROOT));
    }

    static void validate(SellWandConfig cfg, ConfigErrors e) {
        e.check(cfg.cooldownMillis >= 0, "cooldown-millis", "must be >= 0");
        e.check(!cfg.tiers.isEmpty(), "tiers", "needs at least one tier");
        cfg.tiers.forEach((id, tier) -> {
            e.check(id.equals(id.toLowerCase(Locale.ROOT)), "tiers." + id, "ids are lower case");
            e.check(tier.uses == -1 || tier.uses > 0, "tiers." + id + ".uses", "must be -1 or more than 0");
            e.check(tier.multiplier > 0 && tier.multiplier <= 1_000, "tiers." + id + ".multiplier", "must be 1-1000");
            e.check(tier.material != null && tier.material.isItem(), "tiers." + id + ".material", "must be an item");
        });
    }

    @ConfigSerializable
    public static final class Tier {
        private String name = "<soft>Sell Wand</soft>";
        private Material material = Material.STICK;
        private int uses = 100;
        private int multiplier = 100;

        public Tier() {
        }

        Tier(String name, Material material, int uses, int multiplier) {
            this.name = name;
            this.material = material;
            this.uses = uses;
            this.multiplier = multiplier;
        }

        public String name() {
            return name;
        }

        public Material material() {
            return material;
        }

        /** Sales before it breaks; -1 = unlimited. */
        public int uses() {
            return uses;
        }

        /** Percent of the shop price paid. */
        public int multiplier() {
            return multiplier;
        }
    }
}
