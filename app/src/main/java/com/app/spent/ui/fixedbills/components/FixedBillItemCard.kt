package com.app.spent.ui.fixedbills.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen

@Composable
fun FixedBillItemCard(
    rule: RecurringRuleEntity,
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    currentMillis: Long,
    currentMonth: Int,
    currentYear: Int,
    onPayBill: (amount: Double, name: String, categoryId: String, ruleId: String) -> Unit,
    onDeleteBill: (ruleId: String) -> Unit
) {
    val cleanName = rule.note.removePrefix("Bill: ").removePrefix("Factura: ").ifEmpty { "Bill" }

    val nowCal = Calendar.getInstance().apply { timeInMillis = currentMillis }
    val currentDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)
    val currentWeekOfYear = nowCal.get(Calendar.WEEK_OF_YEAR)

    // Check how many periods elapsed since startDate based on rule.frequency
    val daysSinceStart = ((currentMillis - rule.startDate) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)
    val elapsedCycles = when (rule.frequency) {
        "DAILY" -> daysSinceStart.toInt() + 1
        "WEEKLY" -> (daysSinceStart / 7).toInt() + 1
        else -> (daysSinceStart / 30).toInt() + 1
    }

    // Check payments recorded
    val paidCyclesCount = transactions.count { tx ->
        tx.type == "EXPENSE" &&
        (tx.recurringRuleId == rule.id || tx.note.contains(cleanName, ignoreCase = true))
    }

    val isPaidThisCurrentPeriod = transactions.any { tx ->
        val txMatches = tx.type == "EXPENSE" &&
            (tx.recurringRuleId == rule.id || tx.note.contains(cleanName, ignoreCase = true))
        if (!txMatches) return@any false

        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
        when (rule.frequency) {
            "DAILY" -> txCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear && txCal.get(Calendar.YEAR) == currentYear
            "WEEKLY" -> txCal.get(Calendar.WEEK_OF_YEAR) == currentWeekOfYear && txCal.get(Calendar.YEAR) == currentYear
            else -> txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }
    }

    val unpaidCycles = if (isPaidThisCurrentPeriod) 0 else (elapsedCycles - paidCyclesCount).coerceAtLeast(1)
    val accumulatedAmount = rule.amount * (if (unpaidCycles > 0) unpaidCycles else 1)
    val isCutoffWarning = unpaidCycles >= 3

    val dueCal = Calendar.getInstance().apply { timeInMillis = rule.startDate }
    val dueDay = dueCal.get(Calendar.DAY_OF_MONTH)
    val formattedArrival = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(rule.startDate))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCutoffWarning) ExpenseRed.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCutoffWarning) ExpenseRed.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCutoffWarning) Icons.Default.Warning else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isCutoffWarning) ExpenseRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = cleanName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val freqBadge = when (rule.frequency) {
                            "DAILY" -> stringResource(R.string.frequency_daily)
                            "WEEKLY" -> stringResource(R.string.frequency_weekly)
                            else -> stringResource(R.string.frequency_monthly)
                        }
                        Text(
                            text = "${stringResource(R.string.arrival_date_format, formattedArrival)} • $freqBadge",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { onDeleteBill(rule.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ExpenseRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ⚠️ Service Cut-Off Warning Banner (If 3+ cycles unpaid)
            if (isCutoffWarning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ExpenseRed.copy(alpha = 0.2f))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.service_cutoff_alert, unpaidCycles),
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed,
                            fontSize = 12.sp
                        )
                        Text(
                            text = stringResource(R.string.service_cutoff_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (unpaidCycles == 2) {
                // 2 Cycles Overdue Warning Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.unpaid_cycles_warning, unpaidCycles, currencySymbol, accumulatedAmount),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD97706),
                        fontSize = 12.sp
                    )
                }
            }

            // Cost & Status Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val cadenceLabel = when (rule.frequency) {
                        "DAILY" -> stringResource(R.string.frequency_daily)
                        "WEEKLY" -> stringResource(R.string.frequency_weekly)
                        else -> stringResource(R.string.bill_due_day_format, dueDay)
                    }
                    Text(
                        text = if (unpaidCycles > 1) stringResource(R.string.accumulated_total_label) else cadenceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol${"%.2f".format(accumulatedAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isCutoffWarning) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unpaidCycles > 0) {
                        Button(
                            onClick = {
                                onPayBill(accumulatedAmount, cleanName, rule.categoryId, rule.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCutoffWarning) ExpenseRed else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.pay_bill_btn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.bill_status_paid),
                                style = MaterialTheme.typography.labelMedium,
                                color = IncomeGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
