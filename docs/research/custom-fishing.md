# Custom fishing: EvenMoreFish, next to AuraSkills

Written 2026-09-30. The owner asked for "custom fishing as another way of rewarding players". This
note covers which plugin does it, what it does to the economy, and a boot test on our Paper build.

## Choice: EvenMoreFish 2.5.0, with AuraSkills kept as the treasure layer

| | EvenMoreFish 2.5.0 | CustomFishing 2.3.28 | AuraSkills 2.4.0 (already run) |
|---|---|---|---|
| What it is | Custom fish, rarities, competitions, `/emf shop`, baits, journal | The same plus minigames, fishing bag, lava/void fishing | A Fishing skill, abilities, and a fishing loot table |
| 26.1.2 | **Boot-tested clean** (below); Modrinth lists 26.1–26.3 | Source has a `26_1` NMS module; **not tested** | Boot-tested 2026-09-23 (`skills-rpg.md`) |
| Licence (read from the repo) | MIT | GPL-3.0 source, but the **jar is sold** ($11.98) | GPL-3.0 |
| Cost | Free | Paid binary, or build it ourselves | Free |
| Command rewards | `COMMAND:` on competition places, catch, sell, eat and interact | Yes (action system) | `type: command` loot entries |
| MariaDB | **Tested**: creates `emf_*` tables on 11.8.9 | MariaDB listed as preferred | Tested earlier |
| Bedrock | Vanilla materials, chest GUIs, boss bar: no pack needed | Minigames draw with a font from a **resource pack** | No pack |

