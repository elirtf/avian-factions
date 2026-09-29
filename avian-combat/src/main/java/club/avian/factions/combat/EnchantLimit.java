package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

/**
 * At most {@code max-enchantments-per-item} enchantments on one item, vanilla and custom together
 * (owner, 2026-09-29: spread power across sets instead of one overpowered set). The anvil is the only
 * way to stack enchantments past what a table gives, so an anvil result over the limit is removed:
 * the anvil shows nothing and the player is told why. Custom enchants (ExcellentEnchants) are real
 * registry enchantments, so {@link ItemStack#getEnchantments()} counts them too. Enchanted books are
 * not capped: they only matter once applied, and the item they land on is.
 */
final class EnchantLimit implements Listener {

    private final ConfigHandle<CombatConfig> config;

    EnchantLimit(ConfigHandle<CombatConfig> config) {
        this.config = config;
    }

    /** True when this item carries more enchantments than {@code max} allows (0 = no limit). */
    static boolean overLimit(ItemStack item, int max) {
        if (item == null || max <= 0 || item.getType() == Material.ENCHANTED_BOOK) {
            return false;
        }
        return item.getEnchantments().size() > max;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvil(PrepareAnvilEvent event) {
        int max = config.get().maxEnchantmentsPerItem();
        if (!overLimit(event.getResult(), max)) {
            return;
        }
        event.setResult(null);
        if (event.getView().getPlayer() instanceof Player player) {
            player.sendActionBar(Brand.mm("<bad>✖</bad> <soft>An item can hold at most</soft> <gem>" + max
                    + " enchantments</gem><soft>.</soft>"));
        }
    }
}
