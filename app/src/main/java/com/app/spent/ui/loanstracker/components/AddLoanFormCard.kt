package com.app.spent.ui.loanstracker.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen
import com.app.spent.ui.transaction.components.CategoryEnvelopeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanFormCard(
    initialType: String,
    editingLoan: LoanEntity?,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onCloseForm: () -> Unit,
    onAddNewCategoryClick: () -> Unit,
    onSaveLoan: (
        type: String,
        amount: Double,
        categoryId: String,
        calculationMode: String,
        isInstallment: Boolean,
        installmentAmount: Double?,
        installmentDurationMonths: Int?,
        interestRate: Double,
        startDate: Long,
        endDate: Long?,
        note: String,
        editingId: String?
    ) -> Unit
) {
    var loanType by remember(initialType, editingLoan) {
        mutableStateOf(editingLoan?.type ?: initialType)
    }

    // Calculation mode: 0 = By Monthly Quota ("MONTHLY_QUOTA"), 1 = By Total Amount ("TOTAL_PRINCIPAL")
    var selectedTabMode by remember(editingLoan) {
        mutableIntStateOf(
            if (editingLoan?.calculationMode == "MONTHLY_QUOTA") 0 else 1
        )
    }

    // Path 1 Inputs (Monthly Quota)
    var monthlyQuotaText by remember(editingLoan) {
        mutableStateOf(
            if (editingLoan?.calculationMode == "MONTHLY_QUOTA" && editingLoan?.installmentAmount != null && editingLoan.installmentAmount > 0) {
                "%.2f".format(editingLoan.installmentAmount).removeSuffix(".00")
            } else ""
        )
    }
    var quotaDurationMonths by remember(editingLoan) {
        mutableIntStateOf(editingLoan?.installmentDurationMonths ?: 12)
    }

    // Path 2 Inputs (Total Principal)
    var totalPrincipalText by remember(editingLoan) {
        mutableStateOf(
            if (editingLoan?.calculationMode != "MONTHLY_QUOTA" && editingLoan != null) {
                "%.2f".format(editingLoan.principalAmount).removeSuffix(".00")
            } else ""
        )
    }
    var interestRateText by remember(editingLoan) {
        mutableStateOf(editingLoan?.let { if (it.interestRate > 0) "%.1f".format(it.interestRate).removeSuffix(".0") else "" } ?: "")
    }
    var totalDurationMonths by remember(editingLoan) {
        mutableStateOf<Int?>(if (editingLoan?.calculationMode != "MONTHLY_QUOTA") editingLoan?.installmentDurationMonths else null)
    }

    // Start & End Dates
    var startDateMillis by remember(editingLoan) {
        mutableLongStateOf(editingLoan?.startDate ?: System.currentTimeMillis())
    }
    var customEndDateMillis by remember(editingLoan) {
        mutableStateOf<Long?>(editingLoan?.endDate)
    }

    // Category
    val defaultCategoryId = remember(categories) {
        val loanCat = categories.find {
            it.id == "cat_loans" ||
            it.name.contains("Loan", ignoreCase = true) ||
            it.name.contains("Préstamo", ignoreCase = true) ||
            it.name.contains("General", ignoreCase = true)
        }
        loanCat?.id ?: categories.firstOrNull()?.id ?: "cat_general"
    }

    var selectedCategoryId by remember(editingLoan, defaultCategoryId) {
        mutableStateOf(editingLoan?.categoryId ?: defaultCategoryId)
    }

    var notesText by remember(editingLoan) {
        mutableStateOf(editingLoan?.note ?: "")
    }

    // Focus Requester
    val focusRequesterQuota = remember { FocusRequester() }
    val focusRequesterTotal = remember { FocusRequester() }

    LaunchedEffect(selectedTabMode) {
        if (selectedTabMode == 0) {
            focusRequesterQuota.requestFocus()
        } else {
            focusRequesterTotal.requestFocus()
        }
    }

    // Date Picker Dialog State
    var activeDatePickerTarget by remember { mutableStateOf<String?>(null) } // "START" or "END"

    val isIOwe = loanType == "I_OWE"
    val accentColor = if (isIOwe) ExpenseRed else IncomeGreen

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isIOwe) Icons.Default.Payments else Icons.Default.Handshake,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (editingLoan != null) {
                            stringResource(R.string.edit_loan_title)
                        } else if (isIOwe) {
                            stringResource(R.string.add_loan_screen_title_owe)
                        } else {
                            stringResource(R.string.add_loan_screen_title_lend)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                IconButton(
                    onClick = onCloseForm,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Direction Selector: "Debo a" vs "Me deben"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { loanType = "I_OWE" },
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
                    onClick = { loanType = "OWED_TO_ME" },
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

            // Mode Selector Tabs (Monthly Quota vs Total Principal)
            TabRow(
                selectedTabIndex = selectedTabMode,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabMode]),
                        color = accentColor
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTabMode == 0,
                    onClick = { selectedTabMode = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.tab_calc_monthly_quota),
                            fontWeight = if (selectedTabMode == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (selectedTabMode == 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTabMode == 1,
                    onClick = { selectedTabMode = 1 },
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

            // ==================== PATH 1: BY MONTHLY QUOTA ====================
            if (selectedTabMode == 0) {
                val parsedQuota = monthlyQuotaText.toDoubleOrNull() ?: 0.0
                val computedTotal = parsedQuota * quotaDurationMonths

                // 1. Monthly Quota Input Field
                OutlinedTextField(
                    value = monthlyQuotaText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^[0-9+×÷\\-\\.\\,\\s]*$"))) {
                            monthlyQuotaText = input
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
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterQuota)
                )

                // 2. Duration Shortcuts (Months)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.loan_duration_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf(3, 6, 12, 18, 24, 36, 48)) { months ->
                            FilterChip(
                                selected = quotaDurationMonths == months,
                                onClick = {
                                    quotaDurationMonths = months
                                    // Update end date automatically based on duration
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = startDateMillis
                                        add(Calendar.MONTH, months)
                                    }
                                    customEndDateMillis = cal.timeInMillis
                                },
                                label = { Text("$months m", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // 3. Start Date & End Date Row
                val computedEndDate = remember(startDateMillis, quotaDurationMonths, customEndDateMillis) {
                    customEndDateMillis ?: Calendar.getInstance().apply {
                        timeInMillis = startDateMillis
                        add(Calendar.MONTH, quotaDurationMonths)
                    }.timeInMillis
                }
                val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Date Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { activeDatePickerTarget = "START" }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.loan_start_date_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(dateFormat.format(Date(startDateMillis)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // End Date Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { activeDatePickerTarget = "END" }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.loan_end_date_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(dateFormat.format(Date(computedEndDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4. Live Total Summary Pill
                if (parsedQuota > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.loan_calculated_total_pill,
                                "$currencySymbol${"%.2f".format(computedTotal)}",
                                quotaDurationMonths,
                                "$currencySymbol${"%.2f".format(parsedQuota)}"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ==================== PATH 2: BY TOTAL LOAN AMOUNT ====================
            if (selectedTabMode == 1) {
                val parsedPrincipal = totalPrincipalText.toDoubleOrNull() ?: 0.0
                val parsedRate = interestRateText.toDoubleOrNull() ?: 0.0
                val totalWithInterest = if (parsedRate > 0) parsedPrincipal * (1.0 + (parsedRate / 100.0)) else parsedPrincipal
                val activeDuration = totalDurationMonths
                val isIndefinite = activeDuration == null || activeDuration <= 0

                // Safe monthly quota computation (guards against division by 0 / infinity)
                val safeMonthlyQuota = if (!isIndefinite && activeDuration != null && activeDuration > 0) {
                    totalWithInterest / activeDuration
                } else null

                // 1. Total Principal Amount Input
                OutlinedTextField(
                    value = totalPrincipalText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^[0-9+×÷\\-\\.\\,\\s]*$"))) {
                            totalPrincipalText = input
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
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterTotal)
                )

                // 2. Interest Rate (%) Input Field (Default 0%)
                OutlinedTextField(
                    value = interestRateText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            interestRateText = input
                        }
                    },
                    label = { Text(stringResource(R.string.loan_interest_label), fontSize = 13.sp) },
                    placeholder = { Text("0.0%", fontSize = 13.sp) },
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Duration Selector (Default Indefinite / or Duration Shortcuts)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.loan_duration_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = isIndefinite,
                                onClick = {
                                    totalDurationMonths = null
                                    customEndDateMillis = null
                                },
                                label = { Text(stringResource(R.string.loan_duration_indefinite), fontSize = 12.sp) }
                            )
                        }
                        items(listOf(3, 6, 12, 18, 24, 36, 48)) { months ->
                            FilterChip(
                                selected = totalDurationMonths == months,
                                onClick = {
                                    totalDurationMonths = months
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = startDateMillis
                                        add(Calendar.MONTH, months)
                                    }
                                    customEndDateMillis = cal.timeInMillis
                                },
                                label = { Text("$months m", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // 4. Start & End Dates (shown when not indefinite)
                val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Date Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { activeDatePickerTarget = "START" }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.loan_start_date_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(dateFormat.format(Date(startDateMillis)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!isIndefinite && customEndDateMillis != null) {
                        // End Date Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { activeDatePickerTarget = "END" }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.loan_end_date_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(dateFormat.format(Date(customEndDateMillis!!)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 5. Live Summary Pill
                if (parsedPrincipal > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        val summaryText = if (safeMonthlyQuota != null) {
                            stringResource(
                                R.string.loan_calculated_quota_pill,
                                "$currencySymbol${"%.2f".format(safeMonthlyQuota)}",
                                "$currencySymbol${"%.2f".format(totalWithInterest)}"
                            )
                        } else {
                            "Total: $currencySymbol${"%.2f".format(totalWithInterest)}"
                        }
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ==================== COMMON: CATEGORY SELECTOR ====================
            CategoryEnvelopeSelector(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it },
                onAddNewCategoryClick = onAddNewCategoryClick
            )

            // ==================== COMMON: NOTE / DESCRIPTION (Clean, no "(opcional)") ====================
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(stringResource(R.string.loan_notes_label), fontSize = 13.sp) },
                placeholder = { Text(stringResource(R.string.loan_notes_placeholder), fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // ==================== SAVE BUTTON ====================
            val isFormValid = if (selectedTabMode == 0) {
                (monthlyQuotaText.toDoubleOrNull() ?: 0.0) > 0
            } else {
                (totalPrincipalText.toDoubleOrNull() ?: 0.0) > 0
            }

            Button(
                onClick = {
                    if (isFormValid) {
                        if (selectedTabMode == 0) {
                            val monthly = monthlyQuotaText.toDoubleOrNull() ?: 0.0
                            val total = monthly * quotaDurationMonths
                            val calEnd = customEndDateMillis ?: Calendar.getInstance().apply {
                                timeInMillis = startDateMillis
                                add(Calendar.MONTH, quotaDurationMonths)
                            }.timeInMillis

                            onSaveLoan(
                                loanType,
                                total,
                                selectedCategoryId.ifEmpty { defaultCategoryId },
                                "MONTHLY_QUOTA",
                                true,
                                monthly,
                                quotaDurationMonths,
                                0.0,
                                startDateMillis,
                                calEnd,
                                notesText.trim(),
                                editingLoan?.id
                            )
                        } else {
                            val principal = totalPrincipalText.toDoubleOrNull() ?: 0.0
                            val rate = interestRateText.toDoubleOrNull() ?: 0.0
                            val totalWithRate = if (rate > 0) principal * (1.0 + (rate / 100.0)) else principal
                            val isInst = totalDurationMonths != null && totalDurationMonths!! > 0
                            val quota = if (isInst) totalWithRate / totalDurationMonths!! else null

                            onSaveLoan(
                                loanType,
                                totalWithRate,
                                selectedCategoryId.ifEmpty { defaultCategoryId },
                                "TOTAL_PRINCIPAL",
                                isInst,
                                quota,
                                totalDurationMonths,
                                rate,
                                startDateMillis,
                                customEndDateMillis,
                                notesText.trim(),
                                editingLoan?.id
                            )
                        }
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(
                    text = if (editingLoan != null) {
                        stringResource(R.string.update_loan_btn)
                    } else {
                        stringResource(R.string.save_loan_btn)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }

    // DatePicker Dialog for Start/End Date
    if (activeDatePickerTarget != null) {
        val initialSelectedDate = if (activeDatePickerTarget == "START") startDateMillis else (customEndDateMillis ?: startDateMillis)
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDate)

        DatePickerDialog(
            onDismissRequest = { activeDatePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedTime ->
                            if (activeDatePickerTarget == "START") {
                                startDateMillis = selectedTime
                            } else {
                                customEndDateMillis = selectedTime
                            }
                        }
                        activeDatePickerTarget = null
                    }
                ) {
                    Text(stringResource(R.string.btn_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDatePickerTarget = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
