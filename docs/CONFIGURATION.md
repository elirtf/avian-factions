# What you can change, and where

Plain-language guide to every setting. No code needed — these are all text files you open, edit
and save.

**The one rule:** after changing anything, **restart the server**. Type `stop` in the console, wait
for it to finish, then start it again. (There is no reload command yet.)

---

## Where the files live

Everything is inside the `run/` folder, next to this project.

```
run/
  server.properties          ← Minecraft's own settings
  plugins/
    AvianFactions/           ← OUR settings (4 files)
      core.conf              ← server name, database
      factions.conf          ← factions, land, power, protection
      combat.conf            ← how fighting feels
      economy.conf           ← money, starting balances
    Essentials/config.yml    ← homes, teleporting, warps
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

**File:** `run/plugins/AvianFactions/factions.conf`

**Power** is the number that decides how much land a faction can hold. Every player has some. Add
up everyone in the faction, divide by `per-claim`, and that is how many chunks they can own. Die,
and you lose some — drop below what you have claimed, and enemies can raid you.

| Setting | What it does | Default |
|---|---|---|
| `power.starting` | Power a new player begins with | `10` |
| `power.maximum` | Most power one player can have | `10` |
| `power.minimum` | Lowest a player can drop to | `0` |
| `power.regen-per-hour` | Power gained per hour **while online** | `1` |
| `power.death-loss` | Power lost each death | `2` |
| `power.per-claim` | Power needed for each chunk of land | `5` |
| `power.offline-decay-per-hour` | Power lost per hour while offline (`0` = off) | `0` |
| `power.offline-decay-grace-hours` | Hours offline before decay starts | `24` |

> **Example:** 4 players with 10 power each = 40 power. 40 ÷ 5 per-claim = **8 chunks of land**.
> Lower `per-claim` and everyone gets more land. Raise it and land gets scarcer.

**Faction names**

| Setting | What it does | Default |
|---|---|---|
| `names.min-length` | Shortest name allowed | `3` |
| `names.max-length` | Longest name allowed (16 is the hard limit) | `16` |
| `names.reserved` | Names nobody can take | wilderness, warzone, safezone, spawn, admin, avian |
| `names.blocked` | Words that make a name invalid | empty |

**Land**

| Setting | What it does | Default |
|---|---|---|
| `claims.worlds` | Which worlds allow claiming. Anywhere else is unprotected | `world` |
| `claims.map-radius` | How big `/f map` draws | `4` |

**Protection — who can touch what inside a claim**

| Setting | What it does | Default |
|---|---|---|
| `protection.non-member-allowed` | What outsiders may still do. Empty = nothing | empty |
| `protection.explosions-in-claims` | Can TNT and creepers break claimed blocks? | `false` |
| `protection.fire-spread-in-claims` | Can fire spread inside claims? | `false` |
| `protection.fluid-flow-into-claims` | Can lava/water flow in from outside? | `false` |
| `protection.bypass-permission` | Permission that ignores all protection (staff) | `avian.factions.bypass` |
| `protection.deny-message-cooldown-seconds` | Seconds between repeat "you can't do that" messages | `3` |

For `non-member-allowed` the options are: `BUILD`, `CONTAINER` (chests), `DOOR`, `SWITCH` (buttons
and levers), `ENTITY` (item frames, armour stands), `ITEM_USE` (buckets, flint and steel),
`DAMAGE_ENTITY` (hitting animals). Write them in a list, like `["DOOR"]` to let strangers open
doors but nothing else.

> Note: a faction that claims more land than its power allows becomes **raidable** — protection
> switches off for everyone until they unclaim or regain power. That is automatic, not a setting.

---

### How fighting feels

**File:** `run/plugins/AvianFactions/combat.conf`

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
| `hit-delay-ticks` | Invulnerable time after a hit. `0` = leave alone | `0` |
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

- **Free:** Hatchling → Fledgling
- **Paid:** Finch → Cardinal → Falcon → Hawk → Raven
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
```

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
factions.conf → power.per-claim: must be > 0 (got -5)
```

Fix the setting it names and start again. It will never start with a broken setting and silently
use a wrong value instead.

**A setting disappeared from a file after an update.** It did not — new settings get *added*
automatically when the server starts, with a note in the console saying what was added. Your own
values are kept.

**You changed something and nothing happened.** You probably need to restart. There is no reload
command yet.
