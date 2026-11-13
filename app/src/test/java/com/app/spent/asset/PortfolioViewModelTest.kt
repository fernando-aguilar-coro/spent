package com.app.spent.asset

import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.local.entity.MarketPriceEntity
import com.app.spent.data.repository.AssetRepository
import com.app.spent.domain.asset.model.AssetType
import com.app.spent.domain.asset.model.MarketQuote
import com.app.spent.domain.asset.model.PortfolioSummary
import com.app.spent.domain.asset.model.ValuedAsset
import com.app.spent.ui.portfolio.PortfolioUiEffect
import com.app.spent.ui.portfolio.PortfolioUiIntent
import com.app.spent.ui.portfolio.PortfolioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAssetRepository
    private lateinit var viewModel: PortfolioViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAssetRepository()
        viewModel = PortfolioViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateObservesPortfolioSummary() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state.portfolioSummary)
        assertEquals("USD", state.baseCurrency)
    }

    @Test
    fun testAddAssetIntentInvokesRepository() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(
            PortfolioUiIntent.AddAsset(
                symbol = "AAPL",
                name = "Apple Inc.",
                type = AssetType.STOCK,
                quantity = 5.0,
                averageBuyPrice = 220.0
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.portfolioSummary.assets.size)
        assertEquals("AAPL", state.portfolioSummary.assets.first().holding.symbol)
    }

    @Test
    fun testAddStockFromUsdValueCalculatesUnitsFromCurrentQuote() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(
            PortfolioUiIntent.AddAsset(
                symbol = "AAPL",
                name = "Apple Inc.",
                type = AssetType.STOCK,
                quantity = 0.0,
                totalValueUsd = 1000.0
            )
        )
        advanceUntilIdle()

        val holding = viewModel.uiState.value.portfolioSummary.assets.first().holding
        assertEquals(5.0, holding.quantity, 0.001)
        assertTrue(holding.averageBuyPrice == null)
    }

    @Test
    fun testSetFilterIntentUpdatesState() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(PortfolioUiIntent.SetFilter(AssetType.CRYPTO))
        advanceUntilIdle()

        assertEquals(AssetType.CRYPTO, viewModel.uiState.value.selectedFilter)

        viewModel.onIntent(PortfolioUiIntent.SetFilter(null))
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun testSimulateMarketTickIntentTriggersUpdate() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(PortfolioUiIntent.SimulateMarketTick)
        advanceUntilIdle()

        assertTrue(fakeRepository.tickSimulatedCalled)
    }

    @Test
    fun testDeleteAssetIntentRemovesAsset() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(
            PortfolioUiIntent.AddAsset(
                symbol = "BTC",
                name = "Bitcoin",
                type = AssetType.CRYPTO,
                quantity = 1.0
            )
        )
        advanceUntilIdle()

        val assetId = viewModel.uiState.value.portfolioSummary.assets.first().holding.id
        viewModel.onIntent(PortfolioUiIntent.DeleteAsset(assetId))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.portfolioSummary.assets.isEmpty())
    }

    // Fake AssetRepository
    private class FakeAssetRepository : AssetRepository {
        var tickSimulatedCalled = false
        private val holdings = mutableListOf<AssetHoldingEntity>()
        private val summaryFlow = MutableStateFlow(
            PortfolioSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList())
        )

        private fun refresh() {
            val valued = holdings.map { h ->
                ValuedAsset(
                    holding = h,
                    effectiveUnitPrice = 100.0,
                    totalValuation = h.quantity * 100.0,
                    costBasis = h.averageBuyPrice?.let { it * h.quantity },
                    unrealizedPnL = 0.0,
                    unrealizedPnLPct = 0.0,
                    change24hPct = 1.5,
                    isUsingManualPrice = h.isManualPrice
                )
            }
            val total = valued.sumOf { it.totalValuation }
            summaryFlow.value = PortfolioSummary(
                totalValuation = total,
                totalCostBasis = total,
                totalUnrealizedPnL = 0.0,
                totalPnLPct = 0.0,
                totalDayChangeAmount = 0.0,
                totalDayChangePct = 0.0,
                assets = valued
            )
        }

        override fun getHoldingsFlow(): Flow<List<AssetHoldingEntity>> = MutableStateFlow(holdings)
        override fun getMarketPricesFlow(): Flow<List<MarketPriceEntity>> = MutableStateFlow(emptyList())
        override fun getPortfolioSummaryFlow(baseCurrency: String): Flow<PortfolioSummary> = summaryFlow

        override suspend fun getQuote(symbol: String, baseCurrency: String): MarketQuote? =
            MarketQuote(symbol = symbol, priceInBaseCurrency = 200.0)

        override suspend fun refreshPrices(baseCurrency: String): Result<Unit> = Result.success(Unit)

        override suspend fun simulateMarketTick(baseCurrency: String): Result<Unit> {
            tickSimulatedCalled = true
            return Result.success(Unit)
        }

        override suspend fun addAsset(holding: AssetHoldingEntity) {
            holdings.add(holding)
            refresh()
        }

        override suspend fun updateAssetQuantity(id: String, newQuantity: Double) {
            val idx = holdings.indexOfFirst { it.id == id }
            if (idx >= 0) {
                holdings[idx] = holdings[idx].copy(quantity = newQuantity)
                refresh()
            }
        }

        override suspend fun setManualPriceOverride(id: String, overridePrice: Double?, isManual: Boolean) {
            val idx = holdings.indexOfFirst { it.id == id }
            if (idx >= 0) {
                holdings[idx] = holdings[idx].copy(manualPriceOverride = overridePrice, isManualPrice = isManual)
                refresh()
            }
        }

        override suspend fun deleteAsset(id: String) {
            holdings.removeAll { it.id == id }
            refresh()
        }
    }
}
