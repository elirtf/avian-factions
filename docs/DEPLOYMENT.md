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

The network runs on **k3s** and is **GitOps-managed by ArgoCD** (ADR-0008): every cluster is built
from `main`; after a one-time bootstrap nothing is applied by hand, and ArgoCD puts back anything
changed by hand. It's proven first on **k3d** (k3s inside Docker on the dev box).

**Two repos** (both on our Forgejo, git.willowcrest.world/avian):

- **`avian/infra`**: the platform every project shares: the k3d cluster definition, ArgoCD (pinned,
  managing itself), Sealed Secrets, one root per cluster (`clusters/<cluster>`), an ArgoCD project per
  app (Avian may only deploy into its own namespace), and the repo addresses, written once
  (`clusters/base/repos.yaml`). Run with its `./infra` script: `up`, `bootstrap`, `seal-repo-creds`,
  `status`, `ui`, `down`. See its README.
- **This repo**: only the game's manifests, which `infra` points ArgoCD at:

```
deploy/kubernetes/base/                    namespace avian: MariaDB + the Avian Factions server
deploy/kubernetes/overlays/k3d/            the local test: image avian-factions:dev, 4 GB heap, small disks
deploy/kubernetes/overlays/production/     the real k3s box: registry image, 8 GB heap, volumes kept on delete
  sealed-secret.yaml                       the database passwords, encrypted for that cluster
```

CI renders both overlays and validates them with kubeconform on every push.

**Secrets.** Passwords are committed only as a **SealedSecret**: encrypted with a key that only the
cluster holds, so the file is useless to anyone else. Each overlay keeps the plain values in a
git-ignored `secret.env` (copy `secret.env.example`; make passwords with `openssl rand -hex 24`).
`./dev k8s seal` encrypts it into `sealed-secret.yaml`, which you commit, and backs up that cluster's
key to `~/.config/avian/sealed-secrets/<context>.yaml` (never in git; keep a copy somewhere safe).
`../infra/infra bootstrap` puts that key back into a recreated cluster, so committed SealedSecrets still
open. Lose the key and you re-seal from `secret.env` and commit.

**Tools** (into `~/.local/bin`, checksums verified): k3d v5.9.0, kubectl v1.36.4 (matching the cluster;
kubectl supports one minor version either side), kubeseal v0.40.0 and crane v0.22.1 (pushes images).

### The game on the local test cluster

The cluster listens on **127.0.0.1:25700** (Java) and **127.0.0.1:19700** (Bedrock), never the live dev
server's 25565 and 19132. Create and bootstrap it from the infra repo (`../infra/infra up`, `bootstrap`),
then:

| Command | What it does |
|---|---|
| `./dev k8s deploy` | k3d: build the image, import it, ask ArgoCD to refresh, replace the server pod |
| `./dev k8s seal` | Encrypt `secret.env` into the overlay's `sealed-secret.yaml` (commit it) and back up the key |
| `./dev k8s push <version>` | Build the image and publish it for production: to GHCR (private, checked) and, with `AVIAN_REGISTRY` set, our own registry. Never replaces a published version |
| `./dev k8s restore <backup> [--yes]` | Load a `./dev backup`: database into MariaDB, world and plugin data into the volume |
| `./dev k8s status` / `logs` / `cmd "list"` | Pods and volumes / the server's log / one console command |

A manifest change reaches a cluster only through `main`. To try a branch on k3d first, point the
`avian` Application's `targetRevision` at it in the infra repo (on an infra branch), bootstrap, and put
it back to `main` before merging.

Checked 2026-09-30:
- **Rebuilt from git alone:** after deleting the cluster, `up` and `bootstrap` brought everything back,
  ArgoCD included, with the committed SealedSecret opening on the restored key.
- **The server:** booted in about two minutes with every plugin and no errors.
- **ArgoCD:** put back a hand-edited value within seconds, and left a hand-scaled server alone.
- **Players:** Java answers on 25700, Bedrock on UDP 19700.
- **Restore:** a real backup restores with its factions intact.

