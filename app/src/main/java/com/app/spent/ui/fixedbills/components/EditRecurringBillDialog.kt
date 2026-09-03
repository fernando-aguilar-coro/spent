package com.app.spent.ui.fixedbills.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.ui.transaction.components.CategoryEnvelopeSelector

@Composable
fun EditRecurringBillDialog(
    rule: RecurringRuleEntity,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (updatedRule: RecurringRuleEntity) -> Unit
) {
    var nameText by remember {
        mutableStateOf(rule.note.removePrefix("Bill: ").removePrefix("Factura: ").trim())
    }
    var amountText by remember {
        mutableStateOf(if (rule.amount % 1.0 == 0.0) "%.0f".format(rule.amount) else "%.2f".format(rule.amount))
    }
    var selectedCategoryId by remember {
        mutableStateOf(rule.categoryId)
    }
    var selectedFrequency by remember {
        mutableStateOf(rule.frequency)
    }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = parsedAmount > 0 && nameText.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_bill_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(stringResource(R.string.bill_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountText = input
                        }
                    },
                    label = { Text(stringResource(R.string.bill_amount_label)) },
                    prefix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency chips
                Column {
                    Text(
                        text = stringResource(R.string.cycle_period_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "DAILY" to stringResource(R.string.frequency_daily),
                            "WEEKLY" to stringResource(R.string.frequency_weekly),
                            "MONTHLY" to stringResource(R.string.frequency_monthly)
                        ).forEach { (freqKey, freqLabel) ->
                            FilterChip(
                                selected = selectedFrequency == freqKey,
                                onClick = { selectedFrequency = freqKey },
                                label = {
                                    Text(
                                        text = freqLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedFrequency == freqKey) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Category Envelope
                CategoryEnvelopeSelector(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it },
                    onAddNewCategoryClick = {}
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val updated = rule.copy(
                            amount = parsedAmount,
                            note = if (nameText.startsWith("Bill: ") || nameText.startsWith("Factura: ")) nameText else "Bill: $nameText",
                            categoryId = selectedCategoryId,
                            frequency = selectedFrequency
                        )
                        onSave(updated)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.save_bill_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
