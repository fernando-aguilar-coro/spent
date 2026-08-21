package com.app.spent.ui.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen
@Composable
fun SavingsSummaryCard(
currencySymbol: String,
totalIncome: Double,
totalSpent: Double,
netSavings: Double,
savingsRatePercentage: Float
) {
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
          imageVector = Icons.AutoMirrored.Filled.TrendingUp,
          contentDescription = "Savings",
          tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
          text = stringResource(R.string.savings_rate),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
          )
        }
        Text(
        text = "${"%.1f".format(savingsRatePercentage)}%",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      LinearProgressIndicator(
      progress = { savingsRatePercentage / 100f },
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
          Text(stringResource(R.string.total_income), style = MaterialTheme.typography.bodySmall)
          Text(
          "$currencySymbol${"%.2f".format(totalIncome)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = IncomeGreen
          )
        }
        Column {
          Text(stringResource(R.string.total_spent), style = MaterialTheme.typography.bodySmall)
          Text(
          "$currencySymbol${"%.2f".format(totalSpent)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ExpenseRed
          )
        }
        Column {
          Text(stringResource(R.string.net_savings), style = MaterialTheme.typography.bodySmall)
          Text(
          "$currencySymbol${"%.2f".format(netSavings)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}
