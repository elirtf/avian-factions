package club.avian.factions.combat;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gapple, totem and pearl cooldowns, with the defaults: notch 60 s, golden 10 s, totem 60 s, pearl 16 s. */
class ConsumableCooldownsTest {

    ServerMock server;
    ConsumableCooldowns cooldowns;
    PlayerMock player;
    long now = 1_000_000;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        cooldowns = new ConsumableCooldowns(new HitDelayTest.Handle(), () -> now);
        player = server.addPlayer("Wigby");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Eats one, as the server would: the check, then (if allowed) the record. */
    private boolean eat(Material material) {
        var event = new PlayerItemConsumeEvent(player, new ItemStack(material), EquipmentSlot.HAND);
        cooldowns.onEat(event);
        if (!event.isCancelled()) {
            cooldowns.onAte(event);
        }
        return !event.isCancelled();
    }

    private boolean totemSaves() {
        var event = new EntityResurrectEvent(player, EquipmentSlot.HAND);
        cooldowns.onTotem(event);
        if (!event.isCancelled()) {
            cooldowns.onTotemUsed(event);
        }
        return !event.isCancelled();
    }

    /** Throws one, as the server would: the check, then (if allowed) the record and vanilla's 1 s cooldown. */
    private boolean throwPearl() {
        var pearl = player.getWorld().spawn(player.getLocation(), EnderPearl.class);
        var event = new PlayerLaunchProjectileEvent(player, new ItemStack(Material.ENDER_PEARL), pearl);
        cooldowns.onPearl(event);
        if (!event.isCancelled()) {
            cooldowns.onPearlThrown(event);
        }
        return !event.isCancelled();
    }

    @Test
    void aSecondPearlWaitsSixteenSeconds() {
        assertTrue(throwPearl());
        now += 15_000;
        assertFalse(throwPearl(), "still cooling down at 15 s");
        now += 1_000;
        assertTrue(throwPearl());
    }

    @Test
    void vanillasOneSecondPearlCooldownIsStretchedToOurs() {
        assertTrue(throwPearl());
        now += 4_000;
        var vanilla = new PlayerItemCooldownEvent(player, Material.ENDER_PEARL, Material.ENDER_PEARL.getKey(), 20);
        cooldowns.onVanillaCooldown(vanilla);
        assertEquals(12 * 20, vanilla.getCooldown(), "12 s left, in ticks");
    }

    @Test
    void otherItemCooldownsAreLeftAlone() {
        var shield = new PlayerItemCooldownEvent(player, Material.SHIELD, Material.SHIELD.getKey(), 100);
        cooldowns.onVanillaCooldown(shield);
        assertEquals(100, shield.getCooldown());
    }

    @Test
    void aSecondNotchAppleWaitsAMinute() {
        assertTrue(eat(Material.ENCHANTED_GOLDEN_APPLE));
        now += 59_000;
        assertFalse(eat(Material.ENCHANTED_GOLDEN_APPLE), "still cooling down at 59 s");
        now += 1_000;
        assertTrue(eat(Material.ENCHANTED_GOLDEN_APPLE));
    }

    @Test
    void goldenAndNotchApplesHaveSeparateTimers() {
        assertTrue(eat(Material.ENCHANTED_GOLDEN_APPLE));
        assertTrue(eat(Material.GOLDEN_APPLE), "a notch apple does not block a golden apple");
        assertFalse(eat(Material.GOLDEN_APPLE));
        now += 10_000;
        assertTrue(eat(Material.GOLDEN_APPLE));
    }

    @Test
    void ordinaryFoodHasNoCooldown() {
        assertTrue(eat(Material.COOKED_BEEF));
        assertTrue(eat(Material.COOKED_BEEF));
    }

    @Test
    void aTotemOnCooldownDoesNotSave() {
        assertTrue(totemSaves());
        now += 30_000;
        assertFalse(totemSaves(), "the second pop within a minute lets the player die");
        now += 30_000;
        assertTrue(totemSaves());
    }

    @Test
    void theCooldownOutlivesARelog() {
        assertTrue(eat(Material.ENCHANTED_GOLDEN_APPLE));
        player.disconnect();
        player.reconnect();
        cooldowns.onJoin(new org.bukkit.event.player.PlayerJoinEvent(player, net.kyori.adventure.text.Component.empty()));
        assertFalse(eat(Material.ENCHANTED_GOLDEN_APPLE), "relogging must not reset it");
        assertEquals(60_000, cooldowns.remainingMillis(player.getUniqueId(), Material.ENCHANTED_GOLDEN_APPLE));
    }
}
