package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The %avian_…% values the chat hover card shows. */
class AvianPlaceholdersTest {

    ServerMock server;
    EconomyService economy;
    AvianPlaceholders placeholders;
    OfflinePlayer alice;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        economy = new EconomyService(new EconomyServiceTest.MemoryRepository(), new VaultEconomyBridgeTest.Handle(), 1);
        placeholders = new AvianPlaceholders(economy);
        alice = server.addPlayer("Alice");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void balancesAreFormattedForReading() {
        economy.deposit(alice.getUniqueId(), Currency.MONEY, 1_640, "test").join();
        economy.deposit(alice.getUniqueId(), Currency.TOKENS, 12_000, "test").join();
        assertEquals("$1,640", placeholders.onRequest(alice, "balance"));
        assertEquals("12,000", placeholders.onRequest(alice, "tokens"));
        assertEquals("0", placeholders.onRequest(alice, "gems"), "no row reads as zero");
    }

    @Test
    void nameIsTheAccountNameForClickToMessage() {
        assertEquals("Alice", placeholders.onRequest(alice, "name"));
    }

    @Test
    void withoutLuckPermsTheRankIsBlankRatherThanBroken() {
        assertEquals("", placeholders.onRequest(alice, "rank"));
    }

    @Test
    void unknownPlaceholdersAreLeftAlone() {
        assertNull(placeholders.onRequest(alice, "nope"), "null tells PlaceholderAPI to leave the text as typed");
    }
}
