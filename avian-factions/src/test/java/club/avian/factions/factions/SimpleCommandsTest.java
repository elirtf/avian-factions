package club.avian.factions.factions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleCommandsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/f claim 5            | /f claim --radius 5",
            "/F CLAIM 3            | /F claim --radius 3",
            "/factions claim auto  | /factions claim --auto",
            "/f claim fill         | /f claim --fill",
            "/f unclaim 2          | /f unclaim --radius 2",
            "/f unclaim all        | /f unclaim --all-territory",
            "/f autoclaim          | /f claim --auto",
            "/f unclaimall         | /f unclaim --all-territory",
            "/f map on             | /f map --auto-on",
            "/f map off            | /f map --auto-off",
            "/f map height 12      | /f map --set-height 12",
            "/f fly auto           | /f fly --auto",
            "/f warp base hunter2  | /f warp base --password hunter2",
            "/f setwarp base pw    | /f setwarp base --password pw",
            "/f delwarp base       | /f setwarp base --delete",
            "/f deinvite Wigby     | /f invite Wigby --delete",
            "/f delhome            | /f sethome --delete",
    })
    void plainFormsBecomeFlags(String typed, String expected) {
        assertEquals(Optional.of(expected), SimpleCommands.rewrite(typed));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/f claim",
            "/f claim --radius 5",
            "/f unclaim --all-territory",
            "/f map",
            "/f warp base",
            "/f create Gooners",
            "/f claim all",
            "/fly auto",
            "/spawn",
    })
    void everythingElsePassesThrough(String typed) {
        assertEquals(Optional.empty(), SimpleCommands.rewrite(typed));
    }
}
