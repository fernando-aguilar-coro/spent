package com.example.spent.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.data.local.entity.PayCycleEntity

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import java.util.Locale

@Composable
fun PayCycleCard(
    currentPayCycle: PayCycleEntity?,
    currencySymbol: String = "$",
    onSavePayCycle: (frequency: String, income: Double) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val frequencies = listOf(
        "NONE" to "None / Flexible (No regular salary)",
        "WEEKLY" to "Weekly",
        "BIWEEKLY" to "Bi-weekly",
        "SEMIMONTHLY" to "Semi-monthly",
        "MONTHLY" to "Monthly"
    )

    val currentFreq = currentPayCycle?.frequency ?: "MONTHLY"
    val currentFrequencyLabel = frequencies.find { it.first == currentFreq }?.second ?: "Monthly"
    val isCycleActive = currentFreq != "NONE"

    var selectedFrequency by remember(currentPayCycle) { mutableStateOf(currentFreq) }
    var incomeText by remember(currentPayCycle) {
        val inc = currentPayCycle?.income ?: 0.0
        mutableStateOf(if (inc > 0) String.format(Locale.US, "%.2f", inc) else "")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                selectedFrequency = currentPayCycle?.frequency ?: "MONTHLY"
                val inc = currentPayCycle?.income ?: 0.0
                incomeText = if (inc > 0) String.format(Locale.US, "%.2f", inc) else ""
                showDialog = true
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Pay Cycle",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Pay Cycle & Salary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val summaryText = if (isCycleActive) {
                    val inc = currentPayCycle?.income ?: 0.0
                    "$currentFrequencyLabel • Expected: $currencySymbol${String.format(Locale.US, "%.2f", inc)}"
                } else {
                    currentFrequencyLabel
                }
                Text(summaryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Configure Pay Cycle & Income", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select Frequency", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    frequencies.forEach { (freqKey, freqLabel) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFrequency = freqKey }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFrequency == freqKey,
                                onClick = { selectedFrequency = freqKey }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(freqLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (selectedFrequency != "NONE") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Expected Income / Salary per Cycle",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = incomeText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    incomeText = input
                                }
                            },
                            label = { Text("Cycle Income ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Used to calculate your daily Safe to Spend allowance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedIncome = incomeText.toDoubleOrNull() ?: 0.0
                        onSavePayCycle(selectedFrequency, if (selectedFrequency == "NONE") 0.0 else parsedIncome)
                        showDialog = false
                    }
                ) {
                    Text("Save Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
