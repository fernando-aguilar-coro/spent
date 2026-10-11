package com.app.spent.ui.portfolio.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.domain.asset.model.AssetType
import com.app.spent.ui.theme.SageGreenPrimary

@Composable
fun AddAssetDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        symbol: String,
        name: String,
        type: AssetType,
        quantity: Double,
        averageBuyPrice: Double?,
        manualPriceOverride: Double?,
        isManualPrice: Boolean
    ) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AssetType.STOCK) }
    var quantityText by remember { mutableStateOf("") }
    var buyPriceText by remember { mutableStateOf("") }
    var manualPriceText by remember { mutableStateOf("") }
    var isManualPrice by remember { mutableStateOf(false) }

    val presetSymbols = listOf(
        Triple("EUR", "Euro (Cash/Bank)", AssetType.FIAT),
        Triple("GBP", "British Pound", AssetType.FIAT),
        Triple("JPY", "Japanese Yen", AssetType.FIAT),
        Triple("MXN", "Mexican Peso", AssetType.FIAT),
        Triple("CAD", "Canadian Dollar", AssetType.FIAT),
        Triple("CHF", "Swiss Franc", AssetType.FIAT),
        Triple("USD", "US Dollar", AssetType.FIAT),
        Triple("BTC", "Bitcoin", AssetType.CRYPTO),
        Triple("ETH", "Ethereum", AssetType.CRYPTO),
        Triple("AAPL", "Apple Inc.", AssetType.STOCK),
        Triple("MSFT", "Microsoft", AssetType.STOCK),
        Triple("XAU", "Gold (oz)", AssetType.COMMODITY)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Asset / Currency",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Preset chips
                Text(
                    text = "Quick Presets & Currencies",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetSymbols.forEach { (sym, n, type) ->
                        FilterChip(
                            selected = symbol.equals(sym, ignoreCase = true),
                            onClick = {
                                symbol = sym
                                name = n
                                selectedType = type
                            },
                            label = { Text(sym, fontSize = 12.sp) }
                        )
                    }
                }

                if (selectedType == AssetType.FIAT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Live ECB rate via Frankfurter API automatically applied",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SageGreenPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Symbol input
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("Currency / Ticker Symbol (e.g. EUR, MXN, AAPL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset / Wallet Name (e.g. Euro Cash, Bank MXN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Asset Type Selector
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssetType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quantity input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(if (selectedType == AssetType.FIAT) "Amount held in $symbol" else "Units / Quantity Owned") },
                    placeholder = { Text(if (selectedType == AssetType.FIAT) "e.g. 500.00" else "e.g. 5.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),

                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Average Purchase Price (Cost Basis)
                OutlinedTextField(
                    value = buyPriceText,
                    onValueChange = { buyPriceText = it },
                    label = { Text("Avg Purchase Price (optional for P&L)") },
                    placeholder = { Text("e.g. 210.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Manual Price toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manual Fixed Price",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Override dynamic market quotes",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isManualPrice,
                        onCheckedChange = { isManualPrice = it }
                    )
                }

                if (isManualPrice) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualPriceText,
                        onValueChange = { manualPriceText = it },
                        label = { Text("Manual Unit Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantity = quantityText.toDoubleOrNull() ?: 0.0
                    val buyPrice = buyPriceText.toDoubleOrNull()
                    val manualPrice = manualPriceText.toDoubleOrNull()
                    if (symbol.isNotBlank() && quantity > 0) {
                        onConfirm(
                            symbol.trim().uppercase(),
                            name.trim(),
                            selectedType,
                            quantity,
                            buyPrice,
                            manualPrice,
                            isManualPrice
                        )
                    }
                },
                enabled = symbol.isNotBlank() && (quantityText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary)
            ) {
                Text("Add Asset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
