package com.xavopls.ibkr_dashboard.enums;

/**
 * Indicates whether a trade was an opening purchase or closing sale.
 * In IBKR exports a positive quantity corresponds to BUY and a negative quantity to SELL.
 */
public enum TradeDirection {
    BUY,
    SELL
}
