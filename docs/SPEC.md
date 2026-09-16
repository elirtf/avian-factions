# Legacy Factions — Claude Build Specification

## 0. Purpose

Build a Minecraft Java Edition competitive Factions server inspired by the gameplay philosophy of classic VanityMC-era Factions.

IMPORTANT:
- Recreate gameplay mechanics and design patterns, not proprietary VanityMC source code.
- Do not copy VanityMC branding, logos, resource packs, builds, paid assets, configuration files, or proprietary code.
- Use an original server identity and original implementation.
- Treat this document as the authoritative product specification.
- Build incrementally. Do not attempt the entire project in one pass.

PROJECT CODE NAME: LegacyFactions

TARGET:
- Minecraft Java Edition
- Paper or Purpur
- Modern Java version appropriate for the selected Minecraft version
- Production database: MariaDB/MySQL
- Development database: SQLite may be supported
- Server architecture should support large concurrent populations and seasonal resets.

---

# 1. Product Vision

The server must feel like a classic competitive OP/Hybrid Factions server.

Primary gameplay loop:

JOIN
-> STARTER KIT
-> CREATE/JOIN FACTION
-> GATHER RESOURCES
-> BUILD BASE
-> FARM/SPAWNERS
-> UPGRADE GEAR
-> CUSTOM ENCHANTS
-> PVP
-> RAID
-> KOTH/EVENTS
-> ACCUMULATE WEALTH
-> COMPETE FOR F-TOP
-> SEASON PAYOUT
-> MAP RESET
-> NEW SEASON

Core priorities:
1. PvP
2. Faction warfare
3. Raiding
4. Economy
5. Progression
6. Custom enchantments
7. Faction competition
8. Seasonal resets
9. Events
10. Leaderboards

Avoid turning the server into:
- Vanilla SMP
- Towny
- MMORPG
- Quest-heavy RPG
- Generic survival server
- Large minigame network

The faction is the central unit of competitive progression.

---

# 2. Development Rules

Claude MUST follow these rules:

1. Build one subsystem at a time.
2. Compile after every major subsystem.
3. Write automated tests for important calculations.
4. Never perform blocking database operations on the Minecraft main thread.
5. Never trust client-provided data.
6. Never identify custom items only by display name.
7. Use UUIDs internally instead of usernames.
8. Use PersistentDataContainer for custom item identifiers.
9. Make gameplay values configurable.
10. Keep module APIs clean.
11. Document public APIs.
12. Log destructive administrative actions.
13. Make season resets transactional and recoverable.
14. Archive completed seasons instead of deleting their historical data.
15. Verify dependencies rather than assuming they exist.
16. Optimize for large player populations.
17. Test TNT, claims, combat, economy, and chunk loading under load.
18. Never copy proprietary VanityMC code/assets/configuration.
19. Use original branding, builds, textures, and implementation.
20. Prefer maintainability over cleverness.
21. Do not hardcode prices, rewards, cooldowns, enchantment chances, knockback, power, claim limits, kit contents, or event schedules.
22. All major systems must expose configuration and administrative controls.
23. Never make a large synchronous world scan during gameplay.
24. Use caching and scheduled/asynchronous processing for expensive calculations.
25. Every milestone must leave the server in a runnable state.

---

# 3. Architecture

Recommended custom modules:

legacy-core
legacy-factions
legacy-economy
legacy-enchants
legacy-items
legacy-kits
legacy-crates
legacy-events
legacy-raiding
legacy-spawners
legacy-season
legacy-scoreboard
legacy-leaderboards
legacy-admin

The modules may initially be implemented in one Gradle multi-module project if that is simpler, but keep package boundaries clear.

Suggested structure:

src/
  main/
    java/
      com.example.legacy/
        core/
        factions/
        economy/
        enchants/
        items/
        kits/
        crates/
        events/
        raiding/
        spawners/
        season/
        scoreboard/
        leaderboards/
        admin/
    resources/
      plugin.yml
      config/
        factions.yml
        economy.yml
        enchants.yml
        kits.yml
        crates.yml
        events.yml
        spawners.yml
        combat.yml
        messages.yml
        items.yml
        season.yml

tests/
docs/

---

# 4. External Dependencies

Recommended:
- Paper or Purpur
- LuckPerms
- PlaceholderAPI
- WorldEdit
- WorldGuard
- CoreProtect
- ProtocolLib if required by selected mechanics
- Chunky for world pre-generation
- Vault-compatible economy integration where useful
- Spark for performance diagnostics

Do not create hard dependencies unless necessary.

---

# 5. LegacyCore

