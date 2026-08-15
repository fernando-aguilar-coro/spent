package com.example.spent.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.R

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String
)

@Composable
fun CurrencySelectionCard(
    currentCurrencySymbol: String,
    onSelectCurrencySymbol: (symbol: String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val currencyList = remember {
        listOf(
            CurrencyOption("USD", "$", "US Dollar ($)"),
            CurrencyOption("BOB", "Bs.", "Bolivian Boliviano (Bs.)"),
            CurrencyOption("VES", "Bs.", "Venezuelan Bolívar (Bs.)"),
            CurrencyOption("EUR", "€", "Euro (€)"),
            CurrencyOption("GBP", "£", "British Pound (£)"),
            CurrencyOption("MXN", "MXN $", "Mexican Peso (MXN $)"),
            CurrencyOption("COP", "COP $", "Colombian Peso (COP $)"),
            CurrencyOption("BRL", "R$", "Brazilian Real (R$)"),
            CurrencyOption("PEN", "S/", "Peruvian Sol (S/)"),
            CurrencyOption("CLP", "CLP $", "Chilean Peso (CLP $)"),
            CurrencyOption("ARS", "ARS $", "Argentine Peso (ARS $)"),
            CurrencyOption("PYG", "₲", "Paraguayan Guaraní (₲)"),
            CurrencyOption("JPY", "¥", "Japanese Yen (¥)")
        )
    }

    val selectedLabel = remember(currentCurrencySymbol) {
        currencyList.find { it.symbol == currentCurrencySymbol }?.name ?: currentCurrencySymbol
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = "Currency",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.currency_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.currency_title), fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    items(currencyList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCurrencySymbol(item.symbol)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentCurrencySymbol == item.symbol,
                                onClick = {
                                    onSelectCurrencySymbol(item.symbol)
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }
}
