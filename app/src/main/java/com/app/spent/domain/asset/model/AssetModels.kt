package com.app.spent.domain.asset.model

import com.app.spent.data.local.entity.AssetHoldingEntity

enum class AssetType {
    STOCK,
    CRYPTO,
    FIAT,
    COMMODITY,
    CUSTOM;

    companion object {
        fun fromString(value: String): AssetType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CUSTOM
        }
    }
}

data class MarketQuote(
    val symbol: String,
    val priceInBaseCurrency: Double,
    val change24hPct: Double = 0.0,
    val high24h: Double? = null,
    val low24h: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ValuedAsset(
    val holding: AssetHoldingEntity,
    val effectiveUnitPrice: Double,
    val totalValuation: Double,
    val costBasis: Double? = null,
    val unrealizedPnL: Double? = null,
    val unrealizedPnLPct: Double? = null,
    val change24hPct: Double = 0.0,
    val isUsingManualPrice: Boolean = false
)

data class PortfolioSummary(
    val totalValuation: Double,
    val totalCostBasis: Double,
    val totalUnrealizedPnL: Double,
    val totalPnLPct: Double,
    val totalDayChangeAmount: Double,
    val totalDayChangePct: Double,
    val assets: List<ValuedAsset>
)
