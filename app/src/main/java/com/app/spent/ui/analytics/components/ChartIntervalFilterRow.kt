package com.app.spent.ui.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.ui.analytics.ChartInterval

@Composable
fun ChartIntervalFilterRow(
    selectedInterval: ChartInterval,
    onSelectInterval: (ChartInterval) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IntervalChip(
            text = stringResource(R.string.interval_day),
            isSelected = selectedInterval == ChartInterval.DAY,
            onClick = { onSelectInterval(ChartInterval.DAY) }
        )
        IntervalChip(
            text = stringResource(R.string.interval_week),
            isSelected = selectedInterval == ChartInterval.WEEK,
            onClick = { onSelectInterval(ChartInterval.WEEK) }
        )
        IntervalChip(
            text = stringResource(R.string.interval_month),
            isSelected = selectedInterval == ChartInterval.MONTH,
            onClick = { onSelectInterval(ChartInterval.MONTH) }
        )
    }
}

@Composable
private fun IntervalChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    )
}
