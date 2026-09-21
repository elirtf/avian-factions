# Avian Factions

A modern, original competitive Factions server for Minecraft Java Edition (Paper).

- Product spec: [`docs/SPEC.md`](docs/SPEC.md)
- Glossary: [`CONTEXT.md`](CONTEXT.md)
- Decisions: [`docs/adr/`](docs/adr/)
- Planning map: the GitHub issue labelled `wayfinder:map`

## Status

Scaffolded. The plugin builds, boots on the pinned Paper, and enables its modules. No gameplay yet — see the map issue for the frontier.

## Dev loop

Requires any JDK 17–26 to launch Gradle (the JDK 25 toolchain is auto-provisioned on first run) and network access.

```sh
./gradlew build              # compile, test, shade → avian-plugin/build/libs/AvianFactions-<version>.jar
./gradlew runServer          # download Paper 26.1.2 build 74 (once), boot it with the jar loaded
./gradlew downloadPlugins    # fetch the pinned third-party stack into run/plugins/ (hash-verified)
```

`runServer` runs in the foreground; type `stop` to shut it down. Expect the log to show
`[AvianFactions] Enabled N modules` followed by `Done (…s)!`. Running it means you accept the
[Minecraft EULA](https://aka.ms/MinecraftEULA) for the local dev server (set via
`-Dcom.mojang.eula.agree=true` in `avian-plugin/build.gradle.kts`).

Pins live in `gradle.properties` (Minecraft/Paper build, Java) and `gradle/libs.versions.toml`
(every library and Gradle plugin). Nothing floats.

## Layout

| Module | Package | Holds |
|---|---|---|
| `avian-api` | `club.avian.factions.api` | interfaces other modules compile against (`AvianModule`, `ModuleContext`, `Services`, …) |
| `avian-core` | `club.avian.factions.core` | bootstrap (`ModuleBootstrap`, `ServiceRegistry`), and soon config, database, profiles, messages |
| `avian-factions` | `club.avian.factions.factions` | the factions module (skeleton) |
| `avian-plugin` | `club.avian.factions` | the one `JavaPlugin`, `plugin.yml`, the ordered module list, shadow + run-paper |
| `buildSrc` | — | `avian.java-conventions` (toolchain, Paper API, JUnit, `-Werror` on deprecation) |

Git-ignored: `run/` (Paper jar, world, plugin data, downloaded plugins), `build/`, `.gradle/`, IDE files.
