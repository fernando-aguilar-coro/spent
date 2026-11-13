package com.app.spent.ui.loanstracker.components

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import com.app.spent.R
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen

@Composable
fun LoanDirectionSelector(
    loanType: String,
    onTypeChanged: (String) -> Unit
) {
    val isIOwe = loanType == "I_OWE"
    val accentColor = if (isIOwe) ExpenseRed else IncomeGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { onTypeChanged("I_OWE") },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isIOwe) ExpenseRed else Color.Transparent,
                contentColor = if (isIOwe) Color.White else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.tab_i_owe),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Button(
            onClick = { onTypeChanged("OWED_TO_ME") },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!isIOwe) IncomeGreen else Color.Transparent,
                contentColor = if (!isIOwe) Color.White else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.tab_owed_to_me),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LoanCalculationModeTabs(
    selectedTabMode: Int,
    onTabChanged: (Int) -> Unit,
    accentColor: Color
) {
    androidx.compose.material3.TabRow(
        selectedTabIndex = selectedTabMode,
        containerColor = Color.Transparent,
        indicator = { tabPositions ->
            androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabMode]),
                color = accentColor
            )
        },
        divider = {}
    ) {
        androidx.compose.material3.Tab(
            selected = selectedTabMode == 0,
            onClick = { onTabChanged(0) },
            text = {
                Text(
                    text = stringResource(R.string.tab_calc_monthly_quota),
                    fontWeight = if (selectedTabMode == 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (selectedTabMode == 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        androidx.compose.material3.Tab(
            selected = selectedTabMode == 1,
            onClick = { onTabChanged(1) },
            text = {
                Text(
                    text = stringResource(R.string.tab_calc_total_amount),
                    fontWeight = if (selectedTabMode == 1) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (selectedTabMode == 1) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Composable
fun LoanDatePickerCard(
    labelRes: Int,
    dateMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = androidx.compose.runtime.remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                Text(dateFormat.format(java.util.Date(dateMillis)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LoanSummaryPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun LoanDurationChips(
    selectedMonths: Int?,
    onMonthSelected: (Int?) -> Unit,
    indefiniteLabelRes: Int = R.string.loan_duration_indefinite
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.loan_duration_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            FilterChip(
                selected = selectedMonths == null || selectedMonths <= 0,
                onClick = { onMonthSelected(null) },
                label = { Text(stringResource(indefiniteLabelRes), fontSize = 12.sp) }
            )
            listOf(3, 6, 12, 18, 24, 36, 48).forEach { months ->
                FilterChip(
                    selected = selectedMonths == months,
                    onClick = { onMonthSelected(months) },
                    label = { Text("$months m", fontSize = 12.sp) }
                )
            }
        }
    }
}