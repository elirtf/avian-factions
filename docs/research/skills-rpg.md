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

## Things to decide before adopting

1. **PvP balance.** Stats add HP, attack damage, damage reduction and crit. On a competitive
   Factions server a maxed player beats a new one on stats alone. Options: disable or cap the
   combat stats (keep gathering stats like double drop), or accept it as progression. This
   interacts with `combat-mechanics-on-paper.md`.
2. **Economy.** The money reward and `money` loot pay through Vault; that has to fit the token/gem
   economy and EconomyShopGUI sell prices, or double drop inflates everything.
3. **Stacked mobs.** Fighting XP per kill with RoseStacker whole-stack kills — check it is not
   one kill's XP for a 500-stack, or 500 kills' worth.
4. **Quests plugin** as a separate choice.
