-- Avian Player profiles (spec §5, §6). UUID stored as CHAR(36) for portability and readability.
-- Migration naming: V<yyyyMMddHHmm>__<module>_<description>.sql — timestamps keep versions unique
-- across modules that share this one classpath location.
-- Dialect note (ADR-0002): DATETIME(3) rather than TIMESTAMP to avoid MariaDB's 2038 limit and
-- implicit ON UPDATE semantics; all timestamps are UTC instants written explicitly by the app.
CREATE TABLE players (
    uuid          CHAR(36)    NOT NULL,
    name          VARCHAR(16) NOT NULL,
    first_join_at DATETIME(3) NOT NULL,
    last_seen_at  DATETIME(3) NOT NULL,
    created_at    DATETIME(3) NOT NULL,
    updated_at    DATETIME(3) NOT NULL,
    PRIMARY KEY (uuid)
);

CREATE INDEX idx_players_name ON players (name);
