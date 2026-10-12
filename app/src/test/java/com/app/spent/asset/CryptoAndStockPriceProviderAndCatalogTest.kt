package com.app.spent.asset

import com.app.spent.data.remote.CryptoPriceProvider
import com.app.spent.data.remote.ExchangeRateApiPriceProvider
import com.app.spent.data.remote.StockPriceProvider
import com.app.spent.domain.asset.CompositeMarketPriceProvider
import com.app.spent.domain.asset.SimulatedMarketPriceEngine
import com.app.spent.domain.asset.catalog.AssetCatalog
import com.app.spent.domain.asset.model.AssetType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CryptoAndStockPriceProviderAndCatalogTest {

    private val sampleCoinCapJson = """
        {
          "data": [
            {
              "id": "bitcoin",
              "rank": "1",
              "symbol": "BTC",
              "name": "Bitcoin",
              "supply": "19000000.00",
              "maxSupply": "21000000.00",
              "marketCapUsd": "1200000000000.00",
              "volumeUsd24Hr": "30000000000.00",
              "priceUsd": "64500.50",
              "changePercent24Hr": "2.75",
              "vwap24Hr": "64000.00"
            },
            {
              "id": "ethereum",
              "rank": "2",
              "symbol": "ETH",
              "name": "Ethereum",
              "priceUsd": "3450.25",
              "changePercent24Hr": "-1.20"
            },
            {
              "id": "solana",
              "rank": "3",
              "symbol": "SOL",
              "name": "Solana",
              "priceUsd": "150.00",
              "changePercent24Hr": "5.50"
            }
          ],
          "timestamp": 1720000000000
        }
    """.trimIndent()

    private val sampleYahooFinanceChartJson = """
        {
          "chart": {
            "result": [
              {
                "meta": {
                  "currency": "USD",
                  "symbol": "AAPL",
                  "exchangeName": "NMS",
                  "instrumentType": "EQUITY",
                  "regularMarketPrice": 224.50,
                  "chartPreviousClose": 220.00
                },
                "indicators": {
                  "quote": [
                    {
                      "close": [221.0, 222.5, 224.50]
                    }
                  ]
                }
              }
            ],
            "error": null
          }
        }
    """.trimIndent()

    private val sampleExchangeRates = """
        {
          "result": "success",
          "base_code": "USD",
          "rates": { "USD": 1.0, "EUR": 0.92, "BOB": 6.91 }
        }
    """.trimIndent()

    @Test
    fun testCryptoPriceProviderJsonParsingInUsd() = runTest {
        val fxProvider = ExchangeRateApiPriceProvider(httpFetcher = { sampleExchangeRates })
        val provider = CryptoPriceProvider(exchangeRateApiProvider = fxProvider, httpFetcher = { sampleCoinCapJson })

        val quotesResult = provider.fetchQuotes(listOf("BTC", "ETH", "SOL"), "USD")
        assertTrue(quotesResult.isSuccess)
        val quotes = quotesResult.getOrThrow()

        assertNotNull(quotes["BTC"])
        assertEquals(64500.50, quotes["BTC"]!!.priceInBaseCurrency, 0.01)
        assertEquals(2.75, quotes["BTC"]!!.change24hPct, 0.01)

        assertNotNull(quotes["ETH"])
        assertEquals(3450.25, quotes["ETH"]!!.priceInBaseCurrency, 0.01)
        assertEquals(-1.20, quotes["ETH"]!!.change24hPct, 0.01)

        assertNotNull(quotes["SOL"])
        assertEquals(150.00, quotes["SOL"]!!.priceInBaseCurrency, 0.01)
    }

    @Test
    fun testCryptoPriceProviderWithNonUsdBaseCurrency() = runTest {
        // 1 USD = 6.91 BOB (1 BOB = 0.1447178 USD)
        val fxProvider = ExchangeRateApiPriceProvider(httpFetcher = { url ->
            if (url.endsWith("/BOB")) {
                """{"result":"success","base_code":"BOB","rates":{"USD": 0.1447178}}"""
            } else {
                sampleExchangeRates
            }
        })
        val provider = CryptoPriceProvider(exchangeRateApiProvider = fxProvider, httpFetcher = { sampleCoinCapJson })

        val quotesResult = provider.fetchQuotes(listOf("BTC"), "BOB")
        assertTrue(quotesResult.isSuccess)
        val quotes = quotesResult.getOrThrow()

        val btcQuote = quotes["BTC"]
        assertNotNull(btcQuote)
        assertEquals(64500.50 * 6.91, btcQuote!!.priceInBaseCurrency, 0.5)
    }

    @Test
    fun testStockPriceProviderJsonParsingInUsd() = runTest {
        val fxProvider = ExchangeRateApiPriceProvider(httpFetcher = { sampleExchangeRates })
        val provider = StockPriceProvider(exchangeRateApiProvider = fxProvider, httpFetcher = { sampleYahooFinanceChartJson })

        val quotesResult = provider.fetchQuotes(listOf("AAPL"), "USD")
        assertTrue(quotesResult.isSuccess)
        val quotes = quotesResult.getOrThrow()

        val quote = quotes["AAPL"]
        assertNotNull(quote)
        assertEquals("AAPL", quote!!.symbol)
        assertEquals(224.50, quote.priceInBaseCurrency, 0.01)
        // Previous close: 220.00 -> change: (224.50 - 220.00) / 220.00 * 100 = 2.045%
        assertEquals(2.045, quote.change24hPct, 0.01)
    }

    @Test
    fun testStockPriceProviderWithNonUsdBaseCurrency() = runTest {
        // 1 USD = 6.91 BOB (1 BOB = 0.1447178 USD)
        val fxProvider = ExchangeRateApiPriceProvider(httpFetcher = { url ->
            if (url.endsWith("/BOB")) {
                """{"result":"success","base_code":"BOB","rates":{"USD": 0.1447178}}"""
            } else {
                sampleExchangeRates
            }
        })
        val provider = StockPriceProvider(exchangeRateApiProvider = fxProvider, httpFetcher = { sampleYahooFinanceChartJson })

        val quotesResult = provider.fetchQuotes(listOf("AAPL"), "BOB")
        assertTrue(quotesResult.isSuccess)
        val quotes = quotesResult.getOrThrow()

        val quote = quotes["AAPL"]
        assertNotNull(quote)
        assertEquals(224.50 * 6.91, quote!!.priceInBaseCurrency, 0.5)
    }

    @Test
    fun testAssetCatalogSearchAndFilter() {
        // Total verified assets
        val allAssets = AssetCatalog.ASSETS
        assertTrue(allAssets.isNotEmpty())

        // Search by name "Bitcoin"
        val btcResults = AssetCatalog.search("Bitcoin")
        assertTrue(btcResults.any { it.symbol == "BTC" && it.type == AssetType.CRYPTO })

        // Search by symbol "AAPL"
        val aaplResults = AssetCatalog.search("AAPL")
        assertTrue(aaplResults.any { it.symbol == "AAPL" && it.type == AssetType.STOCK })

        // Search by fiat "Euro"
        val eurResults = AssetCatalog.search("Euro")
        assertTrue(eurResults.any { it.symbol == "EUR" && it.type == AssetType.FIAT })

        // Filter by AssetType
        val cryptoOnly = AssetCatalog.ASSETS.filter { it.type == AssetType.CRYPTO }
        assertTrue(cryptoOnly.isNotEmpty() && cryptoOnly.all { it.type == AssetType.CRYPTO })

        val fiatOnly = AssetCatalog.ASSETS.filter { it.type == AssetType.FIAT }
        assertTrue(fiatOnly.isNotEmpty() && fiatOnly.all { it.type == AssetType.FIAT })

        val stockOnly = AssetCatalog.ASSETS.filter { it.type == AssetType.STOCK }
        assertTrue(stockOnly.isNotEmpty() && stockOnly.all { it.type == AssetType.STOCK })

        // Exact find
        val foundBtc = AssetCatalog.findBySymbol("BTC")
        assertNotNull(foundBtc)
        assertEquals("Bitcoin", foundBtc!!.name)
    }

    @Test
    fun testCompositeRoutingToAllProviders() = runTest {
        val exchangeRateProvider = ExchangeRateApiPriceProvider(httpFetcher = { sampleExchangeRates })
        val cryptoProvider = CryptoPriceProvider(exchangeRateApiProvider = exchangeRateProvider, httpFetcher = { sampleCoinCapJson })
        val stockProvider = StockPriceProvider(exchangeRateApiProvider = exchangeRateProvider, httpFetcher = { sampleYahooFinanceChartJson })
        val simulatedEngine = SimulatedMarketPriceEngine(random = Random(42))

        val composite = CompositeMarketPriceProvider(
            exchangeRateApiProvider = exchangeRateProvider,
            simulatedEngine = simulatedEngine,
            cryptoPriceProvider = cryptoProvider,
            stockPriceProvider = stockProvider
        )

        val quotes = composite.getQuotes(listOf("EUR", "BTC", "AAPL"), "USD")

        // 1. EUR should be routed to exchangeRateProvider
        assertNotNull(quotes["EUR"])
        assertEquals(1.0 / 0.92, quotes["EUR"]!!.priceInBaseCurrency, 0.01)

        // 2. BTC should be routed to cryptoProvider
        assertNotNull(quotes["BTC"])
        assertEquals(64500.50, quotes["BTC"]!!.priceInBaseCurrency, 0.01)

        // 3. AAPL should be routed to stockProvider
        assertNotNull(quotes["AAPL"])
        assertEquals(224.50, quotes["AAPL"]!!.priceInBaseCurrency, 0.01)
    }
}
