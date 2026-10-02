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
