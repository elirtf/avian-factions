# Running the server in a container (and on Kubernetes, and behind a hub)

`./dev start` is the development loop. This page covers the other way to run the server: as one
container image, which runs the same way on this machine, a new machine or a Kubernetes cluster.
Moving machines with `./dev` alone is in [MIGRATION.md](MIGRATION.md); this page builds on it.

## What's in the image

`./dev image` builds `avian-factions:dev` from this checkout:

- the same Paper build (pinned by hash in `gradle.properties`);
- the same pinned plugin jars as `./dev start`, plus our plugin;
- the tracked config (`dev-server/`), laid over the server at every start by `tools/sync-config`,
  the same script the dev loop uses.

The image holds no world and no passwords. Everything that changes while people play (worlds,
plugin data, logs) lives in the `/data` volume, and settings come from environment variables.

| Variable | What it does | Default |
|---|---|---|
| `EULA` | Must be `true`: you accept the [Minecraft EULA](https://aka.ms/MinecraftEULA) | none, won't start |
| `MEMORY` | Java heap (`AVIAN_MEMORY` in `.env` for Compose) | `4G` (Compose: `8G`) |
| `AVIAN_DB_HOST`, `_PORT`, `_NAME`, `_USER`, `_PASSWORD` | The database, for our plugin and every plugin that stores data in it (LuckPerms, CoreProtect, AuraSkills, CraftEngine) | the dev values |
| `AVIAN_VELOCITY`, `AVIAN_VELOCITY_SECRET`, `AVIAN_ONLINE_MODE` | Running behind a Velocity proxy (see [A hub](#a-hub-later)) | off |
| `JAVA_OPTS` | Extra JVM flags | none |

**How settings reach the config.** A tracked config file can say `${AVIAN_DB_HOST:-127.0.0.1}`:
`tools/sync-config` fills that in from the environment at start, or uses the default after `:-`.
Values must not contain quotes, backslashes or newlines, and the sync stops if one does. Use
letters and digits for passwords: `openssl rand -hex 24`.

The server runs as user `avian` (uid 1001), never root. Running it this way meets item 3 of
CLAUDE.md's "Before production" list: a Paper or plugin exploit stays inside the container.

## On one machine (Docker Compose)

```sh
./dev image                                   # build the image
# in .env: EULA=true (and AVIAN_MEMORY, ports if needed; see .env.example)
docker compose --profile server up -d         # start it (the database starts too)
docker compose logs -f server                 # watch the log
docker compose exec server avian-console "list"   # a console command, like ./dev cmd
docker compose --profile server stop server   # stop: it saves and exits cleanly
```

Don't run it alongside `./dev start`: both want port 25565 (set `AVIAN_GAME_PORT` to try both).

**Moving the dev server's game into the container:**

```sh
./dev backup                                  # while ./dev start is running is fine
./dev stop
./dev restore-container backups/avian-backup-<date>.tar.gz
docker compose --profile server up -d
```

`restore-container` loads the backup into the database and into the container's `/data` volume.
The same command sets up a new machine from a backup: clone, copy `.env`, `./dev image`, then this.

**Updating:** pull the new code, run `./dev image`, then `docker compose --profile server up -d`
again. Compose replaces the container and the volume carries the game across. The entrypoint
removes plugin jars the new image no longer has.

**Stopping is safe.** `docker stop`, a reboot or a Kubernetes rollout sends SIGTERM, and the
entrypoint turns that into the `stop` console command, so the world is saved first. Paper's own
SIGTERM handling deadlocks on "Saving players" on this build, so this matters. Allow 120 s.

## On Kubernetes

The network runs on **k3s** (ADR-0008), proven first on **k3d**, which is k3s inside Docker on the
dev box. Manifests are a kustomize base with two overlays:

```
deploy/k3d/cluster.yaml                  the local test cluster (k3s v1.36.4, one node)
deploy/kubernetes/base/                  namespace avian: MariaDB + the Avian Factions server
deploy/kubernetes/overlays/k3d/          the local test: image avian-factions:dev, 4 GB heap, small disks
deploy/kubernetes/overlays/production/   the real k3s box: registry image, 8 GB heap, volumes kept on delete
```

Each overlay reads its database passwords from `secret.env` (git-ignored; copy `secret.env.example`
and fill it with `openssl rand -hex 24`). CI renders both overlays and validates them on every push.

**Tools** (into `~/.local/bin`, checksums verified): k3d v5.9.0 and kubectl v1.36.4, matching the
cluster's version (kubectl supports one minor version either side).

### The local test cluster

Everything goes through `./dev k8s`. It never touches the live dev server: the cluster listens on
**127.0.0.1:25700** (Java) and **127.0.0.1:19700** (Bedrock), not 25565 and 19132.

| Command | What it does |
|---|---|
| `./dev k8s up` | Create the k3d cluster from `deploy/k3d/cluster.yaml` |
| `./dev k8s deploy` | Build the image, import it into k3d, apply the overlay, wait until the server is ready |
| `./dev k8s restore <backup> [--yes]` | Load a `./dev backup` file: database into MariaDB, world and plugin data into the server's volume |
| `./dev k8s status` / `logs` | Pods, services and volumes / the server's log |
| `./dev k8s cmd "list"` | One console command, like `./dev cmd` |
| `./dev k8s down` | Delete the cluster and everything in it |

Checked 2026-09-30: the server boots in about two minutes with every plugin and no errors, Java
clients get the MOTD on 25700, Bedrock gets Geyser's on UDP 19700, and a real backup restores with
its factions intact. A fresh, empty cluster has no ranks yet; a restored backup brings them in the
database, or apply `dev-server/luckperms/ranks.lp` through `./dev k8s cmd`.

### Moving to the real k3s box (runbook)

1. **Install k3s** at the same version, without Traefik (nothing uses it yet):
   `curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION=v1.36.4+k3s1 sh -s - --disable=traefik`.
   Copy `/etc/rancher/k3s/k3s.yaml` to the machine you manage it from as a kubeconfig context.
2. **Get the image there:** push `avian-factions:<version>` to a registry and set it in
   `overlays/production/kustomization.yaml`, or copy it straight in with
   `docker save avian-factions:<version> | sudo k3s ctr images import -`.
3. **Secrets:** `overlays/production/secret.env` with real passwords.
4. **Apply:** `AVIAN_K8S_CONTEXT=<context> AVIAN_K8S_OVERLAY=deploy/kubernetes/overlays/production ./dev k8s deploy`
   (the same commands take those two variables for status, cmd and restore).
5. **Bring the game over:** `./dev backup` on the old box, then `./dev k8s restore <file>` with the
   same two variables. Stop the old server first so nothing is played on it afterwards.
6. **Players:** k3s answers on the node's own ports 25565 (TCP) and 19132 (UDP). Point the router or
   DNS at the new box, and follow CLAUDE.md's "Before production" list for the firewall.

### Why it's shaped like this

- The server is a **StatefulSet with one replica** and its own volume: a world has exactly one
  writer and never scales out. Every world it hosts (today `world`; planned: the spawn world with
  the warzone, the resource world, the darkzone and the flat claiming world) lives on that volume.
- `terminationGracePeriodSeconds: 150` gives the save-then-stop time to finish.
- The **Service** publishes TCP 25565 and UDP 19132 together. k3s's ServiceLB answers it on the node,
  so a home box needs no MetalLB. Once the Velocity proxy exists, it takes this role and the server's
  Service becomes ClusterIP.
- **Memory:** the pod gets the heap (`MEMORY`) plus about 2 GiB for the JVM itself.
- **Production volumes** use `local-path-retain`: deleting a claim keeps the data on disk.
- **The config sync** leaves existing directories' owner and mode alone (`tar --no-overwrite-dir`):
  the volume's root belongs to root and the server runs as user 1001.
- **Backups:** `./dev backup` on the dev box today. On the cluster, a scheduled job that dumps the
  database and copies the server's volume is the next step.

## A hub (later)

The plan is a Velocity proxy in front, a hub server, and this Factions server behind it. What's
ready now:

- **Player data that must follow players between servers is already in MariaDB:** balances,
  tokens and gems, ranks (LuckPerms), skills (AuraSkills). Point every backend at the same
  database. Everything else (worlds, homes, factions, CoreProtect's history) belongs to this
  server alone.
- **Proxy forwarding is one switch:** `AVIAN_VELOCITY=true`, `AVIAN_VELOCITY_SECRET=<Velocity's
  forwarding.secret>` and `AVIAN_ONLINE_MODE=false`. This fills `config/paper-global.yml` and
  `server.properties`, checked on Paper 26.1.2. With that on, the server must be reachable
  **only** through the proxy: a ClusterIP Service on Kubernetes, and no port forward at home.
- **TAB** has `proxy-support` for a tab list across servers; it stays off until the proxy exists.
- **Features stay world-agnostic:** no hardcoded world names, so a separate spawn world or hub
  needs no code changes.

On Kubernetes the network becomes three workloads in the `avian` namespace: `avian-velocity` (a
Deployment with the LoadBalancer Service players connect to), `avian-hub` (a StatefulSet), and
`avian-factions` (this server, its Service switched to ClusterIP). They share `avian-mariadb`.

Still to do when the hub happens:

- The Velocity proxy itself: its own image and config, with ViaVersion and Geyser moving there.
- The hub server.
- LuckPerms' messaging (`messaging-service: sql`), so a rank change reaches every server at once.
- CarbonChat's cross-server channels.
- A per-server CoreProtect table prefix.
