package com.saqib.fxengine;

// FX currency pairs - could switch to int (e.g., 1 = EUR_USD) for minor performance gain
public enum CurrencyPair {
    EUR_USD, GBP_USD, USD_JPY
    // Add more pairs as needed - ordinal() used for serialisation
}