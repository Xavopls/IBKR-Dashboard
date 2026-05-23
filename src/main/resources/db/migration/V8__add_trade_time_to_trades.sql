ALTER TABLE trades
ADD COLUMN trade_time TIMESTAMP;

UPDATE trades
SET trade_time = trade_date::timestamp
WHERE trade_time IS NULL;

COMMENT ON COLUMN trades.trade_time IS 'Full trade execution timestamp from IBKR when available';
