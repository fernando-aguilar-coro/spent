package com.app.spent.domain.asset

import com.app.spent.domain.asset.model.MarketQuote

/**
 * Contract for fetching asset quotes from external or simulated feeds.
 */
interface MarketPriceProvider {
    suspend fun getQuote(symbol: String, baseCurrency: String = "USD"): MarketQuote?
    suspend fun getQuotes(symbols: List<String>, baseCurrency: String = "USD"): Map<String, MarketQuote>
}
