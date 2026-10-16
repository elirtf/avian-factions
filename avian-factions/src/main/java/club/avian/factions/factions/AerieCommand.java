package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.kitteh.factions.Factions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

import java.util.function.LongSupplier;

/** {@code /aerie}: who holds it and what it pays; {@code /aerie reset} and {@code /aerie give <faction>} for admins. */
final class AerieCommand {

    static final String ADMIN_PERMISSION = "avian.admin.aerie";

    private final AerieRunner runner;
    private final ConfigHandle<AerieConfig> config;
    private final LongSupplier clock;

    AerieCommand(AerieRunner runner, ConfigHandle<AerieConfig> config, LongSupplier clock) {
        this.runner = runner;
        this.config = config;
        this.clock = clock;
    }

    LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("aerie")
                .executes(ctx -> {
                    info(ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.literal("reset")
                        .requires(src -> src.getSender().hasPermission(ADMIN_PERMISSION))
                        .executes(ctx -> {
                            runner.handle(runner.aerie().release(), clock.getAsLong());
                            ctx.getSource().getSender().sendMessage(Brand.mm("<cane>The Aerie is unclaimed again.</cane>"));
                            return 1;
                        }))
                .then(Commands.literal("give")
                        .requires(src -> src.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .executes(ctx -> {
                                    var tag = StringArgumentType.getString(ctx, "faction");
                                    var faction = Factions.factions().get(tag);
                                    if (faction == null || !faction.isNormal()) {
                                        ctx.getSource().getSender().sendMessage(Brand.mm("<bad>No faction called " + tag + ".</bad>"));
                                        return 0;
                                    }
                                    runner.handle(runner.aerie().forceCapture(faction.id(), clock.getAsLong()), clock.getAsLong());
                                    return 1;
                                })))
                .build();
    }

    private void info(CommandSender to) {
        var cfg = config.get();
        var aerie = runner.aerie();
        var top = "<dim>━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dim>";
        to.sendMessage(Brand.mm(top));
        to.sendMessage(Brand.mm(" <bold><gradient:#FFD23F:#FF8A3D>🦅 THE AERIE</gradient></bold> <soft>· a capturable outpost in the warzone</soft>"));
        if (aerie.holder() != null) {
            to.sendMessage(Brand.mm(" <sun>◆</sun> <soft>Held by</soft>  " + AerieRunner.name(aerie.holder())
                    + " <soft>for</soft> <sun>" + AerieRunner.held(clock.getAsLong() - aerie.hold().since()) + "</sun>"));
        } else {
            to.sendMessage(Brand.mm(" <sun>◆</sun> <soft>Held by</soft>  <cane>nobody, it's free</cane>"));
        }
        if (aerie.capturer() != null) {
            to.sendMessage(Brand.mm(" <sun>◆</sun> <soft>Capturing</soft>  " + AerieRunner.name(aerie.capturer())
                    + (aerie.contested() ? " <bad>(contested)</bad>" : " <sun>"
                    + AerieRunner.clock(aerie.captureSeconds() - aerie.progress()) + " left</sun>")));
        }
        to.sendMessage(Brand.mm(" <sun>◆</sun> <soft>Where</soft>  <white>" + cfg.centerX() + ", " + cfg.centerY() + ", "
                + cfg.centerZ() + "</white> <soft>in the warzone</soft>"));
        to.sendMessage(Brand.mm(" <sun>◆</sun> <soft>Take it</soft>  <white>stand inside with no enemy for "
                + AerieRunner.clock(cfg.captureSeconds()) + "</white>"));
        to.sendMessage(Brand.mm(" <sun>◆</sun> <soft>Holders get</soft>  <money>+" + cfg.sellBonusPercent() + "% on everything they sell</money>"));
        for (var reward : cfg.rewards()) {
            to.sendMessage(Brand.mm("   <soft>+</soft> <cane>" + reward.description() + "</cane> <soft>per online member</soft>"));
        }
        if (cfg.milestoneHours() > 0) {
            to.sendMessage(Brand.mm("   <soft>+ hold it</soft> <sun>" + cfg.milestoneHours() + "h</sun> <soft>straight: the</soft> "
                    + "<bold><gradient:#FFD23F:#FF8A3D>⛰ AERIE</gradient></bold> <soft>tag for every member</soft>"));
        }
        to.sendMessage(Brand.mm(top));
    }
}
