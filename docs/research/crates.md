# Crates: CrazyCrates, and the command surface we need before it is useful

Written 2026-09-22. Resolves #23. Spec §19.

## Choice: CrazyCrates

| | CrazyCrates | ExcellentCrates |
|---|---|---|
| 26.1.2 build | **Yes** — `26.1.2-3726eba`, published 2026-09-19 | No — tops out at 1.21.11 |
| Licence | MIT | GPL-3.0-only |
| Downloads (Modrinth) | 318k | 123k |

ExcellentCrates is out on version support alone. CrazyCrates publishes a build named for our exact
Minecraft version, three days old at time of writing, under MIT — the most permissive licence
anything in our stack carries.

Spec §19's requirements are all standard crate-plugin features and CrazyCrates has them: weighted
rewards, animations, sounds, particles, configurable reward tables, key consumption. It ships
several example crates (`CrateExample.yml`, `AdvancedExample.yml`, `CasinoCrate.yml`,
`CosmicCrate.yml`) showing the range.

## 1. Can reward tables grant *our* things?

**Yes, through commands — and this is the finding that matters.** Crate prizes support a
`Prize-Commands` list and per-prize `commands`, run from console with `%player%` substituted, and
PlaceholderAPI placeholders available.

That means **any** Avian reward can be a crate prize, *provided a console command exists to grant
it*. And today, most do not:

| Reward | Command exists? |
|---|---|
| Vanilla items | Yes — vanilla `give` |
| Money | Yes — EssentialsX `eco give` via our Vault bridge |
| **Tokens** | **No** |
| **Gems** | **No** |
| **Spawners** | Partly — via EconomyShopGUI/RoseStacker, needs checking |
| **Harvester Hoe** (#18) | No — the item does not exist yet |
| Faction power / upgrades | No |

**So the blocker is not the crate plugin, it is our missing admin command surface.** Before crates
can pay out anything Avian-specific we need something like:

```
/tokens give <player> <amount> [reason]
/gems give <player> <amount> [reason]
```

which is small work — `Economy.deposit` already exists, audited, with a reason parameter. It is
filed separately. Note the reason parameter earns its keep here: every crate payout lands in
`economy_transactions` with `reason = "crate:talon"` or similar, so the audit trail explains where
a player's tokens came from.

## 2. Can keys be granted programmatically?

Yes, by console command (`crazycrates give …`), which is how bosses, KOTH and the Aerie will hand
them out — each already needs to run *something* on a win, and a console command is the simplest
contract. `BukkitKeyManager` is also reachable through `CrazyCratesPaper` for a typed route if the
command form proves awkward.

## 3. Seven tiers?

Yes — crates are one config file each, so the count is ours to choose. Feather, Plume, Talon,
Pinion, Raven, Phoenix, Flock is seven files.

## 4. The earned-versus-store split — no longer a problem

CrazyCrates does not distinguish a key a player earned from one they bought. That was flagged here
as a gap while ADR-0006 required store keys to be cosmetic-only. **It is not a gap any more.**

The ADR was revised: nothing sold may be *exclusive*, but the same Talon key may drop from a boss
and sell in the store, because what money buys is the shortcut rather than the contents. One key,
one reward table, either route. The plugin's inability to tell them apart is now irrelevant, and a
rule that would have lived on discipline alone has disappeared instead of being mitigated.

What remains is lighter and checkable: for every key sold, **write down the in-game route and its
rate**. If the free rate turns out to be nominal, raise the drop rate rather than pulling the key
from the store.

## Recommendation

Adopt CrazyCrates. Install it now so the infrastructure is in place, but **leave the reward tables
empty of Avian-specific rewards until the token and gem commands exist** — a crate that cannot pay
out tokens is not much of a crate on a server whose progression currency is tokens.

Order: admin commands → crate reward tables → wire bosses/KOTH/Aerie to grant keys.

## Sources

- [CrazyCrates on Modrinth](https://modrinth.com/plugin/crazycrates) — version `26.1.2-3726eba`
- [ExcellentCrates on Modrinth](https://modrinth.com/plugin/excellentcrates) — version support checked
- `CrazyCratesPaper`, `BukkitKeyManager` and the bundled `crates/*.yml` read from the jar
