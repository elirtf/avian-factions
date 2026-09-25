# Custom weapons, tools and items

Researched 2026-09-25 against primary sources:

- the vanilla **26.1.2 client jar**, downloaded from Mojang's own
  [version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)
  (`client.jar` sha1 `4e618f09a0c649dde3fdf829df443ce0b8831e65`, `pack_version.resource_major` 84);
- CraftEngine source at tag `26.9` (commit `30b7e1da49e4a2d5753cc070866825b1f3584542`) and its wiki
  repo (`Xiao-MoMi/craft-engine-wiki` @ `40d6145`);
- Paper `main` @ `a15fed9c`;
- Geyser `master` @ `63a4e2b` and GeyserMC/PackConverter @ `48cb61a`;
- the asset stores' own pages and licence documents.

Nothing was boot-tested and the dev server was not touched. Claims marked **unverified** could not be
traced to a primary source, or were inferred rather than tested.

## Recommendation

**Retexture the vanilla weapons and tools by dropping replacement PNGs into our CraftEngine pack. Give
our own items (the Harvester Hoe first) custom art with the vanilla `item_model` component, set by our
plugin. Do not make them CraftEngine items.** For art, use two tiers:

- **Vanilla re-skins, which means volume: licensed art, remixed.** Buy **Raven Fantasy Icons
  Premium** ($35, itch.io). Remix it: redraw the silhouettes to one shape per tool type and swap the
  palette into the seven material tiers plus the Avian palette. Before buying, **email the author**
  to confirm a Minecraft server resource pack is covered (see §3). Ship at the art's native **16×16**.
- **Avian-specific items, which carry the identity: commissioned 32×32 originals.** That means the
  Harvester Hoe, crate and boss drops, and later keys and tokens. We own them outright, and they are
  what SPEC §61 means by "its own identity". Budget about **$10–15 per 32px texture**.

Why this shape:

- **Retexturing vanilla needs no code and no JSON** for 42 of the 47 items. A PNG at
  `assets/minecraft/textures/item/<name>.png` replaces the vanilla one, and CraftEngine packs it
  as-is. The spear, shield and trident are the only special cases (§1).
- **`item_model` set by our plugin is the lowest-coupling route to custom art.** The plugin keeps
  identifying items by its own PDC keys, as it already does. The only link to the pack is a
  namespaced key string, and it goes in config. There is no CraftEngine API, no GPL question and no
  second item identity (§2).
- **The Raven pack is cheap and huge, and its licence allows commercial use and modification.** But
  its 32×32 files are exact 2× upscales of 16px art: verified on the free edition, pixel for pixel.
  Shipping them "as 32×32" adds nothing. The 32×32 look SPEC §61 asks for can only come from
  original 32px drawing, which is why the signature items are commissioned.
- **Bedrock waits** (§4). Bedrock players see vanilla weapons, and a netherite hoe with our name and
  lore. Everything still works for them.

**Cost:** $35 for the Raven pack, plus about $50–150 to commission the first 5–10 signature items.
Commissioning the whole vanilla set at 32px instead would be about $630–950 (§3).

## 1. Retexturing every vanilla weapon and tool (26.1.2)

### What exists and how each item renders

These come from the 26.1.2 client jar (`assets/minecraft/items/*.json`, `models/item/*`,
`textures/item/*`). Seven materials exist: **wooden, stone, copper, iron, golden, diamond,
netherite**. Copper tools and **spears** are new since 1.21 and are easy to forget.

