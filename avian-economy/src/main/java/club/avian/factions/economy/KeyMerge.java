package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import club.avian.factions.api.text.Ui;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;

/**
 * {@code /mergekeys} (also {@code /keymerge}): trade spare crate keys up a tier (owner, 2026-10-16), e.g. 5 Common
 * for 1 Rare. A menu with one button per step ({@code keymerge.conf}): click merges once, shift-click as many as the
 * player can. Keys are CrazyCrates' virtual keys, read through its placeholders and moved with its own commands.
 * The take is checked before the give, so a failed take never makes a key.
 */
final class KeyMerge implements Listener {

    /** The key store, as merging needs it. The real one is CrazyCrates; tests use a map. */
    interface Keys {
        int virtual(Player player, String crate);

        /** Takes {@code amount}; false (and nothing taken) if the player doesn't have them. */
        boolean take(Player player, String crate, int amount);

        void give(Player player, String crate, int amount);
    }

    private static final int SIZE = 45;
    private static final int INFO = 4;
    private static final int[] SLOTS = {11, 13, 15, 29, 31, 33};
    private static final int CLOSE = 40;

    private final ConfigHandle<KeyMergeConfig> config;
    private final Keys keys;
    private final Logger log;

    KeyMerge(ConfigHandle<KeyMergeConfig> config, Keys keys, Logger log) {
        this.config = config;
        this.keys = keys;
        this.log = log;
    }

    /** CrazyCrates: {@code %crazycrates_<crate>_virtual_raw%}, and its take/give commands as the console. */
    static Keys crazyCrates() {
        return new Keys() {
            @Override
            public int virtual(Player player, String crate) {
                var raw = PlaceholderAPI.setPlaceholders(player, "%crazycrates_" + crate + "_virtual_raw%");
                try {
                    return Integer.parseInt(raw.trim());
                } catch (NumberFormatException e) {
                    return 0;   // unknown crate, or CrazyCrates not answering: nothing to merge
                }
            }

            @Override
            public boolean take(Player player, String crate, int amount) {
                int before = virtual(player, crate);
                if (before < amount) {
                    return false;
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "crazycrates take virtual " + crate + " " + amount + " " + player.getName());
                return virtual(player, crate) == before - amount;
            }

            @Override
            public void give(Player player, String crate, int amount) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "crazycrates give virtual " + crate + " " + amount + " " + player.getName() + " -s");
            }
        };
    }

    /** Merges once, or as often as the keys allow ({@code all}); returns how many keys of the next tier were made. */
    static int merge(Keys keys, Player player, KeyMergeConfig.Step step, boolean all) {
        int have = keys.virtual(player, step.from());
        int times = all ? have / step.cost() : Math.min(1, have / step.cost());
        if (times <= 0 || !keys.take(player, step.from(), times * step.cost())) {
            return 0;
        }
        keys.give(player, step.to(), times);
        return times;
    }

    LiteralCommandNode<CommandSourceStack> command(String name) {
        return Commands.literal(name)
                .executes(ctx -> {
                    if (ctx.getSource().getExecutor() instanceof Player player) {
                        open(player);
                    }
                    return 1;
                })
                .build();
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    void open(Player player) {
        var holder = new Holder();
        holder.inventory = Bukkit.createInventory(holder, SIZE, Ui.title(Ui.UP, "Merge Keys"));
        render(holder.inventory, player);
        player.openInventory(holder.inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.4f);
    }

    private void render(Inventory inv, Player player) {
        inv.clear();
        Ui.accents(inv, Material.YELLOW_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE);
        inv.setItem(INFO, Ui.item(Material.TRIPWIRE_HOOK, "<sun><bold>" + Ui.UP + " Merge Keys</bold></sun>",
                Ui.Tooltip.of("Trade spare crate keys for one", "of the next tier up.")
                        .line("<dim>" + Ui.DOT + "</dim> <soft>Your keys: <white>/keys</white>").lines()));
        var steps = config.get().steps();
        for (int i = 0; i < steps.size() && i < SLOTS.length; i++) {
            inv.setItem(SLOTS[i], button(steps.get(i), keys.virtual(player, steps.get(i).from())));
        }
        inv.setItem(CLOSE, Ui.close());
    }

    private static ItemStack button(KeyMergeConfig.Step step, int have) {
        int possible = have / step.cost();
        var tip = Ui.Tooltip.of("Trade " + step.cost() + " " + label(step.from()) + " keys", "for 1 " + label(step.to()) + " key.")
                .stat("You have", String.valueOf(have), possible > 0 ? "cane" : "bad")
                .stat("You can make", String.valueOf(possible), possible > 0 ? "sun" : "dim");
        if (possible > 0) {
            tip.action("Click", "Merge once").action("Shift-click", "Merge " + possible);
        } else {
            tip.line("<bad>Need " + (step.cost() - have % step.cost()) + " more " + label(step.from()) + ".</bad>");
        }
        var item = Ui.item(possible > 0 ? Material.TRIPWIRE_HOOK : Material.GRAY_DYE,
                "<sun><bold>" + step.cost() + " " + label(step.from()) + " " + Ui.ARROW + " 1 " + label(step.to()) + "</bold></sun>",
                tip.lines());
        return possible > 0 ? Ui.glow(item) : item;
    }

    /** "EnchantCommon" → "Enchant Common". */
    static String label(String crate) {
        return crate.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == CLOSE) {
            player.closeInventory();
            return;
        }
        var steps = config.get().steps();
        for (int i = 0; i < steps.size() && i < SLOTS.length; i++) {
            if (SLOTS[i] != slot) {
                continue;
            }
            var step = steps.get(i);
            int made = merge(keys, player, step, event.isShiftClick());
            if (made > 0) {
                player.sendMessage(Brand.mm("<sun>" + Ui.UP + "</sun> <soft>Merged</soft> <white>" + made * step.cost() + " "
                        + label(step.from()) + "</white> <soft>into</soft> <sun><bold>" + made + " " + label(step.to())
                        + "</bold></sun> <soft>key" + (made == 1 ? "" : "s") + ".</soft>"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
                log.info("keymerge: " + player.getName() + " " + made * step.cost() + " " + step.from() + " -> " + made + " " + step.to());
            } else {
                player.sendMessage(Brand.mm("<bad>Not enough " + label(step.from()) + " keys: you need " + step.cost() + ".</bad>"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
            }
            render(event.getView().getTopInventory(), player);
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    static List<String> commands() {
        return List.of("mergekeys", "keymerge");
    }
}
