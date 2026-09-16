# Build and test toolchain — versions, coordinates, gotchas

Research for issue #3. Verified against primary sources on 2026-09-16 (every version claim
links to the source it was read from). The exact Paper/Minecraft version is pinned by a
sibling issue; this note only records where a tool is version-sensitive.

## Headline findings

1. **Paper 26.x requires Java 25, not 21.** The Paper Fill API reports `java.minimum: 25` for
   26.2 (current STABLE channel, build 124) and 26.3 (ALPHA); 1.21.11 (Java 21) has been
   UNSUPPORTED since 2026-06-15. The Paper docs table says "26.1+ → Java 25". `CLAUDE.md`
   currently says "Java 21+" — the toolchain must be `JavaLanguageVersion.of(25)` if the
   pinned version is 26.x. MockBukkit's 26.2 artifact is also compiled for Java 25 (class
   file major 69).
2. **Paper's artifact versioning changed at 26.1.** `paper-api` is now
   `io.papermc.paper:paper-api:26.2.build.<N>-stable` (e.g. `26.2.build.124-stable`), not
   `1.21.x-R0.1-SNAPSHOT`. This lets us pin an exact build, which we want.
3. **Shade + relocate the DB stack; do not shade anything Paper bundles.** Paper's
   `plugin.yml` `libraries:` mechanism is a viable alternative (per-plugin `URLClassLoader`,
   no relocation needed) but it downloads from Maven Central at server start.
4. **Flyway needs `mergeServiceFiles()`** — `flyway-core` and `flyway-mysql` both ship
   `META-INF/services/org.flywaydb.core.extensibility.Plugin`; without merging, one overwrites
   the other and Flyway can't find the MariaDB database type.
5. **JUnit is at 6.x now** (Java 17+); MockBukkit 26.2 depends on `junit-jupiter-api 6.1.3`.
   Testcontainers 2.x renamed every module to `testcontainers-*`.

## Summary table

| Tool | Coordinates | Version (date) | Shade? | Notes |
|---|---|---|---|---|
| Gradle wrapper | `gradle-9.7.1-bin.zip` | 9.7.1 (2026-08-19) | n/a | Runs on JVM 17–26; toolchains up to Java 26; embeds Kotlin 2.4.0 |
| Foojay toolchain resolver | `org.gradle.toolchains.foojay-resolver-convention` (settings plugin) | 1.0.0 (2025-05-19) | n/a | Auto-downloads the JDK 25 toolchain |
| Shadow | `com.gradleup.shadow` | 9.6.1 (2026-07-22) | n/a | Needs Gradle ≥ 9.2.0; next release bumps to ≥ 9.4.0; legacy id `com.github.johnrengelman.shadow` is dead |
| run-paper | `xyz.jpenilla.run-paper` | 3.1.0 (2026-08-08) | n/a | **Requires Gradle 9.7+**; uses Fill v3; `build(N)` pins a Paper build |
| paperweight-userdev | `io.papermc.paperweight.userdev` | 2.0.0-beta.23 (2026-08-28) | n/a | **Not needed** (no NMS). See §5 for cost if ever needed |
| Paper API | `io.papermc.paper:paper-api` | `26.2.build.124-stable` (latest stable at time of writing) | `compileOnly` | Format `<mc>.build.<N>-stable`; brings Adventure BOM 5.2.0, Guava 33.6.0, Gson 2.14.0, slf4j-api 2.0.17, snakeyaml 2.2 |
| Adventure / MiniMessage | `net.kyori:adventure-api`, `net.kyori:adventure-text-minimessage` | 5.2.0 via paper-api BOM | **never** | Transitive `compileOnly` via paper-api; do not declare separately |
| HikariCP | `com.zaxxer:HikariCP` | 7.1.0 (2026-06-14) | shade + relocate | Java 11+ bytecode (major 55); needs slf4j (Paper provides) |
| MariaDB Connector/J | `org.mariadb.jdbc:mariadb-java-client` | 3.5.10 (2026-07-29) | shade + relocate | Multi-release jar (Java 8 base); 5 `META-INF/services` files; 3.5.10 is a security release |
| Flyway core | `org.flywaydb:flyway-core` | 13.7.0 (2026-09-15) | shade + relocate | Java 17 bytecode (major 61); `commons-text`/`jackson` optional only |
| Flyway MariaDB support | `org.flywaydb:flyway-mysql` | 13.7.0 | shade + relocate | Correct module for MariaDB (contains `org.flywaydb.database.mysql.mariadb.*`); OSS edition |
| JUnit | `org.junit:junit-bom`, `org.junit.jupiter:junit-jupiter`, `org.junit.platform:junit-platform-launcher` | 6.1.3 (2026-08-07) | test only | Java 17+; JUnit 4 runner removed; Vintage deprecated |
| MockBukkit | `org.mockbukkit.mockbukkit:mockbukkit-v26.2` | 4.116.1 (2026-08-12) | test only | Artifact id encodes the MC version; built against Paper `26.2.build.111-stable`; **Java 25 bytecode**; no 26.3 artifact yet |
| Testcontainers | `org.testcontainers:testcontainers-mariadb`, `org.testcontainers:testcontainers-junit-jupiter` | 2.0.5 (2026-04-20) | test only | 2.x renamed modules (`mariadb` → `testcontainers-mariadb`); JUnit 4 removed; needs the MariaDB driver on the test classpath |
| H2 | `com.h2database:h2` | 2.5.250 (2026-08-31) | test only (optional) | `MODE=MariaDB;DATABASE_TO_LOWER=TRUE`; not a substitute for Testcontainers for migration tests |

