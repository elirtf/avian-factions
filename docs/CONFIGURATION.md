# What you can change, and where

Plain-language guide to every setting. No code needed — these are all text files you open, edit
and save.

**The one rule:** after changing anything, **restart the server** with `./dev restart`. There's no
reload command yet. Running the server is covered in [`DEVELOPING.md`](DEVELOPING.md).

---

## Where the files live

Everything is inside the `run/` folder, next to this project.

```
run/
  server.properties          ← Minecraft's own settings
  plugins/
    AvianFactions/           ← OUR settings (5 files)
      core.conf              ← server name, database
      factions.conf          ← extras on top of FactionsUUID (base power, upgrades)
      combat.conf            ← how fighting feels
      economy.conf           ← money, starting balances
      ftop.conf              ← what spawners and blocks are worth on F-Top
      trails.conf            ← particle trails for /trails (particle, colours, name)
    Essentials/config.yml    ← homes, teleporting, warps
    FactionsUUID/config/     ← factions, land, power, protection, role names
    CommandTimer/timers/     ← when raiding is allowed
    EconomyShopGUI/          ← shop prices and menus
    RoseStacker/config.yml   ← how many mobs/spawners stack
    WorldGuard/              ← protected areas like spawn
```

> **Careful:** the `run/` folder is not saved to git. If you wipe it, your edits are gone. Settings
> we want to keep forever get copied into the `dev-server/` folder — ask and it can be added there.

---

## "I want to change…"

### The server's name, or the database

**File:** `run/plugins/AvianFactions/core.conf`

| Setting | What it does | Default |
|---|---|---|
| `server.name` | The name shown in messages | `Avian Factions` |
| `server.address` | The address players type | `mc.avian.club` |
| `database.host` | Where the database lives | `127.0.0.1` |
| `database.port` | Database port number | `3306` |
| `database.name` | Which database to use | `avian` |
| `database.user` | Database username | `avian` |
| `database.pool-size` | How many database connections at once | `8` |

The database **password** is not in this file on purpose — it lives in a separate file called
`.env` so it never gets shared by accident. Copy `.env.example` to `.env` and put the password there.

---

### How much land a faction can claim, and how power works

Factions come from the **FactionsUUID** plugin. Most settings are in its files; a few extras are ours.

**Power** decides how much land a faction can hold: **one power = one chunk**. A faction's power is
all its members' power added up, plus a flat bonus. Die, and you lose some. If a faction's land
goes above its power, it becomes **raidable**: enemies can claim over it.

**File:** `run/plugins/FactionsUUID/config/main.conf`, in the `landRaidControl` → `power` section

| Setting | What it does | Ours |
|---|---|---|
| `playerStarting` | Power a new player begins with | `20` |
| `playerMax` | Most power one player can have | `20` |
| `playerMin` | Lowest a player can drop to | `0` |
| `powerPerMinute` | Power gained per minute **while online** (`0.0166667` = 1 an hour) | `0.0166667` |
| `lossPerDeath` | Power lost each death | `2` |
| `raidability` | Land above power makes a faction raidable | `true` |
| `factionMemberLimit` (in `other`) | Most members a faction can have. A hard cap: the `max_members` upgrade stays off | `10` |

**File:** `run/plugins/AvianFactions/factions.conf` (ours)

| Setting | What it does | Default |
|---|---|---|
| `faction-base-power` | Flat power every **new** faction gets. Never lost on death | `5` |
| `enabled-upgrades` | Which faction upgrades are on (`/f upgrades`). `warps` must stay: it sets the warp limit (5) | claim_boost, spawner_rate, crop_yield, growth, mob_exp, tnt_bank, warps |
| `chunk-buster.min-distance-from-spawn` | Chunk busters do nothing closer than this to spawn (flat distance) | `1000` |
| `chunk-buster.grant-on-join` | Chunk busters a player gets the first time they found or join a faction. Once per player, ever, so they can't be farmed | `5` |
| `netherite-alert.enabled` | Tell the whole server when someone mines ancient debris (placed debris never counts) | `true` |
| `netherite-alert.cooldown-seconds` | Seconds before the same player is announced again, so a vein is one message | `60` |

> **Example:** a solo player has 5 base + 20 = **25 chunks**, a 5×5 square. A 4-player faction has
> 5 + 80 = **85 chunks**. The **Claim Boost** upgrade adds +10, +30 or +70 more.

Upgrade prices and levels live in `run/plugins/FactionsUUID/data/universe.json`. Edit that only
while the server is **stopped**.

Role names (Leader, Co-Leader, Officer, Member, Recruit) and every message are in
`run/plugins/FactionsUUID/config/translations.conf`.

---

### When raiding is allowed

Outside raid hours, FactionsUUID's **grace** is switched on and **no explosions happen anywhere**.
The **CommandTimer** plugin switches it on and off:

