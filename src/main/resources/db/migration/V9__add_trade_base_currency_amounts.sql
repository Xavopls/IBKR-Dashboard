ALTER TABLE trades
ADD COLUMN trade_currency VARCHAR(10),
ADD COLUMN fx_rate_to_base NUMERIC(20,10),
ADD COLUMN realized_pnl_base NUMERIC(20,6),
ADD COLUMN net_amount_base NUMERIC(20,6),
ADD COLUMN commission_base NUMERIC(20,6);

UPDATE trades
SET trade_currency = i.currency
FROM instruments i
WHERE trades.instrument_id = i.id
  AND trades.trade_currency IS NULL;

COMMENT ON COLUMN trades.trade_currency IS 'Currency of raw IBKR trade amounts';
COMMENT ON COLUMN trades.fx_rate_to_base IS 'IBKR FX rate from trade currency to account base currency';
COMMENT ON COLUMN trades.realized_pnl_base IS 'Realized P&L converted to account base currency';
COMMENT ON COLUMN trades.net_amount_base IS 'Net cash amount converted to account base currency';
COMMENT ON COLUMN trades.commission_base IS 'Commission converted to account base currency';
