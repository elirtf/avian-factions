# Log review and economy notes, 2026-09-24 (02:18–08:20 CDT)

Six hours of the dev server log, sampled every 10 minutes for TPS, tick time and players online,
read hourly, then compared with how modern factions and survival-RPG servers run their economies.
Everything below is a recommendation; nothing here has been changed on the server except the
items marked **done**.

## What the logs showed

- **Health: good.** TPS 20.0 for all six hours; tick time peaked at 34.9 ms right after a restart,
  otherwise under 3 ms. No plugin errors, warnings or stack traces outside restarts.
- **Load: almost none.** One player (the owner) from 02:18 to 02:57, then nobody. So these numbers
  prove the idle server is clean, not that it holds up under a real session. That needs the
  two-player playtest (#31).
- **Economy activity:** 25 shop transactions, all in that one session. They found the biggest bug
  of the night (below).

## Fixes, by priority

| # | What | Status |
|---|---|---|
| 1 | **Shop paid a fraction of intended prices.** EconomyShopGUI prices each entry per its `stack-size`, and the sheet wrote per-item prices, so 16 diamonds sold for $177.78 and 6 cane for $24. Every sellable item was affected. | **Done** (#50): prices scaled; 9 diamonds then sold for $1,350 |
| 2 | **Four `/f` shortcuts pointed at commands that don't exist** (`delhome`, `setwarp`, `delwarp`, and `sethome` unhandled): FactionsUUID 4.7 keeps homes and warps under `/f set`. | **Done** (#52), plus `/f ally`, `/f desc`, `/f tag`, `/f who` and the `/f` click menu |
| 3 | **Harvester Hoe harvests give no AuraSkills farming XP**, because the hoe cancels the real break. Neighbouring columns do give 1 block of XP each, via their protection check, so it's inconsistent. Either grant XP for every harvested block through the AuraSkills API, or none. Recommend granting it: farming feeds the RPG side. | To do |
| 4 | **Buying one item at a time.** Sand was bought 16 times in a row, one each. Set the EconomyShopGUI buy screen's quantity buttons (1 / 16 / 32 / 64) and a sensible default amount. | To do |
| 5 | **Starter cane costs too much to start a farm.** Cane sells for $16 and the "buy ≥ 4× sell" rule makes it $64 to buy, so 64 cane is $4,096 with a starting balance of $0. Cane is found naturally, but a new player shouldn't need luck. Drop cane's buy price to about $24 (still above sell, so no loop) or give a starting balance (see Economy 2). | To do |
| 6 | **`./dev check-log` can fail right after a restart** because Geyser logs "Started Geyser" a few seconds after "Done". Make check-log wait for its expected lines (up to about 30 s) before failing. | To do |
| 7 | DeluxeMenus warns `Could not setup a NMS hook` on 26.1.2. It only disables NBT item options, which the menu doesn't use. | Harmless; watch for an update |

## Economy: how modern servers do it, and where we stand

**What the successful factions servers converge on:**
- **Sugar cane and spawners are the two income engines.** Cane is the best early earner, and spawner drops become the backbone later ([McServ factions guide](https://mcserv.org/blog/how-to-start-and-progress-in-minecraft-factions), [Minecraft Forum](https://www.minecraftforum.net/forums/minecraft-java-edition/discussion/2101593-whats-the-easiest-way-to-make-money-on-factions), [JartexNetwork](https://jartexnetwork.com/threads/how-to-earn-huge-amount-of-money-in-factions.135593/)). **We match this.**
- **A second and third currency on top of money.** These are typically tokens and mob coins, spent in their own shops ([KosmosPvP](https://www.planetminecraft.com/server/kosmospvp-factions/), [CosmicReborn](https://minecraftservershq.com/server/cosmicreborn)). **We have tokens (cane, hoe); mob coins are #28.**
- **Upgradable harvester hoes and sell wands, and TNT wands for raid supply** ([KosmosPvP](https://www.planetminecraft.com/server/kosmospvp-factions/), [Gemscraft](https://www.minecraftforum.net/forums/servers-java-edition/pc-servers/3110647-gemscraft-factions-with-custom-economt-75-bc)). **We have the hoe; no sell wand yet.**
- **Raid items priced deliberately** (TNT, sand, dispensers, obsidian), because raids consume them ([Minecraft Forum](https://www.minecraftforum.net/forums/support/server-support-and/3020418-balancing-factions-economy)). Ours: TNT $107, sand $6, obsidian $32, dispenser $34.
- **An auction house** as the player-to-player market ([McServ](https://mcserv.org/blog/what-are-minecraft-factions-complete-guide-mechanics-strategies-2026)). **We have none.**
- **Money sinks sized to the taps.** Any infinitely renewable item sold to an admin shop sets a floor under the currency's value. A common guideline is that 0.5–0.7 of every dollar created should leave through sinks. The best sinks are ones players want to save for, like ranks bought with in-game money, plus listing fees, taxes and travel fees ([MineGuard](https://mineguard.pro/en/blog/minecraft-server-economy-setup-guide), [XGamingServer](https://xgamingserver.com/blog/how-to-add-money-to-any-minecraft-server/), [SpigotMC](https://www.spigotmc.org/threads/ways-to-fix-a-super-inflated-economy.199590/)).

**Where we stand.**
- **Taps:** cane, spawner drops, ores. All are renewable and all go into an admin shop, so money creation scales with farm size, with no ceiling.
- **Sinks:** shop purchases (gear by tier, raid items, building blocks), spawners, the $25k hoe, and FactionsUUID upgrade costs. There's no recurring sink that grows with wealth.

## Economy rebalancing, by priority

1. **Add a wealth-scaled sink before launch.** Otherwise a large cane farm ($320k an hour) inflates everything within a season. Options, best first:
   - **Money rank-up for the free ranks** (Hatchling → Fledgling, then prestige tiers). Players want it, and it's ADR-0006-safe because it's free-to-earn.
   - **Faction upgrades priced in money, growing per level** (Claim Boost and the upgrade list, #30).
   - **An auction house with a listing fee and sales tax (2–5%).**
   - A small `/pay` tax is the least popular option; skip it unless the others fall short.
2. **Starting balance of about $2,500.** That's enough for starter cane and wooden/stone tools, not a head start. The guides warn against large grants, which devalue everyone else's work.
3. **A way to sell what auto-farms collect.** Hoppers fill chests, and today the only sale is carrying it to `/sell`. A **sell wand** (right-click a chest to sell its contents at shop prices, with limited uses or upgradable) is the standard answer. It's Avian-specific enough to build next to the hoe, using the same player-aware price lookup.
4. **Mob coins (#28)** as the spawner side's second currency, mirroring tokens for cane. They're spent in a rotating shop, which is also a sink for spawner wealth.
5. **Tie AuraSkills to the economy, lightly.**
   - AuraSkills currently rewards stats only; farming levels grant strength and health. Grant **tokens at skill milestones** (every 10 levels, say): that rewards RPG progression without adding money.
   - Also check whether AuraSkills' farming double-drop ability multiplies hoe income. If it does, count it in the cane numbers.
6. **Keep ores worth mining but not farmable.** Diamonds are $150 and can't be farmed, which is good. Keep villager iron farms off (done) and watch piglin gold farms (gold is $8).
7. **Events as temporary multipliers, not permanent price hikes.** For example, sell-booster weekends through EconomyShopGUI's `enable-sell-multipliers`, and the Aerie outpost's sell buff (#21). Modern servers use boosts to create moments without inflating the base.

## Ideas (not economy)

- **Faction menu:** turn the "type `/f invite <player>`" hints into clickable chat suggestions (DeluxeMenus `[json]` with `suggest_command`), so players click instead of type.
- **Sidebar or tab:** show balance, tokens and faction power (PlaceholderAPI is ready: `%avian_balance%`, `%avian_tokens%`).
- **`/sellgui` or `/sell all`** permissions for everyone. `/sell hand` was used over 10 times in 40 minutes.
- **An economy dashboard** built on the audit log we already write (every balance change has a reason): daily money created by cane, spawners and ores against money destroyed by shop, spawners and upgrades. That's the number that tells us whether sinks keep up (target 0.5–0.7).
- **Two-player playtest (#31)** before any further balance changes: the only real data so far is one 40-minute session.
