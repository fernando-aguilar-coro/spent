package com.app.spent.domain.asset

import com.app.spent.data.remote.ExchangeRateApiPriceProvider
import com.app.spent.domain.asset.model.MarketQuote

/**
 * Composite Market Price Provider:
 * Seamlessly pairs real-world live ExchangeRate-API rates for ALL world currencies
 * (supporting 160+ fiat currencies without API keys) with high-fidelity simulated
 * market pricing for stocks, crypto, commodities, and offline fallback.
 */
class CompositeMarketPriceProvider(
    private val exchangeRateApiProvider: ExchangeRateApiPriceProvider,
    private val simulatedEngine: SimulatedMarketPriceEngine
) : MarketPriceProvider {

    companion object {
        private val KNOWN_NON_FIAT = setOf(
            "AAPL", "MSFT", "GOOGL", "TSLA", "NVDA", "AMZN", "VOO", "SPY",
            "BTC", "ETH", "SOL", "ADA", "DOT", "XAU", "XAG"
        )
    }

    override suspend fun getQuote(symbol: String, baseCurrency: String): MarketQuote? {
        return getQuotes(listOf(symbol), baseCurrency)[symbol.trim().uppercase()]
    }

    override suspend fun getQuotes(
        symbols: List<String>,
        baseCurrency: String
    ): Map<String, MarketQuote> {
        val resultMap = mutableMapOf<String, MarketQuote>()
        val upperSymbols = symbols.map { it.trim().uppercase() }.distinct()

        val (nonFiatSymbols, fiatSymbols) = upperSymbols.partition { sym ->
            KNOWN_NON_FIAT.contains(sym)
        }

        // 1. Fetch real-world live rates for fiat currencies via ExchangeRate-API
        val missingFiatSymbols = mutableListOf<String>()
        if (fiatSymbols.isNotEmpty()) {
            val liveResult = exchangeRateApiProvider.fetchRates(fiatSymbols, baseCurrency)
            liveResult.onSuccess { liveQuotes ->
                resultMap.putAll(liveQuotes)
                fiatSymbols.forEach { sym ->
                    if (!liveQuotes.containsKey(sym)) {
                        missingFiatSymbols.add(sym)
                    }
                }
            }.onFailure {
                // Offline fallback
                missingFiatSymbols.addAll(fiatSymbols)
            }
        }

        // 2. Fallback or simulate for non-fiat symbols (stocks, crypto) + missing fiat symbols
        val symbolsForSimulation = (nonFiatSymbols + missingFiatSymbols).distinct()
        if (symbolsForSimulation.isNotEmpty()) {
            val simulatedQuotes = simulatedEngine.getQuotes(symbolsForSimulation, baseCurrency)
            resultMap.putAll(simulatedQuotes)
        }

        return resultMap
    }

    suspend fun tickSimulation() {
        simulatedEngine.tickSimulation()
    }
}
