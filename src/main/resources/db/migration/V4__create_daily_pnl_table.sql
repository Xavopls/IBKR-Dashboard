-- Daily P&L: one row per account per trading day
-- cumulative_pnl is NOT stored here; it is computed on-the-fly via a window function
-- to avoid stale data when historical rows are updated.
CREATE TABLE daily_pnl (
    id              BIGSERIAL     PRIMARY KEY,
    account_id      BIGINT        NOT NULL REFERENCES accounts(id),
    date            DATE          NOT NULL,
    realized_pnl    NUMERIC(20,6),
    unrealized_pnl  NUMERIC(20,6),
    total_pnl       NUMERIC(20,6),

    CONSTRAINT uq_daily_pnl_account_date UNIQUE (account_id, date)
);

COMMENT ON TABLE  daily_pnl IS 'Daily P&L snapshot per account; cumulative values are derived via window functions at query time';
COMMENT ON COLUMN daily_pnl.total_pnl IS 'realized_pnl + unrealized_pnl for the day';
