package club.avian.factions.factions.command;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.faction.Faction;
import club.avian.factions.api.faction.FactionRank;
import club.avian.factions.factions.FactionIndex;
import club.avian.factions.factions.FactionName;
import club.avian.factions.factions.FactionsConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * {@code /f} — create, disband, who (spec §7). The rest of the tree lands with claims and power.
 *
 * <p>Messages are inline {@code Component}s for now; they move to {@code messages.conf} when
 * core's message system lands (ADR-0003 rule 10).
 */
public final class FactionCommand {

    private final FactionIndex factions;
    private final ConfigHandle<FactionsConfig> config;
    private final Logger log;

    public FactionCommand(FactionIndex factions, ConfigHandle<FactionsConfig> config, Logger log) {
        this.factions = factions;
        this.config = config;
        this.log = log;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("f")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(this::create)))
                .then(Commands.literal("disband").executes(this::disband))
                .then(Commands.literal("who")
                        .executes(ctx -> who(ctx, null))
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .executes(ctx -> who(ctx, StringArgumentType.getString(ctx, "faction")))))
                .executes(this::usage)
                .build();
    }

    private int usage(CommandContext<CommandSourceStack> ctx) {
        send(ctx, Component.text("/f create <name> · /f disband · /f who [faction]", NamedTextColor.GRAY));
        return 1;
    }

    private int create(CommandContext<CommandSourceStack> ctx) {
        var player = player(ctx);
        if (player == null) {
            return 0;
        }
        var name = StringArgumentType.getString(ctx, "name");
        var rules = config.get().names();

        var problem = FactionName.validate(name, rules);
        if (problem != null) {
            send(ctx, error(switch (problem) {
                case TOO_SHORT -> "Faction names must be at least " + rules.minLength() + " characters.";
                case TOO_LONG -> "Faction names must be at most " + rules.maxLength() + " characters.";
                case ILLEGAL_CHARACTERS -> "Faction names may only use letters, numbers and underscores.";
                case RESERVED -> "That name is reserved.";
                case BLOCKED -> "That name is not allowed.";
                case TAKEN -> "That name is taken.";
            }));
            return 0;
        }
        if (factions.nameTaken(name)) {
            send(ctx, error("A faction called " + name + " already exists."));
            return 0;
        }
        if (factions.ofPlayer(player.getUniqueId()).isPresent()) {
            send(ctx, error("You are already in a faction. Leave or disband it first."));
            return 0;
        }

        var created = factions.create(name, player.getUniqueId());
        created.persisted().exceptionally(t -> {
            // The index already has it, so the session continues; the next boot reloads from the
            // database, which is the source of truth. Loudly logged rather than silently lost.
            log.log(Level.SEVERE, "Faction '" + name + "' was not persisted", t);
            return null;
        });
        send(ctx, Component.text("Created faction ", NamedTextColor.GRAY)
                .append(Component.text(created.faction().name(), NamedTextColor.GOLD))
                .append(Component.text(". You are its Leader.", NamedTextColor.GRAY)));
        return 1;
    }

    private int disband(CommandContext<CommandSourceStack> ctx) {
        var player = player(ctx);
        if (player == null) {
            return 0;
        }
        var faction = factions.ofPlayer(player.getUniqueId()).orElse(null);
        if (faction == null) {
            send(ctx, error("You are not in a faction."));
            return 0;
        }
        if (faction.rankOf(player.getUniqueId()) != FactionRank.LEADER) {
            send(ctx, error("Only the Leader can disband the faction."));
            return 0;
        }
        var name = faction.name();
        factions.disband(faction.id()).exceptionally(t -> {
            log.log(Level.SEVERE, "Faction '" + name + "' was not deleted", t);
            return null;
        });
        send(ctx, Component.text("Disbanded " + name + ".", NamedTextColor.GRAY));
        return 1;
    }

    private int who(CommandContext<CommandSourceStack> ctx, String name) {
        Faction faction;
        if (name != null) {
            faction = factions.byName(name).orElse(null);
            if (faction == null) {
                send(ctx, error("No faction called " + name + "."));
                return 0;
            }
        } else {
            var player = player(ctx);
            if (player == null) {
                return 0;
            }
            faction = factions.ofPlayer(player.getUniqueId()).orElse(null);
            if (faction == null) {
                send(ctx, error("You are not in a faction. Try /f who <faction>."));
                return 0;
            }
        }
        send(ctx, Component.text(faction.name(), NamedTextColor.GOLD)
                .append(Component.text(" — " + faction.members().size() + " member(s)", NamedTextColor.GRAY)));
        send(ctx, Component.text(describeMembers(faction), NamedTextColor.GRAY));
        return 1;
    }

    private String describeMembers(Faction faction) {
        return faction.members().entrySet().stream()
                .sorted(Comparator.comparingInt((java.util.Map.Entry<UUID, FactionRank> e) -> e.getValue().ordinal()).reversed())
                .map(e -> rankName(e.getValue()) + " " + nameOf(e.getKey()))
                .collect(Collectors.joining(", "));
    }

    private static String rankName(FactionRank rank) {
        var lower = rank.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String nameOf(UUID player) {
        var offline = org.bukkit.Bukkit.getOfflinePlayer(player);
        var name = offline.getName();
        return name == null ? player.toString().substring(0, 8) : name;
    }

    private Player player(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getSender() instanceof Player player) {
            return player;
        }
        send(ctx, error("Only players can use this command."));
        return null;
    }

    private static Component error(String message) {
        return Component.text(message, NamedTextColor.RED);
    }

    private static void send(CommandContext<CommandSourceStack> ctx, Component message) {
        ctx.getSource().getSender().sendMessage(message);
    }
}
