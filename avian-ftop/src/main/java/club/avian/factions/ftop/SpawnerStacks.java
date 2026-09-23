package club.avian.factions.ftop;

import org.bukkit.block.Block;

/** How many spawners are stacked in a spawner block. RoseStacker in production, a fake in tests. */
@FunctionalInterface
public interface SpawnerStacks {

    /** Stack size of the spawner block; 1 for a spawner RoseStacker has not stacked. */
    int size(Block spawner);
}
