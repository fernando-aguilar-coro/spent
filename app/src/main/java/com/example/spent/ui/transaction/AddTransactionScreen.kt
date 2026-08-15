package com.example.spent.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spent.R
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.ui.components.CustomNumericKeypad
import com.example.spent.ui.theme.ExpenseRed
import com.example.spent.ui.theme.IncomeGreen
import com.example.spent.ui.theme.SageGreenPrimary
import com.example.spent.ui.transaction.components.AddCategoryDialog
import com.example.spent.ui.transaction.components.CategoryEnvelopeSelector
import com.example.spent.ui.transaction.components.DateTimePickerField
import com.example.spent.ui.transaction.components.RecurringOptionsSection
import com.example.spent.ui.transaction.components.TransactionTypeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    initialType: String = "EXPENSE",
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onNavigateBack: () -> Unit,
    onAddNewCategory: (name: String, colorHex: String) -> Unit,
    onAddTransaction: (amount: Double, type: String, categoryId: String, note: String, isRecurring: Boolean, frequency: String, timestamp: Long) -> Unit
) {
    var amountExpression by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedCategoryId by remember { mutableStateOf("") }

    // Date & Time Picker State
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // Recurring Options State
    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf("MONTHLY") }

    // Category Dialog State
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Keypad visibility toggle
    var showKeypad by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        text = if (selectedType == "EXPENSE") stringResource(R.string.add_expense_title) else stringResource(R.string.add_income_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Type Toggle (Expense / Income)
            TransactionTypeSelector(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Amount Card Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { showKeypad = !showKeypad }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.amount_label, currencySymbol),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (amountExpression.isEmpty()) "$currencySymbol 0.00" else "$currencySymbol $amountExpression",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == "EXPENSE") ExpenseRed else IncomeGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Keypad",
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Custom Keypad
            AnimatedVisibility(visible = showKeypad) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomNumericKeypad(
                        currentExpression = amountExpression,
                        onExpressionChanged = { amountExpression = it },
                        onConfirm = { showKeypad = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Envelope Selector Row
            CategoryEnvelopeSelector(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it },
                onAddNewCategoryClick = { showAddCategoryDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Note / Merchant Input Field
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.note_merchant_optional)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date & Time Picker Section
            DateTimePickerField(
                timestamp = selectedTimestamp,
                onTimestampChanged = { selectedTimestamp = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recurring Payment Section
            RecurringOptionsSection(
                isRecurring = isRecurring,
                onRecurringChange = { isRecurring = it },
                selectedFrequency = selectedFrequency,
                onFrequencySelected = { selectedFrequency = it }
            )

            Spacer(modifier = Modifier.height(28.dp))

            val parsedAmount = amountExpression.toDoubleOrNull() ?: 0.0
            val isValid = parsedAmount > 0

            Button(
                onClick = {
                    if (isValid) {
                        val targetCatId = selectedCategoryId.ifEmpty {
                            categories.find { it.id == "cat_general" }?.id ?: categories.firstOrNull()?.id ?: "cat_general"
                        }
                        onAddTransaction(parsedAmount, selectedType, targetCatId, noteText, isRecurring, selectedFrequency, selectedTimestamp)
                        onNavigateBack()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "EXPENSE") ExpenseRed else SageGreenPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.save_transaction),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add New Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSaveCategory = { name, colorHex ->
                onAddNewCategory(name, colorHex)
            }
        )
    }
}
