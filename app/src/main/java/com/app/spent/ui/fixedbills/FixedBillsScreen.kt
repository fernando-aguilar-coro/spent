package com.app.spent.ui.fixedbills

import java.util.Calendar
import java.util.TimeZone
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.fixedbills.components.AddFixedBillFormCard
import com.app.spent.ui.fixedbills.components.FixedBillItemCard
import com.app.spent.ui.fixedbills.components.FixedBillsEmptyState
import com.app.spent.ui.transaction.components.AddCategoryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedBillsScreen(
    viewModel: FixedBillsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val recurringRules = state.recurringRules
    val transactions = state.transactions
    val categories = state.categories
    val currencySymbol = state.currencySymbol

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FixedBillsUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    var isAddingNewBill by remember { mutableStateOf(false) }
    var arrivalTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var billDueDay by remember {
        val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        mutableIntStateOf(todayDay)
    }

    // Filter only fixed bills (excluding debt installment plans)
    val billRules = remember(recurringRules) {
        recurringRules.filter { !it.note.startsWith("Debt Installment:") }
    }

    val currentCal = Calendar.getInstance()
    val currentMonth = currentCal.get(Calendar.MONTH)
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentMillis = System.currentTimeMillis()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        text = stringResource(R.string.fixed_bills_screen_title),
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
                },
                actions = {
                    if (!isAddingNewBill) {
                        IconButton(onClick = { isAddingNewBill = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_fixed_bill_title),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
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
            // New Bill Form (when activated)
            item {
                AnimatedVisibility(visible = isAddingNewBill) {
                    AddFixedBillFormCard(
                        categories = categories,
                        currencySymbol = currencySymbol,
                        arrivalTimestamp = arrivalTimestamp,
                        billDueDay = billDueDay,
                        onOpenDatePicker = { showDatePicker = true },
                        onCloseForm = { isAddingNewBill = false },
                        onAddNewCategoryClick = { viewModel.onIntent(FixedBillsUiIntent.ShowAddCategoryDialog(true)) },
                        onSaveBill = { name, amount, dueDay, categoryId, timestamp ->
                            viewModel.onIntent(
                                FixedBillsUiIntent.AddBill(
                                    name = name,
                                    amount = amount,
                                    dueDay = dueDay,
                                    categoryId = categoryId,
                                    arrivalTimestamp = timestamp
                                )
                            )
                            isAddingNewBill = false
                        }
                    )
                }
            }

            // Overview & Description Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.tool_fixed_bills_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${billRules.size} ${if (billRules.size == 1) "Service" else "Services"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // List of registered fixed bills
            if (billRules.isEmpty()) {
                item {
                    FixedBillsEmptyState()
                }
            } else {
                items(billRules, key = { it.id }) { rule ->
                    FixedBillItemCard(
                        rule = rule,
                        transactions = transactions,
                        currencySymbol = currencySymbol,
                        currentMillis = currentMillis,
                        currentMonth = currentMonth,
                        currentYear = currentYear,
                        onPayBill = { amount, name, categoryId, ruleId ->
                            viewModel.onIntent(
                                FixedBillsUiIntent.PayBill(
                                    amount = amount,
                                    name = name,
                                    categoryId = categoryId,
                                    ruleId = ruleId
                                )
                            )
                        },
                        onDeleteBill = { ruleId ->
                            viewModel.onIntent(FixedBillsUiIntent.DeleteBill(ruleId))
                        }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = arrivalTimestamp
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = arrivalTimestamp
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            arrivalTimestamp = cal.timeInMillis
                            billDueDay = utcCal.get(Calendar.DAY_OF_MONTH)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (state.showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { viewModel.onIntent(FixedBillsUiIntent.ShowAddCategoryDialog(false)) },
            onSaveCategory = { name, colorHex, iconName ->
                viewModel.onIntent(FixedBillsUiIntent.CreateCategory(name, colorHex, iconName))
            }
        )
    }
}
