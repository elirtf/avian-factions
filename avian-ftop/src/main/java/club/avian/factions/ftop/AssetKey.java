package club.avian.factions.ftop;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.UUID;

/** A block position. Worlds by UUID, so renaming a world folder does not orphan its assets. */
public record AssetKey(UUID world, int x, int y, int z) {

    public static AssetKey of(Block block) {
        return new AssetKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static AssetKey of(Location location) {
        return new AssetKey(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
    }

    public AssetKey offset(int dx, int dy, int dz) {
        return new AssetKey(world, x + dx, y + dy, z + dz);
    }
}
