# Classic 1.8-style PvP feel on modern Paper

Research for GitHub issue #5. Covers SPEC §43 (Combat) and §44 (Knockback).
Researched 2026-09-16 against Paper `main` (javadoc line `paper-api 26.3.build.8-alpha`),
the Paper 1.21.8 javadocs, docs.papermc.io, minecraft.wiki, and plugin sources on GitHub,
then re-verified against the project's pinned target **Paper 26.1.2 build 74**
(`io.papermc.paper:paper-api:26.1.2.build.74-stable`, Java 25) — see §7.1.
Every claim below carries a source link; anything not verifiable is flagged as such.

## Recommended approach (summary)

**Config-only is not enough. One small plugin listener module ("combat") is required.**
Paper ships *no* config key that disables the 1.9 attack cooldown or that changes melee
knockback strength — verified by reading the full global, world-defaults and spigot.yml
references ([global][cfg-global], [world][cfg-world], [spigot][cfg-spigot]). What Paper does
ship is a first-class, mutable, cancellable knockback event, so the plugin is small.

1. **No attack cooldown** – set the player's `minecraft:attack_speed` attribute base
   value high (OldCombatMechanics uses **40**; vanilla is 4, hard max 1024). Apply on
   `PlayerJoinEvent`, `PlayerChangedWorldEvent` and (belt-and-braces) `PlayerRespawnEvent`
   via `player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(v)`. The base value is
   saved in player NBT and copied to the new player entity on respawn, so it persists,
   but re-applying is idempotent and protects against admins/other plugins resetting it.
2. **Knockback** – listen to
   `com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent` (which extends
   the current, non-deprecated `io.papermc.paper.event.entity.EntityKnockbackEvent`).
   Filter `getCause() == ENTITY_ATTACK` and `getHitBy() instanceof Player`, then
   `setKnockback(Vector)` with a vector computed from `knockback.horizontal`,
   `knockback.vertical`, `knockback.sprint-multiplier` and the Knockback enchantment
   level. The vector is a **delta added to the victim's current velocity**, and Paper
   fires the event **twice per sprint/enchanted hit** (base 0.4 from `hurt()`, bonus from
   `Player.attack()`), so the listener must handle both calls (details in §2).
   Do **not** use `PlayerVelocityEvent` (§2.5).
3. **Paper config** – keep `entities.behavior.disable-player-crits: false` (1.8 had
   crits), keep `misc.disable-sprint-interruption-on-attack: false` (1.8 also reset
   sprint), and leave `unsupported-settings.skip-vanilla-damage-tick-when-shield-blocked`
   at default. Nothing else in the config files affects melee feel.
4. **Sweep attacks** – with no cooldown every hit is "full strength", so sword sweeps
   fire constantly. Cancel `EntityDamageByEntityEvent` whose cause is
   `ENTITY_SWEEP_ATTACK`; Paper then also skips the sweep knockback.
5. **Reach / hit registration** – the server validates hits with the
   `entity_interaction_range` attribute (default 3) and, since 1.21.11, the item
   `minecraft:attack_range` component (`max_reach` 3.0, `hitbox_margin` 0.3). Both are
   settable via the Paper API without ProtocolLib. Ping compensation ("knockback sync")
   needs packet-level access (PacketEvents/ProtocolLib); note only.
6. **Pearls / gapples / potions** – `HumanEntity.setCooldown(Material|ItemStack|Key, ticks)`
   plus Paper's cancellable `PlayerItemCooldownEvent` (to remove/replace the vanilla
   1-second pearl cooldown), `PlayerLaunchProjectileEvent` (pearl throw, cancellable,
   `setShouldConsume`), `PlayerItemConsumeEvent` (gapples/potions, cancellable).

The rest of this document gives the exact keys and signatures.

---

## 1. Disabling the 1.9 attack cooldown

### 1.1 What the cooldown is

Vanilla Java Edition scales melee damage by charge:
`0.2 + ((t + 0.5) / T)^2 * 0.8` where `t` = ticks since last attack and
`T = 20 / attack_speed`, clamped to 0.2–1.0. Critical hits, sweep attacks and sprint
knockback attacks require ≥ 84.8% charge ([wiki: Damage][wiki-damage]). Weapons carry
`attack_speed` modifiers: sword 1.6, trident 1.1, pickaxe 1.2, shovel 1.0, axes 0.8–1.0,
mace 0.6 ([wiki: Damage][wiki-damage]).

In Paper's server source the charge is `getAttackStrengthScale(0.5F)`, a hit is "full
strength" when `attackStrengthScale > 0.9F`, and crits require `fullStrengthAttack`
([Paper Player.java.patch][paper-player-patch]).

### 1.2 Attribute key and value

| Item | Value | Source |
|---|---|---|
| Vanilla attribute id | `minecraft:attack_speed` — "number of full-strength attacks per second. Only players have this attribute." Default 4, min 0, max 1024 | [wiki: Attribute][wiki-attr] |
| Bukkit constant | `org.bukkit.attribute.Attribute.ATTACK_SPEED` (`getAttribute("attack_speed")`). `Attribute` is an interface with static fields, not an enum; `valueOf()`/`values()` are `@Deprecated(since="1.21.3", forRemoval=true)` | [Paper Attribute.java][paper-attr-src], [jd Attribute][jd-attr] |
| Value to use | OldCombatMechanics: "Default for 1.9 is 4, at least 40 is needed for no cooldown." Its default is `generic-attack-speed: 40` | [OCM config.yml][ocm-config] |

Why 40 works: with `attack_speed = 40`, `T = 20/40 = 0.5` ticks, so
`(0 + 0.5) / 0.5 = 1.0` — full charge on the very tick after the previous hit.
Anything from 40 up to the cap of 1024 behaves identically for players; 1024 is a fine
"absolutely never" value, 40 is the conventional one. Because `attack_speed` is a synced
attribute, the client's crosshair cooldown indicator disappears too (no client mod).

### 1.3 How to apply

```java
AttributeInstance inst = player.getAttribute(Attribute.ATTACK_SPEED);
if (inst != null && inst.getBaseValue() != value) inst.setBaseValue(value);
```

Relevant API ([jd AttributeInstance][jd-attrinst], [jd AttributeModifier][jd-attrmod]):

