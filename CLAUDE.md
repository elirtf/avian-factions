# Avian Factions

Minecraft Java Edition competitive Factions server (Paper). Product spec: `docs/SPEC.md` — authoritative for gameplay; it still uses the old code name "Legacy" — read `Legacy*` as `Avian*`.

## The goal

**A fully functional server we can run, manage and enjoy — not a custom plugin.** The plugin is a
means to that end, nothing more. So:

- **Prefer an existing plugin** whenever one does the job. Already bought rather than built:
  EssentialsX, LuckPerms, Vault, FastAsyncWorldEdit/WorldGuard, PlaceholderAPI, CoreProtect, Chunky, spark,
  FactionsUUID (ADR-0007), CommandTimer, EconomyShopGUI (`/shop` + sell prices), and a stacking
  plugin for mobs and spawners (#19).
- **Build only what is Avian-specific** and what nothing off the shelf provides: faction add-ons,
  the token/gem side of the economy, the Harvester Hoe, F-Top valuation (`avian-ftop`), seasons.
- When a feature could go either way, the question is "what gets us a working, fun server soonest
  and leaves us able to change it later" — not "what would be cleanest to own".
- **Take inspiration from popular servers** for events, crates, ranks and progression. Copy the
  *patterns* that are proven to be fun; never copy assets, configs or branding (spec §2).

**Nothing sold is exclusive** (ADR-0006): Mojang forbids selling advantage that is *designed* to be
exclusive. Money may buy something sooner, never something a free player cannot get. So:
paid ranks (Harpy, Griffin, Wyvern, Dragon, Phoenix) may carry kits and gear, and anything
gameplay-relevant — rank perks, kits, crate keys — needs a written-down in-game route at a rate a
real player reaches. Free ranks are just Hatchling and Fledgling.

Brand: bird-themed throughout — ranks, crates, events and kits all lean on the raven/hawk sigil
and the `mc.avian.club` identity.

## Locked foundations (see docs/adr/ and the wayfinder map issue for why)

- Server: **Paper 26.1.2 build 74** (not Purpur); pinned in `gradle.properties`, never floating. Re-evaluate 26.2 when EssentialsX cuts a stable release. Pins for the whole plugin stack: `docs/research/mc-version-and-plugin-stack.md`.
- Language/build: **Java 25** (Paper 26.1.2 minimum; this dev box has only 21 — use Gradle toolchain auto-provisioning or install openjdk25), **Gradle Kotlin DSL, multi-module, ONE shaded plugin jar** (`avian-api`, `avian-core`, `avian-factions`, …).
- Base package: `club.avian.factions`.
- Database: **MariaDB everywhere** (Docker Compose locally), HikariCP + Flyway, portable SQL, all DB access behind per-module repository interfaces. No SQLite path.
- Factions: **FactionsUUID 4.7.0** (ADR-0007), built from pinned source; `avian-factions` only adds what it lacks (base power, Claim Boost, upgrade list) through its API.
- Third-party stack: EssentialsX, LuckPerms, Vault, FastAsyncWorldEdit (replaces WorldEdit; dev build until 2.15.5 ships)+WorldGuard, PlaceholderAPI, CoreProtect, Spark, Chunky, FactionsUUID, RoseStacker, EconomyShopGUI, CrazyCrates, CommandTimer, AuraSkills (skills/stats; combat stats capped), Geyser + Floodgate + ViaVersion (Bedrock crossplay on UDP 19132; `docs/research/bedrock-crossplay.md`), DeluxeMenus (click menus: the `/f` faction menu), MiniMOTD (server-list MOTD), TAB (sidebar, tab list and nametags), CrazyAuctions (`/ah` auction house), ExcellentEnchants + nightcore + packetevents (custom enchants, `/enchanter`; `docs/ENCHANTS.md`), BetterRTP (`/rtp` 5,000+ blocks out), FancyNpcs (spawn NPCs), PlayTimeManager (playtime; promotes Hatchling to Fledgling).
- Brand: **Avian Factions** — raven/hawk sigil, dark premium arcane; stone/gold/red/purple. Future IP `mc.avian.club` (config placeholder only, never hardcoded).
- V1 world border: 10,000 blocks diameter (5,000 out from spawn); `/rtp` lands 2,000–4,800 out. Halved (owner, 2026-09-29) until a bigger machine can pre-generate 20,000.

## Dev loop

Backups and moving machines: `./dev backup` / `./dev restore <file>`, guide in `docs/MIGRATION.md`. Code and config are in git; the world, database and plugin data are not — they travel as a backup file.

Container and Kubernetes: `./dev image` builds the whole server as one image, `./dev restore-container <file>` loads a backup into it; guide in `docs/DEPLOYMENT.md`. Tracked config never holds machine values: write `${AVIAN_NAME:-default}` and `tools/sync-config` fills it from `.env` or the environment.

Everything goes through `./dev` (guide: `docs/DEVELOPING.md`): `./dev setup` once, then `./dev start` / `restart` / `stop`. The server runs in a tmux session named `avian`: `./dev console` attaches, `./dev cmd "<command>"` runs one console command and prints the reply (use this instead of attaching), `./dev check-log` fails on plugin errors. `./dev start` applies `dev-server/luckperms/ranks.lp` and the world border. Edit `dev-server/`, never `run/` (overwritten on every start). `./dev test` = `./gradlew build` (JDK 25 auto-provisioned; unit + MockBukkit + Testcontainers). Pins: `gradle.properties` + `gradle/libs.versions.toml`. CI boots the full stack with `./dev` on every push and PR.

## Development rules

Follow `docs/SPEC.md` §2 (Development Rules), §65 (async/sync), §74 (security), §81 (Definition of Done). In particular: never block the main thread on DB I/O; identify custom items by PersistentDataContainer, never display name; UUIDs internally; every gameplay value configurable. Everything custom players see (menus, item names, lore, messages) is **bright, colourful and engaging**: write it with `club.avian.factions.api.text.Brand` (palette, gradients, bars) and build menus with `club.avian.factions.api.text.Ui` (the menu kit): colour on the top and bottom rows only with the middle open, the subject top-centre, tooltips in the order purpose → `◆ Label  value` stats → `▶ Click · what happens`, `◀`/`▶`/`✖ Close` footer, glyph icons, click sounds. Engaging, never overbearing.

## Agent skills

### Issue tracker

GitHub Issues on `elirtf/avian-factions` via `gh`. See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context: `CONTEXT.md` at the root, ADRs in `docs/adr/`. See `docs/agents/domain.md`.

### Research notes

`/research` findings go in `docs/research/<slug>.md`.
