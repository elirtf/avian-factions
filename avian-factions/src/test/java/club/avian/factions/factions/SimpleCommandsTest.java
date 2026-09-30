package club.avian.factions.factions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            "/f setwarp base       | /f set warp base",
            "/f setwarp base pw    | /f set warp base --password pw",
            "/f delwarp base       | /f set warp base --delete",
            "/f deinvite Wigby     | /f invite Wigby --delete",
            "/f sethome            | /f set home",
            "/f delhome            | /f set home --delete",
            "/f desc Birds of prey | /f set description Birds of prey",
            "/f tag Ravens         | /f set tag Ravens",
            "/f ally Ravens        | /f relation Ravens ally",
            "/f enemy Hawks        | /f relation Hawks enemy",
            "/f truce Owls         | /f relation Owls truce",
            "/f neutral Owls       | /f relation Owls neutral",
            "/f who Wigby          | /f show Wigby",
            "/f who                | /f show",
            "/f                    | /fmenu",
            "/factions             | /fmenu",
            "/f menu               | /fmenu",
            "/f gui                | /fmenu",
            "/f top                | /ftop",
            "/f top 2              | /ftop 2",
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

    @ParameterizedTest
    @ValueSource(strings = {"f", "f info", "f info Ravens", "f who Wigby", "f sethome", "f claim 5",
            "f ally Ravens", "f map height 12", "factions desc Birds of prey", "f show Ravens"})
    void theClientTreeAcceptsTheShortcuts(String typed) {
        var dispatcher = new CommandDispatcher<Object>();
        // A stand-in for FactionsUUID's tree: /f and /factions, only the real subcommand "show".
        for (String root : List.of("f", "factions")) {
            dispatcher.register(LiteralArgumentBuilder.literal(root)
                    .then(LiteralArgumentBuilder.literal("show").executes(c -> 1)
                            .then(RequiredArgumentBuilder.argument("faction", StringArgumentType.word()).executes(c -> 1))));
        }

        SimpleCommands.addShortcuts(dispatcher.getRoot());

        var parse = dispatcher.parse(typed, new Object());
        assertFalse(parse.getReader().canRead(), () -> "unparsed input in: " + typed);
        assertNotNull(parse.getContext().getCommand(), () -> "incomplete command: " + typed);
    }
}
