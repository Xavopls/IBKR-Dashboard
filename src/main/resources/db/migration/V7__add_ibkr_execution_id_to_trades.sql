ALTER TABLE trades
ADD COLUMN ibkr_execution_id VARCHAR(100);

CREATE UNIQUE INDEX uq_trades_account_ibkr_execution
ON trades(account_id, ibkr_execution_id)
WHERE ibkr_execution_id IS NOT NULL;

COMMENT ON COLUMN trades.ibkr_execution_id IS 'Stable IBKR execution identifier from Flex/trade reports';
