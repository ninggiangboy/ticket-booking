-- Shared trigger function for immutable tables, e.g. seat_map_version (DR-16).
CREATE FUNCTION forbid_update() RETURNS trigger LANGUAGE plpgsql AS
$$ BEGIN RAISE EXCEPTION '% is immutable', TG_TABLE_NAME; END $$;
