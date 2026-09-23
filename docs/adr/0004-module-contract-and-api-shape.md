---
status: accepted
date: 2026-09-21
---
# Modules boot from an explicit list through a `ModuleContext`; `avian-api` holds only what a second module needs

A tiny `avian-plugin` Gradle module owns the single `JavaPlugin` (`club.avian.factions.AvianFactionsPlugin`), `plugin.yml`, and the shadow config, and constructs every module in a hand-ordered list. Each module implements `AvianModule` and receives a `ModuleContext` at `enable`: typed accessors for core's services and a typed service registry for cross-module services, gated by the module's declared `dependsOn`. Events are plain Bukkit events, commands are Paper Brigadier, an `enable` failure disables the whole plugin. `AvianPlayer` is a thin cached read view; every mutation goes through a service that takes a UUID and a reason. An interface enters `avian-api` only when a second module needs it.

Deliberately not overbuilt: no DI framework, no discovery, no custom event bus, no command framework. Each is a one-file swap later if the explicit version becomes painful once the server is running.

**Considered:** `ServiceLoader` discovery (needs topo sort; failures at runtime; solves a problem 14 fixed modules don't have); hand-wired constructor injection (clearest, but the wiring block grows quadratically and fights the "load all configs before any module enables" phase from ADR-0003); Guice/Dagger (weight and magic); a custom event bus (Bukkit's already has priority, cancellation, and third-party listeners); Cloud/ACF for commands (revisit if `/f`'s subcommand tree hurts); spec-literal `player.addMoney()` (hides the async/audit boundary every mutation must cross).

**Consequences:** core never depends on a gameplay module; the module list is the load order and is greppable; the Gradle boundary is mirrored at runtime by `require` refusing services from undeclared modules; tests hand a module a fake `ModuleContext`. Package stutter `club.avian.factions.factions` is accepted — it is the spec's own layout and the base package is locked.

## Module contract

```java
public interface AvianModule {
    String id();                                             // "factions", "economy", …
    default Set<Class<? extends AvianModule>> dependsOn() { return Set.of(); }
    default List<ConfigSpec<?>> configs() { return List.of(); }   // collected before any enable (ADR-0003)
    void enable(ModuleContext ctx);
    default void disable() {}
}
```

```java
public interface ModuleContext {
    Plugin plugin();                       // the one JavaPlugin, for Bukkit APIs that need it
    Logger logger();                       // prefixed "[Avian/factions]"
    <T> ConfigHandle<T> config(ConfigSpec<T> spec);
    Players players();                     // AvianPlayer cache + async load
    Database database();                   // DataSource + async executor; only repositories touch it
    Messages messages();                   // ADR-0003 §10
    AvianScheduler scheduler();            // thin wrapper over Paper's global/async schedulers
    Commands commands();                   // register(LiteralCommandNode) via LifecycleEvents.COMMANDS
    Events events();                       // register(Listener) = PluginManager#registerEvents
    Services services();                   // cross-module registry, below
}

public interface Services {
    <T> void provide(Class<T> api, T impl);   // by the providing module, in its enable
    <T> T require(Class<T> api);              // throws unless provider ∈ caller.dependsOn()
    <T> Optional<T> find(Class<T> api);       // soft dependency
}
```

Rules:

1. **Boot order** = list order in `AvianFactionsPlugin`. Core asserts every `dependsOn` appears earlier; otherwise boot fails naming both modules.
2. **Phases:** construct all → collect and load every `ConfigSpec` (ADR-0003, all-or-nothing) → `enable` in order → on any exception: log, `disable` the enabled ones in reverse, `setEnabled(false)`. Never a half-enabled plugin.
3. **Hold what you `require` in fields at `enable`.** No static registry access later; a module's dependencies are visible in one method.
4. **Events:** cross-module events are `org.bukkit.event.Event` subclasses in `avian-api` under `…api.event`, fired with `PluginManager#callEvent`; `isAsynchronous()` true when fired off-main.
5. **Commands:** Brigadier `LiteralCommandNode`s registered through `ctx.commands()`; no `plugin.yml` `commands:` block.
6. **Threading:** service methods that can hit the database return `CompletableFuture`; methods on the online cache are synchronous and main-thread only. Callbacks that touch Bukkit go back through `ctx.scheduler().sync(...)`.

## `AvianPlayer` and services

```java
public interface AvianPlayer {                // thin, cached, safe to hand around
    UUID uuid();
    String name();
    Optional<UUID> factionId();
    long balance();                           // minor units
    long tokens();
    PlayerStats stats();                      // kills, deaths, playtime, …
}

public interface Players {
    Optional<AvianPlayer> online(UUID uuid);                     // sync cache hit
    CompletableFuture<Optional<AvianPlayer>> load(UUID uuid);    // offline lookup
}

public interface Economy {
    CompletableFuture<TxResult> deposit(UUID player, long amount, String reason);
    CompletableFuture<TxResult> withdraw(UUID player, long amount, String reason);
    CompletableFuture<TxResult> transfer(UUID from, UUID to, long amount, String reason);
}
```

Profiles load in `AsyncPlayerPreLoginEvent` (login denied with a message if the load fails), live in the cache while online, and save asynchronously on quit. Permissions are Bukkit/LuckPerms (`Player#hasPermission`); nothing is duplicated. The `reason` on every mutation is what feeds the §66 audit trail.

## What `avian-api` holds on day one

By the second-module rule:

- **Contract:** `AvianModule`, `ModuleContext`, `Services`, `ConfigSpec`, `ConfigHandle`, `@RequiresRestart`.
- **Core services:** `Players`, `AvianPlayer`, `Economy`, `Tokens`, `Messages`, `AvianScheduler`, `Commands`, `Events`, `Database` (DataSource access for repositories only).
- *Superseded by ADR-0007: factions come from FactionsUUID and these interfaces were deleted; use its API.*
- **From factions** (needed by raiding, events, scoreboard, economy): `Territory at(World, int cx, int cz)` with `at(Location)`/`at(Block)` overloads, non-null with a wilderness singleton; `FactionService` read views (`Faction`, `Relation` ordered enum with `isAtLeast`, faction power on demand); `ProtectionPolicy` with pure `canBuild(actor, territory)`, `canUse(actor, territory, kind)`, `canDamage(actor, target)`, `denyExplode(source, territory, …)` ordered world → bypass → territory type → raid/shield → relation/role. Listeners live in `avian-factions` and only translate events into these calls.
- **Events:** `…api.event.faction.*` (claim, unclaim, relation change, disband), `…api.event.player.*` (profile loaded, balance changed).

Anything else stays package-private in its module until a second module asks.

## Packages

```
club.avian.factions                      AvianFactionsPlugin (avian-plugin)
club.avian.factions.api.module           AvianModule, ModuleContext, Services
club.avian.factions.api.config           ConfigSpec, ConfigHandle, RequiresRestart
club.avian.factions.api.player           AvianPlayer, Players, PlayerStats
club.avian.factions.api.economy          Economy, Tokens, TxResult
club.avian.factions.api.faction          Faction, FactionService, Territory, Relation, ProtectionPolicy
club.avian.factions.api.event.*          Bukkit event subclasses
club.avian.factions.core.<area>          core implementations
club.avian.factions.<module>.<area>      gameplay modules (factions, economy, combat, …)
club.avian.factions.libs.*               relocated third-party (Configurate, Hikari, Flyway, driver)
```