| Item(s) | Item definition (`items/<id>.json`) | Model → texture | What to replace |
|---|---|---|---|
| `<mat>_sword/_axe/_pickaxe/_shovel/_hoe` (35) | `minecraft:model` → `item/<id>` | parent `item/handheld`, `layer0: item/<id>` | `textures/item/<id>.png` |
| `mace` | `model` → `item/mace` | parent `item/handheld_mace` | `textures/item/mace.png` |
| `<mat>_spear` (7) | `select` on `display_context`: `gui/ground/fixed/on_shelf` → `item/<id>`; fallback `item/<id>_in_hand`; `swap_animation_scale: 1.95` | icon: parent `generated`; in-hand: parent `item/spear_in_hand` | `textures/item/<id>.png` **and** `<id>_in_hand.png`, which is **already 32×32 in vanilla** |
| `bow` | `condition using_item` → `range_dispatch use_duration` (scale 0.05; 0.65 → pulling_1, 0.9 → pulling_2; fallback pulling_0) | parent `item/bow` | `bow.png`, `bow_pulling_0/1/2.png` |
| `crossbow` | `select charge_type`: `arrow` → `crossbow_arrow`, `rocket` → `crossbow_firework`; fallback `condition using_item` → `range_dispatch crossbow/pull` (0.58, 1.0) | parent `item/crossbow` | `crossbow_standby.png`, `crossbow_pulling_0/1/2.png`, `crossbow_arrow.png`, `crossbow_firework.png` |
| `trident` | `select display_context`: GUI/ground/frame/shelf → `item/trident` (flat, `generated`); in hand → `special` `minecraft:trident`, base `trident_in_hand` or `trident_throwing` | the in-hand and thrown trident is the **entity model** | `textures/item/trident.png` (icon) + `textures/entity/trident/trident.png` (32×32 entity UV sheet) |
| `shield` | `condition using_item` → `special` `minecraft:shield`, base `item/shield` or `item/shield_blocking` | **entity model**, in every context | `textures/entity/shield/shield_base_nopattern.png` (64×64); `shield_base.png` is the one used under banner patterns |

