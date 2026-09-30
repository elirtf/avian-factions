# Avian Factions rank setup — the source of truth for groups, tracks, prefixes and permissions.
#
# Apply with:  ./gradlew applyRanks      (see avian-plugin/build.gradle.kts)
# or paste into the server console. Every line is a console command; lines starting with # and
# blank lines are skipped. Re-running is safe: creategroup on an existing group is a no-op and
# every other command sets rather than appends.
#
# Naming and the reasoning behind it: docs/research/server-inspiration.md §2.
# Monetisation rule these permissions must respect: docs/adr/0006-nothing-sold-is-exclusive.md.
# In short: nothing sold may be exclusive. Money may buy something sooner, never something a free
# player cannot reach.
#
# Palette (brand: stone / gold / red / purple, dark premium arcane):
#   stone  #9E9E9E   light stone #C9C9C9
#   gold   #E0B44A   bright gold #D4A017
#   red    #C1444A
#   purple #8E5FBF   deep purple #5B2C8D

# ---------------------------------------------------------------------------------------------
# Free ranks. Two only, by design — there is no long free ladder, because rank perks are meant to
# be obtainable through crates, events and achievement instead (ADR-0006).
# These carry the baseline gameplay permissions every player needs.
# ---------------------------------------------------------------------------------------------

lp creategroup hatchling
lp group hatchling setweight 10
lp group hatchling meta removeprefix 10
# Rank tags are suffixes, shown before the name in chat via %avian_rank%. They are MiniMessage
# (CarbonChat renders chat), not &-codes: <#RRGGBB> colours, <bold>, <gradient:#A:#B>.
lp group hatchling meta setsuffix 10 "<#9E9E9E>ʜᴀᴛᴄʜʟɪɴɢ"
lp group hatchling permission set essentials.spawn true
lp group hatchling permission set betterrtp.use true
# BetterRTP also checks a per-world permission; without it a non-op gets "not allowed rtp in the world
# world" (found by the owner, 2026-09-29). The Nether and End send /rtp to the overworld (Overrides).
lp group hatchling permission set betterrtp.world.world true
lp group hatchling permission set essentials.tpa true
lp group hatchling permission set essentials.tpaccept true
lp group hatchling permission set essentials.tpdeny true
lp group hatchling permission set essentials.msg true
lp group hatchling permission set essentials.r true
# Chat is CarbonChat (hover card, ➜ arrow): speaking, private messages, ignoring.
lp group hatchling permission set carbon.channel.global.see true
lp group hatchling permission set carbon.channel.global.speak true
lp group hatchling permission set carbon.whisper true
lp group hatchling permission set carbon.whisper.send true
lp group hatchling permission set carbon.whisper.receive true
lp group hatchling permission set carbon.whisper.message true
lp group hatchling permission set carbon.whisper.reply true
lp group hatchling permission set carbon.whisper.continue true
lp group hatchling permission set carbon.whisper.ping_sounds true
lp group hatchling permission set carbon.ping_sounds true
lp group hatchling permission set carbon.ignore true
lp group hatchling permission set carbon.ignore.unignore true
lp group hatchling permission set carbon.itemlink true
# Kits: the weekly Harvester Hoe (/kit harvester, dev-server/plugins/Essentials/kits.yml).
lp group hatchling permission set essentials.kit true
lp group hatchling permission set essentials.kits.harvester true
lp group hatchling permission set essentials.help true
lp group hatchling permission set essentials.rules true
lp group hatchling permission set essentials.balance true
lp group hatchling permission set essentials.pay true
lp group hatchling permission set essentials.home true
lp group hatchling permission set essentials.sethome true
# Home limits are keys under sethome-multiple in Essentials' config.yml; a player needs BOTH
# essentials.sethome.multiple and essentials.sethome.multiple.<key>, or EssentialsX caps them at 1.
lp group hatchling permission set essentials.sethome.multiple true
lp group hatchling permission set essentials.sethome.multiple.hatchling true
lp group hatchling meta set homes 1
lp group hatchling permission set EconomyShopGUI.shop true
lp group hatchling permission set EconomyShopGUI.sellall true
# /ec: your own ender chest anywhere, for everyone (owner, 2026-09-29); never someone else's (that is
# essentials.enderchest.others). Spawners can't be kept in it (avian-factions EnderChestSpawnerBan).
lp group hatchling permission set essentials.enderchest true
# /f fly (flight in your own land) is everyone's: FactionsUUID grants it by default. An earlier version
# denied it here; these unsets clear that from databases that have it. Essentials /fly (anywhere) is
# Phoenix's and a crate prize (docs/ECONOMY.md → Flight and rank prizes). Neither works in combat.
lp group hatchling permission unset factions.fly
lp group hatchling permission unset factions.fly.auto
# Custom enchants (ExcellentEnchants): /eenchants list shows every enchant and what it does. Books
# come from /enchanter (tokens), crates, enchanting tables, loot and villagers.
lp group hatchling permission set excellentenchants.command.list true
# Auction house (CrazyAuctions, /ah): the most listings a player may have at once is the HIGHEST
# crazyauctions.sell.<n> / bid.<n> they hold. With none at all the limit is unlimited, so every rank
# needs one. Paid ranks inherit Fledgling's.
# The player commands. CrazyAuctions declares these without registering them, so they are not true
# by default: without these lines only ops could sell, bid or collect (found in testing, 2026-09-29).
lp group hatchling permission set crazyauctions.access true
lp group hatchling permission set crazyauctions.use true
lp group hatchling permission set crazyauctions.help true
lp group hatchling permission set crazyauctions.view true
lp group hatchling permission set crazyauctions.sell true
lp group hatchling permission set crazyauctions.bid true
lp group hatchling permission set crazyauctions.listed true
lp group hatchling permission set crazyauctions.expired true
lp group hatchling permission set crazyauctions.collect true
lp group hatchling permission set crazyauctions.sell.5 true
lp group hatchling permission set crazyauctions.bid.5 true
# F-Top is ours: /f top (rewritten to /ftop) opens a menu ranked by spawner and block value. FactionsUUID's own /f top ranks by money and
# would contradict it, so it is hidden from everyone.
lp group hatchling permission set factions.top false
# Faction warps: /f warp and /f setwarp. FactionsUUID doesn't give these by default (found by the owner,
# 2026-09-29); the "warps" upgrade in factions.conf sets how many (5, free).
lp group hatchling permission set factions.warp true
lp group hatchling permission set factions.setwarp true

