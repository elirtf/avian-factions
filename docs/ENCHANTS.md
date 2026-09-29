# Custom enchants

Custom enchants come from **ExcellentEnchants** 5.4.3 (GPL-3.0, running on the nightcore library):
81 vanilla-style enchants, of which we run 58. Players add them to gear with books in an anvil, up
to 5 custom enchants per item. `/eenchants list` shows every enchant and what it does, and each
book's tooltip carries its description (drawn through packetevents). Descriptions say "Chance to…"
and never show the percentage (owner, 2026-09-29), so the numbers can be tuned freely.

## Where books come from

- **The Enchanter, `/enchanter`:** XP levels buy a random book from a tier (owner, 2026-09-29: books
  cost XP, not tokens). The button checks the levels, takes them, then rolls that tier's hidden book
  crate (CrazyCrates `forceopen`), so the player sees the spin and gets the book. Right-click a tier
  to see every prize and its odds.
- **Enchanting tables, villager trades, mob gear and loot**, the vanilla way. Legendary and Mythic
  enchants are *treasure*: never from a table, only loot, fishing and trades. That's their free
  in-game route (ADR-0006).

| Tier | XP levels | Enchants | What's in it |
|---|---|---|---|
| **Common** | 10 | 20 | Utility and grinding |
| **Rare** | 20 | 22 | Combat effects, arrows, better tools |
| **Legendary** | 30 | 12 | The strongest enchants |
| **Mythic** | 40 | 4 | The rarest: Auto Reel, Ice Aspect, Ice Shield, Stopping Force |

A tier's roll holds one prize per enchant level, and a higher level is rarer: level I has weight
= max level, down to 1 for the top level. Colours follow the crates: Common grey `#C9C9C9`, Rare
blue `#4DA3FF`, Legendary orange `#FFB84D`, Mythic red `#FF4F7A`, so a book's tier shows in its name.

## The tiers

**Common:** Bane of Netherspawn, Cure, Double Catch, Glass Breaker, Haste, Jumping, Lightweight,
Lingering, Lucky Miner, Night Vision, Replanter, River Master, Saturation, Seasoned Angler, Smelter,
Sniper, Survivalist, Village Defender, Water Breathing, Wisdom.

**Rare:** Blindness, Cold Steel, Confusing Arrows, Confusion, Darkness Arrows, Decapitator,
Electrified Arrows, Elemental Protection, Exhaust, Hardened, Infernus, Poisoned Arrows, Rage,
Restore, Speed, Swiper, Treefeller, Vampiric Arrows, Veinminer, Venom, Wither, Withered Arrows.

**Legendary:** Darkness Cloak, Double Strike, Dragon Heart, Dragonfire Arrows, Flame Walker,
Paralyze, Rebound, Regrowth, Temper, Thunder, Tunnel, Vampire.

**Mythic:** Auto Reel, Ice Aspect, Ice Shield, Stopping Force.

**Tuning:** Dragon Heart max level 2 (from 5: permanent extra health), Speed 1 (from 2: permanent
Speed II out-runs everything), Stopping Force triggers 25 % + 10 % a level (it was every hit).

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
| All curses (Breaking, Death, Drowned, Fragility, Mediocrity, Misfortune) | Owner, 2026-09-29 |
| Cutter, Fire Shield, Flare, Hover, Nimble, Telekinesis | Owner, 2026-09-29 |

## Skill XP (AuraSkills)

Gathering enchants feed the RPG side:

- **Veinminer, Tunnel, Treefeller** break each extra block as the player (`Player#breakBlock`), so
  AuraSkills counts every one, and claims and WorldGuard regions are respected. Checked on a
  scratch server: 5 ores with Veinminer gave 5× the mining XP of one ore, and a 5-log tree with
  Treefeller 5× the foraging XP of one log.
- **Double Catch:** AuraSkills pays once per catch, so a doubled fish earns the catch's fishing XP
  again (`FishingEnchantXp`). **Seasoned Angler** adds 10 % fishing XP a level. Both in
  `economy.conf` (`double-catch-skill-xp`, `seasoned-angler-skill-xp-per-level`).

## Classes, later

Classes aren't built yet (docs/research/hcf-patterns.md). When they are, these groups line up with
them, so a class can favour its own enchants:

| Class | Its enchants |
|---|---|
| **Bard** (team support) | Regrowth, Saturation, Speed, Dragon Heart, Hardened |
| **Archer** | Every arrow enchant, Sniper, Dragonfire Arrows |
| **Rogue** (burst melee) | Venom, Blindness, Confusion, Wither, Vampire, Double Strike, Temper |
| **Miner** | Haste, Smelter, Veinminer, Tunnel, Lucky Miner |

## Changing it

The tables live in **`tools/enchant-tiers.mjs`**, which writes every enchant file, the fixed
`item_types.yml` (the plugin ships the typo "Brekable") and the four book crates:

```sh
node tools/enchant-tiers.mjs <plugins/ExcellentEnchants/enchants of a server with default config>
```

Get the defaults by booting ExcellentEnchants once on a scratch server with none of our config.
Commit what it writes, then restart: enchant definitions only load at startup. XP prices are in
`dev-server/plugins/DeluxeMenus/gui_menus/enchanter.yml`. The generator stops if a description still
shows a chance percentage, so a new enchant gets a "Chance to…" rewrite.

- **Disabling** moves an enchant's file into `enchants/_disabled_/`. Our config sync copies files
  but never deletes them, so on a server that already ran with the enchant enabled, also delete
  `run/plugins/ExcellentEnchants/enchants/<name>.yml` (or the container's `/data/...`).
- **Book commands** need the namespaced id: `eenchants book excellentenchants:venom 2 <player>`.
  A bare `venom` is "not a valid enchantment".
