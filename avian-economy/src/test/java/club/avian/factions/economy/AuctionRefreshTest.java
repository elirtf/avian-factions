package club.avian.factions.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionRefreshTest {

    private static final String SHOP = "&6&lAuction &e&lHouse &8· ";

    @Test
    void shopPagesAreTheNameThenAPageNumber() {
        assertTrue(AuctionRefresh.isShopPage(SHOP + "1", SHOP));
        assertTrue(AuctionRefresh.isShopPage(SHOP + "12", SHOP));
    }

    @Test
    void otherMenusAreLeftAlone() {
        assertFalse(AuctionRefresh.isShopPage(SHOP + " #1", SHOP), "the admin viewer");
        assertFalse(AuctionRefresh.isShopPage(SHOP, SHOP));
        assertFalse(AuctionRefresh.isShopPage("&6&lPlace a bid", SHOP));
        assertFalse(AuctionRefresh.isShopPage("anything 1", ""), "no configured name");
    }
}
