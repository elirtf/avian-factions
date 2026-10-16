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
 * {@code gens.conf}: gen blocks (owner, 2026-10-16), the factions "gen bucket". Place one and it lays its
 * block in a line, one at a time, charging each block at its /shop price, through air only, passing over
 * blocks of its own kind, and stopping at anything else. The item is never used up.
 */
@ConfigSerializable
public final class GenBlocksConfig {

    public static final ConfigSpec<GenBlocksConfig> SPEC = ConfigSpec.of("gens.conf", GenBlocksConfig.class)
            .version(1)
            .freeForm("types")
            .validate(GenBlocksConfig::validate)
            .build();

    @Comment("Whether gen blocks work at all.")
    private boolean enabled = true;

    @Comment("""
            The gen blocks, by id (/genblock give <player> <id>, and the /shop "Base Building" items, whose
            ids are gen_<id>).
              name       — shown on the item, MiniMessage with the Brand colours
              material   — the block it lays (and the item it looks like)
              vertical   — true: straight down from where it's placed; false: the way the player faces
              cost       — money per block laid; the block's /shop price""")
    private Map<String, Type> types = defaultTypes();

    @Comment("Blocks each running gen lays per tick (20 ticks a second).")
    private int blocksPerTick = 1;

    @Comment("The most blocks one horizontal gen lays (vertical ones run to the ground, or the world's floor).")
    private int maxLength = 128;

    @Comment("Gens one player can have running at once.")
    private int maxRunning = 8;

    @Comment("Blocks laid between money withdrawals (each one is a database write).")
    private int chargeEvery = 16;

    private static Map<String, Type> defaultTypes() {
        var t = new LinkedHashMap<String, Type>();
        t.put("horizontal_sand", new Type("<sun>Horizontal Sand Gen</sun>", Material.SAND, false, 6));
        t.put("vertical_sand", new Type("<sun>Vertical Sand Gen</sun>", Material.SAND, true, 6));
        t.put("horizontal_cobblestone", new Type("<soft>Horizontal Cobblestone Gen</soft>", Material.COBBLESTONE, false, 1));
        t.put("vertical_cobblestone", new Type("<soft>Vertical Cobblestone Gen</soft>", Material.COBBLESTONE, true, 1));
        t.put("horizontal_obsidian", new Type("<gem>Horizontal Obsidian Gen</gem>", Material.OBSIDIAN, false, 32));
        t.put("vertical_obsidian", new Type("<gem>Vertical Obsidian Gen</gem>", Material.OBSIDIAN, true, 32));
        return t;
    }

    public boolean enabled() {
        return enabled;
    }

    public Map<String, Type> types() {
        return types;
    }

    public Type type(String id) {
        return id == null ? null : types.get(id.toLowerCase(Locale.ROOT));
    }

    public int blocksPerTick() {
        return blocksPerTick;
    }

    public int maxLength() {
        return maxLength;
    }

    public int maxRunning() {
        return maxRunning;
    }

    public int chargeEvery() {
        return chargeEvery;
    }

    static void validate(GenBlocksConfig cfg, ConfigErrors e) {
        e.check(cfg.blocksPerTick >= 1 && cfg.blocksPerTick <= 64, "blocks-per-tick", "must be 1-64");
        e.check(cfg.maxLength >= 1 && cfg.maxLength <= 1024, "max-length", "must be 1-1024");
        e.check(cfg.maxRunning >= 1, "max-running", "must be at least 1");
        e.check(cfg.chargeEvery >= 1, "charge-every", "must be at least 1");
        cfg.types.forEach((id, t) -> {
            e.check(t.material != null && t.material.isBlock(), "types." + id + ".material", "must be a block");
            e.check(t.cost >= 0, "types." + id + ".cost", "must be >= 0");
        });
    }

    /** One kind of gen block. */
    @ConfigSerializable
    public static final class Type {

        private String name = "<soft>Gen Block</soft>";
        private Material material = Material.COBBLESTONE;
        private boolean vertical = false;
        private long cost = 1;

        public Type() {
        }

        Type(String name, Material material, boolean vertical, long cost) {
            this.name = name;
            this.material = material;
            this.vertical = vertical;
            this.cost = cost;
        }

        public String name() {
            return name;
        }

        public Material material() {
            return material;
        }

        public boolean vertical() {
            return vertical;
        }

        public long cost() {
            return cost;
        }
    }
}
