package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import net.milkbowl.vault.economy.EconomyResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Vault bridge, exercised through Vault's own interface. This is what EssentialsX,
 * EconomyShopGUI and every other Vault-aware plugin call.
 */
class VaultEconomyBridgeTest {

    static final class Handle implements club.avian.factions.api.config.ConfigHandle<EconomyConfig> {
        final EconomyConfig config = new EconomyConfig();
        @Override public EconomyConfig get() { return config; }
        @Override public void onReload(Consumer<EconomyConfig> callback) { }
    }

    ServerMock server;
    EconomyServiceTest.MemoryRepository repo;
    EconomyService economy;
    VaultEconomyBridge bridge;
    UUID alice;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        repo = new EconomyServiceTest.MemoryRepository();
        economy = new EconomyService(repo, new Handle(), 1);
        bridge = new VaultEconomyBridge(economy, "Avian");
        alice = server.addPlayer("Alice").getUniqueId();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void reportsWholeDollarsWithNoFractionalDigits() {
        assertEquals(0, bridge.fractionalDigits(), "money has no cents");
        assertEquals("Avian Economy", bridge.getName());
        assertFalse(bridge.hasBankSupport(), "faction banks are ours, not Vault's");
    }

    @Test
    void depositThroughVaultLandsInOurEconomy() {
        var response = bridge.depositPlayer("Alice", 500);
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type);
        assertEquals(500, response.balance);
        assertEquals(500, economy.balance(alice, Currency.MONEY), "the money is in our ledger");
        assertEquals(1, repo.audit.size(), "and it was audited");
        assertTrue(repo.audit.getFirst().endsWith("vault"), "with the vault reason: " + repo.audit);
    }

    @Test
    void withdrawThroughVaultTakesFromOurEconomy() {
        bridge.depositPlayer("Alice", 500);
        var response = bridge.withdrawPlayer("Alice", 200);
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type);
        assertEquals(300, response.balance);
        assertEquals(300, economy.balance(alice, Currency.MONEY));
    }

    @Test
    void overdraftThroughVaultFailsWithoutChangingTheBalance() {
        bridge.depositPlayer("Alice", 100);
        var response = bridge.withdrawPlayer("Alice", 900);
        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type);
        assertEquals(100, response.balance, "the balance Vault reports back is the real one");
        assertEquals(100, economy.balance(alice, Currency.MONEY));
    }

    @Test
    void balanceAndHasReadFromOurCache() {
        bridge.depositPlayer("Alice", 750);
        assertEquals(750, bridge.getBalance("Alice"));
        assertTrue(bridge.has("Alice", 750));
        assertFalse(bridge.has("Alice", 751));
    }

    @Test
    void anUnknownPlayerFailsRatherThanCreatingMoney() {
        var response = bridge.depositPlayer("NeverSeen", 100);
        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type);
        assertEquals(0, bridge.getBalance("NeverSeen"));
        assertEquals(0, repo.audit.size(), "nothing was written for a player we cannot resolve");
    }

    @Test
    void banksAreReportedUnsupportedRatherThanSilentlySucceeding() {
        assertEquals(EconomyResponse.ResponseType.NOT_IMPLEMENTED, bridge.createBank("x", "Alice").type);
        assertEquals(EconomyResponse.ResponseType.NOT_IMPLEMENTED, bridge.bankDeposit("x", 10).type);
        assertEquals(Map.of().size(), bridge.getBanks().size());
    }

    @Test
    void formatMatchesOurWholeDollarFormatting() {
        assertEquals("$1,640", bridge.format(1_640));
    }

    // --- accounts keyed by OfflinePlayer: how FactionsUUID stores faction banks --------------------

    /** A faction bank: a UUID the server has never seen as a real player. */
    private org.bukkit.OfflinePlayer factionBank() {
        return server.getOfflinePlayer(UUID.randomUUID());
    }

    @Test
    void aFactionBankAccountExistsEvenThoughNoPlayerHasThatUuid() {
        assertTrue(bridge.hasAccount(factionBank()),
                "answering false here made FactionsUUID loop into a StackOverflowError on /f claim");
    }

    /**
     * FactionsUUID's Econ.checkStatus: if the account is missing it sets the balance, and setting the
     * balance reads it, which checks status again. It terminates only if hasAccount becomes true.
     */
    @Test
    void factionsUuidsAccountCheckTerminates() {
        var bank = factionBank();
        int depth = 0;
        while (!bridge.hasAccount(bank)) {
            bridge.createPlayerAccount(bank);
            assertTrue(++depth < 5, "hasAccount never became true: this is the loop that crashed /f claim");
        }
        assertEquals(0, bridge.getBalance(bank));
    }

    @Test
    void aFactionBankCanBePaidIntoAndChargedByUuid() {
        var bank = factionBank();
        assertEquals(EconomyResponse.ResponseType.SUCCESS, bridge.depositPlayer(bank, 300).type);
        assertEquals(300, bridge.getBalance(bank));
        assertTrue(bridge.has(bank, 300));

        var claimCost = bridge.withdrawPlayer(bank, 120);
        assertEquals(EconomyResponse.ResponseType.SUCCESS, claimCost.type);
        assertEquals(180, bridge.getBalance(bank));
        assertEquals(180, economy.balance(bank.getUniqueId(), Currency.MONEY), "stored in our ledger by UUID");
    }

    @Test
    void aFactionBankCannotBeOverdrawn() {
        var bank = factionBank();
        bridge.depositPlayer(bank, 50);
        assertEquals(EconomyResponse.ResponseType.FAILURE, bridge.withdrawPlayer(bank, 500).type);
        assertEquals(50, bridge.getBalance(bank));
    }

    @Test
    void zeroAmountsSucceedWithoutWritingAnything() {
        var bank = factionBank();
        int before = repo.audit.size();
        assertEquals(EconomyResponse.ResponseType.SUCCESS, bridge.depositPlayer(bank, 0).type);
        assertEquals(EconomyResponse.ResponseType.SUCCESS, bridge.withdrawPlayer(bank, 0).type);
        assertEquals(before, repo.audit.size(), "zeroing an account is not a transaction");
    }

    @Test
    void aRealPlayerIsTheSameAccountByNameOrByUuid() {
        bridge.depositPlayer(server.getOfflinePlayer(alice), 75);
        assertEquals(75, bridge.getBalance("Alice"), "both halves of the Vault API see one balance");
    }
}
