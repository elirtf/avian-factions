package club.avian.factions.api.faction;

/**
 * What a player is trying to do in a Territory. Listeners translate ~60 Bukkit events onto these
 * few kinds so the decision lives in one place (#4: the surveyed plugins' protection regressions
 * were all missing branches in per-event if-chains).
 */
public enum Interaction {
    /** Place or break a block, including pistons moving blocks and fluids flowing in. */
    BUILD,
    /** Open something that holds items: chests, barrels, furnaces, hoppers, shulkers. */
    CONTAINER,
    /** Doors, trapdoors, fence gates. */
    DOOR,
    /** Buttons, levers, pressure plates, note blocks — state changes that hold nothing. */
    SWITCH,
    /** Item frames, armour stands, paintings, leads, vehicles: entities used as decoration. */
    ENTITY,
    /** Buckets, flint and steel, bone meal, spawn eggs, end crystals — items used on the world. */
    ITEM_USE,
    /** Damaging a non-player entity: animals, villagers, golems, pets. */
    DAMAGE_ENTITY
}
