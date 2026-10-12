package com.app.spent.domain.asset

import com.app.spent.data.remote.CryptoPriceProvider
import com.app.spent.data.remote.ExchangeRateApiPriceProvider
import com.app.spent.data.remote.StockPriceProvider
import com.app.spent.domain.asset.model.MarketQuote

/**
 * Composite Market Price Provider:
 * Seamlessly pairs real-world live ExchangeRate-API rates for ALL world currencies,
 * CoinCap live quotes for Cryptocurrencies, and Yahoo Finance live quotes for Stocks/ETFs,
 * with high-fidelity simulated engine fallback for offline resilience.
 */
class CompositeMarketPriceProvider(
    private val exchangeRateApiProvider: ExchangeRateApiPriceProvider,
    private val simulatedEngine: SimulatedMarketPriceEngine,
    private val cryptoPriceProvider: CryptoPriceProvider? = null,
    private val stockPriceProvider: StockPriceProvider? = null
) : MarketPriceProvider {

    companion object {
        private val KNOWN_CRYPTO = setOf(
            "BTC", "ETH", "SOL", "BNB", "XRP", "ADA", "DOGE", "DOT", "AVAX", "LINK",
            "MATIC", "SHIB", "LTC", "TRX", "NEAR", "UNI", "ICP", "DAI", "BCH", "ETC"
        )
        private val KNOWN_STOCKS = setOf(
            "AAPL", "MSFT", "GOOGL", "TSLA", "NVDA", "AMZN", "META", "VOO", "SPY", "QQQ"
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
        val missingSymbols = mutableListOf<String>()

        val cryptos = upperSymbols.filter { sym ->
            cryptoPriceProvider?.isCryptoSupported(sym) == true || sym in KNOWN_CRYPTO
        }
        val stocks = upperSymbols.filter { sym ->
            stockPriceProvider?.isStockSupported(sym) == true || sym in KNOWN_STOCKS
        }
        val fiats = upperSymbols.filter { sym ->
            sym !in cryptos && sym !in stocks
        }

        // 1. Fetch live rates for fiat currencies via ExchangeRate-API
        if (fiats.isNotEmpty()) {
            val liveResult = exchangeRateApiProvider.fetchRates(fiats, baseCurrency)
            liveResult.onSuccess { liveQuotes ->
                resultMap.putAll(liveQuotes)
                fiats.forEach { sym ->
                    if (!liveQuotes.containsKey(sym)) missingSymbols.add(sym)
                }
            }.onFailure {
                missingSymbols.addAll(fiats)
            }
        }

        // 2. Fetch live crypto quotes via CryptoPriceProvider
        if (cryptos.isNotEmpty()) {
            if (cryptoPriceProvider != null) {
                val cryptoResult = cryptoPriceProvider.fetchQuotes(cryptos, baseCurrency)
                cryptoResult.onSuccess { cryptoQuotes ->
                    resultMap.putAll(cryptoQuotes)
                    cryptos.forEach { sym ->
                        if (!cryptoQuotes.containsKey(sym)) missingSymbols.add(sym)
                    }
                }.onFailure {
                    missingSymbols.addAll(cryptos)
                }
            } else {
                missingSymbols.addAll(cryptos)
            }
        }

        // 3. Fetch live stock quotes via StockPriceProvider
        if (stocks.isNotEmpty()) {
            if (stockPriceProvider != null) {
                val stockResult = stockPriceProvider.fetchQuotes(stocks, baseCurrency)
                stockResult.onSuccess { stockQuotes ->
                    resultMap.putAll(stockQuotes)
                    stocks.forEach { sym ->
                        if (!stockQuotes.containsKey(sym)) missingSymbols.add(sym)
                    }
                }.onFailure {
                    missingSymbols.addAll(stocks)
                }
            } else {
                missingSymbols.addAll(stocks)
            }
        }

        // 4. Fallback to simulation engine for any unmapped or failed symbols
        val symbolsForSimulation = missingSymbols.distinct()
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
