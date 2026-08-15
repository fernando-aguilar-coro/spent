package com.example.spent.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spent.R
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.theme.ExpenseRed
import com.example.spent.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    savingsGoalName: String,
    savingsGoalTotal: Double,
    savingsMonthlyContribution: Double,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onNavigateBack: () -> Unit,
    onSetSavingsGoal: (name: String, totalGoal: Double, monthlyContribution: Double) -> Unit,
    onClearSavingsGoal: () -> Unit,
    onDepositFunds: (amount: Double, note: String) -> Unit
) {
    val savingsCat = categories.find { it.id == "cat_savings" || it.name.equals("Savings", ignoreCase = true) }
    val savingsTransactions = transactions.filter { it.categoryId == savingsCat?.id && it.type == "EXPENSE" }
    val totalSaved = savingsTransactions.sumOf { it.amount }

    var isEditingGoal by remember { mutableStateOf(false) }
    var isDepositing by remember { mutableStateOf(false) }
    val hasActiveGoal = savingsGoalTotal > 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SavingsTopBar(onNavigateBack = onNavigateBack)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Goal Configuration Form or Active Goal Card
            item {
                if (isEditingGoal || !hasActiveGoal) {
                    SavingsGoalFormCard(
                        hasActiveGoal = hasActiveGoal,
                        initialName = savingsGoalName,
                        initialTotal = savingsGoalTotal,
                        initialMonthly = savingsMonthlyContribution,
                        currencySymbol = currencySymbol,
                        onCancelEdit = { isEditingGoal = false },
                        onSaveGoal = { name, total, monthly ->
                            onSetSavingsGoal(name, total, monthly)
                            isEditingGoal = false
                        }
                    )
                } else {
                    SavingsActiveGoalHeroCard(
                        goalName = savingsGoalName,
                        goalTotal = savingsGoalTotal,
                        monthlyContribution = savingsMonthlyContribution,
                        totalSaved = totalSaved,
                        currencySymbol = currencySymbol,
                        onEditGoal = { isEditingGoal = true },
                        onClearGoal = onClearSavingsGoal
                    )
                }
            }

            // Deposit Funds Section
            item {
                SavingsDepositSection(
                    isDepositing = isDepositing,
                    currencySymbol = currencySymbol,
                    onToggleDepositing = { isDepositing = it },
                    onConfirmDeposit = { amount, note ->
                        onDepositFunds(amount, note)
                        isDepositing = false
                    }
                )
            }

            // Savings Deposit History Header
            item {
                Text(
                    text = stringResource(R.string.savings_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Savings Deposit History List
            if (savingsTransactions.isEmpty()) {
                item {
                    SavingsEmptyHistoryPlaceholder()
                }
            } else {
                items(savingsTransactions, key = { it.id }) { tx ->
                    SavingsDepositHistoryItem(
                        transaction = tx,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
            Text(
                text = stringResource(R.string.savings_tracker_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_close)
                )
            }
        }
    )
}

@Composable
private fun SavingsGoalFormCard(
    hasActiveGoal: Boolean,
    initialName: String,
    initialTotal: Double,
    initialMonthly: Double,
    currencySymbol: String,
    onCancelEdit: () -> Unit,
    onSaveGoal: (name: String, totalGoal: Double, monthlyContribution: Double) -> Unit
) {
    var goalNameText by remember(initialName) { mutableStateOf(initialName) }
    var totalGoalText by remember(initialTotal) {
        mutableStateOf(if (initialTotal > 0) initialTotal.toString() else "")
    }
    var monthlyContributionText by remember(initialMonthly) {
        mutableStateOf(if (initialMonthly > 0) initialMonthly.toString() else "")
    }

    val parsedTotalGoal = totalGoalText.toDoubleOrNull() ?: 0.0
    val parsedMonthly = monthlyContributionText.toDoubleOrNull() ?: 0.0
    val isGoalValid = parsedTotalGoal > 0 && parsedMonthly > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (hasActiveGoal) {
                            stringResource(R.string.edit_target_goal)
                        } else {
                            stringResource(R.string.add_savings_goal_btn)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (hasActiveGoal) {
                    IconButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }
            }

            OutlinedTextField(
                value = goalNameText,
                onValueChange = { goalNameText = it },
                label = { Text(stringResource(R.string.note_goal_name_label)) },
                placeholder = { Text("e.g. Emergency Fund, New Car") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = totalGoalText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        totalGoalText = input
                    }
                },
                label = { Text(stringResource(R.string.goal_target_label, currencySymbol)) },
                placeholder = { Text("5000.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = monthlyContributionText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        monthlyContributionText = input
                    }
                },
                label = { Text(stringResource(R.string.monthly_savings_contribution_label, currencySymbol)) },
                placeholder = { Text("200.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(10.dp)
            ) {
                Text(
                    text = "💡 " + stringResource(R.string.savings_info_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    if (isGoalValid) {
                        val name = goalNameText.trim().ifEmpty { "Savings Goal" }
                        onSaveGoal(name, parsedTotalGoal, parsedMonthly)
                    }
                },
                enabled = isGoalValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.save_goal), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SavingsActiveGoalHeroCard(
    goalName: String,
    goalTotal: Double,
    monthlyContribution: Double,
    totalSaved: Double,
    currencySymbol: String,
    onEditGoal: () -> Unit,
    onClearGoal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goalName.ifEmpty { stringResource(R.string.savings_goal) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.savings_goal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onEditGoal,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearGoal,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = ExpenseRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Total Saved vs Goal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.total_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol%.2f".format(totalSaved),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.goal_target_label, "").trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol%.2f".format(goalTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Progress Bar
            val progress = if (goalTotal > 0) (totalSaved / goalTotal).toFloat().coerceIn(0f, 1f) else 0f
            val percent = (progress * 100).toInt()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = IncomeGreen,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.savings_progress_text, percent),
                        style = MaterialTheme.typography.labelSmall,
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                    val remainingToGoal = (goalTotal - totalSaved).coerceAtLeast(0.0)
                    Text(
                        text = stringResource(R.string.savings_to_go, currencySymbol, remainingToGoal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Monthly Contribution Info Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.savings_monthly_reservation),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol%.2f / mo".format(monthlyContribution),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (monthlyContribution > 0) {
                    val remainingMonths = ((goalTotal - totalSaved).coerceAtLeast(0.0) / monthlyContribution).toInt() + 1
                    Text(
                        text = stringResource(R.string.savings_months_left, remainingMonths),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsDepositSection(
    isDepositing: Boolean,
    currencySymbol: String,
    onToggleDepositing: (Boolean) -> Unit,
    onConfirmDeposit: (Double, String) -> Unit
) {
    if (!isDepositing) {
        Button(
            onClick = { onToggleDepositing(true) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_funds_to_savings), fontWeight = FontWeight.Bold)
        }
    } else {
        var depositAmountText by remember { mutableStateOf("") }
        var depositNoteText by remember { mutableStateOf("") }

        val parsedDeposit = depositAmountText.toDoubleOrNull() ?: 0.0
        val isDepositValid = parsedDeposit > 0

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.deposit_into_savings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onToggleDepositing(false) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }

                OutlinedTextField(
                    value = depositAmountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            depositAmountText = input
                        }
                    },
                    label = { Text(stringResource(R.string.deposit_amount_label, currencySymbol)) },
                    placeholder = { Text("100.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = depositNoteText,
                    onValueChange = { depositNoteText = it },
                    label = { Text(stringResource(R.string.note_goal_name_label)) },
                    placeholder = { Text("e.g. Monthly allocation, Bonus") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (isDepositValid) {
                            onConfirmDeposit(parsedDeposit, depositNoteText.trim())
                        }
                    },
                    enabled = isDepositValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                ) {
                    Text(stringResource(R.string.deposit_funds), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SavingsDepositHistoryItem(
    transaction: TransactionEntity,
    currencySymbol: String
) {
    val formattedDate = remember(transaction.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
    }
    val defaultNote = stringResource(R.string.savings_default_deposit_note)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = transaction.note.ifBlank { defaultNote },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "+$currencySymbol%.2f".format(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IncomeGreen
            )
        }
    }
}

@Composable
private fun SavingsEmptyHistoryPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.savings_no_history),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
