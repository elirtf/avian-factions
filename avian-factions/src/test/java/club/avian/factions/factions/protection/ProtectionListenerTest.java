package club.avian.factions.factions.protection;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.faction.Claims;
import club.avian.factions.api.faction.Faction;
import club.avian.factions.api.faction.Territory;
import club.avian.factions.factions.FactionsConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ADR-0005 layer 2: the real Bukkit events through the real listeners, on a mock server.
 *
 * <p>This is the regression suite #4 asks for — "deny for enemy, allow for member and bypass" per
 * event — without needing two people on a live server.
 */
class ProtectionListenerTest {

    static final class Handle implements ConfigHandle<FactionsConfig> {
        final FactionsConfig config = new FactionsConfig();
        @Override public FactionsConfig get() { return config; }
        @Override public void onReload(Consumer<FactionsConfig> callback) { }
    }

    /** A claim map the test fills by chunk coordinate. */
    static final class StubClaims implements Claims {
        final Map<Long, Territory> byChunk = new HashMap<>();

        @Override public Territory at(World world, int chunkX, int chunkZ) {
            return byChunk.getOrDefault(key(chunkX, chunkZ), Territory.wilderness());
        }

        @Override public int countOf(UUID factionId) { return 0; }

        @Override public int total() { return byChunk.size(); }

        void put(int chunkX, int chunkZ, Territory territory) {
            byChunk.put(key(chunkX, chunkZ), territory);
        }

        private static long key(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }
    }

    ServerMock server;
    WorldMock world;
    StubClaims claims;
    Handle config;
    Set<UUID> bypassing;
    Set<UUID> raidable;
    Faction ravens;
    Player member;
    Player enemy;
    Player staff;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        claims = new StubClaims();
        config = new Handle();
        bypassing = new HashSet<>();
        raidable = new HashSet<>();

        member = server.addPlayer("Member");
        enemy = server.addPlayer("Enemy");
        staff = server.addPlayer("Staff");
        bypassing.add(staff.getUniqueId());

        ravens = new club.avian.factions.factions.FactionRecord(UUID.randomUUID(), "Ravens",
                member.getUniqueId(),
                Map.of(member.getUniqueId(), club.avian.factions.api.faction.FactionRank.LEADER),
                1, java.time.Instant.EPOCH);

        var policy = new FactionProtectionPolicy(config, bypassing::contains,
                (uuid, faction) -> faction.hasMember(uuid),
                faction -> raidable.contains(faction.id()));
        var guard = new ProtectionGuard(claims, policy, new DenyFeedback(config));
        var plugin = MockBukkit.createMockPlugin();
        server.getPluginManager().registerEvents(new BlockProtectionListener(guard, config), plugin);
        server.getPluginManager().registerEvents(new InteractProtectionListener(guard), plugin);
        server.getPluginManager().registerEvents(new EntityProtectionListener(guard), plugin);

        // Chunk 0,0 belongs to Ravens; everything else is wilderness.
        claims.put(0, 0, factionTerritory(ravens));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    static Territory factionTerritory(Faction faction) {
        return new Territory() {
            @Override public Kind kind() { return Kind.FACTION; }
            @Override public Optional<Faction> faction() { return Optional.of(faction); }
        };
    }

    static Territory safezone() {
        return new Territory() {
            @Override public Kind kind() { return Kind.SAFEZONE; }
            @Override public Optional<Faction> faction() { return Optional.empty(); }
        };
    }

    /** A block inside the Ravens claim (chunk 0,0). */
    private Block claimedBlock() {
        return world.getBlockAt(5, 64, 5);
    }

    /** A block in wilderness (chunk 10,10). */
    private Block wildBlock() {
        return world.getBlockAt(170, 64, 170);
    }

