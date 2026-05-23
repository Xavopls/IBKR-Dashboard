-- Positions: current holdings snapshot (one row per account + instrument combination)
CREATE TABLE positions (
    id              BIGSERIAL     PRIMARY KEY,
    version         BIGINT        NOT NULL DEFAULT 0,   -- optimistic locking
    account_id      BIGINT        NOT NULL REFERENCES accounts(id),
    instrument_id   BIGINT        NOT NULL REFERENCES instruments(id),
    quantity        NUMERIC(20,6),
    avg_cost        NUMERIC(20,6),
    current_price   NUMERIC(20,6),
    unrealized_pnl  NUMERIC(20,6),
    last_updated    TIMESTAMP,

    CONSTRAINT uq_positions_account_instrument UNIQUE (account_id, instrument_id)
);

COMMENT ON TABLE  positions IS 'Current position snapshot; upserted on each CSV import';
COMMENT ON COLUMN positions.version IS 'JPA optimistic locking version column';
COMMENT ON COLUMN positions.avg_cost IS 'Average cost basis per unit of the position';
