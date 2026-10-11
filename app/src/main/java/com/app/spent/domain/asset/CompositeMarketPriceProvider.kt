package com.app.spent.domain.asset

import com.app.spent.data.remote.FrankfurterPriceProvider
import com.app.spent.domain.asset.model.MarketQuote

/**
 * Composite Market Price Provider:
 * Seamlessly pairs real-world live Frankfurter ECB rates for foreign currencies
 * with high-fidelity simulated market pricing for stocks, crypto, commodities,
 * and offline fallback.
 */
class CompositeMarketPriceProvider(
    private val frankfurterProvider: FrankfurterPriceProvider,
    private val simulatedEngine: SimulatedMarketPriceEngine
) : MarketPriceProvider {

    override suspend fun getQuote(symbol: String, baseCurrency: String): MarketQuote? {
        return getQuotes(listOf(symbol), baseCurrency)[symbol.trim().uppercase()]
    }

    override suspend fun getQuotes(
        symbols: List<String>,
        baseCurrency: String
    ): Map<String, MarketQuote> {
        val resultMap = mutableMapOf<String, MarketQuote>()
        val upperSymbols = symbols.map { it.trim().uppercase() }.distinct()

        val (fiatSymbols, nonFiatSymbols) = upperSymbols.partition { sym ->
            frankfurterProvider.isCurrencySupported(sym)
        }

        // 1. Fetch real-world live rates for supported currencies via Frankfurter
        val missingFiatSymbols = mutableListOf<String>()
        if (fiatSymbols.isNotEmpty()) {
            val liveResult = frankfurterProvider.fetchRates(fiatSymbols, baseCurrency)
            liveResult.onSuccess { liveQuotes ->
                resultMap.putAll(liveQuotes)
                // Identify any requested fiat symbols that Frankfurter didn't return
                fiatSymbols.forEach { sym ->
                    if (!liveQuotes.containsKey(sym)) {
                        missingFiatSymbols.add(sym)
                    }
                }
            }.onFailure {
                // If offline or network error, fallback to simulated engine for all fiat symbols
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

    /**
     * Advance market simulation ticks for simulated assets.
     */
    suspend fun tickSimulation() {
        simulatedEngine.tickSimulation()
    }
}
