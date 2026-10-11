package com.app.spent.data.remote.dto

/**
 * Standard Foreign Exchange API response (e.g., Frankfurter, OpenExchangeRates, Fixer).
 */
data class ForexRatesResponseDto(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

/**
 * Standard Equity / Stock Quote API response (e.g., Finnhub, Yahoo Finance, AlphaVantage).
 */
data class StockQuoteResponseDto(
    val c: Double,   // Current price
    val d: Double,   // Absolute price change
    val dp: Double,  // Percentage price change
    val h: Double,   // Day high
    val l: Double,   // Day low
    val o: Double,   // Open price
    val pc: Double,  // Previous close
    val t: Long      // Unix timestamp
)

/**
 * Standard Cryptocurrency Market API response (e.g., CoinGecko simple/price or coins/markets).
 */
data class CryptoPriceResponseDto(
    val currentPrice: Double,
    val priceChange24h: Double,
    val priceChangePercentage24h: Double,
    val high24h: Double,
    val low24h: Double,
    val lastUpdatedAt: Long
)
