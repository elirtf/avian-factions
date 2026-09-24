# Skills and RPG progression: AuraSkills

Written 2026-09-23. Prompted by stranded.lol, whose "RPG progression" (Foraging, Mining, Defense,
Mobility… with XP and quests) the owner liked. We asked Stranded whether theirs is open source;
no answer yet, and nothing public names or publishes it. Their server lists call the origins
system custom and say nothing about the skills plugin.

## Choice: AuraSkills 2.4.0 (was Aurelium Skills)

| | AuraSkills 2.4.0 |
|---|---|
| Source | github.com/Archy-X/AuraSkills, tag `2.4.0`, released 2026-09-20, actively maintained |
| Licence | GPL-3.0 |
| 26.1.2 | **Boot-tested, clean** (Modrinth tags 26.1, 26.1.1, 26.2, 26.3 — 26.1.2 is missing from the tag list but works; 2.3.12 tags it explicitly) |
| Skills | Farming, Foraging, Mining, Fishing, Excavation, Archery, Defense, Fighting, Agility, Enchanting, Alchemy |
| Stats | 9 stats, 17 traits (HP, attack damage, damage reduction, crit, movement speed, double drop, mana…) |
| Storage | YAML files, or SQL (`type: mysql`, bundled HikariCP) — **works on MariaDB 11.8.9** |
| Hooks | LuckPerms, PlaceholderAPI, Vault, WorldGuard all detected on our stack |

"Mobility" on Stranded maps to AuraSkills' Agility; skills can be renamed in config, and new ones
added through the API (below). It does **not** do quests — that needs a separate plugin
(BetonQuest or Quests are the usual free ones) whose rewards grant AuraSkills XP.

The other candidates (FreeRPG, Dynamic Skills, SkillForge999, mcMMO) were not tested: AuraSkills
is the only one with Foraging/Defense/Agility-shaped skills, a public content API and an active
2026 release on our Minecraft version.

## Boot test (2026-09-23)

Scratch Paper 26.1.2 build 74, JDK 25, alongside EssentialsX 2.22.0, LuckPerms 5.5.84, Vault 1.7.3,
PlaceholderAPI 2.12.3, FAWE 1389, WorldGuard 7.0.18, FactionsUUID 4.7.0, RoseStacker 1.5.42.

- All 9 plugins enabled. AuraSkills: "Loaded 11 skills with 327 total sources", "Loaded 9 stats and
  17 traits", "Loaded 7 menus", all four hooks registered.
- `skills top foraging` answers from console; `/skills` is player-only (menu).
- Second boot with `sql.enabled: true` against a throwaway `mariadb:11.8.9`: Hikari connected and
  AuraSkills created its own `auraskills_*` tables (with a `schema_migrations` table of its own).
  Zero ERROR/Exception lines in that boot.
- Not tested: an actual player gaining XP (no client in the test). First thing to check on `./dev`.

Jar SHA-256 (Modrinth `https://cdn.modrinth.com/data/uDdZAVls/versions/9rSJ3THD/AuraSkills-2.4.0.jar`):
`de54cbd2e33d65e8b1704751ae4121ed2f5b466ba89c63a1aadf3a3e629d6a40`.

## Modifying it: extend first, fork only if we must

The code: ~58k lines of Java in `api`, `common`, `bukkit`, `paper` modules; Gradle Kotlin DSL,
Java 21 toolchain. **No NMS / CraftBukkit imports**, which is why it survives Minecraft bumps.
`./gradlew build -x test` on tag `2.4.0` succeeds with JDK 21 (its Gradle 8.14.4 wrapper will not
run *on* JDK 25 — use 21 for the daemon).

Most of what we would want to change does not need a fork:

- **Config only:** skill names, XP sources (`sources/*.yml`), XP curve (`xp_requirements.yml`),
  level rewards and money (`rewards/`), loot tables (`loot/`), stats per skill (`stats.yml`),
  abilities, menus and messages. Bird theming lives here.
- **API, from our own plugin** (the same pattern as `avian-factions` on FactionsUUID):
  `auraSkills.useRegistry("avian", getDataFolder())` then `registry.registerSkill(...)`,
  `registerStat`, `registerTrait`, `registerSourceType`, custom abilities. Sources and rewards
  for those come from yml in our data folder. This is how a real "Mobility" skill, or a
  Factions-specific skill (raiding, claiming), would be added without touching their code.
