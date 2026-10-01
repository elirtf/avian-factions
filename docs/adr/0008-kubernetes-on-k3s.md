---
status: accepted
date: 2026-09-30
---
# Run the network on Kubernetes, on k3s

The server network moves to **Kubernetes on k3s**, proven first on **k3d** (k3s in Docker) on the dev
box, then installed on the bigger machine. Manifests live in `deploy/kubernetes` as a kustomize base
with two overlays, `k3d` and `production`; `./dev k8s` drives both.

**Why Kubernetes at all.** A Minecraft server is one process with one world writer, so Kubernetes
doesn't scale it out. What it buys us: the server restarts itself, an update is a new image applied
declaratively, backups become scheduled jobs, and the planned network (a Velocity proxy in front of a
hub server and Avian Factions) is three workloads in one place instead of hand-run processes. It
also closes the "Before production" item about the server running as a `docker`-group user: in a
pod it runs as an unprivileged user with no host access.

**Why k3s rather than upstream Kubernetes (kubeadm).** Both are certified Kubernetes: the same API,
so the manifests carry over either way. The difference is what you assemble yourself:

- k3s ships a load balancer (ServiceLB), so our Service publishing TCP 25565 and UDP 19132 works on a
  home machine. Upstream leaves it pending until MetalLB or similar is installed.
- k3s ships storage (local-path) for the server's and database's volumes; upstream needs a provisioner.
- k3s is one binary and about 0.5 GB of memory; kubeadm runs separate control-plane components, about
  2 GB before any workload, and is designed for multi-node clusters run by an operations team.

We have one box now and maybe a few later. k3s still grows: worker nodes join with one command, and
three servers give it high availability. Managed cloud Kubernetes was set aside: it costs monthly and
adds latency for players. If we ever move, the same manifests apply.

**Considered:** Docker Compose for production (already works, `--profile server`). Rejected for the
network because restarts, rollouts and several servers behind a proxy are exactly what it doesn't do;
it stays the simplest way to run one server.

**Consequences:**

- k3d tests use the same k3s version as production (pinned in `deploy/k3d/cluster.yaml`), bound to
  127.0.0.1 on ports 25700 and 19700 so they never touch the live dev server.
- Production's volumes use `local-path-retain`, so a world or database survives a deleted claim.
- CI renders both overlays and validates them with kubeconform on every push.
- The Avian Factions server keeps every world it hosts (spawn and warzone, the resource world, later
  the darkzone and the flat claiming world) on its one volume; the hub and Velocity are separate
  workloads added later.
