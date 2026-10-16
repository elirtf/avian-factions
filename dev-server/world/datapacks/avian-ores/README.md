# avian-ores (world data pack)

Overworld ores at **1.5× vanilla** (owner, 2026-10-16: "a little higher than vanilla", so mining
pays as a way to earn money and levels AuraSkills' Mining faster). Each file is vanilla 26.1.2's
placed feature with only its first placement changed: `count` ×1.5, or for the rare large diamond
vein `rarity_filter` 1 in 9 → 1 in 6. Vein sizes, heights and the biomes they appear in are
untouched.

| Ore | Vanilla → Avian (attempts per chunk) |
|---|---|
| Coal | 20 + 30 → 30 + 45 |
| Copper | 16 + 16 → 24 + 24 |
| Iron | 10 + 10 + 90 (mountains) → 15 + 15 + 135 |
| Gold | 4 + 50 (badlands) → 6 + 75; the deep 0–1 extra vein is unchanged |
| Redstone | 4 + 8 → 6 + 12 |
| Lapis | 2 + 4 → 3 + 6 |
| Diamond | 7 + 4 + 2, large 1 in 9 → 10 + 6 + 3, large 1 in 6 |
| Emerald (mountains) | 100 → 150 |

Nether ores are left alone, except ancient debris (`avian-netherite`, 3×). Only chunks generated
**after** the pack is enabled get it. The fresh resource world gets it everywhere; already-explored
land keeps vanilla rates. Tectonic's own ore files are only used by its mod version, not the
datapack we run, so these apply. To tune: edit a `count` (or `chance`) and restart.

**Measured** (2026-10-16, Paper 26.1.2 + Tectonic, seed 4242, 64 chunks, the same seed with and
without the pack): diamond 2,382 vs 1,591 (1.50×), iron 7,324 vs 4,855 (1.51×), coal 9,404 vs 6,075 (1.55×).

Copied into `run/world/datapacks/` by `./dev start` like the rest of `dev-server/`. Built for data
pack format 101 (26.1.2).
