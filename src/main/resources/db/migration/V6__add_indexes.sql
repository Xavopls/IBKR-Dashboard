-- Performance indexes on the most commonly filtered / sorted columns

-- Trades: date range queries are the primary access pattern
CREATE INDEX idx_trades_trade_date        ON trades (trade_date DESC);
CREATE INDEX idx_trades_account_date      ON trades (account_id, trade_date DESC);
CREATE INDEX idx_trades_instrument_id     ON trades (instrument_id);
CREATE INDEX idx_trades_strategy_tag      ON trades (strategy_tag) WHERE strategy_tag IS NOT NULL;
CREATE INDEX idx_trades_direction         ON trades (direction);
CREATE INDEX idx_trades_realized_pnl      ON trades (realized_pnl) WHERE realized_pnl IS NOT NULL;

-- Daily P&L: always queried ordered by date for a specific account
CREATE INDEX idx_daily_pnl_account_date   ON daily_pnl (account_id, date ASC);

-- Positions: most often fetched by account
CREATE INDEX idx_positions_account_id     ON positions (account_id);

-- Instruments: symbol lookups and full-text search
CREATE INDEX idx_instruments_symbol       ON instruments (symbol);
CREATE INDEX idx_instruments_asset_class  ON instruments (asset_class);
