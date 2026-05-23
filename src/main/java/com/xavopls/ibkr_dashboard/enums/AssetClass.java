package com.xavopls.ibkr_dashboard.enums;

/**
 * Represents the category of financial instrument being traded.
 * Maps to the "Asset Category" field in IBKR Flex Query exports.
 */
public enum AssetClass {
    STOCK,
    OPTION,
    FUTURE,
    FOREX,
    CRYPTO;

    /**
     * Converts an IBKR Flex Query asset category string to the corresponding enum constant.
     * IBKR uses full words like "Stocks", "Options", "Futures", etc.
     */
    public static AssetClass fromIbkrLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Asset class label must not be null");
        }
        return switch (label.trim().toUpperCase()) {
            case "STOCKS", "STK"             -> STOCK;
            case "OPTIONS", "OPT"            -> OPTION;
            case "FUTURES", "FUT"            -> FUTURE;
            case "FOREX", "CASH"             -> FOREX;
            case "CRYPTO", "CRYPTOCURRENCY"  -> CRYPTO;
            default -> throw new IllegalArgumentException("Unknown IBKR asset class: " + label);
        };
    }
}
