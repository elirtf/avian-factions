# Developing Avian Factions

How to run the dev server, change things, and get changes merged. Every command here is typed in a
terminal inside the project folder (`~/projects/minecraft/factions`).

Everything goes through one script: **`./dev`**. Run `./dev` on its own to see the list.

---

## The short version

```sh
./dev start       # start the server (builds the plugin first)
./dev console     # open its console; leave with Ctrl-b then d
./dev restart     # after changing code or config
./dev stop        # stop it
```

Connect Minecraft to **`localhost`**.

---

## First time only

You need **Java** (any version 17 or newer; the right one, 25, is downloaded for you), **Docker** and
**tmux**. Then:

```sh
./dev setup
```

That:
1. creates `.env`, the local database passwords (never committed)
2. starts the database in Docker
3. downloads the plugin stack and builds FactionsUUID

The first time takes a few minutes. After that it's quick.

---

## Everyday use

### Starting and stopping

| You want to… | Run |
|---|---|
| Start the server | `./dev start` |
| Stop it | `./dev stop` |
| Restart after changing code or config | `./dev restart` |
| Check what's running | `./dev status` |

`./dev start` waits until the server is ready, then applies the ranks and the 5,000-block world
border for you, but only when `dev-server/luckperms/ranks.lp` or the border changed since last
time (or the world or database was wiped). `./dev ranks` re-applies them regardless.

If you're in game while they apply, you'll see a burst of `[LP] … already has … set` and
`Nothing changed. The world border is already that size` messages. That's harmless: it's the
console confirming the settings are already in place, echoed to ops. `lp log recent` shows every
change came from `(Console)`.

**The server runs in the background.** Closing your terminal does not stop it. It keeps going
until you run `./dev stop`, or until the computer shuts down.

### The console

The console is where you type server commands like `list`, `op Steve` or `lp user Steve parent add
owner`.

- **Open it:** `./dev console`
- **Type commands without a slash:** `list`, not `/list`
- **Leave it and keep the server running:** press **Ctrl-b**, let go, then press **d**

> Pressing Ctrl-C in the console stops the server. That's fine if you meant to; if not, run
> `./dev start` again.

**One command without opening the console:**

```sh
./dev cmd list
./dev cmd "lp user Steve parent add owner"
```

It prints the server's reply.

### Watching the log

```sh
./dev logs        # follows the log live; Ctrl-C stops watching (the server keeps running)
```

### Making yourself staff on a fresh server

```sh
./dev cmd "lp user YourName parent add owner"
```

`owner` has every permission. The ranks themselves come from `dev-server/luckperms/ranks.lp`.

---

## Changing things

### Where settings live

There are two copies of the config. **Only one of them is kept.**

| Folder | What it is | Kept in git? |
|---|---|---|
| `dev-server/` | The settings we chose: FactionsUUID, RoseStacker, CommandTimer raid windows, LuckPerms ranks, … | **Yes** |
| `run/` | The live server: world, plugin jars, plugin data, and a copy of `dev-server/` | No, it's scratch |

**Edit files in `dev-server/`**, then `./dev restart`. Every start copies `dev-server/` over `run/`.
An edit made straight in `run/plugins/...` works until the next start, then gets overwritten
by the `dev-server/` copy (or, if the file isn't tracked, is simply lost when `run/` is wiped).

Our own plugin's settings (`run/plugins/AvianFactions/*.conf`) are the exception: they're
created with their defaults on first start. [`CONFIGURATION.md`](CONFIGURATION.md) explains every
setting in plain words.

### Changing code

1. Edit the Java code.
2. `./dev restart`. It rebuilds the plugin before starting, so there's no separate build step.
3. If it refuses to start, the reason is printed. See [Troubleshooting](#troubleshooting).

### Running the tests

```sh
./dev test
```

This runs every test and builds the jar. It's the same check CI runs first, and it needs Docker for
the database tests.

---

## Getting a change merged

1. Make a branch: `git switch -c my-change`
2. Commit and push: `git push -u origin my-change`
3. Open a pull request on GitHub.
4. **CI checks it automatically.** Both checks must be green before merging.

### What CI checks

| Check | What it does | Catches |
|---|---|---|
| **Build, test, shade** | Lints `./dev`, runs every test, builds the jar | Broken code, failing tests |
| **Boot the full server stack** | Runs `./dev setup`, `./dev start`, `./dev check-log` and `./dev stop` on a fresh machine, with every plugin and our `dev-server/` config | A plugin that fails to load, a broken config file, a missing hook between plugins |

If the boot check fails, open the failed run on GitHub. The **Show the end of the log** step prints
what went wrong, and the full log is attached as **server-log**.

### Releasing

```sh
git tag -a v0.2.0 -m "What changed" && git push origin v0.2.0
```

CI builds `AvianFactions-0.2.0.jar` and attaches it to a GitHub Release.

---

## Troubleshooting

**"A server is already running outside ./dev (process 12345)"**
Something started the server without `./dev`, like a plain `./gradlew runServer`. If you still
have its console, type `stop` there. If not, `kill 12345` stops it just as cleanly. Then use
`./dev start`.

**"Avian Factions refused to start"**
A setting is invalid. The message names the file and the setting, like
`factions.conf → faction-base-power: must be a finite number >= 0`. Fix it and `./dev restart`.

**"The server did not finish starting"**
Run `./dev console` to see what it's doing. The first start after a Paper or plugin update
downloads files and can be slow. Run `./dev start` again once it's done.

**"The database did not become healthy"**
Is Docker running? Try `docker compose logs mariadb`.

**Port 25565 is already in use**
Another Minecraft server is running on this computer. Stop it, or change `server-port` in
`run/server.properties`.

### Starting over

| Wipe | How | You lose |
|---|---|---|
| The world | `./dev stop`, then `rm -rf run/world*` | The map; factions and plugin data stay |
| All plugin data | `./dev stop`, then `rm -rf run/plugins/*/` | Factions, F-Top, plugin state; the jars stay |
| The database | `./dev stop`, then `docker compose down -v` | Balances, ranks, F-Top history, CoreProtect logs |
| Everything local | all of the above, then `rm -rf run` and `./dev setup` | A fresh start |

None of these touch git.

---

## What's where

| Path | What |
|---|---|
| `dev` | The script this guide is about |
| `dev-server/` | Tracked config for the third-party plugins; see its README |
| `avian-*/` | Our plugin's modules (listed in the main [README](../README.md#layout)) |
| `docs/CONFIGURATION.md` | Every setting, in plain words |
| `docs/adr/` | Why things are the way they are |
| `.github/workflows/ci.yml` | The CI checks above |
