package com.app.spent.ui.loanstracker.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import java.util.Calendar

@Composable
fun LoanMonthlyQuotaSection(
    monthlyQuotaText: String,
    onMonthlyQuotaChange: (String) -> Unit,
    durationMonths: Int,
    onDurationChange: (Int) -> Unit,
    startDateMillis: Long,
    customEndDateMillis: Long?,
    onDateClick: (String) -> Unit,
    currencySymbol: String,
    accentColor: androidx.compose.ui.graphics.Color,
    focusRequester: FocusRequester
) {
    val parsedQuota = monthlyQuotaText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val computedTotal = parsedQuota * durationMonths
    val computedEndDate = remember(startDateMillis, durationMonths, customEndDateMillis) {
        customEndDateMillis ?: Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            add(Calendar.MONTH, durationMonths)
        }.timeInMillis
    }

    OutlinedTextField(
        value = monthlyQuotaText,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^[0-9+×÷\\-\\.\\,\\s]*$"))) {
                onMonthlyQuotaChange(input)
            }
        },
        label = { Text(stringResource(R.string.loan_monthly_quota_label), fontSize = 13.sp) },
        placeholder = { Text("0.00", fontSize = 13.sp) },
        prefix = {
            Text(
                text = "$currencySymbol ",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = accentColor
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.loan_duration_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            items(listOf(3, 6, 12, 18, 24, 36, 48)) { months ->
                FilterChip(
                    selected = durationMonths == months,
                    onClick = { onDurationChange(months) },
                    label = { Text("$months m", fontSize = 12.sp) }
                )
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LoanDatePickerCard(
            labelRes = R.string.loan_start_date_label,
            dateMillis = startDateMillis,
            onClick = { onDateClick("START") },
            modifier = Modifier.weight(1f)
        )
        LoanDatePickerCard(
            labelRes = R.string.loan_end_date_label,
            dateMillis = computedEndDate,
            onClick = { onDateClick("END") },
            modifier = Modifier.weight(1f)
        )
    }

    if (parsedQuota > 0) {
        LoanSummaryPill(
            text = stringResource(
                R.string.loan_calculated_total_pill,
                "$currencySymbol${"%.2f".format(computedTotal)}",
                durationMonths,
                "$currencySymbol${"%.2f".format(parsedQuota)}"
            )
        )
    }
}

@Composable
fun LoanTotalPrincipalSection(
    principalText: String,
    onPrincipalChange: (String) -> Unit,
    interestRateText: String,
    onInterestRateChange: (String) -> Unit,
    durationMonths: Int?,
    onDurationChange: (Int?) -> Unit,
    startDateMillis: Long,
    customEndDateMillis: Long?,
    onDateClick: (String) -> Unit,
    currencySymbol: String,
    accentColor: androidx.compose.ui.graphics.Color,
    focusRequester: FocusRequester
) {
    val principal = principalText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val rate = interestRateText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val totalWithInterest = if (rate > 0) principal * (1.0 + rate / 100.0) else principal
    val isIndefinite = durationMonths == null || durationMonths <= 0
    val monthlyQuota = if (isIndefinite) null else totalWithInterest / durationMonths!!

    OutlinedTextField(
        value = principalText,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^[0-9+×÷\\-\\.\\,\\s]*$"))) {
                onPrincipalChange(input)
            }
        },
        label = { Text(stringResource(R.string.loan_total_principal_label), fontSize = 13.sp) },
        placeholder = { Text("0.00", fontSize = 13.sp) },
        prefix = {
            Text(
                text = "$currencySymbol ",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = accentColor
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
    )

    OutlinedTextField(
        value = interestRateText,
        onValueChange = { input ->
            val normalized = input.replace(',', '.')
            if (normalized.isEmpty() || normalized.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                onInterestRateChange(normalized)
            }
        },
        label = { Text(stringResource(R.string.loan_interest_label), fontSize = 13.sp) },
        placeholder = { Text("0.0%", fontSize = 13.sp) },
        trailingIcon = {
            Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )

    LoanDurationChips(
        selectedMonths = durationMonths,
        onMonthSelected = onDurationChange
    )

    if (!isIndefinite) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LoanDatePickerCard(
                labelRes = R.string.loan_start_date_label,
                dateMillis = startDateMillis,
                onClick = { onDateClick("START") },
                modifier = Modifier.weight(1f)
            )
            if (customEndDateMillis != null) {
                LoanDatePickerCard(
                    labelRes = R.string.loan_end_date_label,
                    dateMillis = customEndDateMillis,
                    onClick = { onDateClick("END") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (principal > 0) {
        val summary = if (monthlyQuota != null) {
            stringResource(
                R.string.loan_calculated_quota_pill,
                "$currencySymbol${"%.2f".format(monthlyQuota)}",
                "$currencySymbol${"%.2f".format(totalWithInterest)}"
            )
        } else {
            "Total: $currencySymbol${"%.2f".format(totalWithInterest)}"
        }
        LoanSummaryPill(text = summary)
    }
}
