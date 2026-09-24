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
| `server.properties` | `white-list=true`, `enforce-whitelist=true`: port 25565 is forwarded to the internet, so only listed players may join. `management-server-secret` is left blank so the server generates its own; never commit one |
| `whitelist.json` | Players allowed to join. Add with `./dev cmd "whitelist add <name>"`, then copy `run/whitelist.json` back here |

Credentials here are the local dev ones from `.env.example`. A real deployment supplies its own.

The 5,000-block world border is world state, not config: it lives in `run/world/level.dat` after
`worldborder set 5000` on the console. A fresh world needs that command once (or Chunky's
`/chunky worldborder`); the README's dev-loop section says so.

`plugins/EconomyShopGUI/config.yml` — EconomyShopGUI owns sell prices when installed (see
`ShopSellValues`); this tracks the settings we changed from its defaults. Its `/shops` and
`/sections` directories are left at their defaults and are not tracked.

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
and don't attack, but water still pushes them), die to **one hit** (`instant-kill-disabled-ai`), and
that hit kills the **whole stack** with loot and XP for every mob
(`disable-mob-ai-options.kill-entire-stack-on-death`). Natural mobs and dungeon spawners are
unchanged. `global-spawner-settings.max-stack-size` (32) is the number most likely to need raising
for an OP economy, and that is a balance decision (see `docs/research/mob-stacking.md`).
