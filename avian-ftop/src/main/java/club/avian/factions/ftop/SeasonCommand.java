package club.avian.factions.ftop;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.text.Brand;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.time.Clock;
import java.time.Duration;

/**
 * {@code /season} (spec §51): {@code status} for everyone; {@code start}, {@code end confirm} and {@code preview}
 * for admins. {@code preview} shows what the next weekly payout would pay today without paying anything.
 */
final class SeasonCommand {

    static final String ADMIN = "avian.admin.season";

    private final SeasonService seasons;
    private final Economy economy;
    private final Clock clock;

    SeasonCommand(SeasonService seasons, Economy economy, Clock clock) {
        this.seasons = seasons;
        this.economy = economy;
        this.clock = clock;
    }

    LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("season")
                .executes(ctx -> status(ctx.getSource().getSender()))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource().getSender())))
                .then(Commands.literal("preview").requires(s -> s.getSender().hasPermission(ADMIN))
                        .executes(ctx -> preview(ctx.getSource().getSender())))
                .then(Commands.literal("start").requires(s -> s.getSender().hasPermission(ADMIN))
                        .executes(ctx -> {
                            var s = seasons.season();
                            if (s != null && s.active()) {
                                ctx.getSource().getSender().sendMessage(Brand.mm("<bad>Season " + s.id() + " is still running.</bad>"));
                                return 0;
                            }
                            var started = seasons.start(s == null ? 1 : s.id() + 1);
                            ctx.getSource().getSender().sendMessage(Brand.mm("<cane>Season " + started.id()
                                    + " started. It must match economy.conf's season-id.</cane>"));
                            return 1;
                        }))
                .then(Commands.literal("end").requires(s -> s.getSender().hasPermission(ADMIN))
                        .executes(ctx -> {
                            ctx.getSource().getSender().sendMessage(Brand.mm("<bad>This ends the season now and pays the final "
                                    + "F-Top payout. Type /season end confirm.</bad>"));
                            return 1;
                        })
                        .then(Commands.literal("confirm").executes(ctx -> {
                            seasons.end();
                            return 1;
                        })))
                .build();
    }

    private int status(CommandSender to) {
        var s = seasons.season();
        if (s == null) {
            to.sendMessage(Brand.mm("<soft>No season has started yet. F-Top payouts begin with the first season.</soft>"));
        } else if (s.active()) {
            var left = Duration.between(clock.instant(), s.endsAt());
            to.sendMessage(Brand.mm("<sun>🏆 Season " + s.id() + "</sun> <soft>ends in</soft> <white>" + left.toDays() + "d "
                    + left.toHoursPart() + "h</white><soft>. The top 3 on</soft> <white>/ftop</white> <soft>are paid every week.</soft>"));
        } else {
            to.sendMessage(Brand.mm("<soft>Season " + s.id() + " has ended. The next one hasn't started yet.</soft>"));
        }
        return 1;
    }

    private int preview(CommandSender to) {
        var awards = seasons.preview();
        to.sendMessage(Brand.mm("<sun>Weekly payout, if it ran now (nothing is paid):</sun>"));
        if (awards.isEmpty()) {
            to.sendMessage(Brand.mm("<soft>No faction on the F-Top yet.</soft>"));
        }
        for (var a : awards) {
            to.sendMessage(Brand.mm("  <sun>#" + a.place() + "</sun> <white>" + MiniMessage.miniMessage().escapeTags(a.faction().tag())
                    + "</white> <soft>· " + a.members().size() + " members ·</soft> <money>"
                    + economy.format(Currency.MONEY, a.moneyEach()) + "</money> <soft>+</soft> <token>"
                    + economy.format(Currency.TOKENS, a.tokensEach()) + "</token> <soft>each</soft>"));
        }
        return 1;
    }
}
