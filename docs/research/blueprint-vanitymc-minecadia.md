# Blueprint: VanityMC and Minecadia, and the open-source plugins to build them with

The owner's blueprint is the old **VanityMC** factions realms (Demonic and Heroic, 2020–21). The
modern reference is **Minecadia** (Factions: Pirate, then Galaxy), except its **masks**, which the
owner does not want.

This note does three things:
- lists what those servers actually ran;
- maps each mechanic to where Avian stands;
- names the **open-source** plugin to fork or configure for each one, so we build only what is
  truly ours (CLAUDE.md).

**Sources.**
- VanityMC's site is offline, so its posts are read from the Internet Archive: [Demonic Season 35 release](http://web.archive.org/web/20210127024922/https://vanitymc.co/threads/vanitymc-factions-demonic-season-35.17136/) (Jan 2021) and [Factions custom enchant list](http://web.archive.org/web/20210511075222/https://vanitymc.co/threads/factions-custom-enchant-list.16323/) (Jul 2020).
- Minecadia: [Phase III: Factions Galaxy](https://www.minecadia.com/blog/factions-galaxy/) (Jun 2023, archived copy), plus [its wiki](https://wiki.minecadia.com/index.php?title=Custom_Enchants) (blocks automated reads; titles and search snippets only).
- Plugin facts come from each project's GitHub and Modrinth pages, checked 2026-09-24.

## 1. What VanityMC ran (the blueprint)

| Mechanic | How it worked | Avian today |
|---|---|---|
| **Harvester hoe** | Every player spawned with one. "Back to simpler times … without super unique custom enchantments", **explicitly to stop factions making billions from cane**. Three upgrades, 1–5 each: **Cultivation** (more mcMMO herbalism XP), **Essence** (more tokens from cane), **Possess** (more drops). | **Built** (#51): radius, token boost, money multiplier, random drops. Weekly kit (#53) plus $25k in `/hoe`. **Gap:** no skill-XP upgrade, and hoe harvests give no AuraSkills XP at all (log review, fix 3). |
| **Tokens from cane → token shop** | Tokens came from harvesting cane. The token shop sold keys, rank keys, kits and books. | Cane tokens **built**. **No token shop yet**: it needs console commands to take tokens (#26). |
| **Custom enchants** | Three tiers, **Common → Rare → Legendary**, with max levels 1–3. Mostly **passive effects per armour piece** (Owl = night vision, Saturation, Fire Resistance, Speed, Strength, Jelly Legs = no fall damage) plus utility (Haste, Magnet, Obsidian Breaker) and grind enchants (**Token Hunter**: more tokens per mob; **Overkill**: kills several mobs of a stack per hit). An **Alchemist** combined low-level books into higher ones. | None. |
| **Castle / outposts** | Holding the Castle paid money per online member every 15 min, plus Health Boost, dungeon access and a **25 % sugar cane sell booster**. The Raiding Outpost (a cannon target) gave 2× TNT from creeper spawners and hourly money. | The Aerie (#21) is planned as the sell-buff outpost: **matches**. |
| **KOTH and bosses** | A KOTH top leaderboard with weekly rewards. Darkzone bosses every few hours, with rewards by damage dealt. | Events (#24) not started. |
| **Gkits** | Themed kits on cooldowns (Raider, Builder, Warrior, Bard, Token), from the store **and** in game. | Weekly Harvester kit only. ADR-0006: any sold kit needs an in-game route. |
| **Faction missions** | A daily random faction challenge paying 2,500 tokens. | None. |
| **Raid rules** | Raid claims, a 45-minute raid timer, a paid daily "force-field" window. Two-week maps: 6 days grace, 8 days raiding. | CommandTimer raid windows. FactionsUUID has shields (`/f shield`): check before building a force-field. |
| **Payouts and leaderboards** | Weekly payouts to top F-Top, top herbalism and top KOTH. | F-Top built. Leaderboards: see §3. |

## 2. What Minecadia adds (the modern layer)

- **Tiered progression across skills.** Levels 1–19, 20–39 and so on unlock better resource and
  loot tiers, and **every skill is balanced to take equal time**. There are daily global level
  caps at season start (10, 20, 30, …) so no one runs away on day one. **AuraSkills can do most of
  this** (per-skill level rewards, loot tables).
- **Warzone bosses as buffs**, like the dragons in League of Legends. Every 3 hours a random boss
  spawns. The last-hitting faction gets a one-hour buff (+2 hearts, +10 % damage, −10 % damage
  taken, regeneration) and the top three damagers get loot. This is a strong, cheap-to-run pattern.
- **Planets and regions with different rules**: drop rates of 100 / 80 / 60 %, member limits and
  border sizes. There's also a **vanilla competitive End** where enchants, masks and pets are
  disabled. Useful later for PvP balance.
- **Vaulting content every season.** Features and enchants are removed or rotated to keep maps
  fresh. This fits our seasons plan.
- **Custom enchants.**
  - Enchants are called "runes", in tiers.
  - Up to 8 per item with Slot Crystals, and 10 with Armor Orbs.
  - An **XP shop** is a core economy sink: keys, consumables and enchants are bought with XP.
- **Masks: skip** (owner's call). Minecadia's pets and "partner items" are also heavy
  custom content, so skip them for v1.

## 3. Open-source plugins that fit, and what to change

All of these support **26.1.2** (a Modrinth build exists) and have public source under a
permissive or GPL licence, unless noted otherwise. "Fork" means we build it from pinned
source with our edits, as we do for FactionsUUID.

| Need | Plugin | Licence, activity | Fit, and the edit we'd make |
|---|---|---|---|
| **Sell wand** (sell a chest's contents: the auto-farm gap from the log review) | [AxSellwands](https://github.com/Artillex-Studios/AxSellwands) 1.18.0 | MIT; pushed 2026-09-20; 19k downloads | Reads **EconomyShopGUI** prices, pays through **Vault** and understands **RoseStacker**, all out of the box. **Edit:** its protection hooks cover WorldGuard but **not FactionsUUID**, so add a FactionsUUID hook (only sell containers the player may open). A small, self-contained fork. **Adopt.** |
| **Custom enchants** | [ExcellentEnchants](https://github.com/nulli0n/ExcellentEnchants-spigot) 5.4.3 (needs [nightcore](https://github.com/nulli0n/nightcore-spigot) ≥ 2.15.2) | GPL-3.0; 56k downloads; free builds | Over 80 vanilla-like, configurable enchants, obtained from tables, villagers, loot and fishing, with anvil support and PlaceholderAPI. **No tiers**, so VanityMC's Common/Rare/Legendary would be *our* grouping: a token **Enchanter** menu (DeluxeMenus) selling random books by tier. Needs #26 (console token take/give) first. Disable enchants that don't suit factions PvP. **Adopt, via config rather than a fork.** |
| Custom enchants (alternative) | [EcoEnchants](https://github.com/Auxilor/EcoEnchants) | GPL-3.0; 349 stars; very active | 250+ enchants, closer to Cosmic/Vanity style, but needs the eco and libreforge frameworks, and prebuilt jars are paid (source is free to build). Heavier to maintain. **Keep as plan B.** |
| **Auction house** (market plus a fee sink, from the log review) | [CrazyAuctions](https://github.com/Crazy-Crew/CrazyAuctions) 26.1.2 build | MIT; pushed 2026-09-23; 138k downloads | Same team as CrazyCrates, which we already run. **Edit (config):** listing fee and tax as a money sink. **Adopt.** |
| **Envoys** (supply drops, spec §32) | [CrazyEnvoys](https://github.com/Crazy-Crew/CrazyEnvoys) 1.15.0 | MIT; pushed 2026-09-23 | Also Crazy-Crew. **Adopt with events (#24).** |
| **KOTH** | [VelKoth](https://github.com/Velmax-Studios/VelKoth) 1.0.9 | MIT; 0 stars, single author | Small and young. Evaluate against CaptureZones, or build KOTH into the Aerie code (#21), which needs capture logic anyway. **Evaluate.** |
| **Bosses** (warzone-boss buffs) | [EliteMobs](https://github.com/MagmaGuy/EliteMobs) 10.9.5 | GPL-3.0; 76k downloads; active | Custom bosses, loot, arenas and dungeons. Big; we'd use its custom-boss part and give the buff ourselves. Note that MythicMobs is **not** open source. **Evaluate with #24.** |
| **Scoreboard and tab** | [TAB](https://github.com/NEZNAMY/TAB) 6.2.0 | Apache-2.0; 1.1k stars; 1.2M downloads | Sidebar with balance, tokens and power through `%avian_*%`. Works with Carbon and Velocity. **Adopt.** |
| **Leaderboards and holograms** (payout boards: F-Top, herbalism, KOTH) | [ajLeaderboards](https://github.com/ajgeiss0702/ajLeaderboards) 2.11.0, [FancyHolograms](https://github.com/FancyInnovations/FancyPlugins) | GPL-3.0 / MIT; active | Boards from any PlaceholderAPI value, shown as holograms at spawn. **Adopt when spawn is dressed.** |
| Quests and faction missions | [BetonQuest](https://github.com/BetonQuest/BetonQuest) (26.1.2 dev builds) | GPL-3.0; active | Powerful but heavy, and it isn't faction-aware. VanityMC-style *faction* missions are small enough to build (they are Avian-specific). **Build later.** |
| Harvester hoe | [AxHoes](https://docs.artillex-studios.com/) | **Closed source, paid** | Rejected. Ours stays (CLAUDE.md lists the hoe as Avian-built). |
| Player vaults (#30) | [PlayerVaults](https://github.com/drtshock/PlayerVaults) | GPL-3.0; last push 2024-12 | Modrinth's PlayerVaultsX listing is "All Rights Reserved"; build from the GPL source as #30 already plans. |

## 4. Recommended order

1. **Console token commands (#26).** A token shop, the enchanter, crate rewards and faction
   missions all need a way to take and give tokens from menus and console. This is small, and it
   unblocks everything below.
2. **Hoe parity with VanityMC.**
   - Grant AuraSkills farming XP on hoe harvests.
   - Add a **Cultivation** upgrade (skill-XP multiplier). This mirrors Vanity and ties farming into
     the RPG side.
3. **AxSellwands, forked with a FactionsUUID hook.** This closes the auto-farm selling gap.
4. **Token shop and Enchanter** (DeluxeMenus + ExcellentEnchants): Vanity's tier feel, built from
   config.
5. **TAB sidebar**: balance, tokens, power.
6. **CrazyAuctions** with a listing fee and tax (a money sink).
7. Events (#24): **CrazyEnvoys**, KOTH (VelKoth or our own), warzone bosses (EliteMobs plus our buff).

Each adoption follows the usual path: pin by hash, boot-test on the dev server, read the code
before forking, and track the config in `dev-server/`.
