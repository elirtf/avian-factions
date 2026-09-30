# Economy

How money and tokens are earned and spent, and why the numbers are what they are. Prices live in
EconomyShopGUI's shop files (`dev-server/plugins/EconomyShopGUI/shops/`); everything here is
configurable, and this page is the reasoning to keep in step when a number changes.

## The shape

The two best ways to earn are, by design:

1. **Sugar cane farming**: money from selling it (auto-farms included), and **tokens** from
   harvesting grown cane by hand.
2. **Spawners**: auto-farmed mob drops. Higher tiers cost more and earn more per spawner.

Everything else (mining, other crops) earns, but less.

## Selling: a short list, never the whole catalogue

Only raw farm, mob and ore items can be sold (70 shop entries). Crafted items, blocks, gear,
potions and food (other than meat) cannot. That removes every "buy cheap, craft, sell high" loop at
once, and keeps money tied to farming and spawners. It also means a buy price is never below its
sell price. Items that craft or smelt back into a sellable one (nuggets, raw ore, ore blocks,
hay, slime blocks, melons) are never cheaper to buy than what they turn into.

| Crops | $ each | | Mob drops | $ each | | Ores | $ each |
|---|---|---|---|---|---|---|---|
| **Sugar cane** | **16** ($1,024 a stack) | | Rotten flesh | 12 | | **Diamond** | **150** |
| Cactus | 4 | | Bone | 10 | | Iron ingot | 40 |
| Pumpkin | 3 | | String | 10 | | Gold ingot | 8 |
| Wheat, carrot, potato, beetroot, nether wart, cocoa, chorus | 2 | | Spider eye | 8 | | Coal | 5 |
| Melon slice, berries, bamboo, kelp, mushrooms | 1 | | Gunpowder | 30 | | Quartz | 6 |
| | | | Ender pearl | 60 | | Lapis | 4 |
| | | | Slime ball | 15 | | Amethyst | 3 |
| | | | Magma cream | 25 | | Netherite scrap / debris | 750 |
| | | | **Blaze rod** | **120** | | Netherite ingot | 3,000 |
| | | | | | | Redstone | 2 |
| | | | Raw meat 1, cooked 2, feather 2, leather 5 | | | Storage blocks | 9 × |

Emeralds and copper are not sellable: villager trading and copper scraping would otherwise be free
money.

**Sugar cane in numbers.** Farm crops grow at 120 % of vanilla speed (`spigot.yml` growth
modifiers; saplings, vines and mushrooms are unchanged), so a cane column yields about 4 cane an
hour. At $16 each ($1,024 a stack), an early 200-column farm makes about $12,500 an hour and a
5,000-column auto-farm about $320,000: early on, cane out-earns any spawner a new faction can
afford, and it stays competitive at scale. Hand-harvesting also pays **tokens**: a 2 % chance of 1 token per grown
block (`economy.conf`: `sugar-cane-token-chance`, `sugar-cane-tokens`). That is about 20–40
tokens an hour of active farming. Cane the player placed never pays tokens, so place-and-break
earns nothing, and pistons and observers earn money but no tokens.

## Spawners: the tier ladder

Each spawner is priced to **pay itself back in about 8 hours** of running. That assumes about
500 mobs an hour at vanilla rates with drops sold at the prices above, so price ≈ drop value per
kill × 4,000. Looting and stacking change the speed, not the ratios. F-Top counts each spawner at
its shop price (`ftop.conf`), so F-Top ranks what a faction invested.

| Tier | Spawner | Price | Drop value / kill |
|---|---|---|---|
| 1 | Chicken, pig, sheep | $15,000 | ~$3 |
| 1 | Cow | $25,000 | ~$8 |
| 2 | Zombie, skeleton, spider, cave spider | $50,000 | ~$12 |
| 3 | Creeper, enderman, slime, magma cube | $125,000 | ~$30 |
| 4 | **Blaze** | $250,000 | ~$60 |
| 5 | **Iron golem** | $600,000 | ~$160 |

