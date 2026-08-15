package com.example.spent.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.RecurringRuleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.analytics.AnalyticsScreen
import com.example.spent.ui.analytics.AnalyticsViewModel
import com.example.spent.ui.dashboard.FixedBillsScreen
import com.example.spent.ui.dashboard.LoansTrackerScreen
import com.example.spent.ui.dashboard.SavingsScreen
import com.example.spent.ui.dashboard.DashboardScreen
import com.example.spent.ui.dashboard.DashboardViewModel
import com.example.spent.ui.settings.SettingsScreen
import com.example.spent.ui.transaction.AddTransactionScreen
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

@Composable
fun SpentAppNavHost(
    repository: SpentRepository,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Analytics.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return DashboardViewModel(repository) as T
                        }
                    }
                )
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToAddTransaction = { type ->
                        navController.navigate(Screen.AddTransaction.createRoute(type))
                    },
                    onNavigateToSavingsTracker = {
                        navController.navigate(Screen.SavingsTracker.route)
                    },
                    onNavigateToFixedBills = {
                        navController.navigate(Screen.FixedBills.route)
                    },
                    onNavigateToLoansTracker = {
                        navController.navigate(Screen.LoansTracker.route)
                    }
                )
            }

            composable(Screen.Analytics.route) {
                val analyticsViewModel: AnalyticsViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return AnalyticsViewModel(repository) as T
                        }
                    }
                )
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(repository = repository)
            }

            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    navArgument("initialType") {
                        type = NavType.StringType
                        defaultValue = "EXPENSE"
                    }
                )
            ) { backStackEntry ->
                val initialType = backStackEntry.arguments?.getString("initialType") ?: "EXPENSE"
                val categories by repository.getCategoriesFlow().collectAsState(initial = emptyList())
                val currencySymbol by repository.currencySymbolFlow.collectAsState(initial = "$")

                AddTransactionScreen(
                    initialType = initialType,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onNavigateBack = { navController.popBackStack() },
                    onAddNewCategory = { name, colorHex, iconName ->
                        scope.launch {
                            val newCat = CategoryEntity(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                iconName = iconName,
                                colorHex = colorHex,
                                budgetAmount = 0.0,
                                displayOrder = categories.size + 1
                            )
                            repository.addCategory(newCat)
                        }
                    },
                    onAddTransaction = { amount, type, categoryId, note, isRecurring, frequency, timestamp ->
                        scope.launch {
                            val txId = UUID.randomUUID().toString()
                            var ruleId: String? = null

                            if (isRecurring) {
                                ruleId = UUID.randomUUID().toString()
                                val rule = RecurringRuleEntity(
                                    id = ruleId,
                                    amount = amount,
                                    categoryId = categoryId,
                                    frequency = frequency,
                                    startDate = timestamp,
                                    note = note
                                )
                                repository.addRecurringRule(rule)
                            }

                            val newTx = TransactionEntity(
                                id = txId,
                                amount = amount,
                                type = type,
                                categoryId = categoryId,
                                note = note,
                                timestamp = timestamp,
                                recurringRuleId = ruleId
                            )
                            repository.addTransaction(newTx)
                        }
                    }
                )
            }

            composable(Screen.FixedBills.route) {
                val recurringRules by repository.getRecurringRulesFlow().collectAsState(initial = emptyList())
                val transactions by repository.getTransactionsFlow().collectAsState(initial = emptyList())
                val categories by repository.getCategoriesFlow().collectAsState(initial = emptyList())
                val currencySymbol by repository.currencySymbolFlow.collectAsState(initial = "$")

                FixedBillsScreen(
                    recurringRules = recurringRules,
                    transactions = transactions,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onNavigateBack = { navController.popBackStack() },
                    onAddBill = { name, amount, dueDay, categoryId, arrivalTimestamp ->
                        scope.launch {
                            val rule = RecurringRuleEntity(
                                id = UUID.randomUUID().toString(),
                                amount = amount,
                                categoryId = categoryId,
                                frequency = "MONTHLY",
                                startDate = arrivalTimestamp,
                                note = "Bill: $name"
                            )
                            repository.addRecurringRule(rule)
                        }
                    },
                    onDeleteBill = { ruleId ->
                        scope.launch {
                            repository.deleteRecurringRuleById(ruleId)
                        }
                    },
                    onPayBill = { amount, name, categoryId, ruleId ->
                        scope.launch {
                            val tx = TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                amount = amount,
                                type = "EXPENSE",
                                categoryId = categoryId,
                                note = "Bill Payment: $name",
                                timestamp = System.currentTimeMillis(),
                                recurringRuleId = ruleId
                            )
                            repository.addTransaction(tx)
                        }
                    }
                )
            }

            composable(Screen.LoansTracker.route) {
                val transactions by repository.getTransactionsFlow().collectAsState(initial = emptyList())
                val currencySymbol by repository.currencySymbolFlow.collectAsState(initial = "$")
                val categories by repository.getCategoriesFlow().collectAsState(initial = emptyList())
                val generalCatId = categories.find { it.id == "cat_general" }?.id ?: categories.firstOrNull()?.id ?: "cat_general"

                LoansTrackerScreen(
                    transactions = transactions,
                    currencySymbol = currencySymbol,
                    onNavigateBack = { navController.popBackStack() },
                    onAddDebtLoanTransaction = { amount, type, note ->
                        scope.launch {
                            val tx = TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                amount = amount,
                                type = type,
                                categoryId = generalCatId,
                                note = note,
                                timestamp = System.currentTimeMillis()
                            )
                            repository.addTransaction(tx)
                        }
                    },
                    onAddDebtInstallmentPlan = { installmentAmount, durationMonths, note ->
                        scope.launch {
                            val cal = Calendar.getInstance()
                            val startDate = cal.timeInMillis
                            cal.add(Calendar.MONTH, durationMonths)
                            val endDate = cal.timeInMillis

                            val rule = RecurringRuleEntity(
                                id = UUID.randomUUID().toString(),
                                amount = installmentAmount,
                                categoryId = generalCatId,
                                frequency = "MONTHLY",
                                startDate = startDate,
                                endDate = endDate,
                                note = note
                            )
                            repository.addRecurringRule(rule)
                        }
                    }
                )
            }

            composable(Screen.SavingsTracker.route) {
                val savingsGoalName by repository.savingsGoalNameFlow.collectAsState(initial = "")
                val savingsGoalTotal by repository.savingsGoalTotalFlow.collectAsState(initial = 0.0)
                val savingsMonthlyContribution by repository.savingsMonthlyContributionFlow.collectAsState(initial = 0.0)
                val transactions by repository.getTransactionsFlow().collectAsState(initial = emptyList())
                val categories by repository.getCategoriesFlow().collectAsState(initial = emptyList())
                val currencySymbol by repository.currencySymbolFlow.collectAsState(initial = "$")

                val savingsCatId = categories.find { it.id == "cat_savings" || it.name.equals("Savings", ignoreCase = true) }?.id ?: "cat_savings"

                SavingsScreen(
                    savingsGoalName = savingsGoalName,
                    savingsGoalTotal = savingsGoalTotal,
                    savingsMonthlyContribution = savingsMonthlyContribution,
                    transactions = transactions,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onNavigateBack = { navController.popBackStack() },
                    onSetSavingsGoal = { name, totalGoal, monthlyContribution ->
                        scope.launch {
                            repository.setSavingsGoal(name, totalGoal, monthlyContribution)
                        }
                    },
                    onClearSavingsGoal = {
                        scope.launch {
                            repository.clearSavingsGoal()
                        }
                    },
                    onDepositFunds = { amount, note ->
                        scope.launch {
                            val tx = TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                amount = amount,
                                type = "EXPENSE",
                                categoryId = savingsCatId,
                                note = note.ifBlank { "Savings Deposit" },
                                timestamp = System.currentTimeMillis()
                            )
                            repository.addTransaction(tx)
                        }
                    }
                )
            }
        }
    }
}
