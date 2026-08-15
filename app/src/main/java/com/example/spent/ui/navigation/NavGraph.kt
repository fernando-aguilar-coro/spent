package com.example.spent.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.analytics.AnalyticsScreen
import com.example.spent.ui.analytics.AnalyticsViewModel
import com.example.spent.ui.dashboard.DashboardScreen
import com.example.spent.ui.dashboard.DashboardViewModel
import com.example.spent.ui.dashboard.FixedBillsScreen
import com.example.spent.ui.dashboard.FixedBillsViewModel
import com.example.spent.ui.dashboard.LoansTrackerScreen
import com.example.spent.ui.dashboard.LoansTrackerViewModel
import com.example.spent.ui.dashboard.SavingsScreen
import com.example.spent.ui.dashboard.SavingsViewModel
import com.example.spent.ui.settings.SettingsScreen
import com.example.spent.ui.settings.SettingsViewModel
import com.example.spent.ui.transaction.AddTransactionScreen
import com.example.spent.ui.transaction.AddTransactionViewModel

@Composable
fun SpentAppNavHost(
    repository: SpentRepository,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
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
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AnalyticsViewModel(repository) as T
                        }
                    }
                )
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SettingsViewModel(repository) as T
                        }
                    }
                )
                SettingsScreen(viewModel = settingsViewModel)
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
                val addTransactionViewModel: AddTransactionViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AddTransactionViewModel(repository, initialType) as T
                        }
                    }
                )
                AddTransactionScreen(
                    viewModel = addTransactionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.FixedBills.route) {
                val fixedBillsViewModel: FixedBillsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return FixedBillsViewModel(repository) as T
                        }
                    }
                )
                FixedBillsScreen(
                    viewModel = fixedBillsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.LoansTracker.route) {
                val loansTrackerViewModel: LoansTrackerViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return LoansTrackerViewModel(repository) as T
                        }
                    }
                )
                LoansTrackerScreen(
                    viewModel = loansTrackerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SavingsTracker.route) {
                val savingsViewModel: SavingsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SavingsViewModel(repository) as T
                        }
                    }
                )
                SavingsScreen(
                    viewModel = savingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
