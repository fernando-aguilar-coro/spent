package com.app.spent.ui.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
@Composable
fun RecurringOptionsSection(
isRecurring: Boolean,
onRecurringChange: (Boolean) -> Unit,
selectedFrequency: String,
onFrequencySelected: (String) -> Unit,
modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
    modifier = Modifier
    .fillMaxWidth()
    .clip(RoundedCornerShape(16.dp))
    .background(MaterialTheme.colorScheme.surfaceVariant)
    .clickable { onRecurringChange(!isRecurring) }
    .padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
        imageVector = Icons.Default.EventRepeat,
        contentDescription = "Recurring",
        tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f, fill = false)) {
          Text(
          text = stringResource(R.string.make_recurring_payment),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold
          )
          Text(
          text = stringResource(R.string.recurring_schedule_desc),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Switch(
      checked = isRecurring,
      onCheckedChange = onRecurringChange
      )
    }

    if (isRecurring) {
      Spacer(modifier = Modifier.height(12.dp))
      Text(
      text = stringResource(R.string.repeat_frequency),
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
        "DAILY" to stringResource(R.string.frequency_daily),
        "WEEKLY" to stringResource(R.string.frequency_weekly),
        "MONTHLY" to stringResource(R.string.frequency_monthly)
        ).forEach { (freqKey, freqLabel) ->
          FilterChip(
          selected = selectedFrequency == freqKey,
          onClick = { onFrequencySelected(freqKey) },
          label = {
            Text(
            text = freqLabel,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            softWrap = false
            )
          },
          modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}
