package club.avian.factions.factions;

import club.avian.factions.api.faction.FactionRank;
import club.avian.factions.testing.MariaDbExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
class JdbcFactionRepositoryIT {

    static final Instant T0 = Instant.parse("2026-09-22T12:00:00.500Z");

    final FactionRepository repo = new JdbcFactionRepository(MariaDbExtension.database());

    private FactionRecord faction(String name, int season, UUID leader) {
        return new FactionRecord(UUID.randomUUID(), name, leader, Map.of(leader, FactionRank.LEADER), season, T0);
    }

    @Test
    void createThenLoadRoundTripsTheFactionAndItsLeader() {
        var leader = UUID.randomUUID();
        var created = faction("Ravens" + System.nanoTime() % 100000, 1, leader);
        repo.create(created).join();

        var loaded = repo.loadSeason(1).stream()
                .filter(f -> f.id().equals(created.id())).findFirst().orElseThrow();
        assertEquals(created.name(), loaded.name());
        assertEquals(leader, loaded.leader());
        assertEquals(Map.of(leader, FactionRank.LEADER), loaded.members());
        assertEquals(T0, loaded.createdAt());
    }

    @Test
    void duplicateNameInTheSameSeasonIsRefusedByTheDatabaseCaseInsensitively() {
        var name = "Dup" + System.nanoTime() % 100000;
        repo.create(faction(name, 1, UUID.randomUUID())).join();
        var clash = faction(name.toUpperCase(java.util.Locale.ROOT), 1, UUID.randomUUID());
        var e = assertThrows(CompletionException.class, () -> repo.create(clash).join());
        assertTrue(e.getCause().getMessage().toLowerCase(java.util.Locale.ROOT).contains("duplicate"), e.getCause().getMessage());
    }

    @Test
    void theSameNameIsFreeInAnotherSeason() {
        var name = "Season" + System.nanoTime() % 100000;
        repo.create(faction(name, 1, UUID.randomUUID())).join();
        repo.create(faction(name, 2, UUID.randomUUID())).join();
        assertEquals(1, repo.loadSeason(2).stream().filter(f -> f.name().equals(name)).count());
    }

    @Test
    void membersAreAddedRemovedAndCascadeOnDelete() {
        var leader = UUID.randomUUID();
        var member = UUID.randomUUID();
        var created = faction("Members" + System.nanoTime() % 100000, 1, leader);
        repo.create(created).join();

        repo.addMember(created.id(), member, FactionRank.RECRUIT, T0).join();
        assertEquals(FactionRank.RECRUIT, reload(created.id()).rankOf(member));

        repo.addMember(created.id(), member, FactionRank.OFFICER, T0).join();
        assertEquals(FactionRank.OFFICER, reload(created.id()).rankOf(member), "re-adding updates the rank");

        repo.removeMember(created.id(), member).join();
        assertTrue(reload(created.id()).members().containsKey(leader));
        assertEquals(1, reload(created.id()).members().size());

        repo.delete(created.id()).join();
        assertTrue(repo.loadSeason(1).stream().noneMatch(f -> f.id().equals(created.id())));
        var orphans = MariaDbExtension.database().query(c -> {
            try (var ps = c.prepareStatement("SELECT COUNT(*) FROM faction_members WHERE faction_id = ?")) {
                ps.setString(1, created.id().toString());
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }).join();
        assertEquals(0, orphans, "membership rows must cascade with the faction");
    }

    private FactionRecord reload(UUID id) {
        return repo.loadSeason(1).stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
    }
}