    private boolean breakDenied(Player player, Block block) {
        var event = new BlockBreakEvent(block, player);
        server.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    // --- block break / place -----------------------------------------------------------------

    @Test
    void enemiesCannotBreakBlocksInAClaim() {
        assertTrue(breakDenied(enemy, claimedBlock()));
    }

    @Test
    void membersCanBreakBlocksInTheirOwnClaim() {
        assertFalse(breakDenied(member, claimedBlock()));
    }

    @Test
    void staffBypassTheClaim() {
        assertFalse(breakDenied(staff, claimedBlock()));
    }

    @Test
    void anyoneCanBreakBlocksInWilderness() {
        assertFalse(breakDenied(enemy, wildBlock()));
    }

    @Test
    void enemiesCanBreakBlocksInARaidableClaim() {
        raidable.add(ravens.id());
        assertFalse(breakDenied(enemy, claimedBlock()), "overclaimed factions lose protection");
    }

    @Test
    void enemiesCannotPlaceBlocksInAClaim() {
        var block = claimedBlock();
        var against = world.getBlockAt(5, 63, 5);
        var event = new BlockPlaceEvent(block, block.getState(), against,
                enemy.getInventory().getItemInMainHand(), enemy, true, org.bukkit.inventory.EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
    }

    // --- containers and doors -----------------------------------------------------------------

    private boolean interactDenied(Player player, Block block) {
        var event = new PlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItemInMainHand(), block, org.bukkit.block.BlockFace.UP);
        server.getPluginManager().callEvent(event);
        // PlayerInteractEvent#isCancelled is deprecated; the block-use result is the real signal.
        return event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY;
    }

    @Test
    void enemiesCannotOpenChestsInAClaim() {
        var chest = claimedBlock();
        chest.setType(Material.CHEST);
        assertTrue(interactDenied(enemy, chest));
        assertFalse(interactDenied(member, chest), "members open their own chests");
    }

    @Test
    void theNonMemberAllowListCanOpenDoorsWithoutOpeningChests() throws Exception {
        var field = ProtectionConfig.class.getDeclaredField("nonMemberAllowed");
        field.setAccessible(true);
        field.set(config.get().protection(), java.util.List.of("DOOR"));
        var parsed = ProtectionConfig.class.getDeclaredField("parsed");
        parsed.setAccessible(true);
        parsed.set(config.get().protection(), null);

        var door = claimedBlock();
        door.setType(Material.OAK_DOOR);
        assertFalse(interactDenied(enemy, door), "doors were opened to non-members");

        var chest = world.getBlockAt(6, 64, 6);
        chest.setType(Material.CHEST);
        assertTrue(interactDenied(enemy, chest), "chests were not");
    }

    // --- pvp --------------------------------------------------------------------------------

    /**
     * Every public constructor of EntityDamageByEntityEvent is deprecated on 26.1.2 — Paper gives
     * no supported way to synthesise one — so the suppression is scoped to this test helper. The
     * listener under test consumes the event through non-deprecated API.
     */
    @SuppressWarnings({"deprecation", "removal"})
    private boolean damageDenied(Player attacker, Player victim) {
        var event = new EntityDamageByEntityEvent(attacker, victim,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.PLAYER_ATTACK).build(), 1.0);
        server.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    @Test
    void pvpIsAllowedInsideAClaim() {
        member.teleport(new Location(world, 5, 64, 5));
        enemy.teleport(new Location(world, 6, 64, 6));
        assertFalse(damageDenied(enemy, member), "claims protect blocks, not players");
    }

    @Test
    void pvpIsDeniedWhenTheVictimStandsInASafezone() {
        claims.put(10, 10, safezone());
        member.teleport(new Location(world, 170, 64, 170));
        enemy.teleport(new Location(world, 5, 64, 5));
        assertTrue(damageDenied(enemy, member));
    }

    @Test
    void shootingOutOfASafezoneIsAlsoDenied() {
        claims.put(10, 10, safezone());
        enemy.teleport(new Location(world, 170, 64, 170));
        member.teleport(new Location(world, 5, 64, 5));
        assertTrue(damageDenied(enemy, member), "spawn must not be a firing position");
    }
}
