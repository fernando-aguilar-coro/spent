package com.app.spent.ui.loanstracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.loanstracker.components.AddLoanFormCard
import com.app.spent.ui.loanstracker.components.LoanItemCard
import com.app.spent.ui.loanstracker.components.LoansEmptyState
import com.app.spent.ui.loanstracker.components.LoansSummaryHeroCard
import com.app.spent.ui.loanstracker.components.RecordLoanPaymentDialog
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen
import com.app.spent.ui.transaction.components.AddCategoryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansTrackerScreen(
    viewModel: LoansTrackerViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoansTrackerUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    var showAddTypeMenu by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        text = stringResource(R.string.loans_tracker_screen_title),
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
                    if (!state.isFormOpen) {
                        Box {
                            IconButton(onClick = { showAddTypeMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Loan or Debt",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = showAddTypeMenu,
                                onDismissRequest = { showAddTypeMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.tab_i_owe),
                                            fontWeight = FontWeight.SemiBold,
                                            color = ExpenseRed
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showAddTypeMenu = false
                                        viewModel.onIntent(LoansTrackerUiIntent.OpenAddForm("I_OWE"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.tab_owed_to_me),
                                            fontWeight = FontWeight.SemiBold,
                                            color = IncomeGreen
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Handshake,
                                            contentDescription = null,
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showAddTypeMenu = false
                                        viewModel.onIntent(LoansTrackerUiIntent.OpenAddForm("OWED_TO_ME"))
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.isFormOpen) {
                Box {
                    FloatingActionButton(
                        onClick = { showAddTypeMenu = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Loan")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Add / Edit Form (When Active)
            item {
                AnimatedVisibility(visible = state.isFormOpen) {
                    AddLoanFormCard(
                        initialType = state.loanTypeToAdd,
                        editingLoan = state.editingLoan,
                        categories = state.categories,
                        currencySymbol = state.currencySymbol,
                        onCloseForm = { viewModel.onIntent(LoansTrackerUiIntent.CloseForm) },
                        onAddNewCategoryClick = {
                            viewModel.onIntent(LoansTrackerUiIntent.ShowAddCategoryDialog(true))
                        },
                        onSaveLoan = { type, amount, catId, calcMode, isInst, instAmount, instDuration, rate, start, end, note, editId ->
                            viewModel.onIntent(
                                LoansTrackerUiIntent.SaveLoan(
                                    type = type,
                                    counterpartyName = "",
                                    amount = amount,
                                    categoryId = catId,
                                    calculationMode = calcMode,
                                    isInstallment = isInst,
                                    installmentAmount = instAmount,
                                    installmentDurationMonths = instDuration,
                                    interestRate = rate,
                                    startDate = start,
                                    endDate = end,
                                    note = note,
                                    editingId = editId
                                )
                            )
                        }
                    )
                }
            }

            // 2. Summary Hero Card
            item {
                LoansSummaryHeroCard(
                    totalIOwe = state.totalIOwe,
                    totalOwedToMe = state.totalOwedToMe,
                    netBalance = state.netBalance,
                    currencySymbol = state.currencySymbol
                )
            }

            // 3. Filter Segmented Chips (All, I Owe, Owed to Me)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedFilter == LoanFilter.ALL,
                        onClick = { viewModel.onIntent(LoansTrackerUiIntent.SetFilter(LoanFilter.ALL)) },
                        label = { Text(stringResource(R.string.tab_loans_all), fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = state.selectedFilter == LoanFilter.I_OWE,
                        onClick = { viewModel.onIntent(LoansTrackerUiIntent.SetFilter(LoanFilter.I_OWE)) },
                        label = { Text(stringResource(R.string.tab_i_owe), fontSize = 12.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = state.selectedFilter == LoanFilter.OWED_TO_ME,
                        onClick = { viewModel.onIntent(LoansTrackerUiIntent.SetFilter(LoanFilter.OWED_TO_ME)) },
                        label = { Text(stringResource(R.string.tab_owed_to_me), fontSize = 12.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 4. Content: Empty State vs Loan List
            if (state.filteredLoans.isEmpty()) {
                item {
                    LoansEmptyState(
                        onAddIOweClick = {
                            viewModel.onIntent(LoansTrackerUiIntent.OpenAddForm("I_OWE"))
                        },
                        onAddOwedToMeClick = {
                            viewModel.onIntent(LoansTrackerUiIntent.OpenAddForm("OWED_TO_ME"))
                        }
                    )
                }
            } else {
                items(state.filteredLoans, key = { it.id }) { loan ->
                    val cat = state.categories.find { it.id == loan.categoryId }
                    LoanItemCard(
                        loan = loan,
                        category = cat,
                        currencySymbol = state.currencySymbol,
                        onAddPaymentClick = {
                            viewModel.onIntent(LoansTrackerUiIntent.OpenPaymentDialog(it))
                        },
                        onEditClick = {
                            viewModel.onIntent(LoansTrackerUiIntent.OpenEditForm(it))
                        },
                        onSettleClick = { loanId ->
                            viewModel.onIntent(LoansTrackerUiIntent.SettleLoan(loanId))
                        },
                        onDeleteClick = { loanId ->
                            viewModel.onIntent(LoansTrackerUiIntent.DeleteLoan(loanId))
                        }
                    )
                }
            }
        }
    }

    // Payment / Abono Dialog
    state.selectedLoanForPayment?.let { loan ->
        RecordLoanPaymentDialog(
            loan = loan,
            currencySymbol = state.currencySymbol,
            onDismiss = { viewModel.onIntent(LoansTrackerUiIntent.ClosePaymentDialog) },
            onConfirmPayment = { amount ->
                viewModel.onIntent(LoansTrackerUiIntent.RecordPayment(loan.id, amount))
            }
        )
    }

    // Add New Category Dialog
    if (state.showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { viewModel.onIntent(LoansTrackerUiIntent.ShowAddCategoryDialog(false)) },
            onSaveCategory = { name, colorHex, iconName ->
                viewModel.onIntent(LoansTrackerUiIntent.CreateCategory(name, colorHex, iconName))
            }
        )
    }
}
