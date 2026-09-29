package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The /tokens command tree, parsed the way the server parses it. The token shop broke on the live
 * server (2026-09-29) because a word argument rejected the ':' in {@code shop:key-common}, which unit
 * tests of {@link Purchase} never saw; so this runs every menu line through the real parser.
 */
class EconomyCommandTest {

    static final Path MENUS = Path.of("../dev-server/plugins/DeluxeMenus/gui_menus");
    static final Pattern CHARGE = Pattern.compile("'\\[console] (tokens charge .*)'");

    ServerMock server;
    EconomyService economy;
    PlayerMock buyer;
    List<String> rewards = new ArrayList<>();
    CommandDispatcher<CommandSourceStack> dispatcher;
    CommandSourceStack console;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        economy = new EconomyService(new EconomyServiceTest.MemoryRepository(), new VaultEconomyBridgeTest.Handle(), 1);
        buyer = server.addPlayer("Buyer");
        var purchase = new Purchase(economy, Runnable::run, rewards::add, Logger.getAnonymousLogger());
        dispatcher = new CommandDispatcher<>();
        for (var node : new EconomyCommand(economy, Runnable::run, purchase).build()) {
            dispatcher.getRoot().addChild(node);
        }
        var sender = server.getConsoleSender();
        // CommandSourceStack is an interface; the commands only ever ask it for the sender.
        console = (CommandSourceStack) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {CommandSourceStack.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSender" -> sender;
                    case "getExecutor" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void run(String command) throws Exception {
        dispatcher.execute(command, console);
    }

    @Test
    void everyTokenShopButtonParsesAndCharges() throws Exception {
        var lines = chargeLines("token_shop.yml");
        assertFalse(lines.isEmpty(), "the token shop has buttons");
        economy.deposit(buyer.getUniqueId(), Currency.TOKENS, 1_000_000, "test").join();
        for (var line : lines) {
            rewards.clear();
            long before = economy.balance(buyer.getUniqueId(), Currency.TOKENS);
            run(line.replace("%avian_name%", "Buyer"));
            assertEquals(1, rewards.size(), "reward ran for: " + line);
            assertFalse(rewards.getFirst().contains("%"), "no unfilled placeholder in: " + rewards.getFirst());
            long cost = Long.parseLong(line.split(" ")[3]);
            assertEquals(before - cost, economy.balance(buyer.getUniqueId(), Currency.TOKENS), line);
        }
    }

    @Test
    void theReasonKeepsItsColon() throws Exception {
        economy.deposit(buyer.getUniqueId(), Currency.TOKENS, 500, "test").join();
        run("tokens charge Buyer 200 shop:key-common crazycrates give virtual Common 1 Buyer");
        assertEquals(List.of("crazycrates give virtual Common 1 Buyer"), rewards);
        assertEquals(300, economy.balance(buyer.getUniqueId(), Currency.TOKENS));
    }

    @Test
    void splitNeedsBothAReasonAndAReward() {
        assertArrayEquals(new String[] {"shop:x", "give it"}, EconomyCommand.splitReasonAndReward(" shop:x  give it "));
        assertNull(EconomyCommand.splitReasonAndReward("shop:x"));
        assertNull(EconomyCommand.splitReasonAndReward("shop:x   "));
    }

    /** The console commands a menu runs through `tokens charge`, as written in the file. */
    static List<String> chargeLines(String menu) throws IOException {
        var out = new ArrayList<String>();
        for (var line : Files.readAllLines(MENUS.resolve(menu))) {
            var m = CHARGE.matcher(line.strip().replaceFirst("^- ", ""));
            if (m.matches()) {
                out.add(m.group(1));
            }
        }
        return out;
    }
}
