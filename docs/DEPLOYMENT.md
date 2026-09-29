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

Manifests are in `deploy/kubernetes/`. They pass strict schema validation (kubeconform) but
haven't run on a real cluster yet. Treat them as the starting point.

1. **Push the image** somewhere the cluster can pull from:
   `docker tag avian-factions:dev <registry>/avian-factions:<version>` and `docker push`. Set that
   name in `server.yaml`.
2. **Create the Secret:** copy `secret.example.yaml` to `secret.yaml` (git-ignored), set real
   passwords, then `kubectl apply -f deploy/kubernetes/secret.yaml`.
3. **Apply** `mariadb.yaml`, then `server.yaml`.
4. **Load the game** from a backup: scale the server to 0, load `database.sql` into MariaDB
   (`kubectl exec -i avian-mariadb-0 -- mariadb -u… -p… avian < database.sql`, into an empty
   database), copy the backup's `run/` contents into the server's volume with a temporary pod, then
   scale back to 1.
5. **Console:** `kubectl exec avian-0 -- avian-console "list"`.

The shape, and why:

- The server is a **StatefulSet with one replica** and its own volume. A Minecraft world has
  exactly one writer; it never scales out.
- `terminationGracePeriodSeconds: 150` gives the save-then-stop time to finish.
- The **Service** publishes TCP 25565 (Java) and UDP 19132 (Bedrock through Geyser). A
  LoadBalancer with both protocols needs Kubernetes 1.26+. On a home cluster, use MetalLB, or a
  NodePort with the router forwarding to it.
- **Memory:** the pod gets the heap (`MEMORY`) plus about 2 GiB for the JVM itself.
- **Backups:** dump the database (`mariadb-dump --single-transaction`) and snapshot the server's
  volume, or run `avian-console "save-off"`, `"save-all flush"`, copy `/data`, then `"save-on"`, as
  `./dev backup` does.

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

Still to do when the hub happens:

- The Velocity proxy itself: its own image and config, with ViaVersion and Geyser moving there.
- The hub server.
- LuckPerms' messaging (`messaging-service: sql`), so a rank change reaches every server at once.
- CarbonChat's cross-server channels.
- A per-server CoreProtect table prefix.
