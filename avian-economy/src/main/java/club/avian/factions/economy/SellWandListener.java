package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.economy.SellValues;
import club.avian.factions.api.text.Brand;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Right-click a container with a sell wand: everything in it that {@code /shop} buys is sold at the
 * player's own shop price (their multipliers and limits), times the wand's multiplier, and paid
 * through the audited economy as {@code sellwand:<tier>}. Sneak to see the value without selling.
 *
 * <p>Protection is whatever every other plugin already enforces: this runs after them (HIGH) and
 * does nothing when the click was denied, so a chest in someone else's claim, or a WorldGuard
 * region, cannot be sold from any more than it can be opened.
 */
final class SellWandListener implements Listener {

    /** What a container would sell for: money, and how many items. */
    record Quote(long money, int items) {}

    private final Economy economy;
    private final SellValues sellValues;
    private final ConfigHandle<SellWandConfig> config;
    private final LongSupplier clock;
    private final Logger log;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    SellWandListener(Economy economy, SellValues sellValues, ConfigHandle<SellWandConfig> config,
                     LongSupplier clockMillis, Logger log) {
        this.economy = economy;
        this.sellValues = sellValues;
        this.config = config;
        this.clock = clockMillis;
        this.log = log;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        var wand = event.getItem();
        var tierId = SellWand.tierId(wand);
        if (tierId.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        var inventory = inventoryOf(event.getClickedBlock(), player);
        if (inventory == null) {
            return;     // not a container: the wand does nothing here
        }
        boolean denied = event.useInteractedBlock() == Event.Result.DENY;
        event.setCancelled(true);   // never open the container with a wand in hand
        if (denied) {
            player.sendMessage(Brand.mm("<bad>✖</bad> <soft>You can't sell from a container you can't open.</soft>"));
            return;
        }
        var tier = config.get().tier(tierId.get());
        if (tier == null) {
            player.sendMessage(Brand.mm("<bad>✖</bad> <soft>This wand's tier no longer exists. Ask staff to replace it.</soft>"));
            return;
        }
        long now = clock.getAsLong();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < config.get().cooldownMillis()) {
            return;
        }
        lastUse.put(player.getUniqueId(), now);

        if (player.isSneaking()) {
            var quote = quote(player, inventory, tier.multiplier());
            player.sendMessage(quote.items() == 0
                    ? Brand.mm("<soft>Nothing in there sells.</soft>")
                    : Brand.mm("<soft>Worth</soft> <money>$" + number(quote.money()) + "</money> <soft>for</soft> <white>"
                            + number(quote.items()) + "</white> <soft>items</soft> <dim>(×"
                            + SellWand.multiplierText(tier.multiplier()) + ")</dim>"));
            return;
        }

        var sold = sell(player, inventory, tier.multiplier());
        if (sold.items() == 0) {
            player.sendMessage(Brand.mm("<soft>Nothing in there sells.</soft>"));
            return;
        }
        economy.deposit(player.getUniqueId(), Currency.MONEY, sold.money(), "sellwand:" + tierId.get())
                .exceptionally(t -> {
                    log.log(Level.SEVERE, "Sell wand payout of $" + sold.money() + " to " + player.getName()
                            + " failed; the items were already taken", t);
                    return null;
                });
        player.sendMessage(Brand.mm("<money>+$" + number(sold.money()) + "</money> <soft>for</soft> <white>"
                + number(sold.items()) + "</white> <soft>items</soft> <dim>(×"
                + SellWand.multiplierText(tier.multiplier()) + ")</dim>"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);

        if (!SellWand.recordSale(wand, sold.money(), tier)) {
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            player.sendMessage(Brand.mm("<hot>Your sell wand crumbles after its last sale.</hot>"));
        }
    }

    /** What selling this inventory would pay, without touching it. */
    Quote quote(Player player, Inventory inventory, int multiplierPercent) {
        long money = 0;
        int items = 0;
        for (var stack : inventory.getContents()) {
            long price = priceOf(player, stack, multiplierPercent);
            if (price > 0) {
                money += price;
                items += stack.getAmount();
            }
        }
        return new Quote(money, items);
    }

    /** Sells everything the shop buys out of this inventory; returns what it paid for. */
    Quote sell(Player player, Inventory inventory, int multiplierPercent) {
        long money = 0;
        int items = 0;
        var contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            var stack = contents[slot];
            long price = priceOf(player, stack, multiplierPercent);
            if (price > 0) {
                money += price;
                items += stack.getAmount();
                inventory.setItem(slot, null);
            }
        }
        return new Quote(money, items);
    }

    private long priceOf(Player player, ItemStack stack, int multiplierPercent) {
        if (stack == null || stack.getType().isAir() || isCustom(stack)) {
            return 0;
        }
        return SellValues.applyMultiplier(sellValues.priceFor(player, stack), multiplierPercent * 100);
    }

    /** Our own items (Harvester Hoe, sell wands, …) are never sold, whatever their base material. */
    private static boolean isCustom(ItemStack stack) {
        return stack.hasItemMeta() && stack.getPersistentDataContainer().getKeys().stream()
                .anyMatch(key -> key.getNamespace().equals("avian"));
    }

    private static Inventory inventoryOf(Block block, Player player) {
        if (block == null) {
            return null;
        }
        if (block.getType() == Material.ENDER_CHEST) {
            return player.getEnderChest();
        }
        return block.getState() instanceof Container container ? container.getInventory() : null;
    }

    private static String number(long n) {
        return new DecimalFormat("#,##0").format(n);
    }
}
