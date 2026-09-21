# Avian Factions

Minecraft Java Edition competitive Factions server (Paper). Product spec: `docs/SPEC.md` — authoritative for gameplay; it still uses the old code name "Legacy" — read `Legacy*` as `Avian*`.

## Locked foundations (see docs/adr/ and the wayfinder map issue for why)

- Server: **Paper 26.1.2 build 74** (not Purpur); pinned in `gradle.properties`, never floating. Re-evaluate 26.2 when EssentialsX cuts a stable release. Pins for the whole plugin stack: `docs/research/mc-version-and-plugin-stack.md`.
- Language/build: **Java 25** (Paper 26.1.2 minimum; this dev box has only 21 — use Gradle toolchain auto-provisioning or install openjdk25), **Gradle Kotlin DSL, multi-module, ONE shaded plugin jar** (`avian-api`, `avian-core`, `avian-factions`, …).
- Base package: `club.avian.factions`.
- Database: **MariaDB everywhere** (Docker Compose locally), HikariCP + Flyway, portable SQL, all DB access behind per-module repository interfaces. No SQLite path.
- Factions: custom `avian-factions` module, informed by (not copied from) open-source factions plugins.
- Third-party stack: EssentialsX, LuckPerms, Vault, WorldEdit+WorldGuard, PlaceholderAPI, CoreProtect, Spark, Chunky.
- Brand: **Avian Factions** — raven/hawk sigil, dark premium arcane; stone/gold/red/purple. Future IP `mc.avian.club` (config placeholder only, never hardcoded).
- V1 world border: 5,000 blocks diameter.

## Dev loop

`./gradlew build` (JDK 25 toolchain auto-provisioned), `./gradlew runServer` (Paper pinned in `gradle.properties`, type `stop` to exit), `./gradlew downloadPlugins` (pinned stack → `run/plugins/`). Pins: `gradle.properties` + `gradle/libs.versions.toml`. Details in `README.md`.

## Development rules

Follow `docs/SPEC.md` §2 (Development Rules), §65 (async/sync), §74 (security), §81 (Definition of Done). In particular: never block the main thread on DB I/O; identify custom items by PersistentDataContainer, never display name; UUIDs internally; every gameplay value configurable.

## Agent skills

### Issue tracker

GitHub Issues on `elirtf/avian-factions` via `gh`. See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context: `CONTEXT.md` at the root, ADRs in `docs/adr/`. See `docs/agents/domain.md`.

### Research notes

`/research` findings go in `docs/research/<slug>.md`.
