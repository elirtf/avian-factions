package club.avian.factions.economy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class KitBooksCommandTest {

    @Test
    void readsOnlyWeightedBookPrizes(@TempDir Path dir) throws IOException {
        Path crate = dir.resolve("EnchantRare.yml");
        Files.writeString(crate, """
                Crate:
                  Prizes:
                    "1":
                      Weight: 3
                      Commands: ["eenchants book excellentenchants:vampire 1 %player%"]
                    "2":
                      Weight: 1
                      Commands: ["eco give %player% 5"]
                    "3":
                      Commands: ["eenchants book excellentenchants:thunder 1 %player%"]
                """);

        var pool = KitBooksCommand.readPool(crate.toFile());

        assertEquals(List.of(new KitBooksCommand.Book(3, "eenchants book excellentenchants:vampire 1 %player%")), pool);
    }

    @Test
    void picksByWeight() {
        var pool = List.of(new KitBooksCommand.Book(1, "a"), new KitBooksCommand.Book(3, "b"));
        assertEquals("a", KitBooksCommand.pick(pool, 0.0).command());
        assertEquals("a", KitBooksCommand.pick(pool, 0.24).command());
        assertEquals("b", KitBooksCommand.pick(pool, 0.25).command());
        assertEquals("b", KitBooksCommand.pick(pool, 0.999).command());
    }

    @Test
    void givesTheCountForThePlayer() {
        var ran = new ArrayList<String>();
        var pools = Map.of("Rare", List.of(new KitBooksCommand.Book(1, "eenchants book x 1 %player%")));
        var command = new KitBooksCommand(t -> pools.getOrDefault(t, List.of()), ran::add, () -> 0.5);

        command.give("Wigby", "Rare", 3);

        assertEquals(List.of("eenchants book x 1 Wigby", "eenchants book x 1 Wigby", "eenchants book x 1 Wigby"), ran);
    }

    @Test
    void unknownTiersGiveNothing() {
        var command = new KitBooksCommand(t -> List.of(), s -> { }, () -> 0);
        assertFalse(command.give("Wigby", "Rare", 1));
        assertNull(KitBooksCommand.tierName("Epic"));
        assertEquals("Legendary", KitBooksCommand.tierName("legendary"));
    }
}