## 1. Gradle

- **Wrapper:** 9.7.1 is the latest stable (2026-08-19); 9.7.0 was 2026-08-06
  ([gradle.org/releases](https://gradle.org/releases/)).
- **JVM compatibility (9.7.1):** Gradle itself runs on JVM 17–26 ("JVM 27 and later versions are
  not yet supported"); Java 25 toolchains supported since 9.1.0, Java 26 since 9.4.0; embedded
  Kotlin 2.4.0 with language version 2.2
  ([compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)).
- **Java toolchain** (Kotlin DSL, [toolchains guide](https://docs.gradle.org/current/userguide/toolchains.html)):

  ```kotlin
  // in a convention plugin or each JVM subproject
  java {
      toolchain {
          languageVersion = JavaLanguageVersion.of(25) // Paper 26.x minimum
      }
  }
  ```

  Auto-provisioning needs a resolver in `settings.gradle.kts`; Gradle "only downloads JDK
  versions for GA releases":

  ```kotlin
  plugins {
      id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  }
  ```

  ([plugin portal](https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention);
  1.0.0 is Gradle 9 compatible and shades its own deps).
- **settings.gradle.kts conventions** ([multi-project builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)):
  always set `rootProject.name`; `include("avian-api", "avian-core", "avian-factions")`;
  project names match folder names, lower-case hyphenated; nested paths use
  `include("services:person-service")`.
- **Shared build logic:** Gradle discourages `allprojects {}` / `subprojects {}` ("cross-project
  configuration usually grows in complexity and becomes a burden") in favour of convention
  plugins in `buildSrc/` or an included `build-logic` build
  ([sharing build logic](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html)):

  ```kotlin
  // buildSrc/src/main/kotlin/avian.java-conventions.gradle.kts
  plugins { `java-library` }
  repositories {
      mavenCentral()
      maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
  }
  java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
  dependencies {
      compileOnly("io.papermc.paper:paper-api:26.2.build.124-stable") // exact build, never `.+`
      testImplementation(platform("org.junit:junit-bom:6.1.3"))
      testImplementation("org.junit.jupiter:junit-jupiter")
      testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  }
  tasks.test { useJUnitPlatform() }
  ```

  Gradle 9.7.1 note: "Kotlin DSL accessor generation is no longer stored in the build cache by
  default" and Isolated Projects graduated to incubating
  ([9.7.1 release notes](https://docs.gradle.org/9.7.1/release-notes.html)).

## 2. Shadow (`com.gradleup.shadow`)

- **Coordinates/version:** `id("com.gradleup.shadow") version "9.6.1"` (2026-07-22)
  ([plugin portal](https://plugins.gradle.org/plugin/com.gradleup.shadow),
  [releases](https://github.com/GradleUp/shadow/releases)). The `com.github.johnrengelman.shadow`
  id belongs to the abandoned 8.x line; the changelog says 8.3.x only receives Gradle 9
  compatibility backports.
- **Requirements** ([CHANGELOG](https://github.com/GradleUp/shadow/blob/main/CHANGELOG.md)):
  9.5.0 bumped min Gradle to 9.2.0; the unreleased next version bumps to 9.4.0; min Java 17 to
  run the plugin. ASM/jdependency support Java 26 (9.3.0) and Java 27 (9.5.0), so Java 25 class
  files are fine.
- **9.6.0 breaking change:** `@ShadowDsl` on `ShadowJar`, `ResourceTransformer`,
  `DependencyFilter`, `Relocator` "restricts nested DSL configuration blocks from implicitly
  calling outer receiver APIs in Kotlin script files" — qualify with `this@shadowJar` if needed.
- **Defaults** ([getting started](https://gradleup.com/shadow/getting-started/)): `shadowJar`
  bundles `main` output + `runtimeClasspath`, classifier `all`, excludes signature files and
  `module-info.class`; the plain `jar` task is left alone.
- **One jar from many subprojects** ([dependencies](https://gradleup.com/shadow/configuration/dependencies/)):
  apply Shadow **only** to the top-level plugin module; it `implementation(project(":avian-core"))`
  etc., and because project dependencies are on `runtimeClasspath` they are merged
  automatically. Filtering (`dependencies { include/exclude(dependency(...)) }`) "does **not**
  apply to transitive dependencies".
- **Relocation** ([relocation](https://gradleup.com/shadow/configuration/relocation/)) and
  **service files** ([merging](https://gradleup.com/shadow/configuration/merging/)):

  ```kotlin
  // avian-plugin/build.gradle.kts
  plugins {
      id("avian.java-conventions")
      id("com.gradleup.shadow") version "9.6.1"
  }
  dependencies {
      implementation(project(":avian-api"))
      implementation(project(":avian-core"))
      implementation(project(":avian-factions"))
      implementation("com.zaxxer:HikariCP:7.1.0")
      implementation("org.mariadb.jdbc:mariadb-java-client:3.5.10")
      implementation("org.flywaydb:flyway-core:13.7.0")
      implementation("org.flywaydb:flyway-mysql:13.7.0")
  }
  tasks.shadowJar {
      archiveClassifier = ""            // ship the shaded jar as the artifact
      mergeServiceFiles()               // REQUIRED for Flyway + JDBC driver (see gotchas)
      val libs = "club.avian.factions.libs"
      relocate("com.zaxxer.hikari", "$libs.hikari")
      relocate("org.mariadb.jdbc", "$libs.mariadb")
      relocate("org.flywaydb", "$libs.flyway")
      // never relocate org.slf4j, net.kyori, com.google.gson, com.google.common — Paper provides them
  }
  tasks.build { dependsOn(tasks.shadowJar) }
  ```

  `ServiceFileTransformer` relocates both the service file name and each line of its content
  (verified in [ServiceFileTransformer.kt](https://github.com/GradleUp/shadow/blob/main/src/main/kotlin/com/github/jengelman/gradle/plugins/shadow/transformers/ServiceFileTransformer.kt)),
  and the relocation docs note "any non-class files that are stored within a package structure are
  also relocated". `enableAutoRelocation = true` + `relocationPrefix` exists but relocates
  *everything*, including things we want left alone — prefer explicit `relocate()`.
- **`minimize()`** ([minimizing](https://gradleup.com/shadow/configuration/minimizing/)): skip it.
  The docs warn the analyzer misses classes "loaded dynamically via `Class.forName(String)`" —
  exactly how JDBC drivers and Flyway plugins load. If ever used,
  `minimize { exclude(dependency("org.mariadb.jdbc:.*")) ... }`.

## 3. run-paper (`xyz.jpenilla.run-paper`)

- **Version:** 3.1.0 (2026-08-08) — "**Requires Gradle 9.7+** — this release fixes
  compatibility with Gradle 9.7" ([release](https://github.com/jpenilla/run-task/releases/tag/v3.1.0),
  [plugin portal](https://plugins.gradle.org/plugin/xyz.jpenilla.run-paper)). 3.0.0 (2025-09-07)
  added config-cache support and switched to Paper's Fill v3 download API
  (`https://fill.papermc.io/v3/` in [DownloadsAPI.kt](https://github.com/jpenilla/run-task/blob/master/plugin/src/main/kotlin/xyz/jpenilla/runtask/paperapi/DownloadsAPI.kt)).
- **Pinning the Paper build:** `AbstractRun` exposes `build(buildNumber: Int)` →
  `DownloadsAPIService.Build.Specific(n)`; default is `Build.Latest`
  ([AbstractRun.kt](https://github.com/jpenilla/run-task/blob/master/plugin/src/main/kotlin/xyz/jpenilla/runtask/task/AbstractRun.kt)).
  Latest-build checks are rate-limited by the `xyz.jpenilla.run-task.updateCheckFrequency`
  Gradle property (default 1h) ([wiki: Properties](https://github.com/jpenilla/run-task/wiki/Properties)).
- **Plugins in the run dir** ([wiki: Basic Usage](https://github.com/jpenilla/run-task/wiki/Basic-Usage),
  [RunWithPlugins.kt](https://github.com/jpenilla/run-task/blob/master/plugin/src/main/kotlin/xyz/jpenilla/runtask/task/RunWithPlugins.kt)):
  our own `jar`/`shadowJar` is added automatically (`runPaper.disablePluginJarDetection()` to
  opt out); extra local jars via `pluginJars(...)`; downloads via `downloadPlugins { }` with
  `modrinth(id, version)`, `hangar(plugin, version)`, `github(owner, repo, tag, assetName)`,
  `url("...")`; reusable specs via `runPaper.downloadPluginsSpec { }` + `from(spec)`.
  Plugins are passed with Paper's `-add-plugin=` flag (MC ≥ 1.16.5), not copied.

  ```kotlin
  plugins { id("xyz.jpenilla.run-paper") version "3.1.0" }

  tasks.runServer {
      minecraftVersion("26.2")        // must match gradle.properties pin
      build(124)                      // pin the Paper build; omit for latest
      runDirectory(layout.projectDirectory.dir("run").asFile)
      jvmArgs("-Xmx2G")
      downloadPlugins {
          hangar("LuckPerms", "<version>")           // check ids on hangar.papermc.io
          modrinth("worldedit", "<version-id>")
          github("PlaceholderAPI", "PlaceholderAPI", "<tag>", "PlaceholderAPI-<v>.jar")
          url("https://...")
      }
      pluginJars(file("run-plugins/CoreProtect.jar"))
  }
  ```

  Defaults set by the task ([AbstractRun.kt](https://github.com/jpenilla/run-task/blob/master/plugin/src/main/kotlin/xyz/jpenilla/runtask/task/AbstractRun.kt),
  [RunServer.kt](https://github.com/jpenilla/run-task/blob/master/plugin/src/main/kotlin/xyz/jpenilla/runpaper/task/RunServer.kt)):
  `run/` working dir, `--nogui`, `-Ddisable.watchdog=true`,
  `-Dnet.kyori.adventure.text.warnWhenLegacyFormattingDetected=true`, `-Dfile.encoding=UTF-8`.
  `disablePluginRemapping()` is available but irrelevant for a pure-API plugin; from 26.1 Paper
  no longer ships obfuscated internals anyway (see §5). The JVM running the task must be Java
  25 for Paper 26.x — the task is a `JavaExec`, so set `javaLauncher` from the toolchain or run
  Gradle with JDK 25.

## 4. Paper API, Adventure, MiniMessage

- **Coordinate format** ([project setup](https://docs.papermc.io/paper/dev/project-setup/)):
  `compileOnly("io.papermc.paper:paper-api:26.2.build.+")` in the docs; "Before `26.1`
  (`1.21.11` and below), the version string format used was `{VERSION}-R0.1-SNAPSHOT`, with no way
  to reference a specific build." Replace `+` with an exact build (`26.2.build.124-stable`) —
  our rule is never floating. The repo's `maven-metadata.xml` lists `26.2.build.1…124-stable`
  and `26.3.build.N-alpha`
  ([repo.papermc.io](https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml)).
- **`api-version`** ([plugin.yml](https://docs.papermc.io/paper/dev/plugin-yml/)): "The valid
  versions are 1.13 - 26.2"; minor versions allowed from 1.20.5; `api-version: '26.2'`.
- **What paper-api pulls in (compile scope)** — read from
  [paper-api-26.2.build.124-stable.pom](https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/26.2.build.124-stable/paper-api-26.2.build.124-stable.pom):
  `net.kyori:adventure-bom:5.2.0` (adventure-api, adventure-key, adventure-text-minimessage,
  serializer-gson/legacy/plain, logger-slf4j), Guava 33.6.0-jre, Gson 2.14.0, snakeyaml 2.2,
  joml 1.10.8, fastutil 8.5.18, log4j-api 2.26.0, slf4j-api 2.0.17, brigadier 1.3.10,
  jspecify 1.0.0, checker-qual 3.49.2, maven-resolver 1.9.18 (runtime).
  **None of these may be shaded**; all are on the server classpath. Adventure has been native on
  Paper since 1.16.5 build 473 — native platforms "bundle Adventure automatically" and avoid
  "users having to handle distributing Adventure and some platform adapter themselves"
  ([Adventure native platforms](https://docs.papermc.io/adventure/platform/native/)).
  Do **not** add `adventure-platform-bukkit`. If a module needs Adventure types without
  paper-api, use `compileOnly(platform("net.kyori:adventure-bom:5.2.0"))` +
  `compileOnly("net.kyori:adventure-text-minimessage")` (5.2.0 is the latest on
  [Maven Central](https://repo1.maven.org/maven2/net/kyori/adventure-bom/maven-metadata.xml)).

## 5. paperweight-userdev — not needed

- We stay on `paper-api` (no NMS), so **do not apply it**. Current release is 2.0.0-beta.23
  (2026-08-28) ([releases](https://github.com/PaperMC/paperweight/releases)); Paper docs say
  "only the latest version of `paperweight-userdev` is officially supported" and to use the
  latest stable Gradle ([userdev docs](https://docs.papermc.io/paper/dev/userdev/)).
- **Cost if ever needed:** a `paperweight.paperDevBundle("26.2.build.+")` dependency triggers
  `paperweightUserdevSetup`, which downloads the dev bundle and produces a decompiled,
  Mojang-mapped server jar for the IDE (minutes and gigabytes on first run per Paper build);
  every Paper build bump re-runs it and every MC update can break NMS code. "From Minecraft
  version 26.1 onwards, Paper no longer supports obfuscated plugins" (Mojang dropped server
  obfuscation), so the old `reobf` dance is gone — the remaining cost is purely NMS fragility.

## 6. Database stack: HikariCP, MariaDB Connector/J, Flyway

### Versions
- **HikariCP 7.1.0** (tag 2026-06-14; [tags](https://github.com/brettwooldridge/HikariCP/tags),
  [Maven Central](https://repo1.maven.org/maven2/com/zaxxer/HikariCP/maven-metadata.xml)).
  README: "Java 11+" artifact, requires "slf4j library"
  ([README](https://github.com/brettwooldridge/HikariCP/blob/dev/README.md)). Jar class-file major 55.
- **MariaDB Connector/J 3.5.10** (2026-07-29; [releases](https://github.com/mariadb-corporation/mariadb-connector-j/releases),
  [Maven Central](https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/maven-metadata.xml)).
  3.5.10/3.4.4/3.3.6 were released together as security fixes (socket-factory RCE when the
  JDBC URL is attacker-controlled) — stay on 3.5.x. LGPL-2.1; multi-release jar; optional deps
  (slf4j, bouncycastle, waffle-jna, AWS RDS) are not pulled transitively.
- **Flyway 13.7.0** (2026-09-15; [releases](https://github.com/flyway/flyway/releases),
  [flyway-core](https://repo1.maven.org/maven2/org/flywaydb/flyway-core/maven-metadata.xml),
  [flyway-mysql](https://repo1.maven.org/maven2/org/flywaydb/flyway-mysql/maven-metadata.xml)).
  Releases weekly; 13.0.0 was 2026-07-20 with no documented breaking changes
  ([engine release notes](https://documentation.red-gate.com/flyway/release-notes-and-older-versions/release-notes-for-flyway-engine)).
  Class-file major 61 → Java 17+.

### Is `flyway-mysql` the right module for MariaDB, and is it Community?
Yes on both. Red Gate's MariaDB reference says support is provided by the "`flyway-mysql`"
plugin module, "available in both Redgate and Open Source distributions", default driver class
`org.mariadb.jdbc.Driver`, URL `jdbc:mariadb://host:port/database`, driver "2.0.0 and later",
verified MariaDB 5.1–12.3.2
([MariaDB driver reference](https://documentation.red-gate.com/flyway/reference/database-driver-reference/mariadb)).
There is no `flyway-database-mariadb` artifact (404 on Maven Central). The 13.7.0 jar contains
`org/flywaydb/database/mysql/mariadb/{MariaDBDatabaseType,MariaDBDatabase,MariaDBParser}.class`
and registers them in `META-INF/services/org.flywaydb.core.extensibility.Plugin`.

`flyway-core` 13.7.0 declares only optional/provided deps (slf4j-api, commons-logging,
log4j-api, jboss-vfs, osgi, commons-text, jackson-databind) plus `lombok`/`annotations`
(compile-time only) — so shading it does not drag in Jackson. It logs through slf4j when present,
which Paper provides.

### Shade + relocate, or `plugin.yml` `libraries:`?
- **Why relocate:** every plugin gets its own classloader with the server as parent, but the
  plugin stack we ship alongside (LuckPerms, CoreProtect, EssentialsX, …) bundles its own
  HikariCP/driver copies. Unrelocated copies in multiple plugin jars are the classic source of
  `NoSuchMethodError`/`LinkageError` when Bukkit's plugin classloaders cross-resolve
  (`org.bukkit.plugin.java.PluginClassLoader` looks up classes across other plugins'
  loaders). Relocating to `club.avian.factions.libs.*` sidesteps it entirely.
  Decision: **shade + relocate HikariCP, mariadb-java-client, flyway-core, flyway-mysql.**
- **Alternative — `libraries:`** ([plugin.yml docs](https://docs.papermc.io/paper/dev/plugin-yml/)):
  "These libraries will be downloaded from the Maven Central repository and added to the
  classpath. This removes the need to shade and relocate the libraries." Paper resolves them with
  Maven Resolver into a local `libraries/` repo and creates a **per-plugin** `URLClassLoader`
  parented to the server loader
  ([LibraryLoader.java](https://github.com/PaperMC/Paper/blob/main/paper-api/src/main/java/org/bukkit/plugin/java/LibraryLoader.java)),
  so clashes with other plugins are avoided without relocation. Repository is only Maven Central
  (overridable with `PAPER_DEFAULT_CENTRAL_REPOSITORY` /
  `org.bukkit.plugin.java.LibraryLoader.centralURL`). Trade-offs: needs network on first boot,
  no transitive pinning beyond Maven's resolution, and the jar is not self-contained. Paper
  plugins (`paper-plugin.yml`) instead use a `PluginLoader` with `MavenLibraryResolver`, which is
  still marked experimental ([paper-plugins](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/)).
  Keep this as the fallback if the shaded jar ever hits size or licensing friction (Connector/J is
  LGPL; shading it is permitted but must be noted in the distributed license file).

### Config shape

```kotlin
// in the plugin, after relocation
val cfg = HikariConfig().apply {
    jdbcUrl = "jdbc:mariadb://$host:$port/$db"
    driverClassName = "club.avian.factions.libs.mariadb.Driver" // relocated name; see gotcha 4
    username = user; password = pass
    maximumPoolSize = 10
}
val ds = HikariDataSource(cfg)

Flyway.configure(getClass().getClassLoader())     // plugin classloader, not the server's
    .dataSource(ds)
    .locations("classpath:db/migration")
    .load()
    .migrate()
```

## 7. Test stack

- **JUnit 6.1.3** (2026-08-07; [releases](https://github.com/junit-team/junit-framework/releases),
  [junit-bom](https://repo1.maven.org/maven2/org/junit/junit-bom/maven-metadata.xml)). "JUnit
  requires Java 17 (or higher) at runtime" ([user guide](https://docs.junit.org/current/user-guide/)).
  6.0.0 notes: "Minimum required Java version is now 17", "Minimum required Kotlin version is now
  2.2", the JUnit 4 `junit-platform-runner` was removed, Vintage is deprecated, and "Platform
  artifacts now use the same version number as Jupiter" ([6.0.0 release notes](https://docs.junit.org/6.0.0/release-notes/)).
  Use `platform("org.junit:junit-bom:6.1.3")`, `junit-jupiter`, and `testRuntimeOnly
  junit-platform-launcher` (Gradle 9 no longer auto-loads the launcher).
- **MockBukkit** ([repo](https://github.com/MockBukkit/MockBukkit), default branch
  `minecraft/v26.2`, pushed 2026-09-13). Coordinates are
  `org.mockbukkit.mockbukkit:mockbukkit-v<MC>:<mockbukkit-version>`; Maven Central currently has
  `mockbukkit-v1.21` (4.116.3, built against `1.21.11-R0.1-SNAPSHOT`, Java 21 bytecode),
  `mockbukkit-v26.1.2` (4.115.0) and `mockbukkit-v26.2` (4.116.1, manifest
  `Paper-Version: 26.2.build.111-stable`, **Java 25 bytecode**, depends on `junit-jupiter-api
  6.1.3`, hamcrest 3.0, byte-buddy 1.18.11) — no `mockbukkit-v26.3` yet
  ([Maven Central listing](https://repo1.maven.org/maven2/org/mockbukkit/mockbukkit/),
  [v26.2 metadata](https://repo1.maven.org/maven2/org/mockbukkit/mockbukkit/mockbukkit-v26.2/maven-metadata.xml)).
  The README recommends reading the `Paper-Version` manifest attribute from the MockBukkit jar to
  align the `paper-api` version; with exact pins we just keep both in `gradle.properties` and
  assert they agree. `testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")`.
  **Version-sensitive:** MockBukkit tracks the MC minor; a Paper bump to 26.3 must wait for (or
  temporarily skip) the matching artifact.
- **Testcontainers 2.0.5** (2026-04-20; [releases](https://github.com/testcontainers/testcontainers-java/releases)).
  2.0.0 (2025-10-14): "Removed JUnit 4 support", "All modules are now prefixed with
  `testcontainers-`", "Container classes relocated to `org.testcontainers.<module-name>`" →
  `org.testcontainers.mariadb.MariaDBContainer`
  ([2.0.0 notes](https://github.com/testcontainers/testcontainers-java/releases/tag/2.0.0)).
  Coordinates: `org.testcontainers:testcontainers-mariadb:2.0.5`
  ([MariaDB module](https://java.testcontainers.org/modules/databases/mariadb/)) and
  `org.testcontainers:testcontainers-junit-jupiter:2.0.5`
  ([JUnit 5 integration](https://java.testcontainers.org/test_framework_integration/junit_5/)).
  The old `org.testcontainers:mariadb` / `:junit-jupiter` ids stopped at 1.21.4. "Adding this
  Testcontainers library JAR will not automatically add a database driver JAR" — add
  `mariadb-java-client` to `testRuntimeOnly`. Default image is `mariadb:10.3.39`; override with
  `MariaDBContainer("mariadb:<tag>")` matching whatever Compose pins. The Jupiter extension "has
  only been tested with sequential test execution".
- **H2 2.5.250** (2026-08-31; [releases](https://github.com/h2database/h2database/releases)).
  Compatibility mode: `jdbc:h2:mem:test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE` (optionally
  `;CASE_INSENSITIVE_IDENTIFIERS=TRUE`); "Do not change value of DATABASE_TO_LOWER after creation
  of database"; text comparison is case-sensitive unless `SET IGNORECASE TRUE`; supports
  `AUTO_INCREMENT`, `ON DUPLICATE KEY UPDATE`, `INSERT IGNORE`/`REPLACE INTO` "partially",
  `DATETIME` as `TIMESTAMP` ([H2 features](https://h2database.com/html/features.html)).
  Verdict: fine for fast repository unit tests of portable SQL; **not** a substitute for running
  Flyway migrations against real MariaDB (engine-specific DDL, collations, `utf8mb4`, JSON). Use
  Testcontainers for migration/integration tests and H2 only if the CI-without-Docker case matters.

## Gotchas

1. **Java 25 is the floor for Paper 26.x** (Fill API `java.minimum: 25` for 26.2/26.3; docs table
   "26.1+ → Java 25"). Everything in this stack runs on 25 (Gradle 9.7.1 runs on JVM 17–26;
   JUnit/Flyway need 17; HikariCP 11; MockBukkit 26.2 *requires* 25). Update `CLAUDE.md`'s
   "Java 21+" once the version pin lands.
2. **Gradle floor is 9.7** because run-paper 3.1.0 requires it; Shadow 9.6.1 needs ≥ 9.2 (next
   release ≥ 9.4). Pin the wrapper to 9.7.1 and don't let Renovate/Dependabot bump run-paper
   without also bumping the wrapper.
3. **`mergeServiceFiles()` is mandatory.** `flyway-core` and `flyway-mysql` both contain
   `META-INF/services/org.flywaydb.core.extensibility.Plugin`; Shadow's default duplicate
   strategy keeps the first, so the MariaDB `DatabaseType` silently disappears ("No database found
   to handle jdbc:mariadb://…"). The MariaDB driver has five service files that must survive too.
4. **Relocation changes the driver class name.** After `relocate("org.mariadb.jdbc", …)` the
   service file says `club.avian.factions.libs.mariadb.Driver`; `DriverManager` discovery
   through Bukkit's plugin classloader is unreliable, so set Hikari's `driverClassName` to the
   relocated name (HikariCP README: needed "if you get an obvious error message indicating that
   the driver was not found"). Shadow rewrites string constants in class files by default
   (`skipStringConstants = false`), so literal FQCNs in our source are rewritten too, but strings
   assembled at runtime or read from config are not.
5. **Flyway must be given the plugin classloader:** `Flyway.configure(getClass().getClassLoader())`
   (`configure(ClassLoader)` verified present in 13.7.0), otherwise `classpath:db/migration`
   scanning uses the server's loader and finds nothing. Keep migrations in the shaded module's
   resources.
6. **Never shade what Paper bundles**: Adventure/MiniMessage 5.2.0, Guava, Gson, slf4j-api,
   log4j-api, snakeyaml, fastutil, joml, brigadier, jspecify, checker-qual. Shading Gson or slf4j
   (HikariCP/Flyway transitive) would shadow the server's copies in our loader and break
   Adventure serializers/logging. Declare them `compileOnly` if referenced directly.
7. **Shadow 9.6 `@ShadowDsl`** blocks implicit outer-receiver calls inside `relocate {}` /
   `dependencies {}` / `transform {}` lambdas in `.kts` — use `this@shadowJar.` explicitly.
8. **JUnit 6, not 5.** `junit-jupiter:5.x` and MockBukkit 26.2's `junit-jupiter-api:6.1.3` would
   coexist badly (Gradle picks the highest and the 5.x engine/launcher mismatch fails at
   runtime). Use the 6.1.3 BOM everywhere and add `junit-platform-launcher` explicitly.
9. **Testcontainers 2.x renames** — `org.testcontainers:mariadb` (1.21.4) is a different, frozen
   artifact from `testcontainers-mariadb` (2.0.5); the BOM had gaps for prefixed names early in
   2.0, so declare explicit versions from a version catalog rather than trusting the BOM.
10. **MockBukkit is MC-minor-locked and Java-25-only.** No 26.3 artifact exists (26.3 is still
    Paper ALPHA); the `mockbukkit-v1.21` line is on Java 21 bytecode. A Paper bump = a MockBukkit
    bump, and the `Paper-Version` in its manifest should be checked against our pin.
11. **`paper-api` `.build.+` in the docs is floating** — use the exact `26.2.build.<N>-stable`
    string in `gradle.properties` (project rule: never floating) and the same `N` in
    `runServer { build(N) }`.
12. **Shadow `minimize()`** would strip the JDBC driver and Flyway plugins (dynamic loading).
    Leave it off.
13. **1.21.11 is end-of-life at Paper** (UNSUPPORTED since 2026-06-15 per Fill API). If the
    version pin lands on 1.21.x for plugin-stack reasons, the Java floor drops to 21 and MockBukkit
    becomes `mockbukkit-v1.21:4.116.3`, but Paper will not ship fixes.

## Sources

- Gradle releases: https://gradle.org/releases/
- Gradle 9.7.1 release notes: https://docs.gradle.org/9.7.1/release-notes.html
- Gradle compatibility matrix: https://docs.gradle.org/current/userguide/compatibility.html
- Gradle toolchains: https://docs.gradle.org/current/userguide/toolchains.html
- Gradle multi-project builds: https://docs.gradle.org/current/userguide/multi_project_builds.html
- Gradle sharing build logic: https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html
- Foojay resolver plugin: https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
- Shadow plugin portal: https://plugins.gradle.org/plugin/com.gradleup.shadow
- Shadow releases: https://github.com/GradleUp/shadow/releases
- Shadow changelog: https://github.com/GradleUp/shadow/blob/main/CHANGELOG.md
- Shadow docs: https://gradleup.com/shadow/getting-started/ , /configuration/dependencies/ , /configuration/relocation/ , /configuration/merging/ , /configuration/minimizing/
- Shadow ServiceFileTransformer source: https://github.com/GradleUp/shadow/blob/main/src/main/kotlin/com/github/jengelman/gradle/plugins/shadow/transformers/ServiceFileTransformer.kt
- run-paper plugin portal: https://plugins.gradle.org/plugin/xyz.jpenilla.run-paper
- run-task releases: https://github.com/jpenilla/run-task/releases (v3.1.0, v3.0.0)
- run-task README and wiki: https://github.com/jpenilla/run-task , https://github.com/jpenilla/run-task/wiki/Basic-Usage , /wiki/Properties , /wiki/Extensions
- run-task source (RunServer.kt, AbstractRun.kt, RunWithPlugins.kt, DownloadsAPI.kt): https://github.com/jpenilla/run-task/tree/master/plugin/src/main/kotlin
- Paper Fill API: https://fill.papermc.io/v3/projects/paper , /versions/26.2 , /versions/26.3 , /versions/1.21.11
- Paper getting started (Java table): https://docs.papermc.io/paper/getting-started/
- Paper project setup: https://docs.papermc.io/paper/dev/project-setup/
- Paper plugin.yml: https://docs.papermc.io/paper/dev/plugin-yml/ (source: PaperMC/docs `src/content/docs/paper/dev/getting-started/plugin-yml.mdx`)
- Paper plugins / loaders: https://docs.papermc.io/paper/dev/getting-started/paper-plugins/
- Paper userdev: https://docs.papermc.io/paper/dev/userdev/
- Paper LibraryLoader source: https://github.com/PaperMC/Paper/blob/main/paper-api/src/main/java/org/bukkit/plugin/java/LibraryLoader.java
- paper-api metadata and POM: https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml , .../26.2.build.124-stable/paper-api-26.2.build.124-stable.pom
- paperweight releases: https://github.com/PaperMC/paperweight/releases
- Adventure native platforms: https://docs.papermc.io/adventure/platform/native/
- adventure-bom metadata: https://repo1.maven.org/maven2/net/kyori/adventure-bom/maven-metadata.xml
- HikariCP README and tags: https://github.com/brettwooldridge/HikariCP/blob/dev/README.md , https://github.com/brettwooldridge/HikariCP/tags ; Maven Central: https://repo1.maven.org/maven2/com/zaxxer/HikariCP/maven-metadata.xml
- MariaDB Connector/J releases: https://github.com/mariadb-corporation/mariadb-connector-j/releases ; Maven Central: https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/maven-metadata.xml
- Flyway releases: https://github.com/flyway/flyway/releases ; engine release notes: https://documentation.red-gate.com/flyway/release-notes-and-older-versions/release-notes-for-flyway-engine
- Flyway MariaDB reference: https://documentation.red-gate.com/flyway/reference/database-driver-reference/mariadb
- Flyway Maven Central: https://repo1.maven.org/maven2/org/flywaydb/flyway-core/maven-metadata.xml , https://repo1.maven.org/maven2/org/flywaydb/flyway-mysql/maven-metadata.xml (jar/POM contents inspected for 13.7.0)
- JUnit releases: https://github.com/junit-team/junit-framework/releases ; user guide: https://docs.junit.org/current/user-guide/ ; 6.0.0 notes: https://docs.junit.org/6.0.0/release-notes/ ; BOM metadata: https://repo1.maven.org/maven2/org/junit/junit-bom/maven-metadata.xml
- MockBukkit repo/README: https://github.com/MockBukkit/MockBukkit ; Maven Central: https://repo1.maven.org/maven2/org/mockbukkit/mockbukkit/ (jar manifests inspected for v26.2 4.116.1 and v1.21 4.116.3)
- Testcontainers releases: https://github.com/testcontainers/testcontainers-java/releases (2.0.0 notes) ; docs: https://java.testcontainers.org/ , /modules/databases/mariadb/ , /test_framework_integration/junit_5/ ; Maven Central metadata for `testcontainers-mariadb`, `testcontainers-junit-jupiter`, `mariadb`, `junit-jupiter`
- H2 releases: https://github.com/h2database/h2database/releases ; features/compatibility modes: https://h2database.com/html/features.html

Could not be fetched (404 at time of writing, so not relied on): Flyway editions page
(`documentation.red-gate.com/flyway/flyway-concepts/flyway-editions`), Flyway MySQL driver page,
Testcontainers "upgrading to 2.0" page (the 2.0.0 GitHub release notes were used instead), and
`docs.papermc.io/adventure/platform/paper/` (the native-platforms page was used instead).
