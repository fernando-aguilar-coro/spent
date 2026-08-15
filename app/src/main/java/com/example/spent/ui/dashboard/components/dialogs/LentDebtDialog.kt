package com.example.spent.ui.dashboard.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import com.example.spent.R

@Composable
fun LentDebtDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddDebtLoanTransaction: (amount: Double, type: String, note: String) -> Unit
) {
    var debtPersonName by remember { mutableStateOf("") }
    var debtAmountText by remember { mutableStateOf("") }
    var debtType by remember { mutableStateOf("LENT") } // LENT (i gave money) vs OWED (i owe money)

    val lentToTemplate = stringResource(R.string.lent_to_prefix)
    val owedToTemplate = stringResource(R.string.owed_to_prefix)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lent_debt_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { debtType = "LENT" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.i_lent_money), fontWeight = if (debtType == "LENT") FontWeight.Bold else FontWeight.Normal)
                    }
                    TextButton(
                        onClick = { debtType = "OWED" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.i_owe_money), fontWeight = if (debtType == "OWED") FontWeight.Bold else FontWeight.Normal)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = debtPersonName,
                    onValueChange = { debtPersonName = it },
                    label = { Text(stringResource(R.string.person_name_label)) },
                    placeholder = { Text(stringResource(R.string.person_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = debtAmountText,
                    onValueChange = { debtAmountText = it },
                    label = { Text(stringResource(R.string.amount_label, currencySymbol)) },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val parsedAmount = debtAmountText.toDoubleOrNull() ?: 0.0
            TextButton(
                onClick = {
                    if (parsedAmount > 0 && debtPersonName.isNotBlank()) {
                        val note = if (debtType == "LENT") String.format(lentToTemplate, debtPersonName) else String.format(owedToTemplate, debtPersonName)
                        val type = if (debtType == "LENT") "EXPENSE" else "INCOME"
                        onAddDebtLoanTransaction(parsedAmount, type, note)
                        onDismiss()
                    }
                },
                enabled = parsedAmount > 0 && debtPersonName.isNotBlank()
            ) {
                Text(stringResource(R.string.save_record))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
