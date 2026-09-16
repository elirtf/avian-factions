---
status: accepted
date: 2026-09-16
---
# MariaDB everywhere, with portable SQL and repository interfaces

Dev, CI, and prod all run MariaDB (locally via Docker Compose); there is no SQLite dev path, because two dialects means every query is tested on one of them. The Minecraft plugin ecosystem (LuckPerms, EssentialsX, CoreProtect — the last is MySQL-only) is MySQL-first, so one engine serves our plugin and the third-party stack. Migration risk is managed not by engine choice but by discipline: Flyway migrations stick to ANSI SQL where possible (every dialect-specific statement gets a comment), and every module accesses the DB only through its own repository interface. Moving to PostgreSQL later would be a migration-script rewrite and a driver swap, not an application rewrite.

**Considered:** SQLite dev + MariaDB prod (spec allowed; rejected for dialect drift); PostgreSQL (CoreProtect can't use it; no scale need at a 5k-border server).
