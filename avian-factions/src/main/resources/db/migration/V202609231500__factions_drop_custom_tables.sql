-- ADR-0007: FactionsUUID owns factions, claims and power, and stores them itself. The tables from
-- our own implementation are dropped; their CREATE migrations stay so Flyway's history validates.
DROP TABLE IF EXISTS faction_claims;
DROP TABLE IF EXISTS faction_members;
DROP TABLE IF EXISTS player_power;
DROP TABLE IF EXISTS factions;
