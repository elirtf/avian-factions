# Tracked dev-server config

`run/` is git-ignored (Paper jar, worlds, plugin data), so the third-party plugin settings we
actually changed live here and are copied into `run/` by `./gradlew syncDevConfig`, which
`runServer` depends on. Only files we edited are tracked; everything else is each plugin's default.

| File | What we changed |
|---|---|
| `plugins/Essentials/config.yml` | `unsafe-enchantments: true` (spec §22 custom enchants need levels above vanilla caps) |
| `plugins/LuckPerms/config.yml` | `storage-method: mariadb`, pointed at the Compose database (`luckperms_` tables) |
| `plugins/CoreProtect/config.yml` | `use-mysql: true`, pointed at the Compose database (`co_` tables) |
| `plugins/FactionsUUID/config/main.conf` | Power: player start/max 20, min 0, regen 1/hour, death loss 2; `raidability = true` (land > power); `economy.enabled = true` for banks and upgrades (ADR-0007) |
| `plugins/FactionsUUID/config/translations.conf` | Role names: Leader, Co-Leader, Officer, Member, Recruit |
| `plugins/RoseStacker/config.yml` | Only spawners stack (block stacking off); spawners are raidable: TNT drops 75% as items, destroys the rest |
| `plugins/CommandTimer/timers/*.json` | Raid windows: grace (no explosions) outside Mon–Fri 20–23 and Sat–Sun 18–24, server time |
| `plugins/AuraSkills/config.yml` | `sql.enabled: true`, pointed at the Compose database (`auraskills_` tables) |
| `plugins/AuraSkills/stats.yml`, `abilities.yml`, `mana_abilities.yml` | Combat stats and abilities capped for Factions PvP, still growing to level 100; the numbers and why are in `docs/research/skills-rpg.md` |
| `server.properties` | `white-list=true`, `enforce-whitelist=true`: port 25565 is forwarded to the internet, so only listed players may join. `management-server-secret` is left blank so the server generates its own; never commit one |
| `whitelist.json` | Players allowed to join. Add with `./dev cmd "whitelist add <name>"` (Bedrock players: `fwhitelist add <Gamertag>`), then copy `run/whitelist.json` back here |
| `plugins/MiniMOTD/main.conf` | The server-list message: three bright, bird-themed MOTDs picked at random. Icons are off until the sigil is ready |
| `plugins/DeluxeMenus/` | The faction menu (`gui_menus/factions_menu.yml`): opens on `/f`, `/f menu`, `/f gui` or `/fmenu`. Buttons run FactionsUUID's exact commands; members and factionless players see different buttons. `config.yml` registers only our menu, so the plugin's examples are never created |
| `spigot.yml` | Farm crops (cane, cactus, melon, pumpkin, wheat, carrot, potato, beetroot, nether wart, cocoa, bamboo, berries, kelp) grow at 120 % so small farms are not miserable; saplings, vines and mushrooms stay vanilla (docs/ECONOMY.md) |
| `plugins/Geyser-Spigot/config.yml` | `java.auth-type: floodgate` (Bedrock players need no Java account); MOTD and `server-name` say Avian Factions. See `docs/research/bedrock-crossplay.md` |
| `plugins/CarbonChat/channels/global.conf` | The chat line: name, rank tag, a dim ➜, the message. Hovering the name shows rank, balance, tokens, gems and faction; clicking it starts a `/msg`. `%avian_*%` placeholders come from our plugin (`AvianPlaceholders`) |
| `plugins/CarbonChat/config.conf` | `use-carbon-nicknames=false`: nicknames stay with EssentialsX `/nick` (otherwise Carbon clears them on join). Storage is Carbon's default JSON (only ignores and channel choices) |
| `plugins/CarbonChat/command-settings.conf` | Carbon's `/nick` is off so EssentialsX's answers. Carbon owns `/msg`, `/r` and `/ignore` |
| `plugins/AvianFactions/combat.conf` | `hit-delay-ticks=16` (faster PvP combos); `clear-stacked-mob-corpses=true` (a mob killed out of a stack vanishes at once so the next hit on the stack lands) ; gapple, notch apple and totem cooldowns (docs/ECONOMY.md) |
| `plugins/AvianFactions/economy.conf` | Sugar cane tokens (2 % per grown block harvested by hand), villagers cannot summon iron golems, fallback sell values |
| `plugins/AvianFactions/ftop.conf` | Spawner and block values equal to their /shop prices |

Credentials here are the local dev ones from `.env.example`. A real deployment supplies its own.

The 5,000-block world border is world state, not config: it lives in `run/world/level.dat` after
`worldborder set 5000` on the console. A fresh world needs that command once (or Chunky's
`/chunky worldborder`); the README's dev-loop section says so.

`plugins/EconomyShopGUI/` — EconomyShopGUI owns sell prices when installed (see `ShopSellValues`). `config.yml` holds the settings we changed; `shops/` is the whole Avian price sheet (sell list, gear by tier, spawner ladder) and `sections/Magic/potions.yml` turns the potion shop off. Numbers and reasoning: `docs/ECONOMY.md`.

## Ranks

`luckperms/ranks.lp` is the **source of truth** for groups, tracks, prefixes and permissions —
plain text so it reviews and diffs in git, unlike LuckPerms' own storage (which lives in MariaDB).

`./dev start` applies it automatically, typing every line into the console once the server is up
(`./dev ranks` re-applies it by hand). LuckPerms has no "run this file" command, so this is the
mechanism. Re-running is safe — `creategroup` on an existing group is a no-op and everything else
sets rather than appends.

To back up what is live: `lp export <name>` writes `run/plugins/LuckPerms/<name>.json.gz`. That is
a snapshot, not the source of truth; if the two ever disagree, fix `ranks.lp` and reapply.

`plugins/RoseStacker/config.yml` — stacking for mobs, items and spawners (not blocks). Changed so
spawner farming pays: mobs from **player-placed** spawners have their AI goals removed
(`global-spawner-settings.disable-mob-ai` + `disable-mob-ai-only-player-placed` — they stand still
and don't attack, but water still pushes them), and die to **one hit** (`instant-kill-disabled-ai`)
— **one mob per hit**, not the whole stack (`disable-mob-ai-options.kill-entire-stack-on-death:
false`; one swing wiping a 42-blaze stack was too much). For a middle ground, `multikill-options`
kills a set number per hit. Natural mobs and dungeon spawners are
unchanged. `global-spawner-settings.max-stack-size` (32) is the number most likely to need raising
for an OP economy, and that is a balance decision (see `docs/research/mob-stacking.md`).
