# Quests: LMBishop Quests, starting with fishing

Written 2026-09-30. The owner wants quests, starting with fishing ("cool quests related to
fishing… this could be the start of that"). This note covers which plugin does it, how rewards fit
the economy, a starter set of six fishing quest lines, and a boot test on our Paper build.

## Choice: LMBishop Quests (master build `4dbf0ff`)

| | LMBishop Quests | PikaMug Quests | BetonQuest | BeautyQuests |
|---|---|---|---|---|
| Latest | 3.16.1 release (2025-11-30); master CI build 2026-08-15 | 5.3.2 (2026-08-03) | 3.2.0 (2026-08-17), 3.3.0-DEV-49 (2026-09-27) | 2.1.0 (2026-08-23), alpha build 157 (2026-09-23) |
| 26.1.2 | **Boot-tested clean** (below). The release's listings stop at 1.21.10, but its own download page lists "1.21.5 – 26.1.2" on Java 25 | Modrinth lists 26.1–26.2 | Modrinth lists 26.1–26.3 | Modrinth lists 26.1–26.2 |
| Licence (read from the repo) | GPL-3.0 | MIT | GPL-3.0 | MIT |
| EvenMoreFish catches by rarity | **Yes, built in**: `evenmorefish_fishing` with `rarities:` and `fishes:` | No (vanilla `CATCH_FISH` only) | No (vanilla `fish` objective) | No (vanilla `StageFish`) |
| AuraSkills level | `placeholderapi_evaluate` on `%auraskills_fishing%` | No | Yes (`AuraSkillsLevelCondition`) | No |
| Daily / weekly | `repeatable` + `cooldown` (minutes) + `autostart` | Planner (repeat / cooldown) | Hand-built with conditions and actions | Timers per quest |
| Rewards | Console commands, `vaultreward` | Commands, money, items | Actions (commands etc.) | Rewards incl. commands |
| MariaDB | **Tested**: `mysql` provider, HikariCP, `quests_` tables | MySQL (Hikari) | Yes | Yes |
| Style | Chest GUI: categories → quests, YAML per quest | Chest GUI + chat editor, NPC-led | Conversation/NPC story engine | GUI editor, NPC-led stages |

We take **LMBishop Quests**: it's the only free, maintained plugin that counts **EvenMoreFish**
catches by rarity and fish out of the box, and its model (one YAML file per quest, console-command
rewards, autostarting repeatable quests on a cooldown, a chest GUI) is exactly "daily and weekly
goals" with nothing to build. The others would need our own code to see EvenMoreFish at all.

## LMBishop Quests

**Release.** 3.16.1, published 2025-11-30 (Modrinth `quests`, id `iU5kx4FN`,
`https://api.modrinth.com/v2/project/quests/version`; same on Hangar
`hangar.papermc.io/api/v1/projects/LMBishop/Quests/versions` and GitHub release `v3.16.1`). Its
listed game versions stop at 1.21.10, but the project's own download page
(`docs/download.md`) maps "Spigot API 1.21.5 – 26.1.2" to Java 25 and current Quests. The GitHub
repo's last push was 2026-08-30 and the last commits are 2026-08-15 ("Improve bartering task type")
and 2026-08-10 ("**Update EvenMoreFish API to 2.1.2**") (`gh api repos/LMBishop/Quests/commits`).
The `evenmorefish_fishing` and `evenmorefish_hunting` task types exist "Since v3.16"
(`docs/task-types/evenmorefish_fishing-(task-type).md`).

**Use the master build, not the 3.16.1 release.** The release was compiled against EvenMoreFish's
old API: its bytecode calls `com/oheers/fish/api/EMFFishEvent.getFish` and
`com/oheers/fish/fishing/items/Fish` (`javap` on the jar). EvenMoreFish 2.5.0 still ships those as
`@Deprecated` and re-fires them from the new events (`com.oheers.fish.events.DeprecatedEventListener`
calls `new EMFFishEvent(...).callEvent()` from `onCaughtEvent(EMFFishCaughtEvent)`), so the release
*works*, but on our build it also:

- logs "Your server is running version 1.1" (it misreads 26.1.2) and registers 48 task types
  instead of 50 (`blockchanging` and `blocklootdispensing` missing);
- logs two Paper warnings: "has registered a listener for com.oheers.fish.api.EMFFishEvent …
  but the event is Deprecated".

The master build listens to `com.oheers.fish.api.events.EMFFishCaughtEvent` directly
(`bukkit/.../dependent/EvenMoreFishFishingTaskType.java`, `@EventHandler(priority = MONITOR,
ignoreCancelled = true)`), so a catch EvenMoreFish cancels (its anti-AFK) never counts.

- Build: GitHub Actions run `31915954492`, commit `4dbf0ff936652c02a508b14b3bc7c91f1f1ca976`,
  artifact "! Quests-JDK25" → `Quests-3.16.1-4dbf0ff.jar`
  (`gh run download 31915954492 -R LMBishop/Quests -n "! Quests-JDK25"`).
- SHA-256: `b6478e47062240c9f88358a7f3f23010d04e9f8a52bbafe09a53dfdeb73c9172`, 1,444,040 bytes.
- **The artifact expires 2026-11-13** (GitHub API `actions/runs/31915954492/artifacts`). Either keep
  the jar ourselves or build the pinned commit (`./gradlew`, README "Downloads / Building"), and
  move to the next tagged release when one ships.

**Licence: GPL-3.0.** `LICENSE.txt` in the repo is the GNU GPL v3 text, and `docs/download.md`
quotes "Copyright (C) 2022 Leonardo Bishop and contributors … GNU General Public License … version
3, or (at your option) any later version". There's no non-commercial clause; running it on a server
that sells ranks is fine. We only run it, so the GPL's source duties never come up.

**Dependencies.** None required. `plugin.yml` soft-depends on Essentials, EvenMoreFish, Vault,
PlaceholderAPI, FancyNpcs, CoreProtect, CustomFishing, Votifier/VotingPlugin and others, none of
them paid. `api-version: "1.13"`, `folia-supported: true`.

**Storage.** `options.storage.provider: yaml | mysql` (`docs/configuration/storage-providers.md`).
MySQL goes through HikariCP with `table-prefix: "quests_"`; "the database specified **must** exist
before connecting". **Tested against MariaDB 11.8.9** (below). It creates
`quests_database_information`, `quests_player_preferences`, `quests_quest_progress`,
`quests_task_progress`, so it can share our `avian` database without clashing.

**Quest model** (`docs/configuration/creating-a-quest.md`):

- One file per quest in `quests/`; the file name is the quest id. Categories in `categories.yml`.
- `tasks:` each with a `type`. All tasks must be done to finish the quest.
- `rewards:` a list of **console commands** with `{player}` (`player:` prefix runs as the player),
  plus `vaultreward:`, `rewardstring:`, `startcommands:`, `expirycommands:`.
- `options:` `repeatable`, `cooldown: { enabled, time }` (minutes), `time-limit`, `autostart`,
  `counts-towards-limit`, `requires: [quest ids]` (chains), `permission-required`, `hidden`,
  `sort-order`, and per-state display items (completed, cooldown, locked).
- `config.yml`: `quest-limit.default: 2` (`quests.limit.<rank>` for more), `quest-autostart`,
  bossbar and actionbar progress, titles, sounds, `gui-hide-locked`, `gui-use-placeholderapi`.
- Colours: `&` codes and hex as `&#RRGGBB` (`docs/configuration/colour-codes.md`), so the Brand
  palette works. MiniMessage isn't mentioned.
- PlaceholderAPI expansion `quests` registers (boot test), for TAB or a sidebar.

**Fishing task types** (all present in the boot test, `/quests a types`):

- `evenmorefish_fishing` / `evenmorefish_hunting`: `amount`, `rarity`/`rarities`, `fish`/`fishes`,
  `rarity-match-mode` and `fish-match-mode` (`EQUALS`, `STARTS_WITH`, `ENDS_WITH`), `worlds`.
  Rarity is matched on the rarity **id** (`Fish.getRarity().getId()` in
  `EvenMoreFishFishTaskType.java`); ours are `Junk`, `Common`, `Rare`, `Epic`, `Legendary`
  (`dev-server/plugins/EvenMoreFish/rarities/*.yml`).
- `fishing`: vanilla catches (`docs/task-types/fishing-(task-type).md`).
- `placeholderapi_evaluate`: `placeholder`, `evaluates`, `operator: GREATER_THAN_OR_EQUAL_TO …`,
  polled every 30 ticks by default. AuraSkills' `%auraskills_[skill]%` is "Level for a certain
  skill" (`wiki/placeholders.md` at AuraSkills tag `2.4.0`), so `%auraskills_fishing%` ≥ N works.
- `permission`: polls online players every 30 ticks and completes when they hold the node
  (`PermissionTaskType.java`). That's how a contest placing reaches a quest (below).

There's **no "won a contest" task type**. EvenMoreFish's place rewards run console commands, so
they grant a temporary LuckPerms node that a `permission` task watches.

**Commands.** `/quests` (aliases `/q`, `/quest`) opens the GUI; `quests.command` is default true.
`/quests c <category>` needs `quests.command.category` (default op), so grant it to `default` in
LuckPerms if a DeluxeMenus button opens the fishing category directly. Admin:
`/quests a moddata start|complete|reset <player> <quest>`, `/quests a reload`, `/quests a config`
(validates), `/quests a debug` (`docs/commands-and-permissions.md`).

**Bedrock.** Chest-inventory menus with vanilla item displays and lore, titles, boss/action bars:
all rendered by Geyser, no pack needed. Left-click starts, right-click cancels, **drop key tracks**
(`gui-actions`); with `autostart` nobody needs to click anything, which suits Bedrock.

**Maintenance.** One maintainer plus contributors, 212 stars, not archived. Releases are slow
(3.15 → 3.16 took 15 months), but master is kept current: EvenMoreFish API bumped 2026-08-10.

## Rejected

- **PikaMug Quests** (Modrinth `quests.classic`, 5.3.2, 2026-08-03, game versions to 26.2, MIT
  `LICENSE.txt`). Solid and current, MySQL via Hikari (`storage/implementation/sql/connection/
  hikari/MySqlConnectionFactory.java`). But its objectives (`api/.../enums/ObjectiveType.java`) are
  vanilla only: `CATCH_FISH` plus `CUSTOM` modules, and there's no EvenMoreFish module. It's built
  around NPC-given story quests and a chat editor. **Second choice** if LMBishop stalls, at the cost
  of writing an EvenMoreFish module.
- **BetonQuest** (GPL-3.0, 3.2.0 release lists 26.1.2; DEV builds daily). It has an AuraSkills
  integration (`compatibility/auraskills/condition/AuraSkillsLevelCondition.java`) and a vanilla
  `fish` objective, but no EvenMoreFish. It's a scripting engine for NPC conversations and stories:
  far more than daily goals need. **Revisit for a spawn storyline**, not for dailies.
- **BeautyQuests** (MIT `LICENSE.md`; 2.1.0 release lists 26.1.2, newer builds are alpha). Vanilla
  `StageFish` on `PlayerFishEvent`; integrations (`integrations/`) don't include EvenMoreFish or
  AuraSkills. NPC-led like PikaMug.
- **NotQuests** (GPL-3.0). 7.0.0 (2026-09-22) is **26.3 only**; 6.3.0 (2026-06-15) is the last with
  26.1.2. Vanilla `FishItemsObjective`, no EvenMoreFish.
- **AuroraQuests** (Apache-2.0). Has daily/weekly pools on cron, an AuraSkills XP objective
  (`hooks/auraskills/objective/GainAuraSkillsXpObjective.java`) and EconomyShopGUI, but
  2.5.0-b168 declares `api-version: '26.2'` (`src/main/resources/plugin.yml`) and won't load on
  26.1.2; it needs the `Aurora` library, which Modrinth lists as "All Rights Reserved" with no
  licence in its repo (`gh api repos/AuroraNetworkStudios/Aurora` → `license: null`); fishing hooks
  CustomFishing, not EvenMoreFish. **Look again if we move to 26.2.**
- **ODailyQuests** (GPL-3.0). The only other one with EvenMoreFish built in
  (`events/listeners/integrations/emf/EMFFishCaughtListener.java`), but the repo is **archived**
  (`archived: true`, last push 2026-04-30) and 3.0.2 stops at 1.21.11. Its "Continued" fork
  (`odailyquests-continued`, 4.0, 2026-09-05) is 26.2-only with 11 downloads.
- **Battle passes.** BattlePass-Fork (8.2.5, lists 26.1.2) has **no LICENSE file** in its repo
  (GitHub `license: null`; upstream `lino9999/BattlePass` is MIT); FrPass (LGPL-3.0, 0.4.1) is new
  and small; BattlePass and TaskMaster are "All Rights Reserved". None counts EvenMoreFish catches.
  A season pass is a later idea, and LMBishop chains can approximate one.
- **Small or closed projects**: Quests EPIC, QuestSMP, NERDPOLE Quests, AnturniaQuests (all "All
  Rights Reserved" on Modrinth), MegaQuests (GPL-3.0, 452 downloads), MythicQuests (a Fabric and
  NeoForge mod, not a plugin).

## Starter fishing quests

Numbers we price against (`docs/ECONOMY.md`, `docs/research/custom-fishing.md`): hand-farmed tokens
20–40/h; Common key 200 tokens, Rare 600, Epic 1,500; contests already pay 40–300 tokens; custom
fish sell for about $8 each, $1,500–3,500/h. Our rarity weights are Common 100, Rare 10, Junk 5,
Epic 3, Legendary 1 (119 in all), so **Rare-or-better is ~1 in 8.5 catches** and **Legendary ~1 in
119**. A vanilla bite takes 5–30 s (less with Lure), so call it 200–350 catches an hour.

| Quest (id) | Task | Repeats | Rewards | Time it takes |
|---|---|---|---|---|
| Daily Catch (`fishdailycatch`) | 20 EvenMoreFish fish, any rarity | Daily, cooldown 1,200 min | $2,500 + 10 tokens | ~5 min |
| Rare Bites (`fishdailyrare`) | 3 fish of `Rare`, `Epic` or `Legendary` | Daily, 1,200 min | 20 tokens | ~25 catches, ~7 min |
| Big Haul (`fishweeklyhaul`) | 250 fish | Weekly, 8,640 min | $15,000 + 50 tokens | ~1 h over the week |
| Legend of the Deep (`fishweeklylegend`) | 1 `Legendary` | Weekly, 8,640 min | 50 tokens + Common key | ~119 catches, ~30–40 min on average |
| On the Podium (`fishweeklypodium`) | Place top 3 in a fishing contest (`permission` task) | Weekly, 8,640 min | 50 tokens | Needs a contest with 3+ players |
| Angler I → II → III (`fishlevel10/25/50`) | Fishing level 10, 25, 50 (`%auraskills_fishing%`) | Once each, chained by `requires` | I: $10,000 + Common key; II: Rare key; III: Epic key | Progression |

**Why these numbers are safe.**

- **They're capped.** The most a player can take is 7 × 30 = 210 tokens + $17,500 from dailies and
  150 tokens + 1 Common key + $15,000 from weeklies: about **360 tokens and one Common key a week**,
  ~560 tokens' worth. That's 14–28 hours of hand-farmed cane, earned in ~2 hours of fishing, but
  only once a week. An upgraded Harvester Hoe makes ~5,000 tokens a week (`ECONOMY.md`'s 20,000 a
  month), so quests are ~10 % on top for a hoe player and a real boost for a new one, which is what a
  daily habit should be.
- **No per-catch tokens.** Rewards land once per quest, never on every Legendary, which
  `custom-fishing.md` warned against (a key per Legendary would be 15–30× cane).
- **Anti-AFK still applies.** The master build ignores cancelled `EMFFishCaughtEvent`s, so a catch
  blocked by `exploits.afk-fishing` doesn't count towards a quest.
- **Milestone keys are one-time**, so they're a written-down free route to Common, Rare and Epic keys
  (ADR-0006), not a faucet. Tune the levels once we know how fast Fishing levels with
  rarity-based XP; 50 may be too far for an Epic key in V1.
- **The podium quest pays tokens, not a key**: the contest already gives 1st a Rare key.

**Rolling cooldowns, not a midnight reset.** `cooldown.time` counts from completion. 1,200 minutes
(20 h) lets a player who finished at 19:00 go again the next day from 15:00, so a daily rhythm
doesn't drift later each day. Likewise 8,640 min (6 days) for weeklies. There's no fixed "resets at
00:00" in LMBishop; if the owner wants that, `/quests a moddata reset` per player from a CommandTimer
job is the only route, and it isn't worth it now.

## Config approach

Files live in `dev-server/plugins/Quests/` (never `run/`); machine values go through
`tools/sync-config`.

`config.yml` (the parts we change):

```yaml
options:
  quest-autostart: false          # per-quest autostart instead
  quest-limit: { default: 3 }     # fishing quests don't count (counts-towards-limit: false)
  allow-quest-cancel: true
  actionbar: { progress: true, complete: true }
  bossbar:   { progress: false, complete: true }
  gui-hide-locked: false
  storage:
    provider: "mysql"
    database-settings:
      network:
        database: "${AVIAN_DB_NAME:-avian}"
        username: "${AVIAN_DB_USER:-avian}"
        password: "${AVIAN_DB_PASSWORD}"
        address: "${AVIAN_DB_HOST:-127.0.0.1}:${AVIAN_DB_PORT:-3306}"
      table-prefix: "quests_"
```

Plus: retheme `messages` and `guinames` in Brand hex (`&#RRGGBB`), remove the shipped
`quests/example*.yml` and example categories, and set sounds to match `Ui`'s click sounds.

`categories.yml`:

```yaml
categories:
  fishing:
    display:
      name: "&#4DA3FF&lFishing Quests"
      lore: [ "&7Daily and weekly goals for anglers." ]
      type: "FISHING_ROD"
```

A quest, `quests/fishdailyrare.yml` (the others follow the same shape; the boot-tested set is the
table above):

```yaml
tasks:
  rare:
    type: "evenmorefish_fishing"
    amount: 3
    rarities: [ "Rare", "Epic", "Legendary" ]
display:
  name: "&#4DA3FFRare Bites"
  lore-normal: [ "&7Catch &f3 &7Rare or better fish today." ]
  lore-started: [ "&7Progress: &f{rare:progress}&7/3" ]
  type: "SALMON"
rewards:
  - "tokens give {player} 20 quest-fishing-daily"
rewardstring: [ "&e+20 tokens" ]
options:
  category: "fishing"
  repeatable: true
  autostart: true
  counts-towards-limit: false
  cooldown: { enabled: true, time: 1200 }
```

Key rewards are `crazycrates give virtual Common 1 {player}`; money is `eco give {player} 2500` (or
`vaultreward:`). Token reasons (`quest-fishing-daily`, `quest-fishing-weekly`,
`quest-fishing-milestone`) keep quest income separable in the token audit trail, so we can measure it.

**Contest placing → quest.** Add to places 1–3 in `EvenMoreFish/competitions/main.yml` and
`sunday.yml`:

```yaml
  - 'COMMAND:lp user {player} permission settemp avian.quests.fishing.podium true 3d'
```

and clear it in the podium quest's rewards with
`lp user {player} permission unsettemp avian.quests.fishing.podium`, so one placing completes one
quest. (A placing during the quest's cooldown waits up to 3 days for the next round.)

**Getting there.** `/quests` works for everyone; a `Quests` button in the main DeluxeMenus menu
running `[player] quests c fishing` needs `quests.command.category` on `default`. A FancyNpcs
"Harbourmaster" at the spawn dock could run the same command later.

**Download pin.** Add the jar to `downloadPlugins` with the SHA-256 above, and keep a copy, since
the CI artifact expires 2026-11-13.

## Boot test (2026-09-30)

Scratch dir `/tmp/claude-1000/questsrv`: Paper 26.1.2 build 74 on JDK 25, 127.0.0.1:25600, tmux
session `questtest` (the dev server was left alone). Alongside it: EvenMoreFish 2.5.0, AuraSkills
2.4.0, EssentialsX 2.22.0, Vault 1.7.3, PlaceholderAPI 2.12.3 and LuckPerms 5.5.84, copied from
`run/plugins/`.

- **Boot 1, master build, YAML.** "Your server is running version 26.1.2.build.74-stable", "Using
  VersionSpecificHandler_V1_21_11", "Successfully hooked into Essentials economy", "**50 task types
  have been registered**", including `evenmorefish_fishing`, `evenmorefish_hunting`, `fishing`,
  `permission` and `placeholderapi_evaluate` (`/quests a types`). `papi list` showed the `quests`,
  `emf` and `auraskills` hooks. The seven starter quests above (rewards swapped to `eco give` and
  `say`, since tokens and CrazyCrates weren't loaded) loaded after `/quests a reload`, and
  `/quests a config` said "Quests did not detect any problems with your configuration". The only
  ERROR line was "No key layers in MapLike[{}]" before any plugin loaded: the scratch flat world,
  seen in the fishing test too.
- **Boot 2, master build, MySQL** against a throwaway `mariadb:11.8.9` container on
  127.0.0.1:33100: "Initialising storage provider 'mysql'", and the four `quests_*` tables were
  created (`quests_quest_progress` has `PRIMARY KEY (uuid, quest_id)`, utf8mb4). **Zero ERROR
  lines.** The container was removed afterwards.
- **Boot 3, 3.16.1 release jar**, same setup: loads, but "Your server is running version 1.1", 48
  task types, and two Paper warnings about listening to the deprecated `EMFFishEvent` /
  `EMFFishHuntEvent`. Hence the master build.
- **Not tested:** anything needing a real client: a catch counting, a reward firing, the GUI.

## Next test on `./dev`, with a player (Java and Bedrock)

1. `/quests`: the fishing category opens, the five repeatables and Angler I show as started
   (autostart), and the Brand colours render.
2. Catch fish: Daily Catch's actionbar ticks per EvenMoreFish catch; a Common catch does **not**
   tick Rare Bites, a Rare one does.
3. Complete Daily Catch: $2,500 lands, `tokens` shows +10 with reason `quest-fishing-daily` in the
   audit, and the quest shows its cooldown item. `/quests a moddata reset <player> fishdailycatch`
   restarts it for retesting.
4. Stand still with an Auto Reel rod until anti-AFK blocks catches: blocked catches don't count.
5. `/quests a moddata complete` on Legend of the Deep: a virtual Common key arrives (`/crates`).
6. `lp user <p> permission settemp avian.quests.fishing.podium true 3d`: the podium quest completes
   within ~2 s and the node is removed. Then force a real contest with 3 accounts.
7. Set the player's Fishing level to 10 with AuraSkills' admin command: Angler I completes and
   Angler II starts.
8. Open `/quests` from Bedrock through Geyser: the chest GUI is usable without a drop key (autostart
   means nothing needs clicking).
9. `./dev check-log` clean after a restart, with the full stack (CraftEngine, CarbonChat, TAB).
