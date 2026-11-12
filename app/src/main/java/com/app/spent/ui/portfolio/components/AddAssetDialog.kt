package com.app.spent.ui.portfolio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.domain.asset.catalog.AssetCatalog
import com.app.spent.domain.asset.catalog.CatalogAsset
import com.app.spent.domain.asset.model.AssetType
import com.app.spent.ui.theme.SageGreenPrimary

enum class AddAssetInputMode { UNITS, USD_VALUE }

@Composable
fun AddAssetDialog(
    baseCurrency: String = "USD",
    onDismiss: () -> Unit,
    onConfirm: (
        symbol: String,
        name: String,
        type: AssetType,
        quantity: Double,
        averageBuyPrice: Double?,
        totalValueUsd: Double?
    ) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<AssetType?>(null) }
    var selectedAsset by remember { mutableStateOf<CatalogAsset?>(null) }
    var inputMode by remember { mutableStateOf(AddAssetInputMode.UNITS) }
    var quantityText by remember { mutableStateOf("") }
    var totalUsdText by remember { mutableStateOf("") }
    var buyPriceText by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, selectedCategoryFilter) {
        val baseResults = AssetCatalog.search(searchQuery)
        val filter = selectedCategoryFilter
        if (filter != null) {
            baseResults.filter { it.type == filter }
        } else {
            baseResults
        }
    }

    val computedQuantity: Double = remember(
        selectedAsset?.type,
        inputMode,
        quantityText,
        totalUsdText,
        buyPriceText
    ) {
        if (inputMode == AddAssetInputMode.UNITS) {
            quantityText.toDoubleOrNull() ?: 0.0
        } else {
            val totalUsd = totalUsdText.toDoubleOrNull() ?: 0.0
            val price = buyPriceText.toDoubleOrNull() ?: 0.0
            if (price > 0) {
                totalUsd / price
            } else {
                totalUsd
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Asset / Holding",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (selectedAsset == null) {
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("Search currency, stock, crypto…", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick category chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryFilter == AssetType.FIAT,
                            onClick = { selectedCategoryFilter = AssetType.FIAT },
                            label = { Text("Currencies", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryFilter == AssetType.CRYPTO,
                            onClick = { selectedCategoryFilter = AssetType.CRYPTO },
                            label = { Text("Crypto", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryFilter == AssetType.STOCK,
                            onClick = { selectedCategoryFilter = AssetType.STOCK },
                            label = { Text("Stocks", fontSize = 11.sp) }
                        )
                    }

                    // Catalog items list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredList, key = { it.symbol }) { item ->
                            val itemColor = when (item.type) {
                                AssetType.CRYPTO -> Color(0xFFF59E0B)
                                AssetType.STOCK -> Color(0xFF3B82F6)
                                else -> SageGreenPrimary
                            }
                            val itemIcon = when (item.type) {
                                AssetType.CRYPTO -> Icons.Default.CurrencyBitcoin
                                AssetType.STOCK -> Icons.AutoMirrored.Filled.ShowChart
                                else -> Icons.Default.CurrencyExchange
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedAsset = item }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(itemColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = itemIcon,
                                        contentDescription = item.type.name,
                                        tint = itemColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.name} (${item.symbol})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!item.description.isNullOrBlank()) {
                                        Text(
                                            text = item.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = itemColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = item.type.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = itemColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Selected Asset Banner
                    val asset = selectedAsset!!
                    val itemColor = when (asset.type) {
                        AssetType.CRYPTO -> Color(0xFFF59E0B)
                        AssetType.STOCK -> Color(0xFF3B82F6)
                        else -> SageGreenPrimary
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(itemColor.copy(alpha = 0.10f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(itemColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = itemColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${asset.name} (${asset.symbol})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-priced from live market feed",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { selectedAsset = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Change Asset",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (asset.type == AssetType.STOCK) {
                        OutlinedTextField(
                            value = totalUsdText,
                            onValueChange = { totalUsdText = it },
                            label = { Text("Valor total que tienes en USD ($)") },
                            placeholder = { Text("e.g. 1000.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Las unidades se calcularán con la cotización actual.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = inputMode == AddAssetInputMode.UNITS,
                                onClick = { inputMode = AddAssetInputMode.UNITS },
                                label = { Text("Por Unidades", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = inputMode == AddAssetInputMode.USD_VALUE,
                                onClick = { inputMode = AddAssetInputMode.USD_VALUE },
                                label = { Text("Por Valor Total ($baseCurrency)", fontSize = 11.sp) }
                            )
                        }

                        if (inputMode == AddAssetInputMode.UNITS) {
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                label = { Text(if (asset.type == AssetType.FIAT) "Cantidad en ${asset.symbol}" else "Unidades") },
                                placeholder = { Text("e.g. 10.5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = totalUsdText,
                                onValueChange = { totalUsdText = it },
                                label = { Text("Valor Total Equivalente ($baseCurrency)") },
                                placeholder = { Text("e.g. 1000.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = buyPriceText,
                            onValueChange = { buyPriceText = it },
                            label = { Text("Precio de Compra por Unidad ($baseCurrency)") },
                            placeholder = { Text("e.g. 180.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (asset.type == AssetType.STOCK) {
                            "• El valor de mercado se mostrará en tu moneda activa."
                        } else {
                            "• Live market valuations are automatically calculated in your active currency."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val asset = selectedAsset
                    val isStock = asset?.type == AssetType.STOCK
                    val totalValueUsd = if (isStock) {
                        totalUsdText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
                    } else {
                        null
                    }
                    val quantity = if (isStock) 0.0 else computedQuantity
                    val buyPrice = if (isStock) null else buyPriceText.toDoubleOrNull()
                    if (asset != null && (totalValueUsd ?: quantity) > 0) {
                        onConfirm(
                            asset.symbol,
                            asset.name,
                            asset.type,
                            quantity,
                            buyPrice,
                            totalValueUsd
                        )
                    }
                },
                enabled = selectedAsset != null && (
                    if (selectedAsset?.type == AssetType.STOCK) {
                        totalUsdText.toDoubleOrNull()?.let { it.isFinite() && it > 0.0 } == true
                    } else {
                        computedQuantity > 0
                    }
                ),
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary)
            ) {
                Text("Add Holding")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