Responsibilities:
- Plugin bootstrap
- Configuration loading
- Player profiles
- Database connection/pooling
- Currency APIs
- Token APIs
- Cooldowns
- Timers
- Menus
- Messages
- Sounds
- Particles
- Statistics
- Placeholder registration
- Season state
- Cross-module event bus
- Common utility APIs

Player API concept:

LegacyPlayer:
- UUID
- name/cache
- balance
- tokens
- kills
- deaths
- playtime
- faction UUID
- permissions

Example conceptual methods:

getPlayer(UUID)
getBalance()
addMoney()
removeMoney()
getTokens()
addTokens()
removeTokens()
hasPermission()
getFaction()
getStatistics()

Do not expose mutable internal database objects directly.

---

# 6. Database

Use MySQL/MariaDB in production.

Required tables/concepts:

players
factions
faction_members
faction_claims
faction_relations
faction_stats
faction_upgrades
economy_transactions
player_kills
player_deaths
kits
kit_claims
enchants
player_enchants
crates
crate_keys
events
event_participants
leaderboards
season_stats
punishments
audit_logs

Recommended fields must include:
- UUIDs
- created timestamps
- updated timestamps where appropriate
- season identifiers for seasonal data
- immutable transaction/audit records where appropriate

Economy transactions must record:
- transaction UUID
- player UUID
- timestamp
- action
- amount
- balance before
- balance after
- source
- optional reference ID

---

# 7. Factions

Commands:

/f create
/f invite
/f join
/f leave
/f kick
/f promote
/f demote
/f disband
/f home
/f sethome
/f claim
/f unclaim
/f map
/f power
/f top
/f who
/f show
/f chat
/f ally
/f truce
/f enemy
/f upgrade

Ranks:
- Leader
- Co-Leader
- Officer
- Member
- Recruit

Faction state:
- UUID
- name
- leader
- members
- ranks
- claims
- power
- relations
- home
- bank
- upgrades
- statistics
- season
- F-Top value

Faction names must be validated for:
- length
- characters
- reserved names
- profanity
- duplicate names

---

# 8. Faction Power

Each player has current and maximum power.

Initial example:
- Starting power: 10
- Maximum power: 10
- Regeneration: +0.5 every 30 minutes
- Death penalty: -2

Faction power:

SUM(member current power)

Claim capacity:

floor(faction power / power-per-claim)

All values configurable.

Example:
Faction power = 80
Power per claim = 5
Maximum claims = 16

Player deaths may reduce faction power.

Power regeneration should be scheduled efficiently and not run unnecessary work every tick.

---

# 9. Claims

Claims are chunk-based.

Store:
- faction UUID
- world UUID/name
- chunk X
- chunk Z
- season

Default protections:
- Enemy block break denied
- Enemy block place denied
- Enemy chest access denied
- Enemy door access denied
- Enemy hopper interaction denied
- Enemy dispenser interaction denied
- Enemy container interaction denied

Exceptions are controlled by raid state.

Implement:
- /f claim
- /f unclaim
- /f map

Faction map should distinguish:
- Own faction
- Ally
- Truce
- Enemy
- Wilderness

Support particle-based chunk border visualization.

---

# 10. Faction Upgrades

Command:
/f upgrade

GUI categories:

Power:
- Power Boost I
- Power Boost II
- Power Boost III

Claims:
- Claim Boost I
- Claim Boost II
- Claim Boost III

Spawners:
- Spawner I
- Spawner II
- Spawner III

Farming:
- Farming I
- Farming II
- Farming III

XP:
- XP I
- XP II
- XP III

Vault:
- Vault I
- Vault II
- Vault III

Upgrade costs and effects must be configurable.

---

# 11. F-Top

Command:
/f top

F-Top must be central to the season.

Display:
Rank
Faction
Value
Optional value breakdown

Faction value may include:
- Spawners
- Blocks
- Items
- Faction upgrades
- Special items
- Configured valuables

Do NOT use only faction bank balance.

Value table must be configurable.

Example:

items:
  diamond_block: 500
  emerald_block: 750
  gold_block: 250

spawners:
  zombie: 250000
  skeleton: 300000
  creeper: 500000
  iron_golem: 1000000

F-Top calculation:
- Must be cached.
- Must not scan thousands of chunks synchronously.
- Recalculate on a schedule, e.g. every 5 minutes.
- Support manual recalculation.
- Store last calculation timestamp.
- Store breakdown for debugging.

---

# 12. Economy

Primary currency:
$

Secondary currency:
Tokens

Optional cosmetic/premium currency:
Gems

Money is used for:
- Shop
- Spawners
- Faction upgrades
- Utilities
- Other configured purchases

