package club.avian.factions.cosmetics;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code trails.conf}: particle trails (owner, 2026-09-29: built in-house because PlayerParticles is
 * licensed non-commercial). Each trail is a permission, {@code avian.trail.<id>}, won from the
 * Cosmetics crate; {@code /trails} picks one.
 */
@ConfigSerializable
public final class TrailsConfig {

    public static final ConfigSpec<TrailsConfig> SPEC = ConfigSpec.of("trails.conf", TrailsConfig.class)
            .version(1)
            .validate(TrailsConfig::validate)
            .freeForm("trails")
            .build();

    static final String PERMISSION_PREFIX = "avian.trail.";

    @Comment("Whether trails show at all.")
    private boolean enabled = true;

    @Comment("Ticks between puffs (20 = one second). Lower looks smoother and costs more.")
    private int intervalTicks = 3;

    @Comment("Only show a trail while the player is moving, the usual look for a trail.")
    private boolean onlyWhileMoving = true;

    @Comment("""
            The trails, by id. particle is a Bukkit particle name; dust, dust colour transition and entity
            effect particles take colours (hex, cycled through). tier is common, rare or epic: the menu's
            colour, and a hint for crate weights.""")
    private Map<String, Trail> trails = defaults();

    @ConfigSerializable
    public static final class Trail {
        private String name = "<white>Trail";
        private String icon = "BLAZE_POWDER";
        private String particle = "FLAME";
        private int count = 2;
        private double spread = 0.15;
        private double speed = 0.0;
        private List<String> colours = List.of();
        private String tier = "common";

        Trail() {
        }

        Trail(String name, String icon, String particle, int count, double spread, double speed,
              List<String> colours, String tier) {
            this.name = name;
            this.icon = icon;
            this.particle = particle;
            this.count = count;
            this.spread = spread;
            this.speed = speed;
            this.colours = colours;
            this.tier = tier;
        }

        public String name() {
            return name;
        }

        public Material icon() {
            var m = Material.matchMaterial(icon);
            return m == null ? Material.BLAZE_POWDER : m;
        }

        public Particle particle() {
            return Particle.valueOf(particle.toUpperCase(Locale.ROOT));
        }

        public int count() {
            return count;
        }

        public double spread() {
            return spread;
        }

        public double speed() {
            return speed;
        }

        public List<Color> colours() {
            return colours.stream().map(TrailsConfig::parseColour).toList();
        }

        public String tier() {
            return tier.toLowerCase(Locale.ROOT);
        }
    }

    private static Map<String, Trail> defaults() {
        var t = new LinkedHashMap<String, Trail>();
        // Common
        t.put("flames", new Trail("<#FF7A45>Flames", "BLAZE_POWDER", "FLAME", 2, 0.12, 0.01, List.of(), "common"));
        t.put("notes", new Trail("<#7CFF4F>Notes", "NOTE_BLOCK", "NOTE", 1, 0.3, 0.0, List.of(), "common"));
        t.put("clouds", new Trail("<white>Clouds", "WHITE_WOOL", "CLOUD", 2, 0.15, 0.0, List.of(), "common"));
        t.put("sparkle", new Trail("<#4CE08A>Sparkle", "EMERALD", "HAPPY_VILLAGER", 2, 0.25, 0.0, List.of(), "common"));
        // Rare
        t.put("hearts", new Trail("<#FF4F7A>Hearts", "PINK_DYE", "HEART", 1, 0.3, 0.0, List.of(), "rare"));
        t.put("snow", new Trail("<#BFEFFF>Snowfall", "SNOWBALL", "SNOWFLAKE", 3, 0.2, 0.0, List.of(), "rare"));
        t.put("soul", new Trail("<#5FF3E0>Soul Fire", "SOUL_LANTERN", "SOUL_FIRE_FLAME", 2, 0.12, 0.01, List.of(), "rare"));
        t.put("enchant", new Trail("<#C58CFF>Arcane", "ENCHANTING_TABLE", "ENCHANT", 6, 0.3, 0.4, List.of(), "rare"));
        // Epic
        t.put("rainbow", new Trail("<rainbow>Rainbow</rainbow>", "PRISMARINE_CRYSTALS", "DUST", 3, 0.2, 0.0,
                List.of("#FF3B3B", "#FF9A3B", "#FFE83B", "#4CE08A", "#3BB8FF", "#8C5CFF", "#FF4FD8"), "epic"));
        t.put("cherry", new Trail("<#FFB7D5>Cherry Blossom", "CHERRY_SAPLING", "CHERRY_LEAVES", 3, 0.3, 0.0, List.of(), "epic"));
        t.put("ender", new Trail("<#8C5CFF>Ender Rift", "ENDER_PEARL", "PORTAL", 8, 0.25, 0.3, List.of(), "epic"));
        t.put("sparks", new Trail("<#FFE66D>Sparks", "LIGHTNING_ROD", "ELECTRIC_SPARK", 3, 0.25, 0.05, List.of(), "epic"));
        return t;
    }

    static Color parseColour(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return Color.fromRGB(Integer.parseInt(h, 16));
    }

    /** Whether we can draw this particle: no data, or data we know how to supply. */
    static boolean drawable(Particle particle) {
        var type = particle.getDataType();
        return type == Void.class || type == Particle.DustOptions.class
                || type == Particle.DustTransition.class || type == Color.class;
    }

    static void validate(TrailsConfig cfg, ConfigErrors e) {
        e.check(cfg.intervalTicks >= 1 && cfg.intervalTicks <= 40, "interval-ticks", "must be 1-40 (got %d)", cfg.intervalTicks);
        cfg.trails.forEach((id, trail) -> {
            String at = "trails." + id;
            e.check(id.matches("[a-z0-9_-]+"), at, "id must be lowercase letters, digits, - or _");
            Particle particle = null;
            try {
                particle = trail.particle();
            } catch (IllegalArgumentException ex) {
                e.check(false, at + ".particle", "%s is not a particle", trail.particle);
            }
            if (particle != null) {
                e.check(drawable(particle), at + ".particle", "%s needs data trails can't supply", trail.particle);
                boolean coloured = particle.getDataType() != Void.class;
                e.check(!coloured || !trail.colours.isEmpty(), at + ".colours", "%s needs at least one colour", trail.particle);
            }
            e.check(Material.matchMaterial(trail.icon) != null, at + ".icon", "%s is not an item", trail.icon);
            e.check(trail.count >= 1 && trail.count <= 20, at + ".count", "must be 1-20 (got %d)", trail.count);
            e.check(List.of("common", "rare", "epic").contains(trail.tier()), at + ".tier", "must be common, rare or epic");
            for (String c : trail.colours) {
                e.check(c.matches("#?[0-9A-Fa-f]{6}"), at + ".colours", "%s is not a #RRGGBB colour", c);
            }
        });
    }

    public boolean enabled() {
        return enabled;
    }

    public int intervalTicks() {
        return intervalTicks;
    }

    public boolean onlyWhileMoving() {
        return onlyWhileMoving;
    }

    public Map<String, Trail> trails() {
        return trails;
    }
}
