# Custom enchants

Custom enchants come from **ExcellentEnchants** 5.4.3 (GPL-3.0, running on the nightcore library):
81 vanilla-style enchants, of which we run 69. Players add them to gear with books in an anvil, up
to 5 custom enchants per item. `/eenchants list` shows every enchant and what it does, and each
book's tooltip carries its description (drawn through packetevents).

## Where books come from

- **The Enchanter, `/enchanter`:** XP levels buy a random book from a tier (owner, 2026-09-29: books
  cost XP, not tokens). The button checks the levels, takes them, then rolls that tier's hidden book
  crate (CrazyCrates `forceopen`), so the player sees the spin and gets the book. Right-click a tier to
  see every prize and its odds.
- **Enchanting tables, villager trades, mob gear and loot**, the vanilla way. Legendary enchants
  are *treasure*: never from a table, only loot, fishing and trades. That's their free in-game route
  (ADR-0006).

| Tier | XP levels | Enchants | What's in it |
|---|---|---|---|
| **Common** | 10 | 23 | Utility and grinding |
| **Rare** | 20 | 28 | Combat effects, arrows, better tools |
| **Legendary** | 30 | 13 | The strongest enchants |

A tier's roll holds one prize per enchant level, and a higher level is rarer: level I has weight
= max level, down to 1 for the top level. Colours follow the crates: Common grey `#C9C9C9`, Rare
blue `#4DA3FF`, Legendary orange `#FFB84D`, so a book's tier shows in its name.

## The tiers

**Common:** Auto Reel, Bane of Netherspawn, Cure, Double Catch, Flare, Glass Breaker, Haste,
Hover, Jumping, Lightweight, Lingering, Lucky Miner, Night Vision, Replanter, River Master,
Saturation, Seasoned Angler, Smelter, Sniper, Survivalist, Village Defender, Water Breathing,
Wisdom.

**Rare:** Blindness, Cold Steel, Confusing Arrows, Confusion, Darkness Arrows, Decapitator,
Electrified Arrows, Elemental Protection, Exhaust, Fire Shield, Hardened, Ice Aspect, Ice Shield,
Infernus, Nimble, Poisoned Arrows, Rage, Restore, Speed, Stopping Force, Swiper, Telekinesis,
Treefeller, Vampiric Arrows, Veinminer, Venom, Wither, Withered Arrows.

**Legendary:** Cutter, Darkness Cloak, Double Strike, Dragon Heart, Dragonfire Arrows, Flame
Walker, Paralyze, Rebound, Regrowth, Temper, Thunder, Tunnel, Vampire.

**Level caps:** Dragon Heart 2 (from 5: it grants permanent extra health), Speed 1 (from 2:
permanent Speed II boots out-run everything).

**Curses** (Breaking, Drowned, Fragility, Mediocrity, Misfortune) stay as the plugin ships them:
loot-only downsides, never sold.

## Disabled, and why

| Enchant | Why it's off |
|---|---|
| Silk Spawner | Mines spawners, skipping the `/shop` spawner economy |
| Thrifty | Drops spawn eggs, which change a spawner's type |
| Silk Chest | Picks up whole chests with their contents: a raid shortcut |
| Ender Bow | Shoots ender pearls, skipping the 16 s pearl cooldown |
| Bomber, Ghast, Explosive Arrows | Free TNT, fireballs and explosions: raiding without paying |
| Kamikadze, Blast Mining | Explosions on death or when mining: griefing |
| Rocket | Launches players into the sky: fall-damage kills |
| Soulbound | Keeps items on death: PvP loot has to drop |
| Curse of Death | Kills the killer: no place in factions PvP |

## Classes, later

Classes aren't built yet (docs/research/hcf-patterns.md). When they are, these groups line up with
them, so a class can favour its own enchants:

| Class | Its enchants |
|---|---|
| **Bard** (team support) | Regrowth, Saturation, Speed, Dragon Heart, Hardened |
| **Archer** | Every arrow enchant, Sniper, Hover, Dragonfire Arrows |
| **Rogue** (burst melee) | Venom, Blindness, Confusion, Wither, Vampire, Double Strike, Cutter, Temper |
| **Miner** | Haste, Smelter, Veinminer, Tunnel, Telekinesis, Lucky Miner |

## Changing it

The tables live in **`tools/enchant-tiers.mjs`**, which writes every enchant file and the three
book crates:

```sh
node tools/enchant-tiers.mjs <plugins/ExcellentEnchants/enchants of a server with default config>
```

Get the defaults by booting ExcellentEnchants once on a scratch server with none of our config.
Commit what it writes, then restart: enchant definitions only load at startup. Prices are in
`dev-server/plugins/DeluxeMenus/gui_menus/enchanter.yml`.

- **Disabling** moves an enchant's file into `enchants/_disabled_/`. Our config sync copies files
  but never deletes them, so on a server that already ran with the enchant enabled, also delete
  `run/plugins/ExcellentEnchants/enchants/<name>.yml` (or the container's `/data/...`).
- **Book commands** need the namespaced id: `eenchants book excellentenchants:venom 2 <player>`.
  A bare `venom` is "not a valid enchantment".