Tokens are used for:
- Custom enchantments
- Special items
- Crates
- Keys
- Progression

Premium currency should primarily provide cosmetics/perks and should not be required for competitive progression unless explicitly configured.

---

# 13. Shop

Command:
/shop

Categories:
- Blocks
- Farming
- Mob Drops
- Spawners
- Combat
- Redstone
- Miscellaneous

All prices configurable.

GUI should support:
- Buy
- Sell
- Buy all
- Sell all
- Quantity selection

Transactions must be atomic.

---

# 14. Auction House

Command:
/ah
/ah sell <price>

Allow:
- Armor
- Weapons
- Tools
- Custom enchanted items
- Spawners
- Rare drops
- Keys
- Special items

Prevent:
- item duplication
- currency duplication
- invalid item serialization
- auction manipulation

Auction listings must have:
- listing UUID
- seller UUID
- item serialized safely
- price
- creation time
- expiration
- status

---

# 15. Custom Enchantment System

Command:
/enchants

Categories:
- Armor
- Weapons
- Tools
- Boots
- Bow
- Universal

Enchants should be data-driven.

Each enchant configuration:
- id
- name
- description
- max level
- cost per level
- activation chance
- cooldown
- compatible items
- conflicts
- effects

Example:

lifesteal:
  name: "&cLifesteal"
  max-level: 5
  levels:
    1:
      chance: 2.0
      heal: 2
    2:
      chance: 4.0
      heal: 2
    3:
      chance: 6.0
      heal: 3
    4:
      chance: 8.0
      heal: 3
    5:
      chance: 10.0
      heal: 4

---

# 16. Custom Enchant List

Armor:
- Tank
- Strength
- Overload
- Lifesteal

Weapons:
- Rage
- Lifesteal
- Execute
- Venom
- Lightning
- Wither

Bows:
- Explosive
- Piercing
- Lightning
- Blind
- Pull

Pickaxes:
- Haste
- Key Finder
- Token Finder
- Jackhammer
- Explosion

These are starting concepts. The implementation should make it easy to add more.

Enchantment compatibility must use a configurable conflict system rather than hardcoded if/else chains.

---

# 17. Custom Item Registry

All custom items must have a unique internal ID.

Examples:
- God Sword
- God Axe
- God Pickaxe
- God Bow
- Harvester Hoe
- Mob Sword
- Cannon Pickaxe
- Jackhammer Pickaxe

Each item:
- internal ID
- material
- display name
- lore
- custom model data where applicable
- PersistentDataContainer ID
- enchantments
- ability
- rarity

Never identify items solely from display names.

---

# 18. Kits

Command:
/kits

Recommended tiers:
- Starter
- Member
- VIP
- VIP+
- Elite
- Legend
- God

Recurring kits:
- Daily
- Weekly
- Monthly

Starter kit should allow immediate participation in:
- Factions
- Mining
- PvP
- Base construction

Kit claim data:
- player UUID
- kit ID
- last claim
- season if applicable

Example cooldowns:
- Starter: once
- Daily: 24 hours
- Weekly: 7 days
- Monthly: 30 days

All configurable.

---

# 19. Crates

Crate types:
- Vote
- Common
- Rare
- Legendary
- God
- Seasonal
- Event

Keys:
- Vote Key
- Rare Key
- Legendary Key
- God Key

Crate system must support:
- weighted rewards
- animations
- sounds
- particles
- configurable reward tables
- key consumption
- protection against duplicate rewards

Do not freeze the server during crate opening.

---

# 20. Warzone

Warzone is the primary public PvP area.

Conceptual layout:

NORTH
KOTH

WEST     SPAWN     EAST

CRATES / EVENT AREAS

SOUTH

Warzone:
- PvP enabled
- No normal claims
- Loot
- Crates
- KOTH
- Event arenas
- NPCs
- Shops
- Vertical terrain

Avoid flat terrain.

Build with:
- ruins
- castle walls
- bridges
- tunnels
- cliffs
- water
- lava
- cover
- open PvP zones

---

# 21. Spawn

Create an original premium fantasy/medieval/arcane spawn.

Suggested size:
~200 x 200 blocks

Required:
- central plaza
- castle or central structure
- NPC district
- crate room
- Warzone entrance
- leaderboards
- tutorial
- rules
- faction information

NPCs:
- Factions
- Shop
- Crates
- Kits
- Enchants
- Auction House
- Events
- Ranks
- Rules

Layout concept:

[CRATES]

[ENCHANTS] [KITS]

[SHOP] SPAWN [AUCTION]

[FACTIONS] [EVENTS]

