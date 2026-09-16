# Minecraft / Paper version and plugin-stack pin

Resolves GitHub issue #2. Researched 2026-09-16 against primary sources only
(PaperMC downloads API + docs, each plugin's GitHub releases / official site /
author-owned Hangar or Modrinth listing). Every version below was fetched on
that date; nothing is from memory.

## Recommendation

**Pin Paper `26.1.2` build `74` (channel STABLE, Java 25).**
Every plugin in the stack has a stable, released build whose author-stated
compatibility range includes 26.1.2. Nothing is held back.

| | |
|---|---|
| Minecraft / Paper version | `26.1.2` |
| Paper build | `74` (2026-07-06, channel `STABLE`, support status `SUPPORTED`) |
| Server jar | https://fill-data.papermc.io/v1/objects/1d70b1dab9cf4a6de615209a536f3a45a2186240253c428213ce2188ab95e5f7/paper-26.1.2-74.jar |
| SHA-256 | `1d70b1dab9cf4a6de615209a536f3a45a2186240253c428213ce2188ab95e5f7` |
| Java | **25** (minimum, per the API's `java.version.minimum` and the Paper docs table) |
| `paper-api` for the build | `io.papermc.paper:paper-api:26.1.2.build.74-stable` (see Maven section) |

Why not newer:

- **26.3** (released 2026-09-15): Paper only has `ALPHA`-channel builds (3-8, the
  newest published today). No stable Paper build exists, so it is disqualified
  before the plugin question even arises. No plugin in the stack lists 26.3 on
  its release channel either.
- **26.2** (Paper STABLE since build 83, 2026-07-26; latest build 124): **held
  back by EssentialsX.** The latest stable EssentialsX release, 2.22.0
  (2026-05-31), states it supports `26.1.2` and `1.21.11`; the "Update to
  Minecraft 26.2" change (PR #6561) was merged 2026-06-16, ten commits *after*
  the 2.22.0 tag, and no 2.22.1/2.23.0 release has been cut. 26.2 support only
  exists in EssentialsX dev builds, which the ticket excludes. CoreProtect
  v24.0 is a secondary, softer gap (see caveats).

When EssentialsX ships its next stable release, re-run this check: every other
plugin already lists 26.2, so 26.2 becomes viable at that point (with the
CoreProtect caveat noted below).

## Plugin pins (for Paper 26.1.2)

### EssentialsX (core, Spawn, Chat)

| | |
|---|---|
| Version | **2.22.0** (released 2026-05-31, GitHub release, not prerelease) |
| MC compatibility | 1.8.8 - 26.1.2. Release notes: "`26.1.2` - EssentialsX actively develops for and supports this version"; 1.8.8 ... 1.21.11 "still supported, but are not a priority". Modrinth listing: 1.8.8 -> 26.1.2 (16 versions). |
| Download (core) | https://github.com/EssentialsX/Essentials/releases/download/2.22.0/EssentialsX-2.22.0.jar (sha256 `bda4685105977fca2e209820a9f0ad24275bd103390a03236f38e59bfdac58e6`) |
| Download (Spawn) | https://github.com/EssentialsX/Essentials/releases/download/2.22.0/EssentialsXSpawn-2.22.0.jar (sha256 `dd5377c4c921b9b67814209f4f6646ffbb959729003e721ec5e63c47c7c010b8`) |
| Download (Chat) | https://github.com/EssentialsX/Essentials/releases/download/2.22.0/EssentialsXChat-2.22.0.jar (sha256 `e5b0211f98af1eaba712d9294997639a39209db1fc842394a0923820073ec65a`) |
| License | GPL-3.0 |
| Caveat on 26.1.2 | None. Folia not supported (release notes). Does **not** support 26.2/26.3 in this release (see above). |
| Source | https://github.com/EssentialsX/Essentials/releases/tag/2.22.0 |

### LuckPerms

| | |
|---|---|
| Version | **5.5.84** (luckperms.net's current release; LuckPerms publishes every build as a release via its own download site and has no GitHub Releases) |
| MC compatibility | Author's Modrinth listing for the Bukkit line (v5.5.71, the newest Modrinth entry): 1.8.9 -> 26.2 (65 versions), loaders bukkit/folia/paper/spigot. No per-version compatibility page exists beyond that. |
| Download | https://download.luckperms.net/1671/bukkit/loader/LuckPerms-Bukkit-5.5.84.jar (sha256 `ee57b908b415a22a770f0e1f5af1e43de9cb2b81b33715c6c60d0f6d89ee3a58`, computed locally on 2026-09-16) |
| Alternative pinned artifact | Modrinth v5.5.71-bukkit: https://cdn.modrinth.com/data/Vebnzrzj/versions/b0mk8uS6/LuckPerms-Bukkit-5.5.71.jar |
| License | MIT |
| Caveat on 26.1.2 | None. |
| Source | https://metadata.luckperms.net/data/all (version 5.5.84, download map), https://api.modrinth.com/v2/project/luckperms/version |

### Vault

| | |
|---|---|
| Version | **1.7.3** (GitHub release, 2020-07-17; latest and only release with an asset) |
| MC compatibility | **No author statement.** `plugin.yml` declares `api-version: 1.13`. Repo last pushed 2024-03-10, not archived; the tracker has no 26.x issues. A maintainer reply on issue #941 (2025-10/2026-02) says to assume plugins work on newer versions until disproven. Treat as "works in practice, unverified by the author". |
| Download | https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar (sha256 `a6b5ed97f43a5cf5bbaf00a7c8cd23c5afc9bd003f849875af8b36e6cf77d01d`, computed locally) |
| License | LGPL-3.0 |
| Caveat on 26.1.2 | Effectively unmaintained (no release since 2020, no push since 2024). It is an API shim, so breakage risk is low, but there is no upstream to fix it if Paper removes something it touches. |
| Source | https://github.com/MilkBowl/Vault/releases/tag/1.7.3, https://github.com/MilkBowl/Vault/issues/941 |

### WorldEdit

| | |
|---|---|
| Version | **7.4.5** (2026-08-09). EngineHub has no GitHub Releases; the tag `7.4.5` exists and the artifact is published on the author-owned Hangar and Modrinth pages. |
| MC compatibility | Modrinth title: "WorldEdit 7.4.5 (Bukkit for 1.21.4-26.2)"; game versions 1.21.4 -> 26.2; loaders bukkit/folia/paper/spigot. |
| Download | https://hangarcdn.papermc.io/plugins/EngineHub/WorldEdit/versions/7.4.5/PAPER/worldedit-bukkit-7.4.5.jar (same file on Modrinth: https://cdn.modrinth.com/data/1u6JkXh5/versions/F5ea2ov3/worldedit-bukkit-7.4.5.jar, sha1 `457add12f5c7eb0dfcb9945e6860e1c9dd8d9a48`) |
| License | GPL-3.0 (LICENSE.txt in repo) |
| Caveat on 26.1.2 | None. 7.4.5 changelog: "Replace PaperLib to improve compatibility with newer Minecraft versions". |
| Source | https://hangar.papermc.io/EngineHub/WorldEdit, https://modrinth.com/plugin/worldedit |

### WorldGuard

| | |
|---|---|
| Version | **7.0.18** (2026-07-31; tag `7.0.18` on GitHub, artifact on Modrinth) |
| MC compatibility | Modrinth title: "WorldGuard 7.0.18 (MC 26.1-26.2)"; game versions 26.1, 26.1.1, 26.1.2, 26.2. (7.0.17 also covered 1.21.11.) |
| Download | https://cdn.modrinth.com/data/DKY9btbd/versions/btHBavWa/worldguard-bukkit-7.0.18.jar (sha1 `2fb02cb19397c3b44433c4199c1dbc14144eb99b`) |
| License | LGPL-3.0 (LICENSE.txt in repo) |
| Caveat on 26.1.2 | None. Requires WorldEdit 7.4.x on the same server. |
| Source | https://modrinth.com/plugin/worldguard, https://github.com/EngineHub/WorldGuard |

### PlaceholderAPI

| | |
|---|---|
| Version | **2.12.3** (GitHub release 2026-07-03) |
| MC compatibility | Modrinth: 1.8 -> 26.2 (74 versions); Hangar PAPER platform includes 26.1 - 26.2. |
| Download | https://github.com/PlaceholderAPI/PlaceholderAPI/releases/download/2.12.3/PlaceholderAPI-2.12.3.jar (sha256 `fde03259f5af6938f3c33eeb4d814000a1adabf1d2304ce14970be81f609a437`) |
| License | GPL-3.0 |
| Caveat on 26.1.2 | None. Release notes warn that 26.2 was "considered experimental on Paper" at release time and that Paper's new version scheme broke PAPI's version parser (fixed in 2.12.3 via PR #1227). |
| Source | https://github.com/PlaceholderAPI/PlaceholderAPI/releases/tag/2.12.3 |

### CoreProtect (Community Edition)

| | |
|---|---|
| Version | **24.0** (GitHub release v24.0, 2026-07-07; the GitHub release carries no jar, the author distributes via Modrinth/Hangar/Patreon) |
| MC compatibility | Release notes: "Added support for Minecraft 26.1"; "Changed minimum supported MC version to 1.16.5". Modrinth: 1.16.5 -> 26.1.2 (33 versions). Hangar PAPER platform tops out at 26.1.2. |
| Download | https://cdn.modrinth.com/data/Lu3KuzdV/versions/Kma0kBsY/CoreProtect-CE-24.0.jar (sha1 `e8425e63e9f22999b090beafbc32bef46d91eb22`) |
| License | Artistic-2.0 |
| Caveat on 26.1.2 | None for 26.1.2. For 26.2: the README says "Downloads for MC 1.14 - 26.2", but the author's own Modrinth/Hangar listings stop at 26.1.2, and PR #990 "Recognize Paper 26.2 platform marker" (merged 2026-09-13, after v24.0) shows v24.0 fails to detect Paper 26.2 as Paper and falls back to the generic Bukkit adapter. So v24.0 on 26.2 is degraded, not proven broken. |
| Source | https://github.com/PlayPro/CoreProtect/releases/tag/v24.0, https://github.com/PlayPro/CoreProtect/pull/990, https://modrinth.com/plugin/coreprotect |

### spark

| | |
|---|---|
| Version | **Bundled with Paper; no separate jar needed.** Paper 26.1.2 ships `me.lucko:spark-paper:1.10.152` (Paper 26.2 ships 1.10.180). |
| MC compatibility | Whatever Paper build it is bundled in. |
| Download | n/a. Paper docs: "Starting with 1.21, Paper bundles the spark profiler". To override with a newer standalone jar, drop it in `plugins/` and set `-Dpaper.preferSparkPlugin=true`. spark's own Modrinth page only publishes Fabric/Forge/NeoForge jars (latest 1.10.185, 2026-09-12); the Bukkit jar comes from spark.lucko.me / Jenkins and is not a versioned "release", which is another reason to rely on the bundled copy. |
| License | GPL-3.0 |
| Caveat on 26.1.2 | None. Paper's build file carries a TODO about JetBrains Runtime 25 having issues with spark; use a standard JDK 25. |
| Source | https://docs.papermc.io/paper/profiling/, https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-server/build.gradle.kts (lines 157-159) |

### Chunky

| | |
|---|---|
| Version | **1.5.3** (2026-05-04; Hangar + Modrinth release channel; no GitHub Releases, tag `1.5.0` is the newest tag) |
| MC compatibility | Hangar PAPER: 26.1, 26.1.1, 26.1.2. Modrinth: 26.1 -> 26.2 (26.2 tag added after the fact). 1.4.40 remains for 1.21.x. |
| Download | https://hangarcdn.papermc.io/plugins/pop4959/Chunky/versions/1.5.3/PAPER/Chunky-Bukkit-1.5.3.jar (same file on Modrinth: https://cdn.modrinth.com/data/fALzjamp/versions/MdY6JATr/Chunky-Bukkit-1.5.3.jar, sha1 `7ff47ee3afec89a1725e6eef393f373c549eff3b`) |
| License | GPL-3.0 |
| Caveat on 26.1.2 | None. |
| Source | https://hangar.papermc.io/pop4959/Chunky, https://modrinth.com/plugin/chunky |

### Stack-gap matrix

Author-stated support by MC version, using each plugin's newest stable release
(Y = listed by the author; dev = only in unreleased dev builds; ? = no statement):

| Plugin (stable) | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|
| Paper channel | STABLE (UNSUPPORTED since 2026-06-15) | STABLE | STABLE | ALPHA only |
| EssentialsX 2.22.0 | Y | Y | **dev only** | dev only |
| LuckPerms 5.5.x | Y | Y | Y | ? |
| Vault 1.7.3 | ? | ? | ? | ? |
| WorldEdit 7.4.5 | Y | Y | Y | ? |
| WorldGuard 7.0.18 | (7.0.17) | Y | Y | ? |
| PlaceholderAPI 2.12.3 | Y | Y | Y | ? |
| CoreProtect 24.0 | Y | Y | **partial** (README yes; Modrinth/Hangar no; Paper detection fixed post-release) | ? |
| spark (bundled) | Y | Y | Y | Y |
| Chunky 1.5.3 | (1.4.40) | Y | Y | ? |

## Paper facts

### Java

- Paper 26.1 and newer: **Java 25**. Paper 1.20 - 1.21.11: Java 21.
  (docs.papermc.io/paper/getting-started/ table; the downloads API reports
  `"java":{"version":{"minimum":25}}` for 26.1.2, 26.2 and 26.3, and
  `minimum: 21` for 1.21.11.)
- Recommended JVM flags are returned by the API per version
  (`version.java.flags.recommended`); they are the standard Aikar G1 set.

### Maven coordinates for `paper-api`

- Repository: `https://repo.papermc.io/repository/maven-public/`
- Artifact: `io.papermc.paper:paper-api`
- Version string format, **26.1 and newer**: `<MC>.build.<BUILD>-<channel>`, e.g.
  `26.1.2.build.74-stable`, `26.2.build.124-stable`, `26.3.build.8-alpha`.
  The docs' example is `io.papermc.paper:paper-api:26.2.build.121-stable`, or
  `26.2.build.+` to float on the newest build of a version.
- Version string format, **1.21.11 and older**: `{VERSION}-R0.1-SNAPSHOT`, e.g.
  `1.21.11-R0.1-SNAPSHOT`. The docs note this form had "no way to reference a
  specific build".
- Verified against `maven-metadata.xml` on 2026-09-16: `26.1.2.build.*`,
  `26.2.build.83-stable` ... `26.2.build.124-stable`, `26.3.build.3-alpha` ...
  `26.3.build.8-alpha`, and `1.21.11-R0.1-SNAPSHOT` are all present.
- Gradle toolchain in the docs example: `JavaLanguageVersion.of(25)`.

Recommended pin for this project: `io.papermc.paper:paper-api:26.1.2.build.74-stable`.

### Downloads API

- Current API: **`https://fill.papermc.io/v3`** ("Fill"). The old
  `https://api.papermc.io/v2` now returns
  `{"ok":false,"error":"sunset","message":"This API version has been sunset..."}`.
- A **non-generic `User-Agent` that identifies the software and includes a
  contact URL or email is required** (docs). Example:
  `avian-factions/1.0 (https://github.com/elirtf/avian-factions)`.
- Endpoints:
  - List versions: `GET /v3/projects/paper`
  - Version metadata (support status, Java min, build ids):
    `GET /v3/projects/paper/versions/{version}`
  - List builds: `GET /v3/projects/paper/versions/{version}/builds`
  - Latest build: `GET /v3/projects/paper/versions/{version}/builds/latest`
  - Specific build: `GET /v3/projects/paper/versions/{version}/builds/{build}`
  - The jar URL is `.downloads["server:default"].url` in the build object and
    points at `https://fill-data.papermc.io/v1/objects/<sha256>/<name>.jar`.
- Example, fetching the pinned build:

  ```sh
  curl -s -A "avian-factions/1.0 (https://github.com/elirtf/avian-factions)" \
    https://fill.papermc.io/v3/projects/paper/versions/26.1.2/builds/74
  # -> {"id":74,"time":"2026-07-06T16:51:09Z","channel":"STABLE", ...,
  #     "downloads":{"server:default":{"name":"paper-26.1.2-74.jar",
  #       "checksums":{"sha256":"1d70b1da...95e5f7"},"size":52893229,
  #       "url":"https://fill-data.papermc.io/v1/objects/1d70b1da...95e5f7/paper-26.1.2-74.jar"}}}
  ```

- A GraphQL endpoint also exists at `https://fill.papermc.io/graphql`.

### Version/channel snapshot (2026-09-16)

| Version | Support status | Latest build | Channel | First STABLE build | Java min |
|---|---|---|---|---|---|
| 26.3 | SUPPORTED | 8 (2026-09-16) | ALPHA | none yet | 25 |
| 26.2 | SUPPORTED | 124 (2026-09-15) | STABLE | 83 (2026-07-26) | 25 |
| 26.1.2 | SUPPORTED | 74 (2026-07-06) | STABLE | - | 25 |
| 1.21.11 | UNSUPPORTED (end 2026-06-15) | 132 (2026-05-11) | STABLE | - | 21 |

## Caveats and gaps

1. **EssentialsX is the blocker for 26.2.** Stable 2.22.0 stops at 26.1.2; 26.2
   (PR #6561, merged 2026-06-16) and 26.3 (PR #6624, merged 2026-09-15) support
   exist only on the `2.x` branch / dev builds. Watch
   https://github.com/EssentialsX/Essentials/releases for the next tag.
2. **CoreProtect 24.0 on 26.2 is ambiguous.** README claims 1.14 - 26.2; the
   author's Modrinth/Hangar listings and release notes say 26.1; PR #990 shows
   the Paper-platform detection did not recognise 26.2 until after the release.
   Treat 26.2 as unsupported by a stable CoreProtect until v24.1/25.0 ships.
3. **Vault has no compatibility statement for anything past 1.13 API** and no
   release since 2020. It loads via `api-version: 1.13` and is widely used, but
   the project cannot fix regressions upstream. Consider this an accepted risk
   or evaluate a maintained fork if a breakage appears.
4. **Paper 1.21.11 is out of support** (end 2026-06-15), so falling back to the
   1.21 line to avoid Java 25 is not an option.
5. **Java 25 is mandatory** for every 26.x Paper build. Paper's build script
   notes issues with the JetBrains Runtime 25 and spark; use a mainstream JDK 25
   (Temurin etc.).
6. **spark is bundled**; do not add a standalone spark jar unless a newer
   version is specifically needed (then set `paper.preferSparkPlugin=true`).
7. **Modrinth "game versions" can be edited after release** (Chunky 1.5.3 and
   the 26.2 tags on several May/June releases show this). Where the Hangar
   range and Modrinth range disagree, the narrower one is recorded above.
8. The public essentialsx.net, luckperms.net and spark.lucko.me pages render
   versions with JavaScript; the static HTML shows placeholders ("??",
   "vunknown") or stale text ("1.21.8"). The machine-readable sources
   (GitHub releases, `metadata.luckperms.net`, Modrinth/Hangar APIs) were used
   instead.

## Sources

Paper

- https://fill.papermc.io/v3/projects/paper (version list)
- https://fill.papermc.io/v3/projects/paper/versions/26.3 , /26.2 , /26.1.2 , /1.21.11 (support status, Java minimum, build ids)
- https://fill.papermc.io/v3/projects/paper/versions/26.3/builds , /26.2/builds (channels per build)
- https://fill.papermc.io/v3/projects/paper/versions/26.1.2/builds/latest (build 74 download + sha256)
- https://api.papermc.io/v2/projects/paper (sunset response)
- https://docs.papermc.io/misc/downloads-api/ (base URL, User-Agent requirement, GraphQL)
- https://docs.papermc.io/paper/getting-started/ (Java version table)
- https://docs.papermc.io/paper/dev/project-setup/ (Maven repo, coordinates, `26.2.build.121-stable` / `26.2.build.+`, legacy `{VERSION}-R0.1-SNAPSHOT` note, `JavaLanguageVersion.of(25)`)
- https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml (published version strings)
- https://docs.papermc.io/paper/profiling/ (spark bundled since 1.21, `paper.preferSparkPlugin`)
- https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-server/build.gradle.kts and .../ver/26.2/... (bundled `spark-paper` versions)

EssentialsX

- https://github.com/EssentialsX/Essentials/releases/tag/2.22.0 (release notes, supported versions, asset digests)
- https://github.com/EssentialsX/Essentials/pull/6561 (Update to Minecraft 26.2, merged 2026-06-16)
- https://github.com/EssentialsX/Essentials/pull/6624 (Update to Minecraft 26.3, merged 2026-09-15)
- GitHub compare `2.22.0...ff54e649` (26.2 merge commit is 10 commits ahead of the tag)
- https://github.com/EssentialsX/Essentials/blob/2.x/README.md (master-branch support list incl. 26.2/26.3)
- https://api.modrinth.com/v2/project/essentialsx/version , https://hangar.papermc.io/api/v1/projects/EssentialsX/Essentials/versions

LuckPerms

- https://metadata.luckperms.net/data/all (current version 5.5.84 + download URLs)
- https://api.modrinth.com/v2/project/luckperms/version (v5.5.71-bukkit game versions)
- https://github.com/LuckPerms/LuckPerms (MIT license)

Vault

- https://github.com/MilkBowl/Vault/releases/tag/1.7.3
- https://github.com/MilkBowl/Vault/blob/master/plugin.yml (`api-version: 1.13`)
- https://github.com/MilkBowl/Vault/issues/941

WorldEdit / WorldGuard

- https://hangar.papermc.io/api/v1/projects/EngineHub/WorldEdit/versions
- https://api.modrinth.com/v2/version/F5ea2ov3 (WorldEdit 7.4.5) , https://api.modrinth.com/v2/version/btHBavWa (WorldGuard 7.0.18)
- https://github.com/EngineHub/WorldEdit/blob/master/LICENSE.txt , https://github.com/EngineHub/WorldGuard/blob/master/LICENSE.txt

PlaceholderAPI

- https://github.com/PlaceholderAPI/PlaceholderAPI/releases/tag/2.12.3
- https://api.modrinth.com/v2/version/pIvQcXW8 , https://hangar.papermc.io/api/v1/projects/HelpChat/PlaceholderAPI/versions

CoreProtect

- https://github.com/PlayPro/CoreProtect/releases/tag/v24.0
- https://github.com/PlayPro/CoreProtect/pull/990 (Recognize Paper 26.2 platform marker)
- https://github.com/PlayPro/CoreProtect/blob/v24.0/README.md
- https://api.modrinth.com/v2/version/Kma0kBsY , https://hangar.papermc.io/api/v1/projects/CoreProtect/CoreProtect/versions

spark

- https://docs.papermc.io/paper/profiling/
- https://api.modrinth.com/v2/project/spark/version (mod-loader jars only)
- https://spark.lucko.me/download (static text: "spark is pre-bundled with Paper 1.21+")

Chunky

- https://hangar.papermc.io/api/v1/projects/pop4959/Chunky/versions
- https://api.modrinth.com/v2/version/MdY6JATr

Modrinth game-version tag list (26.3 added 2026-09-15): https://api.modrinth.com/v2/tag/game_version

All eleven download URLs above returned HTTP 200 with a jar content type on 2026-09-16.
