package com.app.spent.domain.asset

import com.app.spent.data.remote.dto.CryptoPriceResponseDto
import com.app.spent.data.remote.dto.ForexRatesResponseDto
import com.app.spent.data.remote.dto.StockQuoteResponseDto
import com.app.spent.domain.asset.model.MarketQuote
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * High-fidelity market simulation engine.
 * Generates realistic real-time price fluctuations and maps from standard API DTOs.
 */
class SimulatedMarketPriceEngine(
    private val random: Random = Random(System.currentTimeMillis())
) : MarketPriceProvider {

    private val mutex = Mutex()

    // Internal state tracking for symbols: (Current Price in USD, Initial Reference Price, High, Low)
    private val marketState = ConcurrentHashMap<String, SimulatedTickerState>()

    init {
        initializeBaselinePrices()
    }

    private fun initializeBaselinePrices() {
        val baselines = mapOf(
            // Stocks / ETFs
            "AAPL" to 228.50,
            "MSFT" to 420.25,
            "GOOGL" to 175.40,
            "TSLA" to 215.80,
            "NVDA" to 132.60,
            "AMZN" to 186.20,
            "VOO" to 512.00,
            "SPY" to 576.50,

            // Cryptocurrencies
            "BTC" to 65400.00,
            "ETH" to 3450.00,
            "SOL" to 154.20,
            "ADA" to 0.38,
            "DOT" to 4.55,

            // Forex (Value of 1 Unit in USD)
            "EUR" to 1.085,
            "GBP" to 1.305,
            "JPY" to 0.0066,
            "CAD" to 0.730,
            "AUD" to 0.665,
            "MXN" to 0.052,
            "CHF" to 1.160,

            // Commodities
            "XAU" to 2650.00,
            "XAG" to 31.80
        )

        baselines.forEach { (symbol, price) ->
            marketState[symbol.uppercase()] = SimulatedTickerState(
                symbol = symbol.uppercase(),
                currentPrice = price,
                openPrice = price,
                high = price * 1.01,
                low = price * 0.99,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * Advance the market by one simulation tick.
     * Generates simulated Brownian motion drift (-1.8% to +1.8%).
     */
    suspend fun tickSimulation() = mutex.withLock {
        val now = System.currentTimeMillis()
        marketState.forEach { (symbol, state) ->
            // Volatility scale: Crypto higher, Forex lower, Stocks medium
            val volatilityMultiplier = when {
                symbol in listOf("BTC", "ETH", "SOL", "ADA", "DOT") -> 0.025
                symbol in listOf("EUR", "GBP", "JPY", "CAD", "AUD", "MXN", "CHF") -> 0.004
                else -> 0.012
            }

            val deltaPct = (random.nextDouble(-1.0, 1.0)) * volatilityMultiplier
            val newPrice = max(0.000001, state.currentPrice * (1.0 + deltaPct))
            val newHigh = max(state.high, newPrice)
            val newLow = min(state.low, newPrice)

            marketState[symbol] = state.copy(
                currentPrice = newPrice,
                high = newHigh,
                low = newLow,
                lastUpdated = now
            )
        }
    }

    override suspend fun getQuote(symbol: String, baseCurrency: String): MarketQuote? {
        return getQuotes(listOf(symbol), baseCurrency)[symbol.uppercase()]
    }

    override suspend fun getQuotes(symbols: List<String>, baseCurrency: String): Map<String, MarketQuote> = mutex.withLock {
        val baseMultiplier = getBaseCurrencyToUsdRate(baseCurrency)
        val result = mutableMapOf<String, MarketQuote>()

        for (sym in symbols) {
            val upper = sym.trim().uppercase()
            val state = marketState.getOrPut(upper) {
                // Dynamic baseline for arbitrary custom tickers
                val base = 100.0
                SimulatedTickerState(
                    symbol = upper,
                    currentPrice = base,
                    openPrice = base,
                    high = base,
                    low = base,
                    lastUpdated = System.currentTimeMillis()
                )
            }

            // Simulate DTO creation and mapping as if received from actual APIs
            val quote = mapStateToDomainQuote(state, baseMultiplier)
            result[upper] = quote
        }

        result
    }

    /**
     * Demonstrates mapping from realistic API DTOs into the domain model.
     */
    private fun mapStateToDomainQuote(
        state: SimulatedTickerState,
        baseCurrencyToUsdMultiplier: Double
    ): MarketQuote {
        val priceInBaseCurrency = state.currentPrice / baseCurrencyToUsdMultiplier
        val change24hPct = if (state.openPrice > 0) {
            ((state.currentPrice - state.openPrice) / state.openPrice) * 100.0
        } else 0.0

        // Example: simulate parsing through StockQuoteResponseDto or CryptoPriceResponseDto
        if (state.symbol in listOf("BTC", "ETH", "SOL")) {
            val cryptoDto = CryptoPriceResponseDto(
                currentPrice = priceInBaseCurrency,
                priceChange24h = priceInBaseCurrency - (state.openPrice / baseCurrencyToUsdMultiplier),
                priceChangePercentage24h = change24hPct,
                high24h = state.high / baseCurrencyToUsdMultiplier,
                low24h = state.low / baseCurrencyToUsdMultiplier,
                lastUpdatedAt = state.lastUpdated
            )
            return MarketQuote(
                symbol = state.symbol,
                priceInBaseCurrency = cryptoDto.currentPrice,
                change24hPct = cryptoDto.priceChangePercentage24h,
                high24h = cryptoDto.high24h,
                low24h = cryptoDto.low24h,
                lastUpdated = cryptoDto.lastUpdatedAt
            )
        }

        val stockDto = StockQuoteResponseDto(
            c = priceInBaseCurrency,
            d = priceInBaseCurrency - (state.openPrice / baseCurrencyToUsdMultiplier),
            dp = change24hPct,
            h = state.high / baseCurrencyToUsdMultiplier,
            l = state.low / baseCurrencyToUsdMultiplier,
            o = state.openPrice / baseCurrencyToUsdMultiplier,
            pc = state.openPrice / baseCurrencyToUsdMultiplier,
            t = state.lastUpdated / 1000
        )

        return MarketQuote(
            symbol = state.symbol,
            priceInBaseCurrency = stockDto.c,
            change24hPct = stockDto.dp,
            high24h = stockDto.h,
            low24h = stockDto.l,
            lastUpdated = state.lastUpdated
        )
    }

    private fun getBaseCurrencyToUsdRate(baseCurrency: String): Double {
        val upper = baseCurrency.uppercase()
        if (upper == "USD") return 1.0
        val state = marketState[upper]
        return state?.currentPrice ?: 1.0
    }

    private data class SimulatedTickerState(
        val symbol: String,
        val currentPrice: Double,
        val openPrice: Double,
        val high: Double,
        val low: Double,
        val lastUpdated: Long
    )
}
