# Custom terrain and wild bosses

Owner, 2026-10-01: custom terrain for the resource world, the Nether and the End, "more abundant
for exploration / RPG-like aspects", and wild mobs that count as bosses. Find it or build it?

**Answer: find it.** World generation and boss mobs are big, mature areas with maintained,
26.1.2-ready projects; building either ourselves would take months for a worse result. Checked on
Modrinth (licence, supported versions, last update) on 2026-10-01; nothing below is boot-tested yet.

## How terrain changes reach our worlds

World generation comes from **datapacks** (the modern way; Paper loads them from the main world's
`datapacks/` folder). They change the overworld, Nether or End *generator*, so:

- They only affect **newly generated chunks**. The resource world, Nether and End get fresh worlds
  at the migration anyway (owner: "worlds will change"), so that's the moment to add them.
- An overworld pack changes every world that uses the normal overworld generator. The planned
  claiming world is **flat** (terracotta), which isn't affected, so only the resource world changes.
- Players need nothing: datapacks are server-side (unlike mods).

## Terrain candidates

| Pack | What | Licence | 26.1.2 | Notes |
|---|---|---|---|---|
| **Tectonic** | Overworld terrain shaping: higher mountains, deeper valleys, varied landforms | MIT | yes | Open licence, actively updated (2026-09-27). Shape only, vanilla biomes. |
| **Trek** | Overworld structures (more to find while exploring) | MIT | yes | |
| Hopo's Better Mineshafts / Ruined Portals / Underwater Ruins | Vanilla structures, richer | LGPL-3.0 | yes | Small, focused. |
| **Stellarity** | Full End overhaul: biomes, structures, items, weapons, bosses | Custom: for-profit servers allowed, no redistribution | yes | Updated 2026-09-23. The End answer. |
| Terralith (overworld biomes), Incendium (Nether), Nullscape (End), Amplified Nether, Structory | Stardust Labs: the most popular of all | Stardust Labs License | yes | See below. |
| Dungeons and Taverns, Explorify, Geophilic | Structures, overworld | All rights reserved | yes | Would need each author's server terms checked. |
| Towns and Towers | Villages, outposts | CC-BY-NC-SA | yes | Non-commercial only: not with paid ranks. |
| Terra (plugin) | Configurable generator | MIT | **no** (stops at 1.21.8) | Out. |

**Stardust Labs** (Terralith, Incendium, Nullscape) is the best-known set and **permits use on public
and for-profit servers**. Its licence also says their work "may not be used to configure, test, debug,
or augment any artificial intelligence … system" and bans its use "as part of any AI project". To
respect that, Claude doesn't download, configure or test these packs; the owner can add them by hand
(their licence allows it on our server), and nothing else here depends on them.

**The Nether** is the gap among openly licensed packs: Incendium (Stardust) is the standout. Without
it, the Nether gets vanilla terrain plus structure packs and the bosses below.

## Boss and RPG-mob candidates (plugins)

| Plugin | What | Licence | 26.1.2 | Notes |
|---|---|---|---|---|
| **EliteMobs** | Elite mobs spawn in the wild with levels that grow with distance, world bosses, custom loot, arenas, minidungeons | GPL-3.0 | yes | Updated 2026-09-29. Large: it also brings its own currency, quests and gear, which overlap our economy, AuraSkills and enchants; those parts would be switched off. |
| **LevelledMobs** | Every mob gets a level from distance, biome or time: more health, damage and drops | GPL-2.0 | yes | Small and focused; the "RPG feel" without bosses. |
| MythicMobs | Write any custom mob or boss with skills | Free, closed source | yes | The standard for hand-made bosses; needs authoring time. |
| Stellarity | Its own End bosses | (above) | yes | Comes with the End pack. |

## Recommendation

1. **Terrain:** Tectonic (resource world) + Trek and Hopo's packs (structures) + Stellarity (End),
   all openly licensed. The owner adds Terralith / Incendium / Nullscape by hand if wanted (Incendium
   is the strongest Nether option).
2. **Mobs:** LevelledMobs for distance-based difficulty everywhere in the wild, plus **EliteMobs** for
   the bosses, with its economy, quests and gear switched off so it doesn't compete with ours; or
   MythicMobs later for bosses made just for Avian.
3. **Boot-test first** (as with every plugin): each on Paper 26.1.2 in a scratch server, with
   our stack, then a fresh resource world, Nether and End to fly around in before deciding.

Everything gameplay-relevant a boss drops still follows ADR-0006 (obtainable in game) and #40
(rare PvP consumables stay rare: boss loot tables must not hand out golden apples freely).

## Boot test (2026-10-01)

On this dev box (4 cores, shared with the live server), the owner chose to test all of the
recommended set: Tectonic 3.0.25, Trek B0.6.2, Hopo's Better Mineshafts 1.3.6 / Ruined Portals 1.5.0
/ Underwater Ruins 1.2.7, Stellarity 6.0.0 (datapacks), EliteMobs 10.9.7, LevelledMobs 4.5.3.2. All
downloaded from Modrinth with their SHA-512 checked.

- **Everything loads on Paper 26.1.2 with our whole stack**: all six datapacks enable, both plugins
  enable, the server log check is clean. EliteMobs notices LevelledMobs and switches to its own
  "high compatibility mode". EliteMobs registers its currency with nightcore (our enchants'
  library): its economy is one of the parts to switch off.
- **Generation cost**, 441 overworld chunks on plain Paper + Chunky:

  | Packs | Time | vs vanilla |
  |---|---|---|
  | none (vanilla) | 41 s | 1× |
  | Stellarity (End only) | 35 s | no overworld cost |
  | Trek | 66 s | 1.6× |
  | Tectonic | 103 s | 2.5× |
  | all six | 330 s (234 s with CraftEngine) | 6–8× |

  In the full stack, once warm: 3.5–4 chunks a second (vanilla about 11).
- **The first boot of a fresh world froze the server** (Paper's watchdog stopped it after 60 s):
  BetterRTP builds its queue of `/rtp` spots by loading chunks on the main thread, and a fresh chunk
  under these packs takes long enough to stall it. Without BetterRTP, and once the spawn area
  existed, Chunky generated steadily.

**What that means for launch:**

1. **Pre-generate every new world before players arrive**, with Chunky, and only then enable
   `/rtp`. On this box a 5,000-block-wide resource world (~98,000 chunks) would take about 7–8 hours;
   the big machines have more cores and nothing else to do while they generate.
2. **Size the resource world for that**: a full 10,000-wide world is ~390,000 chunks, a day or more of
   generation. The resource world can be smaller than the claiming world, and it gets reset anyway.
3. Stellarity's items need its resource pack (Stellarity RP) merged into ours (CraftEngine), or they
   show as plain base items.
4. **Our enchantments only** (owner, 2026-10-01, after a Stellarity book showed up broken in a chest:
   "keep our stuff"). Stellarity adds 8 enchantments; `tools/datapacks/stellarity-our-enchants-only.js`
   takes them out of the enchantment tags and every random loot roll (8 tag entries, 16 rolls), while
   its own items keep their mechanics. Applied on the test server with a datapack reload; at launch the
   build fetches the pinned Stellarity and runs it, rather than storing a copy in git (its licence
   forbids redistribution).
