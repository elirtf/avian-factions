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
lp group hatchling meta setprefix 10 "&#9E9E9E[Hatchling] "
lp group hatchling permission set essentials.spawn true
lp group hatchling permission set essentials.tpa true
lp group hatchling permission set essentials.tpaccept true
lp group hatchling permission set essentials.tpdeny true
lp group hatchling permission set essentials.msg true
lp group hatchling permission set essentials.r true
lp group hatchling permission set essentials.help true
lp group hatchling permission set essentials.rules true
lp group hatchling permission set essentials.balance true
lp group hatchling permission set essentials.pay true
lp group hatchling permission set essentials.home true
lp group hatchling permission set essentials.sethome true
lp group hatchling permission set essentials.sethome.multiple.hatchling true
lp group hatchling meta set homes 1
lp group hatchling permission set EconomyShopGUI.shop true
lp group hatchling permission set EconomyShopGUI.sellall true
# F-Top is /ftop (value of spawners and blocks). FactionsUUID's own /f top ranks by money and
# would contradict it, so it is hidden from everyone.
lp group hatchling permission set factions.top false

lp creategroup fledgling
lp group fledgling setweight 20
lp group fledgling meta setprefix 20 "&#C9C9C9[Fledgling] "
lp group fledgling parent add hatchling
lp group fledgling permission set essentials.sethome.multiple.fledgling true
lp group fledgling meta set homes 2
lp group fledgling permission set essentials.back true

lp createtrack free
lp track free append hatchling
lp track free append fledgling

# ---------------------------------------------------------------------------------------------
# Paid ranks: Finch -> Cardinal -> Falcon -> Hawk -> Raven.
#
# Nothing sold may be EXCLUSIVE (ADR-0006): money may buy something sooner, never something a free
# player cannot reach. Kits and gear ARE allowed here, because the same kits are winnable from
# crates and events — it just takes a free player longer. Each gameplay perk needs its in-game
# route and rate written down first.
#
# These tiers are thin today only because no kit system exists yet and EssentialsX is all we have.
# That is a gap to fill (#18, crate tables), not a rule.
#
# Each tier inherits the one below, so a Raven has everything.
# Particles, pets and hats beyond Essentials' need a cosmetics plugin we have not chosen yet —
# see #22. Until then these tiers are thin, and that is honest rather than padded with advantage.
# ---------------------------------------------------------------------------------------------

lp creategroup finch
lp group finch setweight 100
lp group finch meta setprefix 100 "&#E0B44A[Finch] "
lp group finch permission set essentials.nick true
lp group finch permission set essentials.nick.color true
lp group finch permission set essentials.chat.color true

lp creategroup cardinal
lp group cardinal setweight 110
lp group cardinal meta setprefix 110 "&#C1444A[Cardinal] "
lp group cardinal parent add finch
lp group cardinal permission set essentials.hat true
lp group cardinal permission set essentials.chat.format true

lp creategroup falcon
lp group falcon setweight 120
lp group falcon meta setprefix 120 "&#D4A017[Falcon] "
lp group falcon parent add cardinal
lp group falcon permission set essentials.nick.hex true
lp group falcon permission set essentials.itemname true

lp creategroup hawk
lp group hawk setweight 130
lp group hawk meta setprefix 130 "&#8E5FBF[Hawk] "
lp group hawk parent add falcon
lp group hawk permission set essentials.itemlore true

lp creategroup raven
lp group raven setweight 140
lp group raven meta setprefix 140 "&#5B2C8D&l[Raven] "
lp group raven parent add hawk
lp group raven permission set essentials.nick.others false

lp createtrack paid
lp track paid append finch
lp track paid append cardinal
lp track paid append falcon
lp track paid append hawk
lp track paid append raven

# ---------------------------------------------------------------------------------------------
# Staff. Deliberately NOT bird-themed: players must identify staff instantly, in chat, under
# pressure, mid-raid. The only concession to the brand is the palette.
# ---------------------------------------------------------------------------------------------

lp creategroup helper
lp group helper setweight 200
lp group helper meta setprefix 200 "&#8E5FBF[Helper] "
lp group helper permission set essentials.mute true
lp group helper permission set essentials.kick true
lp group helper permission set essentials.tempban true
lp group helper permission set essentials.vanish true
lp group helper permission set coreprotect.inspect true
lp group helper permission set coreprotect.lookup true

lp creategroup mod
lp group mod setweight 210
lp group mod meta setprefix 210 "&#5B2C8D[Mod] "
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
lp group admin meta setprefix 220 "&#D4A017[Admin] "
lp group admin parent add mod
lp group admin permission set essentials.gamemode true
lp group admin permission set essentials.give true
lp group admin permission set worldedit.* true
lp group admin permission set worldguard.* true
# FactionsUUID's mod kit adds /fa power, grace (raid windows by hand), disband any; and F-Top recalc.
lp group admin permission set factions.kit.mod true
lp group admin permission set avian.ftop.admin true

lp creategroup owner
lp group owner setweight 230
lp group owner meta setprefix 230 "&#C1444A&l[Owner] "
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
