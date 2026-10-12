package com.app.spent.ui.portfolio

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.domain.asset.model.AssetType
import com.app.spent.domain.asset.model.ValuedAsset
import com.app.spent.ui.portfolio.components.AddAssetDialog
import com.app.spent.ui.portfolio.components.AssetHoldingCard
import com.app.spent.ui.portfolio.components.EditQuantityDialog
import com.app.spent.ui.portfolio.components.PortfolioHeroCard
import com.app.spent.ui.theme.SageGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<ValuedAsset?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PortfolioUiEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    val filteredAssets = remember(state.portfolioSummary.assets, state.selectedFilter) {
        val filter = state.selectedFilter
        if (filter == null) {
            state.portfolioSummary.assets
        } else {
            state.portfolioSummary.assets.filter {
                it.holding.type.equals(filter.name, ignoreCase = true)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        text = "Variable Assets & Portfolio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(PortfolioUiIntent.RefreshPrices) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Market Rates"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SageGreenPrimary,
                contentColor = MaterialTheme.colorScheme.surface,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Asset", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Portfolio summary hero card
            PortfolioHeroCard(
                summary = state.portfolioSummary,
                baseCurrency = state.baseCurrency,
                isTicking = state.isTicking,
                onSimulateTick = { viewModel.onIntent(PortfolioUiIntent.SimulateMarketTick) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedFilter == null,
                    onClick = { viewModel.onIntent(PortfolioUiIntent.SetFilter(null)) },
                    label = { Text("All (${state.portfolioSummary.assets.size})") }
                )
                AssetType.entries.forEach { type ->
                    val count = state.portfolioSummary.assets.count {
                        it.holding.type.equals(type.name, ignoreCase = true)
                    }
                    FilterChip(
                        selected = state.selectedFilter == type,
                        onClick = { viewModel.onIntent(PortfolioUiIntent.SetFilter(type)) },
                        label = { Text("${type.name} ($count)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Holdings List or Empty Placeholder
            if (filteredAssets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.portfolioSummary.assets.isEmpty())
                                "No variable assets yet"
                            else
                                "No assets in this category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Track currencies (EUR, GBP), stocks (AAPL, VOO), and crypto (BTC, ETH) with dynamic market valuations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredAssets,
                        key = { it.holding.id }
                    ) { asset ->
                        AssetHoldingCard(
                            asset = asset,
                            baseCurrency = state.baseCurrency,
                            onEditQuantity = { editingAsset = asset },
                            onDelete = {
                                viewModel.onIntent(PortfolioUiIntent.DeleteAsset(asset.holding.id))
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Asset Dialog
    if (showAddDialog) {
        AddAssetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { symbol, name, type, quantity, buyPrice ->
                showAddDialog = false
                viewModel.onIntent(
                    PortfolioUiIntent.AddAsset(
                        symbol = symbol,
                        name = name,
                        type = type,
                        quantity = quantity,
                        averageBuyPrice = buyPrice
                    )
                )
            }
        )
    }

    // Edit Quantity Dialog
    editingAsset?.let { asset ->
        EditQuantityDialog(
            symbol = asset.holding.symbol,
            currentQuantity = asset.holding.quantity,
            onDismiss = { editingAsset = null },
            onConfirm = { newQty ->
                editingAsset = null
                viewModel.onIntent(PortfolioUiIntent.UpdateQuantity(asset.holding.id, newQty))
            }
        )
    }
}