# Fledgling is earned by 12 hours of playtime: PlayTimeManager adds the group
# (dev-server/plugins/PlayTimeManager/Goals/fledgling.yml).
lp creategroup fledgling
lp group fledgling setweight 20
lp group fledgling meta removeprefix 20
lp group fledgling meta setsuffix 20 "<#C9C9C9>ꜰʟᴇᴅɢʟɪɴɢ"
lp group fledgling parent add hatchling
lp group fledgling permission set essentials.sethome.multiple.fledgling true
lp group fledgling meta set homes 2
lp group fledgling permission set essentials.back true
# /craft: a crafting table anywhere, from Fledgling up (owner, 2026-09-29).
lp group fledgling permission set essentials.workbench true
lp group fledgling permission set crazyauctions.sell.8 true
lp group fledgling permission set crazyauctions.bid.8 true

lp createtrack free
lp track free append hatchling
lp track free append fledgling

# ---------------------------------------------------------------------------------------------
# Paid ranks: Harpy -> Griffin -> Wyvern -> Dragon, then Phoenix (owner, 2026-09-29; they replaced
# Finch -> Cardinal -> Falcon -> Hawk -> Raven, which are deleted below).
#
# Phoenix is NOT sold. The only way to get it is winning F-Top: the winning faction's leader chooses
# which member gets it (owner, 2026-09-29). It sits on this track because it is the top of the ladder.
#
# Nothing sold may be EXCLUSIVE (ADR-0006): money may buy something sooner, never something a free
# player cannot reach. Kits and gear ARE allowed here, because the same kits are winnable from
# crates and events — it just takes a free player longer. Each gameplay perk needs its in-game
# route and rate written down first.
#
# Each tier inherits the one below, and Harpy inherits Fledgling, so a Phoenix has everything.
#
# Per tier (owner, 2026-09-29):     auction listings   also
#   Harpy                              10                 coloured nickname and chat, /pweather
#   Griffin                            12                 /hat, bold/italic chat
#   Wyvern                             15                 hex and gradient colours, /itemname
#   Dragon                             18                 /itemlore
#   Phoenix (F-Top only)               21                 /fly anywhere (outside combat)
# Homes are 2 for every rank past Hatchling (Fledgling's, inherited); /craft is Fledgling's and /ec
# everyone's. Auction limits take the highest crazyauctions.sell.<n> a player holds.
# Each tier also has a weekly kit, /kit <rank> (Essentials kits.yml).
# ---------------------------------------------------------------------------------------------

