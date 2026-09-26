# Anticheat: GrimAC

Written 2026-09-26. Spec §45. A launch blocker from the 2026-09-26 plan.

## Choice: GrimAC 2.3.74

| | GrimAC 2.3.74-8eb5f28 |
|---|---|
| Source | github.com/GrimAnticheat/Grim, GPL-3.0 |
| Downloads | ~700k (Modrinth), updated 2026-09-10 |
| 26.1.2 | Modrinth build for 26.1.2 (Paper); **boot-tested clean** (below) |
| How it works | Prediction engine: it simulates each player's movement from their inputs and flags anything the client couldn't have done. That covers movement (fly, speed, no-fall, timer), reach, knockback (velocity) and bad packets. |
| Storage | Its own SQLite file for violations (`plugins/GrimAC/databases`); nothing touches our MariaDB. |

Alternatives weren't needed. Grim is the open-source standard for modern versions, and the
closed-source ones (Vulcan, Spartan, Polar) are paid and can't be read.

## Boot test (2026-09-26)

Scratch Paper 26.1.2 build 74 on JDK 25, port 25599, alongside Floodgate 2.2.5 b141 and
ViaVersion 5.12.0.
- **Loading:** Grim loaded its bundled packetevents 2.13.1 and the 26.1 block mappings, then registered packets and schedulers.
- **Storage:** its datastore created its tables.
- **Log:** no ERROR or exception lines; "Done (35.9s)".
- **Not tested:** a real player (no client in the test). The first playtest on `./dev` is the real check.

## What matters for us

- **Safe defaults.** `punishments.yml` ships with alerts (`[alert]`) and logs (`[log]`) only; no
  kick or ban commands. Leave it that way until the playtest shows few false flags, then add
  kicks for the clear-cut checks (Timer, BadPackets) first.
- **Bedrock players.** Geyser clients move differently and would false-flag. Grim detects them
  itself (`ac.grim.grimac.utils.reflection.GeyserUtil`, via Floodgate) and exempts them. Verify
  with a Bedrock join.
- **Our combat changes.**
  - *Knockback:* `avian-combat` applies 1.8-style knockback by setting the victim's velocity, and
    the server sends that to the client as a velocity packet. Grim's Knockback check expects the
    client to take exactly the velocity the server sent, so our custom values are fine in
    principle. Watch for Knockback flags in the playtest.
  - *Attack speed:* the attack-speed attribute (40) and `hit-delay-ticks` (16) are server-side;
    Grim doesn't check attack cooldown.
  - *Reach:* the default is 3.0005 blocks, vanilla.
- **Alerts** go to players with `grim.alerts` (give it to the staff ranks in LuckPerms) and to the
  console.
- **Velocity later.** When the proxy lands (#25), Grim can share alerts across servers through
  the proxy's plugin-message channel (`alerts.proxy` in `config.yml`).

## Pin

`https://cdn.modrinth.com/data/LJNGWSvH/versions/Gd6BG1HA/grimac-bukkit-2.3.74-8eb5f28.jar`,
SHA-256 `91c06e7ae7da53636bc5e500d5af3d36a6180247e155fa5b4340da5a72f9eeb7` (the Modrinth
SHA-512 was checked on download too).
