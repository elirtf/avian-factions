# Moving the server to a new machine

Plain-language guide. Follow it top to bottom.

**Production runs on Kubernetes** (k3s on the home machines, managed by ArgoCD; owner's choice, for
uptime and so the machines share the load). If a big machine dies, the game restarts on the other one
with its world. So the real move is **[Moving to production](#moving-to-production-kubernetes)** below.
The plain `./dev` steps after it are for a dev box, or a single machine in a pinch. Both paths use the same
backup file.

## Moving to production (Kubernetes)

The details live in two runbooks; this is the order to follow them in.

1. **Before anything:** the "Before production" list in `CLAUDE.md`: close the dev box's 25565 and
   remove its port forward. On the cluster the server already runs as an unprivileged user (1001), not as
   a member of the `docker` group, which covers that list's third item.
2. **The machines and the cluster:** the infra repo's `hosts/README.md` (Ubuntu Server, k3s on big-1
   first, then small-1 and small-2, then big-2), ending with `./infra bootstrap`. Its
   `docs/architecture.md` explains why it's shaped this way.
3. **The game:** [DEPLOYMENT.md → Moving to the real k3s box](DEPLOYMENT.md#moving-to-the-real-k3s-box-runbook):
   - push the image;
   - seal the database passwords;
   - stop the dev server and take a `./dev backup`;
   - `./dev k8s restore` it.
4. **The worlds:** the claiming world comes across in the backup. The resource world, the Nether and
   the End get fresh worlds with the custom terrain, pre-generated before anyone joins:
   [DEPLOYMENT.md → New worlds with the custom terrain](DEPLOYMENT.md#new-worlds-with-the-custom-terrain).
5. **Check it's healthy:** `./dev k8s status`, `./dev k8s logs` (no plugin errors) and
   `./dev k8s cmd "spark tps"` (20). Then join and check a few things: your faction and claims, your
   balance, `/spawn`, an envoy spot.
6. **Players in:**
   - point the router's 25565 at the game's VIP, behind TCPShield;
   - point `mc.avian.club` at it;
   - keep the dev box's server stopped, so only one copy of the world is ever live.
7. **Backups on the cluster.** ⚠ **Not built yet**: there is no `./dev k8s backup` and no hourly job
   (planned in the infra repo's `docs/architecture.md` → Backups). Build it before opening to players.

---

## The big idea: two halves

Everything the server needs is in one of two places:

| Half | What's in it | Where it lives | How it moves |
|---|---|---|---|
| **Code and settings** | Our plugin, every config file, the ranks, the plugin list, the spawn schematic | **Git** (our Forgejo) | `git clone` |
| **The game itself** | The world, factions and claims, balances, homes, warps, protected regions, crate locations, who holds which rank, the CoreProtect history | **This machine only** | `./dev backup`, then copy the file |

Git holds the first half. It can't sensibly hold the second — that changes every minute players
are online. So you move it with a backup file.

**One thing is in neither, on purpose:** the `.env` file. It holds passwords. Copy it by hand,
privately (a USB stick or a private message to yourself — **never** GitHub, never a public chat).

---

## Moving the plain way (one machine, no Kubernetes)

### On the OLD machine

**1. Take a backup.** This works while the server is running — nobody gets kicked.

```sh
./dev backup
```

It prints where it saved, like `backups/avian-backup-20260923-193848.tar.gz`.

**2. Copy two files to the new machine:**
- that backup file
- the `.env` file (privately — see above)

That's all. Everything else comes from git.

---

### On the NEW machine

You need **Docker**, **git**, **Java 17 or newer** (Java 25 downloads by itself) and **at least
one system font** (`dejavu-fonts-ttf` on Void, `fonts-dejavu-core` on Debian/Ubuntu; without any,
the HUD's text fails to load).

**1. Get the code:**

```sh
git clone https://git.willowcrest.world/avian/avian-factions.git
cd avian-factions
```

**2. Put the `.env` file** you copied into this folder.

**3. Set things up** (starts the database, downloads every plugin):

```sh
./dev setup
```

**4. Restore the game:**

```sh
./dev restore path/to/avian-backup-20260923-193848.tar.gz
```

It asks you to type `restore` to confirm, because it replaces whatever world and data are there.

**5. Start it:**

```sh
./dev start
```

**6. Check it's healthy:**

```sh
./dev check-log
./dev cmd "spark tps"
```

TPS should be 20.

---

## Giving the server more memory

The server gets **8 GB** today. On a bigger machine, give it more by editing one line in
`avian-plugin/build.gradle.kts`:

```kotlin
jvmArgs("-Xms2G", "-Xmx8G")
```

Change `-Xmx8G` to, say, `-Xmx16G`. Leave a few GB for the machine itself and the database — on a
32 GB machine, 16–20 GB for the server is sensible. Then `./dev restart`.

**On Kubernetes** the heap is `MEMORY` in `deploy/kubernetes/base/factions.yaml` (8G today). Raise
the pod's `memory` request and limit next to it as well, to the heap plus about 2 GiB. Merge it, and
ArgoCD rolls the server over.

---

## Things to check after moving

- **Port forwarding.** The new machine needs **TCP 25565** forwarded on its router (on Kubernetes: to
  the game's VIP, behind TCPShield). The database
  port (3306) must **not** be forwarded — it only listens on the machine itself, on purpose.
- **The whitelist** came across in git. If someone can't join, `./dev cmd "whitelist add <name>"`,
  then copy `run/whitelist.json` into `dev-server/whitelist.json` and commit it.
- **mc.avian.club** needs pointing at the new machine's IP address.

---

## Backups in general

Take one **before anything risky** — a big paste, a plugin update, a season reset — and keep a few
old ones. Backups are big and change constantly, so they live in `backups/`, which git ignores.
Copy them somewhere else too: a backup that only exists on the machine that broke is not a backup.

## What is NOT in a backup (and why that's fine)

- **Plugin `.jar` files** — `./dev setup` downloads the exact pinned versions again.
- **Logs** — not needed to run anything.
- **Plugin library downloads** (like LuckPerms' `libs/`) — each plugin fetches its own.