# The old paid ranks. Nobody held them when they were renamed. Deleting a group that is already
# gone only prints a notice, so these stay safe to re-run.
lp deletegroup finch
lp deletegroup cardinal
lp deletegroup falcon
lp deletegroup hawk
lp deletegroup raven

lp creategroup harpy
lp group harpy setweight 100
lp group harpy meta removeprefix 100
lp group harpy meta setsuffix 100 "<#E0474C>ʜᴀʀᴘʏ"
lp group harpy parent add fledgling
lp group harpy permission set essentials.nick true
lp group harpy permission set essentials.nick.color true
# Chat colours are CarbonChat MiniMessage tags now (<red>hi, <#FF4FA3>hi); EssentialsXChat is gone.
lp group harpy permission unset essentials.chat.color
lp group harpy permission set carbon.messagetags.color true
# /pweather: your own weather, seen only by you (owner, 2026-09-29).
lp group harpy permission set essentials.pweather true
lp group harpy permission set essentials.kits.harpy true
lp group harpy permission set crazyauctions.sell.10 true
lp group harpy permission set crazyauctions.bid.10 true

lp creategroup griffin
lp group griffin setweight 110
lp group griffin meta removeprefix 110
lp group griffin meta setsuffix 110 "<#F0923A>ɢʀɪꜰꜰɪɴ"
lp group griffin parent add harpy
lp group griffin permission set essentials.hat true
lp group griffin permission unset essentials.chat.format
lp group griffin permission set carbon.messagetags.decorations true
lp group griffin permission set essentials.nick.format true
lp group griffin permission set crazyauctions.sell.12 true
lp group griffin permission set crazyauctions.bid.12 true
lp group griffin permission set essentials.kits.griffin true

lp creategroup wyvern
lp group wyvern setweight 120
lp group wyvern meta removeprefix 120
lp group wyvern meta setsuffix 120 "<#B07FE0>ᴡʏᴠᴇʀɴ"
lp group wyvern parent add griffin
# Hex colours. EssentialsX calls this "rgb"; an earlier version granted "essentials.nick.hex", which
# does not exist, so the unset below cleans it out of databases that already have it.
lp group wyvern permission unset essentials.nick.hex
lp group wyvern permission set essentials.nick.rgb true
lp group wyvern permission unset essentials.chat.rgb
lp group wyvern permission set carbon.messagetags.gradient true
lp group wyvern permission set essentials.itemname true
lp group wyvern permission set essentials.kits.wyvern true
lp group wyvern permission set crazyauctions.sell.15 true
lp group wyvern permission set crazyauctions.bid.15 true

lp creategroup dragon
lp group dragon setweight 130
lp group dragon meta removeprefix 130
lp group dragon meta setsuffix 130 "<bold><#FFD84D>ᴅʀᴀɢᴏɴ</bold>"
lp group dragon parent add wyvern
lp group dragon permission set essentials.itemlore true
lp group dragon permission set crazyauctions.sell.18 true
lp group dragon permission set crazyauctions.bid.18 true
lp group dragon permission set essentials.kits.dragon true

lp creategroup phoenix
lp group phoenix setweight 140
lp group phoenix meta removeprefix 140
lp group phoenix meta setsuffix 140 "<bold><gradient:#FFD84D:#FF7A45:#FF3B3B>ᴘʜᴏᴇɴɪx</gradient></bold>"
lp group phoenix parent add dragon
lp group phoenix permission set essentials.nick.others false
# /fly anywhere, outside combat (the combat tag grounds you and blocks it). Owner, 2026-09-29.
lp group phoenix permission set essentials.fly true
lp group phoenix permission set essentials.kits.phoenix true
lp group phoenix permission set crazyauctions.sell.21 true
lp group phoenix permission set crazyauctions.bid.21 true

