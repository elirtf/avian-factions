-- Balances and the audit trail (spec §12, §66). Amounts are minor units in BIGINT: money in
-- cents, tokens and gems whole. Never DECIMAL or DOUBLE — a drifting balance is an exploit.
CREATE TABLE balances (
    player     CHAR(36) NOT NULL,
    season_id  INT      NOT NULL,
    currency   VARCHAR(8) NOT NULL,
    amount     BIGINT   NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (player, season_id, currency),
    -- A balance may never go negative; enforced here as well as in the application so a bug in
    -- one withdrawal path cannot mint money.
    CONSTRAINT chk_balances_non_negative CHECK (amount >= 0)
);

-- Immutable: rows are inserted, never updated or deleted (spec §66 "audit records must be
-- queryable"; #4 FUUID #1363 — "bank cleared for no reason" is unanswerable without this).
CREATE TABLE economy_transactions (
    id             CHAR(36)    NOT NULL,
    season_id      INT         NOT NULL,
    player         CHAR(36)    NOT NULL,
    currency       VARCHAR(8)  NOT NULL,
    action         VARCHAR(16) NOT NULL,
    amount         BIGINT      NOT NULL,
    balance_before BIGINT      NOT NULL,
    balance_after  BIGINT      NOT NULL,
    reason         VARCHAR(64) NOT NULL,
    reference_id   CHAR(36)    NULL,
    created_at     DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_economy_tx_player (player, created_at),
    KEY idx_economy_tx_reason (reason, created_at)
);
