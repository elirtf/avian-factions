---
status: accepted
date: 2026-09-22
---
# Nothing sold is exclusive: anything bought must be obtainable in game

Money may not buy something a free player cannot get. It may buy it **sooner**.

Mojang's Usage Guidelines forbid selling anything "designed to give someone a competitive gameplay
advantage or make another player's experience worse". The operative word is *designed*. A reward a
free player can also earn, at a rate they realistically reach, is not designed to confer advantage
— it sells time. That is the standard model across the genre and it is the one we follow.

**One rule, applied everywhere.** There is no softer category and no stricter one:

- **Paid ranks may carry gameplay perks** — kits, gear sets, whatever — because those same kits and
  sets are winnable in game from crates, events and achievement. It takes a free player longer and
  costs them more effort; that is the whole product.
- **Crate keys may be sold with gameplay contents**, because the same keys drop from voting,
  bosses, KOTH and the Aerie. There is no separate cosmetic-only store key line.
- **Cosmetics** — prefixes, chat colour, particles, pets, join messages, nicknames — carry no
  condition at all, because there is nothing to be exclusive about.

**The condition the first two rest on entirely:** the free route must be real. "Obtainable in game"
does a lot of work in that sentence — a drop rate that exists to win the argument is not a free
route, it is the violation with extra steps. If a Raven kit is a realistic reward for a month of
determined play, selling it is selling time. If it is a one-in-ten-thousand drop, it is not.
**Whenever anything goes in the store, its in-game route and rate get written down and checked.**

Still never sold, because no free route makes them fair: anything that makes another player's
experience worse, and advantages with no in-game equivalent at all.

**Considered:** cosmetic-only for everything including keys — rejected; it is stricter than the
rule requires, leaves the store with almost nothing to sell once you accept cosmetics are all
EssentialsX gives us, and treats a widely-accepted model as forbidden. A middle path where only
*small* advantages are sold, unconditionally — rejected; "small" is unenforceable in review, where
"also obtainable free" is at least checkable. Cosmetics plus "priority queue" — parked, see
consequences.

**A note on sourcing, because the earlier version of this ADR overstated it:** the claim that paid
crate keys with gameplay contents are *themselves* a violation came from a third-party
server-admin wiki, not from Mojang. Mojang's own text is the "designed to give a competitive
advantage" wording quoted above. The stricter reading was an interpretation presented as the rule.

**Consequences:** the paid ranks have something real to sell — kits and sets, not only colours —
which matters because cosmetics alone would have left the store thin. It also means the free side
needs those same kits reachable through crates and events, so rank design and crate design are one
problem rather than two.

There is **one** set of crates, not an earned line and a store line — the same
Talon key drops from a boss and sells in the store. That removes a rule we could only have enforced
by remembering to, which is a real gain: CrazyCrates cannot tell an earned key from a bought one,
so the split would have lived on discipline alone.

What replaces it is a lighter but checkable obligation: **for every item in the store, the in-game
route and its rate are written down**, so "is this obtainable free" has an answer someone can
audit rather than an assurance. Where a rate turns out to be nominal, the fix is to raise the drop
rate, not to remove the item from the store.

Spec §12's "premium currency should primarily provide cosmetics/perks and should not be required
for competitive progression **unless explicitly configured**" still holds as written — gems must
never be *required* for progression, whatever else they can buy.

Priority queue on a full server is arguably access rather than advantage; it stays undecided until
we ever have a queue.

## The ladders

**Paid — five tiers**, escalating from small bird to bird of prey:

**Finch → Cardinal → Falcon → Hawk → Raven.**

**Free — two tiers only**: **Hatchling** (on joining) and **Fledgling** (early play). There is
deliberately no long free rank ladder mirroring the paid one.

The compliance mechanism is not a parallel ladder — it is that **the perks themselves are
obtainable in game**. Ranks, their kits and their sets can be won from crates, events and
achievement, so nothing a paying player has is closed to a free one; it just takes longer and more
effort. That is simpler than two ladders and it is what keeps the perks honest.

Paid ranks may carry kits and gear, since the same kits and sets are winnable in game — slower and
with more effort, which is exactly what a paying player is skipping. The condition is the same as
for the store: **a gameplay perk on a paid rank needs a written-down in-game route and a rate a
real player reaches.** Where a perk has no in-game equivalent at all, it does not go on a rank.

As configured today the paid tiers only carry cosmetics — nick, chat colour, hat, item naming —
because that is all EssentialsX offers and no kit system exists yet. That is a gap in what we have
built, not a rule. Kits are the obvious thing to add once #18 and the crate tables land.

**Keys** (Feather, Plume, Talon, Pinion, Raven) come from voting, bosses, KOTH and the Aerie, and
may also be sold. Same key, same table, either route — the store sells the shortcut, not the
contents.
