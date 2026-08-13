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
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.analytics.AnalyticsScreen
import com.example.spent.ui.analytics.AnalyticsViewModel
import com.example.spent.ui.dashboard.DashboardScreen
import com.example.spent.ui.dashboard.DashboardViewModel
import com.example.spent.ui.settings.SettingsScreen
import com.example.spent.ui.transaction.AddTransactionScreen
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SpentAppNavHost(
    repository: SpentRepository,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    // Show bottom bar only on core 3 tab screens
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
                    onAddTransaction = { amount, type, categoryId, note ->
                        scope.launch {
                            val newTx = TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                amount = amount,
                                type = type,
                                categoryId = categoryId,
                                note = note,
                                timestamp = System.currentTimeMillis()
                            )
                            repository.addTransaction(newTx)
                        }
                    }
                )
            }
        }
    }
}
