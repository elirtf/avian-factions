package club.avian.factions.ftop;

import java.util.Map;

/** What an asset is worth at a moment, and what picking some of it up costs. Pure. */
public final class Valuation {

    /** Cost of a pickup: how many units are charged, and the total in whole dollars. */
    public record Cost(int chargedUnits, long amount) {
        public static final Cost FREE = new Cost(0, 0);
    }

    private final Map<String, Long> spawnerValues;
    private final Map<String, Long> blockValues;
    private final FTopConfig.Aging aging;
    private final FTopConfig.PickupCost pickup;

    public Valuation(FTopConfig config) {
        this.spawnerValues = config.normalised(config.spawnerValues());
        this.blockValues = config.normalised(config.blockValues());
        this.aging = config.aging();
        this.pickup = config.pickupCost();
    }

    /** Full value of one unit; 0 for anything not in the tables. */
    public long unitValue(AssetKind kind, String type) {
        var table = kind == AssetKind.SPAWNER ? spawnerValues : blockValues;
        return table.getOrDefault(type, 0L);
    }

    /** True when a block material is valuable enough to track. */
    public boolean isValuableBlock(String material) {
        return blockValues.containsKey(material);
    }

    /** Share of full value an asset placed at {@code placedAt} is worth at {@code now}, 0 to 1. */
    public double ageFactor(long placedAt, long now) {
        if (!aging.enabled()) {
            return 1.0;
        }
        double start = aging.startingPercent() / 100.0;
        double full = aging.hoursToFullValue() * 3_600_000.0;
        double elapsed = Math.max(0, now - placedAt);
        return Math.min(1.0, start + (1.0 - start) * elapsed / full);
    }

    /** Current value of the whole stack. */
    public long value(Asset asset, long now) {
        return (long) Math.floor(asset.count() * (double) unitValue(asset.kind(), asset.type())
                * ageFactor(asset.placedAt(), now));
    }

    /** Cost of picking up {@code units} of {@code asset} by hand at {@code now}. */
    public Cost pickupCost(Asset asset, int units, long now) {
        if (!pickup.enabled() || units <= 0 || (asset.kind() == AssetKind.BLOCK && !pickup.appliesToBlocks())) {
            return Cost.FREE;
        }
        int charged = Math.max(0, Math.min(units, asset.count()) - asset.freeUnits(now));
        if (charged == 0) {
            return Cost.FREE;
        }
        double perUnit = unitValue(asset.kind(), asset.type()) * ageFactor(asset.placedAt(), now)
                * pickup.percentOfValue() / 100.0;
        return new Cost(charged, (long) Math.ceil(charged * perUnit));
    }
}
