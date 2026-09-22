# Avian Factions

A modern, original competitive Factions server for Minecraft Java Edition (Paper).

- Product spec: [`docs/SPEC.md`](docs/SPEC.md)
- Glossary: [`CONTEXT.md`](CONTEXT.md)
- Decisions: [`docs/adr/`](docs/adr/)
- Planning map: the GitHub issue labelled `wayfinder:map`

## Status

Scaffolded. The plugin builds, boots on the pinned Paper, and enables its modules. No gameplay yet — see the map issue for the frontier.

## Dev loop

Requires any JDK 17–26 to launch Gradle (the JDK 25 toolchain is auto-provisioned on first run), Docker, and network access.

```sh
cp .env.example .env         # once; DB credentials for Compose and the plugin
docker compose up -d         # MariaDB 11.8 on 127.0.0.1:3306 (data in a named volume; `down -v` wipes it)
./gradlew build              # compile, unit + MockBukkit tests, Testcontainers integration tests, shade
./gradlew test               # unit + MockBukkit only (no Docker needed)
./gradlew integrationTest    # repository tests against a throwaway MariaDB container
set -a; . ./.env; set +a     # export AVIAN_DB_* so the plugin can read the password
./gradlew runServer          # download Paper 26.1.2 build 74 (once), boot it with the jar loaded
./gradlew downloadPlugins    # fetch the pinned third-party stack into run/plugins/ (hash-verified)
```

The shaded jar is `avian-plugin/build/libs/AvianFactions-<version>.jar`.

`runServer` runs in the foreground; type `stop` to shut it down. Expect the log to show
`[AvianFactions] Enabled N modules` followed by `Done (…s)!`. Running it means you accept the
[Minecraft EULA](https://aka.ms/MinecraftEULA) for the local dev server (set via
`-Dcom.mojang.eula.agree=true` in `avian-plugin/build.gradle.kts`).

Pins live in `gradle.properties` (Minecraft/Paper build, Java, MariaDB image) and `gradle/libs.versions.toml`
(every library and Gradle plugin). Nothing floats.

## CI

`.github/workflows/ci.yml`: **build** (every push and PR — unit + MockBukkit + Testcontainers, uploads the jar and test reports) → **smoke** (`main` and `v*` tags — boots the shaded jar on the pinned Paper against a MariaDB service, asserts the plugin enabled, reached `Done`, and disabled cleanly) → **release** (`v*` tags — attaches `AvianFactions-<version>.jar` to the GitHub Release).

Cutting a release: `git tag -a v1.2.3 -m "..." && git push origin v1.2.3`. The tag name minus the leading `v` becomes the jar version (`-Pversion=`), so `v0.1.0` ships `AvianFactions-0.1.0.jar`.

## Dev server plugin stack

`./gradlew downloadPlugins` fetches the pinned third-party jars (hash-verified) into `run/plugins/`.
The settings we changed are tracked in `dev-server/` and copied into `run/` by `syncDevConfig`,
which `runServer` depends on — see `dev-server/README.md`. A fresh world needs
`worldborder set 5000` once on the console (it persists in `level.dat`).

## Configuration

One HOCON file per module in `run/plugins/AvianFactions/` (ADR-0003), written with commented
defaults on first boot. `core.conf` holds the database connection; `database.password` has no
default on purpose — set it in the file or, preferably, export `AVIAN_DB_PASSWORD` (`AVIAN_DB_HOST`,
`_PORT`, `_NAME`, `_USER` also override). Any invalid value in any file aborts startup with every
error listed as `file → path: message`.

Migrations: `avian-core/src/main/resources/db/migration/V<yyyyMMddHHmm>__<module>_<what>.sql`,
portable SQL with a `-- Dialect note` comment on anything MariaDB-specific (ADR-0002).

## Layout

| Module | Package | Holds |
|---|---|---|
| `avian-api` | `club.avian.factions.api` | interfaces other modules compile against (`AvianModule`, `ModuleContext`, `Services`, …) |
| `avian-core` | `club.avian.factions.core` | bootstrap (`ModuleBootstrap`, `ServiceRegistry`), config (`ConfigService`), database (`HikariDatabase` + Flyway), Avian Player profiles (`PlayerService`, `PlayerRepository`) |
| `avian-factions` | `club.avian.factions.factions` | the factions module (skeleton) |
| `avian-plugin` | `club.avian.factions` | the one `JavaPlugin`, `plugin.yml`, the ordered module list, shadow + run-paper |
| `buildSrc` | — | `avian.java-conventions` (toolchain, Paper API, JUnit, `-Werror` on deprecation) |

Git-ignored: `run/` (Paper jar, world, plugin data, downloaded plugins), `.env`, `build/`, `.gradle/`, IDE files.
