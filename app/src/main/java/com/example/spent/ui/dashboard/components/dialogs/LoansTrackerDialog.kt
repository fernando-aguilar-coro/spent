package com.example.spent.ui.dashboard.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.spent.R
import com.example.spent.ui.theme.IncomeGreen

@Composable
fun LoansTrackerDialog(
    netLoanRemaining: Double,
    totalLoansReceived: Double,
    totalLoansPaid: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddDebtLoanTransaction: (amount: Double, type: String, note: String) -> Unit
) {
    var loanActionType by remember { mutableStateOf("PAY") } // "PAY" (pay installment) vs "NEW" (borrow/receive loan)
    var loanNameText by remember { mutableStateOf("") }
    var lenderBankText by remember { mutableStateOf("") }
    var loanAmountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = stringResource(R.string.loans_tracker_title),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = { Text(stringResource(R.string.loans_tracker_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // Loan Summary Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.outstanding_loan_debt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol${"%.2f".format(netLoanRemaining)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.borrowed_amount, currencySymbol, totalLoansReceived),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(R.string.repaid_amount, currencySymbol, totalLoansPaid),
                                style = MaterialTheme.typography.bodySmall,
                                color = IncomeGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { loanActionType = "PAY" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.pay_installment), fontWeight = if (loanActionType == "PAY") FontWeight.Bold else FontWeight.Normal)
                    }
                    TextButton(
                        onClick = { loanActionType = "NEW" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.register_loan), fontWeight = if (loanActionType == "NEW") FontWeight.Bold else FontWeight.Normal)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = loanNameText,
                    onValueChange = { loanNameText = it },
                    label = { Text(stringResource(R.string.loan_name_label)) },
                    placeholder = { Text(stringResource(R.string.loan_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (loanActionType == "NEW") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = lenderBankText,
                        onValueChange = { lenderBankText = it },
                        label = { Text(stringResource(R.string.lender_bank_label)) },
                        placeholder = { Text(stringResource(R.string.lender_bank_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = loanAmountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            loanAmountText = input
                        }
                    },
                    label = { Text(if (loanActionType == "PAY") stringResource(R.string.payment_amount_label, currencySymbol) else stringResource(R.string.loan_capital_label, currencySymbol)) },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val parsed = loanAmountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (parsed > 0 && loanNameText.isNotBlank()) {
                        if (loanActionType == "NEW") {
                            val lender = if (lenderBankText.isNotBlank()) " from $lenderBankText" else ""
                            onAddDebtLoanTransaction(parsed, "INCOME", "Loan: $loanNameText$lender")
                        } else {
                            onAddDebtLoanTransaction(parsed, "EXPENSE", "Loan Payment: $loanNameText")
                        }
                        onDismiss()
                    }
                },
                enabled = parsed > 0 && loanNameText.isNotBlank()
            ) {
                Text(if (loanActionType == "PAY") stringResource(R.string.record_payment) else stringResource(R.string.register_loan))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
