package club.avian.factions.economy;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps an open {@code /ah} page up to date. CrazyAuctions draws its shop once and only redraws on
 * the Refresh button, so a listing, sale or bid by someone else never showed until you clicked it.
 * On any of its auction events this re-opens the shop, on the same page and category, for everyone
 * looking at it: exactly what the Refresh button does, so the cursor stays put.
 *
 * <p>CrazyAuctions publishes no API artifact, so this goes through reflection against the pinned
 * build (avian-plugin/build.gradle.kts). If a class or member is missing it logs once and does
 * nothing; the Refresh button still works.
 */
final class AuctionRefresh implements Listener {

    private static final String PACKAGE = "com.badbones69.crazyauctions.";
    private static final List<String> EVENTS = List.of("AuctionListEvent", "AuctionBuyEvent",
            "AuctionCancelledEvent", "AuctionExpireEvent", "AuctionNewBidEvent", "AuctionWinBidEvent");

    private final Plugin owner;
    private final Logger logger;
    private final String shopTitle;
    private final Class<?> menuClass;
    private final Method title;
    private final Method page;
    private final Method openShop;
    private final Map<UUID, ?> shopType;
    private final Map<UUID, ?> shopCategory;
    private boolean pending;

    private AuctionRefresh(Plugin owner, Logger logger, String shopTitle, ClassLoader loader)
            throws ReflectiveOperationException {
        this.owner = owner;
        this.logger = logger;
        this.shopTitle = shopTitle;
        menuClass = loader.loadClass(PACKAGE + "controllers.AuctionMenu");
        title = menuClass.getMethod("getTitle");
        page = menuClass.getMethod("getPageNumber");
        Class<?> gui = loader.loadClass(PACKAGE + "controllers.GuiListener");
        // openShop(Player, ShopType, Category, int); the enums live in two different packages.
        openShop = Arrays.stream(gui.getMethods())
                .filter(m -> m.getName().equals("openShop") && m.getParameterCount() == 4)
                .findFirst().orElseThrow(() -> new NoSuchMethodException("GuiListener.openShop"));
        shopType = staticMap(gui, "shopType");
        shopCategory = staticMap(gui, "shopCategory");
    }

    /** Hooks CrazyAuctions when it is installed. */
    static void install(Plugin owner, Logger logger) {
        Plugin auctions = Bukkit.getPluginManager().getPlugin("CrazyAuctions");
        if (auctions == null) {
            return;
        }
        try {
            var config = YamlConfiguration.loadConfiguration(new File(auctions.getDataFolder(), "config.yml"));
            ClassLoader loader = auctions.getClass().getClassLoader();
            var refresh = new AuctionRefresh(owner, logger, config.getString("Settings.GUIName", ""), loader);
            for (String name : EVENTS) {
                Class<? extends Event> type = loader.loadClass(PACKAGE + "api.events." + name).asSubclass(Event.class);
                Bukkit.getPluginManager().registerEvent(type, refresh, EventPriority.MONITOR,
                        (listener, event) -> refresh.schedule(), owner, true);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.log(Level.WARNING, "Open /ah pages will not update live; CrazyAuctions changed: " + e);
        }
    }

    /** One redraw on the next tick, however many events fire in this one. */
    private void schedule() {
        if (!pending) {
            pending = true;
            Bukkit.getScheduler().runTask(owner, this::refreshViewers);
        }
    }

    private void refreshViewers() {
        pending = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Object holder = player.getOpenInventory().getTopInventory().getHolder(false);
            if (!menuClass.isInstance(holder)) {
                continue;
            }
            try {
                Object type = shopType.get(player.getUniqueId());
                Object category = shopCategory.get(player.getUniqueId());
                if (type != null && category != null && isShopPage((String) title.invoke(holder), shopTitle)) {
                    openShop.invoke(null, player, type, category, (int) page.invoke(holder));
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                logger.log(Level.WARNING, "Could not refresh /ah for " + player.getName(), e);
            }
        }
    }

    /**
     * Whether a CrazyAuctions menu title is a shop page: the configured name followed by the page
     * number. Its other menus (your listings, expired items, the buy and bid screens, and the
     * {@code " #"} admin viewer) must not be re-opened as the shop.
     */
    static boolean isShopPage(String title, String shopTitle) {
        if (shopTitle.isEmpty() || !title.startsWith(shopTitle) || title.length() == shopTitle.length()) {
            return false;
        }
        return title.substring(shopTitle.length()).chars().allMatch(Character::isDigit);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ?> staticMap(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<UUID, ?>) field.get(null);
    }
}
