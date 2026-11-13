package com.app.spent.asset

import com.app.spent.domain.asset.SimulatedMarketPriceEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class SimulatedMarketPriceEngineTest {

    private lateinit var engine: SimulatedMarketPriceEngine

    @Before
    fun setUp() {
        // Deterministic random for reproducible tests
        engine = SimulatedMarketPriceEngine(random = Random(42))
    }

    @Test
    fun testBaselinePricesExistForSupportedAssets() = runTest {
        val quotes = engine.getQuotes(listOf("AAPL", "BTC", "EUR", "XAU"), "USD")

        val aapl = quotes["AAPL"]
        assertNotNull(aapl)
        assertTrue("AAPL price should be > 200", aapl!!.priceInBaseCurrency > 200.0)

        val btc = quotes["BTC"]
        assertNotNull(btc)
        assertTrue("BTC price should be > 50000", btc!!.priceInBaseCurrency > 50000.0)

        val eur = quotes["EUR"]
        assertNotNull(eur)
        assertTrue("EUR price should be around 1.08", eur!!.priceInBaseCurrency in 1.0..1.2)

        val gold = quotes["XAU"]
        assertNotNull(gold)
        assertTrue("Gold price should be > 2000", gold!!.priceInBaseCurrency > 2000.0)
    }

    @Test
    fun testDynamicHandlingOfCustomTicker() = runTest {
        val quotes = engine.getQuotes(listOf("CUSTOM_COIN_XYZ"), "USD")
        val custom = quotes["CUSTOM_COIN_XYZ"]

        assertNotNull(custom)
        assertEquals("CUSTOM_COIN_XYZ", custom!!.symbol)
        assertTrue("Custom asset should have positive initial price", custom.priceInBaseCurrency > 0)
    }

    @Test
    fun testSimulationTickDriftsPricesRealistically() = runTest {
        val initialQuote = engine.getQuote("AAPL", "USD")
        assertNotNull(initialQuote)
        val initialPrice = initialQuote!!.priceInBaseCurrency

        // Advance simulation by 5 ticks
        repeat(5) {
            engine.tickSimulation()
        }

        val updatedQuote = engine.getQuote("AAPL", "USD")
        assertNotNull(updatedQuote)
        val updatedPrice = updatedQuote!!.priceInBaseCurrency

        // Price should fluctuate but not jump unreasonably (within +- 15%)
        assertTrue("Price should stay positive", updatedPrice > 0)
        val ratio = updatedPrice / initialPrice
        assertTrue("Drift should be bounded within 0.85 and 1.15", ratio in 0.85..1.15)
        assertTrue("Timestamp should advance", updatedQuote.lastUpdated >= initialQuote.lastUpdated)
    }

    @Test
    fun testBaseCurrencyConversion() = runTest {
        val quoteInUsd = engine.getQuote("AAPL", "USD")
        val quoteInEur = engine.getQuote("AAPL", "EUR")

        assertNotNull(quoteInUsd)
        assertNotNull(quoteInEur)

        // Since 1 EUR = ~1.085 USD, AAPL in EUR should be less nominal units than in USD (e.g. 228 USD / 1.085 = ~210 EUR)
        assertTrue("AAPL in EUR should be lower nominal value than in USD", quoteInEur!!.priceInBaseCurrency < quoteInUsd!!.priceInBaseCurrency)
    }
}
