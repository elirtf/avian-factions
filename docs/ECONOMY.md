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

The signature farming tool (spec §31, `hoe.conf`). Buy it for **$25,000** from `/hoe`, then hold it
and run `/hoe` to upgrade it with tokens. Breaking sugar cane with it takes every block above the
root, so nothing needs replanting. The cane sells on the spot at `/shop` prices, and each grown
block rolls for tokens just like hand-harvesting.

| Upgrade | Per level | Levels | Token costs |
|---|---|---|---|
| Harvest radius | one more ring of columns (3x3, then 5x5) | 2 | 250, 1,000 |
| Token boost | +25 % token chance | 5 | 100, 250, 500, 1,000, 2,000 |
| Money multiplier | +5 % sell price | 5 | 150, 400, 800, 1,500, 3,000 |
| Random drops | turns drops on; each level multiplies their chance | 3 | 200, 600, 1,500 |

A fully upgraded hoe (15,750 tokens in all) harvests 25 columns a swing at 1.25× price, with
double the token chance. Random drops are a config list of chance → tokens and/or a console
command, so crate keys plug in with no code once crates exist: for example
`{ chance=0.0005, command="crazycrates give virtual feather 1 {player}", message="a Feather Crate key" }`.
The one drop today is 25 bonus tokens at 0.2 % per grown block.

The hoe is identified by its item data, not its name, and its levels are stored on the item, so it
keeps them when traded or stored. Neighbouring columns are only harvested where the player could
break them (claims, WorldGuard). Admins can give one with `/hoe give <player>`
(`avian.hoe.admin`).

## PvP consumables

Cooldowns in `combat.conf`:

- Enchanted golden apple: 60 s.
- Golden apple: 10 s.
- Totem of undying: 60 s. A totem that pops during its cooldown does not save the player.

The cooldown shows on the item like a vanilla one and survives relogging and dying.

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
