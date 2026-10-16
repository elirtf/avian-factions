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

**Emeralds sell for $250 each** (owner, 2026-10-01): with nether stars from the Darkzone, the
biggest earner. They come from **villager spawners** (tier 6 below) and **mining emerald ore**, never
from trading: villagers no longer pay emeralds for anything (`economy.conf` `emerald-trades=false`),
so crops, paper and meat can't be turned into money through them. Emerald ore can't be bought, and
emeralds and emerald blocks cost 4× their sell price, so nothing buys back cheaper than it sells.
Copper is not sellable: scraping would otherwise be free money.

**Mining pays** (owner, 2026-10-16): overworld ores generate at **1.5× vanilla** in new chunks
(`dev-server/world/datapacks/avian-ores`), emerald ore included. So a trip to the resource world earns
money through the prices above and levels AuraSkills' Mining faster. Ancient debris is 3× (`avian-netherite`).

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
| 6 | **Villager** (one emerald a villager) | $1,000,000 | $250 |

Only these 15 are sold. A villager drops its emerald only when a spawner a player placed spawned it
(`economy.conf` `spawner-drops`), so bred or wild villagers are worth nothing. Villagers can no longer summon iron golems (`economy.conf`:
`villager-iron-golems=false`); a vanilla iron farm would otherwise make golem spawners pointless.

**Which mobs stack** (owner, 2026-10-01; `economy.conf` `stacking`): stacking turns many mobs into
one kill target, so it sets how fast a spawner farm earns. Only mobs from spawners a **player placed**
stack. Wild mobs and mobs from dungeon, fortress or stronghold spawners never do (RoseStacker's
`only-stack-from-spawners`, narrowed by our plugin, which marks each mob a placed spawner spawns).
**Villagers, iron golems and silverfish never stack** at all: they feed the emerald economy, so every
one is its own kill.

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

