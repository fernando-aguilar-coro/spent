package com.app.spent.ui.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.ui.analytics.components.CategoryDistributionItem
import com.app.spent.ui.analytics.components.IncomeExpenseChart
import com.app.spent.ui.analytics.components.SavingsSummaryCard
@Composable
fun AnalyticsScreen(
viewModel: AnalyticsViewModel
) {
  val state by viewModel.uiState.collectAsState()

  LazyColumn(
  modifier = Modifier.fillMaxSize(),
  contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
  ) {
    item {
      Text(
      text = stringResource(R.string.analytics_title),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold
      )
      Text(
      text = stringResource(R.string.analytics_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(16.dp))
    }

    // Income vs Expense Summary Card
    item {
      SavingsSummaryCard(
      currencySymbol = state.currencySymbol,
      totalIncome = state.totalIncome,
      totalSpent = state.totalSpent,
      netSavings = state.netSavings,
      savingsRatePercentage = state.savingsRatePercentage
      )
      Spacer(modifier = Modifier.height(20.dp))
    }

    // 2D Income vs Expenses Over Time Chart
    item {
      IncomeExpenseChart(
      transactions = state.recentTransactions,
      currencySymbol = state.currencySymbol
      )
      Spacer(modifier = Modifier.height(24.dp))
    }

    // Category Breakdown Section
    item {
      Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 12.dp)
      ) {
        Icon(
        imageVector = Icons.Default.PieChart,
        contentDescription = "Breakdown",
        tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
        text = stringResource(R.string.category_distribution),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
        )
      }
    }

    if (state.categoryBreakdowns.isEmpty() || state.totalSpent == 0.0) {
      item {
        Box(
        modifier = Modifier
        .fillMaxWidth()
        .padding(32.dp),
        contentAlignment = Alignment.Center
        ) {
          Text(
          text = stringResource(R.string.no_spending_recorded),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      items(state.categoryBreakdowns) { breakdown ->
        CategoryDistributionItem(
        breakdown = breakdown,
        currencySymbol = state.currencySymbol
        )
      }
    }
  }
}
