-- Seasons and F-Top payouts (spec §50-52; owner, 2026-10-16). Nothing is written until /season start.
-- seasons.id is the season number, the same as economy.conf's season-id.
CREATE TABLE seasons (
    id         INT         NOT NULL,
    started_at DATETIME(3) NOT NULL,
    ends_at    DATETIME(3) NOT NULL,
    status     VARCHAR(16) NOT NULL,   -- ACTIVE, ENDED
    ended_at   DATETIME(3) NULL,
    PRIMARY KEY (id)
);

-- One row per payout that has run: claiming it first is what stops a payout running twice (two ticks, a
-- restart mid-payout). kind is WEEKLY or SEASON; period is the payout's date, or 'final'.
CREATE TABLE ftop_payout_runs (
    season_id INT         NOT NULL,
    kind      VARCHAR(16) NOT NULL,
    period    VARCHAR(16) NOT NULL,
    run_at    DATETIME(3) NOT NULL,
    PRIMARY KEY (season_id, kind, period)
);

-- What each placed faction got, for the record and for answering "where's my payout?".
CREATE TABLE ftop_payouts (
    season_id   INT          NOT NULL,
    kind        VARCHAR(16)  NOT NULL,
    period      VARCHAR(16)  NOT NULL,
    place       INT          NOT NULL,
    faction_id  INT          NOT NULL,
    faction_tag VARCHAR(64)  NOT NULL,
    value       BIGINT       NOT NULL,
    members     INT          NOT NULL,
    money_each  BIGINT       NOT NULL,
    tokens_each BIGINT       NOT NULL,
    paid_at     DATETIME(3)  NOT NULL,
    PRIMARY KEY (season_id, kind, period, place)
);

-- The final F-Top, frozen when a season ends (spec §51 step 5).
CREATE TABLE season_rankings (
    season_id   INT         NOT NULL,
    place       INT         NOT NULL,
    faction_id  INT         NOT NULL,
    faction_tag VARCHAR(64) NOT NULL,
    value       BIGINT      NOT NULL,
    PRIMARY KEY (season_id, place)
);
