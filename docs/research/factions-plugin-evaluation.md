# Factions: adopt an existing plugin or keep building our own?

Resolves GitHub issue #29 (research). Written 2026-09-23 against the pinned target **Paper 26.1.2 build 74 / Java 25**.

Crates (#23) and stacking (#19) were each evaluated before we built anything. Factions never were: "custom `avian-factions`" was locked as a foundation because of a licensing worry (see "Licensing" below). This note runs that evaluation now.

## TL;DR

**Adopt FactionsUUID 4.7.0, pinned, running as its own jar.** It is the only candidate that runs on our Paper build. It already covers nearly all of spec §7 and much of §8–10 and §25–27. The remaining gaps are mostly covered by other existing plugins (see "Filling the gaps without writing code"). The one real build is F-Top, done as a fork of SlashFTop, and possibly a tiny base-power listener. We would delete our custom membership, claims, power and protection code. `avian-core`, `avian-economy` and `avian-combat` stay as they are.

The cost is one exception to ADR-0002: FactionsUUID stores its data in JSON files, not MariaDB.

## Candidates

| Plugin | Boots on Paper 26.1.2 b74? | Status | Licence |
|---|---|---|---|
| **FactionsUUID 4.7.0** ([FactionsU/UID](https://github.com/FactionsU/UID), tag `4.7.0`) | **Yes, clean.** Verified with our full stack | Active: 4.7.0 released 2026-07-26 and targets exactly `26.1.2`; 4.7.1 (2026-09-17) moved to 26.2 | GPL-3.0. Premium on Spigot; the author asks servers that make money to buy it |
| SaberFactions 4.1.9-STABLE ([SaberLLC/Saber-Factions](https://github.com/SaberLLC/Saber-Factions)) | **No.** Crashes on enable: `IllegalArgumentException: Failed to parse server version from: 26.1.2-74-e4e17fc` (shaded XSeries) | Last release 2026-03 (1.21.x); latest commit message is "Introducing 26.1 error with dependency on build" | GPL-3.0 (`licenses/`) |
| Medieval Factions, ImprovedFactions | Not tested | Active | GPL-3.0 |

Medieval Factions and ImprovedFactions were ruled out without testing. Medieval is a roleplay factions plugin, not a competitive one. ImprovedFactions runs a SQL query per event (see `factions-prior-art.md`) and has no raiding, F-Top or upgrades. MassiveCraft and FactionsX are dead.

## How it was verified

- **Build.** Built from tag `4.7.0` with JDK 25. Upstream has a build quirk: `:bukkit:jar` and `:bukkit:shadowJar` write the same file, so a plain `shadowJar` can ship without cloud and fail with `NoClassDefFoundError: …/incendo/cloud/CommandManager`. The reliable sequence is `:bukkit:jar`, then `:bukkit:shadowJar -x :bukkit:jar`, then `:paper:shadowJar -x :bukkit:jar -x :bukkit:shadowJar`. That produced `factionsuuid.jar` with SHA-256 `ce6dfbed9f0904d20278570b65e9fdd753a1697e0597e38cba60e46cfde2652c`.
- **Boot.** Used a throwaway copy of `run/` with the full pinned stack plus our jar. FactionsUUID loaded as a Paper plugin and logged the following, with no errors:
  - `Loading as a fully-featured Paper plugin`
  - `Successfully hooked into LuckPerms for permission contexts!`
  - `Found and connected to Essentials`
  - PlaceholderAPI expansion `factionsuuid` registered
  - `Found support for WorldGuard 7.0.18` (registers the `fuuid-claim`/`fuuid-pvp`/`fuuid-noloss` flags)
  - `Found economy plugin through Vault: Avian Economy`
- **Not yet verified: gameplay with real players.** Claiming, raiding, TNT and power loss need a session with two players, just as #14 does for combat.

## Coverage against the spec

| Spec | FactionsUUID 4.7.0 | Gap |
|---|---|---|
| §7 commands | create, invite, join, leave, kick, role (promote/demote), disband, home/sethome, claim/unclaim, map, power, top, show, chat, relation (ally/truce/enemy/neutral), upgrades, warp, vault, fly, tnt, shield, grace, stuck, near, ban | None |
| §7 ranks | Five roles: admin, coleader, moderator, normal, recent. These map to Leader, Co-Leader, Officer, Member, Recruit, and are renamed in `translations.conf` | Naming only |
| §7 name validation | Length and characters in config | Profanity and reserved names are unverified |
| §8 power | Lazy per-player power with a configurable max, min, regen, death loss and offline behaviour. It supports a per-faction `powerBoost` and `permanentPower` through the API, and a DTR alternative | **No config setting for a "faction base power" of 100.** See the gap table below |
| §9 claims | `/f claim` with radius and fill flags, `/f map`, relation colours, seechunk particles, buffer zones | None. Our `/f claim <radius>` is replaced by theirs |
| §9 protection | Mature protection system plus WorldGuard flags | Our `ProtectionPolicy` is replaced by theirs |
| §10 upgrades | Built-in `power_max`, `spawner_rate`, `crop_yield`, `growth`, `mob_drop`, `mob_exp`, `shield`, `flight`, `max_members`, `vaults` and more. `UpgradeRegistry.registerUpgrade` lets addons add their own | Claim Boost: `power_max` only raises a faction-wide cap, so it is **not** a claim boost; we register our own `claim_boost` (see below). Vault tiers are `vaults` with PlayerVaultsX |
| §11 F-Top | Sorts by money, power, land, members or founded date only | **No value-based F-Top.** Fork SlashFTop (see below) |
| §25 raidability | `raidability` (land > power) exists but defaults to `false` | Set to `true` |
| §26 TNT | Faction TNT bank (`/f tnt`) | Cannon limits and explosion queues are unverified; decide with #14-style testing |
| §27 raid shield | Activated shield (duration + cooldown upgrade) and grace | **No scheduled weekday/weekend windows**, the spec's model. CommandTimer can switch grace on and off on a schedule (see below) |
| Economy | Faction banks through Vault, which is already our economy | Enable `economy.enabled` if we want banks |

## API for anything we build on top

Source under `bukkit/src/main/java/dev/kitteh/factions/`:
- **Lookups:** `Board.factionAt(FLocation)`, `Faction.claims()`, `claimCount()`, `members()`, `members(Role)`, `power()`, `powerBoost(double)`
- **Events:** `FactionCreateEvent`, `FactionDisbandEvent`, `FPlayerJoinEvent`, `FPlayerLeaveEvent`, `LandClaimEvent`, `LandUnclaimEvent`, `LandUnclaimAllEvent`, `PowerLossEvent`, `FactionRelationEvent`, `FactionRenameEvent`
- **Extension points:** `ThirdPartyCommands.register(plugin, name, …)` for `/f` subcommands and `UpgradeRegistry.registerUpgrade(...)` for upgrades. The repo's `example-plugin` shows both.
- **Compiling against it:** there is no published Maven artifact, so we compile against the pinned jar (`compileOnly(files(...))` or a local repo).

That covers what F-Top (#11), seasons (§50) and audit (§66) need.

## Storage

JSON only (`data.storage=JSON`: "Presently, the only option is JSON"). It uses atomic writes (see `factions-prior-art.md`). This is an exception to ADR-0002 for one plugin. I think it's acceptable because:
- the data is small at our scale (5,000-block border)
- backups are file copies
- season resets are file operations
- our own tables (players, economy) stay in MariaDB

## Licensing

The earlier reason for building our own was that copying GPL code would force our shaded jar to be GPL. That only applies to code merged into our jar. Running FactionsUUID **as a separate, unmodified jar** raises no licensing question. Even modifying it would only oblige us to share source if we distributed it, and running it on our own server isn't distribution.

Separately, the author asks servers that make money to buy it on Spigot. We should, since monetisation is planned (ADR-0006).

## Version pinning

4.7.0 is the last release for 26.1.2; 4.7.1 already targets 26.2. So FactionsUUID bug fixes now land on the 26.2 line only. That fits the existing plan to move to 26.2 once EssentialsX cuts a stable release: move Paper and FactionsUUID together.

## Filling the gaps without writing code

Before building anything, I searched GitHub, Modrinth and Hangar for existing plugins covering each gap. Most need no code. The small builds are a claim boost upgrade and a base-power listener; F-Top is a fork of an existing plugin.

| Gap | Covered by | Code needed |
|---|---|---|
| **Vault tiers (§10)** | FactionsUUID's own `vaults` upgrade, backed by PlayerVaultsX (GPL-3.0, now [KittehDev/PlayerVaultsX](https://github.com/KittehDev/PlayerVaultsX); the Modrinth project named "PlayerVaultsX" is an unrelated all-rights-reserved plugin) | None, but **deferred**: PlayerVaultsX has no downloadable releases (Spigot only, Cloudflare-gated) and targets spigot-api 1.21.11, so it needs its own source build. Not needed for launch |
| **Claim Boost (§10)** | **Correction:** FactionsUUID's `power_max` only raises a faction-wide power cap (`factionMax`, 0 = none), so with no cap it adds nothing. Nothing off the shelf adds flat power per level | **Small build:** a `claim_boost` upgrade registered through `UpgradeRegistry` that moves the faction's `powerBoost` by the difference between levels |
| **Faction base power 100** | FactionsUUID has per-faction `powerBoost` and `permanentPower`, set with `/f admin power …`, but nothing applies them by default when a faction is created | Either **retune per-player power** (`playerMax`, `playerStarting`), which needs no code, or a ~10-line `FactionCreateEvent` listener that sets `powerBoost(100)`. It's a balance call: base power mostly helps solo and small factions |
| **Scheduled raid-shield windows (§27)** | FactionsUUID's **grace** system blocks explosions server-wide for a duration (`/f admin set grace on <duration>`, `grace off`). [CommandTimer](https://github.com/titivermeesch/CommandTimer) 8.18.0 (Apache-2.0 on Modrinth, released 2026-09-18, lists 26.1.2) runs console commands at `times` on selected `days` (`Task.java`: `Collection<DayOfWeek> days`, `Collection<TaskTime> times`). A timer at the end of each raid window runs `grace on <hours until next window>`. Because the duration is built in, the grace expires on its own even if a timer misses | None. One difference from the spec: grace stops **all** explosions, including warzone and KOTH, where the spec only stops enemy TNT on claims. CommandTimer also serves #24 (the event timetable) |
| **Outcome for F-Top** | Reading SlashFTop in full showed it needs most of its code rewritten: legacy FactionsUUID API, InvUI 1.44 with a 1.21.1-only NMS module, `SpawnerUnstackEvent` handling that throws on explosions (null player) when a spawner is locked, cached totals that never re-age, a reload that restores only loaded chunks, and a synchronous load of every claimed chunk on enable | **Built fresh as `avian-ftop`** (ADR-0007), using its ideas only |
| **Value-based F-Top (§11)** | [BadgersMC/SlashFTop](https://github.com/BadgersMC/SlashFTop) (AGPL-3.0, last push 2025-11) is **built for FactionsUUID + RoseStacker**: <br>• it tracks spawner and block stacks from place/break and RoseStacker stack/unstack events, with no chunk scanning <br>• it has configurable spawner and block values, "aging" to full value over days, and a pickup cost after a grace period <br>• it has a GUI breakdown, MySQL (so MariaDB works) and PAPI placeholders | **A fork, not a build.** Three changes: <br>(1) It targets the legacy FactionsUUID 0.6 API (`com.massivecraft.factions`, about 13 distinct calls in 8 files, mostly `getTag`, `getId`, `getFactionById`, `isWilderness`) and Paper 1.21.1, so it needs porting to 4.x (`dev.kitteh.factions`) and 26.1.2. <br>(2) **It has no explosion handling**, so a spawner destroyed in a TNT raid would keep its F-Top value. We add an `EntityExplodeEvent` listener. <br>(3) It has 0 stars, 0 releases and a single author, so treat it as a starting point, not a dependency. <br>AGPL means we must offer the modified source to players; a public fork does that |

Also checked and rejected:
- **novucs/factions-top** (MIT): last push 2021 and uses the legacy API.
- **tjtanjin/SurvivalTop** (GPL-3.0): last push 2024, legacy API, supports WildStacker but not RoseStacker, and scans chunks.
- **Flockshot/FactionUpgrades**: no licence, and FactionsUUID's upgrade registry already covers it.
- **FactionShields** repos: no licence, stale, and they implement placeable shield blocks, not windows.
- **PvPIndex-Factions** (a MassiveCraft refactor on Modrinth that lists 26.1.2 and MariaDB): a full factions plugin rather than an add-on, but it has no F-Top, upgrades, TNT bank or shield. It's weaker than FactionsUUID for competitive play.

## What changes if adopted

- **Deleted:**
  - from `avian-factions`: faction CRUD, `FactionIndex`, the claim index, power, the protection listeners, `/f` and their migrations (down-migrations, or leave the tables unused)
  - the `Factions`, `Territory` and `ProtectionPolicy` interfaces in `avian-api`, which get replaced by a thin adapter over FactionsUUID
- **Kept:** `avian-core`, `avian-economy` (Vault provider, which FactionsUUID already uses), `avian-combat`.
- **Next, per the gap table above:**
  - add CommandTimer; register `claim_boost`; choose which upgrades are on (PlayerVaultsX deferred)
  - fork SlashFTop for F-Top (port it to FactionsUUID 4.x and add explosion handling)
  - base power: retune the config, or a tiny listener
  - raid windows: CommandTimer running grace
- **#27** (write ordering) still matters for economy writes; its factions half disappears.
- **Recorded as an ADR** superseding the "custom factions" foundation in `CLAUDE.md` and the map.

## Sources

- FactionsUUID repo, tag `4.7.0`: `gradle/libs.versions.toml` (`apiversion = "26.1.2"`), `paper/build.gradle.kts`, `bukkit/build.gradle.kts`, `upgrade/Upgrades.java`, `upgrade/UpgradeRegistry.java`, `command/ThirdPartyCommands.java`, `command/defaults/CmdTop.java`, `command/defaults/CmdClaim.java`, `Faction.java`, `Board.java`, `event/`, `example-plugin/`
- FactionsUUID default `config/main.conf` generated on our boot (`data`, `factions.claims`, `factions.landRaidControl`)
- SaberFactions release `4.1.9-STABLE` (`SaberFactions.jar`, SHA-256 `155084547ed93a53e071f13a0a30b251c66f5de13127cfd473b5a877a6f85750`), boot log on our stack
- https://factions.support (documentation, 4.7.0)
