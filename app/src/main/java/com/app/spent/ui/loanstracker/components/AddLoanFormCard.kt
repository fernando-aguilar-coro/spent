package com.app.spent.ui.loanstracker.components

import java.text.SimpleDateFormat
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@Composable
fun AddLoanFormCard(
    initialType: String,
    editingLoan: LoanEntity?,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onCloseForm: () -> Unit,
    onOpenDatePicker: () -> Unit,
    dueDateTimestamp: Long?,
    onAddNewCategoryClick: () -> Unit,
    onSaveLoan: (
        type: String,
        counterpartyName: String,
        amount: Double,
        categoryId: String,
        isInstallment: Boolean,
        installmentAmount: Double?,
        installmentDurationMonths: Int?,
        interestRate: Double,
        dueDate: Long?,
        note: String,
        editingId: String?
    ) -> Unit
) {
    var loanType by remember(initialType, editingLoan) {
        mutableStateOf(editingLoan?.type ?: initialType)
    }

    var principalAmountText by remember(editingLoan) {
        mutableStateOf(editingLoan?.let { "%.2f".format(it.principalAmount).removeSuffix(".00") } ?: "")
    }

    var counterpartyNameText by remember(editingLoan) {
        mutableStateOf(editingLoan?.counterpartyName ?: "")
    }

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

    var isInstallment by remember(editingLoan) {
        mutableStateOf(editingLoan?.isInstallment ?: false)
    }

    var installmentMonthlyText by remember(editingLoan) {
        mutableStateOf(editingLoan?.installmentAmount?.let { "%.2f".format(it).removeSuffix(".00") } ?: "")
    }

    var installmentDurationMonths by remember(editingLoan) {
        mutableIntStateOf(editingLoan?.installmentDurationMonths ?: 12)
    }

    var interestRateText by remember(editingLoan) {
        mutableStateOf(editingLoan?.let { if (it.interestRate > 0) "%.1f".format(it.interestRate).removeSuffix(".0") else "" } ?: "")
    }

    var notesText by remember(editingLoan) {
        mutableStateOf(editingLoan?.note ?: "")
    }

    val focusRequester = remember { FocusRequester() }

    // Auto-focus amount field when entering screen/form
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val isIOwe = loanType == "I_OWE"
    val accentColor = if (isIOwe) ExpenseRed else IncomeGreen

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isIOwe) Icons.Default.Payments else Icons.Default.Handshake,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
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
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onCloseForm,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Direction Selector Buttons ("I Owe" vs "Owed to Me")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
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

                Spacer(modifier = Modifier.width(6.dp))

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

            // 1. Amount Field (Auto-focused on entry)
            OutlinedTextField(
                value = principalAmountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^[0-9+×÷\\-\\.\\,\\s]*$"))) {
                        principalAmountText = input
                    }
                },
                label = { Text(stringResource(R.string.loan_amount_label)) },
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        fontWeight = FontWeight.Bold,
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
                    .focusRequester(focusRequester)
            )

            // 2. Category Envelope Selector (Vertical Dropdown Menu)
            CategoryEnvelopeSelector(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it },
                onAddNewCategoryClick = onAddNewCategoryClick
            )

            // 3. Optional Counterparty Name (Creditor or Debtor)
            OutlinedTextField(
                value = counterpartyNameText,
                onValueChange = { counterpartyNameText = it },
                label = {
                    Text(
                        text = if (isIOwe) {
                            stringResource(R.string.lender_creditor_label)
                        } else {
                            stringResource(R.string.debtor_borrower_label)
                        }
                    )
                },
                placeholder = {
                    Text(
                        text = if (isIOwe) {
                            stringResource(R.string.lender_creditor_placeholder)
                        } else {
                            stringResource(R.string.debtor_borrower_placeholder)
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Installment & Repayment Plan (Toggle & Options)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.repayment_plan_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.repayment_plan_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isInstallment,
                    onCheckedChange = { isInstallment = it }
                )
            }

            AnimatedVisibility(
                visible = isInstallment,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val parsedPrincipal = principalAmountText.toDoubleOrNull() ?: 0.0
                    val parsedRate = interestRateText.toDoubleOrNull() ?: 0.0
                    val totalWithInterest = if (parsedRate > 0) parsedPrincipal * (1.0 + (parsedRate / 100.0)) else parsedPrincipal
                    val autoQuota = if (installmentDurationMonths > 0) totalWithInterest / installmentDurationMonths else 0.0

                    // Monthly installment input
                    OutlinedTextField(
                        value = installmentMonthlyText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                installmentMonthlyText = input
                            }
                        },
                        label = { Text(stringResource(R.string.monthly_installment_label)) },
                        placeholder = { Text("%.2f".format(autoQuota)) },
                        prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Duration presets
                    Text(
                        text = stringResource(R.string.installment_duration_months),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf(3, 6, 12, 18, 24, 36, 48)) { months ->
                            FilterChip(
                                selected = installmentDurationMonths == months,
                                onClick = {
                                    installmentDurationMonths = months
                                    if (installmentMonthlyText.isBlank() && autoQuota > 0) {
                                        installmentMonthlyText = ""
                                    }
                                },
                                label = { Text(stringResource(R.string.installment_months_label, months), fontSize = 12.sp) }
                            )
                        }
                    }

                    // Interest Rate (%) input
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                interestRateText = input
                            }
                        },
                        label = { Text(stringResource(R.string.interest_rate_optional_label)) },
                        placeholder = { Text("0.0%") },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (parsedPrincipal > 0) {
                        val activeQuota = installmentMonthlyText.toDoubleOrNull() ?: autoQuota
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.calculated_total_financing,
                                    "$currencySymbol${"%.2f".format(totalWithInterest)}",
                                    "$currencySymbol${"%.2f".format(activeQuota)}",
                                    installmentDurationMonths
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 5. Optional Due Date Row
            val formattedDueDate = remember(dueDateTimestamp) {
                if (dueDateTimestamp != null && dueDateTimestamp > 0) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dueDateTimestamp))
                } else null
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onOpenDatePicker() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Pick Due Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.loan_due_date_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedDueDate ?: stringResource(R.string.no_due_date),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (formattedDueDate != null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.btn_change),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // 6. Notes / Description (Optional)
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(stringResource(R.string.loan_notes_label)) },
                placeholder = { Text(stringResource(R.string.loan_notes_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Save / Update Button
            val parsedAmount = principalAmountText.toDoubleOrNull() ?: 0.0
            val isValid = parsedAmount > 0

            Button(
                onClick = {
                    if (isValid) {
                        val monthlyQuota = if (isInstallment) {
                            installmentMonthlyText.toDoubleOrNull() ?: (parsedAmount / installmentDurationMonths)
                        } else null
                        val parsedRate = interestRateText.toDoubleOrNull() ?: 0.0

                        onSaveLoan(
                            loanType,
                            counterpartyNameText.trim(),
                            parsedAmount,
                            selectedCategoryId.ifEmpty { defaultCategoryId },
                            isInstallment,
                            monthlyQuota,
                            if (isInstallment) installmentDurationMonths else null,
                            parsedRate,
                            dueDateTimestamp,
                            notesText.trim(),
                            editingLoan?.id
                        )
                    }
                },
                enabled = isValid,
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
                    color = Color.White
                )
            }
        }
    }
}
