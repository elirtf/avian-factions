package club.avian.factions.ftop;

import dev.kitteh.factions.Board;
import dev.kitteh.factions.FLocation;
import org.bukkit.Bukkit;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/** Which player faction's claim a position is in, from FactionsUUID. Main thread. */
final class ClaimOwners implements Function<AssetKey, Ranking.@Nullable Owner> {

    @Override
    public Ranking.@Nullable Owner apply(AssetKey key) {
        var world = Bukkit.getWorld(key.world());
        if (world == null) {
            return null;
        }
        var faction = Board.board().factionAt(new FLocation(world.getName(), key.x() >> 4, key.z() >> 4));
        // Wilderness, safezone and warzone are not "normal" factions and own nothing.
        return faction.isNormal() ? new Ranking.Owner(faction.id(), faction.tag()) : null;
    }
}
