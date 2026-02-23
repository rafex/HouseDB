BEGIN;

-- No mandatory seed data.
-- HouseDB depends on UUID identifiers from Kiwi (users, objects, locations),
-- so base data should be loaded by synchronization jobs or integration flows.

COMMIT;
