---
status: accepted
date: 2026-09-19
---
# Typed, per-module HOCON configuration via Configurate, loaded by core

Every module owns one HOCON file (`core.conf`, `factions.conf`, `combat.conf`, …, `messages.conf`) flat in the plugin data folder, mapped by **Configurate 4.2.0** (`configurate-hocon`, shaded + relocated) onto an immutable config object. Modules never load files themselves: each declares a `ConfigSpec<T>`, and `avian-core` loads and validates *all* specs in one phase before any module enables. Any error anywhere aborts the whole plugin, with every error across every file printed together. Modules read config through a `ConfigHandle<T>` and never cache values; `/avian reload [module]` swaps the handle's object atomically and fires callbacks on the main thread.

HOCON, not YAML, because Configurate's YAML loader cannot write per-key comments (its `saveInternal` dumps raw snakeyaml with `setProcessComments(false)`; upstream [#175](https://github.com/SpongePowered/Configurate/issues/175)/[#410](https://github.com/SpongePowered/Configurate/issues/410) are open) and would strip an admin's comments on every write-back. With HOCON, `@Comment` annotations are emitted and existing comments survive rewrites. The spec's `*.yml` tree (§3) is illustrative; §67/§77 are the binding requirements and this satisfies both.

**Considered:** Paper `YamlConfiguration` (no deps, but stringly-typed getters everywhere and hand-written validation for ~11 files); hand-rolled typed layer over it (reinvents Configurate); Configurate + YAML (comment loss, above); Java records as config types (Configurate's `RecordFieldDiscoverer` has no per-component defaults, so backfill can't work — use final classes with initialised fields instead).

**Consequences:** `configurate-core` + `configurate-hocon` (typesafe-config is already relocated inside it) + `geantyref` are shaded and relocated under `club.avian.factions.libs`; `checker-qual` is excluded (Paper bundles it). Adds ~1 MB to the jar. Config files are `.conf`; anyone expecting `.yml` reads the header comment core writes into every file.

## The pattern every module follows

1. **One file per module**, named after the module, flat in the data folder. `core.conf` holds server identity, DB, and the `mc.avian.club` placeholder. Secrets in `core.conf` are overridable by environment variables (`AVIAN_DB_PASSWORD`, `AVIAN_DB_USER`, `AVIAN_DB_URL`); Compose and CI never write passwords to disk.
2. **Config type = `@ConfigSerializable final class`**, `private` fields with initialisers, accessor methods, no setters. Nested sections are nested classes of the same shape. Every field has `@Comment`.
3. **Safe vs. dangerous defaults.** A field whose wrong value merely looks odd gets a default and is *backfilled*: when the key is missing from an existing file, the default is used, a `WARN` names file + key + value, and the merged file is saved with the new key and its comment. A field whose wrong value costs players or data (DB host, world border, anything in §74) is `@Required` and has no default: missing → error.
4. **Invariants** live in a `validate(Errors e)` method on the config class, called after mapping: `e.check(powerPerClaim > 0, "factions.power.per-claim", "must be > 0 (got %d)", powerPerClaim)`. Configurate's own `SerializationException` (type/required errors) is wrapped into the same `ConfigError(file, path, message)` shape.
5. **Startup is all-or-nothing.** Core collects every module's `ConfigSpec`, loads each file, aggregates every `ConfigError`, and if any exist prints them all as `factions.conf → factions.power.per-claim: must be > 0 (got -5)` then disables the plugin. Missing file → write the commented defaults and continue (the one place defaults are unconditionally safe). Never fail-on-first: an admin fixes ten typos in one restart.
6. **Read on use, never cache.** Modules get `ConfigHandle<T>`; `handle.get()` returns the current immutable object. Anything *derived* from config (a rescheduled timer, a rebuilt price table) is built in `handle.onReload(cfg -> …)`.
7. **`/avian reload [module]`** re-runs steps 3–4 on the named files (all, if none named). An invalid file is rejected wholesale — the old object stays live, errors go to the admin. Fields annotated `@RequiresRestart` keep their old value on reload and log `core.conf → database.pool-size changed; requires restart`. §77's never-reload list (DB connection, listeners, item registries, faction state) is not config and is untouched by construction.
8. **Versioning.** Every file has `config-version = N` (the version key of a `ConfigurationTransformation.versionedBuilder()`). Renames/restructures are registered as versioned transformations on the spec; on load, transformations run, the file is written back (original kept as `<file>.bak`), then mapped. Keys left over after migration → `WARN factions.conf: unknown key factions.power.regen (ignored)`.
9. **Threading.** Load and reload run synchronously on the main thread (a dozen small files parse in single-digit ms). `onReload` callbacks therefore may touch Bukkit API and must not do DB I/O.
10. **Messages** are not a typed config. One `messages.conf` on disk, string keys; each module ships a default bundle (`messages-<module>.conf` in the jar) that core merges in with the same backfill-and-warn rule. A `style { }` section defines tokens (`<prefix>`, `<gold>`, `<accent>`, …) expanded before MiniMessage parsing. Modules hold keys as constants (`FactionsMessages.CLAIM_SUCCESS = "factions.claim.success"`) and a startup test asserts every constant exists in the bundle. A missing key at runtime logs once and renders `<red>[missing: factions.claim.success]</red>` in-game. Always reloadable; never `@RequiresRestart`.

