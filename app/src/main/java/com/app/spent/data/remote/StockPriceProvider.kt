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
 * Real-world Stock / ETF market price provider connecting to the public Yahoo Finance chart quote API
 * (https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d).
 * Retrieves live/delayed market quotes and 24h percentage changes for US equities and ETFs
 * (AAPL, MSFT, GOOGL, AMZN, TSLA, NVDA, META, VOO, SPY, QQQ, etc.),
 * converting into the user's base currency via [ExchangeRateApiPriceProvider].
 */
class StockPriceProvider(
    private val exchangeRateApiProvider: ExchangeRateApiPriceProvider,
    private val baseUrl: String = "https://query1.finance.yahoo.com/v8/finance/chart",
    private val httpFetcher: (suspend (String) -> String)? = null
) {

    private data class CachedStockQuote(
        val priceUsd: Double,
        val change24hPct: Double,
        val expiresAtEpochSeconds: Long
    )

    private val cache = mutableMapOf<String, CachedStockQuote>()

    suspend fun fetchQuotes(
        symbols: List<String>,
        baseCurrency: String = "USD"
    ): Result<Map<String, MarketQuote>> = withContext(Dispatchers.IO) {
        runCatching {
            val upperSymbols = symbols.map { it.trim().uppercase() }.distinct()
            if (upperSymbols.isEmpty()) return@runCatching emptyMap()

            val usdMultiplier = exchangeRateApiProvider.getUsdToBaseMultiplier(baseCurrency)
            val nowSeconds = System.currentTimeMillis() / 1000
            val nowMillis = System.currentTimeMillis()
            val resultMap = mutableMapOf<String, MarketQuote>()

            for (sym in upperSymbols) {
                val cached = cache[sym]
                val stockData = if (cached != null && cached.expiresAtEpochSeconds > nowSeconds) {
                    cached
                } else {
                    val urlString = "$baseUrl/$sym?interval=1d&range=1d"
                    val jsonString = if (httpFetcher != null) {
                        httpFetcher.invoke(urlString)
                    } else {
                        executeHttpRequest(urlString)
                    }
                    val parsed = parseYahooChartJson(jsonString, sym)
                    if (parsed != null) {
                        val toCache = CachedStockQuote(
                            priceUsd = parsed.priceInBaseCurrency,
                            change24hPct = parsed.change24hPct,
                            expiresAtEpochSeconds = nowSeconds + 900 // 15-minute cache
                        )
                        cache[sym] = toCache
                        toCache
                    } else {
                        null
                    }
                }

                if (stockData != null && stockData.priceUsd > 0.0) {
                    val convertedPrice = stockData.priceUsd * usdMultiplier
                    resultMap[sym] = MarketQuote(
                        symbol = sym,
                        priceInBaseCurrency = convertedPrice,
                        change24hPct = stockData.change24hPct,
                        lastUpdated = nowMillis
                    )
                }
            }

            resultMap
        }
    }

    fun parseYahooChartJson(jsonString: String, symbol: String): MarketQuote? {
        val root = JSONObject(jsonString)
        val chartObj = root.optJSONObject("chart") ?: return null
        val resultArray = chartObj.optJSONArray("result") ?: return null
        if (resultArray.length() == 0) return null

        val firstResult = resultArray.optJSONObject(0) ?: return null
        val meta = firstResult.optJSONObject("meta") ?: return null

        val currentPrice = meta.optDouble("regularMarketPrice", 0.0)
        val previousClose = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", currentPrice))

        if (currentPrice <= 0.0) return null

        val change24hPct = if (previousClose > 0.0) {
            ((currentPrice - previousClose) / previousClose) * 100.0
        } else {
            0.0
        }

        return MarketQuote(
            symbol = symbol.uppercase(),
            priceInBaseCurrency = currentPrice,
            change24hPct = change24hPct,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun isStockSupported(symbol: String): Boolean {
        val sym = symbol.trim().uppercase()
        val knownStocks = setOf(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA", "META", "BRK.B", "UNH",
            "JNJ", "V", "JPM", "PG", "MA", "HD", "DIS", "NFLX", "AMD", "INTC", "CSCO",
            "VOO", "SPY", "QQQ", "VTI", "IWM", "GLD", "SLV"
        )
        return sym in knownStocks
    }

    private fun executeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw Exception("Yahoo Finance request failed with HTTP code $responseCode")
        }

        return BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }
}
