---
status: accepted
date: 2026-09-21
---
# Three test layers (JUnit, MockBukkit, Testcontainers MariaDB) plus a headless server smoke boot in CI

Tests live in three layers, each with one tool: pure **unit** tests (JUnit 6, no Bukkit) for every calculation in spec §73 and for the `ProtectionPolicy` decision function; **Bukkit-level** tests (MockBukkit `mockbukkit-v26.1.2`) for listeners, commands, GUI holders and module boot against a fake `ModuleContext`; **repository** tests (Testcontainers MariaDB, one container per JVM) for Flyway migrations and every repository's SQL. There is no H2 path. On `main` and on tags, CI additionally boots the real shaded jar on the pinned Paper build headlessly and asserts the plugin enabled — the only layer that catches shading, relocation, `plugin.yml`, Flyway-classloader and Brigadier-registration mistakes.

**Considered:** H2 in MariaDB mode for repository tests (faster, but the toolchain note already ruled it out for migrations, and two engines put the bugs in the gap between them — ADR-0002's reasoning); skipping MockBukkit (the #3 worry — a `v26.1.2` artifact exists: 4.115.0, built against Paper `26.1.2.build.72-stable`, Java 25 bytecode, Jupiter 6.1.2); smoke boot on every PR (60–90 s + a MariaDB service per run; PRs get the three layers instead); a coverage threshold (a number in place of §81's "tests exist for important logic" judgement).

**Consequences:** Docker is required to run `integrationTest` locally (present on the dev box; GitHub `ubuntu-latest` has it). `check` depends on both `test` and `integrationTest`. Moving to MC 26.2 means bumping the MockBukkit artifact id alongside the Paper pin. The §73 "server restart persistence" case is a boot-write-stop-boot-read extension of the smoke job, owned by #10.

## Layers

| Layer | Covers | Tool | Source set / task | Runs |
|---|---|---|---|---|
| Unit | §73 calculations (power, claim limits, transactions, F-Top, enchant probabilities/conflicts, cooldowns, crate weights, payouts, raidability, auctions), `ProtectionPolicy`, config validation/migration, claim index invariants (index ↔ reverse index agree after claim/unclaim/disband/world-unload) | JUnit 6.1.3 | `src/test` → `test` | every build, every PR |
| Bukkit-level | listeners (event → policy call), Brigadier commands, `InventoryHolder` GUIs, event firing, `AvianModule.enable` with a fake `ModuleContext` | JUnit 6 + MockBukkit `org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.115.0` | `src/test` → `test` | every build, every PR |
| Repository | Flyway migrations apply cleanly from empty; each repository's queries; optimistic-lock `WHERE id=? AND version=?` fails on a stale write; async writer drains | JUnit 6 + Testcontainers 2.0.5 `testcontainers-mariadb` (+`-junit-jupiter`) | `src/integrationTest` → `integrationTest` | every build, every PR |
| Smoke boot | shaded jar + pinned Paper `26.1.2 build 74` + MariaDB service; assert `Done (` and `[Avian] Enabled` in the log, then `stop` | run-paper `runServer` with `nogui`, `eula=true`, timeout | CI job `smoke` | `main` pushes and tags only |

## Shared test fixtures

An `avian-testing` module (`java-test-fixtures`) holds: `FakeModuleContext` (in-memory `Services`, no-op scheduler that runs sync, recording `Messages`), `MariaDbExtension` (JUnit extension that starts one `MariaDBContainer` per JVM and hands out a `DataSource` with migrations applied), and builders (`anAvianPlayer()`, `aFaction()`, `aTerritory()`). Modules depend on it with `testImplementation(testFixtures(project(":avian-testing")))`.

## Review rules (what "tests exist for important logic" means here)

1. Pure logic gets a unit test; if it has no unit test it is not pure enough — extract it.
2. Every protection listener gets a regression test per event: deny for enemy and neutral, allow for member, ally and bypass, plus wilderness, safezone and warzone cases. (The #4 survey found both FactionsUUID regressions were missing-case bugs.)
3. Every repository gets a Testcontainers test; every Flyway migration is covered by the migrations-apply test automatically, and a migration that changes existing rows gets a fixture-before/assert-after test.
4. No coverage gate. The reviewer checks rules 1–3 against the diff.

## CI matrix (for #11)

- `build`: JDK 25 via toolchain, `./gradlew build` (= `test` + `integrationTest` + `shadowJar`), upload the jar as an artifact. Every push and PR.
- `smoke`: needs `build`; MariaDB service container; download the jar; `./gradlew runServer` with a 5-minute timeout, tail the log for `Done (` and `[Avian] Enabled`, send `stop`, fail on either string missing or on any `[Avian]` `ERROR`. `main` and tags only.
- `release`: needs `smoke`; on tags, attach the jar to the GitHub release.
