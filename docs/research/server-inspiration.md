# What popular OP factions servers actually run, and how Avian should name it

Written 2026-09-22. Purpose: take the *patterns* proven to be fun on established servers and map
them onto our spec and our bird theme. Patterns only — never their assets, configs or branding
(spec §2, rule 18).

## 1. What the big servers run

Surveyed: server lists and feature pages for PikaNetwork, JartexNetwork, CosmicPvP, OPBlocks and
the commercial "factions setup" bundles that clone them. The feature set is remarkably consistent,
which is itself the finding — these are the things players expect, and a server missing them reads
as unfinished.

| Feature | How it is usually run | In our spec? |
|---|---|---|
| **Crates** | 4–6 tiers, 140+ rewards; keys from voting, bosses, the warzone shop and the store | §19 — yes, 7 types |
| **Envoys** | **Hourly**, crates fall around spawn and warps, 3 rarity tiers | §33 — yes |
| **KOTH** | Two sites, one starting **every ~3 hours** on a schedule, so PvP is always imminent | §23 — yes |
| **Outpost** | A **24/7 capturable KOTH**; the holding faction gets a **sell buff** and other perks | **No — gap** |
| **Bosses** | Spawn at fixed warzone locations, drop crate keys and monthly-crate keys | §35 — yes |
| **Custom enchants** | 100+ enchants across ~5 tiers; the main token sink | §15–16 — yes |
| **GKits** | ~10 kits of custom-enchanted gear, usually rank- or crate-gated, on long cooldowns | §18 partially |
| **Ranks** | ~6 donor ranks + ~4 staff ranks | §17 mentions only VIP / VIP+ |
| **Seasons** | Map resets with leaderboards and payouts | §57 — yes |
| **Sell-path variety** | Shops, jobs and crates so players who avoid PvP still progress | §12–13, §30 |

### Two things worth stealing

1. **The Outpost.** A permanently contestable objective that grants an *economic* buff (sell
   multiplier) to whoever holds it, rather than a one-off reward. It gives small factions a reason
   to fight daily and makes map control pay continuously. This is the clearest gap in our spec.
2. **Event cadence as a design parameter.** KOTH every ~3 hours, envoys hourly. The point is that a
   player logging in at any time is close to *something*. Our §32 events framework should schedule
   from a single timetable so cadence is one config value, not per-event guesswork.

### One thing to be careful about

Every bundle sells "140+ crate rewards" and "100+ custom enchants" as a headline. Volume is not the
fun part; a small, legible reward table that players can reason about beats a huge random one.
Spec §22 ("avoid unrestricted combat advantages") already points this way. Start small.

## 2. Bird naming

The brand is a raven/hawk sigil, dark premium arcane, stone/gold/red/purple, at `mc.avian.club`.
Everything player-facing should read as birds without becoming twee. Proposals below; all are
config values, none are hardcoded (spec §2 rule 9).

### Ranks — two ladders

Mojang's rules forbid selling competitive advantage, and factions is the genre where that bites
hardest. So gameplay progression is free and paid ranks are cosmetic — see
`docs/adr/0006-cosmetic-only-monetisation.md`.

The two ladders are named on **different axes** so a player can tell instantly which is which:
**free ranks are life stages, paid ranks are raptor species.**

#### Free ladder — earned, and where every gameplay perk lives

| Tier | Name | Earned by |
|---|---|---|
| 1 | **Hatchling** | Joining |
| 2 | **Fledgling** | Early playtime, first quests |
| 3 | **Flier** | Sustained play |
| 4 | **Soarer** | Significant achievement |
| 5 | **Skyborne** | Long-term mastery |

Perks: sell-multiplier steps, kit access, home slots, claim bonuses.

#### Paid ladder — cosmetic and social only

| Tier | Name | Why |
|---|---|---|
| 1 | **Kestrel** | Small falcon, common, unmistakably a bird of prey |
| 2 | **Osprey** | Bigger, a specialist hunter |
| 3 | **Falcon** | The fast one; widely recognised as elite |
| 4 | **Hawk** | Half the sigil |
| 5 | **Raven** | The other half, and the apex — the server's own emblem |

Perks: prefix, chat colour, particles, pets, join message, nickname, Discord role. **Nothing that
affects who wins.** The two ladders stack, because they are orthogonal.

Optional sixth paid tier, **Phoenix**, if a mythic rank above Raven is ever wanted. Held back for
now so Raven stays the summit and the theme stays real birds.

### Staff ranks — deliberately not bird-themed

**Helper, Mod, Admin, Owner.** Players must identify staff instantly, in chat, under pressure, often
mid-raid. A themed staff title is a small aesthetic gain for a real moderation cost. The one
concession: staff prefixes use the brand's gold/purple rather than the usual generic colours.

### Crates — mapped onto spec §19's types

