-- Claimed chunks (spec §9). One row per chunk; the runtime index lives in memory and this table
-- is loaded once at boot, then only written to (#4 prior art).
CREATE TABLE faction_claims (
    world      VARCHAR(64) NOT NULL,
    chunk_x    INT         NOT NULL,
    chunk_z    INT         NOT NULL,
    season_id  INT         NOT NULL,
    faction_id CHAR(36)    NOT NULL,
    claimed_by CHAR(36)    NOT NULL,
    claimed_at DATETIME(3) NOT NULL,
    -- A chunk has at most one owner per season; the PK is the uniqueness rule.
    PRIMARY KEY (world, chunk_x, chunk_z, season_id),
    KEY idx_faction_claims_faction (faction_id),
    CONSTRAINT fk_faction_claims_faction FOREIGN KEY (faction_id) REFERENCES factions (id) ON DELETE CASCADE
);