[WARZONE]

Do not build a maze.

---

# 22. Wilderness

Initial world border:
10,000–20,000 blocks diameter depending on expected population.

Start smaller during development.

Pre-generate worlds using Chunky.

Wilderness should contain:
- ores
- villages
- caves
- structures
- custom loot
- farming resources

Resource scarcity should encourage competition.

---

# 23. KOTH

Implement multiple KOTH locations:
- Castle
- Desert
- Jungle
- Volcano

KOTH flow:

Player enters
-> Capture starts
-> Enemy enters
-> Capture is contested
-> Enemy eliminated/leaves
-> Capture resumes
-> Timer completes
-> Winner receives rewards

Rewards:
- Money
- Tokens
- Keys
- Spawners
- Custom items
- Enchant books
- Event currency

---

# 24. SOTW

Start of the World is a major event.

Event states:
- PRE_SOTW
- COUNTDOWN
- SOTW
- POST_SOTW

Command:
/sotw

Countdown:
10
9
8
7
6
5
4
3
2
1
GO

At SOTW:
- Claims enabled
- PvP enabled
- Economy enabled
- Crates enabled
- KOTH enabled

Optional grace period:
- First 30 minutes: no TNT
- First 60 minutes: no enemy claims
- First 2 hours: reduced PvP
- After grace: full warfare

All settings configurable.

---

# 25. Raiding

Raiding is a primary system.

Support:
- TNT
- TNT cannons
- Obsidian
- Walls
- Buffers
- Sand stacking if implemented
- Raidability
- Faction power interaction

Faction is raid-able when:

Faction Power < Claimed Land

Example:
Claims = 30
Power = 20
Raidable = YES

Raid state must be visible to players.

---

# 26. TNT Cannon System

Support:
- TNT projectiles
- TNT cannons
- TNT explosions
- cannon limits
- obsidian resistance
- water protection if configured
- rate limits

Do not permit uncontrolled TNT duplication.

The system must account for:
- concurrent cannons
- explosions
- chunk loading
- block calculations

Explosion handling must be optimized.

Use:
- maximum blocks affected
- explosion queues where appropriate
- chunk-loading limits
- TNT rate limits
- cannon validation

Admin testing command:
/cannon test

---

# 27. Raid Shield

Optional configurable raid shield.

Example:
Weekdays: 20:00–23:00
Weekend: 18:00–00:00

Outside raid window:
- enemy TNT damage disabled

Inside:
- full raid mechanics

Must support per-season configuration.

---

# 28. Spawners

Spawners:
- Zombie
- Skeleton
- Creeper
- Spider
- Blaze
- Enderman
- Iron Golem

Higher-value spawners require greater progression.

Spawner stacking:
1 Zombie Spawner
2
4
8
etc.

Display stacked quantity.

Use stacking/entity optimization to prevent mob farms from destroying TPS.

---

# 29. Mob Drops

Example:

Zombie:
- Rotten Flesh
- Money
- Tokens
- Rare key chance

Creeper:
- Gunpowder
- Money
- TNT
- Key chance

Make all drops configurable.

---

# 30. Farming

Main crops:
- Sugar Cane
- Cactus
- Wheat
- Carrot
- Potato
- Melon
- Pumpkin

Support:
- sell multipliers
- Harvester Hoes
- crop upgrades
- token rewards

Farming must be economically viable without becoming the only profitable strategy.

---

# 31. Harvester Hoe

Create a signature progression item.

Example:

LEGENDARY HARVESTER HOE

Sugar Cane Level: 1
Token Boost: 1
Sell Multiplier: 1x
Random Drop Level: 1

Upgrade command:
/hoe

Upgrade categories:
- Crop level
- Token boost
- Money multiplier
- Random drops

All costs and effects configurable.

---

# 32. Events Framework

Create EventManager abstraction.

Event types:
- KOTH
- SOTW
- Envoy
- Airdrop
- Boss
- SupplyDrop
- LMS
- Tournament
- RaidEvent

Event lifecycle:

start()
stop()
isActive()
getParticipants()
getRewards()

Every event should:
- announce itself
- track participants
- prevent invalid participation
- distribute rewards safely
- clean itself up after ending

---

# 33. Envoys

Random supply drops around Warzone.

Announcement:
"Envoys have spawned in the Warzone!"

Rewards:
- Money
- Keys
- Tokens
- Armor
- Enchant books
- Rare items

---

# 34. Airdrops

Announcement includes coordinates.

Example:

"A SUPPLY DROP HAS BEEN DEPLOYED!
X: 1234
Z: -842"

Players compete for the drop.

---

