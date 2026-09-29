package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The token shop's charge-then-reward: {@code /tokens charge <player> <cost> <reason> <reward>}. */
class PurchaseTest {

    ServerMock server;
    EconomyServiceTest.MemoryRepository repository;
    EconomyService economy;
    PlayerMock player;
    List<String> ran = new ArrayList<>();
    boolean rewardWorks = true;
    Purchase purchase;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        repository = new EconomyServiceTest.MemoryRepository();
        economy = new EconomyService(repository, new VaultEconomyBridgeTest.Handle(), 1);
        player = server.addPlayer("Buyer");
        purchase = new Purchase(economy, Runnable::run, command -> {
            ran.add(command);
            return rewardWorks;
        }, Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void fund(long tokens) {
        economy.deposit(player.getUniqueId(), Currency.TOKENS, tokens, "test").join();
    }

    private Purchase.Outcome buy(long cost) {
        return purchase.buy(player, Currency.TOKENS, cost, "shop:rare-key",
                "/crazycrates give virtual Rare 1 Buyer").join();
    }

    @Test
    void paysThenRunsTheRewardWithoutItsSlash() {
        fund(1_000);

        assertEquals(Purchase.Outcome.BOUGHT, buy(750));

        assertEquals(250, economy.balance(player.getUniqueId(), Currency.TOKENS));
        assertEquals(List.of("crazycrates give virtual Rare 1 Buyer"), ran);
        assertTrue(repository.audit.contains("WITHDRAW -750 shop:rare-key"));
    }

    @Test
    void tooPoorGetsNothingAndKeepsTheirTokens() {
        fund(700);

        assertEquals(Purchase.Outcome.TOO_POOR, buy(750));

        assertEquals(700, economy.balance(player.getUniqueId(), Currency.TOKENS));
        assertTrue(ran.isEmpty());
    }

    @Test
    void secondClickCannotSpendTheSameTokens() {
        fund(1_000);

        assertEquals(Purchase.Outcome.BOUGHT, buy(750));
        assertEquals(Purchase.Outcome.TOO_POOR, buy(750));

        assertEquals(1, ran.size());
        assertEquals(250, economy.balance(player.getUniqueId(), Currency.TOKENS));
    }

    @Test
    void failedRewardIsRefunded() {
        fund(1_000);
        rewardWorks = false;

        assertEquals(Purchase.Outcome.REFUNDED, buy(750));

        assertEquals(1_000, economy.balance(player.getUniqueId(), Currency.TOKENS));
        assertTrue(repository.audit.contains("DEPOSIT 750 refund:shop:rare-key"));
    }

    @Test
    void rewardThatThrowsIsRefunded() {
        fund(1_000);
        purchase = new Purchase(economy, Runnable::run, command -> {
            throw new IllegalStateException("boom");
        }, Logger.getAnonymousLogger());

        assertEquals(Purchase.Outcome.REFUNDED, buy(750));

        assertEquals(1_000, economy.balance(player.getUniqueId(), Currency.TOKENS));
    }

    @Test
    void buyerWhoLeftIsRefundedInsteadOfRewarded() {
        fund(1_000);
        player.disconnect();

        assertEquals(Purchase.Outcome.REFUNDED, buy(750));

        assertTrue(ran.isEmpty());
        assertEquals(1_000, economy.balance(player.getUniqueId(), Currency.TOKENS));
    }
}