| Days | Raiding allowed |
|---|---|
| Monday to Friday | 20:00 to 23:00 |
| Saturday and Sunday | 18:00 to midnight |

The times are in **server time**. Each rule is a file in `run/plugins/CommandTimer/timers/`. A
`raid-close-…` file switches grace on for however long it is until the next window opens, and a
`raid-open-…` file switches it off. To change the hours, edit both the times and the grace lengths
(like `fa set grace on 21h`) so they still meet up.

By hand, in the console: `fa set grace on 3h` stops explosions for three hours, `fa set grace off`
allows them again, and `f grace` shows the current state.

---

### F-Top: what a faction's base is worth

**File:** `run/plugins/AvianFactions/ftop.conf`

`/ftop` ranks factions by the **spawners** and **valuable blocks** inside their claims. Hover over
a line to see the breakdown. The ranking is recalculated every few minutes. Staff can force it with
`/ftop recalc`.

| Setting | What it does | Default |
|---|---|---|
| `recalculate-minutes` | How often the ranking updates | `5` |
| `spawner-values` | Worth of one spawner, by mob | zombie/spider 250k, skeleton 300k, blaze 400k, creeper 500k, enderman 600k, iron golem 1M |
| `block-values` | Worth of one placed block. Only these blocks count | gold 250, diamond 500, emerald 750 |
| `aging.starting-percent` | New spawners and blocks start at this share of their value | `10` |
| `aging.hours-to-full-value` | …and grow to full value over this many hours | `72` |
| `pickup-cost.grace-minutes` | Minutes after placing when spawners can be picked up free | `5` |
| `pickup-cost.percent-of-value` | After that, breaking your own spawner costs this % of its value | `50` |

> **Why aging and the pickup cost:** without them a faction can buy spawners the night before a
> payout, or mine its spawners up to hide them when a raid starts. Explosions never pay the
> pickup cost, so raiders take spawners for free.

Only spawners **placed by players** count. Naturally generated dungeon spawners never do.

---

### How fighting feels

**File:** `dev-server/plugins/AvianFactions/combat.conf` — edit this one, not the copy in `run/`: every
start copies `dev-server/` over `run/`, so a change made in `run/` is lost at the next restart.

The quickest change is `preset`:

- `CLASSIC` — old-school 1.8 combat
- `COMPETITIVE` — slightly tighter, "practice server" feel
- `CUSTOM` — use your own numbers from the `knockback` section below

**CLASSIC and COMPETITIVE ignore the knockback numbers.** To tune your own, copy a preset's numbers
into the `knockback` block and set `preset = CUSTOM`.

| Setting | What it does | Default |
|---|---|---|
| `disable-attack-cooldown` | `true` = no waiting between hits (old style) | `true` |
| `attack-speed` | How fast you can swing. 40 is instant, 4 is modern | `40` |
| `disable-sweep-attack` | Stops swords hitting several things at once | `true` |
| `hit-delay-ticks` | How often a hit can land. A hit counts every **half** this many ticks: 20 = vanilla, 16 = a bit faster (our setting), 14 = fast. Lower = easier combos | `16` |
| `knockback.horizontal` | How far back a hit pushes someone | `0.4` |
| `knockback.vertical` | How far up a hit pushes someone | `0.4` |
| `knockback.vertical-limit` | Cap on upward push, stops people flying | `0.4` |
| `knockback.extra-horizontal` | Extra push when sprinting or using Knockback enchant | `0.5` |
| `knockback.extra-vertical` | Extra lift when sprinting | `0.1` |
| `knockback.respect-knockback-resistance` | Does netherite armour reduce knockback? | `true` |

> **How to tune:** bigger `horizontal` = people fly further back. Bigger `vertical` = fights get
> floatier. This needs two people hitting each other — you cannot judge it alone.

---

### Money and prices

**Money is whole dollars.** No cents anywhere.

**File:** `run/plugins/AvianFactions/economy.conf`

| Setting | What it does | Default |
|---|---|---|
| `starting-money` | Money a new player gets | `0` |
| `starting-tokens` | Tokens a new player gets | `0` |
| `starting-gems` | Gems a new player gets | `0` |
| `provide-vault` | Share our money with other plugins. Leave `true` | `true` |
| `sell-values` | Backup price list, used only if the shop plugin is missing | a starter list |

**Shop prices live somewhere else.** Because EconomyShopGUI is installed, *it* owns prices:

**Folder:** `run/plugins/EconomyShopGUI/shops/`

One file per shop category. Edit prices there and both the shop **and** the Harvester Hoe's
auto-sell use the new price — they read the same list on purpose, so they can never disagree.

---

### How many mobs and spawners stack

**File:** `run/plugins/RoseStacker/config.yml`

