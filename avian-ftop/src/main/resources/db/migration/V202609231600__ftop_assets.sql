-- F-Top (spec §11). Every player-placed spawner stack and valuable block, wherever it is; which
-- faction it counts for is decided at calculation time from the claim it sits in, so claiming,
-- unclaiming and overclaiming need no bookkeeping here.
CREATE TABLE ftop_assets (
    world       CHAR(36)    NOT NULL,
    x           INT         NOT NULL,
    y           INT         NOT NULL,
    z           INT         NOT NULL,
    kind        VARCHAR(8)  NOT NULL,   -- SPAWNER or BLOCK
    type        VARCHAR(64) NOT NULL,   -- entity type for spawners, material for blocks
    amount      INT         NOT NULL,   -- stack size; always 1 for blocks
    placed_at   BIGINT      NOT NULL,   -- epoch ms, averaged over the stack, for value aging
    grace_until BIGINT      NOT NULL,   -- epoch ms; until then grace_units can be picked up free
    grace_units INT         NOT NULL,
    PRIMARY KEY (world, x, y, z),
    CONSTRAINT chk_ftop_assets_amount CHECK (amount >= 1)
);

-- The latest ranking, replaced whole on every calculation (spec §11: store the last calculation
-- time and a breakdown for debugging). Readable by anything outside the server, like a website.
CREATE TABLE ftop_results (
    rank_no       INT          NOT NULL,
    faction_id    INT          NOT NULL,
    faction_tag   VARCHAR(64)  NOT NULL,
    total_value   BIGINT       NOT NULL,
    spawner_value BIGINT       NOT NULL,
    block_value   BIGINT       NOT NULL,
    breakdown     TEXT         NOT NULL,   -- "ZOMBIE x12, DIAMOND_BLOCK x40, …"
    calculated_at DATETIME(3)  NOT NULL,
    PRIMARY KEY (rank_no)
);
