# Resource pack layer (issue #41)

Researched 2026-09-25 against primary sources: CraftEngine source at tag `26.9`
(commit `30b7e1da49e4a2d5753cc070866825b1f3584542`) and its wiki repo
(`Xiao-MoMi/craft-engine-wiki` @ `40d6145`), BetterHud source at tag `2.0.0`
(`59e4fb8d8785ca490ff9ba89819b425a9105870f`) and its GitHub wiki, Geyser `master` @ `63a4e2b`
(2026-09-25), GeyserMC/PackConverter and Rainbow READMEs, and the Modrinth API, Spiget API
(SpigotMC listing data) and Nexo docs for version support. JARs were downloaded and hashed. Nothing
was boot-tested. Claims marked **unverified** could not be traced to a primary source.

## Recommendation

**Use CraftEngine Community Edition 26.9.1 to build and serve the pack. Add BetterHud 2.0.0 for the
hotbar money/token HUD, and let it merge into CraftEngine's pack automatically.** Keep the pack
sources in git under `dev-server/plugins/CraftEngine/resources/avian/`. Serve the pack with
CraftEngine's built-in host on the **same port as Minecraft** (`self-host.port: auto`), so the
firewall and router need **no new port**. Treat drawn menus and the HUD as Java-only. Bedrock gets
at most the plain icon glyphs, through a separate converted pack in Geyser's `packs/` folder.

Why CraftEngine over the alternatives:

- It is free, the source is public (GPL-3.0), and it supports 26.1.2.
- It rewrites `<image:…>` and `<shift:…>` tags **in outgoing packets**. That covers menu titles,
  chat, tab, scoreboard, bossbar and item names, so the drawn backgrounds and icons work in
  DeluxeMenus, EconomyShopGUI, CrazyCrates, CarbonChat and our own `Ui` menus without touching
  those plugins.
- It serves the pack itself and computes the SHA-1, so we need no pack server of our own.
- BetterHud merges into it with no configuration.

| Plugin | Pin | File | SHA-256 | Source |
|---|---|---|---|---|
| CraftEngine (Community) | **26.9.1** (2026-09-18) | `craft-engine-paper-plugin-26.9.1.jar` | `021260c87e3546730d321f4360b1744e6e33caa1d68e98ae30dcdbbdc347ae0f` | https://cdn.modrinth.com/data/tRX6FMfQ/versions/EDh6mvv2/craft-engine-paper-plugin-26.9.1.jar |
| BetterHud | **2.0.0** (2026-05-16, release channel) | `BetterHud-bukkit-2.0.0.jar` | `4ff7892b474870adf5f3fc9b2419fe80fcdb5a7f895fadd0f9de91bebbd3307b` | https://cdn.modrinth.com/data/JUl6WIK2/versions/bedIGBtb/BetterHud-bukkit-2.0.0.jar (same file as the GitHub release asset) |

The SHA-1 values match Modrinth's listing: CraftEngine `81d834a916b071f6726b6a93fca5e5891373c46a`,
BetterHud `f41351922346a11f1f140987d380aa90b692b7ce`.

## 1. CraftEngine

### Licence

- **GPL-3.0-only.** Sources: [LICENSE](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/LICENSE)
  and the Modrinth project `license.id = GPL-3.0-only`.
- GPL obligations apply only when we *convey* (distribute) the software. Running an unmodified jar
  on our own server creates no obligations. We also have no reason to link `avian-plugin` against its
  API, because everything we need goes through config and text tags. If we ever `compileOnly`
  against `net.momirealms:craft-engine-*` (README, `repo.momirealms.net`), that is still not
  distribution while the repo stays private. (Repo visibility today: `PRIVATE`, no licence.)
