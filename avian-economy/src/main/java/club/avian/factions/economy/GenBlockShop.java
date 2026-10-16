package club.avian.factions.economy;

import me.gypopo.economyshopgui.api.events.PostTransactionEvent;
import me.gypopo.economyshopgui.util.Transaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.EnumSet;

/**
 * Gen blocks bought in /shop's Base Building section (shop item ids {@code gen_<type>}): EconomyShopGUI hands
 * over plain blocks, and this swaps them for the real gen blocks straight away.
 */
final class GenBlockShop implements Listener {

    private static final EnumSet<Transaction.Type> BUYS = EnumSet.of(
            Transaction.Type.BUY_SCREEN, Transaction.Type.BUY_STACKS_SCREEN, Transaction.Type.QUICK_BUY);

    private final GenBlocks gens;

    GenBlockShop(GenBlocks gens) {
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
        if (!id.startsWith("gen_")) {
            return;
        }
        var gen = gens.create(id.substring(4), event.getAmount());
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