| Spec type | Avian name | Key source |
|---|---|---|
| Vote | **Feather** | Voting |
| Common | **Plume** | Envoys, warzone shop |
| Rare | **Talon** | Bosses, KOTH |
| Legendary | **Pinion** | Outpost holds, store |
| God | **Raven** | Rare drops, store |
| Seasonal | **Phoenix** | Season rollover — rebirth, which is exactly what a season reset is |
| Event | **Flock** | Event participation |

Keys take the crate's name (`Talon Key`). Spec §19's four key types become one per crate tier.

**Crates split by how the key is obtained, not by rarity alone** (ADR-0006). An **earned** key —
voting, bosses, KOTH, the Aerie — may contain anything, including gameplay rewards. A **purchased**
key may contain cosmetics only; paid crate keys with gameplay-relevant loot are named explicitly in
the guidance as a violation. Voting is the interesting case and it is fine: it earns the server
something real and costs the player nothing, so a Feather key can hold gameplay rewards.

### Events and places

| Thing | Avian name | Note |
|---|---|---|
| Outpost | **The Aerie** | An aerie is a bird-of-prey nest on a high crag — exactly an outpost |
| Envoy | **Migration** | A flock arriving; fits crates descending on spawn |
| Airdrop | **Drop** | Keep plain; it is already understood |
| KOTH | **KOTH** | Universally understood; name the *sites* after birds instead |
| KOTH sites | Roost, Crag, Eyrie, Spire | Warzone landmarks |
| World boss | **The Roc** | The mythical giant bird; a boss deserves the myth |

Keep **KOTH** and **Drop** unthemed on purpose: a returning factions player should not have to learn
new words for mechanics they already know. Theme the *proper nouns*, not the mechanics.

## 3. What this implies for the build order

- **The Aerie needs a spec section.** It is not in `docs/SPEC.md` and it is the strongest idea the
  survey turned up. Filed separately.
- **Ranks are two LuckPerms tracks, not code.** A free track (Hatchling→Skyborne) carrying the
  gameplay permissions and a paid track (Kestrel→Raven) carrying cosmetics, both tracked in
  `dev-server/`. The free track is the one that needs design effort — it is the real progression.
- **Crates are probably bought, not built.** Spec §19's requirements (weighted rewards, animations,
  sounds, particles, key consumption) describe every crate plugin on the market. Evaluate before
  writing one — the same call we made for shops and stacking.
- **Events wants one scheduler.** Spec §32's `EventManager` should own a single timetable that KOTH,
  Migration, Drop and boss spawns all hang off, so cadence is tuned in one place.

## Sources

- [Best Factions Minecraft Servers 2026 — Minecraft Server List](https://minecraft-serverlist.com/minecraft-factions-servers)
- [Best Factions Servers (2026) — Minehut](https://www.minehut.com/sl/best-factions-minecraft-servers)
- [Best Minecraft Factions Servers in 2026 — McServ](https://mcserv.org/blog/best-minecraft-factions-servers-2026)
- [Top Minecraft Factions Servers in 2026 — anyserver.pro](https://anyserver.pro/blog/best-factions-servers)
- [PikaNetwork — ranks](https://pika-network.net/tags/rank/) and [vote crates](https://pika-network.net/tags/vote-crates/)
- [JartexNetwork — supply crate ranks](https://jartexnetwork.com/threads/supply-crate-ranks.181504/)
- [OPBlocks OP Factions release notes](https://opblocks.com/threads/opblocks-op-factions-release.28689/)
- [Nyx OP Factions feature list](https://www.minecraftforum.net/forums/servers-java-edition/pc-servers/2947699-nyx-op-factions-mcmmo-custom-enchants-quests)
- Commercial factions setup feature lists (envoys / bosses / crates / outpost / KOTH), e.g. [BuiltByBit listing](https://builtbybit.com/resources/hq-factions-setup-75-off-sale-envoys-bosses-crates-outpost-koth-coinflip.9151/) — read for feature inventory only

## Sources — monetisation rules

- [Minecraft Usage Guidelines for Fans and Creators](https://www.minecraft.net/en-us/usage-guidelines)
- [Commercial usage guidelines updated — Minecraft.net](https://www.minecraft.net/en-us/article/commercial-usage-guidelines-updated)
- [Minecraft EULA and Commercial Usage Guidelines Updates — Minecraft.net](https://www.minecraft.net/en-us/article/minecraft-eula-and-commercial-usage-guidelines-updates)
- [Server ranks, stores and pay-to-win — minecraftservers wiki](https://minecraftserve.rs/wiki/ranks-and-pay-to-win) — the concrete list of unsafe perks
- [Can You Charge for a Minecraft Server? EULA and Monetization Rules (2026)](https://space-node.net/blog/minecraft-eula-monetization-rules-2026)
- [How to Monetize Your Minecraft Server (Legally and Effectively) — GGServers](https://ggservers.com/blog/how-to-monetize-your-minecraft-server-legally-and-effectively/)
