-- Trades: individual executed trades associated with an account and instrument
CREATE TABLE trades (
    id              BIGSERIAL      PRIMARY KEY,
    account_id      BIGINT         NOT NULL REFERENCES accounts(id),
    instrument_id   BIGINT         NOT NULL REFERENCES instruments(id),
    trade_date      DATE           NOT NULL,
    settlement_date DATE,
    direction       VARCHAR(10)    NOT NULL CHECK (direction IN ('BUY','SELL')),
    quantity        NUMERIC(20,6)  NOT NULL CHECK (quantity > 0),
    price           NUMERIC(20,6)  NOT NULL CHECK (price > 0),
    commission      NUMERIC(20,6),
    net_amount      NUMERIC(20,6),
    realized_pnl    NUMERIC(20,6),
    strategy_tag    VARCHAR(100),
    notes           VARCHAR(1000)
);

COMMENT ON TABLE  trades IS 'Individual trade executions imported from IBKR or entered manually';
COMMENT ON COLUMN trades.net_amount    IS 'Signed cash-flow impact: negative for buys, positive for sells';
COMMENT ON COLUMN trades.realized_pnl  IS 'P&L for closing trades; NULL for opening trades';
COMMENT ON COLUMN trades.strategy_tag  IS 'User-defined label for grouping trades by strategy';
