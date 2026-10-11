package com.app.spent.asset

import com.app.spent.data.local.dao.AssetDao
import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.local.entity.MarketPriceEntity
import com.app.spent.data.repository.AssetRepository
import com.app.spent.data.repository.AssetRepositoryImpl
import com.app.spent.domain.asset.MarketPriceProvider
import com.app.spent.domain.asset.model.MarketQuote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssetRepositoryTest {

    private lateinit var fakeDao: FakeAssetDao
    private lateinit var fakeProvider: FakeMarketPriceProvider
    private lateinit var repository: AssetRepository

    @Before
    fun setUp() {
        fakeDao = FakeAssetDao()
        fakeProvider = FakeMarketPriceProvider()
        repository = AssetRepositoryImpl(fakeDao, fakeProvider)
    }

    @Test
    fun testPortfolioValuationAndPnLCalculations() = runTest {
        // Holding 1: 10 shares of AAPL, cost basis $200 each, market price $220 -> +$200 P&L (+10%)
        val aaplHolding = AssetHoldingEntity(
            id = "h_aapl",
            symbol = "AAPL",
            name = "Apple Inc.",
            type = "STOCK",
            quantity = 10.0,
            averageBuyPrice = 200.0
        )

        // Holding 2: 2 BTC, cost basis $60,000 each, market price $65,000 -> +$10,000 P&L (+8.33%)
        val btcHolding = AssetHoldingEntity(
            id = "h_btc",
            symbol = "BTC",
            name = "Bitcoin",
            type = "CRYPTO",
            quantity = 2.0,
            averageBuyPrice = 60000.0
        )

        fakeDao.insertOrUpdateHolding(aaplHolding)
        fakeDao.insertOrUpdateHolding(btcHolding)

        fakeDao.insertOrUpdatePrices(
            listOf(
                MarketPriceEntity(symbol = "AAPL", priceInBaseCurrency = 220.0, change24hPct = 2.0),
                MarketPriceEntity(symbol = "BTC", priceInBaseCurrency = 65000.0, change24hPct = 5.0)
            )
        )

        val summary = repository.getPortfolioSummaryFlow("USD").first()

        // Total Valuation = (10 * 220) + (2 * 65000) = 2200 + 130000 = 132,200.0
        assertEquals(132200.0, summary.totalValuation, 0.001)

        // Total Cost Basis = (10 * 200) + (2 * 60000) = 2000 + 120000 = 122,000.0
        assertEquals(122000.0, summary.totalCostBasis, 0.001)

        // Unrealized P&L = 132200 - 122000 = 10,200.0
        assertEquals(10200.0, summary.totalUnrealizedPnL, 0.001)

        // P&L Percentage = (10200 / 122000) * 100 = ~8.36%
        assertTrue("Total P&L % should be ~8.36%", summary.totalPnLPct in 8.35..8.37)
    }

    @Test
    fun testManualPriceOverrideTakesPrecedence() = runTest {
        val holding = AssetHoldingEntity(
            id = "h_manual",
            symbol = "EUR",
            name = "Euro",
            type = "FIAT",
            quantity = 1000.0,
            isManualPrice = true,
            manualPriceOverride = 1.25 // Market says 1.08, but manual says 1.25
        )

        fakeDao.insertOrUpdateHolding(holding)
        fakeDao.insertOrUpdatePrice(MarketPriceEntity(symbol = "EUR", priceInBaseCurrency = 1.08))

        val summary = repository.getPortfolioSummaryFlow("USD").first()
        val asset = summary.assets.first()

        assertTrue(asset.isUsingManualPrice)
        assertEquals(1.25, asset.effectiveUnitPrice, 0.001)
        assertEquals(1250.0, asset.totalValuation, 0.001)
    }

    @Test
    fun testUpdateQuantityAndDeletion() = runTest {
        val holding = AssetHoldingEntity(
            id = "h_test",
            symbol = "GOOGL",
            name = "Alphabet",
            type = "STOCK",
            quantity = 5.0
        )
        fakeDao.insertOrUpdateHolding(holding)
        fakeDao.insertOrUpdatePrice(MarketPriceEntity(symbol = "GOOGL", priceInBaseCurrency = 180.0))

        repository.updateAssetQuantity("h_test", 10.0)
        var summary = repository.getPortfolioSummaryFlow("USD").first()
        assertEquals(1800.0, summary.totalValuation, 0.001)

        repository.deleteAsset("h_test")
        summary = repository.getPortfolioSummaryFlow("USD").first()
        assertTrue(summary.assets.isEmpty())
        assertEquals(0.0, summary.totalValuation, 0.001)
    }

    // Fake in-memory DAO for testing
    private class FakeAssetDao : AssetDao {
        private val holdingsMap = mutableMapOf<String, AssetHoldingEntity>()
        private val pricesMap = mutableMapOf<String, MarketPriceEntity>()

        private val holdingsFlow = MutableStateFlow<List<AssetHoldingEntity>>(emptyList())
        private val pricesFlow = MutableStateFlow<List<MarketPriceEntity>>(emptyList())

        private fun updateFlows() {
            holdingsFlow.value = holdingsMap.values.toList()
            pricesFlow.value = pricesMap.values.toList()
        }

        override fun getHoldingsFlow(): Flow<List<AssetHoldingEntity>> = holdingsFlow
        override fun getMarketPricesFlow(): Flow<List<MarketPriceEntity>> = pricesFlow

        override suspend fun getHoldingById(id: String): AssetHoldingEntity? = holdingsMap[id]
        override suspend fun getAllSymbols(): List<String> = holdingsMap.values.map { it.symbol }.distinct()

        override suspend fun insertOrUpdateHolding(holding: AssetHoldingEntity) {
            holdingsMap[holding.id] = holding
            updateFlows()
        }

        override suspend fun updateHolding(holding: AssetHoldingEntity) {
            holdingsMap[holding.id] = holding
            updateFlows()
        }

        override suspend fun deleteHoldingById(id: String) {
            holdingsMap.remove(id)
            updateFlows()
        }

        override suspend fun getMarketPrice(symbol: String): MarketPriceEntity? = pricesMap[symbol.uppercase()]

        override suspend fun insertOrUpdatePrices(prices: List<MarketPriceEntity>) {
            prices.forEach { pricesMap[it.symbol.uppercase()] = it }
            updateFlows()
        }

        override suspend fun insertOrUpdatePrice(price: MarketPriceEntity) {
            pricesMap[price.symbol.uppercase()] = price
            updateFlows()
        }

        override suspend fun deletePrice(symbol: String) {
            pricesMap.remove(symbol.uppercase())
            updateFlows()
        }
    }

    private class FakeMarketPriceProvider : MarketPriceProvider {
        val quotes = mutableMapOf<String, MarketQuote>()

        override suspend fun getQuote(symbol: String, baseCurrency: String): MarketQuote? {
            return quotes[symbol.uppercase()]
        }

        override suspend fun getQuotes(symbols: List<String>, baseCurrency: String): Map<String, MarketQuote> {
            return symbols.associateWith { quotes[it.uppercase()] ?: MarketQuote(it, 100.0) }
        }
    }
}
