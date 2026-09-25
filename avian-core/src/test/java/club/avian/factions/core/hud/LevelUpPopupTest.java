package club.avian.factions.core.hud;

import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A level gained shows the popup; a level lost, or a blank popup name, shows nothing. */
class LevelUpPopupTest {

    ServerMock server;
    List<String> commands = new ArrayList<>();
    String popup = "avian_levelup";

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void level(int from, int to) {
        var player = server.addPlayer("Wren");
        new LevelUpPopup(() -> popup, commands::add).onLevel(new PlayerLevelChangeEvent(player, from, to));
    }

    @Test
    void gainingALevelShowsThePopup() {
        level(4, 5);
        assertEquals(List.of("hud popup show Wren avian_levelup"), commands);
    }

    @Test
    void losingLevelsShowsNothing() {
        level(5, 0);   // death, or spending levels at an anvil
        assertEquals(List.of(), commands);
    }

    @Test
    void aBlankPopupNameTurnsItOff() {
        popup = "";
        level(4, 5);
        assertEquals(List.of(), commands);
    }
}