- Two editions ([README](https://github.com/Xiao-MoMi/craft-engine#differences-between-versions)):
  - **Community Edition** (free, Modrinth): no official support, no exclusive features, no dev
    builds.
  - **Premium** (VoxelShop / BuiltByBit / NMCrate): adds support, dev builds and, for example, pack
    `protection` (anti-extraction; `config.yml` marks it "requires premium version"). We don't need
    Premium.

### 26.1.2 support and pin

- Modrinth lists **26.9.1** for loaders `folia, paper, purpur` and game versions
  `26.1, 26.1.1, 26.1.2, 26.2` ([Modrinth API](https://api.modrinth.com/v2/project/craftengine/version)).
- **Every CraftEngine Modrinth version is typed `beta`.** The author uses no "release" channel at
  all, so this reflects how they publish rather than being a warning about this build. Still, it
  breaks our "release channel only" habit, so note it in the pin comment.
- Use the **paper** jar, not `craft-engine-bukkit-plugin-*`. Its `paper-plugin.yml` declares
  `load: STARTUP`, a bootstrapper, and `folia-supported: true`.
- Requirements ([installation](https://xiao-momi.github.io/craft-engine-wiki/getting_start/installation)):
  JDK 21+ and Minecraft 1.20+. Java 25 is fine.
- The GitHub release `26.9` has no assets, only a link to Modrinth. There is no GitHub tag for
  `26.9.1`, so the Modrinth CDN is the download source.

### How it builds and hosts the pack

All of this is from `config.yml` at tag 26.9
([source](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/common-files/src/main/resources/config.yml))
and the wiki's [hosting](https://xiao-momi.github.io/craft-engine-wiki/getting_start/set_up_host) and
[self-host](https://xiao-momi.github.io/craft-engine-wiki/getting_start/set_up_host/self) pages.

- **Build.** A `workflows.default` pipeline runs on `reload_pack` (`/ce reload all`):
  `generate → validate → zip (./generated/resource_pack.zip) → upload → send_pack`.
  - Pack sources are merged from every folder under `plugins/CraftEngine/resources/<pack>/`.
  - `merge-external-folders` and `merge-external-zip-files` pull in other plugins' packs.
  - `duplicated-files-handler` merges fonts, `pack.mcmeta`, `sounds.json` and item definitions.
- **Host types:**
  - `self`: the built-in HTTP server (the default).
  - `external`: our own URL, with optional `sha1` and `uuid`.
  - Upload targets: Lobfile, MCPacks, S3/R2, OneDrive, Dropbox, GitLab and OpenList.
- **Built-in server port.** In `self-host`, `port: "auto"` **"shares the Minecraft server port
  (piggybacks on its Netty pipeline)"**, so the pack downloads from `http://<ip>:25565/download/default`.
  - Set a number (e.g. `8163`) to use a dedicated port instead.
  - `ip: "auto"` detects the public IP through Cloudflare trace.
  - `url:` overrides the advertised URL, for a reverse proxy, NAT or a DNS name.
  - The server speaks HTTP only. HTTPS needs a reverse proxy in front.
- **Access control (defaults).**
  - `deny_non_minecraft_request: true`: the `User-Agent` must start with `Minecraft Java/`.
  - `one_time_token: true`: each download URL gets a single-use `?token=`.
  - `strict_validation: false`: when on, the token is tied to the `X-Minecraft-UUID` header.
  - Rate limits: 5 MB/s total and 10 requests per 60 s per IP.
- **SHA-1 and UUID.** CraftEngine computes the hash and sends it with the pack push
  ([`ResourcePackUtils.java`](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/bukkit/src/main/java/net/momirealms/craftengine/bukkit/util/ResourcePackUtils.java)).
  Nothing goes in `server.properties`.
- **Prompt and force** (`resource-pack.delivery`):
  - `prompt` is a MiniMessage string.
  - `send-on-join: true`.
  - `kick-if-declined: true`. **This value is also sent as the packet's `required` flag**
    (`ResourcePackUtils` passes `Config.kickOnDeclined()` as the `required` argument), so the client
    shows the "required" prompt.
  - `kick-if-failed-to-apply: false`.
  - `strict-player-uuid-validation: true`.
  - Players can toggle packs with `/pack enable|disable <id>`. The `%ce_pack-state_<id>%`
    placeholder reports each player's choice.
- **Storage.** Pack preferences are stored with `storage.type` = json, sqlite (the default), h2,
  mysql, mariadb, postgresql or mongodb. The house rule is "MariaDB everywhere". The data is trivial,
  but `mariadb` against the Compose database keeps it consistent with the rest of the stack.

### What it needs from us

- One CraftEngine pack, `resources/avian/`, with three parts
  ([project structure](https://xiao-momi.github.io/craft-engine-wiki/getting_start/project_structure)):
  - `pack.yml`
  - `configuration/*.yml`: `images:` and optional `emoji:`, `items:`
  - `resourcepack/assets/avian/textures/font/*.png`
- Menu backgrounds go in `textures/font/`, **not** `textures/item` or `textures/block`, because those
  are in the atlas and get mipmapped
  ([first image](https://xiao-momi.github.io/craft-engine-wiki/getting_start/first_image)).
- A single glyph cell can be at most 256×256 px, and `height ≥ ascent`
  ([image](https://xiao-momi.github.io/craft-engine-wiki/configuration/image)).

### 1a. Drawn chest-menu backgrounds

- **The mechanism.** An image is a glyph in a font. You write `<image:ns:id>` for the glyph and
  `<shift:N>` for N pixels of negative or positive space.
  - The shift glyphs are built in: `image.offset-characters` maps −256…+256 to `U+F800…U+F844` in
    `minecraft:default`.
  - CraftEngine's own GUIs use exactly this pattern:
    `title: "<white><shift:-11><image:internal:item_browser>"` (`config.yml`, `gui:` section).
- **Other plugins' menus work through packet rewriting.** With `network.intercept-packets.container: true`
  (on by default), `OpenScreenListener` reads every outgoing open-screen packet
  ([source](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/network/listener/game/OpenScreenListener.java)).
  - It walks every string in the title component and tokenises it for the tags `image`, `shift`,
    `global`, `l10n` and `papi`
    ([`AbstractNetworkManager.matchNetworkTags`](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/core/src/main/java/net/momirealms/craftengine/core/plugin/network/AbstractNetworkManager.java)).
  - It swaps in the pre-built font components.
  - So **the title only needs to contain the literal text `<shift:-8><image:avian:shop_bg>`**.
    CraftEngine does not need to know which plugin owns the window.
- **The tag text has to reach the packet intact.**
  - **DeluxeMenus, EconomyShopGUI, CrazyCrates.** Titles come from YAML strings. With legacy `&`
    parsing, the tag is plain text. With MiniMessage parsing, the tag is unknown, and MiniMessage
    "will treat any invalid tags as normal text", in strict mode too
    ([Adventure docs](https://docs.papermc.io/adventure/minimessage/api/)). Either way the literal
    survives. **Unverified per plugin:** none of the three has been boot-tested with this.
  - **Our own `Ui` menus (Adventure).** Put the tags in **one** `Component.text(...)` node, for
    example a leading child of the title. CraftEngine's replacement runs on text nodes. No CraftEngine
    API dependency is needed.
  - **Alternatives.** PlaceholderAPI can emit the glyph directly: `%image_mm_avian:shop_bg%` for
    MiniMessage, `%image_raw_…%` for the raw character, and `%shift_mm_-8%`
    ([PAPI page](https://xiao-momi.github.io/craft-engine-wiki/compatibility/placeholderapi)). This
    only helps where a plugin runs PAPI *and* keeps the font (MiniMessage or MineDown). A legacy
    `§` string loses the font. The packet route is the robust one.
- **Cost.** Every enabled interceptor adds work on the Netty thread. Turn off the ones we don't use.

### 1b. Icon glyphs (coin, token, gem, rank badges)

- **Where they work.**
  - In MiniMessage strings, `<image:avian:coin>` works anywhere CraftEngine parses the text.
  - Anywhere else, packet interception covers `system-chat`, `player-chat`, `tab-list`,
    `player-info`, `team` (prefix/suffix/nametags), `scoreboard`/`set-score`, `actionbar`, `title`,
    `bossbar`, `item` (name and lore), `advancement`, `combat-kill` and `dialog` (`config.yml`
    `network.intercept-packets`, all `true` by default).
  - PlaceholderAPI can also supply them: `%image_mm_…%`, `%image_raw_…%`.
- **LuckPerms rank badges.** Our rank tags are already MiniMessage LuckPerms suffixes, rendered by
  CarbonChat through `%avian_rank%`. Add `<image:avian:rank_raven>` to the suffix. Carbon passes the
  unknown tag through, and CraftEngine rewrites it in the outgoing chat packet.
- **CarbonChat.**
  - In `player-chat`, CraftEngine rewrites only `unsignedContent` and the chat-type parameters. The
    signed body is untouched, so message signatures are unaffected
    ([`PlayerChatListener`](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/network/listener/game/PlayerChatListener.java)).
  - The illegal-character filter runs on Paper's `AsyncChatDecorateEvent` at `MONITOR`. It edits
    only the player's typed message, masking typed `<image:…>` tags and raw codepoints in
    `minecraft:default` as `*`
    ([`ChatListener`](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/bukkit/src/main/java/net/momirealms/craftengine/bukkit/font/ChatListener.java)).
    Carbon's format string is not touched.
  - `emoji` (`:coin:` → glyph) is optional, and can be restricted with a permission node.
  - **Unverified** until boot-tested: Carbon + CraftEngine on the live stack.
- **Font choice.**
  - The wiki recommends a **custom font** such as `avian:icons`, so players cannot type the glyphs.
  - Only `minecraft:default` glyphs can be converted for Bedrock (§4).
  - Suggestion: put the small icons in `minecraft:default` (the filter blocks typing) and the menu
    backgrounds in a custom font.

## 2. BetterHud

- **Licence:** MIT. Source: [GitHub](https://github.com/toxicity188/BetterHud) and the Modrinth
  project `betterhud2`.
- **26.1.2 support.**
  - Release **2.0.0** (Modrinth `release` channel) lists `bukkit, folia, paper, purpur, spigot` for
    `…1.21.11, 26.1, 26.1.1, 26.1.2`.
  - Its notes say "Minecraft 26.1.x Support (Need to change BetterHud/shaders)" and "Java 25 Usage"
    ([release](https://github.com/toxicity188/BetterHud/releases/tag/2.0.0)). The shaders note only
    matters for upgrades; a fresh install generates the new files.
  - The only newer builds are `2.1.0-SNAPSHOT-*` alpha builds (last one 2026-08-10).
- **Money and tokens on the hotbar.**
  - The `[papi:<placeholder>]` syntax needs no setup
    ([wiki: placeholders](https://github.com/toxicity188/BetterHud/wiki/placeholders)). The code
    registers a `papi` placeholder that calls `PlaceholderAPI.setPlaceholders`
    ([`BukkitBootstrapImpl.kt`](https://github.com/toxicity188/BetterHud/blob/2.0.0/bootstrap/bukkit/src/main/kotlin/kr/toxicity/hud/bootstrap/bukkit/BukkitBootstrapImpl.kt)).
    So `[papi:avian_balance]` and `[papi:avian_tokens]` work.
  - Layouts, images and backgrounds are all YAML.
  - The HUD is drawn through the bossbar plus a `rendertype_text` core shader.
- **Merging with CraftEngine is automatic.**
  - With `merge-with-external-resources: true` (the default), BetterHud's `CraftEngineCompatibility`
    listens for CraftEngine's `AsyncResourcePackCacheEvent`. It rebuilds its own pack and adds it to
    CraftEngine's external zips or folders ("Successfully merged with CraftEngine.")
    ([source](https://github.com/toxicity188/BetterHud/blob/2.0.0/bootstrap/bukkit/src/main/kotlin/kr/toxicity/hud/bootstrap/bukkit/compatibility/craftengine/CraftEngineCompatibility.kt)).
  - Keep BetterHud's `enable-self-host: false` (the default), so only CraftEngine serves.
  - The documented manual route is CraftEngine's `merge-external-zip-files`.
- **Bedrock.** BetterHud's default `disable-to-bedrock-player: true` means Bedrock players get no
  HUD.
- **Shader overlap.** CraftEngine ships core shaders only in its optional `remove_shulker_head`
  pack (`rendertype_entity_solid`). There is no clash with BetterHud's `rendertype_text` in our setup.

## 3. Alternatives

| Option | Licence / price | 26.1.2 status | Notes |
|---|---|---|---|
| **Nexo** | Proprietary, paid (docs say support needs "proof of purchase"). **Price unverified**: the VoxelShop/BuiltByBit pages returned 403. | **Supported**, Nexo 1.22+ for 26.1–26.1.2 ([version support](https://docs.nexomc.com/version-support)). Latest on `repo.nexomc.com` is 1.28.0. | The most direct Oraxen successor. BetterHud documents a merge route (`Nexo/pack/external_packs/BetterHud`). |
| **ItemsAdder** | Premium, **€19.99** (Spiget, resource 73355) | Spigot "tested" list includes **26.1, 26.2**. Latest 4.0.18 (2026-08-21). | Closed source. Hosting and packet features are similar. |
| **Oraxen** | Custom "Oraxen License" (no redistribution, source visible) ([LICENSE.md](https://github.com/oraxen/oraxen/blob/master/LICENSE.md)). Premium **€19.99** on Spigot. | Spigot "tested" list stops at **1.21**. The v1.214.0 notes fix a path "on Paper 26.1+". **26.1.2 support is unverified**, not stated. Latest v1.219.1 (2026-09-18). | Maintained, but not clearly on 26.x. |
| **Hand-built pack** (our Gradle task zips `resource-pack/`, served as a GitHub release asset, with `resource-pack` + `resource-pack-sha1` in `server.properties`) | Free, all ours | Any version | No packet rewriting, so other plugins' titles would need raw PUA characters in `minecraft:default`, since legacy strings can't set a font. We would also write the negative-space font and codepoint bookkeeping ourselves. **The repo is private**, so release-asset URLs need auth and won't work for clients unless the assets live in a public repo or bucket. **Unverified** whether the client follows GitHub's redirect to `objects.githubusercontent.com`. |

**Verdict.** The paid options add nothing we need for fonts and menus, and cost money and source
access. The hand-built route recreates what CraftEngine already does (packet rewriting,
SHA-1/hosting, BetterHud merging). If we ever leave CraftEngine, the `resourcepack/` folder is a
vanilla-layout pack and moves with us.

## 4. Bedrock via Geyser

- **Pack prompt.** Geyser answers the Java pack push for the Bedrock player
  ([`JavaClientboundResourcePackPushPacket`](https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/translator/protocol/java/JavaClientboundResourcePackPushPacket.java)):
  - If the pack is **required**, it fakes `ACCEPTED → DOWNLOADED → SUCCESSFULLY_LOADED` so the
    server doesn't kick the player.
  - Otherwise it sends `DECLINED`.
  - CraftEngine's `required` flag equals `kick-if-declined`, so **Bedrock players are never kicked**
    with either setting.
- **Text on Bedrock.**
  - Geyser flattens every component to a legacy `§` string (`MessageTranslator.BEDROCK_SERIALIZER`,
    a `LegacyComponentSerializer`). The **font is lost**.
  - Bedrock therefore draws each glyph codepoint in its own default font.
  - Custom-font glyphs and the `U+F800…` shift characters show as missing glyphs or boxes unless a
    Bedrock pack supplies those codepoints. The exact look is **unverified** (not tested).
  - **Drawn menu backgrounds cannot work on Bedrock.** The Bedrock glyph format (below) has no
    per-glyph ascent or negative advance, so there is no equivalent of Java's offset trick.
- **Converting icons.**
  - GeyserMC **PackConverter / Thunder** (MIT) converts a Java pack to Bedrock. Its
    `FontTransformer` notes "Currently, only the default font is converted, custom fonts are not
    supported on bedrock"
    ([source](https://github.com/GeyserMC/PackConverter/blob/master/converter/src/main/java/org/geysermc/pack/converter/type/texture/transformer/type/ui/FontTransformer.java)).
  - It writes `font/glyph_XX.png` 16×16 grids with every cell scaled to one size.
  - The README warns: "still a work in progress and should not be used on production".
  - So only **`minecraft:default` icons (coin, token, gem, badges)** can reach Bedrock, and roughly
    at that.
- **Codepoint clash.**
  - CraftEngine auto-assigns `minecraft:default` icons from `U+E000`
    (`codepoint-starting-value.overrides.minecraft:default: 57344`).
  - Per Bedrock Wiki (community, **not** Microsoft), `glyph_E0`/`glyph_E1` hold Bedrock's own control
    icons, and E2–F8 are free. ([bedrock.dev/text/custom-emojis](https://wiki.bedrock.dev/text/custom-emojis))
  - If we want Bedrock icons, start at `U+E200` (`57856`). **Unverified** beyond that wiki.
- **Rainbow** (GeyserMC, client-side Fabric mod for 26.2) builds Geyser mappings and a Bedrock pack
  for custom items, blocks and sounds ([README](https://github.com/GeyserMC/Rainbow)). It does
  nothing for fonts or menus. It would matter only if we add custom item models later.
- **Serving a Bedrock pack.**
  - Drop the `.mcpack`/`.zip` in Geyser's `packs/` folder
    ([`ResourcePackLoader`](https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/registry/loader/ResourcePackLoader.java)).
    Geyser sends it over the Bedrock connection (UDP 19132), so no extra port is needed.
  - `gameplay.force-resource-packs` (default `true`) forces it
    ([`GeyserConfig`](https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/configuration/GeyserConfig.java)).
- **What to accept as Java-only:** drawn chest backgrounds, the BetterHud hotbar HUD and custom-font
  text. Bedrock players see the plain vanilla menus (EconomyShopGUI already offers Forms) and plain
  text. Keep every menu title readable **without** the background: text after the image, with the
  meaning in item names and lore.

## 5. Plan

1. **Pin.** Add both jars (the table at the top) to the `PinnedPlugin` list in
   `avian-plugin/build.gradle.kts`, with a comment that CraftEngine publishes only `beta`-typed
   builds.
2. **Pack sources.**
   - `dev-server/plugins/CraftEngine/resources/avian/`, containing:
     - `pack.yml`
     - `configuration/images.yml` (backgrounds, icons, badges)
     - `resourcepack/assets/avian/textures/font/…`
   - `dev-server/plugins/CraftEngine/config.yml`, holding only our changes.
   - `dev-server/plugins/BetterHud/` for the HUD layouts.
   - `syncDevConfig` already copies `dev-server/` into `run/`. Never edit `run/`.
   - Art stays original (spec §61).
3. **CraftEngine `config.yml` changes:**
   - `resource-pack.delivery.prompt` in Brand colours.
   - `kick-if-declined: true`, because drawn menus look broken without the pack and Bedrock is
     unaffected. Owner decision; `false` makes it optional.
   - `self-host.port: auto`, which reuses 25565.
   - Leave `self-host.ip: auto` for the dev box. In production, set `self-host.url` to the public
     host (a config value, never hardcoded).
   - `storage.type: mariadb`.
   - Disable the interceptors we don't use.
   - Set the `minecraft:default` codepoint start to `57856` (U+E200).
4. **server.properties:** leave `resource-pack=`, `resource-pack-sha1=` and
   `require-resource-pack=false` empty and false, as they are now. CraftEngine pushes the pack
   itself. Setting both would send two packs.
5. **Menus.**
   - Add an optional background to `Ui`: a leading `Component.text("<shift:-8><image:avian:menu_…>")`
     node.
   - Add the same literal to the DeluxeMenus / EconomyShopGUI / CrazyCrates titles in `dev-server/`.
   - Boot-test each one.
6. **Icons.** Add `<image:avian:rank_…>` to the LuckPerms suffixes in `dev-server/luckperms/ranks.lp`.
   Add coin, token and gem icons in the Brand strings and PAPI outputs.
7. **HUD.** Build the BetterHud layout with `[papi:avian_balance]` and `[papi:avian_tokens]`. Keep
   `enable-self-host: false` and `merge-with-external-resources: true`.
8. **Bedrock (optional, later).** Run Thunder (PackConverter) over the generated
   `plugins/CraftEngine/generated/resource_pack.zip`, then put the result in
   `dev-server/plugins/Geyser-Spigot/packs/`. Only the icons come through.

### Ports and firewall

- **No new port.** With `port: auto` the pack is served on TCP 25565, which is already open and
  forwarded for the remote friend. The download URL is `http://<public IP>:25565/download/default`.
- **Tradeoffs:**
  - `ip: auto` advertises the **public** IP. LAN and Tailscale players only get the pack if the
    router supports NAT hairpinning (**unverified** for this router). The fix is `self-host.url`
    pointing at an address every player can reach.
  - A dedicated port (e.g. 8163) would need a new `host-firewall` rule, a router forward and a
    "Before production" entry. Avoid it.
  - Pack downloads use the dev box's upload bandwidth (capped at 5 MB/s by default).
  - The pack host is another HTTP surface on the public port. It only serves the token-gated pack,
    and rejects non-Minecraft user agents.
- **Velocity later.** Behind a proxy, the backend's 25565 is no longer public, so `port: auto` on
  Paper stops being reachable. At that point, switch to `type: external` or an S3/R2 upload, or give
  the host its own port and `url`.

## Open questions for the owner

- Should the pack be **required** (a declining Java player is kicked) or optional? This is the
  recommendation's main tradeoff.
- Do we care about Bedrock icons at all? If yes, it means the Thunder step plus U+E200 codepoints.
  If no, skip §4 entirely.
- Accepting CraftEngine's `beta`-typed Modrinth channel as our "stable" pin.
- Nothing here has been boot-tested. Before building art, a throwaway boot with the full stack
  should check:
  - DeluxeMenus, EconomyShopGUI and CrazyCrates titles
  - Carbon chat badges
  - BetterHud merging
  - the LAN/Tailscale download path
