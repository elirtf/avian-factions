package club.avian.factions.economy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

/**
 * {@code /kitbooks <player> <tier> [count]}: random custom enchant books of one tier, for rank kits
 * (owner, 2026-09-29: "3 random custom enchant books for each rank"). The pool is the Enchanter's own
 * roll for that tier, the CrazyCrates file {@code Enchant<Tier>.yml}, read with the same weights, so a
 * kit book and a bought one come from the same odds and never drift apart. It runs the chosen
 * prize's {@code eenchants book} command without the spin: a kit can't play several spins at once.
 */
final class KitBooksCommand {

    static final String PERMISSION = "avian.admin.economy";   // kits run it as the console
    static final List<String> TIERS = List.of("Common", "Rare", "Legendary", "Mythic");

    /** One book in a tier's pool: its weight and the command that gives it ({@code %player%} unfilled). */
    record Book(double weight, String command) { }

    private final Function<String, List<Book>> pools;
    private final Consumer<String> console;
    private final DoubleSupplier random;

    KitBooksCommand(Function<String, List<Book>> pools, Consumer<String> console, DoubleSupplier random) {
        this.pools = pools;
        this.console = console;
        this.random = random;
    }

    /** The books in a CrazyCrates crate file: every prize whose command is an {@code eenchants book}. */
    static List<Book> readPool(File crateFile) {
        var books = new ArrayList<Book>();
        ConfigurationSection prizes = YamlConfiguration.loadConfiguration(crateFile).getConfigurationSection("Crate.Prizes");
        if (prizes == null) {
            return books;
        }
        for (String key : prizes.getKeys(false)) {
            double weight = prizes.getDouble(key + ".Weight", 0);
            for (String command : prizes.getStringList(key + ".Commands")) {
                if (weight > 0 && command.startsWith("eenchants book ")) {
                    books.add(new Book(weight, command));
                }
            }
        }
        return books;
    }

    /** A weighted pick, {@code roll} in [0, 1). */
    static Book pick(List<Book> pool, double roll) {
        double total = pool.stream().mapToDouble(Book::weight).sum();
        double target = roll * total;
        for (Book book : pool) {
            target -= book.weight();
            if (target < 0) {
                return book;
            }
        }
        return pool.getLast();
    }

    /** Gives {@code count} books; false when the tier is unknown or has no books. */
    boolean give(String player, String tier, int count) {
        List<Book> pool = pools.apply(tier);
        if (pool.isEmpty()) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            console.accept(pick(pool, random.getAsDouble()).command().replace("%player%", player));
        }
        return true;
    }

    /** "rare" or "RARE" → "Rare"; null when it isn't a tier. */
    static String tierName(String input) {
        return TIERS.stream().filter(t -> t.equalsIgnoreCase(input)).findFirst().orElse(null);
    }

    LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("kitbooks")
                .requires(src -> src.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("tier", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    TIERS.forEach(t -> b.suggest(t.toLowerCase(Locale.ROOT)));
                                    return b.buildFuture();
                                })
                                .executes(ctx -> run(ctx.getSource(), StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "tier"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 9))
                                        .executes(ctx -> run(ctx.getSource(), StringArgumentType.getString(ctx, "player"),
                                                StringArgumentType.getString(ctx, "tier"),
                                                IntegerArgumentType.getInteger(ctx, "count"))))))
                .build();
    }

    private int run(CommandSourceStack source, String player, String tierInput, int count) {
        String tier = tierName(tierInput);
        if (tier == null || !give(player, tier, count)) {
            source.getSender().sendMessage("kitbooks: no book pool for tier '" + tierInput + "' (one of " + TIERS + ")");
            return 0;
        }
        return 1;
    }
}