# 35. Boss Events

Example original boss:
THE CORRUPTED KING

Example:
- 5,000 HP
- Knockback resistance
- AOE attack
- Lightning
- Minions

Rewards:
- Boss Key
- Tokens
- Money
- Rare Item

Bosses must be configurable and use safe entity/event handling.

---

# 36. Last Man Standing

Optional Warzone event.

Flow:
- Players enter
- PvP activates
- Last player alive wins
- Winner receives reward

Example reward:
- $250,000
- God Key
- Exclusive title

---

# 37. Tournaments

Weekly tournament framework.

Formats:
- 1v1
- 2v2
- 4v4
- Faction
- LMS

Bracket:
Round 1
-> Quarter Finals
-> Semi Finals
-> Final

Implement:
- registration
- bracket generation
- match assignment
- arena allocation
- elimination
- rewards

Faction tournaments may score:
- Kills
- KOTH wins
- Raid wins
- Event wins
- F-Top position

Keep scoring configurable.

---

# 38. Leaderboards

Command:
/top

Categories:
- F-Top
- Kills
- Deaths
- K/D
- Balance
- Tokens
- Playtime
- KOTH Wins
- Raid Wins

Support physical NPC/hologram leaderboards at spawn.

---

# 39. Player Statistics

Track:
- Kills
- Deaths
- K/D
- Playtime
- Blocks mined
- Blocks placed
- Money earned
- Money spent
- Faction
- Faction kills
- KOTH captures
- Events won
- Raids

Seasonal statistics should be archived.

---

# 40. Scoreboard

Example:

------------------
      LEGACY
------------------

$ 1,245,200

Tokens: 12,420

Faction:
Reapers

Power:
84 / 100

K/D:
3.42

Online:
284

------------------
play.example.com

Everything configurable.

---

# 41. Tab List

Header:
LEGACY FACTIONS

Player format:
[RANK] Player

Footer:
Store: store.example.com
Discord: discord.gg/example

Do not hardcode final domains until the project branding is established.

---

# 42. Chat

Modes:
- Global
- Faction
- Ally
- Staff

Command:
/f chat

Example:
[FACTION] [Reapers] Player: enemy incoming

Use configurable formatting.

---

# 43. Combat

Implement:
- Combat tag
- Hit tracking
- Knockback
- Ender pearls
- Golden apples
- Potions
- Death handling
- Kill tracking

Combat tag example:
15 seconds

Disconnecting while combat tagged should result in configurable punishment, normally death.

---

# 44. Knockback

Classic competitive Factions PvP should have noticeable knockback.

Do not hardcode.

Example configuration:

knockback:
  horizontal: 0.42
  vertical: 0.36
  sprint-multiplier: 1.15

Support presets:
- Classic
- Competitive
- Custom

Test extensively on the chosen Minecraft version because combat behavior differs between versions.

---

# 45. Anti-Cheat

Use a current compatible anti-cheat appropriate for the selected Minecraft/Paper version.

Monitor:
- Reach
- Velocity
- Flight
- Speed
- KillAura
- AutoClick
- NoFall
- Inventory exploits
- Bad packets

Do not automatically permanently ban based on one detection.

Use:
- violation score
- staff review
- evidence logging

---

# 46. Exploit Testing

Explicitly test:
- Item duplication
- Item cloning
- NBT exploits
- Inventory desync
- TNT duplication
- Chunk exploits
- Ender pearl exploits
- Claim bypass
- Economy duplication
- Auction duplication
- Crate duplication
- Spawner duplication
- Death duplication
- Disconnect duplication
- Kit duplication

Create automated regression tests wherever possible.

---

# 47. CoreProtect

Use CoreProtect or equivalent.

Track:
- Block break
- Block place
- Container access
- Chest transactions
- Explosions
- Player actions

Staff commands:
- /co inspect
- /co lookup
- /co rollback
- /co restore

---

# 48. Admin System

Command:
/legacyadmin

GUI sections:
- Players
- Factions
- Economy
- Events
- World
- Items
- Kits
- Crates
- Punishments
- Season

Player tools:
- Teleport
- Freeze
- Mute
- Ban
- Kick
- Inventory
- Ender Chest
- Give Item
- Give Money
- Give Tokens
- Reset Player

Destructive actions require confirmation.

---

# 49. Staff Mode

Command:
/staff

Features:
- Vanish
- Flight
- Night Vision
- Staff Chat
- Teleport
- Inspect
- Freeze

Staff mode must prevent accidental normal-game actions.

---

# 50. Season System

Every competitive object should be associated with a season where appropriate.

