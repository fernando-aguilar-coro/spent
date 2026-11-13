package com.app.spent.ui.portfolio

import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.repository.AssetRepository
import com.app.spent.domain.asset.model.AssetType
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class PortfolioViewModel(
    private val repository: AssetRepository
) : BaseViewModel<PortfolioUiState, PortfolioUiIntent, PortfolioUiEffect>(
    PortfolioUiState(baseCurrency = BASE_CURRENCY)
) {

    init {
        observePortfolio()
    }

    private fun observePortfolio() {
        viewModelScope.launch {
            repository.refreshPrices(BASE_CURRENCY)
        }

        viewModelScope.launch {
            repository.getPortfolioSummaryFlow(BASE_CURRENCY)
            .catch { throwable ->
                setState { copy(errorMessage = throwable.message, isLoading = false) }
            }
            .collect { summary ->
                setState {
                    copy(
                        portfolioSummary = summary,
                        isLoading = false
                    )
                }
            }
        }
    }

    override fun onIntent(intent: PortfolioUiIntent) {
        when (intent) {
            is PortfolioUiIntent.RefreshPrices -> refreshPrices()
            is PortfolioUiIntent.SimulateMarketTick -> simulateMarketTick()
            is PortfolioUiIntent.SetFilter -> setFilter(intent.filter)
            is PortfolioUiIntent.AddAsset -> addAsset(intent)
            is PortfolioUiIntent.UpdateQuantity -> updateQuantity(intent.assetId, intent.newQuantity)
            is PortfolioUiIntent.ToggleManualPrice -> toggleManualPrice(intent.assetId, intent.overridePrice, intent.isManual)
            is PortfolioUiIntent.DeleteAsset -> deleteAsset(intent.assetId)
        }
    }

    private fun refreshPrices() {
        setState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.refreshPrices(currentState.baseCurrency)
                .onSuccess {
                    setState {
                        copy(
                            isLoading = false,
                            lastRefreshedTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                .onFailure { error ->
                    setState { copy(isLoading = false, errorMessage = error.message) }
                    sendEffect(PortfolioUiEffect.ShowToast("Failed to refresh quotes: ${error.message}"))
                }
        }
    }

    private fun simulateMarketTick() {
        setState { copy(isTicking = true) }
        viewModelScope.launch {
            repository.simulateMarketTick(currentState.baseCurrency)
                .onSuccess {
                    setState {
                        copy(
                            isTicking = false,
                            lastRefreshedTimestamp = System.currentTimeMillis()
                        )
                    }
                    sendEffect(PortfolioUiEffect.ShowToast("Market prices updated"))
                }
                .onFailure { error ->
                    setState { copy(isTicking = false) }
                    sendEffect(PortfolioUiEffect.ShowToast("Simulation tick failed: ${error.message}"))
                }
        }
    }

    private fun setFilter(filter: AssetType?) {
        setState { copy(selectedFilter = filter) }
    }

    private fun addAsset(intent: PortfolioUiIntent.AddAsset) {
        viewModelScope.launch {
            try {
                val symbol = intent.symbol.trim().uppercase()
                val quantity = if (intent.totalValueUsd != null) {
                    if (!intent.totalValueUsd.isFinite() || intent.totalValueUsd <= 0.0) {
                        sendEffect(PortfolioUiEffect.ShowToast("Enter a valid positive USD amount for $symbol"))
                        return@launch
                    }
                    val quote = repository.getQuote(symbol, "USD")
                    val unitPriceUsd = quote?.priceInBaseCurrency
                    if (unitPriceUsd == null || !unitPriceUsd.isFinite() || unitPriceUsd <= 0.0) {
                        sendEffect(PortfolioUiEffect.ShowToast("Could not get a valid USD quote for $symbol"))
                        return@launch
                    }
                    intent.totalValueUsd / unitPriceUsd
                } else {
                    intent.quantity
                }
                if (!quantity.isFinite() || quantity <= 0.0) {
                    sendEffect(PortfolioUiEffect.ShowToast("The holding quantity for $symbol is invalid"))
                    return@launch
                }

                val holding = AssetHoldingEntity(
                    symbol = symbol,
                    name = intent.name.trim().ifBlank { symbol },
                    type = intent.type.name,
                    quantity = quantity,
                    averageBuyPrice = intent.averageBuyPrice,
                    currency = currentState.baseCurrency,
                    manualPriceOverride = intent.manualPriceOverride,
                    isManualPrice = intent.isManualPrice,
                    notes = intent.notes
                )
                repository.addAsset(holding)
                sendEffect(PortfolioUiEffect.ShowToast("Added ${holding.symbol} to portfolio"))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                sendEffect(PortfolioUiEffect.ShowToast("Failed to add ${intent.symbol}: ${error.message}"))
            }
        }
    }

    private fun updateQuantity(assetId: String, newQuantity: Double) {
        viewModelScope.launch {
            repository.updateAssetQuantity(assetId, newQuantity)
            sendEffect(PortfolioUiEffect.ShowToast("Quantity updated"))
        }
    }

    private fun toggleManualPrice(assetId: String, overridePrice: Double?, isManual: Boolean) {
        viewModelScope.launch {
            repository.setManualPriceOverride(assetId, overridePrice, isManual)
            sendEffect(PortfolioUiEffect.ShowToast(if (isManual) "Using manual price" else "Switched to market rate"))
        }
    }

    private fun deleteAsset(assetId: String) {
        viewModelScope.launch {
            repository.deleteAsset(assetId)
            sendEffect(PortfolioUiEffect.ShowToast("Asset removed"))
        }
    }

    companion object {
        private const val BASE_CURRENCY = "USD"
    }
}
