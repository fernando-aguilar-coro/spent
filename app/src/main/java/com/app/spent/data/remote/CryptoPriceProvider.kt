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
 * Real-world cryptocurrency market price provider connecting to public CoinCap API
 * (https://api.coincap.io/v2/assets?limit=100) with zero API key requirement.
 * Delivers live USD prices and 24h percentage changes for top 100 cryptocurrencies
 * (BTC, ETH, SOL, BNB, XRP, ADA, DOGE, DOT, AVAX, LINK, MATIC, etc.),
 * converting into the user's base currency via [ExchangeRateApiPriceProvider].
 */
class CryptoPriceProvider(
    private val exchangeRateApiProvider: ExchangeRateApiPriceProvider,
    private val apiUrl: String = "https://api.coincap.io/v2/assets?limit=100",
    private val httpFetcher: (suspend (String) -> String)? = null
) {

    private data class CachedCrypto(
        val quotesInUsd: Map<String, CryptoQuoteUsd>,
        val expiresAtEpochSeconds: Long
    )

    data class CryptoQuoteUsd(
        val symbol: String,
        val name: String,
        val priceUsd: Double,
        val change24hPct: Double
    )

    private var cache: CachedCrypto? = null

    suspend fun fetchQuotes(
        symbols: List<String>,
        baseCurrency: String = "USD"
    ): Result<Map<String, MarketQuote>> = withContext(Dispatchers.IO) {
        runCatching {
            val upperSymbols = symbols.map { it.trim().uppercase() }.distinct()
            if (upperSymbols.isEmpty()) return@runCatching emptyMap()

            val nowSeconds = System.currentTimeMillis() / 1000
            val cachedData = cache
            val usdMap = if (cachedData != null && cachedData.expiresAtEpochSeconds > nowSeconds) {
                cachedData.quotesInUsd
            } else {
                val jsonString = if (httpFetcher != null) {
                    httpFetcher.invoke(apiUrl)
                } else {
                    executeHttpRequest(apiUrl)
                }
                val parsed = parseCoinCapJson(jsonString)
                if (parsed.isNotEmpty()) {
                    cache = CachedCrypto(
                        quotesInUsd = parsed,
                        expiresAtEpochSeconds = nowSeconds + 300 // 5-minute cache
                    )
                }
                parsed
            }

            val usdMultiplier = exchangeRateApiProvider.getUsdToBaseMultiplier(baseCurrency)
            val nowMillis = System.currentTimeMillis()
            val resultMap = mutableMapOf<String, MarketQuote>()

            for (sym in upperSymbols) {
                val quoteUsd = usdMap[sym]
                if (quoteUsd != null && quoteUsd.priceUsd > 0.0) {
                    val convertedPrice = quoteUsd.priceUsd * usdMultiplier
                    resultMap[sym] = MarketQuote(
                        symbol = sym,
                        priceInBaseCurrency = convertedPrice,
                        change24hPct = quoteUsd.change24hPct,
                        lastUpdated = nowMillis
                    )
                }
            }

            resultMap
        }
    }

    fun parseCoinCapJson(jsonString: String): Map<String, CryptoQuoteUsd> {
        val root = JSONObject(jsonString)
        val dataArray = root.optJSONArray("data") ?: return emptyMap()
        val result = mutableMapOf<String, CryptoQuoteUsd>()

        for (i in 0 until dataArray.length()) {
            val item = dataArray.optJSONObject(i) ?: continue
            val sym = item.optString("symbol", "").trim().uppercase()
            val name = item.optString("name", "")
            val priceUsd = item.optDouble("priceUsd", 0.0)
            val change24h = item.optDouble("changePercent24Hr", 0.0)

            if (sym.isNotBlank() && priceUsd > 0.0) {
                result[sym] = CryptoQuoteUsd(
                    symbol = sym,
                    name = name,
                    priceUsd = priceUsd,
                    change24hPct = change24h
                )
            }
        }

        return result
    }

    fun isCryptoSupported(symbol: String): Boolean {
        val sym = symbol.trim().uppercase()
        val knownCryptos = setOf(
            "BTC", "ETH", "SOL", "BNB", "XRP", "ADA", "DOGE", "DOT", "AVAX", "LINK",
            "MATIC", "SHIB", "LTC", "TRX", "NEAR", "UNI", "ICP", "DAI", "BCH", "ETC",
            "FIL", "RENDER", "ATOM", "XLM", "SUI", "APT", "PEPE", "HBAR", "ARB", "OP"
        )
        return sym in knownCryptos
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
            throw Exception("CoinCap request failed with HTTP code $responseCode")
        }

        return BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }
}