Season:
- UUID
- name
- start date
- end date
- status
- world/seed information
- version
- payout information

Example:
Season 1
Season 2
Season 3

---

# 51. Season Reset

Commands:

/season prepare
/season start
/season stop
/season status
/season archive

Reset sequence:

1. Stop gameplay changes.
2. Save database state.
3. Save faction statistics.
4. Calculate final F-Top.
5. Freeze final rankings.
6. Generate payout records.
7. Archive season.
8. Reset or replace world.
9. Reset factions.
10. Reset claims.
11. Reset economy if configured.
12. Reset seasonal leaderboards.
13. Initialize new season.
14. Start new season.

Must be recoverable if a step fails.

Do not implement as one giant synchronous operation.

---

# 52. Season Rewards

Top faction rewards should support:
- Cash payout
- Exclusive title
- Cosmetic
- Season trophy
- Other configurable rewards

Example:
#1, #2, #3

Exact values must be configuration.

---

# 53. Season Length

Initial recommended testing season:
2 weeks

Initial production candidate:
4–8 weeks

Support configurable season durations:
- Hardcore: 2 weeks
- Competitive: 4 weeks
- Long: 8 weeks

Do not hardcode season length.

---

# 54. Cosmetics

Cosmetics:
- Kill effects
- Death effects
- Projectile trails
- Chat colors
- Titles
- Tags
- Particles
- Join messages
- Victory effects

Cosmetics should not affect PvP balance.

---

# 55. Ranks

Use an original naming scheme.

Suggested structure:
- Default
- VIP
- VIP+
- Elite
- Legend
- God
- Highest premium/cosmetic tier

Do not copy VanityMC's exact branding.

Rank benefits may include:
- Extra homes
- Kit access
- /sell features
- Colored chat
- Cosmetics
- Auction slots
- Faction perks

Avoid unrestricted combat advantages unless intentionally designed and configured.

---

# 56. Voting

Command:
/vote

Rewards:
- Money
- Vote Keys
- Tokens
- Temporary buffs

Vote Party:
When configured vote threshold is reached:
- announce Vote Party
- reward all eligible online players
- play fireworks/particles/sounds

Example:
100 votes
-> Vote Key
-> $25,000
-> 500 Tokens

Rewards configurable.

---

# 57. Daily Rewards

Command:
/rewards

Example 7-day cycle:
Day 1: $10,000
Day 2: Key
Day 3: Tokens
Day 4: $25,000
Day 5: Rare Key
Day 6: Enchant Book
Day 7: God Key

Make progression configurable.

---

# 58. Missions

Optional.

Keep missions simple:
- Kill 10 players
- Mine 500 blocks
- Sell $100,000
- Capture KOTH
- Open 3 crates

Rewards:
- Tokens
- Money
- Keys

Do not turn missions into a full MMORPG quest system.

---

# 59. GUI Design

Use one consistent visual language.

Recommended:
- dark background
- metallic/fantasy aesthetic
- gold/white primary text
- red enemy state
- green ally state
- gray inactive state

Avoid excessive rainbow gradients.

All GUI titles, icons, sounds, and lore configurable where practical.

---

# 60. NPCs and Holograms

NPCs:
- Factions
- Shop
- Crates
- Kits
- Enchants
- Auction
- Events
- Ranks
- Rules

Holograms:
- Top factions
- Event status
- Server information
- Vote information

Leaderboard/hologram updates must be asynchronous/cached.

---

# 61. Resource Pack

Create an original 32x32-style resource pack.

Custom textures:
- Keys
- Crates
- Custom weapons
- Custom armor
- Tokens
- Special tools
- GUI icons
- Ranks

Do not reuse VanityMC's old resource pack.

Resource pack must have its own identity.

---

# 62. Branding

Visual direction:
- Dark
- Premium
- Fantasy
- Competitive
- Arcane
- Metal
- Stone
- Gold
- Red
- Purple

Create original:
- Server name
- Logo
- Icons
- Typography
- Textures
- Builds

Do not reproduce VanityMC branding.

---

# 63. Build Package

Required original builds:
1. Spawn
2. Warzone
3. KOTH Castle
4. KOTH Desert
5. KOTH Jungle
6. KOTH Volcano
7. Event Arena
8. Tournament Arena
9. Boss Arena
10. Crate Area

Spawn:
~200 x 200 blocks

Warzone:
~500 x 500 blocks initially

Use vertical terrain.

---

# 64. Performance Requirements

Primary target:
20 TPS under normal operation.

Stress test:
- 50 players
- 100 players
- 200 players
- 500 players if infrastructure permits

