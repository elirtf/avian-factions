package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;

import club.avian.factions.api.economy.SellValues;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

/**
 * The sell table from {@code economy.conf}, parsed once into an {@link EnumMap} and rebuilt on
 * reload. Prices are whole dollars throughout — config, memory and database agree.
 */
public final class ConfiguredSellValues implements SellValues {

    private final ConfigHandle<EconomyConfig> config;
    private volatile Map<Material, Long> prices;

    public ConfiguredSellValues(ConfigHandle<EconomyConfig> config) {
        this.config = config;
        this.prices = parse();
        config.onReload(cfg -> prices = parse());
    }

    @Override
    public long unitPrice(Material material) {
        return prices.getOrDefault(material, 0L);
    }

    public int size() {
        return prices.size();
    }

    private Map<Material, Long> parse() {
        Map<Material, Long> parsed = new EnumMap<>(Material.class);
        for (var entry : config.get().sellValues().entrySet()) {
            var material = Material.matchMaterial(entry.getKey());
            if (material == null) {
                continue;   // startup validation already reported it; skip rather than crash here
            }
            parsed.put(material, entry.getValue());
        }
        return parsed;
    }
}
