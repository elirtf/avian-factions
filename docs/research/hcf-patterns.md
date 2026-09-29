# HCF patterns for Avian Factions

The owner asked (2026-09-29) how the server could feel more like **HCF** (Hardcore Factions). This
note lists what defines HCF, what we already have, and what each piece would cost.

**Sources.** FactionsUUID's capabilities come from its 4.7.0 source (`MainConfig.java` and the
command list), read on 2026-09-29. The HCF conventions below are the genre's widely known
defaults (small teams, DTR, deathbans, classes, KOTH, SOTW/EOTW). They were not checked against
any one server's docs, and every number here is a starting point to tune.

## What makes HCF, HCF

HCF is Factions compressed: small teams, short maps, and every death costs something.

| HCF pattern | What it is | Avian today | Cost to adopt |
|---|---|---|---|
| **DTR instead of power** | "Deaths until raidable": a faction goes raidable at DTR ≤ 0. Each member death costs 1 DTR, and regeneration **freezes** for a while after a death. | FactionsUUID 4.7.0 has it: `factions.landRaidControl.system = "dtr"`, with `freezeTime`, `lossPerDeath`, `maxDTR`, per-world multipliers and even DTR stealing ("vampirism"). We use `power` today. | **Config only.** It changes how claims and raids feel, so it's a season-start decision. |
| **Small factions** | Typically 5–10 members, and few or no allies. | FactionsUUID: `factionMemberLimit` (0 = unlimited today). | **Config only.** |
| **Deathban / lives** | Dying bans you for a set time (shorter for higher ranks), and "lives" revive you early. The core HCF tension. | Nothing. | **Build** (small: a ban on death with a timer, lives in MariaDB). Too harsh for our main realm; see "Recommendation". |
| **Combat tag + pearl cooldown** | A tag on hit, and logging out during it leaves a killable body. Ender pearl cooldown about 15 s. | Combat tag (20 s) with a killable logout body is built (`avian-combat`, #70). Gapple, notch apple and totem cooldowns are built. **No pearl cooldown.** | **Tiny:** a pearl entry in `ConsumableCooldowns` (`combat.conf`). |
| **Spawn, warzone, roads** | A safe spawn, a no-claim PvP warzone ring around it, and roads out to the border. | FactionsUUID `/f zone` makes safezone and warzone land, and WorldGuard is installed. Spawn protection is deferred until the separate spawn world exists. | **Config + building**, when spawn moves. |
| **KOTH / Conquest / Citadel** | Timed capture points that pay crate keys and faction points. Conquest has several points; Citadel is the weekly big one. | Planned (#24). The Aerie (#21) is our capture-point outpost. | **Build or adopt:** one capture engine covers KOTH, Conquest and the Aerie. |
| **SOTW / EOTW** | Start of the world: a PvP-off timer. End of the world: the border shrinks, everyone is raidable, and a final KOTH. | Seasons exist in the spec, and CommandTimer + `/f grace` already switch raiding on a schedule. | **Mostly config** (CommandTimer, `/f grace`, the world border). EOTW's "everyone raidable" needs one command. |
| **PvP timer for new players** | About 30 minutes of protection after first join, lost on attacking or entering the warzone. | Nothing. | **Build** (small), or find a plugin. |
| **Classes** | Armour sets give roles: **Bard** (holds items to give teammates Speed, Strength or Resistance on an energy bar), **Archer** (marks targets for bonus damage), **Rogue** (backstab), **Miner** (Haste, invisibility underground). | Nothing. AuraSkills covers the RPG grind, not team roles. | **Build** in `avian-combat`: the biggest item here, and the most HCF-flavoured. |
| **Map kit** | Enchant and potion caps (e.g. Prot II, Sharp II, no Strength II), so gear isn't everything. | Consumables are rare (#40); crates give up to Prot IV / Sharp V. | **Config + a small listener** to cap enchant levels if we want it. |
| **Faction points / focus** | Points from KOTH caps and kills rank factions. `/f focus` highlights a target for your team. | F-Top ranks wealth (`avian-ftop`). | **Build:** points are a second F-Top column; focus is a nametag colour via TAB. |

## Recommendation

Don't turn the main Factions realm into HCF. Deathbans and short maps fight the grind we built:
cane, spawners, the hoe, F-Top. Take the patterns that add tension without punishing play time:

1. **Now, config only:** a faction member limit (~10), DTR with a freeze
   (try it on the playtest, #31), and an ender pearl cooldown.
2. **With events (#24):** one capture engine for KOTH, Conquest and the Aerie, paying keys and
   faction points; SOTW/EOTW as season bookends.
3. **Later, the signature build:** HCF classes (Bard first; it's the one players remember).

**The hub opens a better option:** a separate **HCF server** behind the Velocity proxy, with
deathbans, lives, short maps and kits, and the main realm stays Factions. Balances, ranks and
skills already live in MariaDB, so the two can share a store, crate keys and ranks.
docs/DEPLOYMENT.md → "A hub" lists what that needs.
