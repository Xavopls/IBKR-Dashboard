-- Accounts: brokerage accounts held at IBKR
CREATE TABLE accounts (
    id             BIGSERIAL    PRIMARY KEY,
    account_number VARCHAR(50)  NOT NULL,
    account_name   VARCHAR(100) NOT NULL,
    currency       VARCHAR(10)  NOT NULL
                                CHECK (currency IN (
                                    'USD','EUR','GBP','JPY','CAD','AUD','CHF',
                                    'HKD','SGD','SEK','NOK','DKK','NZD','MXN',
                                    'BTC','ETH'
                                )),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_accounts_account_number UNIQUE (account_number)
);

COMMENT ON TABLE  accounts IS 'Brokerage accounts held at Interactive Brokers';
COMMENT ON COLUMN accounts.account_number IS 'IBKR account identifier, e.g. U1234567';
COMMENT ON COLUMN accounts.currency IS 'Base currency of the account (ISO 4217)';
