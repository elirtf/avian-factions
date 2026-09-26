package club.avian.factions.combat;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The combat tag with the defaults: 20 s, the stock blocked-command list. */
class CombatTagListenerTest {

    ServerMock server;
    CombatTagListener listener;
    PlayerMock attacker;
    PlayerMock victim;
    long now = 1_000_000;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        listener = new CombatTagListener(new HitDelayTest.Handle(), new CombatTags(() -> now), null);
        attacker = server.addPlayer("Wigby");
        victim = server.addPlayer("Pip");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @SuppressWarnings("removal")   // the only way to build this event outside the server
    private void hit(boolean cancelledByAnotherPlugin) {
        var event = new EntityDamageByEntityEvent(attacker, victim, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                DamageSource.builder(DamageType.PLAYER_ATTACK).withDirectEntity(attacker).build(), 1.0);
        event.setCancelled(cancelledByAnotherPlugin);
        if (!event.isCancelled()) {
            listener.onHit(event);
        }
    }

    @Test
    void aHitTagsBothPlayers() {
        hit(false);
        assertTrue(listener.isTagged(attacker));
        assertTrue(listener.isTagged(victim));
    }

    @Test
    void aHitAnotherPluginStoppedTagsNobody() {
        hit(true);      // e.g. faction-mates, or a safe zone
        assertFalse(listener.isTagged(attacker));
        assertFalse(listener.isTagged(victim));
    }

    @Test
    void creativePlayersAreNeverTagged() {
        attacker.setGameMode(GameMode.CREATIVE);
        hit(false);
        assertFalse(listener.isTagged(attacker));
        assertTrue(listener.isTagged(victim));
    }

    @Test
    void theTagRunsOutAfterTwentySeconds() {
        hit(false);
        now += 20_000;
        listener.tick();
        assertFalse(listener.isTagged(victim));
    }

    @Test
    void escapeCommandsAreRefusedInCombat() {
        hit(false);
        var home = new PlayerCommandPreprocessEvent(victim, "/home base");
        listener.onCommand(home);
        assertTrue(home.isCancelled());

        var msg = new PlayerCommandPreprocessEvent(victim, "/msg Wigby gg");
        listener.onCommand(msg);
        assertFalse(msg.isCancelled());
    }

    @Test
    void blockedCommandsMatchNamespacesAndSubcommands() {
        assertTrue(listener.blocked("/essentials:spawn"));
        assertTrue(listener.blocked("/F  Home"));
        assertTrue(listener.blocked("/tpa Pip"));
        assertFalse(listener.blocked("/f show"));
        assertFalse(listener.blocked("/homeless"));
    }

    @Test
    void commandTeleportsAreRefusedInCombat() {
        hit(false);
        var to = new Location(victim.getWorld(), 100, 64, 100);
        var tp = new PlayerTeleportEvent(victim, victim.getLocation(), to, PlayerTeleportEvent.TeleportCause.COMMAND);
        listener.onTeleport(tp);
        assertTrue(tp.isCancelled());

        var pearl = new PlayerTeleportEvent(victim, victim.getLocation(), to, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        listener.onTeleport(pearl);
        assertFalse(pearl.isCancelled());
    }
}
