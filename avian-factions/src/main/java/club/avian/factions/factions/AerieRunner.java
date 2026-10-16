package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import dev.kitteh.factions.FPlayer;
import dev.kitteh.factions.FPlayers;
import dev.kitteh.factions.Faction;
import dev.kitteh.factions.Factions;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

/**
 * The Aerie in the world: once a second it finds who stands in the zone, ticks {@link Aerie}, and turns what
 * happens into announcements, the boss bar for players inside, the floating sign, the holder's sell bonus and
 * their timed rewards. Everything visible is announced server-wide, so a capture always draws a fight.
 */
final class AerieRunner implements Listener {

    private final Plugin plugin;
    private final ConfigHandle<AerieConfig> config;
    private final AerieRepository repository;
    private final LongSupplier clock;
    private final Logger log;
    private Aerie aerie;

    private final BossBar bar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    private final Set<UUID> barShown = new HashSet<>();
    private final Map<UUID, PermissionAttachment> bonus = new HashMap<>();
    private final Map<Integer, Long> lastStartAnnounced = new HashMap<>();
    private TextDisplay sign;

    AerieRunner(Plugin plugin, ConfigHandle<AerieConfig> config, AerieRepository repository, LongSupplier clock, Logger log) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.clock = clock;
        this.log = log;
    }

    void start() {
        var cfg = config.get();
        aerie = new Aerie(cfg.captureSeconds(), cfg.rewardEveryMillis(), cfg.milestoneHours() * 3_600_000L,
                repository.load(), clock.getAsLong());
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20, 20);
        log.info("The Aerie is " + (aerie.holder() == null ? "unclaimed" : "held by " + factionName(aerie.holder())));
    }

    void stop() {
        bonus.values().forEach(PermissionAttachment::remove);
        bonus.clear();
        for (var id : barShown) {
            var p = Bukkit.getPlayer(id);
            if (p != null) {
                p.hideBossBar(bar);
            }
        }
        barShown.clear();
        if (sign != null) {
            sign.remove();
        }
    }

    Aerie aerie() {
        return aerie;
    }

    // --- The tick -------------------------------------------------------------------------------------------

    private void tick() {
        var cfg = config.get();
        long now = clock.getAsLong();
        if (aerie.holder() != null) {
            var holder = Factions.factions().get(aerie.holder());
            if (holder == null || !holder.isNormal()) {
                handle(aerie.release(), now);
            }
        }
        var world = Bukkit.getWorld(cfg.world());
        Set<Object> teams = new HashSet<>();
        Set<Player> inside = new HashSet<>();
        if (cfg.enabled() && world != null) {
            for (var player : world.getPlayers()) {
                var at = player.getLocation();
                if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE
                        || !cfg.contains(at.getBlockX(), at.getBlockY(), at.getBlockZ())) {
                    continue;
                }
                inside.add(player);
                teams.add(team(player));
            }
            handle(aerie.tick(teams, now), now);
        }
        updateBar(inside, now);
        updateSign(cfg, world, now);
        reconcileBonus(cfg);
    }

    /** A player's side in the zone: their faction's id, or themselves when they have none. */
    private static Object team(Player player) {
        FPlayer member = FPlayers.fPlayers().get(player);
        return member.hasFaction() && member.faction().isNormal() ? (Object) member.faction().id() : player.getUniqueId();
    }

    // --- What happened --------------------------------------------------------------------------------------

    void handle(List<Aerie.Event> events, long now) {
        var cfg = config.get();
        for (var event : events) {
            switch (event) {
                case Aerie.CaptureStarted e -> {
                    Long last = lastStartAnnounced.get(e.faction());
                    if (last == null || now - last >= cfg.announceCooldownSeconds() * 1000L) {
                        lastStartAnnounced.put(e.faction(), now);
                        broadcast("<hot>⚔</hot> " + name(e.faction()) + " <soft>is capturing</soft> " + AERIE
                                + " <soft>(" + clock(aerie.captureSeconds()) + ")! Stop them!</soft>", Sound.EVENT_RAID_HORN, 0.5f);
                    }
                    tellFaction(e.faction(), "<cane>You're capturing the Aerie. Hold the zone alone for "
                            + clock(aerie.captureSeconds()) + ".</cane>");
                    if (aerie.holder() != null) {
                        tellFaction(aerie.holder(), "<bad><bold>⚠ " + plain(e.faction()) + " is taking your Aerie!</bold></bad> <soft>Get there.</soft>");
                    }
                }
                case Aerie.Contested e -> broadcast("<hot>⚔</hot> " + AERIE + " <soft>is</soft> <bad><bold>contested</bold></bad><soft>: "
                        + "</soft>" + name(e.faction()) + "<soft>'s capture is paused.</soft>", null, 0);
                case Aerie.Halfway e -> broadcast("<hot>⚔</hot> " + name(e.faction()) + " <soft>is</soft> <sun>halfway</sun> <soft>to taking</soft> "
                        + AERIE + "<soft>!</soft>", Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f);
                case Aerie.OneMinuteLeft e -> broadcast("<hot>⚔</hot> " + name(e.faction()) + " <soft>takes</soft> " + AERIE
                        + " <soft>in</soft> <bad><bold>1 minute</bold></bad><soft>!</soft>", Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f);
                case Aerie.CaptureFailed e -> tellFaction(e.faction(), "<bad>Capture lost: you left the Aerie.</bad>");
                case Aerie.Captured e -> captured(cfg, e);
                case Aerie.RewardDue e -> reward(cfg, e);
                case Aerie.Milestone e -> milestone(cfg, e);
                case Aerie.Released e -> broadcast(AERIE + " <soft>is free: its holders are gone.</soft>", null, 0);
            }
            if (event instanceof Aerie.Captured || event instanceof Aerie.Milestone || event instanceof Aerie.Released) {
                repository.save(aerie.hold());
            }
        }
    }

    private void captured(AerieConfig cfg, Aerie.Captured e) {
        broadcast("<sun>🦅</sun> " + name(e.faction()) + " <soft>has taken</soft> " + AERIE
                + (e.previous() == null ? "" : " <soft>from</soft> " + name(e.previous()))
                + "<soft>! Their sales pay</soft> <money>+" + cfg.sellBonusPercent() + "%</money><soft>.</soft>",
                Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f);
        for (var p : online(e.faction())) {
            p.showTitle(Title.title(Brand.mm("<bold><gradient:#FFD23F:#FF8A3D>THE AERIE IS YOURS</gradient></bold>"),
                    Brand.mm("<money>+" + cfg.sellBonusPercent() + "% sell</money> <soft>· keys and tokens every hour you hold it</soft>"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofSeconds(1))));
        }
        if (e.previous() != null) {
            tellFaction(e.previous(), "<bad><bold>You lost the Aerie</bold></bad> <soft>to</soft> " + name(e.faction()) + "<soft>.</soft>");
        }
    }

    private void reward(AerieConfig cfg, Aerie.RewardDue e) {
        if (e.index() >= cfg.rewards().size()) {
            return;
        }
        var reward = cfg.rewards().get(e.index());
        var members = online(e.faction());
        for (var p : members) {
            run(reward.commands(), p.getName(), e.faction());
            p.sendMessage(Brand.mm("<sun>🦅</sun> " + AERIE + " <soft>pays its holders:</soft> <cane>" + reward.description() + "</cane>"));
        }
    }

    private void milestone(AerieConfig cfg, Aerie.Milestone e) {
        var faction = Factions.factions().get(e.faction());
        if (faction == null) {
            return;
        }
        for (var member : faction.members()) {
            if (member.name() != null) {
                run(cfg.milestoneCommands(), member.name(), e.faction());
            }
        }
        broadcast("<sun>🦅</sun> " + name(e.faction()) + " <soft>has held</soft> " + AERIE + " <soft>for</soft> <sun>"
                + cfg.milestoneHours() + " hours</sun><soft>! Every member earns the</soft> "
                + "<bold><gradient:#FFD23F:#FF8A3D>⛰ AERIE</gradient></bold> <soft>tag (/tags).</soft>",
                Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f);
    }

    private void run(List<String> commands, String player, int faction) {
        for (var command : commands) {
            var line = command.replace("%player%", player).replace("%faction%", plain(faction));
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line)) {
                log.warning("aerie: command failed: " + line);
            }
        }
    }

    // --- What players see -----------------------------------------------------------------------------------

    private void updateBar(Set<Player> inside, long now) {
        Integer capturer = aerie.capturer();
        if (capturer != null) {
            bar.name(Brand.mm(aerie.contested()
                    ? "<bad><bold>CONTESTED</bold></bad> <soft>·</soft> " + name(capturer) + " <soft>paused at</soft> <sun>" + percent() + "%</sun>"
                    : name(capturer) + " <soft>capturing ·</soft> <sun>" + clock(aerie.captureSeconds() - aerie.progress()) + "</sun>"));
            bar.color(aerie.contested() ? BossBar.Color.RED : BossBar.Color.YELLOW);
            bar.progress(Math.min(1f, aerie.progress() / (float) aerie.captureSeconds()));
        } else if (aerie.holder() != null) {
            bar.name(Brand.mm(AERIE + " <soft>held by</soft> " + name(aerie.holder()) + " <soft>for</soft> <sun>"
                    + held(now - aerie.hold().since()) + "</sun> <soft>· stand here alone to take it</soft>"));
            bar.color(BossBar.Color.PURPLE);
            bar.progress(1f);
        } else {
            bar.name(Brand.mm(AERIE + " <soft>is</soft> <cane>unclaimed</cane> <soft>· stand here alone for "
                    + clock(aerie.captureSeconds()) + " to take it</soft>"));
            bar.color(BossBar.Color.GREEN);
            bar.progress(0f);
        }
        var ids = new HashSet<UUID>();
        for (var p : inside) {
            ids.add(p.getUniqueId());
            if (barShown.add(p.getUniqueId())) {
                p.showBossBar(bar);
            }
        }
        barShown.removeIf(id -> {
            if (ids.contains(id)) {
                return false;
            }
            var p = Bukkit.getPlayer(id);
            if (p != null) {
                p.hideBossBar(bar);
            }
            return true;
        });
    }

    private void updateSign(AerieConfig cfg, org.bukkit.World world, long now) {
        if (world == null || !world.isChunkLoaded((int) Math.floor(cfg.displayX()) >> 4, (int) Math.floor(cfg.displayZ()) >> 4)) {
            sign = null;   // not persistent: it went with its chunk
            return;
        }
        if (sign == null || !sign.isValid()) {
            sign = world.spawn(new Location(world, cfg.displayX(), cfg.displayY(), cfg.displayZ()), TextDisplay.class, d -> {
                d.setPersistent(false);   // never saved: respawned with its chunk, so no stale copies pile up
                d.setBillboard(Display.Billboard.CENTER);
                d.setShadowed(true);
                d.setViewRange(4f);
                d.setBackgroundColor(org.bukkit.Color.fromARGB(110, 20, 12, 30));
            });
        }
        String status;
        if (aerie.capturer() != null) {
            status = aerie.contested()
                    ? "<bad><bold>⚔ CONTESTED ⚔</bold></bad>"
                    : name(aerie.capturer()) + "\n<soft>capturing ·</soft> <sun>" + clock(aerie.captureSeconds() - aerie.progress()) + "</sun>";
        } else if (aerie.holder() != null) {
            status = "<soft>Held by</soft>\n" + name(aerie.holder()) + "\n<soft>for</soft> <sun>" + held(now - aerie.hold().since()) + "</sun>";
        } else {
            status = "<cane>Unclaimed</cane>\n<soft>Stand here alone to take it</soft>";
        }
        sign.text(Brand.mm("<bold><gradient:#FFD23F:#FF8A3D>🦅 THE AERIE 🦅</gradient></bold>\n" + status
                + "\n<money>+" + cfg.sellBonusPercent() + "% sell</money> <soft>· keys · tokens</soft>"));
    }

    /** Online members of the holder get the sell-bonus permission; everyone else loses it. */
    private void reconcileBonus(AerieConfig cfg) {
        Integer holder = aerie.holder();
        for (var player : Bukkit.getOnlinePlayers()) {
            var member = FPlayers.fPlayers().get(player);
            boolean should = holder != null && member.hasFaction() && member.faction().id() == holder;
            var attachment = bonus.get(player.getUniqueId());
            if (should && attachment == null) {
                attachment = player.addAttachment(plugin);
                attachment.setPermission(cfg.sellBonusPermission(), true);
                bonus.put(player.getUniqueId(), attachment);
            } else if (!should && attachment != null) {
                attachment.remove();
                bonus.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var attachment = bonus.remove(event.getPlayer().getUniqueId());
        if (attachment != null) {
            attachment.remove();
        }
        barShown.remove(event.getPlayer().getUniqueId());
    }

    // --- Words ----------------------------------------------------------------------------------------------

    static final String AERIE = "<bold><gradient:#FFD23F:#FF8A3D>the Aerie</gradient></bold>";

    private void broadcast(String message, Sound sound, float pitch) {
        Bukkit.broadcast(Brand.mm(message));
        if (sound != null) {
            for (var p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), sound, 0.6f, pitch);
            }
        }
    }

    private void tellFaction(int faction, String message) {
        for (var p : online(faction)) {
            p.sendMessage(Brand.mm(message));
        }
    }

    private static List<Player> online(int faction) {
        var f = Factions.factions().get(faction);
        return f == null ? List.of() : f.membersOnlineAsPlayers();
    }

    /** The faction's name, coloured, safe to put in MiniMessage. */
    static String name(int faction) {
        return "<white><bold>" + MiniMessage.miniMessage().escapeTags(plain(faction)) + "</bold></white>";
    }

    static String plain(int faction) {
        return factionName(faction);
    }

    private static String factionName(int faction) {
        Faction f = Factions.factions().get(faction);
        return f == null ? "a faction" : f.tag();
    }

    private int percent() {
        return Math.round(100f * aerie.progress() / aerie.captureSeconds());
    }

    static String clock(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    static String held(long millis) {
        long minutes = millis / 60_000L;
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
    }
}
