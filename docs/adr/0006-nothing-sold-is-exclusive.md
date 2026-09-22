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

So:

- **Paid ranks** carry cosmetic and social perks: prefixes, chat colour, particles, pets, join
  messages, nicknames, Discord status. These are the safest thing to sell and carry no conditions.
- **Crate keys may be sold with gameplay contents**, because the same keys drop from voting,
  bosses, KOTH and the Aerie. There is no separate cosmetic-only store key line.
- **Anything else gameplay-relevant** may be sold only if it is obtainable in game on the same
  terms.

**The condition this rests on entirely:** the free route must be real. "Obtainable in game" does a
lot of work in that sentence — a token drop rate that exists to win the argument is not a free
route, it is the violation with extra steps. If a Talon key is a realistic weekly reward for an
active player, selling Talon keys is selling convenience. If it is a one-in-ten-thousand drop, it
is not. **Whenever a key or item is put in the store, the free rate for it gets written down and
checked.**

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

**Consequences:** there is **one** set of crates, not an earned line and a store line — the same
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
in-game achievement, so nothing a paying player has is closed to a free one. That is simpler than
two ladders and it is how the perks stay honest.

The same condition applies here as to the store: **a gameplay-relevant perk on a paid rank needs a
written-down in-game route and a rate a real player reaches.** Where a perk cannot meet that bar,
it does not go on a paid rank. Purely cosmetic perks (prefix, colour, particles, pets, join
message, nickname, Discord role) carry no such condition.

As built today the paid ranks are cosmetic-only anyway — nick, chat colour, hat, item naming — not
because the rule demands it, but because EssentialsX is all we have and we have not chosen a
cosmetics plugin. That is a gap in what there is to sell, not a constraint.

**Earned keys** (Feather, Plume, Talon, Pinion, Raven) come from voting, bosses, KOTH and the Aerie,
and may contain gameplay rewards. **Store keys** are a separate cosmetic-only line. Voting is the
interesting case: it earns the server something real, and the reward is free to the player, so a
vote key with gameplay contents is fine.
