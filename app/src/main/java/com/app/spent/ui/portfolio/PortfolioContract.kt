package com.app.spent.ui.portfolio

import com.app.spent.domain.asset.model.AssetType
import com.app.spent.domain.asset.model.PortfolioSummary
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState

data class PortfolioUiState(
    val isLoading: Boolean = false,
    val isTicking: Boolean = false,
    val baseCurrency: String = "USD",
    val selectedFilter: AssetType? = null, // null means "ALL"
    val portfolioSummary: PortfolioSummary = PortfolioSummary(
        totalValuation = 0.0,
        totalCostBasis = 0.0,
        totalUnrealizedPnL = 0.0,
        totalPnLPct = 0.0,
        totalDayChangeAmount = 0.0,
        totalDayChangePct = 0.0,
        assets = emptyList()
    ),
    val lastRefreshedTimestamp: Long = 0L,
    val errorMessage: String? = null
) : UiState

sealed interface PortfolioUiIntent : UiIntent {
    data object RefreshPrices : PortfolioUiIntent
    data object SimulateMarketTick : PortfolioUiIntent
    data class SetFilter(val filter: AssetType?) : PortfolioUiIntent
    data class AddAsset(
        val symbol: String,
        val name: String,
        val type: AssetType,
        val quantity: Double,
        val averageBuyPrice: Double? = null,
        val totalValueUsd: Double? = null,
        val manualPriceOverride: Double? = null,
        val isManualPrice: Boolean = false,
        val notes: String? = null
    ) : PortfolioUiIntent
    data class UpdateQuantity(val assetId: String, val newQuantity: Double) : PortfolioUiIntent
    data class ToggleManualPrice(val assetId: String, val overridePrice: Double?, val isManual: Boolean) : PortfolioUiIntent
    data class DeleteAsset(val assetId: String) : PortfolioUiIntent
}

sealed interface PortfolioUiEffect : UiEffect {
    data class ShowToast(val message: String) : PortfolioUiEffect
}
