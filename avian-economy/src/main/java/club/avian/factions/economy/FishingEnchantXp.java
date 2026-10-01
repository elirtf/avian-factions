package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.event.skill.XpGainEvent;
import dev.aurelium.auraskills.api.skill.Skills;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fishing skill XP for the custom fishing enchants (owner, 2026-09-29: enchants that gather should
 * feed the RPG side). Block enchants need nothing: Veinminer, Tunnel and Treefeller break each block
 * through {@code Player#breakBlock}, and AuraSkills counts every one (checked on a scratch server).
 * Fishing is different: XP is paid once per catch, so a doubled catch (Double Catch, AuraSkills' Fisher)
 * and Seasoned Angler earn nothing extra. This adds it:
 *
 * <ol>
 *   <li>at the start of every fish event, forget the player's last fishing XP;</li>
 *   <li>record the fishing XP AuraSkills grants during it ({@link XpGainEvent});</li>
 *   <li>at its end, read the rod and how many items were caught, and one tick later (whatever the
 *       listener order, AuraSkills has paid by then) add the bonus through AuraSkills' normal path (so it
 *       shows), with its amount pinned by {@link #onBonus} so multipliers aren't applied twice.</li>
 * </ol>
 *
 * Its own class so the AuraSkills API only loads when the plugin is installed.
 */
final class FishingEnchantXp implements Listener {

    static final NamespacedKey SEASONED_ANGLER = NamespacedKey.fromString("excellentenchants:seasoned_angler");

    private final Plugin plugin;
    private final ConfigHandle<EconomyConfig> config;
    private final Map<UUID, Double> fishingXp = new HashMap<>();
    /** The exact bonus each player is about to be granted, applied by {@link #onBonus}. */
    private final Map<UUID, Double> pendingBonus = new HashMap<>();

    FishingEnchantXp(Plugin plugin, ConfigHandle<EconomyConfig> config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFishStart(PlayerFishEvent event) {
        fishingXp.remove(event.getPlayer().getUniqueId());
    }

    /**
     * The bonus goes through AuraSkills' normal XP path so it shows in the action bar like any gain (the
     * raw API added it silently, so a double catch looked like it paid once). That path applies XP
     * multipliers, and the amount it gets is already after them, so the bonus's event is set to the
     * exact figure here, first.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBonus(XpGainEvent event) {
        if (event.getSkill() == Skills.FISHING) {
            Double exact = pendingBonus.remove(event.getPlayer().getUniqueId());
            if (exact != null) {
                event.setAmount(exact);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onXp(XpGainEvent event) {
        if (event.getSkill() == Skills.FISHING) {
            fishingXp.merge(event.getPlayer().getUniqueId(), event.getAmount(), Double::sum);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caught)) {
            return;
        }
        var player = event.getPlayer();
        var rod = rod(player.getInventory().getItem(event.getHand() == null ? EquipmentSlot.HAND : event.getHand()),
                player.getInventory().getItemInMainHand());
        // Every item in the catch counts, whatever doubled it: Double Catch, AuraSkills' Fisher, or anything
        // else (owner, 2026-09-30: two Common fish gave 60 XP instead of 120).
        int amount = caught.getItemStack().getAmount();
        int angler = level(rod, SEASONED_ANGLER);
        if (amount < 2 && angler == 0) {
            return;
        }
        var uuid = player.getUniqueId();
        var cfg = config.get();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Double paid = fishingXp.remove(uuid);
            if (paid == null || paid <= 0 || !player.isOnline()) {
                return;
            }
            double extra = FishingXpBonus.of(paid, amount, angler, cfg.doubleCatchSkillXp(), cfg.seasonedAnglerSkillXpPerLevel());
            var user = AuraSkillsApi.get().getUser(uuid);
            if (extra > 0 && user != null) {
                pendingBonus.put(uuid, extra);
                user.addSkillXp(Skills.FISHING, extra);
                pendingBonus.remove(uuid);   // in case another plugin cancelled the event before ours ran
            }
        });
    }

    private static ItemStack rod(ItemStack hand, ItemStack fallback) {
        return hand != null && hand.getType() == Material.FISHING_ROD ? hand : fallback;
    }

    private static int level(ItemStack item, NamespacedKey key) {
        if (item == null || key == null) {
            return 0;
        }
        var enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
        return enchantment == null ? 0 : item.getEnchantmentLevel(enchantment);
    }
}
