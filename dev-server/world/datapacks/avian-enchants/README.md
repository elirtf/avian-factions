# avian-enchants (world data pack)

Our own enchantments, as Minecraft's data-driven enchantments: real enchantments that work on books, in anvils,
enchanting tables and trades, counted by the 6-enchant cap like any other.

- **Coin Collector I–V** (`avian:coin_collector`; owner, 2026-10-16): on a melee weapon, every mob coin drop from
  a kill pays **20% more a level** (V = double). The effect is AvianFactions' (`CoinCollector`, through
  UltimateMobCoins' `MobCoinDropEvent`); this file only defines the enchantment. It comes from enchanting tables,
  villager trades and random loot (the tags here), and the /mobcoins shop sells I, III and V books.

New enchantments are read when the server starts, so a change here needs a restart. Copied into
`run/world/datapacks/` by `./dev start` like the rest of `dev-server/`.
