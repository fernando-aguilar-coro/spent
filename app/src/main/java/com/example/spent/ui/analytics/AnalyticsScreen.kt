package com.example.spent.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.ui.theme.ExpenseRed
import com.example.spent.ui.theme.IncomeGreen
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
      text = "Analytics & Reports",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold
      )
      Text(
      text = "Spending breakdown and cycle insights",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(16.dp))
    }

    // Income vs Expense Summary Card
    item {
      Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
              imageVector = Icons.Default.TrendingUp,
              contentDescription = "Savings",
              tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
              text = "Savings Rate",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
              )
            }
            Text(
            text = "${"%.1f".format(state.savingsRatePercentage)}%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          LinearProgressIndicator(
          progress = { state.savingsRatePercentage / 100f },
          modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
          )

          Spacer(modifier = Modifier.height(16.dp))

          Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Total Income", style = MaterialTheme.typography.bodySmall)
              Text(
              "${state.currencySymbol}${"%.2f".format(state.totalIncome)}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = IncomeGreen
              )
            }
            Column {
              Text("Total Spent", style = MaterialTheme.typography.bodySmall)
              Text(
              "${state.currencySymbol}${"%.2f".format(state.totalSpent)}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = ExpenseRed
              )
            }
            Column {
              Text("Net Savings", style = MaterialTheme.typography.bodySmall)
              Text(
              "${state.currencySymbol}${"%.2f".format(state.netSavings)}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

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
        text = "Category Distribution",
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
          text = "No spending recorded for this cycle.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      items(state.categoryBreakdowns) { breakdown ->
        val cat = breakdown.category
        val color = runCatching { Color(android.graphics.Color.parseColor(cat.colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)

        Card(
        modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                text = cat.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
                )
              }

              Text(
              text = "${state.currencySymbol}${"%.2f".format(breakdown.totalSpent)} (${"%.1f".format(breakdown.percentageOfTotal * 100)}%)",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
            progress = { breakdown.percentageOfTotal },
            modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
            )
          }
        }
      }
    }
  }
}
