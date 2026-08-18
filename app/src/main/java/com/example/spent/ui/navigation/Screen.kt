package com.app.spent.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.spent.R
sealed class Screen(val route: String, @StringRes val titleResId: Int, val icon: ImageVector) {
  object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Default.Home)
  object Analytics : Screen("analytics", R.string.nav_analytics, Icons.Default.BarChart)
  object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
  object FixedBills : Screen("fixed_bills", R.string.fixed_bills_screen_title, Icons.Default.Bolt)
  object LoansTracker : Screen("loans_tracker", R.string.loans_tracker_screen_title, Icons.Default.AccountBalance)
  object SavingsTracker : Screen("savings_tracker", R.string.savings_tracker_title, Icons.Default.Savings)
  object SharedLedgers : Screen("shared_ledgers", R.string.shared_ledgers_screen_title, Icons.Default.AccountBalance)
  object Onboarding : Screen("onboarding", R.string.onboarding_welcome_title, Icons.Default.AccountBalance)
  object AddTransaction : Screen("add_transaction/{initialType}", R.string.action_add_expense, Icons.Default.Add) {
    fun createRoute(initialType: String) = "add_transaction/$initialType"
  }
}
