package club.avian.factions.economy;

import me.gypopo.economyshopgui.api.events.PostTransactionEvent;
import me.gypopo.economyshopgui.util.Transaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.EnumSet;

/**
 * What /shop's Base Building section sells: gen blocks (shop item ids {@code gen_<type>}) and infinite buckets
 * ({@code infinite_water}, {@code infinite_lava}). EconomyShopGUI hands over the plain item, and this swaps it for
 * the real one straight away.
 */
final class BaseBuildingShop implements Listener {

    private static final EnumSet<Transaction.Type> BUYS = EnumSet.of(
            Transaction.Type.BUY_SCREEN, Transaction.Type.BUY_STACKS_SCREEN, Transaction.Type.QUICK_BUY);

    private final GenBlocks gens;

    BaseBuildingShop(GenBlocks gens) {
        this.gens = gens;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBought(PostTransactionEvent event) {
        var item = event.getShopItem();
        if (item == null || !BUYS.contains(event.getTransactionType())
                || (event.getTransactionResult() != Transaction.Result.SUCCESS
                && event.getTransactionResult() != Transaction.Result.SUCCESS_COMMANDS_EXECUTED)) {
            return;
        }
        var path = item.getItemPath();
        var id = path == null ? "" : path.substring(path.lastIndexOf('.') + 1);
        var gen = id.startsWith("gen_") ? gens.create(id.substring(4), event.getAmount())
                : id.startsWith("infinite_") ? InfiniteBuckets.create(id.substring(9), event.getAmount()) : null;
        if (gen == null) {
            return;
        }
        var player = event.getPlayer();
        var plain = item.getItemToGive().clone();
        plain.setAmount(event.getAmount());
        player.getInventory().removeItem(plain);
        player.getInventory().addItem(gen).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }
}
