package club.avian.factions.ftop;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The F-Top menu: podium, places 4-10, later pages, and your own faction. */
class FTopMenuTest {

    ServerMock server;
    PlayerMock viewer;
    Ranking ranking;
    FTopMenu menu;

    static final FTopMenu.Factions NO_FUUID = new FTopMenu.Factions() {
        @Override public OfflinePlayer leader(int id) { return null; }
        @Override public int onlineMembers(int id) { return 1; }
        @Override public int members(int id) { return 3; }
        @Override public Integer factionOf(Player player) { return 12; }
    };

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        viewer = server.addPlayer("Wigby");
        var entries = new ArrayList<Ranking.Entry>();
        for (int i = 1; i <= 12; i++) {
            entries.add(new Ranking.Entry(i, new Ranking.Owner(i, "Faction" + i), 1_000_000L / i, 900_000L / i,
                    100_000L / i, Map.of("IRON_GOLEM", 13 - i)));
        }
        ranking = new Ranking(entries, Instant.parse("2026-09-24T10:00:00Z"));
        menu = new FTopMenu(() -> ranking, NO_FUUID, amount -> "$" + String.format("%,d", amount),
                Clock.fixed(Instant.parse("2026-09-24T10:03:00Z"), ZoneOffset.UTC), () -> 5);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private String name(int slot) {
        var item = viewer.getOpenInventory().getTopInventory().getItem(slot);
        return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
    }

    @Test
    void thePodiumHoldsTheTopThreeAndTheRowBelowHoldsFourToTen() {
        menu.open(viewer, 1);
        assertEquals("#1  Faction1", name(13));
        assertEquals("#2  Faction2", name(21));
        assertEquals("#3  Faction3", name(23));
        assertEquals("#4  Faction4", name(28));
        assertEquals("#10  Faction10", name(34));
        assertEquals(Material.GOLD_BLOCK, viewer.getOpenInventory().getTopInventory().getItem(13).getType(),
                "no leader head known: first place is gold");
    }

    @Test
    void elevenAndBelowAreOnPageTwo() {
        assertEquals(2, menu.pages(ranking));
        menu.open(viewer, 2);
        assertEquals("#11  Faction11", name(10));
        assertEquals("#12  Faction12", name(11));
    }

    @Test
    void yourFactionIsShownWhateverItsRank() {
        menu.open(viewer, 1);
        assertEquals("YOUR FACTION  #12", name(49));
    }

    @Test
    void anEmptyRankingSaysHowToGetOnIt() {
        ranking = new Ranking(java.util.List.of(), Instant.EPOCH);
        menu.open(viewer, 1);
        assertEquals("NO FACTION HAS VALUE YET", name(22));
        assertNull(viewer.getOpenInventory().getTopInventory().getItem(50) == null ? null
                : viewer.getOpenInventory().getTopInventory().getItem(50).getType() == Material.ARROW ? "arrow" : null,
                "no next page");
    }

    @Test
    void barsCompareWithFirstPlace() {
        assertTrue(FTopMenu.bar(50, 100).contains("■■■■■</money>"));
        assertEquals("Iron Golem", FTopMenu.pretty("IRON_GOLEM"));
    }
}
