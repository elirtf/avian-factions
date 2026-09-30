package club.avian.factions.cosmetics;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Trails: the default set is valid, colours cycle, and only owned trails can be worn. */
class TrailsTest {

    static final class Handle implements ConfigHandle<TrailsConfig> {
        final TrailsConfig config = new TrailsConfig();

        @Override
        public TrailsConfig get() {
            return config;
        }

        @Override
        public void onReload(Consumer<TrailsConfig> callback) {
        }
    }

    ServerMock server;
    PlayerMock player;
    Trails trails;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        player = server.addPlayer("Wigby");
        trails = new Trails(new Handle());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void theDefaultTrailsAreValid() {
        var errors = new ArrayList<String>();
        TrailsConfig.validate(new TrailsConfig(), (path, message) -> errors.add(path + ": " + message));
        assertEquals(List.of(), errors);
        assertEquals(12, new TrailsConfig().trails().size());
    }

    @Test
    void aTrailYouDontOwnIsNeverWorn() {
        trails.choose(player, "hearts");
        assertEquals(Optional.empty(), trails.chosen(player), "no permission yet");

        player.addAttachment(MockBukkit.createMockPlugin(), "avian.trail.hearts", true);
        assertEquals(Optional.of("hearts"), trails.chosen(player));

        trails.clear(player);
        assertEquals(Optional.empty(), trails.chosen(player));
    }

    @Test
    void rainbowStepsThroughItsColours() {
        var rainbow = new TrailsConfig().trails().get("rainbow");
        var first = (Particle.DustOptions) Trails.data(Particle.DUST, rainbow, 0);
        var second = (Particle.DustOptions) Trails.data(Particle.DUST, rainbow, 1);
        assertEquals(Color.fromRGB(0xFF3B3B), first.getColor());
        assertEquals(Color.fromRGB(0xFF9A3B), second.getColor());
        assertInstanceOf(Particle.DustOptions.class, Trails.data(Particle.DUST, rainbow, 7), "wraps around");
    }

    @Test
    void standingStillIsNotMoving() {
        var world = server.getWorld("world");
        var here = new Location(world, 0, 64, 0);
        assertFalse(Trails.moved(here, here.clone()));
        assertFalse(Trails.moved(null, here));
        assertTrue(Trails.moved(here, here.clone().add(0.3, 0, 0)));
    }

    @Test
    void onlyParticlesTrailsCanDrawAreAccepted() {
        assertFalse(TrailsConfig.drawable(Particle.BLOCK), "block particles need a block, which trails can't supply");
        assertTrue(TrailsConfig.drawable(Particle.FLAME));
    }

    /** A world that records the particles it is asked to spawn. */
    static final class RecordingWorld extends org.mockbukkit.mockbukkit.world.WorldMock {
        final List<Particle> spawned = new ArrayList<>();

        RecordingWorld() {
            super(org.bukkit.Material.GRASS_BLOCK, 64);
            setName("recording");
        }

        @Override
        public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
                                      double offsetZ, double extra, T data) {
            spawned.add(particle);
        }
    }

    @Test
    void aMovingPlayerLeavesTheirTrail() {
        var world = new RecordingWorld();
        server.addWorld(world);
        player.teleport(new Location(world, 0, 65, 0));
        player.addAttachment(MockBukkit.createMockPlugin(), "avian.trail.rainbow", true);
        trails.choose(player, "rainbow");

        trails.tick(List.of(player));                        // first sight: nothing to compare yet
        player.teleport(new Location(world, 1, 65, 0));
        trails.tick(List.of(player));
        assertEquals(List.of(Particle.DUST), world.spawned, "moved: one puff");

        trails.tick(List.of(player));
        assertEquals(1, world.spawned.size(), "standing still: none");
    }
}
