package club.avian.factions.ftop;

import club.avian.factions.api.config.ConfigHandle;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.time.Clock;
import java.time.Duration;
import java.util.function.LongFunction;

/** {@code /ftop [page]} and {@code /ftop recalc}. FactionsUUID's own {@code /f top} ranks by money only. */
final class FTopCommand {

    static final String ADMIN_PERMISSION = "avian.ftop.admin";

    private final FTopService service;
    private final ConfigHandle<FTopConfig> config;
    private final LongFunction<String> money;
    private final Clock clock;

    FTopCommand(FTopService service, ConfigHandle<FTopConfig> config, LongFunction<String> money, Clock clock) {
        this.service = service;
        this.config = config;
        this.money = money;
        this.clock = clock;
    }

    LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("ftop")
                .executes(ctx -> show(ctx.getSource().getSender(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> show(ctx.getSource().getSender(), IntegerArgumentType.getInteger(ctx, "page"))))
                .then(Commands.literal("recalc")
                        .requires(src -> src.getSender().hasPermission(ADMIN_PERMISSION))
                        .executes(ctx -> {
                            var ranking = service.recalculate();
                            ctx.getSource().getSender().sendMessage(Component.text(
                                    "F-Top recalculated: " + ranking.entries().size() + " faction(s) ranked.", NamedTextColor.GOLD));
                            return 1;
                        }))
                .build();
    }

    private int show(CommandSender sender, int page) {
        var ranking = service.latest();
        int size = config.get().pageSize();
        int pages = Math.max(1, (ranking.entries().size() + size - 1) / size);
        if (ranking.entries().isEmpty()) {
            sender.sendMessage(Component.text("No faction has any value yet.", NamedTextColor.GRAY));
            return 1;
        }
        if (page > pages) {
            sender.sendMessage(Component.text("There are only " + pages + " page(s).", NamedTextColor.RED));
            return 0;
        }
        long minutes = Duration.between(ranking.calculatedAt(), clock.instant()).toMinutes();
        sender.sendMessage(Component.text("—— F-Top ", NamedTextColor.GOLD)
                .append(Component.text("(updated " + (minutes < 1 ? "just now" : minutes + " min ago") + ")", NamedTextColor.GRAY))
                .append(Component.text(" ——", NamedTextColor.GOLD)));
        ranking.entries().stream().skip((long) (page - 1) * size).limit(size).forEach(e -> {
            var hover = Component.text("Spawners: " + money.apply(e.spawnerValue()), NamedTextColor.GOLD)
                    .append(Component.newline())
                    .append(Component.text("Blocks: " + money.apply(e.blockValue()), NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text(e.breakdown(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("#" + e.rank() + " ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(e.owner().tag(), NamedTextColor.WHITE))
                    .append(Component.text("  " + money.apply(e.total()), NamedTextColor.GOLD))
                    .hoverEvent(HoverEvent.showText(hover)));
        });
        if (pages > 1) {
            sender.sendMessage(Component.text("Page " + page + "/" + pages
                    + (page < pages ? " — /ftop " + (page + 1) + " for more" : ""), NamedTextColor.GRAY));
        }
        return 1;
    }
}
