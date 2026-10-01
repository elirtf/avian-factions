# Issue tracker: Forgejo

Issues and PRDs for this repo live on our Forgejo, **https://git.willowcrest.world/avian/avian-factions**
(moved from GitHub on 2026-09-30 with every issue and PR; GitHub is now only a push mirror). Use
`tools/forgejo` for all operations: it reads the repo from `git remote get-url origin` and the token
from `~/.config/avian/forgejo-token`. Bodies always come from a file or stdin (`-`), never the command line.

## Conventions

- **Create an issue**: `tools/forgejo issue create --title "..." --body-file - [--label a,b] <<'EOF' … EOF`
- **Read an issue**: `tools/forgejo issue view <number>` (body, labels and every comment)
- **List issues**: `tools/forgejo issue list [--state open|closed|all] [--label <name>]`
- **Comment on an issue**: `tools/forgejo issue comment <number> --body-file -`
- **Apply / remove labels**: `tools/forgejo issue label <number> --add a,b --remove c`
- **Close**: `tools/forgejo issue close <number> [--comment-file -]`

## Pull requests

Branches push to `origin` (Forgejo, mirrored to GitHub). Open and merge PRs on Forgejo:
`tools/forgejo pr create --head <branch> --title "..." --body-file -`, `pr view`, `pr list`, `pr checks`,
`pr merge` (a merge commit, branch deleted).

**PRs as a request surface: no.**

## When a skill says "publish to the issue tracker"

Create a Forgejo issue with `tools/forgejo issue create`.

## When a skill says "fetch the relevant ticket"

Run `tools/forgejo issue view <number>`.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a single issue with **child** issues as tickets.

- **Map**: a single issue labelled `wayfinder:map`, holding the Notes / Decisions-so-far / Fog body.
- **Child ticket**: Forgejo has no sub-issues, so add the child to a task list in the map body and put
  `Part of #<map>` at the top of the child body. Labels: `wayfinder:<type>`
  (`research`/`prototype`/`grilling`/`task`).
- **Blocking**: Forgejo's native issue dependencies: `tools/forgejo issue blocked-by <child> <blocker>`.
  A ticket is unblocked when every blocker is closed.
- **Frontier query**: list the map's open children, drop any with an open blocker; first in map order wins.
- **Claim**: assign it to yourself in the Forgejo UI (or note it in a comment) as the session's first write.
- **Resolve**: comment the answer, close the issue, then append a context pointer (gist + link) to the
  map's Decisions-so-far.