- **Fork** only for behaviour the API cannot reach. A fork costs us every upstream release as a
  merge. GPL-3.0: running a modified jar on our own server is not distribution; if we ever hand
  the modified jar to anyone, its source goes with it.

## Combat balance (decided 2026-09-23)

Owner: "cap the combat stats but not too much — we want grinding to be a part of it." So nothing
combat-related is disabled, and nothing stops early: every value keeps growing all the way to skill
level 100, it just ends at a moderate edge instead of a dominating one. Files:
`dev-server/plugins/AuraSkills/{stats,abilities,mana_abilities}.yml`.

**How the numbers work.** Skill levels grant stat points (`rewards/*.yml`, unchanged); a stat
turns into a trait by `modifier` in `stats.yml`. With every skill at 100 a player has about
250 Strength, 100 Health, 250 Toughness, 250 Regeneration, 100 Crit Chance, 100 Crit Damage.
Abilities unlock in the first five levels and gain a level every five, so they reach about
level 20 at skill 100; value = `base_value + value_per_level × (level − 1)`. Mana abilities reach
about level 16. `max_level` is left at 0 (uncapped) so grinding keeps paying.

Stats (all skills at 100):

| Trait | Default | Avian | Modifier |
|---|---|---|---|
| Attack damage | +100% | **+20%** | 0.4 → 0.08 |
| Max HP | +100 HP (50 hearts) | **+6 HP (3 hearts)** | 1 → 0.06 |
| Damage reduction | 53% | **12%** | 0.3 → 0.05 |
| Crit chance | 100% | **15%** | 1 → 0.15 |
| Crit damage | +100% | **+40%** | 1 → 0.4 |
| Natural regen per tick | +5 HP | **+1 HP** | 0.02 → 0.004 |
| Movement speed (from Fleeting / items) | 1 per point | 0.5 per point | 1 → 0.5 |

Luck (double drops), Wisdom (XP bonus, anvil discount, mana) untouched — they are the gathering
side, and an economy question (below), not a PvP one.

Abilities at skill 100:

| Ability | Default | Avian |
|---|---|---|
| Sword / Axe / Bow Master (damage) | +41% / +61% / +41% | +11.5% each |
| First Strike | +110% first hit | +29% |
| Shielding (sneaking) | −59% damage | −11.5% |
| Immunity (negate a hit) | 8.1% | 3.4% |
| No Debuff (negate harmful potion) | **100%** | 15% |
| Parry | −43% | −24% |
| Bleed chance / damage per tick | 60% / 10 HP | 21% / 1.45 HP |
| Stun chance | 21% | 11.5% |
| Shredder (triple armour durability) | 60% | 20% |
| Golden Heal (regeneration effect) | +119% | +22% |
| Golden Heart (absorption damage taken) | −62% | −17% |
| Recovery (regen under half HP) | +200% | +33.5% |
| Life Steal (on kill) | 21.5% max HP | 11.5% |
| Meal Steal | 39% | 10.5% |
| Fleeting (Speed under 20% HP) | +81 | +24 |
| Absorption (mana ability: damage to mana) | ~47 s | ~5 s |
| Lightning Blade (mana ability: attack speed) | +80% for 65 s | +27.5% for 20 s |
| Charged Shot (per mana) | 2% | ~0.95% |

Worst case, a fully maxed player against a brand-new one: about 1.25× damage (strength plus average
crit, before the weapon-master ability) and about 1.45× effective health (+30% HP, 12% less
damage taken). Gear, potions and skill
still decide a fight; the grind is a clear edge, not a win button. Between two grinded players it
cancels out. Mobs are affected the same way, which only makes PvE a little slower to trivialise.

Tuning later: change `modifier` (stats) or `value_per_level` (abilities); the other files stay at
AuraSkills defaults.

## Still open

1. **Balance in play.** The numbers above are from the formulas, not from fights. Revisit after
   real PvP; `combat-mechanics-on-paper.md` covers the vanilla side.
2. **Economy.** The money reward and `money` loot pay through Vault; that has to fit the token/gem
   economy and EconomyShopGUI sell prices, or double drop inflates everything.
3. **Stacked mobs.** Fighting XP per kill with RoseStacker whole-stack kills — check it is not
   one kill's XP for a 500-stack, or 500 kills' worth.
4. **Quests plugin** as a separate choice.