### Moving to the real k3s box (runbook)

1. **The cluster:** prepare the machine and install k3s by the infra repo's `hosts/README.md` (its
   config, the VIPs, then `./infra bootstrap`). Its `docs/architecture.md` covers the three-node plan.
2. **Image:** `AVIAN_REGISTRY=<registry VIP> ./dev k8s push <version>`, then set that version as `newTag` in
   `overlays/production/kustomization.yaml`. The machines pull `ghcr.io/elirtf/avian-factions` from our
   registry first and from GHCR when it can't serve it (the infra repo's `docs/architecture.md` → Images).
   Tokens, never in git: `~/.config/avian/ghcr-push-token` (classic, `write:packages`) here, and the
   read-only one (`read:packages`) in each machine's `/etc/rancher/k3s/registries.yaml`.
3. **Database secret:** fill `overlays/production/secret.env` here, run
   `AVIAN_K8S_CONTEXT=<context> AVIAN_K8S_OVERLAY=deploy/kubernetes/overlays/production ./dev k8s seal`,
   then commit and merge the new `sealed-secret.yaml`. ArgoCD creates the Secret and the game starts.
4. **Bring the game over:** stop the old server, `./dev backup`, then `./dev k8s restore <file>` with the
   same two variables.
5. **Players:** the game answers on its VIP (kube-vip, set in the infra repo), 25565 TCP (no Bedrock at launch).
   Forward the router's 25565 to it, behind TCPShield, and follow CLAUDE.md's "Before production" list.

### Why it's shaped like this

- The server is a **StatefulSet with one replica** and its own volume: a world has exactly one
  writer and never scales out. Every world it hosts (today `world`; planned: the spawn world with
  the warzone, the resource world, the darkzone and the flat claiming world) lives on that volume.
- `terminationGracePeriodSeconds: 150` gives the save-then-stop time to finish.
- The **Service** publishes TCP 25565 and UDP 19132 together. On k3d, k3s's ServiceLB answers it on
  the node. In production, kube-vip gives it a floating LAN address that moves to a live node. Once the Velocity proxy exists, it takes this role and the server's
  Service becomes ClusterIP.
- **Memory:** the pod gets the heap (`MEMORY`) plus about 2 GiB for the JVM itself.
- **Production volumes** use the platform's `retain` class (the infra repo: Longhorn, replicated across
  the nodes, kept when a claim is deleted). The game never defines storage itself.
- **The config sync** leaves existing directories' owner and mode alone (`tar --no-overwrite-dir`):
  the volume's root belongs to root and the server runs as user 1001.
- **Locked down** (the infra repo's `docs/architecture.md` → Security): the namespace enforces Pod
  Security **restricted**, so every pod runs non-root (the server as 1001, MariaDB as its own 999,
  `./dev k8s restore`'s helper as 1001), with no capabilities and no privilege escalation; anything
  else is refused. **NetworkPolicies** deny all traffic in by default. Players may reach the server's
  25565/19132, only the server may reach MariaDB, and MariaDB can't open any connection out (DNS
  only). Checked 2026-09-30 on k3d: the old specs are refused and the new ones pass; MariaDB
  initialises as 999; the server reaches MariaDB, other pods don't; MariaDB can't reach the internet.
- **Images:** checked 2026-10-01, `./dev k8s push dev-c85a6e6` built and pushed the image to GHCR, the
  package came out private, the machines' read-only token pulls it, and anonymous pulls and pushes with
  the read-only token are refused.
- **Production placement** (the infra repo's `docs/architecture.md`): the server runs only on the two
  32 GB machines and outranks every other pod (`game-critical`), so the surviving one makes room for it
  on a failover. MariaDB sits next to it when it can.
- **No Bedrock at launch** (owner, 2026-09-30): production doesn't publish UDP 19132. TCPShield covers it
  only on its paid plan, and an open port would expose the home IP. Geyser still runs in the pod.
- **Node loss:** the server and MariaDB move after 30 s on a dead node instead of 5 min. That matters
  once there are three nodes and Longhorn volumes to move with them.
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