We take EvenMoreFish for the fish, the rarities and the competitions, and leave AuraSkills doing
what it already does: fishing XP, abilities, and its own rare/epic loot table. Nothing has to be
built. CustomFishing does more, but it is sold as a binary, needs a pack for its minigames (Bedrock
players can't see them), and only offers what we'd switch off anyway.

## EvenMoreFish

**Release.** 2.5.0, published 2026-09-28T19:56Z on Modrinth. Its game versions are
`1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3` and its loaders `paper, folia`
(`https://api.modrinth.com/v2/project/evenmorefish/version`). The same tag is on GitHub
(`github.com/EvenMoreFish/EvenMoreFish/releases/tag/v2.5.0`). The jar ships a `versions/26-1.jar`
module, and the source has `versions/26-1`, `26-2` and `26-3` build folders.

- Download: `https://cdn.modrinth.com/data/vlh7rLCf/versions/Wognhsxd/EvenMoreFish-2.5.0.jar`
- SHA-256: `1ffe00bd2d32ec3811d6d6f4562141182f747fcbd84c871912875e3256ef9bd1` (the SHA-512 matches
  the one Modrinth publishes, prefix `fbe4355dae6d44fd`)
- Size 2,816,928 bytes

**Maintenance.** It releases about every two weeks: 2.4.3 on 07-20, 2.4.4 on 08-03, 2.4.5 on 08-17,
2.4.6 on 08-31, 2.4.7 on 09-15 and 2.5.0 on 09-28 (Modrinth versions API). The last push was
2026-09-28 (GitHub API `repos/EvenMoreFish/EvenMoreFish`). It's the successor of the original
MoreFish (below).

**Licence: MIT.** `GET /repos/EvenMoreFish/EvenMoreFish/license` returns `LICENSE` with
"Copyright (c) 2022-2026 Oheers. Permission is hereby granted, free of charge … to use, copy,
modify, merge, publish, distribute, sublicense, and/or sell copies". There's nothing
non-commercial in it, so it's fine on a server that sells ranks.

**Dependencies.** None are required. Its `plugin.yml` (read from the jar) soft-depends on
AuraSkills, Vault, PlaceholderAPI, WorldGuard, Essentials (for vanish checks), mcMMO,
GriefPrevention, PlayerPoints, ItemsAdder, Nexo, Oraxen and a few more, none of them paid. It
downloads Flyway 12.2.0, mysql-connector-j 9.7.0 and sqlite-jdbc through Paper's library loader
(`libraries:` in `plugin.yml`). CraftEngine isn't a soft-dependency, but it has an item addon for
it: fish and bait items can be `craftengine:namespace:id`
(`docs/docs/features/addons/item-addons.md`, `addons/external/item/CraftEngineItemAddon.java`).

**Storage.** `database.type: mysql | sqlite`, and it's off by default (`config.yml`). With it off,
fish still work, but the fish log and journal stats aren't kept. The driver is MySQL Connector/J,
not the MariaDB driver. That worked against MariaDB 11.8.9 in the boot test. Flyway warned that
"MariaDB 11.8 is newer than the version Flyway has been verified with", which is harmless. Its
tables and its Flyway history table are both prefixed (`emf_flyway_schema_history`), so it can
share our database without clashing with our own Flyway history.

**Features** (from `src/main/resources/`):

- **Rarities and fish.** One file per rarity in `rarities/`: Junk, Common, Rare, Epic and
  Legendary out of the box, 72 fish in all. Each rarity and fish has a `weight`, a size range, a
  `worth-multiplier` or `set-worth`, a permission, biome and region requirements, per-player and
  global catch limits, and a broadcast. The events `catch-event`, `sell-event`, `eat-event` and
  `interact-event` each take a list of rewards (`rarities/_example.yml`).
- **Rewards** (`docs/docs/configuration/reward-types.md`, `CommandRewardType.java`): `COMMAND:`
  runs as console, with `{player}` and PlaceholderAPI filled in. There are also `MONEY:` (Vault),
  `ITEM:`, `EXP:`, `PERMISSION:` and `AURASKILLS_XP:auraskills/fishing,10`. A reward for an offline
  player is held until they rejoin, but lost if the server restarts first.
- **Competitions** (`competitions/_example.yml`): the types include LARGEST_FISH, MOST_FISH,
  LARGEST_TOTAL, SPECIFIC_FISH, SPECIFIC_RARITY and RANDOM. They can be scheduled by `times` and
  `days`, with `minimum-players`, a boss-bar countdown, `required-worlds`, `allowed-rarities`,
  `start-commands`, and rewards for places 1–N plus `participation`. The defaults schedule 30
  competitions a week (boot log).
- **Economy.** `/emf shop` and `/emf sellall` pay money through Vault at *length ×
  worth-multiplier*. PlayerPoints and GriefPrevention claim blocks are also supported
  (`config.yml` `economy:`).
- **Other.** Baits (`baits/`), custom rods (`rods/`), and a journal plus main and sell GUIs
  (`gui/`). Region boosts multiply rarity weights inside WorldGuard regions (a natural "fishing
  spot" at spawn). Lava and void fishing are off by default.
- **Anti-AFK** (`exploits.afk-fishing`, off by default). Once `max-caught` catches (default 10)
  land inside the same `range` (default 3) box, the whole catch is cancelled and its XP set to 0
  (`fishing/exploits/ExploitListener.java`). A player standing still with an auto-clicker or Auto
  Reel gets nothing, and AuraSkills gets no XP event to pay out on either.

**Our AuraSkills and EvenMoreFish's default.** `disable-auraskills-loot: true` is the default,
and it cancels AuraSkills' `LootDropEvent` for the fishing causes (Treasure Hunter, Epic Catch,
fishing luck) whenever a custom fish could land (`events/AuraSkillsFishingEvent.java`). That would
make two AuraSkills fishing abilities useless. **Set it to `false`** so both layers pay out.

**Bedrock.** Fish are vanilla items (cod, salmon, tropical fish…) with a name and lore. Menus are
chest inventories, and the competition bar is a boss bar, which Geyser shows. No pack is needed.
Custom textures would come through the CraftEngine addon, which Bedrock players see as the base
item, the same as the rest of our CraftEngine items.

## AuraSkills fishing (already installed)

What it gives today, with no new plugin:

- **Abilities** (`run/plugins/AuraSkills/abilities.yml`, text from `messages/messages_en.yml`):
  Lucky Catch ("+Fishing Luck, which increases the chance for extra drops"), Fisher ("more
  Fishing XP"), Treasure Hunter ("higher chance to get rare loot"), Grappler ("hook entities with
  more speed"), Epic Catch ("added chance to get epic loot"). Sharp Hook is a mana ability: "deal
  damage to a hooked entity when left clicking with a fishing rod".
- **Loot table** `loot/fishing.yml`: a `rare` pool (base chance 2 %, iron, gold nuggets,
  prismarine…) and an `epic` pool (0.5 %, diamonds, blocks, tipped arrows). Treasure Hunter and
  Epic Catch raise those two pools (`FishingLootHandler.java`, tag `2.4.0`).
- **Command loot works.** `type: command` with `command:` or `commands:` and `executor:
  console|player` (`common/loot/CommandLootParser.java`). `{player}` and PlaceholderAPI are filled
  in before `Bukkit.dispatchCommand` (`bukkit/loot/handler/LootHandler.java#giveCommandLoot`). So a
  crate key can go straight into the epic pool:

  ```yaml
  - type: command
    command: crazycrates give virtual Common 1 {player}
    weight: 2
  ```

  That's the cleanest place for key drops: the chance grows with the player's Fishing level
  (Epic Catch), which is the RPG progression we want.
- **What it lacks:** no custom fish, no sizes or rarities you can collect, no competitions, no fish
  shop. That's the gap EvenMoreFish fills.
- `anti_afk.enabled: false` in our `config.yml`, though it has a `fishing_a` check
  (`min_count: 10`). Turn it on (see the loop risks below).

## Rejected

- **CustomFishing (Xiao-MoMi, the CraftEngine author).** The source is GPL-3.0
  (`GET /repos/Xiao-MoMi/Custom-Fishing/license`), and active: 2.3.28 on 2026-09-27, a commit on
  09-29. But the jar is sold: Polymart (now voxel.shop) resource 2723 is priced "11.98 USD", and
  its latest update, 2.3.28 (2026-09-29), says "26.3 support" (`api.polymart.org/v1/getResourceInfo?resource_id=2723`).
  Free GitHub releases stopped at 2.3.3 (2025-01-25). The GPL lets us build it ourselves (NMS
  through `sparrow-heart`, which has a `26_1` module; the README says to build on JDK 17 and 21),
  but that is a fork-and-build chore for every update. Its minigames draw with the font
  `customfishing:offset_chars` from a pack (`Resource Pack for games.zip` in the repo), which
  Bedrock players can't see. Otherwise it's strong: MariaDB is listed as preferred
  (`database.yml`), there's a daily earnings cap on its market, a toggle against auto-fishing
  mods, and a CraftEngine item/block provider. **Revisit only if we want minigames and accept the
  pack.**
- **MoreFish.** The Spigot resource 22926 was last updated 2019-02-08 and tested up to 1.13
  (Spiget API). The Modrinth `morefish` project is a Forge mod. Dead: EvenMoreFish is its
  continuation.
- **FishingPlus.** Spigot 125440, tested on "1.21" only, last updated 2025-05-31 (Spiget). There's
  no 26.x evidence.
- **Smaller Modrinth projects with 26.1.2 builds.** Fish Rework (MIT, 802 downloads, sea
  creatures and custom gear: a whole RPG layer we don't want), LFishing (MIT, loot boxes only),
  FreshFishing (MIT, sizes and rarities, 142 downloads), DP-CustomFishing (MIT, 117 downloads).
  They're small and single-maintainer, with none of EvenMoreFish's competition and reward depth.
  Fishing + and BetterFishing are "All Rights Reserved" with no source (Modrinth project API).
- **AntiAutoFishing** (Modrinth, Apache-2.0, 26.1.1–26.2 listed, updated 2026-09-22) catches
  auto-fishing *mods*. Keep it in mind if AFK farms get past the checks above.

## Economy: where the loops are

Money today comes from sugar cane (~$12,500/h early) and spawners, and raw fish are **not
sellable** in `/shop` (`sell: -1` for cod, salmon, pufferfish and tropical fish in
`EconomyShopGUI/shops/Farming/food.yml` and `Z_EverythingElse.yml`). Tokens come from cane (20–40/h
by hand), and the token shop prices keys against that (`docs/ECONOMY.md`). Fishing adds three
faucets.

**1. Money from `/emf shop`.** With the default rarities, the expected value per catch is about
(100×$1.5 + 10×$17 + 3×$69 + 1×$480) / 124 ≈ **$8**, taking the midpoint of each size range times
its `worth-multiplier`. At a few hundred catches an hour, that's roughly $1,500–3,500/h: well under
cane, so it's a side income and not a loop. The risk is **AFK**. Our own Mythic enchant **Auto
Reel** ("automatically reels in a hook on bite", `ExcellentEnchants/enchants/auto_reel.yml`) plus
a held-down right click makes an unattended farm. **Double Catch** ("chance to double the caught
item") may duplicate a custom fish and double its sale. Fixes: turn on `exploits.afk-fishing`,
keep worth low, and test Double Catch on an EvenMoreFish fish (below).

EvenMoreFish fish are vanilla materials, so if we ever make cod sellable in `/shop`, sell wands
would sell them too. Keep `sell: -1` on raw fish and let `/emf shop` be the only buyer.

**2. Tokens.** **Never put tokens or keys on `catch-event` for common rarities.** Legendary is
weight 1 of 124, about 0.8 % of catches. A Common key on every Legendary would be ~3 keys an hour
at 400 catches an hour, about 600 tokens' worth, 15–30× hand-farming cane. Tokens go only to:

- **Competition places** (need `minimum-players` real players, so they can't be farmed solo), and
- **A rare key pool.** Either AuraSkills' epic pool (scales with skill), or one extra EvenMoreFish
  rarity with a tiny weight (for example 0.05, about 1 in 2,500 catches: one every ~6 hours of
  active fishing) and a per-player daily cap through `player-catch-limit`, if needed.

**3. AuraSkills XP.** Double Catch and Seasoned Angler already feed fishing XP (`docs/ENCHANTS.md`
"Skill XP"). EvenMoreFish's own `AURASKILLS_XP:` reward would stack on top, so don't use it on
catches, or fishing out-levels every other skill.

**ADR-0006 fit.** This is good news: competitions and rare catches are another written-down free
route for keys, the kind the ADR asks for. Put the numbers in `ECONOMY.md`'s crate table once
they're set.

## Config we'd need

`plugins/EvenMoreFish/config.yml`:

```yaml
database: { enabled: true, type: mysql, address: "${AVIAN_DB_HOST}:${AVIAN_DB_PORT}", database: avian,
            table-prefix: emf_, username: ..., password: ... }   # filled by tools/sync-config
exploits: { afk-fishing: { enabled: true, max-caught: 10, range: 3 } }
disable-auraskills-loot: false        # keep Treasure Hunter / Epic Catch working
economy: { vault: { enabled: true, multiplier: 1.0 } }
bait: { competition-disable: true }
competition-backup: { enabled: true }
```

`competitions/main.yml`, for example two evening slots, at least 3 players:

```yaml
minimum-players: 3
rewards:
  '1': [ "COMMAND:tokens give {player} 150 fishing-competition", "COMMAND:crazycrates give virtual Rare 1 {player}" ]
  '2': [ "COMMAND:tokens give {player} 75 fishing-competition", "COMMAND:crazycrates give virtual Common 1 {player}" ]
  '3': [ "COMMAND:tokens give {player} 40 fishing-competition" ]
  participation: [ "MONEY:2500" ]
```

Also:

- Rarity files: retheme the names and messages in Brand colours, and keep `worth-multiplier` near
  the defaults.
- `messages.yml`: Brand palette.
- AuraSkills `loot/fishing.yml`: one `type: command` Common key entry in `epic`.
  `config.yml`: `anti_afk.enabled: true`.
- `downloadPlugins`: pin the Modrinth URL above with its SHA-256.

## Boot test (2026-09-30)

Scratch dir `/tmp/claude-1000/fishsrv`: Paper 26.1.2 build 74 on JDK 25, 127.0.0.1:25599, tmux
session `emftest` (the dev server was left alone). Alongside it: AuraSkills 2.4.0, EssentialsX
2.22.0, Vault 1.7.3, PlaceholderAPI 2.12.3 and LuckPerms 5.5.84, copied from `run/plugins/`.

- **First boot** (SQLite off): "EvenMoreFish has successfully hooked into Vault", PlaceholderAPI
  expansion `emf` registered, "Loaded FishManager with 5 Rarities and 72 Fish", "Loaded 3
  competition file(s) and 30 scheduled competitions". `emf admin competition start mainCompetition
  2` from the console started a contest, `emf admin competition end` ended it ("There were no
  fishing records"), and `papi parse --null %emf_competition_time_left%` answered.
- **Second boot** against a throwaway `mariadb:11.8.9` container on 127.0.0.1:33099: Hikari
  connected, and Flyway baselined and applied "8.1 Create Tables" and "8.2 Add indexes". Tables
  created: `emf_competitions, emf_fish, emf_fish_log, emf_transactions, emf_user_fish_stats,
  emf_users, emf_users_sales` plus `emf_flyway_schema_history`. **Zero ERROR lines** on that boot.
  The container was removed afterwards.
- **One bug:** `emf help` **from the console** throws `No key layers in MapLike[{}]` inside
  DaisyLib's message replacer (`HelpMessage.send`). It's cosmetic and console-only. Players weren't
  tested. The same text also appeared once *before* any plugin loaded; that line comes from the
  scratch `level-type=flat` with empty generator settings, not from EvenMoreFish.
- **Not tested:** CraftEngine alongside it, and anything that needs a real client.

## Next test on `./dev`, with a player (Java and Bedrock)

1. Catch 20 fish: rarities roll, fish have names and lore, and `/emf shop` pays through Vault into
   the Essentials balance.
2. With `disable-auraskills-loot: false`, confirm AuraSkills fishing XP still lands on an
   EvenMoreFish catch, and that the epic-pool command key fires (raise its chance temporarily).
3. **Double Catch on an EvenMoreFish fish:** is the fish duplicated, and does the copy sell? If
   yes, lower worth or exclude it.
4. Stand still and let an Auto Reel rod catch 12 times: catches 10+ must be blocked.
5. Force a competition with 3 accounts (one of them Bedrock through Geyser): the boss bar shows,
   and 1st place gets tokens (check the `fishing-competition` audit row) and a key.
6. Open `/emf` menus from Bedrock: the chest GUIs are usable.
7. `./dev check-log` clean after a restart, with CraftEngine loaded.
