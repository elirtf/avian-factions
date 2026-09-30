# Avian Factions

A modern, original competitive Factions server for Minecraft Java Edition (Paper).

- Product spec: [`docs/SPEC.md`](docs/SPEC.md)
- Glossary: [`CONTEXT.md`](CONTEXT.md)
- **Running and developing: [`docs/DEVELOPING.md`](docs/DEVELOPING.md)**, the plain-language guide to `./dev`
- **Changing settings: [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md)** — plain-language guide to every option and which file it lives in
- **Moving to a new machine / backups: [`docs/MIGRATION.md`](docs/MIGRATION.md)** — `./dev backup` and `./dev restore`, step by step
- Decisions: [`docs/adr/`](docs/adr/)
- Planning map: the GitHub issue labelled `wayfinder:map`

## Status

The dev server runs the full stack: factions from FactionsUUID, our economy, 1.8 combat, F-Top and
raid windows. The map issue tracks what's next.

## Dev loop

**Start here: [`docs/DEVELOPING.md`](docs/DEVELOPING.md)**, the plain-language guide to running the
dev server, changing things and getting changes merged. Everything goes through `./dev`:

```sh
./dev setup       # once: .env, database in Docker, plugin downloads
./dev start       # build the plugin, start the server in the background (tmux), apply ranks + border
./dev console     # the live console; leave with Ctrl-b then d
./dev restart     # after changing code or config
./dev stop
./dev test        # every test + the jar (Docker needed for the database tests)
```

Needs Java 17+ to launch Gradle (JDK 25 is auto-provisioned), Docker and tmux. Running the server
means you accept the [Minecraft EULA](https://aka.ms/MinecraftEULA) for the local dev server (set via
`-Dcom.mojang.eula.agree=true` in `avian-plugin/build.gradle.kts`).

Underneath, `./dev` runs the Gradle tasks: `downloadPlugins` (pinned stack into `run/plugins/`,
hash-verified; FactionsUUID built from pinned source) and `runServer` (Paper 26.1.2 build 74, our
shaded jar at `avian-plugin/build/libs/AvianFactions-<version>.jar`, `dev-server/` config copied in).

Pins live in `gradle.properties` (Minecraft/Paper build, Java, MariaDB image, FactionsUUID) and
`gradle/libs.versions.toml` (every library and Gradle plugin). Nothing floats.

## CI

`.github/workflows/ci.yml`, on every push and PR:
- **build**: shellcheck on `./dev`, unit + MockBukkit + Testcontainers tests, uploads the jar and test reports
- **smoke**: boots the full stack with `./dev setup`, `./dev start`, `./dev check-log` and `./dev stop`. That's Paper, every pinned plugin and our `dev-server/` config, and it fails on any plugin error or missing hook

On `v*` tags, **release** also attaches `AvianFactions-<version>.jar` to the GitHub Release.

Cutting a release: `git tag -a v1.2.3 -m "..." && git push origin v1.2.3`. The tag name minus the leading `v` becomes the jar version (`-Pversion=`), so `v0.1.0` ships `AvianFactions-0.1.0.jar`.

## Dev server plugin stack

The pinned third-party plugins and the settings we changed are tracked in `dev-server/` and copied
into `run/` on every start. See `dev-server/README.md`. `./dev start` also applies the LuckPerms
ranks and the 10,000-block world border.
## Configuration

Plain-language guide to every setting and where it lives: **[`docs/CONFIGURATION.md`](docs/CONFIGURATION.md)**.


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
| `avian-combat` | `club.avian.factions.combat` | 1.8 PvP feel: attack-speed attribute, 1.8 knockback formula, sweep cancel |
| `avian-economy` | `club.avian.factions.economy` | money/tokens/gems, atomic audited transactions, sell values, Vault + EconomyShopGUI bridges |
| `avian-factions` | `club.avian.factions.factions` | additions to FactionsUUID (ADR-0007): base power, `claim_boost` upgrade, enabled-upgrade list; builds FactionsUUID from pinned source |
| `avian-ftop` | `club.avian.factions.ftop` | F-Top (spec §11): spawner stack and valuable block tracking, aging, pickup cost, `/ftop` |
| `avian-testing` | `club.avian.factions.testing` | test-only helpers shared by integration suites (`MariaDbExtension`) |
| `avian-plugin` | `club.avian.factions` | the one `JavaPlugin`, `plugin.yml`, the ordered module list, shadow + run-paper |
| `buildSrc` | — | `avian.java-conventions` (toolchain, Paper API, JUnit, `-Werror` on deprecation) |

Git-ignored: `run/` (Paper jar, world, plugin data, downloaded plugins), `.env`, `build/`, `.gradle/`, IDE files.
