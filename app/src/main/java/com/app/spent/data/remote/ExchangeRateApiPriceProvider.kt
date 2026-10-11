package com.app.spent.data.remote

import com.app.spent.domain.asset.model.MarketQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live Foreign Exchange rate provider utilizing the free, open-access ExchangeRate-API
 * (https://open.er-api.com/v6/latest/{base}) which supports 160+ world currencies
 * (including BOB, ARS, USD, EUR, BRL, PEN, COP, CLP, UYU, MXN, GBP, JPY, etc.)
 * with zero API key requirement.
 */
class ExchangeRateApiPriceProvider(
    private val baseUrl: String = "https://open.er-api.com/v6/latest",
    private val httpFetcher: (suspend (String) -> String)? = null
) {

    // Simple in-memory cache per base currency
    private data class CachedRates(
        val rates: Map<String, Double>,
        val expiresAtEpochSeconds: Long
    )

    private val cache = mutableMapOf<String, CachedRates>()

    /**
     * Fetches live exchange rates for the given symbols against [baseCurrency].
     * If symbols contains foreign fiat currencies, their unit price in [baseCurrency] is calculated.
     * Also returns the USD conversion rate if present so equities/crypto can be converted to [baseCurrency].
     */
    suspend fun fetchRates(
        symbols: List<String>,
        baseCurrency: String = "USD"
    ): Result<Map<String, MarketQuote>> = withContext(Dispatchers.IO) {
        runCatching {
            val upperBase = baseCurrency.trim().uppercase()
            val upperSymbols = symbols.map { it.trim().uppercase() }.distinct()

            if (upperSymbols.isEmpty()) {
                return@runCatching emptyMap()
            }

            val nowSeconds = System.currentTimeMillis() / 1000
            val cached = cache[upperBase]
            val ratesMap = if (cached != null && cached.expiresAtEpochSeconds > nowSeconds) {
                cached.rates
            } else {
                val urlString = "$baseUrl/$upperBase"
                val jsonString = if (httpFetcher != null) {
                    httpFetcher.invoke(urlString)
                } else {
                    executeHttpRequest(urlString)
                }

                val parsed = parseRatesJson(jsonString)
                if (parsed.rates.isNotEmpty()) {
                    cache[upperBase] = parsed
                }
                parsed.rates
            }

            buildQuotesFromRates(ratesMap, upperBase, upperSymbols)
        }
    }

    /**
     * Returns the rate to convert 1 USD to [baseCurrency].
     * Useful for converting US stock/crypto market prices into the user's base currency.
     */
    suspend fun getUsdToBaseMultiplier(baseCurrency: String): Double {
        val upperBase = baseCurrency.trim().uppercase()
        if (upperBase == "USD") return 1.0

        val result = fetchRates(listOf("USD"), upperBase)
        val usdQuote = result.getOrNull()?.get("USD")
        return usdQuote?.priceInBaseCurrency ?: 1.0
    }

    /**
     * Parses raw JSON string from ExchangeRate-API directly, exposed for unit testing and direct evaluations.
     */
    fun parseExchangeRateJson(
        jsonString: String,
        baseCurrency: String,
        targetSymbols: List<String>
    ): Map<String, MarketQuote> {
        val cached = parseRatesJson(jsonString)
        return buildQuotesFromRates(cached.rates, baseCurrency.trim().uppercase(), targetSymbols.map { it.trim().uppercase() })
    }

    /**
     * Checks if a symbol corresponds to a fiat currency supported by ExchangeRate-API.
     */
    fun isCurrencySupported(symbol: String): Boolean {
        val sym = symbol.trim().uppercase()
        val nonCurrencies = setOf(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "VOO", "SPY", "QQQ", "NVDA",
            "BTC", "ETH", "SOL", "USDT", "ADA", "DOT", "XAU", "XAG"
        )
        return sym.length == 3 && sym !in nonCurrencies
    }

    private fun parseRatesJson(jsonString: String): CachedRates {
        val root = JSONObject(jsonString)
        val ratesObj = root.optJSONObject("rates") ?: return CachedRates(emptyMap(), 0L)
        val nextUpdateUnix = root.optLong("time_next_update_unix", (System.currentTimeMillis() / 1000) + 3600)

        val map = mutableMapOf<String, Double>()
        val keys = ratesObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key.uppercase()] = ratesObj.optDouble(key, 0.0)
        }

        return CachedRates(rates = map, expiresAtEpochSeconds = nextUpdateUnix)
    }

    private fun buildQuotesFromRates(
        rates: Map<String, Double>,
        baseCurrency: String,
        targetSymbols: List<String>
    ): Map<String, MarketQuote> {
        val now = System.currentTimeMillis()
        val quotes = mutableMapOf<String, MarketQuote>()

        // Base currency itself is always 1.0 unit
        if (targetSymbols.contains(baseCurrency)) {
            quotes[baseCurrency] = MarketQuote(
                symbol = baseCurrency,
                priceInBaseCurrency = 1.0,
                change24hPct = 0.0,
                lastUpdated = now
            )
        }

        for (sym in targetSymbols) {
            if (sym == baseCurrency) continue

            val rate = rates[sym]
            if (rate != null && rate > 0.0) {
                // In ExchangeRate-API, rates[sym] tells how much of 'sym' 1 unit of 'baseCurrency' buys.
                // Therefore, 1 unit of 'sym' is worth (1.0 / rate) units of 'baseCurrency'.
                val priceInBase = 1.0 / rate
                quotes[sym] = MarketQuote(
                    symbol = sym,
                    priceInBaseCurrency = priceInBase,
                    change24hPct = 0.0,
                    lastUpdated = now
                )
            }
        }

        return quotes
    }

    private fun executeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Spent-Finance-App/1.0")

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw Exception("ExchangeRate-API request failed with HTTP code $responseCode")
        }

        return BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }
}
