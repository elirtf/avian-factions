# avian-netherite (world data pack)

Triples ancient debris: vanilla places one large vein (y 8–24) and one small vein (anywhere) per
Nether chunk; this places three of each (`"count": 3` in both placed features). Everything else is
vanilla, including "debris never generates touching air". Only chunks generated **after** the pack is
enabled get it, so the Nether explored before it keeps vanilla rates (a new season's map gets it
everywhere). To tune: change both `count` values and restart.

Copied into `run/world/datapacks/` by `./dev start` like the rest of `dev-server/`; new packs there
are enabled automatically when the world loads. Built for data pack format 101 (26.1.2).
