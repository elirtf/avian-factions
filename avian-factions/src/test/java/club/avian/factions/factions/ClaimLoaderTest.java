package club.avian.factions.factions;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Faction land is held while a member is online and released when the last one leaves. */
class ClaimLoaderTest {

    ServerMock server;
    BasePowerListenerTest.Handle config;
    Map<Player, Integer> factionOf = new HashMap<>();
    Map<Integer, List<ClaimLoader.ChunkRef>> claims = new HashMap<>();
    List<String> calls = new ArrayList<>();
    ClaimLoader loader;
    PlayerMock alice, bob, carol;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        config = new BasePowerListenerTest.Handle();
        loader = new ClaimLoader(MockBukkit.createMockPlugin(), config, factionOf::get,
                id -> claims.getOrDefault(id, List.of()), new ClaimLoader.Holder() {
                    @Override public void hold(ClaimLoader.ChunkRef c) { calls.add("hold " + c.x()); }
                    @Override public void release(ClaimLoader.ChunkRef c) { calls.add("release " + c.x()); }
                });
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        carol = server.addPlayer("Carol");
        factionOf.put(alice, 1);
        factionOf.put(bob, 1);
        claims.put(1, List.of(new ClaimLoader.ChunkRef("world", 10, 0), new ClaimLoader.ChunkRef("world", 11, 0)));
        claims.put(2, List.of(new ClaimLoader.ChunkRef("world", 20, 0)));
        factionOf.put(carol, 2);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void landIsHeldWhileAnyMemberIsOnline() {
        loader.reconcile(List.of(alice));
        assertEquals(List.of("hold 10", "hold 11"), calls);
        calls.clear();
        loader.reconcile(List.of(alice, bob));
        assertTrue(calls.isEmpty(), "a second member changes nothing");
        loader.reconcile(List.of(bob));
        assertTrue(calls.isEmpty(), "still one member online");
        loader.reconcile(List.of());
        assertEquals(Set.of("release 10", "release 11"), Set.copyOf(calls));
    }

    @Test
    void otherFactionsAreIndependent() {
        loader.reconcile(List.of(alice, carol));
        assertEquals(3, loader.heldCount());
        calls.clear();
        loader.reconcile(List.of(alice));
        assertEquals(List.of("release 20"), calls);
    }

    @Test
    void newClaimsArePickedUpAndTheCapHolds() {
        loader.reconcile(List.of(alice));
        claims.put(1, IntStream.range(0, 500).mapToObj(i -> new ClaimLoader.ChunkRef("world", i, 5)).toList());
        loader.reconcile(List.of(alice));
        assertEquals(128, loader.heldCount(), "max-chunks-per-faction default");
    }

    @Test
    void playersWithoutAFactionHoldNothing() {
        var dave = server.addPlayer("Dave");
        loader.reconcile(List.of(dave));
        assertEquals(0, loader.heldCount());
    }
}
