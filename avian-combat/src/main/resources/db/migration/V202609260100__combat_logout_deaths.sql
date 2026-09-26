-- Players whose logout body was killed while they were offline (spec §43). The body dropped their
-- items, so on their next join the real inventory is cleared and they die. Kept in the database,
-- not in memory, so a restart between the kill and the rejoin cannot duplicate the loot.
CREATE TABLE combat_logout_deaths (
    player     CHAR(36)    NOT NULL,
    killer     CHAR(36)    NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (player)
);
