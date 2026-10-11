package com.app.spent.ui.portfolio.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.domain.asset.model.ValuedAsset
import com.app.spent.ui.theme.AccentBlue
import com.app.spent.ui.theme.AccentPurple
import com.app.spent.ui.theme.AccentAmber
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen
import com.app.spent.ui.theme.SageGreenPrimary
import java.util.Locale

@Composable
fun AssetHoldingCard(
    asset: ValuedAsset,
    baseCurrency: String,
    onEditQuantity: () -> Unit,
    onToggleManualPrice: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val typeColor = when (asset.holding.type.uppercase()) {
        "STOCK" -> AccentBlue
        "CRYPTO" -> AccentPurple
        "FIAT" -> SageGreenPrimary
        "COMMODITY" -> AccentAmber
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = asset.holding.symbol.take(4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = typeColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Holding Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = asset.holding.symbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = asset.holding.type,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${formatQuantity(asset.holding.quantity)} units @ ${formatCurrency(asset.effectiveUnitPrice, baseCurrency)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (asset.isUsingManualPrice) {
                    Text(
                        text = "Manual price override",
                        fontSize = 10.sp,
                        color = AccentAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Valuation & 24h change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(asset.totalValuation, baseCurrency),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                val isChangePositive = asset.change24hPct >= 0
                val changeColor = if (isChangePositive) IncomeGreen else ExpenseRed

                Text(
                    text = "${if (isChangePositive) "+" else ""}${String.format(Locale.US, "%.2f", asset.change24hPct)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = changeColor
                )
            }

            // Overflow menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Quantity") },
                        onClick = {
                            menuExpanded = false
                            onEditQuantity()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (asset.isUsingManualPrice) "Use Market Price" else "Set Manual Price") },
                        onClick = {
                            menuExpanded = false
                            onToggleManualPrice()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = ExpenseRed) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed)
                        }
                    )
                }
            }
        }
    }
}

private fun formatQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toLong().toString()
    } else {
        String.format(Locale.US, "%.4f", quantity).trimEnd('0').trimEnd('.')
    }
}

private fun formatCurrency(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase()) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        else -> "$currency "
    }
    return "$symbol${String.format(Locale.US, "%,.2f", amount)}"
}