| Setting | What it does | Default |
|---|---|---|
| `global-spawner-settings.max-stack-size` | Most spawners in one stack | `32` |
| `global-entity-settings.max-stack-size` | Most mobs in one stack | `128` |
| `global-entity-settings.merge-radius` | How close mobs must be to stack | `5` |
| `global-item-settings.max-stack-size` | Most dropped items in one stack | `1024` |
| `global-block-settings.stacking-enabled` | Whether placed blocks stack. **Off**: only spawners stack | `false` |
| `global-spawner-settings.explosion-protection` | Spawners immune to TNT? **Off**, so spawners can be raided | `false` |
| `global-spawner-settings.explosion-amount-percentage` | Share of a blown-up stack that drops as spawner items | `75` |
| `global-spawner-settings.explosion-destroy-remaining` | The rest of the stack is destroyed, not left behind | `true` |

> **Raiding a spawner stack:** TNT drops 75% of it as spawner items (rounded up) and destroys the
> rest. A stack of 100 drops 75. Stacks of 1 to 3 lose nothing, because of the rounding up.

> **32 spawners is low for this kind of server.** Raising it means bigger spawner bases and bigger
> faction values — but also more mobs for the server to handle, so raise it a bit at a time and
> watch performance with `/spark tps`.

---

### Homes, teleports, warps, kits

**File:** `run/plugins/Essentials/config.yml`

This is EssentialsX, a plugin we did not write, and it has hundreds of settings. The common ones:

| Setting | What it does |
|---|---|
| `teleport-cooldown` | Seconds between teleports |
| `teleport-delay` | Seconds of standing still before a teleport happens |
| `unsafe-enchantments` | Already `true` for us — allows above-normal enchant levels |

How many **homes** a player gets is set by their rank, not here — see below.

---

### Ranks and permissions

**File:** `dev-server/luckperms/ranks.lp` — this one *is* saved in git.

This is the master list of every rank, its colour, and what it can do. Ranks are:

- **Free:** Hatchling → Fledgling (Fledgling comes automatically after 12 hours of playtime, not counting
  AFK: `dev-server/plugins/PlayTimeManager/Goals/fledgling.yml`)
- **Paid:** Harpy → Griffin → Wyvern → Dragon. **Phoenix** tops the ladder but is never sold: winning F-Top
  earns it, and the winning faction's leader picks who gets it
- **Playtester:** a badge for pre-launch testers, given by hand (`lp user <name> parent add playtester`)
- **Staff:** Helper → Mod → Admin → Owner

To change ranks, edit that file, then run:

```sh
./gradlew -q printRanks
```

and paste the output into the server console. Editing the file alone does nothing until you do that.

> **Important rule:** nothing sold for money may be *exclusive*. Selling something is fine as long
> as a player who never pays can also get it — paying just makes it faster. So paid ranks can
> include kits and gear, and crate keys can be sold, because the same kits and keys are winnable
> from crates, bosses and voting; it simply takes longer. What is never allowed is selling
> something with no way to earn it at all. Whenever something goes in the store, write down how a
> free player gets it and how often. See `docs/adr/0006-nothing-sold-is-exclusive.md`.

---

### Protected areas like spawn

Done with commands in game, not a file. Stand in the area and use WorldGuard:

```
/rg define spawn
/rg flag spawn pvp deny
/rg flag spawn build deny
/rg flag spawn natural-hunger-drain deny
```

The live `spawn` region (the flags it has today) is in `run/plugins/WorldGuard/worlds/world/regions.yml`.
It is world data, so it travels with `./dev backup` rather than git. `natural-hunger-drain deny`
means nobody gets hungrier at spawn (owner, 2026-09-29).

---

### The Minecraft version, Java version, world size

**File:** `gradle.properties` (in the project folder, not `run/`)

| Setting | What it does | Default |
|---|---|---|
| `mcVersion` | Minecraft version | `26.1.2` |
| `paperBuild` | Exact Paper build number | `74` |
| `javaVersion` | Java version | `25` |
| `mariadbImage` | Database version | `mariadb:11.8.9` |

> Do not change these casually — every plugin we use was checked against these exact versions.

**World border** (how far players can travel) is set in game, once per world:

```
worldborder set 5000
```

---

## If something goes wrong

**The server refuses to start and prints a list of errors.** That is deliberate. It will name the
file and the exact setting, like:

```
factions.conf → faction-base-power: must be a finite number >= 0 (got -5)
```

Fix the setting it names and start again. It will never start with a broken setting and silently
use a wrong value instead.

**A setting disappeared from a file after an update.** It did not — new settings get *added*
automatically when the server starts, with a note in the console saying what was added. Your own
values are kept.

**You changed something and nothing happened.** You probably need to restart. There is no reload
command yet.