Model types are documented on the Minecraft wiki
([Items model definition](https://minecraft.wiki/w/Items_model_definition)). Everything in the table
was read from the jar itself.

### Does a 32×32 item texture work?

**Yes, in every context.**

- The flat-item mesh is built from the sprite's real size. The client's
  `net.minecraft.client.resources.model.cuboid.ItemModelGenerator` reads `SpriteContents.width()` and
  `height()` when it extrudes the layer (read from 26.1.2 bytecode with `javap`; Mojang's 26.x jars
  ship unobfuscated). So a 32×32 `layer0` gets per-pixel edges at twice the density.
- Hand, inventory, dropped item, item frame and shelf all use the same baked model with the parent's
  `display` transforms (`handheld`, `generated`), so they all work.
- Vanilla itself mixes resolutions: the 26.1.2 `*_spear_in_hand.png` and `entity/trident/trident.png`
  are 32×32, while the other item textures are 16×16.
- Item textures live in their own `items` atlas in 26.1 (`atlases/items.json`: `directory` source
  `item/`), which is why custom textures must sit under `textures/item/`. CraftEngine's wiki says the
  same ([item models](https://xiao-momi.github.io/craft-engine-wiki/getting_start/item_models)).

Gotchas:

- **Keep sizes powers of two.** Anything else "cause[s] the client to downgrade mipmapping" (same wiki page).
- **Thickness looks different.** The extruded depth stays one model unit (1/16 block) whatever the
  resolution, so a 32px item looks relatively thinner per pixel than a 16px one. This is cosmetic and
  **unverified in game**.
- **Orientation.** Vanilla handheld art runs from bottom-left (grip) to top-right (tip). RPG icons
  mostly match, but some are drawn the other way or centred, and those need redrawing, not just a
  recolour.
- **PvP readability.** A fantasy re-skin must still let players tell diamond from netherite at a
  glance. Keep each material's colour identity, and change the silhouette and detail, not the tier
  colour.
- **Shield and trident are entity models** (`special`), so an item PNG does nothing for their held
  look.
  - To re-skin them, replace the entity textures. Higher resolutions work because entity UVs are
    normalised (per the wiki; **unverified in game**).
  - Or redefine `items/shield.json` / `items/trident.json` to point at a plain Blockbench model.
    Doing that for the shield **loses banner patterns**. Defer both to a later pass.
- **Bow and crossbow** need all 4 and 6 states drawn in a consistent style, or the draw animation
  "jumps".

### Shipping them in the CraftEngine pack

Put the PNGs under
`dev-server/plugins/CraftEngine/resources/avian/resourcepack/assets/minecraft/textures/item/`
(and `.../textures/entity/...` for shield and trident). **No `items/*.json` or `models/item/*.json` is
needed** as long as we only swap textures: the vanilla definitions already point at those paths.

Conflicts with CraftEngine's own merging:

- **No other enabled pack touches these files today.** The enabled packs in `run/plugins/CraftEngine/resources/`
  are `avian`, `internal`, `default_templates`, `remove_shulker_head` and `customcrops`. The only
  vanilla files they ship are `internal`'s `textures/item/custom/gui/*` plus two chainmail trim
  textures, and `remove_shulker_head`'s shulker texture and shader. The generated zip's only vanilla
  item definition is `items/arrow.json` (CraftEngine's GUI buttons, via `custom_model_data`). So
  `duplicated-files-handler` never fires for weapon textures.
- **If a duplicate ever appears** (a merged external pack), the existing rules merge `minecraft/items`
  JSON deeply and `minecraft/models/item` as legacy models. There is **no rule for PNGs**, and
  CraftEngine only "issue[s] a warning ... in the console" when no rule matches
  ([file_conflict](https://xiao-momi.github.io/craft-engine-wiki/reference/file_conflict)).
- **The pack is modern-only, so vanilla item definitions are left alone.** Our `config.yml` has
  `supported-version.min: server` (26.1.2) and `always-use-custom-model-data: false`, so CraftEngine
  gives its own items an `item_model` and does not generate `custom_model_data` overrides
  ([models → root fields](https://xiao-momi.github.io/craft-engine-wiki/configuration/item/models)).
  Legacy overrides are also skipped once the pack minimum is 1.21.4 or higher
  (`generateLegacyItemOverrides`, [AbstractPackManager.java](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/core/src/main/java/net/momirealms/craftengine/core/pack/AbstractPackManager.java)).
- **Even with `custom_model_data`, our file survives.** `generateModernItemOverrides` reads an
  existing `assets/minecraft/items/<id>.json` from our pack and uses it as the `fallback` of the
  `range_dispatch` it wraps around it (same file). It only uses its built-in vanilla preset when we
  ship none. A hand-written `items/diamond_sword.json` would survive.
- **The `bypass-*` lists** (`@vanilla_textures`, `@vanilla_models`, `@vanilla_item_model`) are only
  used by pack **obfuscation**, which is Premium and off for us (`config.yml` `protection.obfuscation`).
  They don't affect us.
- **Reload.** A texture change needs `/ce reload all`, not `config`
  ([item models](https://xiao-momi.github.io/craft-engine-wiki/getting_start/item_models)). That
  touches the running dev server, so ask first.

## 2. Custom items

### How CraftEngine defines them

This is YAML under `resources/<pack>/configuration/*.yml`
([item](https://xiao-momi.github.io/craft-engine-wiki/configuration/item)).

- **Base.** `material:` is the vanilla base item (default `nether_brick`).
- **Look.** Three options, simplest first:
  - `texture:` alone. CraftEngine picks the parent from the material (a tool becomes `handheld`),
    generates the model, and sets `item_model: <ns>:<id>`.
  - `model:` with `generation:`, for full control.
  - An external model path.
  - Multi-state bases use a `textures:`/`models:` list in fixed slot order: bow 4, crossbow 6,
    shield 2, spear 2, fishing rod 2
    ([models → simplified](https://xiao-momi.github.io/craft-engine-wiki/configuration/item/models)).
- **Components.** `data:` sets vanilla data components:
  - `item_name`, `lore` (MiniMessage), `max_damage`, `unbreakable`, `attribute_modifiers`,
    `enchantment`, `equippable`, `tooltip_style`, `pdc`
  - a raw `components:` map for anything else, such as `minecraft:tool` mining speed
  ([data](https://xiao-momi.github.io/craft-engine-wiki/configuration/item/data)).
- **Plugin-side mechanics.** `settings:` holds `break_power`, `repairable`, `anvil_repair_item`,
  `prevent_break`, `tags`, CraftEngine's own attribute system and `equipment_potion_effects`
  ([settings](https://xiao-momi.github.io/craft-engine-wiki/configuration/item/settings)).
  `behavior:` and `events:` handle interactions.
- **Premium-only features.** Client-bound models/data and conditional processors are gated on
  `VersionHelper.PREMIUM`
  ([Config.java L654](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/core/src/main/java/net/momirealms/craftengine/core/plugin/config/Config.java),
  [ItemProcessors.java L59](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/core/src/main/java/net/momirealms/craftengine/core/item/processor/ItemProcessors.java)).
  So our `client-bound-model: true` in `config.yml` is inert on Community.

### Giving and recognising them

- **Giving.**
  - Commands: `/ce item get` and `/ce item give` (`commands.yml` in the 26.9.1 jar, permissions
    `ce.command.admin.get_item` / `give_item`).
  - API: `CraftEngineItems.byId(key).buildBukkitItem()`, plus `isCustomItem(stack)` and
    `getCustomItemId(stack)`
    ([CraftEngineItems.java](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/bukkit/src/main/java/net/momirealms/craftengine/bukkit/api/CraftEngineItems.java)).
    Maven: `repo.momirealms.net/releases/`
    ([api](https://xiao-momi.github.io/craft-engine-wiki/api)).
- **What goes on the stack.** CraftEngine writes the id as the string `craftengine:id` at the **root
  of `minecraft:custom_data`**
  ([ComponentItemFactory1_20_5.java](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/bukkit/src/main/java/net/momirealms/craftengine/bukkit/item/factory/ComponentItemFactory1_20_5.java),
  [IdProcessor.java](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/core/src/main/java/net/momirealms/craftengine/core/item/processor/IdProcessor.java)).
  - That is **not** Bukkit's PersistentDataContainer, which lives in the `PublicBukkitValues`
    compound inside `custom_data`
    ([CraftItemStack.java L572](https://github.com/PaperMC/Paper/blob/main/paper-server/src/main/java/org/bukkit/craftbukkit/inventory/CraftItemStack.java)).
  - So `getPersistentDataContainer()` cannot see CraftEngine's id. Reading it needs the CraftEngine
    API or raw NBT.
  - CraftEngine's `data.pdc:` does write into `PublicBukkitValues`
    ([PDCProcessor.java](https://github.com/Xiao-MoMi/craft-engine/blob/26.9/core/src/main/java/net/momirealms/craftengine/core/item/processor/PDCProcessor.java)).
    A CraftEngine item can therefore also carry an `avian:` PDC tag. We would still have two
    identities to keep in sync.
- **Our rule** ("identify custom items by PersistentDataContainer"). It holds as long as *our* code
  keeps its own PDC key as the source of truth. CraftEngine items are fine for **pure-cosmetic or
  config-only** things that no Avian code needs to recognise.

### The Harvester Hoe today

`avian-economy/.../HarvesterHoe.java` builds the hoe:

- a `NETHERITE_HOE` with PDC `avian:harvester_hoe` (a per-hoe UUID)
- per-track levels under `avian:hoe_*`
- `setUnbreakable`, `itemName(Brand.title(...))`, a glint override, and lore rebuilt from the PDC in
  `refreshLore`

It already follows the rule, and nothing about it needs CraftEngine.

**Option A (recommended): our plugin sets `item_model`.**

- **Plugin change.**
  - In `create()`, add `meta.setItemModel(new NamespacedKey("avian", "harvester_hoe"))`. Paper API:
    `ItemMeta#setItemModel(NamespacedKey)`
    ([ItemMeta.java L571](https://github.com/PaperMC/Paper/blob/main/paper-api/src/main/java/org/bukkit/inventory/meta/ItemMeta.java)).
    `DataComponentTypes.ITEM_MODEL` is the data-component equivalent.
  - Read the key from `HoeConfig`, since every value must be configurable.
  - Set it in `refreshLore` too, so hoes that already exist pick it up the next time they are
    touched.
- **Pack.** Add three hand-written files under `resources/avian/resourcepack/assets/avian/`:
  - `items/harvester_hoe.json`: `{"model":{"type":"minecraft:model","model":"avian:item/harvester_hoe"}}`
  - `models/item/harvester_hoe.json`: parent `minecraft:item/handheld`, `layer0: avian:item/harvester_hoe`
  - `textures/item/harvester_hoe.png`: 32×32

  CraftEngine copies them verbatim. **Don't** also define a CraftEngine item with the id
  `avian:harvester_hoe`. Its generated `assets/avian/items/harvester_hoe.json` would collide with ours:
  CraftEngine warns `resource_pack.item_model.conflict` and skips its own copy (`generateModernItemModels1_21_4`).
- **Coupling.** It is one string. If we ever leave CraftEngine, the three files move with the vanilla
  pack unchanged.
- **Without the pack.** A client without the pack would render a missing model (per the Minecraft
  wiki's item_model semantics; **unverified in game**). Our pack is required
  (`kick-if-declined: true`), and Bedrock sees the base item (§4).

**Option B: make it a CraftEngine item.** We would define `avian:harvester_hoe` in YAML and build it
with `CraftEngineItems.byId(...).buildBukkitItem()`, then add our PDC.

- It gains nothing we need, because the hoe's behaviour is all ours.
- It adds a compile dependency on a GPL-3.0 API, a second identity (`craftengine:id`), a load-order
  dependency, and admins able to `/ce item give` a hoe with no UUID or levels.
- **Reject it.**

The same pattern covers crate and boss drops. Our plugin builds the item (PDC + `item_model`), and
CrazyCrates prizes hand it out through a command. Whether CrazyCrates' own YAML item builder can set
`item_model` directly is **unverified**; our `config.yml` only shows `custom-model-data`.

## 3. Art sourcing

### Raven Fantasy Icons Premium (Clockwork Raven Studios)

Source: [itch.io page](https://clockworkraven.itch.io/raven-fantasy-icons), fetched 2026-09-25.

- **Price.** "Name your own price". The **Premium** file unlocks "if you pay $35 USD or more" (98 MB).
  The three add-on zips (Pets and Animals, Crops and Food 2, Symbols and Runes) are listed at the
  same $35 tier. The Free file is 7.9 MB.
  - The page also offers the **"Raven Fantasy Icons – Full Collection"** bundle: 45 items for
    **$35.50**, "Regularly $164.00", "Offer ends in …"
    ([bundle](https://itch.io/s/87674/raven-fantasy-icons-full-collection)).
  - The bundle includes older sets such as "RPG Icon Pack – 800+ Weapons" and "RPG Icon Pack –
    General Itens and Tools" (192 icons). For the same money it is strictly more, so **buy the bundle**
    while the sale runs.
- **Content.**
  - "8000+" icons on the product line. The author says in the comments that it "currently includes
    more than 10000+ icons" and is updated "almost every month".
  - "All sprites in the main asset come in 16x16, 32x32, and 64x64 ... with a dark outline", as a
    sheet and as separate PNGs.
  - Categories visible on the page include weapons, skills, status effects, map markers and "Epic
    Weapons 3". The author confirms **there is no catalogue** ("since it's a lot of icons, I haven't
    made a list").
  - So **whether every tool type (hoe, shovel, pickaxe) is covered is unverified.** The bundled
    "General Itens and Tools" set suggests it is.
- **Native resolution.** In the free edition (`~/projects/minecraft/art-reference/`, looked at, not
  copied), the 32×32 sheet is **exactly a 2× nearest-neighbour upscale** of the 16×16 sheet: 0 of
  2,244,608 pixels differ, ignoring the RGB of fully transparent pixels. The page calls it "Classic
  16x16 Pixel art style". The Premium edition is presumably the same (**unverified**).
- **Licence.** The page summary says: "This asset can be modified to match any of your needs. This asset can be used in any
  project, even commercial and physical(print or tabletop) ones, but it cannot be distributed or sold
  as a separate product without the creator's permission (me)". It also says the free version's
  "Personal use includes any projects or game released for free with no microtransactions and/or paid
  advertisement/ad", and **we sell ranks, so we need Premium.**
- **Licence PDF.** The full terms are "Clockwork Raven General Licence v.1.2.pdf", linked from the
  page ([Drive folder](https://drive.google.com/drive/folders/121s8vaEk2h2Y-3cHDlfkKcwojsySYh3q)).
  The clauses that matter:
  - 3a: "can be used as part of any personal or commercial digital and physical/print Product, this
    include: Video Games, ... As long as they are tied to a content and are not being distributed as
    assets as they are."
  - 3b: "Not sell or distribute this assets(s) as a separate product ..."
  - 3c: "Modify the assets to suit your needs if necessary. The Modified asset still are part of this
    license ..."
  - 3e: we may not use "the Clockwork Raven name associated with any product, marketing campaign, social media
    except for giving credits".
  - 3f: "Do not train the assets into a machine, AI or any software that may produce derivative or
    visually similar work." Remixing must be manual or a deterministic palette-swap script, never
    generative.
  - FAQ h, on mods: "As long as you specify that the assets cannot be used or redistributed to your
    users, and as long as the mod is free, you can use it."
- **The Minecraft problem.** A server resource pack is closest to that "mod" case. Every player
  downloads a zip with the PNGs inside. So:
  1. Email the author (address in the PDF) for a written yes before buying.
  2. Put a "© Clockwork Raven – not for reuse or redistribution" line in the pack (`pack.mcmeta`
     description, or a `CREDITS.txt` in the pack root).
  3. Remix heavily enough that the textures are ours in look.

  CraftEngine's anti-extraction `protection` would help, but it is Premium.

### Alternatives (commercial licences quoted)

| Pack | Price | Size | Covers | Licence (quoted) | Fit |
|---|---|---|---|---|---|
| [Pixel Fantasy RPG Icons Weapons 32x32](https://cyangmou.itch.io/pixel-fantasy-rpg-icons-weapons-32x32), Thomas Feichtmeir "Cyangmou" | **$44.99** | true 32×32 (also sold at 16 and 24) | swords, spears/polearms, axes/maces/hammers, bows/crossbows, daggers, shields. **No pickaxe/shovel/hoe** (the tools would be in his [Ultimative Basic Iconset 32×32](https://cyangmou.itch.io/pixel-art-fantasy-ultimative-basic-iconset-32), $29.99, **unverified contents**) | "You CAN edit and repurpose ... use the assets in commercial and free projects. You CANNOT redistribute / resell". The [full agreement](https://docs.google.com/document/d/1tNBRxfKhTqwgsrTTRD-nMbe6zew1c-u7yMaOj6vOKrI/edit) limits a purchase to "a single Successful Media Product" and says "A mod is under this license not a Media Product ... If you want to use assets for a mod, please reach out to me via e-mail". It also forbids letting users "extract the Asset(s)" | The best true-32px quality. The licence **explicitly** needs a custom deal for our use, and a resource pack is extractable by design. |
| [Pixel Icons – Medieval Weapons Pack [32x32]](https://eldyth.itch.io/pixel-icons-medieval-weapons-pack-32x32), Eldyth | **$1+** | 32×32 | 13 swords, 13 great swords, 12 axes, 12 great axes, 13 curved swords, 5 bows, 3 crossbows | "Use the assets in personal, non-commercial, or commercial projects. Include the assets in finished products such as games ... You may NOT: Resell, redistribute, or share the assets as standalone files." | Cheap and true 32px, with a clean licence for "finished products". No tools, shield, trident or mace. Good as **reference and extra silhouettes** for boss and crate weapons. |
| [Item Icons Asset Pack 32x32](https://kolovrat-studio.itch.io/item-icons-asset-pack), Kolovrat Studio | name your price | 32×32, 27 icons (with `.aseprite`) | bow, axe, crossbow, shield, sword, armour, misc | "free to use it in both free and commercial projects. You can also edit these sprites ... **Giving credit is mandatory.**" | Too small for a vanilla set. Fine for a one-off drop. |

None of these three says anything about resource packs or server distribution. Treat all of them like
Raven: ask first.

### Commissioning

Published per-texture rates from sellers who list Minecraft item textures:

- [Rporotos on Fiverr](https://www.fiverr.com/rporotos/do-custom-item-textures-for-minecraft):
  "Item Texture (16x16): $5", "Item Texture (32x32): $12 (If you only want to order 1 texture: $10)",
  and "Bows, Crossbow, Shields and Tridents counts as 2 textures".
- [YourDed4Dey on itch.io](https://yourded4dey.itch.io/commission-info):
  "$5: 16x16 $15: 32x32 $30: 64x64 Animated: + $15".

Neither is a vetted studio. These are market data points, not recommendations. Get commercial and
exclusive rights in writing.

**Scope of a full vanilla set:**

| Items | Textures |
|---|---|
| 5 tool types × 7 materials | 35 |
| 7 spears × 2 (icon and in-hand) | 14 |
| Bow | 4 |
| Crossbow | 6 |
| Trident (icon and entity) | 2 |
| Mace | 1 |
| Shield (entity) | 1 |
| **Total** | **≈ 63** |

That comes to about **$315 at 16px** or **$630–950 at 32px**. The first Avian items (hoe, 2–3 crate
weapons, 2–3 boss drops) are about 5–10 textures, so **$50–150 at 32px**.

### How licensed and remixed art fits SPEC §61

§61 asks for "an original 32x32-style resource pack" with "its own identity", and "Do not reuse
VanityMC's old resource pack". Licensed stock art is legal to use, but it is not original. Other
servers can buy the same Raven icons, and its native resolution is 16px. So the split is:

- **Vanilla re-skins** are the "background" of the pack. They should look fantasy-RPG and coherent,
  but players rarely identify a server by its iron shovel. Licensed and remixed art is acceptable
  here, as long as it is remixed into our palette and shapes rather than dropped in raw. That
  also reduces sameness with other servers.
- **Avian-specific items** carry the identity §61 asks for: the Harvester Hoe, keys, crates, tokens,
  and boss and crate weapons. Commission these at 32×32, with our sigil motifs and the
  stone/gold/red/purple palette, and own them outright.
- **Resolution mix.** 16px vanilla items next to 32px signature items is a deliberate hierarchy, and
  vanilla already mixes the two (spear in hand). **Owner decision:** if §61's "32x32-style" must
  cover every item, the vanilla set has to be commissioned (about $630–950), or Cyangmou licensed on
  custom terms.

## 4. Bedrock (Geyser)

- **Retextured vanilla items.** Bedrock players see **Bedrock's own vanilla textures**. Geyser never
  forwards or converts the Java pack. The [Geyser custom items docs](https://geysermc.org/wiki/geyser/custom-items/)
  say "Geyser does not convert resource packs from Java Edition".
  - GeyserMC's PackConverter (Thunder) does map the item textures (`bow → bow_standby`,
    `diamond_sword`, spears → `spear/*`, shield patterns …) in its
    [`mappings/textures.json`](https://github.com/GeyserMC/PackConverter/blob/master/converter/src/main/resources/mappings/textures.json).
  - A converted pack could go in `plugins/Geyser-Spigot/packs/`. Its README still warns "should not be
    used on production".
- **Custom items (the `item_model` route).**
  - Geyser's current **v2 mappings** match "Java item + `item_model`" (plus optional predicates) to a
    Bedrock custom item. `SingleDefinitionReader` reads the Java `model` identifier and a
    `bedrock_identifier`
    ([source](https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/registry/mappings/definition/SingleDefinitionReader.java)).
    v1 ("custom_model_data") is deprecated since API 2.9.3 (docs above).
  - It still needs a hand-made **Bedrock resource pack** (`item_texture.json` + textures) and a
    mappings file. Geyser "does not generate custom item mappings automatically". GeyserMC's
    [Rainbow](https://github.com/GeyserMC/Rainbow) client mod can generate both.
  - Without mappings, a Bedrock player sees the **base item** (a netherite hoe) with our name and
    lore. This is inferred from Geyser translating by Java item id, and is **unverified in game**.
- **Worth it now?** **No.** Every custom item works on Bedrock as its base item, just without the art.
  - The Harvester Hoe's function lives in PDC and is unaffected.
  - The `item_model` route is exactly what Geyser v2 keys on, so nothing we build now blocks adding
    it later.
  - Revisit once the Java art has settled and we know how many Bedrock players we have.

## 5. Plan

1. **Owner decisions** (below). Then email Clockwork Raven with the resource-pack question. On a yes,
   buy the Full Collection bundle ($35.50 while the sale lasts, else Premium at $35).
2. **Where files live** (all in git; art sources are never copied raw):
   - `dev-server/plugins/CraftEngine/resources/avian/resourcepack/assets/minecraft/textures/item/*.png`
     for the vanilla re-skins
   - `.../assets/minecraft/textures/entity/{shield,trident}/…` later
   - `.../assets/avian/{items,models/item,textures/item}/…` for our own items
   - Keep working files (`.aseprite`, palettes) outside the pack folder, e.g. `art/` at the repo root.
     `exclude-file-extensions` does not list `.aseprite`, so one inside the pack would be zipped.
   - Add a credits file in the pack.
3. **First slice: one sword, end to end.**
   1. Add a single `diamond_sword.png` (16×16 remix; optionally a 32×32 test) under
      `assets/minecraft/textures/item/`.
   2. Run `/ce reload all`. **Ask before touching the dev server.**
   3. Check the hand (first and third person), the inventory, a dropped item, an item frame and the
      enchant glint.
   4. Check that a Bedrock client still shows a vanilla sword.
   5. Then do all seven swords, to settle the material-tier palette before doing the other 40 items.
4. **Second slice: the Harvester Hoe's art.**
   1. Add `HoeConfig.itemModel` (default `avian:harvester_hoe`), set in `create()` and `refreshLore()`.
      Extend `HarvesterHoeTest` to assert the component. Whether MockBukkit supports `setItemModel` is
      **unverified**.
   2. Add the three pack files, using a placeholder 32×32 texture until the commission lands.
5. **Then**, in order:
   - the rest of the tools and weapons
   - the bow and crossbow states
   - the spears
   - the shield and trident, which need entity textures or model replacement: a separate decision
6. **Commission** the 32×32 signature set (hoe, first crate and boss drops) with a written brief:
   palette, sigil motifs, grip at the bottom-left, and exclusive commercial rights.

## Open questions for the owner

- **Is 16×16 acceptable** for the re-skinned vanilla set, with 32×32 reserved for Avian items? Or must
  everything be 32px, which means commissioning about 63 textures for roughly $630–950?
- **Is it OK to email Clockwork Raven** and wait for a written yes before buying? If the answer is
  no, the fallback is to commission.
- **Shield and trident:** re-skin the entity textures only, or swap in custom models? A custom shield
  model loses banner patterns.
- **Commission budget and artist choice.** Neither data-point seller above has been vetted.
- **Bedrock art:** confirm "later, not now".
