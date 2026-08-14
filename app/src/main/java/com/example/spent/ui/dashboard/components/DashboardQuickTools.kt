package com.example.spent.ui.dashboard.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.theme.IncomeGreen
import com.example.spent.util.DataExportHelper

@Composable
fun DashboardQuickTools(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    onAddDebtLoanTransaction: (amount: Double, type: String, note: String) -> Unit,
    onAddSavingsTransaction: (amount: Double, note: String) -> Unit = { _, _ -> },
    onUpdateSavingsGoal: (goal: Double) -> Unit = {}
) {
    val context = LocalContext.current

    var showSavingsDialog by remember { mutableStateOf(false) }
    var showLoansDialog by remember { mutableStateOf(false) }
    var showDebtDialog by remember { mutableStateOf(false) }

    // Savings computations
    val savingsCat = categories.find { it.id == "cat_savings" || it.name.equals("Savings", ignoreCase = true) }
    val totalSaved = transactions
        .filter { it.categoryId == savingsCat?.id && it.type == "EXPENSE" }
        .sumOf { it.amount }
    val savingsGoal = savingsCat?.budgetAmount ?: 0.0

    // Loans computations
    val totalLoansReceived = transactions
        .filter { it.type == "INCOME" && (it.note.contains("Loan", ignoreCase = true) || it.note.contains("Préstamo", ignoreCase = true)) }
        .sumOf { it.amount }
    val totalLoansPaid = transactions
        .filter { it.type == "EXPENSE" && (it.note.contains("Loan Payment", ignoreCase = true) || it.note.contains("Pago Préstamo", ignoreCase = true)) }
        .sumOf { it.amount }
    val netLoanRemaining = (totalLoansReceived - totalLoansPaid).coerceAtLeast(0.0)

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "Extra Tools & Shortcuts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: Savings Tracker & Loans (Préstamos)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Savings Tracker Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showSavingsDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "Savings Goal",
                            tint = IncomeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Savings",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (savingsGoal > 0) "Goal: $currencySymbol${"%.0f".format(savingsGoal)}" else "Track goals & funds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // 2. Loans (Préstamos) Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showLoansDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Loans",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Loans",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (netLoanRemaining > 0) "Debt: $currencySymbol${"%.0f".format(netLoanRemaining)}" else "Bank & personal loans",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Lent / Debt (IOU) & Recurring Rules
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3. Lent / Debt (IOU) Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showDebtDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = "Lent / Debt",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Lent / Debt",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Track debts & IOUs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // 4. Recurring Rules Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val activeCount = transactions.count { it.recurringRuleId != null }
                        Toast.makeText(
                            context,
                            "Active Recurring Rules: $activeCount transactions auto-logged",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Recurring Rules",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Auto-scheduled rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3: Export Excel / CSV Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { DataExportHelper.exportTransactionsToExcelCsv(context, transactions, categories) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Excel Export",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Export to Excel / CSV",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Download spreadsheet for Excel or Google Sheets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 1. Savings Tracker Dialog
    if (showSavingsDialog) {
        var depositAmountText by remember { mutableStateOf("") }
        var depositNoteText by remember { mutableStateOf("Deposit into Savings") }
        var isEditingGoal by remember { mutableStateOf(false) }
        var newGoalText by remember { mutableStateOf(if (savingsGoal > 0) "%.2f".format(savingsGoal) else "") }

        val savingsProgress = if (savingsGoal > 0) (totalSaved / savingsGoal).toFloat().coerceIn(0f, 1f) else 0f

        AlertDialog(
            onDismissRequest = { showSavingsDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Savings, contentDescription = "Savings", tint = IncomeGreen, modifier = Modifier.size(26.dp))
                }
            },
            title = { Text("Savings Tracker", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // Overview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$currencySymbol${"%.2f".format(totalSaved)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Savings Goal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$currencySymbol${"%.2f".format(savingsGoal)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { savingsProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = IncomeGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(savingsProgress * 100).toInt()}% of goal reached",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isEditingGoal) {
                        Text("Add Funds to Savings", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = depositAmountText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    depositAmountText = input
                                }
                            },
                            label = { Text("Deposit Amount ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = depositNoteText,
                            onValueChange = { depositNoteText = it },
                            label = { Text("Note / Goal Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { isEditingGoal = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Edit Target Goal")
                        }
                    } else {
                        Text("Update Target Savings Goal", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newGoalText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    newGoalText = input
                                }
                            },
                            label = { Text("Goal Target ($currencySymbol)") },
                            placeholder = { Text("1000.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { isEditingGoal = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Back to Deposit")
                        }
                    }
                }
            },
            confirmButton = {
                if (!isEditingGoal) {
                    val parsed = depositAmountText.toDoubleOrNull() ?: 0.0
                    Button(
                        onClick = {
                            if (parsed > 0) {
                                onAddSavingsTransaction(parsed, depositNoteText.ifBlank { "Savings Deposit" })
                                showSavingsDialog = false
                            }
                        },
                        enabled = parsed > 0
                    ) {
                        Text("Deposit Funds")
                    }
                } else {
                    val parsedGoal = newGoalText.toDoubleOrNull() ?: 0.0
                    Button(
                        onClick = {
                            if (parsedGoal >= 0) {
                                onUpdateSavingsGoal(parsedGoal)
                                isEditingGoal = false
                                showSavingsDialog = false
                            }
                        }
                    ) {
                        Text("Save Goal")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavingsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 2. Loans (Préstamos) Dialog
    if (showLoansDialog) {
        var loanActionType by remember { mutableStateOf("PAY") } // "PAY" (pay installment) vs "NEW" (borrow/receive loan)
        var loanNameText by remember { mutableStateOf("") }
        var lenderBankText by remember { mutableStateOf("") }
        var loanAmountText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLoansDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = "Loans", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }
            },
            title = { Text("Loans Tracker (Préstamos)", fontWeight = FontWeight.Bold) },
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
                            Text("Outstanding Loan Debt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol${"%.2f".format(netLoanRemaining)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Borrowed: $currencySymbol${"%.2f".format(totalLoansReceived)}", style = MaterialTheme.typography.bodySmall)
                                Text("Repaid: $currencySymbol${"%.2f".format(totalLoansPaid)}", style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
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
                            Text("Pay Installment", fontWeight = if (loanActionType == "PAY") FontWeight.Bold else FontWeight.Normal)
                        }
                        TextButton(
                            onClick = { loanActionType = "NEW" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Register Loan", fontWeight = if (loanActionType == "NEW") FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = loanNameText,
                        onValueChange = { loanNameText = it },
                        label = { Text("Loan Name / Description") },
                        placeholder = { Text("e.g. Car Loan, Bank Loan, Mortgage") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (loanActionType == "NEW") {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = lenderBankText,
                            onValueChange = { lenderBankText = it },
                            label = { Text("Lender / Bank Institution") },
                            placeholder = { Text("e.g. Bank, Credit Union, Friend") },
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
                        label = { Text(if (loanActionType == "PAY") "Payment Amount ($currencySymbol)" else "Loan Capital ($currencySymbol)") },
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
                            showLoansDialog = false
                        }
                    },
                    enabled = parsed > 0 && loanNameText.isNotBlank()
                ) {
                    Text(if (loanActionType == "PAY") "Record Payment" else "Register Loan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoansDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 3. Dialog for Lent / Debt Entry (IOU)
    if (showDebtDialog) {
        var debtPersonName by remember { mutableStateOf("") }
        var debtAmountText by remember { mutableStateOf("") }
        var debtType by remember { mutableStateOf("LENT") } // LENT (i gave money) vs OWED (i owe money)

        AlertDialog(
            onDismissRequest = { showDebtDialog = false },
            title = { Text("Log Lent / Debt Record", fontWeight = FontWeight.Bold) },
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
                            Text("I Lent Money", fontWeight = if (debtType == "LENT") FontWeight.Bold else FontWeight.Normal)
                        }
                        TextButton(
                            onClick = { debtType = "OWED" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("I Owe Money", fontWeight = if (debtType == "OWED") FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = debtPersonName,
                        onValueChange = { debtPersonName = it },
                        label = { Text("Person Name") },
                        placeholder = { Text("e.g. John, Maria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = debtAmountText,
                        onValueChange = { debtAmountText = it },
                        label = { Text("Amount ($currencySymbol)") },
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
                            val note = if (debtType == "LENT") "Lent to $debtPersonName" else "Owed to $debtPersonName"
                            val type = if (debtType == "LENT") "EXPENSE" else "INCOME"
                            onAddDebtLoanTransaction(parsedAmount, type, note)
                            showDebtDialog = false
                        }
                    },
                    enabled = parsedAmount > 0 && debtPersonName.isNotBlank()
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDebtDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
