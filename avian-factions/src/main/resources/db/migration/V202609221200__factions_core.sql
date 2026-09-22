-- Factions and membership (spec §7). Claims, power and relations arrive in later migrations.
-- Dialect note (ADR-0002): DATETIME(3) over TIMESTAMP (2038, implicit ON UPDATE); UUIDs CHAR(36).
CREATE TABLE factions (
    id         CHAR(36)    NOT NULL,
    season_id  INT         NOT NULL,
    name       VARCHAR(16) NOT NULL,
    -- Uniqueness must be case-insensitive ("Ravens" and "ravens" are the same faction to players).
    -- Stored lower-cased so the unique index does not depend on the column collation.
    name_key   VARCHAR(16) NOT NULL,
    leader     CHAR(36)    NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_factions_season_name (season_id, name_key)
);

CREATE TABLE faction_members (
    faction_id CHAR(36)    NOT NULL,
    player     CHAR(36)    NOT NULL,
    rank       VARCHAR(16) NOT NULL,
    joined_at  DATETIME(3) NOT NULL,
    PRIMARY KEY (faction_id, player),
    -- A player belongs to at most one Faction per Season; the season lives on the faction row, so
    -- this index plus the application check is what enforces it.
    KEY idx_faction_members_player (player),
    CONSTRAINT fk_faction_members_faction FOREIGN KEY (faction_id) REFERENCES factions (id) ON DELETE CASCADE
);
