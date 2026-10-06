CREATE TABLE spike_item (
    id      uuid PRIMARY KEY DEFAULT uuidv7(),
    status  text NOT NULL CHECK (status IN ('HELD', 'CONFIRMED', 'EXPIRED')),
    payload jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE spike_line (
    item_id uuid NOT NULL REFERENCES spike_item (id),
    sku     text NOT NULL
);

CREATE INDEX spike_item_held_idx ON spike_item (id) WHERE status = 'HELD';

CREATE FUNCTION forbid_update() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'table % is immutable', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE spike_ledger (
    id     uuid PRIMARY KEY DEFAULT uuidv7(),
    amount bigint NOT NULL
);

CREATE TRIGGER spike_ledger_no_update BEFORE UPDATE ON spike_ledger
    FOR EACH ROW EXECUTE FUNCTION forbid_update();
