# VelKoth, patched for Avian

VelKoth (MIT, [Velmax-Studios/VelKoth](https://github.com/Velmax-Studios/VelKoth)) runs our KOTH (issue #24).
`buildVelKoth` downloads the pinned commit (`velKothCommit` in `gradle.properties`, tarball checked by
SHA-256), applies `avian.patch` with `git apply`, and builds it. Upstream changing under the pin can't
slip through: the patch only applies to exactly that source, and the build checks it landed.

What `avian.patch` changes, and why:

1. **FactionsUUID 4.x** (`team/FactionsUUIDHook.java`, `team/TeamManager.java`). Upstream's hook targets
   FactionsUUID 0.x (`com.massivecraft.factions`), and looks for a plugin named "Factions". Ours is
   FactionsUUID 4.7 (`dev.kitteh.factions`, plugin "FactionsUUID"), so VelKoth found no factions plugin
   and faction members contested each other on the hill. The new hook counts two players as one team
   only when both are in the same real faction; factionless players are each on their own (upstream
   would have treated all of them as one team).
2. **Prize to the capturing player only** (`capture/CaptureManager.java`, owner 2026-10-03: "the capturing
   player gets it; their faction gets the bragging rights"). Upstream gives the full prize to every
   online member of the winner's team. `-Dvelkoth.rewardWholeTeam=true` brings that back.
3. **MySQL/MariaDB** (`storage/DatabaseManager.java`). Upstream's MySQL mode uses SQLite-only SQL
   (`INTEGER PRIMARY KEY AUTOINCREMENT`, `ON CONFLICT … excluded`), so its tables were never created. Now
   `AUTO_INCREMENT` and `ON DUPLICATE KEY UPDATE`. SQLite mode is no longer supported: we use MariaDB.

Changing the patch: unpack the pinned tarball twice (`a/`, `b/`), edit `b/`, then
`diff -ruN a b > avian.patch` and strip the timestamps. These fixes are worth offering upstream.
