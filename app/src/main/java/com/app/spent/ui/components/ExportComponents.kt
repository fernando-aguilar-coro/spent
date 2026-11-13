package com.app.spent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.spent.R

@Composable
fun ExportOptionCard(
    icon: ImageVector,
    titleRes: Int,
    descRes: Int,
    iconBackgroundColor: Color,
    iconTintColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ExportSummaryText(
    filteredCount: Int,
    totalCount: Int,
    holdingsCount: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Export $filteredCount of $totalCount records" +
            if (holdingsCount > 0) " + $holdingsCount holdings" else "",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun ExportDetailLevelChips(
    detailed: Boolean,
    onSimpleClick: () -> Unit,
    onDetailedClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.FilterChip(
            selected = !detailed,
            onClick = onSimpleClick,
            label = { Text(stringResource(R.string.export_detail_simple)) }
        )
        androidx.compose.material3.FilterChip(
            selected = detailed,
            onClick = onDetailedClick,
            label = { Text(stringResource(R.string.export_detail_detailed)) }
        )
    }
}

@Composable
fun ExportHoldingsToggle(
    holdingsCount: Int,
    includeHoldings: Boolean,
    onToggle: (Boolean) -> Unit
) {
    if (holdingsCount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!includeHoldings) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Checkbox(
                checked = includeHoldings,
                onCheckedChange = onToggle
            )
            Text(
                text = stringResource(R.string.export_include_holdings, holdingsCount),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}