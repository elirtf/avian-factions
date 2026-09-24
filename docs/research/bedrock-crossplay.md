# Bedrock crossplay (Geyser + Floodgate + ViaVersion)

Researched and boot-tested 2026-09-24 against the GeyserMC download API
(`download.geysermc.org/v2`), the Geyser README / wiki, and ViaVersion's Modrinth listing.

## Pins

| Plugin | Version | Why |
|---|---|---|
| Geyser-Spigot | 2.11.3 build 1246 (2026-09-23) | Translates Bedrock clients to Java. Listens on **UDP 19132**. |
| Floodgate (spigot) | 2.2.5 build 141 (2026-09-17) | Bedrock players join without a Java account. Geyser `java.auth-type: floodgate`. |
| ViaVersion | 5.12.0 (2026-09-18, Modrinth release channel) | Geyser only speaks the newest Java protocol: README "Java: 26.2". ViaVersion lets that protocol (and 26.2 Java clients) join our 26.1.2 server. This is the wiki's documented route for "Geyser on older Java servers". |

URLs and SHA-256 hashes are in `avian-plugin/build.gradle.kts`. GeyserMC has no release channel:
every build is published and "latest" floats, so pin by build number. Bedrock clients auto-update,
and Geyser supports only current Bedrock versions (README: 26.30–26.51), so **expect to re-pin
Geyser whenever Bedrock ships an update**. An old Geyser rejects new phones and consoles.

## Boot test

Throwaway Paper 26.1.2 build 74 plus only these three plugins, on Java 25. Two clean boots: Floodgate
boots, `ViaVersion detected server version: 26.1-26.1.2 (775)`, `Started Geyser on UDP port
19132`, clean shutdown, no errors from any plugin. Floodgate b141's only change is "Update to 26.3",
and it still runs fine on 26.1.2. Not yet tested: an actual Bedrock client joining, or the three
alongside the full stack (CI's `./dev check-log` now expects all three).

## What changes for players and admins

- **Connect:** Bedrock uses the same host with port **19132**. Consoles (Xbox, PlayStation, Switch)
  cannot add custom servers without a workaround such as BedrockConnect; phones, Windows and
  tablets can.
- **Names and UUIDs:** Bedrock players appear as `.Gamertag` (Floodgate `username-prefix`), with a
  UUID derived from their XUID (`00000000-0000-0000-xxxx-xxxxxxxxxxxx`). Everything is keyed by
  UUID, so factions, economy and LuckPerms work unchanged.
- **Whitelist:** `/whitelist add` cannot look up Bedrock names. Use Floodgate's
  `./dev cmd "fwhitelist add <Gamertag>"`. The player must be online, or must have joined once
  since Floodgate installed. Then copy `run/whitelist.json` back into `dev-server/whitelist.json`
  as usual, or the next `./dev start` drops the entry.
- **Menus:** Geyser translates chest GUIs. EconomyShopGUI already has Bedrock Forms settings,
  which activate now that Floodgate is present.
- **PvP:** Bedrock combat has no 1.9 cooldown indicator by default (Geyser shows a crosshair one)
  and touch aiming differs. This matters for a competitive Factions server; watch it in playtests.
- **Linking:** Floodgate global linking is on (default), so a player who links their Java and
  Bedrock accounts at link.geysermc.org shares one profile. Local linking is off, so Floodgate's
  SQLite store is unused.

## Networking (not done: needs sudo and the router)

`~/docker/hardening/host-firewall` only opens **TCP 25565**, so UDP 19132 is blocked except over
Tailscale. For a Bedrock friend on the internet you need both:

1. A firewall rule mirroring the `MC_PUBLIC` one, e.g.
   `ipt -A fw-in -i "$LAN_IF" -p udp --dport 19132 -j ACCEPT` inside the same `if`, then
   `sudo ~/docker/hardening/install.sh`.
2. A router port forward for **UDP** 19132.

Add both to the "Before production" list in `CLAUDE.md` (close UDP 19132 alongside TCP 25565).

## Velocity later

When the planned Velocity proxy arrives, move Geyser and Floodgate to the proxy
(`Geyser-Velocity`, `floodgate-velocity`). Keep Floodgate on the Paper backends too, with
`send-floodgate-data: true` on the proxy, so plugins like EconomyShopGUI still detect Bedrock
players. ViaVersion can then move to the proxy (ViaVersion-Velocity) as well.
