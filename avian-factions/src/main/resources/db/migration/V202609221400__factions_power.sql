-- Per-player Power (spec §8). Three stored columns; regeneration and offline decay are computed
-- from last_power_update_at on read, so no scheduled task ever walks this table (#4 prior art).
CREATE TABLE player_power (
    player                CHAR(36)     NOT NULL,
    season_id             INT          NOT NULL,
    power                 DOUBLE       NOT NULL,
    power_boost           DOUBLE       NOT NULL DEFAULT 0,
    last_power_update_at  DATETIME(3)  NOT NULL,
    -- NULL means offline. Set on join, cleared on quit; decay only accrues while it is NULL.
    online_since          DATETIME(3)  NULL,
    PRIMARY KEY (player, season_id)
);
