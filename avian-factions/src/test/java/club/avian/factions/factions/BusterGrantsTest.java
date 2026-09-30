package club.avian.factions.factions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Starter chunk busters: 5 on founding or joining a faction, once per player ever. */
class BusterGrantsTest {

    ServerMock server;
    PlayerMock player;
    BusterGrants grants;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        player = server.addPlayer("Wigby");
        grants = new BusterGrants(MockBukkit.createMockPlugin(), new BasePowerListenerTest.Handle());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private int busters() {
        return Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull)
                .filter(ChunkBusters::is).mapToInt(i -> i.getAmount()).sum();
    }

    @Test
    void foundingGivesFive() {
        assertTrue(grants.grant(player, true));
        assertEquals(5, busters());
    }

    @Test
    void onlyOnceEvenAfterLeavingAndJoiningAgain() {
        grants.grant(player, true);
        assertFalse(grants.grant(player, false), "joining another faction later gives nothing");
        assertFalse(grants.grant(player, true), "nor does founding a new one");
        assertEquals(5, busters());
    }
}
