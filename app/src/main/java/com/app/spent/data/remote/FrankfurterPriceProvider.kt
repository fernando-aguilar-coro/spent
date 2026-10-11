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
 * Real-world Foreign Exchange rate provider utilizing the free, open Frankfurter API
 * (European Central Bank reference data).
 */
class FrankfurterPriceProvider(
    private val baseUrl: String = "https://api.frankfurter.app",
    private val httpFetcher: (suspend (String) -> String)? = null
) {

    val supportedCurrencies = setOf(
        "AUD", "BGN", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EUR", "GBP",
        "HKD", "HUF", "IDR", "ILS", "INR", "ISK", "JPY", "KRW", "MXN", "MYR",
        "NOK", "NZD", "PHP", "PLN", "RON", "SEK", "SGD", "THB", "TRY", "USD", "ZAR"
    )

    fun isCurrencySupported(symbol: String): Boolean {
        return supportedCurrencies.contains(symbol.trim().uppercase())
    }

    /**
     * Fetches live ECB exchange rates for the given symbols against [baseCurrency].
     */
    suspend fun fetchRates(
        symbols: List<String>,
        baseCurrency: String = "USD"
    ): Result<Map<String, MarketQuote>> = withContext(Dispatchers.IO) {
        runCatching {
            val upperBase = baseCurrency.trim().uppercase()
            val filteredSymbols = symbols.map { it.trim().uppercase() }.filter { isCurrencySupported(it) }

            if (filteredSymbols.isEmpty()) {
                return@runCatching emptyMap()
            }

            val urlString = "$baseUrl/latest?from=$upperBase"
            val jsonString = if (httpFetcher != null) {
                httpFetcher.invoke(urlString)
            } else {
                executeHttpRequest(urlString)
            }

            parseFrankfurterJson(jsonString, upperBase, filteredSymbols)
        }
    }

    fun parseFrankfurterJson(
        jsonString: String,
        baseCurrency: String,
        targetSymbols: List<String>
    ): Map<String, MarketQuote> {
        val root = JSONObject(jsonString)
        val ratesObj = root.optJSONObject("rates") ?: return emptyMap()
        val now = System.currentTimeMillis()
        val quotes = mutableMapOf<String, MarketQuote>()

        // Base currency itself has a rate of 1.0
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

            if (ratesObj.has(sym)) {
                val rate = ratesObj.getDouble(sym)
                if (rate > 0) {
                    // 1 Base = rate Sym -> 1 Sym = (1 / rate) Base
                    val priceInBase = 1.0 / rate
                    quotes[sym] = MarketQuote(
                        symbol = sym,
                        priceInBaseCurrency = priceInBase,
                        change24hPct = 0.0,
                        lastUpdated = now
                    )
                }
            }
        }

        return quotes
    }

    private fun executeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Spent-Android-App")

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw java.io.IOException("Frankfurter API returned HTTP $responseCode")
        }

        return connection.inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    }
}
