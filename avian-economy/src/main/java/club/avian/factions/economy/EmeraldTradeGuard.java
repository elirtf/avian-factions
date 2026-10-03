package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.MerchantRecipe;

/**
 * No emeralds out of trading ({@code economy.conf} {@code emerald-trades}; owner, 2026-10-01). Emeralds
 * sell for money, and villagers pay emeralds for crops, paper and meat, which would turn any farm into
 * free money. Villagers still sell things for emeralds; they just never learn a trade that pays
 * emeralds, and one they learned before this was switched on is refused when used.
 */
record EmeraldTradeGuard(ConfigHandle<EconomyConfig> config) implements Listener {

    static boolean paysEmeralds(MerchantRecipe recipe) {
        return recipe.getResult().getType() == Material.EMERALD;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLearn(VillagerAcquireTradeEvent event) {
        if (!config.get().emeraldTrades() && paysEmeralds(event.getRecipe())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent event) {
        if (!config.get().emeraldTrades() && paysEmeralds(event.getTrade())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Brand.mm("<bad>✖</bad> <soft>Villagers don't pay emeralds here. "
                    + "Emeralds come from <gold>villager spawners</gold> and mining.</soft>"));
        }
    }
}
