package com.app.spent.ui.portfolio.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.ui.theme.SageGreenPrimary

enum class EditQuantityMode { UNITS, USD_VALUE }

@Composable
fun EditQuantityDialog(
    symbol: String,
    currentQuantity: Double,
    unitPrice: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var mode by remember { mutableStateOf(EditQuantityMode.UNITS) }
    var unitsText by remember { mutableStateOf(currentQuantity.toString()) }
    var totalUsdText by remember {
        mutableStateOf(if (unitPrice > 0) (currentQuantity * unitPrice).toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update $symbol Holdings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == EditQuantityMode.UNITS,
                        onClick = { mode = EditQuantityMode.UNITS },
                        label = { Text("Por Unidades", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = mode == EditQuantityMode.USD_VALUE,
                        onClick = { mode = EditQuantityMode.USD_VALUE },
                        label = { Text("Por Valor Total (USD)", fontSize = 11.sp) }
                    )
                }

                if (mode == EditQuantityMode.UNITS) {
                    OutlinedTextField(
                        value = unitsText,
                        onValueChange = { unitsText = it },
                        label = { Text("Unidades / Acciones") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = totalUsdText,
                        onValueChange = { totalUsdText = it },
                        label = { Text("Valor Total Equivalente ($ USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (unitPrice > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Precio actual: $$unitPrice / unidad",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQty = if (mode == EditQuantityMode.UNITS) {
                        unitsText.toDoubleOrNull()
                    } else {
                        val usd = totalUsdText.toDoubleOrNull()
                        if (usd != null && unitPrice > 0) {
                            usd / unitPrice
                        } else {
                            usd
                        }
                    }
                    if (finalQty != null && finalQty >= 0) {
                        onConfirm(finalQty)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
