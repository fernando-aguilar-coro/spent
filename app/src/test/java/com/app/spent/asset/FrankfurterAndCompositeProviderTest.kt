package com.app.spent.asset

import com.app.spent.data.remote.FrankfurterPriceProvider
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

class FrankfurterAndCompositeProviderTest {

    private val sampleFrankfurterJson = """
        {
          "amount": 1.0,
          "base": "USD",
          "date": "2026-10-11",
          "rates": {
            "EUR": 0.9202,
            "GBP": 0.7663,
            "JPY": 151.52,
            "MXN": 19.34
          }
        }
    """.trimIndent()

    @Test
    fun testFrankfurterJsonParsingAndConversion() {
        val provider = FrankfurterPriceProvider()
        val quotes = provider.parseFrankfurterJson(
            jsonString = sampleFrankfurterJson,
            baseCurrency = "USD",
            targetSymbols = listOf("EUR", "GBP", "MXN", "USD")
        )

        // 1. USD (base) should be exactly 1.0
        assertEquals(1.0, quotes["USD"]?.priceInBaseCurrency ?: 0.0, 0.0001)

        // 2. EUR rate is 0.9202 (1 USD = 0.9202 EUR) -> 1 EUR = 1 / 0.9202 = ~1.0867 USD
        val eurQuote = quotes["EUR"]
        assertNotNull(eurQuote)
        assertEquals(1.0 / 0.9202, eurQuote!!.priceInBaseCurrency, 0.001)

        // 3. MXN rate is 19.34 (1 USD = 19.34 MXN) -> 1 MXN = 1 / 19.34 = ~0.0517 USD
        val mxnQuote = quotes["MXN"]
        assertNotNull(mxnQuote)
        assertEquals(1.0 / 19.34, mxnQuote!!.priceInBaseCurrency, 0.001)
    }

    @Test
    fun testCurrencySupportDetection() {
        val provider = FrankfurterPriceProvider()
        assertTrue(provider.isCurrencySupported("EUR"))
        assertTrue(provider.isCurrencySupported("GBP"))
        assertTrue(provider.isCurrencySupported("JPY"))
        assertTrue(provider.isCurrencySupported("MXN"))
        assertFalse(provider.isCurrencySupported("AAPL"))
        assertFalse(provider.isCurrencySupported("BTC"))
    }

    @Test
    fun testCompositeProviderRoutesCurrenciesToFrankfurterAndStocksToSimulation() = runTest {
        // Mock HTTP fetcher that returns sampleFrankfurterJson
        val frankfurterProvider = FrankfurterPriceProvider(httpFetcher = { sampleFrankfurterJson })
        val simulatedEngine = SimulatedMarketPriceEngine(random = Random(42))
        val composite = CompositeMarketPriceProvider(frankfurterProvider, simulatedEngine)

        val quotes = composite.getQuotes(listOf("EUR", "AAPL", "BTC"), "USD")

        // EUR should come from Frankfurter mock
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
    fun testCompositeProviderFallsBackToSimulationWhenFrankfurterFails() = runTest {
        // Simulated network failure
        val failingFrankfurter = FrankfurterPriceProvider(httpFetcher = { throw java.io.IOException("Network unavailable") })
        val simulatedEngine = SimulatedMarketPriceEngine(random = Random(42))
        val composite = CompositeMarketPriceProvider(failingFrankfurter, simulatedEngine)

        val quotes = composite.getQuotes(listOf("EUR", "GBP"), "USD")

        // Should not throw, should fall back to simulated quotes
        assertNotNull(quotes["EUR"])
        assertNotNull(quotes["GBP"])
        assertTrue("Fallback EUR price should be positive", quotes["EUR"]!!.priceInBaseCurrency > 0)
    }
}
