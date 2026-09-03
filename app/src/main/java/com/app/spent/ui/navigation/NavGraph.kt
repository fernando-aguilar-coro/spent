package com.app.spent.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.analytics.AnalyticsScreen
import com.app.spent.ui.analytics.AnalyticsViewModel
import com.app.spent.ui.dashboard.DashboardScreen
import com.app.spent.ui.dashboard.DashboardViewModel
import com.app.spent.ui.fixedbills.FixedBillsScreen
import com.app.spent.ui.fixedbills.FixedBillsViewModel
import com.app.spent.ui.history.TransactionHistoryScreen
import com.app.spent.ui.history.TransactionHistoryViewModel
import com.app.spent.ui.loanstracker.LoansTrackerScreen
import com.app.spent.ui.loanstracker.LoansTrackerViewModel
import com.app.spent.ui.onboarding.OnboardingScreen
import com.app.spent.ui.onboarding.OnboardingViewModel
import com.app.spent.ui.savings.SavingsScreen
import com.app.spent.ui.savings.SavingsViewModel
import com.app.spent.ui.settings.SettingsScreen
import com.app.spent.ui.settings.SettingsViewModel
import com.app.spent.ui.sharedledger.SharedLedgerScreen
import com.app.spent.ui.sharedledger.SharedLedgerViewModel
import com.app.spent.ui.transaction.AddTransactionScreen
import com.app.spent.ui.transaction.AddTransactionViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SpentAppNavHost(
    repository: SpentRepository,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isWalkthroughCompleted by repository.isWalkthroughCompletedFlow.collectAsState(initial = null)

    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Analytics.route,
        Screen.Settings.route
    )

    // Decide start destination only once when initial data is available to prevent flicker
    var initialStartDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(isWalkthroughCompleted) {
        if (initialStartDestination == null && isWalkthroughCompleted != null) {
            initialStartDestination = if (isWalkthroughCompleted == false) Screen.Onboarding.route else Screen.Dashboard.route
        }
    }

    val startDest = initialStartDestination
    if (startDest == null) return // Wait for initial data

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                val onboardingViewModel: OnboardingViewModel = koinViewModel()
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = koinViewModel()
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToAddTransaction = { type ->
                        navController.navigate(Screen.AddTransaction.createRoute(type))
                    },
                    onNavigateToEditTransaction = { type, id ->
                        navController.navigate(Screen.AddTransaction.createRoute(type, id))
                    },
                    onNavigateToSavingsTracker = {
                        navController.navigate(Screen.SavingsTracker.route)
                    },
                    onNavigateToFixedBills = {
                        navController.navigate(Screen.FixedBills.route)
                    },
                    onNavigateToLoansTracker = {
                        navController.navigate(Screen.LoansTracker.route)
                    },
                    onNavigateToSharedLedgers = {
                        navController.navigate(Screen.SharedLedgers.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.TransactionHistory.route)
                    }
                )
            }

            composable(Screen.TransactionHistory.route) {
                val historyViewModel: TransactionHistoryViewModel = koinViewModel()
                TransactionHistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditTransaction = { type, id ->
                        navController.navigate(Screen.AddTransaction.createRoute(type, id))
                    },
                    onNavigateToFixedBills = {
                        navController.navigate(Screen.FixedBills.route)
                    }
                )
            }

            composable(Screen.Analytics.route) {
                val analyticsViewModel: AnalyticsViewModel = koinViewModel()
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = koinViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    repository = repository
                )
            }

            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    navArgument("initialType") {
                        type = NavType.StringType
                        defaultValue = "EXPENSE"
                    },
                    navArgument("transactionId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val initialType = backStackEntry.arguments?.getString("initialType") ?: "EXPENSE"
                val transactionId = backStackEntry.arguments?.getString("transactionId")
                val addTransactionViewModel: AddTransactionViewModel = koinViewModel(
                    parameters = { parametersOf(initialType, transactionId) }
                )
                AddTransactionScreen(
                    viewModel = addTransactionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.FixedBills.route) {
                val fixedBillsViewModel: FixedBillsViewModel = koinViewModel()
                FixedBillsScreen(
                    viewModel = fixedBillsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.LoansTracker.route) {
                val loansTrackerViewModel: LoansTrackerViewModel = koinViewModel()
                LoansTrackerScreen(
                    viewModel = loansTrackerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SavingsTracker.route) {
                val savingsViewModel: SavingsViewModel = koinViewModel()
                SavingsScreen(
                    viewModel = savingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SharedLedgers.route) {
                val sharedLedgerViewModel: SharedLedgerViewModel = koinViewModel()
                SharedLedgerScreen(
                    viewModel = sharedLedgerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
