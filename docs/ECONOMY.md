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
| **Sugar cane** | **16** ($1,024 a stack) | | Rotten flesh | 12 | | Diamond | 100 |
| Cactus | 4 | | Bone | 10 | | Iron ingot | 40 |
| Pumpkin | 3 | | String | 10 | | Gold ingot | 5 |
| Wheat, carrot, potato, beetroot, nether wart, cocoa, chorus | 2 | | Spider eye | 8 | | Coal | 3 |
| Melon slice, berries, bamboo, kelp, mushrooms | 1 | | Gunpowder | 30 | | Quartz | 3 |
| | | | Ender pearl | 60 | | Lapis, amethyst | 2 |
| | | | Slime ball | 15 | | Redstone | 1 |
| | | | Magma cream | 25 | | Netherite scrap / debris | 500 |
| | | | **Blaze rod** | **120** | | Netherite ingot | 2,000 |
| | | | Raw meat 1, cooked 2, feather 2, leather 5 | | | Storage blocks | 9 × |

Emeralds and copper are not sellable: villager trading and copper scraping would otherwise be free
money.

**Sugar cane in numbers.** A cane block grows roughly every 18 minutes, so a column yields about
3.3 cane an hour. At $16 each ($1,024 a stack), an early 200-column farm makes about $10,000 an
hour and a 5,000-column auto-farm about $260,000: early on, cane out-earns any spawner a new
faction can afford, and it stays competitive at scale. Hand-harvesting also pays **tokens**: a 2 % chance of 1 token per grown
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
- **Never sold** (#40): potions of any kind (the section is off), golden apples, enchanted golden
  apples and dragon's breath. They are all obtainable in game, just not bought.

Buying gear is a convenience premium: mining diamonds and crafting a pickaxe still costs three
diamonds.

## PvP consumables

Cooldowns in `combat.conf`:

- Enchanted golden apple: 60 s.
- Golden apple: 10 s.
- Totem of undying: 60 s. A totem that pops during its cooldown does not save the player.

The cooldown shows on the item like a vanilla one and survives relogging and dying.

## Changing prices

Edit the files under `dev-server/plugins/EconomyShopGUI/shops/`, then `./dev restart`. To rebuild
them from the plugin's defaults, for example after an EconomyShopGUI update adds items, run
`tools/reprice-shop.pl` over each default file. Its tables at the top are this page in code.
Spawners are hand-written in `Mobs/spawners.yml`.
