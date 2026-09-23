# Avian Factions

A competitive OP/Hybrid Factions server for Minecraft Java Edition. The Faction is the central unit of competition; seasons reset the world and rankings.

## Language

**Faction**:
A named group of players with a leader, ranks, claims, power, and a bank, competing for F-Top within a Season.
_Avoid_: team, clan, guild, town

**Faction Member**:
A player belonging to exactly one Faction, holding one Faction Rank (Leader, Co-Leader, Officer, Member, Recruit).

**Claim**:
A single chunk owned by a Faction within a Season. Protection rules apply to non-members inside it.
_Avoid_: territory, region (region = WorldGuard, used only for Spawn/Warzone), plot

**Power**:
A per-player value (regenerates over time, lost on death) whose sum across a Faction's members determines how many Claims it may hold.

**Raidable**:
The state of a Faction whose Claims exceed its Power; its Claims lose protection against enemies.

**F-Top**:
The ranked list of Factions by configured value (spawners, blocks, items, upgrades), recalculated on a schedule and cached. Not bank balance.
_Avoid_: leaderboard (that's the player-stat lists under `/top`)

**Season**:
A bounded competitive period with its own world, Claims, F-Top, and payouts; archived, never deleted, on reset.

**SOTW**:
Start Of The World — the event that opens a Season (countdown, then claims/PvP/economy enabled).

**Warzone**:
The public PvP area around Spawn where Claims are disallowed and events (KOTH, envoys) happen.

**Spawn**:
The protected hub players arrive at; holds NPCs, crates, leaderboards, and the Warzone entrance.

**Avian Player**:
The server's profile for a player, keyed by UUID: balance, tokens, stats, faction membership. The API object modules use; never a raw DB row.
_Avoid_: user, account, profile (profile is the persisted record; the API object is the Avian Player)

**Module**:
One Gradle subproject (`avian-core`, `avian-factions`, …) with a clear package boundary, all shaded into the single plugin jar.
_Avoid_: plugin (reserved for the single shipped jar and for third-party plugins)

**Plugin Stack**:
The third-party plugins installed alongside the Avian jar (EssentialsX, LuckPerms, WorldGuard, …).

**Config Spec**:
A Module's declaration of its one config file: file name, config class, version, migrations, invariants. Core collects every Config Spec and loads them all before any Module enables (ADR-0003).

**Config Handle**:
The object a Module reads its configuration through; `get()` returns the current immutable config, `onReload` rebuilds derived state. Read on use, never cache.
_Avoid_: config singleton, static config

**Message Key**:
A dotted string (`factions.claim.success`) naming one MiniMessage template in `messages.conf`. Modules hold Message Keys as constants and ship default bundles; core owns the file and the style tokens.
_Avoid_: lang key, translation key (no locales in V1)

**Module Context**:
What a Module receives at enable: typed accessors for core services (players, database, messages, scheduler, commands, events, config) and the Service registry. A Module's dependencies are whatever it takes from its Module Context in `enable`, held in fields; never static lookups later (ADR-0004).

**Service**:
A cross-module interface in `avian-api` (`Economy`, `Players`, …) that one Module provides and others `require` through the Module Context. An interface becomes a Service only when a second Module needs it.
_Avoid_: manager, handler, helper
