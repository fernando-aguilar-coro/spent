package com.example.spent.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.analytics.AnalyticsScreen
import com.example.spent.ui.analytics.AnalyticsViewModel
import com.example.spent.ui.dashboard.DashboardScreen
import com.example.spent.ui.dashboard.DashboardViewModel
import com.example.spent.ui.settings.SettingsScreen

@Composable
fun SpentAppNavHost(
    repository: SpentRepository,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) }
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
                DashboardScreen(viewModel = dashboardViewModel)
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
        }
    }
}