Test:
- PvP
- TNT
- mob farms
- chunk loading
- F-Top
- events
- auctions
- crates
- faction calculations

Use Spark profiling.

Never:
- scan every faction every tick
- scan every chunk every tick
- calculate F-Top synchronously during gameplay
- execute unbounded entity loops
- perform database queries per player per tick

---

# 65. Async/Sync Rules

Safe to perform asynchronously:
- database reads/writes
- economy transaction processing where API permits
- F-Top calculation
- leaderboard calculation
- large data serialization
- analytics

Must be synchronized with the Bukkit/Paper main thread when required:
- world modification
- entity manipulation
- inventory operations
- player teleportation
- most Bukkit API interactions

Use scheduler APIs correctly for the selected Paper version.

---

# 66. Logging and Auditing

Important logs:
- Economy transactions
- Admin commands
- Item grants
- Item removals
- Faction ownership changes
- Claim changes
- Season changes
- Payouts
- Crate rewards
- Auction transactions
- Raid events
- Punishments

Example economy log:

[ECONOMY]
Player: UUID
Action: SELL
Item: DIAMOND
Amount: 64
Before: $100000
After: $164000

Audit records must be queryable.

---

# 67. Master Configuration

Example:

server:
  name: "Legacy"
  version: "1.0"

factions:
  power-per-player: 10
  power-per-claim: 5
  death-loss: 2
  power-regeneration: 0.5

combat:
  combat-tag-seconds: 15

economy:
  starting-balance: 1000

events:
  koth:
    enabled: true
    interval: 3600

season:
  duration-days: 42

All values should be loaded from configuration and validated on startup.

Invalid configuration should:
- clearly report the error
- identify the file and field
- avoid silently using dangerous defaults

---

# 68. MVP

Do not implement the entire specification before producing a playable server.

MVP includes:

[ ] Paper/Purpur server
[ ] Gradle Java project
[ ] Database
[ ] LegacyCore
[ ] Player profiles
[ ] Economy
[ ] Factions
[ ] Claims
[ ] Power
[ ] Faction ranks
[ ] Faction chat
[ ] Faction map
[ ] /f top
[ ] Shop
[ ] Basic spawners
[ ] Basic kits
[ ] Combat tag
[ ] Kill/death statistics
[ ] Scoreboard
[ ] LuckPerms integration
[ ] PlaceholderAPI integration
[ ] CoreProtect integration
[ ] WorldGuard integration

MVP is considered complete only when:
- server starts cleanly
- players can join
- players can create factions
- players can claim land
- power works
- economy works
- PvP works
- F-Top works
- data survives restart
- no critical duplication exploit exists
- basic stress testing passes

---

# 69. Version 0.2 — Progression

Implement:

[ ] Tokens
[ ] Custom enchants
[ ] Custom items
[ ] Crates
[ ] Keys
[ ] Faction upgrades
[ ] Harvester Hoe
[ ] Auction House
[ ] KOTH
[ ] Envoys
[ ] Airdrops

Acceptance:
Players can progress from starter gear through the economy/custom-enchant system and participate in events.

---

# 70. Version 0.3 — Warfare

Implement:

[ ] TNT system
[ ] Cannon mechanics
[ ] Raid system
[ ] Raidability
[ ] Raid shield
[ ] Advanced combat
[ ] Anti-exploit checks
[ ] Advanced F-Top

Acceptance:
Two factions can build, attack, defend, become raid-able, and conduct a complete raid without server instability or item duplication.

---

# 71. Version 0.4 — Seasonal Competition

Implement:

[ ] SOTW
[ ] Season management
[ ] F-Top payouts
[ ] Tournament system
[ ] Bosses
[ ] LMS
[ ] Vote parties
[ ] Daily rewards
[ ] Cosmetics

Acceptance:
A complete season can be started, played, finalized, paid out, archived, and reset into a new season.

---

# 72. Version 0.5 — Polish

Implement:
[ ] Original resource pack
[ ] Final builds
[ ] GUI polish
[ ] NPCs
[ ] Holograms
[ ] Tab list
[ ] Chat formatting
[ ] Sounds
[ ] Particles
[ ] Tutorials
[ ] Rules
[ ] Staff tools
[ ] Documentation

---

# 73. Testing Strategy

Unit tests required for:
- faction power
- claim limits
- economy transactions
- F-Top calculations
- enchantment probabilities
- enchant conflicts
- kit cooldowns
- crate reward weights
- season payout calculations
- raidability
- auction transactions

Integration tests:
- player creation
- faction creation
- faction membership
- claims
- economy
- custom item serialization
- server restart persistence
- season transition

