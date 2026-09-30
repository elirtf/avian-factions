package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import dev.kitteh.factions.event.FPlayerJoinEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Starter chunk busters (owner, 2026-09-29): 5 for founding a faction and 5 for each member who joins
 * one. Each player gets them once, ever, whether as founder or member: otherwise creating and
 * disbanding a faction over and over would print busters. The mark is on the player's own data, so
 * it lives and resets with the world.
 */
final class BusterGrants implements Listener {

    static final NamespacedKey GRANTED = new NamespacedKey("avian", "starter_busters_given");

    private final Plugin plugin;
    private final ConfigHandle<FactionsConfig> config;

    BusterGrants(Plugin plugin, ConfigHandle<FactionsConfig> config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(FPlayerJoinEvent event) {
        if (event.getReason() == FPlayerJoinEvent.Reason.COMMAND_FORCE || !event.getFPlayer().isOnline()) {
            return;   // staff moving someone, or an offline player: nothing to hand over
        }
        Player player = event.getFPlayer().asPlayer();
        boolean founded = event.getReason() == FPlayerJoinEvent.Reason.CREATE;
        // A tick later, once FactionsUUID has actually put them in the faction.
        plugin.getServer().getScheduler().runTask(plugin, () -> grant(player, founded));
    }

    /** Gives the starter busters unless this player has had them; true when it gave them. */
    boolean grant(Player player, boolean founded) {
        int amount = config.get().chunkBuster().grantOnJoin();
        if (amount <= 0 || !player.isOnline() || !firstTime(player)) {
            return false;
        }
        player.getInventory().addItem(ChunkBusters.create(amount)).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        player.sendMessage(Brand.mm("<hot><bold>CHUNK BUSTERS!</bold></hot> <soft>" + amount + " for "
                + (founded ? "founding your faction" : "joining your faction")
                + ". Clear room for your base in your own land, far from spawn.</soft>"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        return true;
    }

    /** Marks the player as granted; false if they already were. */
    static boolean firstTime(Player player) {
        var data = player.getPersistentDataContainer();
        if (data.has(GRANTED, PersistentDataType.BYTE)) {
            return false;
        }
        data.set(GRANTED, PersistentDataType.BYTE, (byte) 1);
        return true;
    }
}