# Rebuilt from scratch each run, so a renamed rank never lingers on the track.
lp deletetrack paid
lp createtrack paid
lp track paid append harpy
lp track paid append griffin
lp track paid append wyvern
lp track paid append dragon
lp track paid append phoenix

# ---------------------------------------------------------------------------------------------
# Playtester: people who test the server before launch and report bugs (owner, 2026-09-29). A badge,
# not a perk: it shows instead of their free or paid rank (weight above paid, below staff) and
# carries Fledgling's permissions so a tester is not stuck on one home. Give it by hand:
#   lp user <name> parent add playtester
# Their name stays light gray, like every non-staff player (TAB/groups.yml).
# ---------------------------------------------------------------------------------------------

lp creategroup playtester
lp group playtester setweight 150
lp group playtester meta removeprefix 150
lp group playtester meta setsuffix 150 "<#FF6FB5>ᴘʟᴀʏᴛᴇꜱᴛᴇʀ"
lp group playtester parent add fledgling

# ---------------------------------------------------------------------------------------------
# Staff. Deliberately NOT bird-themed: players must identify staff instantly, in chat, under
# pressure, mid-raid. The only concession to the brand is the palette.
# ---------------------------------------------------------------------------------------------

lp creategroup helper
lp group helper setweight 200
lp group helper meta removeprefix 200
lp group helper meta setsuffix 200 "<#5FD3C7>ʜᴇʟᴘᴇʀ"
lp group helper permission set essentials.mute true
lp group helper permission set carbon.mute true
lp group helper permission set carbon.mute.unmute true
lp group helper permission set carbon.mute.info true
lp group helper permission set carbon.mute.notify true
lp group helper permission set essentials.kick true
lp group helper permission set essentials.tempban true
lp group helper permission set essentials.vanish true
lp group helper permission set coreprotect.inspect true
lp group helper permission set coreprotect.lookup true
# GrimAC: see cheat alerts in chat (and toggle them with /grim alerts).
lp group helper permission set grim.alerts true

lp creategroup mod
lp group mod setweight 210
lp group mod meta removeprefix 210
lp group mod meta setsuffix 210 "<#4DA3FF>ᴍᴏᴅ"
lp group mod parent add helper
lp group mod permission set essentials.ban true
lp group mod permission set essentials.tp true
lp group mod permission set essentials.tphere true
lp group mod permission set essentials.invsee true
lp group mod permission set coreprotect.rollback true
# Factions come from FactionsUUID (ADR-0007). Its halfmod kit: bypass claims, manage safezone and
# warzone, kick from any faction, faction chat spy.
lp group mod permission set factions.kit.halfmod true

lp creategroup admin
lp group admin setweight 220
lp group admin meta removeprefix 220
lp group admin meta setsuffix 220 "<#3DDC84>ᴀᴅᴍɪɴ"
lp group admin parent add mod
lp group admin permission set essentials.gamemode true
lp group admin permission set essentials.give true
# /tokens and /gems give|take|set and /tokens|gems <player> (#26); money is EssentialsX /eco. Console always can.
lp group admin permission set avian.admin.economy true
lp group admin permission set avian.hoe.admin true
lp group admin permission set avian.sellwand.admin true
lp group admin permission set avian.admin.chunkbuster true
lp group admin permission set worldedit.* true
lp group admin permission set worldguard.* true
# FactionsUUID's mod kit adds /fa power, grace (raid windows by hand), disband any; and F-Top recalc.
lp group admin permission set factions.kit.mod true
lp group admin permission set avian.ftop.admin true

lp creategroup owner
lp group owner setweight 230
lp group owner meta removeprefix 230
lp group owner meta setsuffix 230 "<bold><gradient:#FF4FA3:#FFBFE3>ᴏᴡɴᴇʀ</gradient></bold>"
lp group owner parent add admin
lp group owner permission set * true

lp createtrack staff
lp track staff append helper
lp track staff append mod
lp track staff append admin
lp track staff append owner

# ---------------------------------------------------------------------------------------------
# Default group: everyone starts as a Hatchling.
# ---------------------------------------------------------------------------------------------

lp group default parent add hatchling
