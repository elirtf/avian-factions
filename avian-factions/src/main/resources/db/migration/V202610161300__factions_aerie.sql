-- The Aerie (#21): who holds it, since when, and whether this hold has earned its milestone. One row per
-- Aerie (id 'aerie'); no row means nobody holds it. Capture progress isn't kept: it's minutes long and
-- starts over after a restart. holder is FactionsUUID's faction id.
CREATE TABLE factions_aerie (
    id             VARCHAR(32) NOT NULL,
    holder         INT         NOT NULL,
    held_since     DATETIME(3) NOT NULL,
    milestone_done BOOLEAN     NOT NULL,
    PRIMARY KEY (id)
);
