-- Instruments: tradeable financial instruments (stocks, options, futures, etc.)
CREATE TABLE instruments (
    id         BIGSERIAL    PRIMARY KEY,
    symbol     VARCHAR(50)  NOT NULL,
    name       VARCHAR(200),
    asset_class VARCHAR(20) NOT NULL
                            CHECK (asset_class IN ('STOCK','OPTION','FUTURE','FOREX','CRYPTO')),
    sector     VARCHAR(100),
    exchange   VARCHAR(50),
    currency   VARCHAR(10)
                            CHECK (currency IS NULL OR currency IN (
                                'USD','EUR','GBP','JPY','CAD','AUD','CHF',
                                'HKD','SGD','SEK','NOK','DKK','NZD','MXN',
                                'BTC','ETH'
                            )),

    CONSTRAINT uq_instruments_symbol_exchange UNIQUE (symbol, exchange)
);

COMMENT ON TABLE  instruments IS 'Reference data for tradeable financial instruments';
COMMENT ON COLUMN instruments.symbol IS 'Ticker symbol as used in IBKR exports';
COMMENT ON COLUMN instruments.asset_class IS 'Instrument category: STOCK, OPTION, FUTURE, FOREX, CRYPTO';
