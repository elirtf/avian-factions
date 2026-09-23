---
status: accepted
date: 2026-09-23
supersedes: the "custom avian-factions" locked foundation in CLAUDE.md, and the factions part of ADR-0004's day-one API
---
# Adopt FactionsUUID instead of building our own factions

Factions, claims, power, relations, roles and claim protection come from **FactionsUUID 4.7.0**,
running as its own plugin jar. `avian-factions` shrinks to what FactionsUUID lacks and talks to it
only through its API. Evaluation: `docs/research/factions-plugin-evaluation.md` (#29).

Crates (#23) and stacking (#19) were evaluated before anything was built; factions never was. The
custom module was locked early on a licensing worry: that GPL code would make our shaded jar GPL.
That applies only to code merged into our jar. A separate, unmodified plugin raises no question,
and running even a modified one on our own server is not distribution. With that gone, the goal in
CLAUDE.md decides it: FactionsUUID already has membership, roles, relations, homes, warps, chat,
fly, a TNT bank, grace, shields and an extensible upgrade system, years of exploit fixes, and it
boots cleanly on Paper 26.1.2 with our stack. We had built create/disband, claims, power and
protection, and none of the rest.

**What we built on top, and why each is ours:**

- **Base power.** FactionsUUID has no "every faction starts with N" setting; a listener sets
  `powerBoost` on `FactionCreateEvent` from `factions.conf` (`faction-base-power`, 5).
- **Claim Boost upgrade.** FactionsUUID's `power_max` only raises a faction-wide power cap, and we
  set none, so it cannot add land. `claim_boost` is registered through `UpgradeRegistry` and moves
  `powerBoost` by the difference between levels, leaving any admin-set boost alone.
- **Which upgrades are on.** FactionsUUID has no command for it, only its API or editing
  `data/universe.json` while stopped; `factions.conf`'s `enabled-upgrades` is applied at enable and
  on reload.

**What we configure instead of building:** power values and raidability in FactionsUUID's
`main.conf`; role names in its `translations.conf`; raid windows as CommandTimer timers switching
FactionsUUID's global grace (explosions off) on and off; spawner explosion drops (75% as items, the
rest destroyed) in RoseStacker. All tracked under `dev-server/`.

**Consequences:**

- **ADR-0002 exception.** FactionsUUID stores its data as JSON files (its only option). Our own
  tables stay in MariaDB. The custom faction tables are dropped by migration
  `V202609231500`; their CREATE migrations stay for Flyway's history.
- **Built from pinned source.** No Maven artifact exists and SpigotMC downloads are
  Cloudflare-gated, so `:avian-factions:buildFactionsUuid` builds the jar from the `4.7.0` tag's
  tarball, pinned by SHA-256 in `gradle.properties`. That one jar is our compile-time API and the
  plugin `downloadPlugins` installs. The author asks servers that make money to buy it on Spigot;
  we should before launch.
- **Version coupling.** 4.7.0 is the last FactionsUUID release for 26.1.2; 4.7.1+ target 26.2. Move
  Paper and FactionsUUID together.
- **New module hook.** `AvianModule.load(Logger)` runs in the plugin's `onLoad`, because
  FactionsUUID closes its upgrade registry at its own enable, which happens before ours.
- **ADR-0004's day-one factions API is gone:** `Territory`, `FactionService`, `ProtectionPolicy`
  and friends were deleted with the implementation. Code that needs factions uses FactionsUUID's
  API (`Board.factionAt`, `Faction.claims()`, its events) directly.
- **Grace is global.** It stops every explosion, including warzone and KOTH, not only enemy TNT on
  claims as spec §27 describes. Accepted for launch; a narrower rule would be a small listener.
- **F-Top is ours: the `avian-ftop` module.** SlashFTop (AGPL-3.0) was the closest existing plugin,
  but it targets the legacy FactionsUUID API and a 1.21.1-only GUI library, crashes when a locked
  spawner is blown up, reloads only loaded chunks, and serves stale aged values. Fixing it meant
  rewriting most of it, so F-Top was written fresh, keeping only its ideas (value aging, a pickup
  cost after a grace period), and no code, so AGPL does not apply. It tracks spawner stacks and
  valuable blocks by reading the world a tick after any event, which covers RoseStacker's
  explosion drops (75% as items, the rest destroyed) with no special cases.

**Considered:** keep building our own — rejected; it re-implements a mature plugin to get the
same thing later. SaberFactions — crashes on enable on 26.1.2. Medieval Factions and PvPIndex —
built for other play styles; no raiding, F-Top or upgrades. Forking FactionsUUID — not needed;
everything we add goes through its API, so upstream fixes keep arriving.
