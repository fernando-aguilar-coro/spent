package com.app.spent.ui.loanstracker.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen

@Composable
fun RecordLoanPaymentDialog(
    loan: LoanEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmPayment: (Double) -> Unit
) {
    var paymentAmountText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val isIOwe = loan.type == "I_OWE"
    val accentColor = if (isIOwe) ExpenseRed else IncomeGreen
    val remaining = loan.remainingAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.record_payment_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val counterpartyDisplay = loan.counterpartyName.ifBlank {
                    if (isIOwe) stringResource(R.string.i_owe_badge) else stringResource(R.string.owed_to_me_badge)
                }

                Text(
                    text = "$counterpartyDisplay • ${stringResource(R.string.remaining_balance_label, "$currencySymbol${"%.2f".format(remaining)}")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick presets (Installment quota, 50%, 100% full balance)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loan.installmentAmount != null && loan.installmentAmount > 0 && loan.installmentAmount <= remaining) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { paymentAmountText = String.format(java.util.Locale.US, "%.2f", loan.installmentAmount) },
                                label = { Text("Quota: $currencySymbol${"%.2f".format(loan.installmentAmount)}", fontSize = 12.sp) }
                            )
                        }
                    }
                    if (remaining > 1) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { paymentAmountText = String.format(java.util.Locale.US, "%.2f", remaining / 2.0) },
                                label = { Text("50%: $currencySymbol${"%.2f".format(remaining / 2.0)}", fontSize = 12.sp) }
                            )
                        }
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { paymentAmountText = String.format(java.util.Locale.US, "%.2f", remaining) },
                            label = { Text("Full: $currencySymbol${"%.2f".format(remaining)}", fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { input ->
                        val normalized = input.replace(',', '.')
                        if (normalized.isEmpty() || normalized.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            paymentAmountText = normalized
                        }
                    },
                    label = { Text(stringResource(R.string.payment_amount_input_label)) },
                    placeholder = { Text("0.00") },
                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, color = accentColor) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            val parsedAmount = paymentAmountText.replace(',', '.').toDoubleOrNull() ?: 0.0
            val isValid = parsedAmount > 0

            Button(
                onClick = {
                    if (isValid) {
                        onConfirmPayment(parsedAmount)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_ok),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
