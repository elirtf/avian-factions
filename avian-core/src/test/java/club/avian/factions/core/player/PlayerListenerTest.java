package club.avian.factions.core.player;

import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ADR-0005 layer 2: the Bukkit login lifecycle through the real listener, no database. */
class PlayerListenerTest {

    static final Instant T0 = Instant.parse("2026-09-21T12:00:00Z");

    ServerMock server;
    PlayerServiceTest.MemoryRepository repo;
    PlayerService players;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        var plugin = MockBukkit.createMockPlugin();
        repo = new PlayerServiceTest.MemoryRepository();
        players = new PlayerService(repo, Clock.fixed(T0, ZoneOffset.UTC));
        server.getPluginManager().registerEvents(new PlayerListener(players, Logger.getLogger("test")), plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Fires the (async) event from a non-main thread, as the real login thread would, and waits. */
    private AsyncPlayerPreLoginEvent callPreLogin(UUID uuid, String name) throws Exception {
        var event = preLogin(uuid, name);
        var thread = new Thread(() -> server.getPluginManager().callEvent(event), "login-thread");
        thread.start();
        thread.join();
        return event;
    }

    private AsyncPlayerPreLoginEvent preLogin(UUID uuid, String name) throws Exception {
        var loopback = InetAddress.getLoopbackAddress();
        // Only the full constructor is non-deprecated on 26.1.2; no connection object exists in MockBukkit.
        return new AsyncPlayerPreLoginEvent(name, loopback, loopback, uuid, false, server.createProfile(uuid, name), "localhost", null);
    }

    @Test
    void preLoginCreatesAndCachesTheProfile() throws Exception {
        var uuid = UUID.randomUUID();
        var event = callPreLogin(uuid, "Raven");
        assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult());
        assertEquals("Raven", players.online(uuid).orElseThrow().name());
        assertEquals(T0, repo.rows.get(uuid).firstJoinAt());
    }

    @Test
    void repositoryFailureKicksWithAMessageInsteadOfLettingAProfilelessPlayerIn() throws Exception {
        var failing = new PlayerRepository() {
            @Override public CompletableFuture<java.util.Optional<PlayerProfile>> find(UUID uuid) { return CompletableFuture.completedFuture(java.util.Optional.empty()); }
            @Override public CompletableFuture<PlayerProfile> findOrCreate(UUID uuid, String name, Instant now) { return CompletableFuture.failedFuture(new IllegalStateException("db down")); }
            @Override public CompletableFuture<Void> touchLastSeen(UUID uuid, Instant at) { return CompletableFuture.completedFuture(null); }
        };
        var service = new PlayerService(failing, Clock.fixed(T0, ZoneOffset.UTC));
        server.getPluginManager().registerEvents(new PlayerListener(service, Logger.getLogger("test")), MockBukkit.createMockPlugin("other"));
        var uuid = UUID.randomUUID();
        var event = callPreLogin(uuid, "Raven");
        assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, event.getLoginResult());
        assertTrue(service.online(uuid).isEmpty());
    }

    @Test
    void quitEvictsAndRecordsLastSeen() throws Exception {
        var player = server.addPlayer("Raven");
        callPreLogin(player.getUniqueId(), "Raven");
        assertTrue(players.online(player.getUniqueId()).isPresent());
        server.getPluginManager().callEvent(new PlayerQuitEvent(player, (net.kyori.adventure.text.Component) null, PlayerQuitEvent.QuitReason.DISCONNECTED));
        assertTrue(players.online(player.getUniqueId()).isEmpty());
        assertEquals(T0, repo.rows.get(player.getUniqueId()).lastSeenAt());
    }
}
