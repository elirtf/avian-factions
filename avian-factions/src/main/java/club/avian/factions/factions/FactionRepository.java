package club.avian.factions.factions;

import club.avian.factions.api.faction.FactionRank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence for Factions (ADR-0002). Writes are single-row upserts and deletes, never a
 * full-table rewrite, and never run on the main thread (#4 prior art, spec §65).
 */
public interface FactionRepository {

    /**
     * Every Faction in the Season with its members. Called once during enable, on the main
     * thread, before the server accepts players (see {@code Database#queryDuringBoot}).
     */
    List<FactionRecord> loadSeason(int seasonId);

    /** Inserts the Faction and its leader row in one transaction. */
    CompletableFuture<Void> create(FactionRecord faction);

    CompletableFuture<Void> addMember(UUID factionId, UUID player, FactionRank rank, Instant at);

    CompletableFuture<Void> removeMember(UUID factionId, UUID player);

    /** Deletes the Faction; membership rows cascade. */
    CompletableFuture<Void> delete(UUID factionId);
}