**Where they come from (#40):** none of the three is sold in `/shop`.

- **Golden apples** can't be crafted (`combat.conf` `disabled-recipes`). Eight gold is cheap: the shop
  sells it at about $32 an ingot and gold farms make it cheaper still, so crafting would make them
  common. They come from the rank kits (2–6 a week), the Rare crate (2, 3.7 % a roll), the Epic crate
  (3, 5.5 %) and chests in the world.
- **Enchanted golden apples**: the Mythic crate (2, 4.9 % a roll, about one pair per 20 Mythic keys)
  and the rarest chests in the world. They were never craftable.
- **Dragon's breath**, and so lingering potions: only from the ender dragon in the End, which is open.
  Rare, and obtainable, as ADR-0006 asks.

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
| Common | $5k–10k, 50 tokens, iron tools, 8 diamonds | Vote crate (5 %); fishing (contest 2nd, AuraSkills epic catch); token shop |
| Rare | $25k, 150 tokens, diamond gear, zombie spawner, 2 golden apples | Common crate; fishing contest 1st; token shop |
| Epic | $75k, 400 tokens, Prot III diamond, Sharp V netherite sword, netherite ingot, skeleton/creeper/blaze/enderman spawners, totem | Rare crate; Mythic crate (2 keys); Sunday fishing contest 1st; token shop |
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

## Starter kit

Every player can claim `/kit starter` once a day: stone tools, leather armour, a Lure I fishing rod, 16
bread, 16 logs and 16 torches (owner, 2026-09-30). Free, so it needs no crate route.

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

## Fishing

Custom fish from EvenMoreFish (owner, 2026-09-30; the reasoning is in
`docs/research/custom-fishing.md`), on top of AuraSkills' own fishing XP and treasure.

- **Money:** custom fish sell only through `/fish shop`, at **3× their base worth** (EvenMoreFish
  `economy.vault.multiplier: 3.0`; owner, 2026-10-16: "it takes a while to even make 1k").
  - Per catch, about $225 on average: Common ~$85, Rare ~$510, Epic ~$1,900, Legendary ~$7,500.
  - Per hour, roughly $45k with a plain rod, $54k with the starter kit's Lure I and $85k with Lure III.
  - That makes fishing the best *active* earner; cane farms earn less but run while you're away. It can't
    be automated (anti-AFK).
  - Each fish scales with its size, so a Common runs about $6–160. It was 1× ($15–28k an hour) before. Raw fish stay unsellable in `/shop`
  (`sell: -1`), so sell wands can't sell them either. Keep it that way.
- **Baits are crafted** (owner, 2026-10-16: "craftable, not crazy difficult, but a challenge; different
  effects"). Each one is in every player's recipe book. One bait covers one catch; drag it onto the rod.
  A craft makes several (`economy.conf` `bait-craft-yield`). They can't be fished up, and EvenMoreFish's
  own example baits are off. Files: `plugins/EvenMoreFish/baits/`.

  | Bait | Effect | Recipe | Per craft |
  |---|---|---|---|
  | Wriggler | ¼ as much junk, Rare ×1.5 | 2 rotten flesh, seeds, bone meal | 8 |
  | Kelp Chum | +1 ❤ and a bite to eat each catch, Rare ×1.25 | 3 dried kelp, sweet berries, bone meal | 8 |
  | Glow Lure | Rare ×2, Epic ×1.5, Night Vision 60 s | glow berries, string, glow ink sac | 6 |
  | Angler's Charm | +10 XP and +15 Fishing XP each catch, Luck 60 s | amethyst, lapis, honey bottle | 4 |
  | Abyss Chum | Epic ×2.5, Legendary ×1.5, half the junk | prismarine shards and crystals, sea pickle | 4 |
  | Gilded Lure | Legendary ×3, Epic ×1.5, half the Commons | nautilus shells, gold ingots, ender pearl | 2 |

  The rare-fish baits raise income a little, since a Legendary is worth ~$7.5k at 3×. That's the point of
  crafting them, and the ingredients (monuments, nautilus shells) are the brake.
- **Tokens and keys come only from contests**, never from ordinary catches, and a contest only runs
  with **3+ players online**, so nobody can farm them alone:

  | Contest | When | 1st | 2nd | 3rd | Everyone |
  |---|---|---|---|---|---|
  | Biggest fish, 10 min | 18:00 and 21:00, not Sunday | 150 tokens + Rare key | 75 + Common key | 40 | $2,500 |
  | Most fish, 30 min | Sunday 19:00 | 300 tokens + Epic key | 150 + Rare key | 75 + Common key | $5,000 |

- **Fishing XP by rarity:** every custom catch gives AuraSkills Fishing XP for its rarity (the
  `catch-event` in `EvenMoreFish/rarities/`): Junk 30, Common 60, Rare 250, Epic 600, Legendary 1,500,
  about the vanilla average per cast. AuraSkills' own flat XP for cod, salmon, pufferfish and tropical
  fish is zeroed (`AuraSkills/sources/fishing.yml`) so nothing counts twice, and players can't turn
  custom fishing off (no `/fish toggle`), so every catch earns by rarity (owner, 2026-09-30).
- **Multi-item catches:** a catch of two or more fish (Double Catch, AuraSkills' Fisher) pays the XP
  for every one, so two Common fish give 120 (avian-economy FishingEnchantXp). The extra shows in the
  action bar as its own gain.
- **One rare catch:** AuraSkills' epic fishing loot includes a Common key, about 1 in 2,200
  catches, more as a player's Fishing level (Epic Catch) rises.
- **Anti-AFK:** EvenMoreFish's `exploits.afk-fishing` (10 catches in the same 3-block spot stops
  catches) and AuraSkills' `fishing_a` check are on, since the Mythic enchant Auto Reel plus a held
  click would otherwise fish unattended. `disable-auraskills-loot` is off so Treasure Hunter and
  Epic Catch still work.

Players use `/fish` (`/fish shop` sells, `/fish top` ranks; `/emf` also works). Staff start a contest early with
`/emfa competition start mainCompetition <minutes>` (needs `emf.admin`). Contests are in
`plugins/EvenMoreFish/competitions/`; the weekend one is switched off.

## Quests

`/quests` (LMBishop Quests, built from a pinned commit; `docs/research/quests.md`). Fishing first
(owner, 2026-09-30); every quest starts itself and the repeating ones come back on a cooldown that
counts from when you finish (20 hours for dailies, 6 days for weeklies).

| Quest | Goal | Repeats | Reward |
|---|---|---|---|
| Daily Catch | 20 custom fish | Daily | $2,500 + 10 tokens |
| Rare Bites | 3 Rare-or-better fish | Daily | 20 tokens |
| Big Haul | 250 fish | Weekly | $15,000 + 50 tokens |
| Legend of the Deep | 1 Legendary | Weekly | 50 tokens + Common key |
| On the Podium | Top 3 in a fishing contest | Weekly | 50 tokens |
| Angler I / II / III | Fishing level 10 / 25 / 50 | Once each | $10k + Common key / Rare key / Epic key |

At most about 360 tokens and a Common key a week, nothing per catch. The Angler milestones are a
written-down free route to keys (ADR-0006). Contest places 1–3 grant a 3-day
`avian.quests.fishing.podium` permission that On the Podium checks for and clears. Quest files are
in `plugins/Quests/quests/`.

## Elites and bosses (EliteMobs)

Owner, 2026-10-02: EliteMobs only for its wild **elites** (tougher mobs with powers) and **bosses**,
paying in *our* economy. Its own currency (Elite Coins), gear and enchantments, shops, quests, guild,
gambling, parties, classes and skill system are all off (`plugins/EliteMobs/`), so nothing competes with
the enchanter, the 6-enchant cap or the gear ladder. Its skill system would also have given players
extra hearts, which PvP can't have. Players get none of its commands except boss tracking.

- **Where:** the resource world, the Nether and the End (`ValidWorlds.yml`), never the claiming world, so
  bases aren't harassed. Today's single `world` is the claiming world, so it is off until the migration
  adds the resource world. Spawner mobs never become elites (`doSpawnersSpawnEliteMobs: false`).
- **Rewards** (`economy.conf` `elite-rewards`), to the player who dealt the most damage:
  - an elite: **$25 × its level**. Elite levels follow nearby players' gear (about 1–20 in vanilla gear),
    so about $25–500 an elite;
  - a boss: **25 tokens and a Rare key** up to level 19, **75 tokens and an Epic key** from level 20.
- **Difficulty follows the player** (owner, 2026-10-16: mobs "only need to get stronger according to how
  strong the player is", nothing crazy hard). Ordinary mobs stay vanilla; about 5% become elites, whose
  level follows nearby players' gear, and EliteMobs' scaled combat fights them at the player's own level.
  We make them a little gentler than EliteMobs' default: players deal **1.25×** damage to them and take
  **0.8×** (`MobCombatSettings.yml`, `scaledDamageTo…Multiplier`). Custom bosses may come later.
- **Bosses** come from EliteMobs content packs (its world bosses and events), installed with `/em setup`;
  the plugin alone ships only test bosses. Pick packs with the owner: gear from bosses is fine as long as
  it's balanced against the enchant cap and the gear ladder (owner, 2026-10-02).

## Base building: gen blocks

Owner, 2026-10-16: the factions "gen bucket", in `/shop` → **Base Building** at **$10,000** each, for sand,
cobblestone and obsidian, horizontal and vertical.

- **Placing one** lays its block in a line, one a tick: horizontal ones run the way you face (up to 128),
  vertical ones straight down to the ground. The item is **never used up**.
- **Fills air only.** It passes over blocks of its own kind already there (so lines join up) and stops at
  anything else, the world border or full length.
- **Each block costs its /shop price:** cobblestone $1, sand $6, obsidian $32. It's charged as it goes, and the
  gen stops the moment you can't afford the next block. A 128-block obsidian wall is $4,096.
- **Same rules as building by hand:** every block goes through a normal place event as the player, so it can't
  run into another faction's land or protected regions (spawn), and CoreProtect logs it.
- At most 8 gens running per player. A summary when each one stops says how many blocks it laid, the cost
  and why it stopped.
- **Infinite buckets**, in the same section: **water $25,000, lava $50,000** (prices chosen 2026-10-16; the
  owner can change them). Pour as often as you like and the bucket stays full. Placing still goes through
  faction land and WorldGuard; only the emptying is undone. They don't stack, and dispensers won't take them
  (that would turn one into a plain bucket).
- Settings are in `plugins/AvianFactions/gens.conf`. The shop items are `EconomyShopGUI/shops/basebuilding.yml`
  (ids `gen_<type>`), and AvianFactions swaps the bought block for the real gen. Admins:
  `/genblock give <player> <type> [amount]`, with `infinite_water` / `infinite_lava` for buckets.

## The Aerie (capturable outpost)

Owner, 2026-10-16 (#21): an outpost in the warzone that any faction can capture at any hour, so map control
pays every hour and there's always something to fight over. KOTH rewards a moment; the Aerie rewards holding.

- **Where:** a rock crag south of spawn (0, 142, 190), 21 blocks high, with a nest of logs on top around a
  blackstone-and-gold pad. A beacon beam marks it from anywhere, and a stair spirals up the outside. The build
  is a datapack function (`dev-server/world/datapacks/avian-aerie`, generated by `tools/aerie/build.js`), so
  it can be rebuilt anywhere.
- **Capture:** stand on the pad with no enemy for **5 minutes**.
  - Anyone else on it (another faction, or a player in no faction) pauses the capture.
  - Leaving drops it.
  - Factionless players can contest but can't capture.
  - The holder keeps it until someone else captures it.
- **Announced server-wide:** when a faction starts capturing (at most once a minute per faction), when it's
  contested, at halfway, with one minute left, and the capture itself. Holders are warned when someone is
  taking it. Players on the pad see a boss bar with the countdown, and a floating sign shows the holder and
  how long they've held it.
- **What holding pays** (`plugins/AvianFactions/aerie.conf`):
  - **+30% on everything sold**: `/sell`, the sell wand and the Harvester Hoe. Online members get
    EconomyShopGUI's `sell-multipliers.aerie`; it multiplies with the hoe's own multiplier.
  - Every hour held: a **Rare key and 25 tokens** for each online member.
  - Every 2 hours held: a **Common key** for each online member.
  - **8 hours held without losing it:** every member of the faction, online or not, earns the
    **⛰ AERIE** chat tag (DeluxeTags, category "Earned"). It's never sold and never in a crate.
- **Payout scale:** 5 members online holding it all day is 120 Rare keys, 60 Common keys and 3,000 tokens a
  day. That's deliberately strong: it's the main reason to fight daily, and it only goes to whoever wins and
  keeps it. Tune it in `aerie.conf`.
- **Survives restarts:** the holder, since when, and the 8-hour progress are kept in MariaDB. Payouts missed
  while the server was down are skipped, not paid in a heap.
- **Season reset:** `/aerie reset` frees it. `/aerie give <faction>` hands it over (admins, for events and tests).
- `/aerie` shows anyone who holds it, where it is and what it pays.

## KOTH and envoys

Owner, 2026-10-03 (issue #24): KOTH every 3 hours, envoys every hour, so a player logging in any time
is close to a fight.

**KOTH** (VelKoth, built from source with our patch: `tools/velkoth/README.md`):

- Every 3 hours, Central time (00:00, 03:00 … 21:00), on the arena called `koth`
  (`plugins/VelKoth/config.yml`). Hold the hill for 2 minutes uncontested; faction members don't
  contest each other.
- **Prize, to the capturing player only:** an **Epic key, $50,000 and 100 tokens**. At up to 8 a day
  that's at most 8 Epic keys (each about 1,500 tokens) and $400k a day.
- **Setting up the arena** (once the warzone hill is built; arenas are drawn in game, so they live in
  the server's data, not git): stand at the hill, `/koth wand`, select the two corners,
  `/koth create koth`, then in `plugins/VelKoth/arenas.yml` set its rewards and `/koth reload`:

  ```yaml
  rewards:
    - 'COMMAND:crates give virtual Epic 1 %player% -s'
    - 'ECONOMY:50000'
    - 'COMMAND:tokens give %player% 100 koth'
  ```

  Rewards can also be set in game: `/koth reward add koth <reward>`. Until the arena exists the schedule
  finds no `koth` arena and does nothing. Change the schedule in `config.yml` (tracked), not with
  `/koth schedule add`, which a restart would undo.

**Envoys** (CrazyEnvoys): about 8–12 crates every hour, each paying one prize by tier
(`plugins/CrazyEnvoys/tiers/`):

| Tier | Chance | Prizes |
|---|---|---|
| Common (`Basic.yml`) | 75 % | $2,500 / $5,000 / 15 tokens / Common key / 8 TNT / 16 XP bottles |
| Rare (`Lucky.yml`) | 22 % | $10,000 / 40 tokens / Rare key / 2 Common keys / 32 TNT |
| Epic (`Titan.yml`) | 3 % | Epic key / $50,000 / 150 tokens / 2 Rare keys |

No golden apples (#40). An envoy hour pays out roughly $40–60k across the server.

- **Drop spots are fixed, placed in the warzone with `/envoy edit`** (right-click blocks to add spots;
  aim for 20–30). Random spots are off: CrazyEnvoys finds them by loading chunks on the main thread,
  which froze a test server for 7–25 seconds. Fixed spots measured no lag.
- Envoys skip empty servers.
- **Easy to spot** (owner, 2026-10-16: they were hard to find): each crate has a floating name in its
  tier colour ("✦ Epic Envoy ✦", "▶ Click to claim"), shown by DecentHolograms, plus a beacon falling
  from the sky when it drops and a firework flare every 15 seconds while it waits. The hologram lines use
  `#RRGGBB` and `&` codes, not MiniMessage: CrazyEnvoys converts only those for holograms.

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
