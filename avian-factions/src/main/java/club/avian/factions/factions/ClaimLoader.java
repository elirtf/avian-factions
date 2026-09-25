package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Keeps a faction's land loaded while any of its members is online, so crops and farms keep growing
 * while they are elsewhere (on 26.1 a plugin-ticketed chunk random-ticks with no player near it).
 * When the last member logs off, the land is let go.
 *
 * <p>Held with plugin chunk tickets, loaded asynchronously so a big faction logging in never
 * stutters the server. Kept in step on join and quit and every {@code reconcile-seconds}, which also
 * picks up claims, unclaims and members joining or leaving factions.
 */
final class ClaimLoader implements Listener {

    /** A chunk to hold. */
    record ChunkRef(String world, int x, int z) {
    }

    /** Loads-and-holds or releases one chunk. */
    interface Holder {
        void hold(ChunkRef chunk);

        void release(ChunkRef chunk);
    }

    private final Plugin plugin;
    private final ConfigHandle<FactionsConfig> config;
    /** Faction id of an online player, or null if they have none. */
    private final Function<Player, Integer> factionOf;
    /** A faction's claimed chunks. */
    private final Function<Integer, Collection<ChunkRef>> claimsOf;
    private final Holder holder;
    private final Set<ChunkRef> held = new HashSet<>();

    ClaimLoader(Plugin plugin, ConfigHandle<FactionsConfig> config, Function<Player, Integer> factionOf,
                Function<Integer, Collection<ChunkRef>> claimsOf, Holder holder) {
        this.plugin = plugin;
        this.config = config;
        this.factionOf = factionOf;
        this.claimsOf = claimsOf;
        this.holder = holder;
    }

    /** The real holder: async load, then a plugin ticket; release removes the ticket. */
    static Holder tickets(Plugin plugin) {
        return new Holder() {
            @Override
            public void hold(ChunkRef c) {
                var world = Bukkit.getWorld(c.world());
                if (world != null) {
                    world.getChunkAtAsync(c.x(), c.z()).thenAccept(chunk -> chunk.addPluginChunkTicket(plugin));
                }
            }

            @Override
            public void release(ChunkRef c) {
                var world = Bukkit.getWorld(c.world());
                if (world != null) {
                    world.removePluginChunkTicket(c.x(), c.z(), plugin);
                }
            }
        };
    }

    /** Starts the periodic check. */
    void start() {
        long period = Math.max(5, config.get().claimLoading().reconcileSeconds()) * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> reconcile(Bukkit.getOnlinePlayers()), 40L, period);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> reconcile(Bukkit.getOnlinePlayers()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID gone = event.getPlayer().getUniqueId();
        // The quitting player is still "online" during this event; leave them out.
        Bukkit.getScheduler().runTask(plugin, () -> reconcile(Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(gone)).toList()));
    }

    /** Holds exactly the claims of factions with someone online (capped per faction); releases the rest. */
    void reconcile(Collection<? extends Player> online) {
        var cfg = config.get().claimLoading();
        var want = new LinkedHashSet<ChunkRef>();
        if (cfg.enabled()) {
            var factions = new LinkedHashSet<Integer>();
            for (var player : online) {
                var id = factionOf.apply(player);
                if (id != null) {
                    factions.add(id);
                }
            }
            for (var id : factions) {
                claimsOf.apply(id).stream().limit(cfg.maxChunksPerFaction()).forEach(want::add);
            }
        }
        for (var it = held.iterator(); it.hasNext(); ) {
            var c = it.next();
            if (!want.contains(c)) {
                holder.release(c);
                it.remove();
            }
        }
        for (var c : want) {
            if (held.add(c)) {
                holder.hold(c);
            }
        }
    }

    /** Lets everything go (plugin disable). */
    void releaseAll() {
        held.forEach(holder::release);
        held.clear();
    }

    int heldCount() {
        return held.size();
    }
}
