package club.avian.factions.factions.command;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.faction.Faction;
import club.avian.factions.api.faction.FactionRank;
import club.avian.factions.factions.FactionIndex;
import club.avian.factions.factions.FactionName;
import club.avian.factions.factions.FactionsConfig;
import club.avian.factions.factions.claim.ClaimIndex;
import club.avian.factions.factions.power.PowerService;
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
    private final ClaimIndex claims;
    private final PowerService power;
    private final ConfigHandle<FactionsConfig> config;
    private final Logger log;

    public FactionCommand(FactionIndex factions, ClaimIndex claims, PowerService power,
                          ConfigHandle<FactionsConfig> config, Logger log) {
        this.factions = factions;
        this.claims = claims;
        this.power = power;
        this.config = config;
        this.log = log;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("f")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(this::create)))
                .then(Commands.literal("disband").executes(this::disband))
                .then(Commands.literal("claim").executes(this::claim))
                .then(Commands.literal("unclaim")
                        .executes(ctx -> unclaim(ctx, false))
                        .then(Commands.literal("all").executes(ctx -> unclaim(ctx, true))))
                .then(Commands.literal("map").executes(this::map))
                .then(Commands.literal("power")
                        .executes(ctx -> power(ctx, null))
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .executes(ctx -> power(ctx, StringArgumentType.getString(ctx, "faction")))))
                .then(Commands.literal("who")
                        .executes(ctx -> who(ctx, null))
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .executes(ctx -> who(ctx, StringArgumentType.getString(ctx, "faction")))))
                .executes(this::usage)
                .build();
    }

    private int usage(CommandContext<CommandSourceStack> ctx) {
        send(ctx, Component.text("/f create <name> · /f disband · /f who [faction] · /f power [faction]", NamedTextColor.GRAY));
        send(ctx, Component.text("/f claim · /f unclaim [all] · /f map", NamedTextColor.GRAY));
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
        var faction = resolve(ctx, name);
        if (faction == null) {
            return 0;
        }
        send(ctx, Component.text(faction.name(), NamedTextColor.GOLD)
                .append(Component.text(" — " + faction.members().size() + " member(s)", NamedTextColor.GRAY)));
        send(ctx, Component.text(describeMembers(faction), NamedTextColor.GRAY));
        return 1;
    }

    private int power(CommandContext<CommandSourceStack> ctx, String name) {
        var faction = resolve(ctx, name);
        if (faction == null) {
            return 0;
        }
        double total = power.powerOf(faction);
        int capacity = power.claimCapacity(faction);
        send(ctx, Component.text(faction.name(), NamedTextColor.GOLD)
                .append(Component.text(String.format(" — power %.1f, claims up to %d", total, capacity),
                        NamedTextColor.GRAY)));
        if (ctx.getSource().getSender() instanceof Player self && faction.hasMember(self.getUniqueId())) {
            send(ctx, Component.text(String.format("Your power: %.1f / %.1f",
                    power.powerOf(self.getUniqueId()), power.maximumOf(self.getUniqueId())), NamedTextColor.GRAY));
        }
        return 1;
    }

    /** The named Faction, or the sender's own; sends the error and returns null when neither resolves. */
    private Faction resolve(CommandContext<CommandSourceStack> ctx, String name) {
        if (name != null) {
            var faction = factions.byName(name).orElse(null);
            if (faction == null) {
                send(ctx, error("No faction called " + name + "."));
            }
            return faction;
        }
        var player = player(ctx);
        if (player == null) {
            return null;
        }
        var faction = factions.ofPlayer(player.getUniqueId()).orElse(null);
        if (faction == null) {
            send(ctx, error("You are not in a faction. Try naming one."));
        }
        return faction;
    }

    // --- claims ------------------------------------------------------------------------------

    private int claim(CommandContext<CommandSourceStack> ctx) {
        var player = player(ctx);
        if (player == null) {
            return 0;
        }
        var faction = factions.ofPlayer(player.getUniqueId()).orElse(null);
        if (faction == null) {
            send(ctx, error("You are not in a faction."));
            return 0;
        }
        if (!faction.rankOf(player.getUniqueId()).isAtLeast(FactionRank.OFFICER)) {
            send(ctx, error("Officers and above may claim land."));
            return 0;
        }
        var chunk = player.getLocation().getChunk();
        int capacity = power.claimCapacity(faction);
        var result = claims.claim(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(),
                faction.id(), player.getUniqueId(), capacity);
        if (!result.ok()) {
            send(ctx, error(switch (result.refusal()) {
                case WORLD_DISABLED -> "Land cannot be claimed in this world.";
                case ALREADY_OWNED_BY_YOU -> "Your faction already owns this chunk.";
                case OWNED_BY_OTHER -> "Another faction owns this chunk.";
                case OVER_CAPACITY -> String.format(
                        "Not enough power: %d/%d chunks claimed. Your faction has %.1f power.",
                        claims.countOf(faction.id()), capacity, power.powerOf(faction));
            }));
            return 0;
        }
        result.persisted().exceptionally(logFailure("persist claim for " + faction.name()));
        send(ctx, Component.text("Claimed ", NamedTextColor.GRAY)
                .append(Component.text(chunk.getX() + ", " + chunk.getZ(), NamedTextColor.GOLD))
                .append(Component.text(String.format(" — %d/%d chunks.",
                        claims.countOf(faction.id()), capacity), NamedTextColor.GRAY)));
        return 1;
    }

    private int unclaim(CommandContext<CommandSourceStack> ctx, boolean all) {
        var player = player(ctx);
        if (player == null) {
            return 0;
        }
        var faction = factions.ofPlayer(player.getUniqueId()).orElse(null);
        if (faction == null) {
            send(ctx, error("You are not in a faction."));
            return 0;
        }
        if (!faction.rankOf(player.getUniqueId()).isAtLeast(FactionRank.OFFICER)) {
            send(ctx, error("Officers and above may unclaim land."));
            return 0;
        }
        if (all) {
            int held = claims.countOf(faction.id());
            claims.unclaimAll(faction.id()).exceptionally(logFailure("unclaim all for " + faction.name()));
            send(ctx, Component.text("Released " + held + " chunk(s).", NamedTextColor.GRAY));
            return 1;
        }
        var chunk = player.getLocation().getChunk();
        var released = claims.unclaim(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), faction.id());
        if (released.isEmpty()) {
            send(ctx, error("Your faction does not own this chunk."));
            return 0;
        }
        released.get().exceptionally(logFailure("unclaim for " + faction.name()));
        send(ctx, Component.text("Released " + chunk.getX() + ", " + chunk.getZ() + ".", NamedTextColor.GRAY));
        return 1;
    }

    /**
     * An ASCII map of nearby chunks (spec §9). Own faction, other factions and wilderness are
     * distinguished; ally/truce/enemy colours join this when relations land.
     */
    private int map(CommandContext<CommandSourceStack> ctx) {
        var player = player(ctx);
        if (player == null) {
            return 0;
        }
        var own = factions.ofPlayer(player.getUniqueId()).map(Faction::id).orElse(null);
        var chunk = player.getLocation().getChunk();
        var world = chunk.getWorld().getName();
        int radius = config.get().claims().mapRadius();

        send(ctx, Component.text("Map around " + chunk.getX() + ", " + chunk.getZ()
                + "  (you are at the centre)", NamedTextColor.GRAY));
        for (int dz = -radius; dz <= radius; dz++) {
            var row = Component.text();
            for (int dx = -radius; dx <= radius; dx++) {
                int cx = chunk.getX() + dx;
                int cz = chunk.getZ() + dz;
                boolean here = dx == 0 && dz == 0;
                var owner = claims.ownerOf(world, cx, cz).orElse(null);
                var colour = owner == null ? NamedTextColor.DARK_GRAY
                        : owner.equals(own) ? NamedTextColor.GREEN : NamedTextColor.RED;
                var glyph = here ? "+" : owner == null ? "-" : "#";
                row.append(Component.text(glyph + " ", here ? NamedTextColor.GOLD : colour));
            }
            send(ctx, row.build());
        }
        send(ctx, Component.text("- wilderness   # claimed   green yours   red others",
                NamedTextColor.DARK_GRAY));
        return 1;
    }

    private java.util.function.Function<Throwable, Void> logFailure(String what) {
        return throwable -> {
            log.log(Level.SEVERE, "Could not " + what, throwable);
            return null;
        };
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