Only these 14 are sold. Villagers can no longer summon iron golems (`economy.conf`:
`villager-iron-golems=false`); a vanilla iron farm would otherwise make golem spawners pointless.

## Buying

- **Everything else**: the plugin's default prices, rounded up to whole dollars (minimum $1).
- **Tools and weapons** by material tier: wood $100, stone $250, copper $500, gold $1,500,
  iron $2,500, diamond $15,000, netherite $60,000. That is the price of a pickaxe, axe or sword;
  spears cost ¾ of it, shovels and hoes ½.
- **Armor**: leather $250, copper $500, gold $1,500, chainmail $2,000, iron $2,500,
  diamond $15,000, netherite $60,000. Multiply by 1¼ for the helmet, 2 for the chestplate,
  1¾ for the leggings and 1 for the boots. A full diamond set is $90,000.
- **Other gear**: bow $500, crossbow $750, shield $500, trident $25,000, mace $100,000, totem
  $25,000, elytra $250,000.
- **Never sold** (#40): potions of any kind and enchanted books (both sections, and the Magic
  button, are off), golden apples, enchanted golden
  apples and dragon's breath. They are all obtainable in game, just not bought.

Buying gear is a convenience premium: mining diamonds and crafting a pickaxe still costs three
diamonds.

## The Harvester Hoe

The signature farming tool (spec §31, `hoe.conf`). Every player can claim one free each week with `/kit harvester`; another costs **$25,000** from `/hoe`, then hold it
and run `/hoe` to upgrade it with tokens. Breaking sugar cane with it takes every block above the
root, so nothing needs replanting. The cane sells on the spot at `/shop` prices, and each grown
block rolls for tokens just like hand-harvesting.

| Upgrade | Per level | Levels | Token costs |
|---|---|---|---|
| Harvest radius | one more ring of columns (3x3, then 5x5) | 2 | 250, 1,000 |
| Token boost | +25 % token chance | 5 | 100, 250, 500, 1,000, 2,000 |
| Money multiplier | +5 % sell price | 5 | 150, 400, 800, 1,500, 3,000 |
| Random drops | turns drops on; each level multiplies their chance | 3 | 200, 600, 1,500 |
| Cultivation | +20 % AuraSkills farming XP | 5 | 100, 250, 500, 1,000, 2,000 |

Every harvest also gives AuraSkills farming XP (2 per grown block, like a hand break), so the
hoe feeds the RPG side as VanityMC's did. A fully upgraded hoe (19,600 tokens in all) harvests 25 columns a swing at 1.25× price, with
double the token chance. Random drops are a config list of chance → tokens and/or a console
command, so crate keys plug in with no code once crates exist: for example
`{ chance=0.0005, command="crazycrates give virtual feather 1 {player}", message="a Feather Crate key" }`.
The one drop today is 25 bonus tokens at 0.2 % per grown block.

The hoe is identified by its item data, not its name, and its levels are stored on the item, so it
keeps them when traded or stored. Neighbouring columns are only harvested where the player could
break them (claims, WorldGuard). Admins can give one with `/hoe give <player>`
(`avian.hoe.admin`).

## Sell wands

Right-click a chest, barrel, hopper or any container with a sell wand: everything `/shop` buys is
sold at the player's own shop price, times the wand's multiplier, and paid as `sellwand:<tier>` in
the audit trail. Sneak and right-click to see what it's worth without selling. It only works on
containers the player could open, so another faction's chests are safe. Our own items (the
Harvester Hoe, wands) are never sold.

| Tier | Item | Uses | Multiplier |
|---|---|---|---|
| `basic` | Stick | 100 | ×1 |
| `gilded` | Blaze rod | 500 | ×1.1 |
| `eternal` | End rod | unlimited | ×1.25 |

Tiers live in `sellwand.conf`. Admins, crates and shops hand them out with
`/sellwand give <player> <tier> [amount]` (`avian.sellwand.admin`).

## Token shop