- `double getBaseValue()` / `void setBaseValue(double)` — "Base value of this instance before modifiers are applied".
- `void addModifier(AttributeModifier)` (persisted) vs `void addTransientModifier(AttributeModifier)` — "Transient modifiers are not persisted".
- `AttributeModifier(NamespacedKey key, double amount, Operation op, EquipmentSlotGroup slot)` — UUID-based constructors are deprecated; "attributes are now identified by keys".
- Modifier operations (vanilla): `add_value`, `add_multiplied_base`, `add_multiplied_total`, applied in that order ([wiki: Attribute][wiki-attr]).

**Base value vs modifier.** Set the *base value* (OCM's approach). Weapon items add
their own `add_value` modifiers (e.g. sword −2.4 on top of base 4 → 1.6); a large base
swamps them. A modifier with our own `NamespacedKey` also works and is easy to remove,
but `add_multiplied_*` interacts with the weapon modifiers and base-only is simpler.
OCM additionally offers `held-item-attack-speeds` per material (trident 1.1, mace 0.6,
spears ~0.87–1.54) so that weapons designed around the cooldown keep theirs; it re-applies
on `PlayerItemHeldEvent` and `PlayerSwapHandItemsEvent` ([OCM ModuleAttackCooldown][ocm-cooldown]).
We only need this if we allow maces/spears on the server.

**When to apply.** OCM applies on `PlayerJoinEvent` (priority HIGH),
`PlayerChangedWorldEvent`, hotbar/hand-swap events, and resets to 4.0 on
`PlayerQuitEvent` ([OCM ModuleAttackCooldown][ocm-cooldown]). For Avian Factions,
`PlayerJoinEvent` + `PlayerRespawnEvent` + `PlayerChangedWorldEvent` is sufficient;
resetting on quit is optional (it keeps player data clean if the plugin is removed).

### 1.4 Does it persist?

- **Across relog:** yes. Entity/player NBT stores an `attributes` list with `id`,
  `base` and `modifiers` per attribute ([wiki: Entity format][wiki-entity]).
- **Across respawn:** yes. Paper's `ServerPlayer.restoreFrom` unconditionally calls
  `this.getAttributes().assignBaseValues(oldPlayer.getAttributes())`; only
  `assignPermanentModifiers` is commented out by CraftBukkit ([Paper ServerPlayer.java.patch][paper-serverplayer-patch]).
  So base values survive death; persisted *modifiers* do not (transient ones never do).
- Re-applying on join/respawn is still recommended: it is idempotent (compare
  `getBaseValue()` first), and it covers `/attribute` commands or other plugins.

**No config alternative exists.** Neither `paper-global.yml`, `paper-world-defaults.yml`
nor `spigot.yml` has an attack-speed/cooldown key ([cfg-global], [cfg-world],
[cfg-spigot]). A datapack could run `attribute @a minecraft:attack_speed base set 40`
every tick, but that is a hack with no per-world/per-player control; the plugin path
is the right one.

### 1.5 Side effects

| Mechanic | Effect of no cooldown | Handling |
|---|---|---|
| **Sweep attack** | Every hit is full strength, so sword hits on a grounded, non-sprinting player sweep constantly (extra damage + knockback to nearby entities; Paper `Cause.SWEEP_ATTACK`, knockback power 0.4 – [Paper Player.java.patch][paper-player-patch]). | Cancel `EntityDamageByEntityEvent` with `getCause() == DamageCause.ENTITY_SWEEP_ATTACK` ("Damage caused when an entity attacks another entity in a sweep attack" – [jd DamageCause][jd-damagecause]). Paper only applies sweep knockback when that damage event is not cancelled ("Only apply knockback if the event is not canceled" – [Paper Player.java.patch][paper-player-patch]). The `sweeping_damage_ratio` attribute (default 0) only scales the *extra* damage, it does not remove the sweep. |
| **Critical hits** | Crits require full charge, so they now trigger on every falling hit — this is 1.8 behaviour and desirable. | Keep. Paper offers `entities.behavior.disable-player-crits: false` ("disable critical hits in PvP, treating them as normal hits" – [cfg-world]) if ever needed. `EntityDamageByEntityEvent.isCritical()` exposes crit status to plugins ([jd EDBEE][jd-edbee]). |
| **Shield** | Unrelated to attack speed; shields still block. Shield-block knockback fires with `Cause.SHIELD_BLOCK` (power 0.5 – [Paper LivingEntity.java.patch][paper-living-patch]). | For 1.8 feel either ban shields in kits/crafting or cancel their use; optional. `unsupported-settings.skip-vanilla-damage-tick-when-shield-blocked` (default `false`) is unrelated to feel. |
| **Sprint reset** | Vanilla resets the attacker's sprint after a knockback hit (`setSprinting(false)`); 1.8 did the same (w-tapping). | Leave `misc.disable-sprint-interruption-on-attack: false` ([cfg-world]). |
| **Hit delay (i-frames)** | Unchanged: 10-tick invulnerability, extra damage in the window applies only the difference ([wiki: Damage][wiki-damage]). Competitive servers often lower it. | `LivingEntity.setMaximumNoDamageTicks(int)` / `setNoDamageTicks(int)` ([jd LivingEntity][jd-living]). OCM's `attack-frequency` module defaults to `playerDelay: 18`, `mobDelay: 16` ([OCM config.yml][ocm-config]). Make it a config value (`combat.hit-delay-ticks`). |
| `PlayerAttackEntityCooldownResetEvent` | Paper event to veto the ticker reset. **Deprecated for removal since 26.1** ("does not properly represent spear attacks…" – [jd PAECRE][jd-paecre]). | Do not use. |

---

## 2. Knockback control

### 2.1 Which event is current

Class hierarchy in Paper `main` ([EntityKnockbackEvent.java][paper-kb-src],
[EntityPushedByEntityAttackEvent.java][paper-pushed-src],
[EntityKnockbackByEntityEvent.java][paper-kbbe-src]):

```
io.papermc.paper.event.entity.EntityKnockbackEvent            (extends EntityEvent, Cancellable)
 └─ io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent
     └─ com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
```

- **Current, not deprecated:** all three classes. The 26.3 javadoc marks neither
  `EntityKnockbackEvent` nor any of its methods deprecated ([jd EntityKnockbackEvent][jd-kb]).
  `EntityKnockbackByEntityEvent` (the old `com.destroystokyo` package) is *not* deprecated
  as a class; it was re-parented onto the new base in 1.20.6.
- **Deprecated members:** `EntityPushedByEntityAttackEvent.getAcceleration()` /
  `setAcceleration(Vector)` are `@Deprecated(since = "1.20.6", forRemoval = true)` — use
  `getKnockback()`/`setKnockback()`. `EntityKnockbackByEntityEvent.getKnockbackStrength()`
  is `@ApiStatus.Obsolete(since = "1.20.6")`: "this value doesn't necessarily relate to
  getKnockback()" ([paper-kbbe-src]).

Exact API:

```java
// EntityKnockbackEvent
public EntityKnockbackEvent.Cause getCause();
public Vector getKnockback();            // returns a clone: "read-only, changes made to it will not have any effect"
public void setKnockback(Vector knockback);
boolean isCancelled(); void setCancelled(boolean);
enum Cause { DAMAGE, ENTITY_ATTACK, EXPLOSION, SHIELD_BLOCK, SWEEP_ATTACK, PUSH, UNKNOWN }

// EntityPushedByEntityAttackEvent
public Entity getPushedBy();
// "Note: Some entities might trigger this multiple times on the same entity
//  as multiple acceleration calculations are done."

// EntityKnockbackByEntityEvent
@Override public LivingEntity getEntity();   // the victim
public Entity getHitBy();                    // same as getPushedBy()
@ApiStatus.Obsolete public float getKnockbackStrength();  // the vanilla `power` after knockback-resistance
```

Listen to `EntityKnockbackByEntityEvent` (victim is guaranteed `LivingEntity`, attacker
available). Bukkit routes subclass events to a superclass handler list and filters by
class, so registering on the subclass receives only that subclass.

### 2.2 What the vector means (server-side math)

From Paper's `LivingEntity.knockback` ([Paper LivingEntity.java.patch][paper-living-patch]),
verbatim structure:

```
power *= 1.0 - KNOCKBACK_RESISTANCE                      // resistance applied BEFORE the event
deltaVector   = normalize(xd, 0, zd) * power
targetMovement = ( dm.x/2 - deltaVector.x,
                   onGround ? min(0.4, dm.y/2 + power) : dm.y,
                   dm.z/2 - deltaVector.z )               // dm = current velocity
knockback = targetMovement - dm                           // <-- this is event.getKnockback()
event = callEntityKnockbackEvent(victim, attacker, cause, power, knockback)
if cancelled: return                                      // needsSync not set, nothing applied
setDeltaMovement(dm + event.getKnockback())
```

Consequences for our implementation:

1. `getKnockback()` is a **delta added to the victim's current velocity**, and it already
   contains vanilla's "halve existing velocity" friction (`dm/2 - dm = -dm/2`). Naively
   multiplying X/Z by a factor also scales that friction. To get a clean, configurable
   1.8 formula, recompute the *target* velocity from `victim.getVelocity()` and set
   `knockback = target - current`.
2. **Knockback resistance is baked in before the event** (`power *= 1 - resistance`).
   CraftBukkit changed the guard to `if (true || !(power <= 0.0))` — "Call event even
   when force is 0" — so a netherite-armoured victim with resistance still produces an
   event whose vector is friction-only. The plugin can therefore ignore or re-apply
   resistance itself by reading `victim.getAttribute(Attribute.KNOCKBACK_RESISTANCE).getValue()`
   (vanilla: "proportion of horizontal knockback … resisted. A value of 1 eliminates the
   knockback", range −2..1 – [wiki: Attribute][wiki-attr]). This is how to give
   Custom/Competitive presets an "ignore netherite KB resistance" switch without
   stripping modifiers the way OCM/KohiKB do.
3. **Two events per boosted hit.** A player melee hit produces:
   - call 1 from `LivingEntity.hurt`: `knockback(0.4F, xd, zd, …, source.getDirectEntity(), ENTITY_ATTACK)`
     — direction is victim-minus-attacker position, power 0.4
     ([paper-living-patch]);
   - call 2 from `Player.attack`, only if `knockbackAmount > 0`
     (Knockback enchantment level, `attack_knockback` attribute, and the sprint bonus):
     `livingTarget.knockback(knockbackAmount, sin(yaw), -cos(yaw), …, this, ENTITY_ATTACK)`
     — direction is the attacker's **yaw**, not position ([paper-player-patch]).
     After it, vanilla does `attacker.setDeltaMovement(dm * (0.6, 1, 0.6))` and
     `setSprinting(false)`. Because `setSprinting(false)` happens *after* the knockback
     call, `((Player) getHitBy()).isSprinting()` is still `true` inside the event.
   The exact scalar applied to the sprint/enchant bonus lives in vanilla code that is not
   in Paper's patch (not verified here; in older versions it was `level * 0.5`). Read it
   at runtime from `getKnockbackStrength()` rather than hard-coding.
4. Projectiles arrive through the same `hurt()` path with `getDirectEntity()` = the
   projectile, so `getCause()` is `ENTITY_ATTACK` and **`getHitBy()` is the Arrow /
   Snowball / etc.**, not the shooter. Check `getHitBy() instanceof Player` for melee and
   `instanceof Projectile` + `getShooter()` for ranged.
5. Sweep hits come with `Cause.SWEEP_ATTACK` (power 0.4); shield blocks with
   `Cause.SHIELD_BLOCK` (power 0.5, applied to the *attacker*); explosions with
   `Cause.EXPLOSION`; everything else `DAMAGE`/`PUSH`/`UNKNOWN`.

### 2.3 Mapping the SPEC config onto the event

SPEC §44 example:

```yaml
knockback:
  horizontal: 0.42
  vertical: 0.36
  sprint-multiplier: 1.15
```

Vanilla 1.8 reference values (the 1.8 formula, as reconstructed by OCM and KohiKB, both
verified from source): base horizontal **0.4**, base vertical **0.4** with a vertical
**limit 0.4**, and per bonus level (each Knockback enchant level, +1 if sprinting) an extra
**0.5** horizontal along attacker yaw and **0.1** vertical ([OCM ModulePlayerKnockback][ocm-kb],
[OCM config.yml][ocm-config], [KohiKB LegacyKB.java][kohi-src]). OCM's comments also give
a "practice server" preset: 0.35 / 0.35 / limit 0.4 / extra 0.425 / extra 0.085.

Suggested listener (pattern, not code):

```
on EntityKnockbackByEntityEvent (priority NORMAL, ignoreCancelled):
  if cause != ENTITY_ATTACK or !(hitBy instanceof Player attacker) or !(entity instanceof Player victim): return
  key = (victim, attacker, currentTick)
  if first event for key:                       // call 1: base knockback
      cur = victim.getVelocity()
      dir = horizontal unit vector from attacker.pos to victim.pos (vanilla's fallback: random tiny vector if < 1e-4)
      h = cfg.horizontal; v = cfg.vertical
      bonus = enchantLevel(attacker main hand, KNOCKBACK) + (attacker.isSprinting() ? 1 : 0)
      target.x = cur.x/2 + dir.x * h * (attacker.isSprinting() ? cfg.sprintMultiplier : 1)
      target.z = cur.z/2 + dir.z * h * (…)
      target.y = min(cur.y/2 + v, cfg.verticalLimit)
      if bonus > 0: target += (−sin(yaw)*bonus*cfg.extraHorizontal, cfg.extraVertical, cos(yaw)*bonus*cfg.extraHorizontal)
      if cfg.respectKnockbackResistance: scale target.x/z by (1 − resistance)   // otherwise resistance is ignored, since we recompute
      event.setKnockback(target − cur)
  else:                                          // call 2: vanilla sprint/enchant bonus
      event.setKnockback(new Vector(0, 0, 0))    // we folded the bonus into call 1
```

Presets then become named value sets: **Classic** = 1.8 numbers above,
**Competitive** = OCM's practice numbers, **Custom** = whatever is in `combat.yml`.
Note that SPEC's `sprint-multiplier` is a *multiplicative* model whereas 1.8 is
*additive* (+0.5/+0.1 per level). Either is implementable; the additive `extra-horizontal`
/ `extra-vertical` pair is what every existing 1.8-KB plugin exposes, so consider adding
those two keys and keeping `sprint-multiplier` as a Custom-preset knob. Decision needed
before implementation (§8).

Zeroing the second event rather than cancelling it is deliberate: `setCancelled(true)`
returns before `needsSync = true`, which is harmless here because call 1 already set it,
but a zero vector is the least surprising for other listeners at MONITOR.

### 2.4 Interaction with Knockback enchantment and resistance

- **Knockback enchantment** only affects call 2 (its level is part of `knockbackAmount`),
  so a listener that recomputes call 1 and zeroes call 2 must read the enchant level
  itself (`ItemStack.getEnchantmentLevel(Enchantment.KNOCKBACK)`); wiki summary of the
  vanilla effect: horizontal boost per level, vertical raised "regardless of the level",
  stacks with sprint ([wiki: Knockback enchantment][wiki-kbench]).
- **`attack_knockback` attribute** (default 0, max 5, "additional knockback applied to
  an entity's melee attack" – [wiki-attr]) also feeds call 2. Players have it at 0 unless
  a plugin sets it.
- **`knockback_resistance`** is applied before the event (see §2.2). OCM and KohiKB both
  strip the victim's resistance *modifiers* on damage ("the knockback resistance attribute
  makes the velocity event not be called") because they work through `PlayerVelocityEvent`;
  with the Paper event that hack is unnecessary.
- **`explosion_knockback_resistance`** is a separate attribute (Paper
  `Attribute.EXPLOSION_KNOCKBACK_RESISTANCE`) and only matters for `Cause.EXPLOSION`.

### 2.5 Why `PlayerVelocityEvent` is the wrong tool

`org.bukkit.event.player.PlayerVelocityEvent` — "Called when the velocity of a player
changes"; `getVelocity()` "Gets the velocity vector that will be sent to the player"
([jd PlayerVelocityEvent][jd-pve]). Paper fires it from `ServerEntity.sendChanges` when
`entity.hurtMarked` is set, immediately before `ClientboundSetEntityMotionPacket`
([Paper ServerEntity.java.patch][paper-serverentity-patch]). That means:

1. It fires once per tracker tick for **any** velocity change (knockback, explosions,
   fishing rods, `setVelocity` from other plugins, water push) with **no cause** and no
   attacker; you must correlate it with a preceding `EntityDamageByEntityEvent` yourself
   (OCM and KohiKB keep a per-victim map that they clear every tick for exactly this
   reason – [ocm-kb], [kohi-src]).
2. It carries the **whole** velocity, so friction, the base hit and the sprint bonus are
   already merged; you can only overwrite, not adjust.
3. It is only for **players** (mobs never fire it), so PvE knockback is untouchable.
4. Cancelling suppresses the motion packet but the server-side velocity is already set —
   client/server desync.
5. Historically it was skipped entirely when knockback resistance zeroed the force
   (OCM/KohiKB source comments).

`EntityKnockbackEvent` has none of these problems: it fires at the point of calculation,
carries the cause, the attacker and the pre-resistance power, and its vector is applied
by the server after the event. OCM still uses `PlayerVelocityEvent` because it must run
on Spigot; Avian Factions targets Paper.

---

## 3. Paper / Spigot config keys that touch combat

Verified against the current references. Keys not listed do not exist (there is no
`knockback`, `attack-speed`, `reach`, `hit-delay` or `fix-entity-position-desync` key in
any of the three files as documented today).

### `paper-world-defaults.yml` ([cfg-world])

| Key | Default | Doc text |
|---|---|---|
| `entities.behavior.disable-player-crits` | `false` | "Instructs the server to disable critical hits in PvP, treating them as normal hits instead" |
| `misc.disable-sprint-interruption-on-attack` | `false` | "Determines if the server will interrupt a sprinting player if they are attacked" (source: `paperConfig().misc.disableSprintInterruptionOnAttack` guards `setSprinting(false)` in `Player.attack` – [paper-player-patch]) |
| `misc.disable-relative-projectile-velocity` | `false` | "Instructs the server to ignore shooter velocity when calculating the velocity of a fired arrow" |
| `misc.legacy-ender-pearl-behavior` | `false` | "Ender pearls will no longer load chunks and will be saved with the launching player" |
| `fixes.disable-unloaded-chunk-enderpearl-exploit` | `false` | "Prevent enderpearls from storing the thrower when in an unloaded chunk" |
| `collisions.only-players-collide` | `false` | "Only calculate collisions if a player is one of the two entities colliding" |
| `collisions.max-entity-collisions` | `8` | "stop processing collisions after this value is reached" |
| `collisions.allow-player-cramming-damage` | `false` | cramming damage for players |

### `paper-global.yml` ([cfg-global])

| Key | Default | Doc text |
|---|---|---|
| `unsupported-settings.skip-vanilla-damage-tick-when-shield-blocked` | `false` | "Whether the server should skip damage ticks when entities are blocking damage via a shield" |
| `unsupported-settings.update-equipment-on-player-actions` | `true` | "controls if equipment should be updated when handling certain player actions" (source: when true, switching main-hand item resets the attack ticker in `detectEquipmentUpdates` – [paper-player-patch]; irrelevant once the cooldown is gone) |
| `unsupported-settings.allow-permanent-block-break-exploits` / `allow-piston-duplication` / `allow-headless-pistons` / `allow-unsafe-end-portal-teleportation` / `skip-tripwire-hook-placement-validation` | `false` | Exploit toggles — keep `false` (SPEC §46). |
| `unsupported-settings.perform-username-validation` | `true` | keep |
| `collisions.send-full-pos-for-hard-colliding-entities` | `true` | boats/minecarts desync mitigation |
| `packet-limiter.*` | — | rate limiting (DROP/KICK) — relevant to anti-cheat, not feel |

### `spigot.yml` ([cfg-spigot])

The reference explicitly contains **no knockback settings**. Relevant keys:

| Key | Default | Doc text |
|---|---|---|
| `settings.attribute.maxHealth.max` | `1024.0` | attribute caps (`movementSpeed.max` 1024, `attackDamage.max` 2048, `maxAbsorption.max` 2048). No `attackSpeed` cap key — the vanilla max 1024 applies. |
| `settings.moved-too-quickly-multiplier` | `10.0` | "Controls how fast a client can move in one packet. If triggered, the server logs to console and prevents the move." Matters for high-KB presets + pearls (rubber-banding). |
| `settings.moved-wrongly-threshold` | `0.0625` | "how far the client can move per move-packet … in blocks squared" |
| `world-settings.default.entity-tracking-range.players` | `128` | how far players are sent to players |
| `world-settings.default.hunger.combat-exhaustion` | `0.1` | "How much exhaustion to give from attacking" (source: `causeFoodExhaustion(spigotConfig.combatExhaustion, …)` – [paper-player-patch]) |

---

## 4. Hit registration and reach

What the server checks, and what Paper exposes:

| Knob | API | Notes |
|---|---|---|
| Player entity reach | attribute `entity_interaction_range` — "determines the entity interaction range for players", default 3, max 64 ([wiki-attr]); Bukkit `Attribute.ENTITY_INTERACTION_RANGE` ([paper-attr-src]) | Per-player, persisted like any attribute. `block_interaction_range` (4.5) is the block equivalent. |
| Per-item reach + hitbox margin (1.21.11+) | item data component `minecraft:attack_range`: `min_reach` 0.0, `max_reach` 3.0, `min_creative_reach` 0.0, `max_creative_reach` 5.0, `hitbox_margin` 0.3, `mob_factor` 1.0 ([wiki: attack_range][wiki-attackrange]; added in 1.21.11, released 2025-12-09 – [wiki 1.21.11][wiki-12111]). Paper: `DataComponentTypes.ATTACK_RANGE` is `DataComponentType.Valued<AttackRange>` ([jd DataComponentTypes][jd-dct]). | OCM's `attack-range` module ("Paper 1.21.11+ only") applies 1.8-style values `max-range: 3.0`, `hitbox-margin: 0.1`, `max-creative-range: 4.0` to held weapons and strips the override when the item leaves the hand ([OCM ModuleAttackRange][ocm-range], [ocm-config]). |
| i-frames | `LivingEntity.setMaximumNoDamageTicks(int)` ([jd-living]) | see §1.5 |
| Movement sanity | `spigot.yml settings.moved-too-quickly-multiplier`, `moved-wrongly-threshold` | see §3 |

**Needs ProtocolLib / PacketEvents (note only):** anything that reads the client's
packet order or timing — e.g. KohiKB's stated purpose ("Fixes a bug with packet order of
movement VS attack packets" – [kohi-repo]) and KnockbackSync's ping-compensated vertical
knockback (depends on `packetevents-spigot` – [kbsync-pom]). The remaining
lag-compensation problem (client hits what it saw N ms ago) is a packet-level concern
and out of scope for v0.1. Reliable hit registration on Paper otherwise comes down to
TPS, tracking range, and the reach/hitbox values above.

---

## 5. Ender pearls, golden apples, potions

| Need | API | Source |
|---|---|---|
| Set / read an item cooldown | `HumanEntity`: `void setCooldown(Material, int ticks)`, `void setCooldown(ItemStack, int)`, `void setCooldown(Key cooldownGroup, int)`, `int getCooldown(...)`, `boolean hasCooldown(...)` — none deprecated | [jd HumanEntity][jd-human] |
| Intercept vanilla cooldowns (pearl 1 s, etc.) | `io.papermc.paper.event.player.PlayerItemCooldownEvent` (extends `PlayerItemGroupCooldownEvent`): "Fired when a player receives an item cooldown when using an item"; `Material getType()`, `NamespacedKey getCooldownGroup()`, `int getCooldown()`, `void setCooldown(int)` (≥ 0), cancellable | [Paper PlayerItemCooldownEvent.java][paper-cooldown-src], [PlayerItemGroupCooldownEvent.java][paper-groupcooldown-src] |
| Pearl throw | `com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent` — "Called when a player shoots a projectile… not called for arrows"; `getProjectile()`, `getItemStack()`, `shouldConsume()/setShouldConsume(boolean)`, cancellable. Paper fires it from `EnderpearlItem.use` before spawning the pearl and, when cancelled, sends a `ClientboundCooldownPacket(…, 0)` "to prevent visual desync of cooldown on the slot" | [jd PLPE][jd-plpe], [Paper EnderpearlItem.java.patch][paper-pearl-patch] |
| Generic projectile launch | `org.bukkit.event.entity.ProjectileLaunchEvent` (all projectiles, cancellable) | Bukkit API |
| Pearl landing / teleport | `ProjectileHitEvent`; `PlayerTeleportEvent` with `TeleportCause.ENDER_PEARL`; damage is `DamageType.ENDER_PEARL` (5 HP – [wiki: Ender Pearl][wiki-pearl]; `DamageType.ENDER_PEARL` exists – [jd DamageType][jd-damagetype]) | |
| Gapple / potion consumption | `PlayerItemConsumeEvent` (cancellable, `getItem()`, `setItem()`); combine with `setCooldown(Material.GOLDEN_APPLE, ticks)` / `ENCHANTED_GOLDEN_APPLE` | Bukkit API; OCM `old-golden-apples.cooldown.normal/enchanted` pattern ([ocm-config]) |
| Vanilla pearl cooldown facts | "Ender pearls have a cooldown of one second (20 ticks)"; cooldown introduced 15w34c (1.9). Since 1.21.2 pearls "load and tick chunks around them in a 3x3 chunk area" — disable with `misc.legacy-ender-pearl-behavior: true` if that is unwanted on a factions map | [wiki-pearl], [wiki 1.21.2][wiki-1212], [cfg-world] |

Design note: to implement a configurable pearl cooldown (SPEC §43), override the
vanilla 20-tick `PlayerItemCooldownEvent` for `Material.ENDER_PEARL` with
`event.setCooldown(cfg.pearlCooldownTicks)`, which keeps the vanilla HUD indicator in
sync for free; deny throws while `hasCooldown(Material.ENDER_PEARL)` in
`PlayerLaunchProjectileEvent` as a safety net (Paper resets the slot's cooldown display
when that event is cancelled). The same pattern (cooldown event + consume-event guard)
covers golden apples and potions; a `use_cooldown` data component (`seconds`, optional
`cooldown_group` – [wiki: use_cooldown][wiki-usecooldown]) is the item-side alternative
for kit items.

---

## 6. What existing open-source plugins do (patterns only)

| Plugin | License | Approach | Takeaway |
|---|---|---|---|
| **OldCombatMechanics** (kernitus/BukkitOldCombatMechanics; pushed 2026-09-12) | MPL-2.0 ([ocm-repo]) | Modular. `disable-attack-cooldown`: `attack_speed` base value 40 on join/world-change/held-item change, reset to 4 on quit, per-material overrides. `old-player-knockback`: `EntityDamageByEntityEvent` (MONITOR) computes the 1.8 vector, stores it per victim, `PlayerVelocityEvent` (LOWEST) overwrites; strips `knockback_resistance` modifiers; entries expire after 1 tick. `attack-range`: `attack_range` component on held weapons (Paper 1.21.11+). `attack-frequency`: `setMaximumNoDamageTicks` 18/16. Gapple and pearl-cooldown modules. ([ocm-cooldown], [ocm-kb], [ocm-range], [ocm-config]) | The attack-speed recipe and the 1.8 numbers (0.4/0.4/0.4/0.5/0.1) are the industry reference. Its KB path is Spigot-compatible, hence `PlayerVelocityEvent`; we can do better with the Paper event. MPL-2.0: fine to learn from, do not copy code. |
| **KohiKB / LegacyKB** (MWHunter/KohiKB; last push 2023) | **no license file** ([kohi-repo]) | Single class; same `EntityDamageByEntityEvent` + `PlayerVelocityEvent` pattern and the same five numbers; per-tick map clear "Hack around issue with knockback in wrong tick" ([kohi-src]). | Confirms the numbers; unlicensed, so patterns only. |
| **KnockbackSync** (Axionize/knockback-sync; pushed 2026-08-22) | GPL-3.0-or-later ([kbsync-repo]) | Not a KB *profile* plugin: on `PlayerVelocityEvent` it replaces the **vertical** component with a client-predicted value using measured ping (`ping_offset: 25`), via PacketEvents ([kbsync-listener], [kbsync-pom]). | Out of scope for v0.1; a "ping-fair KB" feature would need PacketEvents. GPL is incompatible with a closed plugin — patterns only. |
| **knockback-api** (HGLabor; last push 2022) | GPL-3.0 | Kotlin API exposing horizontal/vertical/vertical-limit knobs. Stale. | Ignore. |

---

## 7. Version-sensitive notes

### 7.1 Against the pinned target: Paper 26.1.2 build 74 (`io.papermc.paper:paper-api:26.1.2.build.74-stable`, Java 25)

Everything above was re-checked on Paper's `ver/26.1.2` branch ([branch list][paper-branches]):

| Finding | Status on 26.1.2 | Evidence |
|---|---|---|
| `Attribute.ATTACK_SPEED`, `KNOCKBACK_RESISTANCE`, `ENTITY_INTERACTION_RANGE`, `EXPLOSION_KNOCKBACK_RESISTANCE` | present, same keys | [Attribute.java @ ver/26.1.2][p2612-attr] |
| `EntityKnockbackEvent` → `EntityPushedByEntityAttackEvent` → `EntityKnockbackByEntityEvent`; `getHitBy()`, `getKnockbackStrength()` (`@ApiStatus.Obsolete(since="1.20.6")`) | present, same hierarchy | [EntityKnockbackByEntityEvent.java @ ver/26.1.2][p2612-kbbe] |
| Knockback math: resistance applied before the event, `if (true \|\| !(power <= 0.0))` "Call event even when force is 0", vector = `finalVelocity − deltaMovement`, applied as `deltaMovement.add(event.getKnockback())`, cancel skips `needsSync` | identical | [LivingEntity.java.patch @ ver/26.1.2][p2612-living] |
| Two knockback calls per boosted hit: `hurt()` base `knockback(0.4F, xd, zd, directEntity, ENTITY_ATTACK)`, then `Player.attack` `knockback(knockbackAmount, sin(yaw), -cos(yaw), this, ENTITY_ATTACK)`; sweep `0.4F` with `SWEEP_ATTACK`; shield `0.5` with `SHIELD_BLOCK` | identical (the internal `knockback(...)` overloads on 26.1.2 do not yet take the `DamageSource`/`damage` parameters that `main` adds — plugin-invisible) | [p2612-living], [Player.java.patch @ ver/26.1.2][p2612-player] |
| `entities.behavior.disable-player-crits`, `misc.disable-sprint-interruption-on-attack` | present | [p2612-player] |
| Respawn keeps attribute base values (`assignBaseValues`), drops permanent modifiers | identical | [ServerPlayer.java.patch @ ver/26.1.2][p2612-serverplayer] |
| `DataComponentTypes.ATTACK_RANGE` (`Valued<AttackRange>`), `USE_COOLDOWN`, `WEAPON`, `BLOCKS_ATTACKS`; also `PIERCING_WEAPON` / `KINETIC_WEAPON` (spears) | present — the 1.21.11 reach/hitbox knobs are available on our pin | [DataComponentTypes.java @ ver/26.1.2][p2612-dct] |
| `HumanEntity.setCooldown(Material,int)` / `(ItemStack,int)` / `(Key,int)` and matching `getCooldown` | present | [HumanEntity.java @ ver/26.1.2][p2612-human] |
| `PlayerItemCooldownEvent extends PlayerItemGroupCooldownEvent` | present | [PlayerItemCooldownEvent.java @ ver/26.1.2][p2612-cooldown] |
| `PlayerAttackEntityCooldownResetEvent` | present but `@Deprecated(since = "26.1", forRemoval = true)` — it is deprecated on *exactly* our pin; do not use | [PlayerAttackEntityCooldownResetEvent.java @ ver/26.1.2][p2612-paecre] |
| Spears (`*_SPEAR` materials with jab/charge attacks) exist in 26.1 | if spears are obtainable, follow OCM and give them a per-material attack speed instead of 40, or remove them from kits/crafting | [ocm-config] |

Javadoc for the pin: `https://jd.papermc.io/paper/26.1/` (header reads
`paper-api 26.1.2.build.74-stable`; the site flags it as "old Javadocs … look at Paper
26.3 instead"). Class-level URLs under that path currently resolve to the index, so cite
the `ver/26.1.2` source links above when in doubt.

### 7.2 General

- **Paper versioning changed.** Current javadocs are labelled `paper-api 26.3.build.8-alpha`
  ([jd-index]); the 1.21.8 javadocs say "Paper 26.3 is recommended". Everything here was
  cross-checked against `main` source and the 1.21.8 javadocs; the pinned version chosen
  by the other agent decides which javadoc set to code against.
- **Attribute constants.** `Attribute.ATTACK_SPEED` / `KNOCKBACK_RESISTANCE` /
  `ENTITY_INTERACTION_RANGE` (no `GENERIC_` prefix) are the current names; `Attribute`
  is an interface and `values()`/`valueOf()` are deprecated since 1.21.3 for removal in
  1.22 ([paper-attr-src]). Code targeting ≤ 1.21.1 would use `GENERIC_ATTACK_SPEED`
  (KohiKB's source shows the old name – [kohi-src]). Vanilla dropped the `generic.`
  prefix in the 1.21.2 cycle (exact snapshot not verified here).
- **`AttributeModifier` UUID constructors** are deprecated in favour of `NamespacedKey`
  ([jd-attrmod]).
- **Knockback events.** `EntityKnockbackEvent`/`EntityPushedByEntityAttackEvent` exist
  since 1.20.6 (the `since = "1.20.6"` deprecations date the refactor); older
  `getAcceleration/setAcceleration` are `forRemoval`. Code for ≥ 1.20.6 only.
- **`PlayerAttackEntityCooldownResetEvent`** is deprecated for removal since 26.1 — avoid.
- **`attack_range` component** and spears: 1.21.11+ (2025-12-09). OCM's reach module
  self-disables below that. If our pin is older, reach is `entity_interaction_range` only.
- **Ender pearls load chunks** since 1.21.2; `misc.legacy-ender-pearl-behavior` toggles it.
- **`use_cooldown` component** exists since 1.21.2 ([wiki-1212]); the pearl's 1-second
  cooldown is applied through the cooldown system that `PlayerItemCooldownEvent` hooks.
- **Sprint/enchant bonus scalar** in `Player.attack` was not visible in Paper's patch
  (vanilla code); read `getKnockbackStrength()` at runtime rather than assuming 0.5.
- SPEC §44 says "Test extensively on the chosen Minecraft version" — the two-event
  behaviour in §2.2 is the thing most likely to differ between versions; write an
  integration test that hits a dummy and asserts the number of `EntityKnockbackEvent`
  calls and the resulting velocity.

## 8. Open decisions for implementation

1. Config shape: keep SPEC's `horizontal / vertical / sprint-multiplier` only, or add
   `vertical-limit`, `extra-horizontal`, `extra-vertical` (1.8-additive model) and
   `respect-knockback-resistance`. Recommendation: add them; presets fill them.
2. Attack-speed value: 40 (OCM convention) vs 1024 (cap). Recommendation: 40, configurable.
3. Whether to disable sweep attacks and shields by default in the Classic preset.
   Recommendation: sweep off, shields off (kits don't include them), both configurable.
4. Hit delay: keep vanilla 20 max-no-damage-ticks or expose `combat.hit-delay-ticks`.

## Sources

[cfg-global]: https://docs.papermc.io/paper/reference/global-configuration/
[cfg-world]: https://docs.papermc.io/paper/reference/world-configuration/
[cfg-spigot]: https://docs.papermc.io/paper/reference/spigot-configuration/
[jd-index]: https://jd.papermc.io/paper/
[jd-kb]: https://jd.papermc.io/paper/io/papermc/paper/event/entity/EntityKnockbackEvent.html
[jd-attr]: https://jd.papermc.io/paper/1.21.8/org/bukkit/attribute/Attribute.html
[jd-attrinst]: https://jd.papermc.io/paper/org/bukkit/attribute/AttributeInstance.html
[jd-attrmod]: https://jd.papermc.io/paper/org/bukkit/attribute/AttributeModifier.html
[jd-human]: https://jd.papermc.io/paper/org/bukkit/entity/HumanEntity.html
[jd-living]: https://jd.papermc.io/paper/org/bukkit/entity/LivingEntity.html
[jd-pve]: https://jd.papermc.io/paper/org/bukkit/event/player/PlayerVelocityEvent.html
[jd-plpe]: https://jd.papermc.io/paper/com/destroystokyo/paper/event/player/PlayerLaunchProjectileEvent.html
[jd-paecre]: https://jd.papermc.io/paper/com/destroystokyo/paper/event/player/PlayerAttackEntityCooldownResetEvent.html
[jd-damagecause]: https://jd.papermc.io/paper/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html
[jd-damagetype]: https://jd.papermc.io/paper/org/bukkit/damage/DamageType.html
[jd-edbee]: https://jd.papermc.io/paper/org/bukkit/event/entity/EntityDamageByEntityEvent.html
[jd-dct]: https://jd.papermc.io/paper/io/papermc/paper/datacomponent/DataComponentTypes.html
[paper-kb-src]: https://github.com/PaperMC/Paper/blob/main/paper-api/src/main/java/io/papermc/paper/event/entity/EntityKnockbackEvent.java
[paper-pushed-src]: https://github.com/PaperMC/Paper/blob/main/paper-api/src/main/java/io/papermc/paper/event/entity/EntityPushedByEntityAttackEvent.java
[paper-kbbe-src]: https://github.com/PaperMC/Paper/blob/main/paper-api/src/main/java/com/destroystokyo/paper/event/entity/EntityKnockbackByEntityEvent.java
[paper-attr-src]: https://github.com/PaperMC/Paper/blob/main/paper-api/src/main/java/org/bukkit/attribute/Attribute.java
[paper-cooldown-src]: https://github.com/PaperMC/Paper/blob/main/paper-api/src/main/java/io/papermc/paper/event/player/PlayerItemCooldownEvent.java
[paper-groupcooldown-src]: https://github.com/PaperMC/Paper/blob/main/paper-api/src/main/java/io/papermc/paper/event/player/PlayerItemGroupCooldownEvent.java
[paper-living-patch]: https://github.com/PaperMC/Paper/blob/main/paper-server/patches/sources/net/minecraft/world/entity/LivingEntity.java.patch
[paper-player-patch]: https://github.com/PaperMC/Paper/blob/main/paper-server/patches/sources/net/minecraft/world/entity/player/Player.java.patch
[paper-serverplayer-patch]: https://github.com/PaperMC/Paper/blob/main/paper-server/patches/sources/net/minecraft/server/level/ServerPlayer.java.patch
[paper-serverentity-patch]: https://github.com/PaperMC/Paper/blob/main/paper-server/patches/sources/net/minecraft/server/level/ServerEntity.java.patch
[paper-pearl-patch]: https://github.com/PaperMC/Paper/blob/main/paper-server/patches/sources/net/minecraft/world/item/EnderpearlItem.java.patch
[wiki-attr]: https://minecraft.wiki/w/Attribute
[wiki-damage]: https://minecraft.wiki/w/Damage
[wiki-entity]: https://minecraft.wiki/w/Entity_format
[wiki-kbench]: https://minecraft.wiki/w/Knockback
[wiki-attackrange]: https://minecraft.wiki/w/Data_component_format/attack_range
[wiki-usecooldown]: https://minecraft.wiki/w/Data_component_format/use_cooldown
[wiki-12111]: https://minecraft.wiki/w/Java_Edition_1.21.11
[wiki-1212]: https://minecraft.wiki/w/Java_Edition_1.21.2
[wiki-pearl]: https://minecraft.wiki/w/Ender_Pearl
[ocm-repo]: https://github.com/kernitus/BukkitOldCombatMechanics
[ocm-cooldown]: https://github.com/kernitus/BukkitOldCombatMechanics/blob/master/src/main/java/kernitus/plugin/OldCombatMechanics/module/ModuleAttackCooldown.java
[ocm-kb]: https://github.com/kernitus/BukkitOldCombatMechanics/blob/master/src/main/java/kernitus/plugin/OldCombatMechanics/module/ModulePlayerKnockback.java
[ocm-range]: https://github.com/kernitus/BukkitOldCombatMechanics/blob/master/src/main/java/kernitus/plugin/OldCombatMechanics/module/ModuleAttackRange.java
[ocm-config]: https://github.com/kernitus/BukkitOldCombatMechanics/blob/master/src/main/resources/config.yml
[kohi-repo]: https://github.com/MWHunter/KohiKB
[kohi-src]: https://github.com/MWHunter/KohiKB/blob/master/src/main/java/org/abyssmc/legacykb/LegacyKB.java
[kbsync-repo]: https://github.com/Axionize/knockback-sync
[kbsync-listener]: https://github.com/Axionize/knockback-sync/blob/merge/src/main/java/me/caseload/knockbacksync/listener/PlayerKnockbackListener.java
[kbsync-pom]: https://github.com/Axionize/knockback-sync/blob/merge/pom.xml
[paper-branches]: https://github.com/PaperMC/Paper/branches
[p2612-attr]: https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-api/src/main/java/org/bukkit/attribute/Attribute.java
[p2612-kbbe]: https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-api/src/main/java/com/destroystokyo/paper/event/entity/EntityKnockbackByEntityEvent.java
[p2612-living]: https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-server/patches/sources/net/minecraft/world/entity/LivingEntity.java.patch
[p2612-player]: https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-server/patches/sources/net/minecraft/world/entity/player/Player.java.patch
[p2612-serverplayer]: https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-server/patches/sources/net/minecraft/server/level/ServerPlayer.java.patch
[p2612-dct]: https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-api/src/main/java/io/papermc/paper/datacomponent/DataComponentTypes.java
[p2612-human]: https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-api/src/main/java/org/bukkit/entity/HumanEntity.java
[p2612-cooldown]: https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-api/src/main/java/io/papermc/paper/event/player/PlayerItemCooldownEvent.java
[p2612-paecre]: https://github.com/PaperMC/Paper/blob/ver/26.1.2/paper-api/src/main/java/com/destroystokyo/paper/event/player/PlayerAttackEntityCooldownResetEvent.java
