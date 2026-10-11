package com.app.spent.data.repository

import com.app.spent.data.local.dao.AssetDao
import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.local.entity.MarketPriceEntity
import com.app.spent.domain.asset.MarketPriceProvider
import com.app.spent.domain.asset.SimulatedMarketPriceEngine
import com.app.spent.domain.asset.model.PortfolioSummary
import com.app.spent.domain.asset.model.ValuedAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface AssetRepository {
    fun getHoldingsFlow(): Flow<List<AssetHoldingEntity>>
    fun getMarketPricesFlow(): Flow<List<MarketPriceEntity>>
    fun getPortfolioSummaryFlow(baseCurrency: String = "USD"): Flow<PortfolioSummary>
    suspend fun refreshPrices(baseCurrency: String = "USD"): Result<Unit>
    suspend fun simulateMarketTick(baseCurrency: String = "USD"): Result<Unit>
    suspend fun addAsset(holding: AssetHoldingEntity)
    suspend fun updateAssetQuantity(id: String, newQuantity: Double)
    suspend fun setManualPriceOverride(id: String, overridePrice: Double?, isManual: Boolean)
    suspend fun deleteAsset(id: String)
}

class AssetRepositoryImpl(
    private val assetDao: AssetDao,
    private val priceProvider: MarketPriceProvider
) : AssetRepository {

    override fun getHoldingsFlow(): Flow<List<AssetHoldingEntity>> {
        return assetDao.getHoldingsFlow()
    }

    override fun getMarketPricesFlow(): Flow<List<MarketPriceEntity>> {
        return assetDao.getMarketPricesFlow()
    }

    override fun getPortfolioSummaryFlow(baseCurrency: String): Flow<PortfolioSummary> {
        return combine(
            assetDao.getHoldingsFlow(),
            assetDao.getMarketPricesFlow()
        ) { holdings, marketPrices ->
            val priceMap = marketPrices.associateBy { it.symbol.uppercase() }

            val valuedAssets = holdings.map { holding ->
                val cachedQuote = priceMap[holding.symbol.uppercase()]
                val isUsingManual = holding.isManualPrice && holding.manualPriceOverride != null

                val effectivePrice = when {
                    isUsingManual -> holding.manualPriceOverride ?: 0.0
                    cachedQuote != null -> cachedQuote.priceInBaseCurrency
                    holding.averageBuyPrice != null -> holding.averageBuyPrice
                    else -> 0.0
                }

                val totalValuation = holding.quantity * effectivePrice
                val costBasis = holding.averageBuyPrice?.let { it * holding.quantity }
                val unrealizedPnL = costBasis?.let { totalValuation - it }
                val unrealizedPnLPct = if (costBasis != null && costBasis > 0) {
                    ((totalValuation - costBasis) / costBasis) * 100.0
                } else null

                val change24hPct = if (isUsingManual) 0.0 else (cachedQuote?.change24hPct ?: 0.0)

                ValuedAsset(
                    holding = holding,
                    effectiveUnitPrice = effectivePrice,
                    totalValuation = totalValuation,
                    costBasis = costBasis,
                    unrealizedPnL = unrealizedPnL,
                    unrealizedPnLPct = unrealizedPnLPct,
                    change24hPct = change24hPct,
                    isUsingManualPrice = isUsingManual
                )
            }

            val totalValuation = valuedAssets.sumOf { it.totalValuation }
            val totalCostBasis = valuedAssets.mapNotNull { it.costBasis }.sum()
            val totalUnrealizedPnL = if (totalCostBasis > 0) totalValuation - totalCostBasis else 0.0
            val totalPnLPct = if (totalCostBasis > 0) (totalUnrealizedPnL / totalCostBasis) * 100.0 else 0.0

            val totalDayChangeAmount = valuedAssets.sumOf { asset ->
                val denominator = (1.0 + (asset.change24hPct / 100.0)).coerceAtLeast(0.0001)
                val prevPrice = asset.effectiveUnitPrice / denominator
                val prevVal = asset.holding.quantity * prevPrice
                asset.totalValuation - prevVal
            }
            val previousDayTotal = (totalValuation - totalDayChangeAmount).coerceAtLeast(0.0)
            val totalDayChangePct = if (previousDayTotal > 0.0001) {
                ((totalDayChangeAmount / previousDayTotal) * 100.0).coerceIn(-100.0, 10000.0)
            } else 0.0

            PortfolioSummary(
                totalValuation = totalValuation,
                totalCostBasis = totalCostBasis,
                totalUnrealizedPnL = totalUnrealizedPnL,
                totalPnLPct = totalPnLPct,
                totalDayChangeAmount = totalDayChangeAmount,
                totalDayChangePct = totalDayChangePct,
                assets = valuedAssets
            )
        }
    }

    override suspend fun refreshPrices(baseCurrency: String): Result<Unit> = runCatching {
        val symbols = assetDao.getAllSymbols()
        if (symbols.isEmpty()) return@runCatching

        val quotes = priceProvider.getQuotes(symbols, baseCurrency)
        val entities = quotes.values.map { quote ->
            MarketPriceEntity(
                symbol = quote.symbol.uppercase(),
                priceInBaseCurrency = quote.priceInBaseCurrency,
                change24hPct = quote.change24hPct,
                high24h = quote.high24h,
                low24h = quote.low24h,
                lastUpdated = quote.lastUpdated
            )
        }
        assetDao.insertOrUpdatePrices(entities)
    }

    override suspend fun simulateMarketTick(baseCurrency: String): Result<Unit> = runCatching {
        when (priceProvider) {
            is com.app.spent.domain.asset.CompositeMarketPriceProvider -> priceProvider.tickSimulation()
            is SimulatedMarketPriceEngine -> priceProvider.tickSimulation()
        }
        refreshPrices(baseCurrency)
    }

    override suspend fun addAsset(holding: AssetHoldingEntity) {
        assetDao.insertOrUpdateHolding(holding)
        refreshPrices("USD")
    }

    override suspend fun updateAssetQuantity(id: String, newQuantity: Double) {
        val existing = assetDao.getHoldingById(id) ?: return
        assetDao.updateHolding(existing.copy(quantity = newQuantity))
    }

    override suspend fun setManualPriceOverride(id: String, overridePrice: Double?, isManual: Boolean) {
        val existing = assetDao.getHoldingById(id) ?: return
        assetDao.updateHolding(
            existing.copy(
                manualPriceOverride = overridePrice,
                isManualPrice = isManual
            )
        )
    }

    override suspend fun deleteAsset(id: String) {
        assetDao.deleteHoldingById(id)
    }
}