`/tokenshop` (or `/tshop`) is where tokens are spent. It's a DeluxeMenus menu
(`dev-server/plugins/DeluxeMenus/gui_menus/token_shop.yml`), and it's the in-game route to every
key and wand the store sells (ADR-0006).

| Item | Tokens | Why this price |
|---|---|---|
| Common key | 200 | About 5–10 hours of hand farming: a first goal for a new player |
| Rare key | 600 | |
| Epic key | 1,500 | |
| Mythic key | 10,000 | Hoe-farming territory; also reached through the crate chain |
| Cosmetic key | 800 | Tags and pets only; a duplicate pays 150 back |
| Sell wand | 250 | Cheap enough to be a new player's first auto-farm tool |
| Gilded sell wand | 1,500 | |
| Eternal sell wand | 12,000 | Permanent ×1.25 on every chest: the long-term token sink |

Each key costs 2½ to 3 times the one below. A key never returns more tokens on average than it
costs. A Mythic crate averages about 500 tokens in prizes, plus a 1-in-10 chance of two Epic keys:
about 800 tokens in all against a 10,000 price. So buying keys can never be a token farm. Right-click a key to buy five at
the same unit price.

Every button runs `tokens charge <player> <cost> <reason> <reward command>` as the console. The
withdrawal is atomic, so a double-click can't spend the same tokens twice. The reward runs only
if the payment went through, and it's refunded if the reward fails (for example, an unknown
command or a player who logged out). Both halves land in the audit trail as `shop:<item>` and
`refund:shop:<item>`. To add an item, copy a button and change the cost, reason and command. Never
give a reward from a separate line: it would pay out whether or not the charge succeeded.
`gems charge` works the same way for a future gem shop.

**The Enchanter** (`/enchanter`) sells random custom-enchant books for **XP levels**, not tokens:
Common 10, Rare 20, Legendary 30, Mythic 40. See [ENCHANTS.md](ENCHANTS.md).
## Auction house

`/ah` (CrazyAuctions, `dev-server/plugins/CrazyAuctions/`): players sell to players, at a fixed price
(`/ah sell <price>`) or by auction (`/ah bid <start price>`). It's the money sink the shop can't be,
because the money changes hands between players:

