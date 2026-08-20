package com.app.spent.ui.sharedledger.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.spent.data.sync.SharedCategoryEnvelope
import com.app.spent.ui.components.CategoryIconHelper
import com.app.spent.ui.theme.ExpenseRed

@Composable
fun SharedCategoryEnvelopeRow(
    envelope: SharedCategoryEnvelope,
    currencySymbol: String
) {
    val catColor = remember(envelope.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(envelope.colorHex))
        } catch (e: Exception) {
            Color(0xFF64748B)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CategoryIconHelper.getIconByName(envelope.iconName),
                            contentDescription = envelope.name,
                            tint = catColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = envelope.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val breakdownText = if (envelope.memberBreakdown.isNotEmpty()) {
                            envelope.memberBreakdown.entries.joinToString(" · ") { "${it.key}: $currencySymbol${"%.0f".format(it.value)}" }
                        } else {
                            "You: $currencySymbol${"%.2f".format(envelope.mySpent)}"
                        }
                        Text(
                            text = breakdownText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = if (envelope.totalBudget > 0) {
                        "${currencySymbol}${"%.2f".format(envelope.totalSpent)} / ${currencySymbol}${"%.2f".format(envelope.totalBudget)}"
                    } else {
                        "${currencySymbol}${"%.2f".format(envelope.totalSpent)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (envelope.totalBudget > 0 && envelope.totalSpent > envelope.totalBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
            }

            if (envelope.totalBudget > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { envelope.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (envelope.totalSpent > envelope.totalBudget) ExpenseRed else catColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