## Reference example (to be materialised by the scaffold ticket)

`avian-factions` declares its config:

```java
@ConfigSerializable
public final class FactionsConfig {

    @Comment("Bump only via a registered migration; never edit by hand.")
    private int configVersion = 1;

    private Power power = new Power();
    private Claims claims = new Claims();

    public Power power() { return power; }
    public Claims claims() { return claims; }

    @ConfigSerializable
    public static final class Power {
        @Comment("Max power each member contributes to the faction total.")
        private double perPlayer = 10;

        @Comment("Power a faction must hold per claimed chunk.")
        private double perClaim = 5;

        @Comment("Power lost by the dying player on death.")
        private double deathLoss = 2;

        @Comment("Power regained per player per minute while online.")
        private double regenPerMinute = 0.5;

        public double perPlayer() { return perPlayer; }
        public double perClaim() { return perClaim; }
        public double deathLoss() { return deathLoss; }
        public double regenPerMinute() { return regenPerMinute; }
    }

    @ConfigSerializable
    public static final class Claims {
        @Required
        @RequiresRestart
        @Comment("Worlds in which claiming is allowed. No default: an empty list disables factions.")
        private List<String> worlds;

        @Comment("Hard cap on claims per faction regardless of power.")
        private int maxPerFaction = 500;

        public List<String> worlds() { return worlds; }
        public int maxPerFaction() { return maxPerFaction; }
    }

    void validate(Errors e) {
        e.check(power.perClaim > 0, "factions.power.per-claim", "must be > 0 (got %s)", power.perClaim);
        e.check(power.deathLoss <= power.perPlayer, "factions.power.death-loss",
                "must be <= power.per-player (%s)", power.perPlayer);
        e.check(claims.maxPerFaction > 0, "factions.claims.max-per-faction", "must be > 0");
    }
}
```

```java
// In the module's bootstrap (exact hook is decided in #7):
static final ConfigSpec<FactionsConfig> CONFIG = ConfigSpec
        .of("factions.conf", FactionsConfig.class)
        .version(1)                                  // migrations: .migrate(2, t -> t.moveStrategy(...))
        .validate(FactionsConfig::validate)
        .build();
```

```java
// At a use site — read every time, never store the value:
double cost = config.get().power().perClaim();

// Derived state — rebuilt on reload, on the main thread:
config.onReload(cfg -> powerRegenTask.reschedule(cfg.power().regenPerMinute()));
```

Resulting `factions.conf` on a fresh install:

```hocon
# Avian Factions — factions module. Edit freely; `/avian reload factions` applies changes.
# Fields marked "requires restart" keep their old value until the server restarts.

# Bump only via a registered migration; never edit by hand.
config-version = 1

power {
    # Max power each member contributes to the faction total.
    per-player = 10.0
    # Power a faction must hold per claimed chunk.
    per-claim = 5.0
    # Power lost by the dying player on death.
    death-loss = 2.0
    # Power regained per player per minute while online.
    regen-per-minute = 0.5
}

claims {
    # Worlds in which claiming is allowed. No default: an empty list disables factions.
    # (requires restart)
    worlds = ["world"]
    # Hard cap on claims per faction regardless of power.
    max-per-faction = 500
}
```

Startup output when an admin sets `per-claim = -5` and deletes `worlds`:

```
[Avian] Configuration errors — refusing to start:
[Avian]   factions.conf → factions.power.per-claim: must be > 0 (got -5.0)
[Avian]   factions.conf → factions.claims.worlds: required, no value present
[Avian] Fix the above and restart. Nothing has been loaded.
```

`messages.conf` excerpt:

```hocon
style {
    prefix = "<dark_gray>[<gold>Avian</gold>]</dark_gray> "
    accent = "<#c9a227>"
    error  = "<red>"
}

factions {
    claim {
        success = "<prefix>Claimed <accent><chunk></accent> for <accent><faction></accent>."
        not-enough-power = "<prefix><error>Your faction needs <needed> more power to claim here."
    }
}
```
