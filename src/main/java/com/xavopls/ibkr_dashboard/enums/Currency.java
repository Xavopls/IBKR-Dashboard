package com.xavopls.ibkr_dashboard.enums;

/**
 * ISO 4217 currency codes for the currencies supported by this application.
 * Additional codes can be appended without schema changes because currency
 * columns are stored as VARCHAR, not as database-native enum types.
 */
public enum Currency {
    USD,
    EUR,
    GBP,
    JPY,
    CAD,
    AUD,
    CHF,
    HKD,
    SGD,
    SEK,
    NOK,
    DKK,
    NZD,
    MXN,
    BTC,
    ETH
}
