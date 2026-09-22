# Stacking: RoseStacker, and what we must read from it

Written 2026-09-22. Resolves #19. The decision not to build our own spawner stacking was the
owner's; this records which plugin, and the integration points our own code depends on.

## Choice: RoseStacker 1.5.42

| | RoseStacker 1.5.42 | StackMob 5.10.8 |
|---|---|---|
| Scope | Mobs, items, blocks **and spawners** | Mobs only |
| 26.1.2 | Yes (lists 26.1–26.2) | Yes |
| Licence | MIT-Non-Distribution | GPL-3.0 |
| Downloads | — | — |

RoseStacker, because it covers spawners too. StackMob would leave spawner stacking unsolved and put
us straight back into building it, which is the thing we decided not to do.

**Licence, read rather than assumed.** The file grants "the rights to use, copy, modify or merge
while excluding the rights to publish, (re)distribute, sub-license, and/or sell copies", and
extends the same limit to source and to "other means that can be used to emulate this work".
Running it on our own server is squarely permitted. We could never bundle it into anything we hand
out — the same position we are in with the rest of the stack, so it changes nothing practically.

Pinned by SHA-256 in `downloadPlugins` (`b125525c…`), from Modrinth's versioned CDN URL, so unlike
EconomyShopGUI this one is pinned twice over — the URL itself names the version.

## The integration point that matters

F-Top (§11) values factions by spawner count. If a base holds 500 spawners in stacks and we count
blocks, F-Top is wrong by orders of magnitude. **Verified present in 1.5.42:**

```java
RoseStackerAPI.getInstance().getStackedSpawner(block).getStackSize()   // int
RoseStackerAPI.getInstance().isSpawnerStacked(block)
RoseStackerAPI.getInstance().getStackedSpawners()                      // Map<Block, StackedSpawner>
```

`StackedSpawner` extends `Stack`, whose contract is `getStackSize()`, `getLocation()`,
`getStackSettings()`. There is an equivalent for entities (`getStackedEntity(LivingEntity)`) and
blocks. So when F-Top is built it asks RoseStacker for the count rather than counting blocks, and
degrades to 1-per-block if the plugin is absent.

## What it did for free

On first boot: `[EconomyShopGUI] RoseStacker found, integrating... Using RoseStacker as spawner
provider.` Buying and selling spawners through `/shop` now goes through RoseStacker's stacking
without us configuring anything. It also registered a PlaceholderAPI expansion, so stack counts are
available to scoreboards and holograms as placeholders.

## Defaults, and the one number that is a gameplay decision

| Setting | Default | Note |
|---|---|---|
| `global-spawner-settings.stacking-enabled` | `true` | Needs a full restart to change |
| `global-spawner-settings.max-stack-size` | **32** | See below |
| `global-spawner-settings.spawn-async` | `true` | Spawn condition checks off the main thread |
| entity `max-stack-size` | 128 | |
| entity `min-stack-size` | 2 | |
| entity `merge-radius` | 5 | |
| item `max-stack-size` | 1024 | |
| block `max-stack-size` | 2048 | |

**32 spawners per stack is low for an OP server.** Big spawner bases are the point of the genre,
and this number directly sets the ceiling on the spawner economy and therefore on F-Top values.
Left at the default deliberately — it is a balance decision like the combat presets, not a
technical one, and it belongs to the owner. Raising it also raises the mob load the server carries,
so it wants testing alongside `spawn-async` and entity `max-stack-size`.

## Still unverified

Whether a stack of N mobs drops N lots of loot and whether that routes through our sell multipliers
— it needs mobs actually dying, which needs a client. Worth checking alongside the other in-game
tests. If drops do not scale, the spawner economy is broken in a way no amount of reading the API
would reveal.

Breaking a stacked spawner in a claim should still fire `BlockBreakEvent`, where our protection
listener sees it, but that is also worth confirming in game rather than assumed.

## Sources

- [RoseStacker on Modrinth](https://modrinth.com/plugin/rosestacker)
- [RoseStacker LICENSE](https://raw.githubusercontent.com/Rosewood-Development/RoseStacker/master/LICENSE)
- `RoseStackerAPI` and `StackedSpawner` signatures read from the 1.5.42 jar with `javap`