Load tests:
- 50+ concurrent players
- many factions
- many claims
- large mob farms
- repeated TNT explosions
- simultaneous events
- F-Top calculation
- auction activity

---

# 74. Security/Exploit Principles

Every transaction should be server-authoritative.

Never trust:
- client item data
- client inventory state
- client movement
- client packet claims
- client economy values

Validate:
- item ownership
- item quantity
- currency balance
- kit cooldown
- crate key ownership
- auction item ownership
- faction permissions
- claim permissions

---

# 75. Recovery

Create backups before:
- season reset
- major migration
- database schema migration
- mass rollback
- world replacement

Database migrations must be versioned.

Never make irreversible destructive operations without:
- confirmation
- backup
- audit record

---

# 76. Administrative Commands

Required administrative concepts:

/legacyadmin
/season
/sotw
/cannon
/f top recalculate
/legacy reload
/legacy debug
/legacy status

Only authorized permissions may execute them.

---

# 77. Reload Policy

Do not rely on `/reload`.

Implement targeted configuration reload:

/legacy reload

Reload only safe configuration files.

Never dynamically reload:
- database connection
- core event listeners
- critical item registries
- faction state

without explicit implementation.

---

# 78. Observability

Provide:
- TPS
- MSPT
- memory usage
- online players
- active events
- database latency
- queued tasks
- F-Top calculation duration
- entity counts
- loaded chunks

Command:
/legacy status

This is especially important during raids and SOTW.

---

# 79. Implementation Order

STRICT ORDER:

PHASE 1
Infrastructure
-> Paper
-> Gradle
-> Java
-> Database
-> LegacyCore

PHASE 2
Player profiles
-> Economy
-> Factions
-> Claims
-> Power
-> Relations

PHASE 3
PvP
-> Combat
-> Statistics
-> Scoreboard

PHASE 4
Progression
-> Shop
-> Spawners
-> Tokens
-> Items
-> Enchants
-> Kits
-> Crates

PHASE 5
Events
-> KOTH
-> Envoys
-> Airdrops
-> Boss
-> LMS

PHASE 6
Warfare
-> TNT
-> Cannons
-> Raidability
-> Raid shield

PHASE 7
Competition
-> F-Top
-> Tournaments
-> Seasons
-> SOTW
-> Payouts

PHASE 8
Polish
-> GUI
-> NPCs
-> Holograms
-> Builds
-> Resource pack
-> Cosmetics

PHASE 9
Hardening
-> Exploit testing
-> Load testing
-> Backup/recovery
-> Documentation

---

# 80. Claude Operating Procedure

When beginning work:

1. Inspect the existing repository.
2. Determine what already exists.
3. Do not overwrite existing work without understanding it.
4. Create/update a TODO implementation checklist.
5. Establish the build system.
6. Establish the database layer.
7. Build LegacyCore.
8. Compile.
9. Test.
10. Only then proceed to Factions.

For every feature:
1. Design the data model.
2. Design the API.
3. Implement.
4. Add configuration.
5. Add tests.
6. Compile.
7. Run relevant tests.
8. Document commands/configuration.
9. Mark the feature complete.
10. Proceed to the next feature.

If a dependency or Minecraft API differs from assumptions:
- inspect the actual installed version
- adapt implementation to the actual API
- do not invent methods/classes
- compile to validate

If a feature cannot be safely implemented yet:
- leave a clear TODO
- do not create fake functionality
- do not silently skip requirements

---

# 81. Definition of Done

A feature is NOT complete merely because the code compiles.

A feature is complete when:
- code compiles
- configuration exists
- permissions exist where necessary
- commands work
- GUI works where applicable
- database persistence works where applicable
- reload/restart behavior is correct
- error handling exists
- logging exists where necessary
- tests exist for important logic
- no obvious duplication/exploit path exists
- documentation is updated

---

# 82. Final Product Goal

The final server should feel like:

CLASSIC COMPETITIVE FACTIONS
+
CUSTOM ENCHANT PROGRESSION
+
ECONOMY
+
SPAWNERS/FARMING
+
PVP
+
TNT RAIDING
+
KOTH/EVENTS
+
F-TOP
+
SEASONAL COMPETITION

while using:

MODERN JAVA
+
MODERN PAPER/PURPUR
+
PROPER DATABASE ARCHITECTURE
+
ASYNC PROCESSING
+
TESTING
+
EXPLOIT PREVENTION
+
PERFORMANCE MONITORING
+
ORIGINAL BRANDING/ASSETS

The result should be an original modern Factions server that captures the gameplay style of the classic VanityMC era without copying proprietary assets or implementation.

END OF SPECIFICATION
