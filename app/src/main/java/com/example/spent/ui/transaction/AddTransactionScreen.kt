package com.example.spent.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spent.R
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.ui.components.CustomNumericKeypad
import com.example.spent.ui.theme.ExpenseRed
import com.example.spent.ui.theme.IncomeGreen
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
    onAddNewCategory: (name: String, colorHex: String, iconName: String) -> Unit,
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

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Screen Form Factor (Tablet vs Phone)
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Keypad visibility toggle
    var showKeypad by remember { mutableStateOf(false) }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = if (showKeypad && !isTablet) 320.dp else 24.dp)
            ) {
                // Type Toggle (Expense / Income)
                TransactionTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                if (selectedType == "INCOME") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(IncomeGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.salary_funding_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input Field with Keypad / Calculator Trigger
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = amountExpression,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^[0-9+×÷\\-\\.\\,\\s]*$"))) {
                                amountExpression = input
                            }
                        },
                        readOnly = !isTablet,
                        label = { Text(stringResource(R.string.amount_label, currencySymbol)) },
                        placeholder = { Text("0.00") },
                        prefix = {
                            Text(
                                text = "$currencySymbol ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (selectedType == "EXPENSE") ExpenseRed else IncomeGreen
                            )
                        },
                        singleLine = true,
                        keyboardOptions = if (isTablet) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (!showKeypad) {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                    showKeypad = !showKeypad
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (showKeypad) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = "Toggle Calculator",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(!isTablet) {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                showKeypad = true
                            }
                    )
                }

                // Inline Keypad for Tablet mode
                if (isTablet) {
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
                        containerColor = if (selectedType == "EXPENSE") ExpenseRed else MaterialTheme.colorScheme.primary
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

            // Bottom Docked Custom Numeric Keypad for Mobile (overlaid at bottom like Android IME)
            if (!isTablet) {
                AnimatedVisibility(
                    visible = showKeypad,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // Keypad Header Bar with Drag Handle & Close
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "$currencySymbol${amountExpression.ifEmpty { "0.00" }}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedType == "EXPENSE") ExpenseRed else IncomeGreen
                                    )
                                }

                                IconButton(
                                    onClick = { showKeypad = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            CustomNumericKeypad(
                                currentExpression = amountExpression,
                                onExpressionChanged = { amountExpression = it },
                                onConfirm = { showKeypad = false }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add New Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSaveCategory = { name, colorHex, iconName ->
                onAddNewCategory(name, colorHex, iconName)
            }
        )
    }
}
