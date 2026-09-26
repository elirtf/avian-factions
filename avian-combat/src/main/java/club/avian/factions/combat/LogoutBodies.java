package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * The body a player leaves when they log out in combat (spec §43): a mannequin with their skin,
 * armour and health that stands for {@code logout-body-seconds}.
 *
 * <p>Killed, it drops everything the player carried and the player owes a death, paid on their
 * next join (inventory cleared, then killed, so faction power is lost as on any death). The debt
 * is written to the database before anyone can rejoin, so a restart cannot hand the loot back.
 * If the body outlives its timer, or the player returns first, it vanishes and nothing is lost.
 */
final class LogoutBodies implements Listener {

    private record Body(UUID player, String name, List<ItemStack> items, int exp, Chunk chunk, long expiresAt) {}

    private final Plugin plugin;
    private final ConfigHandle<CombatConfig> config;
    private final LogoutDeathRepository repository;
    private final LongSupplier clock;
    private final Set<UUID> owing;
    private final Map<UUID, Body> bodies = new HashMap<>();      // by mannequin id
    private final Map<UUID, UUID> bodyOf = new HashMap<>();      // player -> mannequin id
    private final Set<UUID> payingNow = new HashSet<>();

    LogoutBodies(Plugin plugin, ConfigHandle<CombatConfig> config, LogoutDeathRepository repository,
                 LongSupplier clockMillis) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.clock = clockMillis;
        this.owing = new HashSet<>(repository.loadPending());
    }

    boolean isBody(Entity entity) {
        return bodies.containsKey(entity.getUniqueId());
    }

    boolean owesDeath(UUID player) {
        return owing.contains(player);
    }

    void spawn(Player player) {
        int seconds = config.get().logoutBodySeconds();
        if (seconds <= 0) {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (var item : player.getInventory().getContents()) {
            if (item != null && !item.isEmpty()) {
                items.add(item.clone());
            }
        }
        int exp = Math.min(player.getLevel() * 7, 100);    // what a player drops on death
        var location = player.getLocation();
        var chunk = location.getChunk();
        chunk.addPluginChunkTicket(plugin);                // the body must not unload with the chunk

        var equipment = player.getEquipment();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        var body = location.getWorld().spawn(location, Mannequin.class, m -> {
            m.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
            m.getEquipment().setArmorContents(copy(equipment.getArmorContents()));
            m.getEquipment().setItemInMainHand(equipment.getItemInMainHand().clone());
            m.getEquipment().setItemInOffHand(equipment.getItemInOffHand().clone());
            m.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            m.setHealth(Math.max(0.5, Math.min(player.getHealth(), maxHealth)));
            m.setFireTicks(player.getFireTicks());
            m.customName(Brand.mm("<bad>☠</bad> <sun>" + player.getName() + "</sun>"));
            m.setCustomNameVisible(true);
            m.setPersistent(false);                         // a restart ends it: the player keeps their things
            m.setRemoveWhenFarAway(false);
        });
        long expiresAt = clock.getAsLong() + seconds * 1000L;
        bodies.put(body.getUniqueId(), new Body(player.getUniqueId(), player.getName(), items, exp, chunk, expiresAt));
        bodyOf.put(player.getUniqueId(), body.getUniqueId());
        describe(body, seconds);
        Bukkit.broadcast(Brand.mm("<bad>☠</bad> <sun>" + player.getName() + "</sun> <soft>logged out in combat. "
                + "Their body stands for <hot>" + seconds + "s</hot></soft> <dim>— kill it for their loot.</dim>"));
    }

    /** Once a second: count bodies down, and let the ones that made it vanish. */
    void tick() {
        long now = clock.getAsLong();
        for (var entry : List.copyOf(bodies.entrySet())) {
            var entity = Bukkit.getEntity(entry.getKey());
            long left = entry.getValue().expiresAt() - now;
            if (entity == null || left <= 0) {
                remove(entry.getKey());
            } else if (entity instanceof Mannequin m) {
                describe(m, (int) ((left + 999) / 1000));
            }
        }
    }

    /** The player is back before their body died: it goes, and they keep everything. */
    boolean returned(Player player) {
        var bodyId = bodyOf.get(player.getUniqueId());
        if (bodyId == null) {
            return false;
        }
        remove(bodyId);
        return true;
    }

    /** Pays the death owed for a killed body: empty inventory and XP, then die. */
    void collect(Player player) {
        UUID id = player.getUniqueId();
        owing.remove(id);
        repository.clear(id);
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        player.setTotalExperience(0);
        payingNow.add(id);
        player.setHealth(0);
        player.sendMessage(Brand.mm("<bad>☠</bad> <soft>Your body was killed while you were logged out in combat."
                + " Its loot went to whoever killed it.</soft>"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBodyDeath(EntityDeathEvent event) {
        var body = bodies.get(event.getEntity().getUniqueId());
        if (body == null) {
            return;
        }
        event.getDrops().clear();
        event.getDrops().addAll(body.items());
        event.setDroppedExp(body.exp());
        remove(event.getEntity().getUniqueId());

        Player killer = event.getEntity().getKiller();
        owing.add(body.player());
        repository.record(body.player(), killer == null ? null : killer.getUniqueId());
        Bukkit.broadcast(Brand.mm("<bad>☠</bad> <sun>" + body.name() + "</sun><soft>'s body was killed"
                + (killer == null ? "" : " by <sun>" + killer.getName() + "</sun>") + ".</soft>"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPayingDeath(PlayerDeathEvent event) {
        if (payingNow.remove(event.getPlayer().getUniqueId())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.deathMessage(Brand.mm("<bad>☠</bad> <sun>" + event.getPlayer().getName()
                    + "</sun> <soft>died for logging out in combat.</soft>"));
        }
    }

    /** Module shutdown: bodies vanish and their players keep everything. */
    void removeAll() {
        List.copyOf(bodies.keySet()).forEach(this::remove);
    }

    private void remove(UUID bodyId) {
        var body = bodies.remove(bodyId);
        if (body == null) {
            return;
        }
        bodyOf.remove(body.player());
        var entity = Bukkit.getEntity(bodyId);
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
        if (bodies.values().stream().noneMatch(b -> b.chunk().equals(body.chunk()))) {
            body.chunk().removePluginChunkTicket(plugin);
        }
    }

    private static void describe(Mannequin body, int secondsLeft) {
        body.setDescription(Brand.mm("<hot>Logged out in combat</hot> <dim>·</dim> <sun>" + secondsLeft + "s</sun>"));
    }

    private static ItemStack[] copy(ItemStack[] items) {
        var out = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            out[i] = items[i] == null ? null : items[i].clone();
        }
        return out;
    }
}
