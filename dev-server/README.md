# Tracked dev-server config

`run/` is git-ignored (Paper jar, worlds, plugin data), so the third-party plugin settings we
actually changed live here and are copied into `run/` by `./gradlew syncDevConfig`, which
`runServer` depends on. Only files we edited are tracked; everything else is each plugin's default.

| File | What we changed |
|---|---|
| `plugins/Essentials/config.yml` | `unsafe-enchantments: true` (spec §22 custom enchants need levels above vanilla caps) |
| `plugins/LuckPerms/config.yml` | `storage-method: mariadb`, pointed at the Compose database (`luckperms_` tables) |
| `plugins/CoreProtect/config.yml` | `use-mysql: true`, pointed at the Compose database (`co_` tables) |

Credentials here are the local dev ones from `.env.example`. A real deployment supplies its own.

The 5,000-block world border is world state, not config: it lives in `run/world/level.dat` after
`worldborder set 5000` on the console. A fresh world needs that command once (or Chunky's
`/chunky worldborder`); the README's dev-loop section says so.

`plugins/EconomyShopGUI/config.yml` — EconomyShopGUI owns sell prices when installed (see
`ShopSellValues`); this tracks the settings we changed from its defaults. Its `/shops` and
`/sections` directories are left at their defaults and are not tracked.