| | |
|---|---|
| Listing fee | **$100**, paid up front and kept if the item doesn't sell, so listings aren't spam |
| Tax | **5 %** of every sale |
| Price range | $10 to $10,000,000 (the plugin's default $1M cap was below an iron golem spawner) |
| Listings at once | Hatchling 5, Fledgling 8, Harpy 10, Griffin 12, Wyvern 15, Dragon 18, Phoenix 21 (`crazyauctions.sell.<n>` / `bid.<n>` in `ranks.lp`) |
| Duration | 2 days for a fixed-price listing, 2.5 minutes for an auction; unsold items wait 10 days in `/ah collect` |

Damaged items can't be listed. Every player needs a `crazyauctions.sell.<n>` permission: with none,
the plugin allows **unlimited** listings. Listings are stored in the plugin's own files, not
MariaDB. That's fine, since the hub won't share this market (owner, 2026-09-29).

## PvP consumables

Cooldowns in `combat.conf`:

- Enchanted golden apple: 60 s.
- Golden apple: 10 s.
- Totem of undying: 60 s. A totem that pops during its cooldown does not save the player.
- Ender pearl: 16 s (vanilla is 1 s), as on HCF servers.

The cooldown shows on the item like a vanilla one and survives relogging and dying, so relogging
never resets a pearl.

## Crates

Five crates in `/crates` (CrazyCrates, `dev-server/plugins/CrazyCrates/crates/`). Each is a shulker box in its
tier colour: Common lime, Rare blue, Epic purple, Mythic red (owner, 2026-09-29). The Legendary gear crate
was removed the same day (its best prizes moved into Epic) so "Legendary" names only an enchant tier.
Left-click opens a crate, right-click previews its prizes. They are virtual:
keys are held on the player and a crate opens from the menu, so no crate blocks are placed and they
work in whichever world spawn ends up in. Every crate can also drop a key for the tier above.

| Crate | Main prizes | Key sources (ADR-0006: every sold key needs one) |
|---|---|---|
| Vote | $1k–2.5k, 10–25 tokens, iron, XP bottles | Voting (plugin to come). Not sold. |
| Common | $5k–10k, 50 tokens, iron tools, 8 diamonds | Vote crate (5 %); token shop |
| Rare | $25k, 150 tokens, diamond gear, zombie spawner, 2 golden apples | Common crate; token shop |
| Epic | $75k, 400 tokens, Prot III diamond, Sharp V netherite sword, netherite ingot, skeleton/creeper/blaze/enderman spawners, totem | Rare crate; Mythic crate (2 keys); token shop |
| Mythic | $500k, 3,000 tokens, full Prot IV netherite, iron golem and blaze spawners, mace, elytra, 2 notch apples | Epic crate; token shop |

The rarest PvP items (golden apples, notch apples, totems) sit at low weights, per #40. Admins give
keys with `/crates give virtual <Crate> <amount> <player>`; token rewards land in the audit trail
as `crate:<tier>`.

## Cosmetics crate

Chat tags, pets and trails, nothing that changes a fight (owner, 2026-09-29). `/tags` picks a tag
(DeluxeTags; it shows after your name in chat, the tab list and above your head), `/pet` summons a
pet (SimplePets) and `/trails` picks a particle trail (ours, `avian-cosmetics`: PlayerParticles is
licensed non-commercial; trails are defined in `plugins/AvianFactions/trails.conf`). Each prize is a permission; one you already own pays **150 tokens** instead.

| | Weight each | Prizes |
|---|---|---|
| Epic tags | 2 | Sl♥y, 2026 (rainbow), Choom, Main Character |
| Rare tags | 5 | ☠ Menace, Cracked, $ Rich $, ⚔ Raider |
| Common tags | 10 | GG, Grinder, Touch Grass, No Cap, lagging… |
| Common pets | 6 | Cat, Fox, Rabbit, Frog |
| Rare pets | 3 | Parrot, Axolotl, Bee, Panda, Armadillo |
| Epic pets | 1 | Allay, Sniffer, Polar Bear |
| Common trails | 5 | Flames, Notes, Clouds, Sparkle |
| Rare trails | 2.5 | Hearts, Snowfall, Soul Fire, Arcane |
| Epic trails | 1 | Rainbow, Cherry Blossom, Ender Rift, Sparks |

Keys: **800 tokens** in the token shop (4,000 for five), and about a 5 % drop from the Vote crate.
Add a tag in `dev-server/plugins/DeluxeTags/config.yml` and its prize in `CrazyCrates/crates/Cosmetic.yml`.

## Rank kits

Each paid rank claims its kit once a week with `/kit <rank>` (`dev-server/plugins/Essentials/kits.yml`),
and every kit below its own. Every kit has armour, a sword, a pickaxe, a bow, arrows, logs, steak,
golden apples and 3 random custom enchant books (owner, 2026-09-29). The books come from `/kitbooks`,
which draws from the Enchanter's own pool for that tier, with the same odds.

| Kit | Armour | Weapons and tools | Extras | Books |
|---|---|---|---|---|
| Harpy | Iron, Prot III, Unb II | Sharp III iron sword, Eff III iron pick, Power II bow (all Unb II) | 32 arrows, 32 logs, 32 steak, 2 golden apples | 3 Common |
| Griffin | Diamond, Prot II, Unb II | Sharp III diamond sword, Eff III diamond pick, Power III bow (all Unb II) | 48 arrows, 48 logs, 32 steak, 3 golden apples | 3 Common |
| Wyvern | Diamond, Prot III, Unb II | Sharp IV sword, Eff IV Fortune II pick, Power III Punch I bow | 64 arrows, 64 logs, 48 steak, 4 golden apples | 3 Rare |
| Dragon | Diamond, Prot IV, Unb III | Sharp V sword, Eff V Fortune III pick, Power IV Punch I bow | 64 arrows, 64 logs, 64 steak, 5 golden apples, 1 netherite ingot | 3 Legendary |
| Phoenix | Netherite helmet and chestplate, diamond legs and boots, Prot IV, Unb III | Sharp V netherite sword, Eff V Fortune III netherite pick, Power V Punch I bow | 64 arrows, 64 logs, 64 steak, 6 golden apples | 3 Mythic |

**The free route (ADR-0006).** Every kit a rank sells is also a crate prize, so it can be won:

| Kit | Crate | Chance per opening | Tokens of keys, on average |
|---|---|---|---|
| Harpy | Common | about 6 % | about 3,400 |
| Griffin | Common | about 3.6 % | about 5,500 |
| Wyvern | Rare | about 5.5 % | about 11,000 |
| Dragon | Epic | about 6.8 % | about 22,000 |
| Phoenix | none | Phoenix is never sold; winning F-Top is the only way to get it |

Balanced 2026-09-29 so the top kit a free player can win, Dragon, costs about a month of steady
farming in keys (ADR-0006's own yardstick), and each lower kit proportionally less. That assumes
roughly 20,000 tokens a month from an upgraded Harvester Hoe; hand farming (20–40 an hour) is far
slower. Measure real token income in playtests and re-weight if it's off. They used to
sit a crate higher; Dragon in Mythic averaged about 215,000 tokens, which is no real route.

## Flight and rank prizes

`/f fly` (FactionsUUID: flight in your own land) is everyone's. **Essentials `/fly`**, flight anywhere,
belongs to Phoenix and is otherwise a super-rare crate prize (owner, 2026-09-29). Neither works in combat:
the combat tag grounds a flying player and blocks both commands until it ends. The ranks themselves are also prizes, incredibly rare, as their free route.

| Prize | Crate | Chance per opening | What it runs |
|---|---|---|---|
| Fly · 1 day | Epic | about 1 % | `lp user <p> permission settemp essentials.fly true 1d accumulate` |
| Fly · 2 days | Mythic | about 1 % | the same, `2d`; a second win adds time |
| Fly · Season | Mythic | about 0.2 % | `lp user <p> permission set essentials.fly true` |
| Harpy rank | Epic | about 0.2 % | `lp user <p> parent add harpy` |
| Griffin rank | Mythic | about 0.2 % | `… parent add griffin` |
| Wyvern rank | Mythic | about 0.1 % | `… parent add wyvern` |
| Dragon rank | Mythic | about 0.05 % | `… parent add dragon` |

**When a season ends**, clear season flight from every player (temporary flight runs out on its own;
Phoenix keeps /fly through its group, which this doesn't touch):

```
lp bulkupdate users delete "permission == essentials.fly"
```

What paid ranks still give beyond cosmetics is their weekly kit (which has its own crate route)
and more auction listings. Homes are 2 for every rank past Hatchling, `/craft` comes with
Fledgling and `/ec` is everyone's (owner, 2026-09-29).

## Changing prices

Every price on this page is for **one** item. EconomyShopGUI prices an entry for its `stack-size`
(a diamond entry with `stack-size: 9` is priced per 9), so the shop files hold price × stack-size;
the script does that multiplication. Getting this wrong made 16 diamonds sell for $178 instead of
$2,400 on 2026-09-24.

Prices can be changed live: copy the files into `run/plugins/EconomyShopGUI/` and run `sreload` on
the console (`./dev cmd "sreload"`). Keep `dev-server/` as the source of truth.

Edit the files under `dev-server/plugins/EconomyShopGUI/shops/`, then `./dev restart`. To rebuild
them from the plugin's defaults, for example after an EconomyShopGUI update adds items, run
`tools/reprice-shop.pl` over each default file. Its tables at the top are this page in code.
Spawners are hand-written in `Mobs/spawners.yml`.
