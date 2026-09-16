---
status: accepted
date: 2026-09-16
---
# One shaded plugin jar built from a multi-module Gradle project

The spec lists ~14 modules (core, factions, economy, …). We build each as its own Gradle subproject so boundaries are enforced at compile time (a module must declare a dependency to see another's API), but shade all of them into a single `AvianFactions.jar` with one `plugin.yml`. One jar avoids Bukkit `depend:` load-order issues, classloader-isolated singletons (DB pool, player cache), and ServicesManager plumbing between modules that will never ship separately.

**Considered:** one jar per module (real plugin modularity, but all the pain above for no benefit); single Gradle module with packages only (fastest start, boundaries erode).

**Consequences:** `avian-api` holds the public interfaces other modules compile against. If a module ever needs to ship separately, split it then — the Gradle boundary makes that cheap.
