package com.app.spent.asset

import com.app.spent.data.remote.ExchangeRateApiPriceProvider
import com.app.spent.domain.asset.CompositeMarketPriceProvider
import com.app.spent.domain.asset.SimulatedMarketPriceEngine
import com.app.spent.domain.asset.model.MarketQuote
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExchangeRateApiAndCompositeProviderTest {

    private val sampleExchangeRateApiJson = """
        {
          "result": "success",
          "base_code": "USD",
          "time_last_update_unix": 1770000000,
          "rates": {
            "USD": 1.0,
            "EUR": 0.9202,
            "GBP": 0.7663,
            "JPY": 151.52,
            "MXN": 19.34,
            "BOB": 6.91,
            "ARS": 980.50,
            "BRL": 5.45
          }
        }
    """.trimIndent()

    private val sampleBobBaseJson = """
        {
          "result": "success",
          "base_code": "BOB",
          "time_last_update_unix": 1770000000,
          "rates": {
            "BOB": 1.0,
            "USD": 0.1447,
            "EUR": 0.1331,
            "ARS": 141.90
          }
        }
    """.trimIndent()

    @Test
    fun testExchangeRateApiJsonParsingInUsdBase() {
        val provider = ExchangeRateApiPriceProvider()
        val quotes = provider.parseExchangeRateJson(
            jsonString = sampleExchangeRateApiJson,
            baseCurrency = "USD",
            targetSymbols = listOf("EUR", "BOB", "ARS", "USD")
        )

        // 1. USD (base) should be exactly 1.0
        assertEquals(1.0, quotes["USD"]?.priceInBaseCurrency ?: 0.0, 0.0001)

        // 2. EUR rate is 0.9202 (1 USD = 0.9202 EUR) -> 1 EUR = 1 / 0.9202 = ~1.0867 USD
        val eurQuote = quotes["EUR"]
        assertNotNull(eurQuote)
        assertEquals(1.0 / 0.9202, eurQuote!!.priceInBaseCurrency, 0.001)

        // 3. BOB rate is 6.91 (1 USD = 6.91 BOB) -> 1 BOB = 1 / 6.91 = ~0.1447 USD
        val bobQuote = quotes["BOB"]
        assertNotNull(bobQuote)
        assertEquals(1.0 / 6.91, bobQuote!!.priceInBaseCurrency, 0.001)

        // 4. ARS rate is 980.50 (1 USD = 980.50 ARS) -> 1 ARS = 1 / 980.50 USD
        val arsQuote = quotes["ARS"]
        assertNotNull(arsQuote)
        assertEquals(1.0 / 980.50, arsQuote!!.priceInBaseCurrency, 0.00001)
    }

    @Test
    fun testExchangeRateApiJsonParsingInBobBase() {
        val provider = ExchangeRateApiPriceProvider()
        val quotes = provider.parseExchangeRateJson(
            jsonString = sampleBobBaseJson,
            baseCurrency = "BOB",
            targetSymbols = listOf("BOB", "USD", "EUR")
        )

        // 1. BOB (base) is 1.0
        assertEquals(1.0, quotes["BOB"]?.priceInBaseCurrency ?: 0.0, 0.0001)

        // 2. 1 BOB = 0.1447 USD -> 1 USD = 1 / 0.1447 BOB = ~6.91 BOB
        val usdQuote = quotes["USD"]
        assertNotNull(usdQuote)
        assertEquals(1.0 / 0.1447, usdQuote!!.priceInBaseCurrency, 0.01)
    }

    @Test
    fun testCurrencySupportDetection() {
        val provider = ExchangeRateApiPriceProvider()
        assertTrue(provider.isCurrencySupported("BOB"))
        assertTrue(provider.isCurrencySupported("ARS"))
        assertTrue(provider.isCurrencySupported("BRL"))
        assertTrue(provider.isCurrencySupported("EUR"))
        assertTrue(provider.isCurrencySupported("USD"))
        assertTrue(provider.isCurrencySupported("JPY"))
        assertTrue(provider.isCurrencySupported("GBP"))
        assertFalse(provider.isCurrencySupported("AAPL"))
        assertFalse(provider.isCurrencySupported("BTC"))
        assertFalse(provider.isCurrencySupported("ETH"))
    }

    @Test
    fun testCompositeProviderRoutesCurrenciesToExchangeRateApiAndStocksToSimulation() = runTest {
        val exchangeRateProvider = ExchangeRateApiPriceProvider(httpFetcher = { sampleExchangeRateApiJson })
        val simulatedEngine = SimulatedMarketPriceEngine(random = Random(42))
        val composite = CompositeMarketPriceProvider(exchangeRateProvider, simulatedEngine)

        val quotes = composite.getQuotes(listOf("BOB", "EUR", "AAPL", "BTC"), "USD")

        // BOB and EUR should come from ExchangeRateApi
        val bob = quotes["BOB"]
        assertNotNull(bob)
        assertEquals(1.0 / 6.91, bob!!.priceInBaseCurrency, 0.001)

        val eur = quotes["EUR"]
        assertNotNull(eur)
        assertEquals(1.0 / 0.9202, eur!!.priceInBaseCurrency, 0.001)

        // AAPL and BTC should come from SimulatedMarketPriceEngine
        val aapl = quotes["AAPL"]
        assertNotNull(aapl)
        assertTrue("AAPL should have simulated price > 200", aapl!!.priceInBaseCurrency > 200.0)

        val btc = quotes["BTC"]
        assertNotNull(btc)
        assertTrue("BTC should have simulated price > 50000", btc!!.priceInBaseCurrency > 50000.0)
    }

    @Test
    fun testCompositeProviderFallsBackToSimulationWhenExchangeRateApiFails() = runTest {
        val failingProvider = ExchangeRateApiPriceProvider(httpFetcher = { throw java.io.IOException("Network down") })
        val simulatedEngine = SimulatedMarketPriceEngine(random = Random(42))
        val composite = CompositeMarketPriceProvider(failingProvider, simulatedEngine)

        val quotes = composite.getQuotes(listOf("BOB", "ARS", "EUR"), "USD")

        // Should not throw, should fall back to simulated quotes
        assertNotNull(quotes["BOB"])
        assertNotNull(quotes["ARS"])
        assertNotNull(quotes["EUR"])
        assertTrue("Fallback BOB price should be positive", quotes["BOB"]!!.priceInBaseCurrency > 0)
    }
}
