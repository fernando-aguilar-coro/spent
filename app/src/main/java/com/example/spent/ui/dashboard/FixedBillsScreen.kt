package com.example.spent.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spent.R
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.RecurringRuleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.theme.ExpenseRed
import com.example.spent.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class QuickPreset(
    val titleResId: Int,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedBillsScreen(
    recurringRules: List<RecurringRuleEntity>,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onNavigateBack: () -> Unit,
    onAddBill: (name: String, amount: Double, dueDay: Int, categoryId: String, arrivalTimestamp: Long) -> Unit,
    onDeleteBill: (ruleId: String) -> Unit,
    onPayBill: (amount: Double, name: String, categoryId: String, ruleId: String) -> Unit
) {
    val context = LocalContext.current
    var isAddingNewBill by remember { mutableStateOf(false) }

    // Form states
    var selectedPresetName by remember { mutableStateOf<String?>(null) }
    var customBillNotesText by remember { mutableStateOf("") }
    var billAmountText by remember { mutableStateOf("") }
    var arrivalTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var billDueDay by remember {
        val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        mutableIntStateOf(todayDay)
    }

    var selectedCategoryId by remember {
        val utilCat = categories.find { it.id == "cat_utilities" || it.name.contains("Utility", ignoreCase = true) || it.name.contains("Servicio", ignoreCase = true) }
        mutableStateOf(utilCat?.id ?: categories.firstOrNull()?.id ?: "cat_general")
    }

    val quickPresets = remember {
        listOf(
            QuickPreset(R.string.preset_electricity, Icons.Default.Bolt, Color(0xFFF59E0B)),
            QuickPreset(R.string.preset_water, Icons.Default.WaterDrop, Color(0xFF0284C7)),
            QuickPreset(R.string.preset_internet, Icons.Default.Wifi, Color(0xFF8B5CF6)),
            QuickPreset(R.string.preset_gas, Icons.Default.LocalFireDepartment, Color(0xFFEA580C)),
            QuickPreset(R.string.preset_rent, Icons.Default.Home, Color(0xFF10B981)),
            QuickPreset(R.string.preset_phone, Icons.Default.PhoneAndroid, Color(0xFF06B6D4)),
            QuickPreset(R.string.preset_streaming, Icons.Default.Tv, Color(0xFFEC4899))
        )
    }

    val currentCal = Calendar.getInstance()
    val currentMonth = currentCal.get(Calendar.MONTH)
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentMillis = System.currentTimeMillis()

    // Filter active bills
    val billRules = recurringRules.filter {
        it.endDate == null || it.endDate >= currentMillis
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        text = stringResource(R.string.fixed_bills_screen_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isAddingNewBill) {
                        IconButton(onClick = { isAddingNewBill = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_fixed_bill_title),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // New Bill Form (when activated)
            item {
                AnimatedVisibility(visible = isAddingNewBill) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.add_fixed_bill_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { isAddingNewBill = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                                }
                            }

                            // 1. Highlighted Quick Presets Section
                            Text(
                                text = stringResource(R.string.quick_presets_title),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(quickPresets) { preset ->
                                    val label = stringResource(preset.titleResId)
                                    val isSelected = selectedPresetName == label

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else preset.color.copy(alpha = 0.15f)
                                            )
                                            .clickable {
                                                selectedPresetName = label
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = preset.icon,
                                                contentDescription = label,
                                                tint = if (isSelected) Color.White else preset.color,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Custom Notes / Provider details (optional)
                            OutlinedTextField(
                                value = customBillNotesText,
                                onValueChange = { customBillNotesText = it },
                                label = { Text(stringResource(R.string.custom_bill_notes_label)) },
                                placeholder = { Text(stringResource(R.string.custom_bill_notes_placeholder)) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 3. Amount Field
                            OutlinedTextField(
                                value = billAmountText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        billAmountText = input
                                    }
                                },
                                label = { Text(stringResource(R.string.bill_amount_label, currencySymbol)) },
                                placeholder = { Text("50.00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 4. Arrival Date (Defaults to Today)
                            val formattedArrivalDate = remember(arrivalTimestamp) {
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(arrivalTimestamp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        showDatePicker = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Pick Date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.arrival_date_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formattedArrivalDate,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
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

                            // 5. Billing Cadence (30 days by default)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.cycle_period_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.period_30_days),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.bill_due_day_format, billDueDay),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Save Bill Button
                            val parsedAmount = billAmountText.toDoubleOrNull() ?: 0.0
                            val finalBillName = selectedPresetName ?: customBillNotesText.trim()
                            val isValid = finalBillName.isNotBlank() && parsedAmount > 0

                            Button(
                                onClick = {
                                    if (isValid) {
                                        val fullName = if (selectedPresetName != null && customBillNotesText.isNotBlank()) {
                                            "$selectedPresetName (${customBillNotesText.trim()})"
                                        } else {
                                            finalBillName
                                        }
                                        onAddBill(fullName, parsedAmount, billDueDay, selectedCategoryId, arrivalTimestamp)
                                        selectedPresetName = null
                                        customBillNotesText = ""
                                        billAmountText = ""
                                        isAddingNewBill = false
                                    }
                                },
                                enabled = isValid,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(stringResource(R.string.save_bill_btn), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Overview & Description Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.tool_fixed_bills_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${billRules.size} ${if (billRules.size == 1) "Service" else "Services"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // List of registered fixed bills with Overdue & Cut-Off Warning calculation
            if (billRules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No fixed bills registered yet. Tap + to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(billRules, key = { it.id }) { rule ->
                    val cleanName = rule.note.removePrefix("Bill: ").removePrefix("Factura: ").ifEmpty { "Bill" }

                    // Check how many 30-day periods elapsed since startDate
                    val daysSinceStart = ((currentMillis - rule.startDate) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)
                    val elapsedCycles = (daysSinceStart / 30).toInt() + 1

                    // Check payments recorded
                    val paidCyclesCount = transactions.count { tx ->
                        tx.type == "EXPENSE" &&
                        (tx.recurringRuleId == rule.id || tx.note.contains(cleanName, ignoreCase = true))
                    }

                    val isPaidThisCurrentMonth = transactions.any { tx ->
                        tx.type == "EXPENSE" &&
                        (tx.recurringRuleId == rule.id || tx.note.contains(cleanName, ignoreCase = true)) &&
                        Calendar.getInstance().apply { timeInMillis = tx.timestamp }.get(Calendar.MONTH) == currentMonth &&
                        Calendar.getInstance().apply { timeInMillis = tx.timestamp }.get(Calendar.YEAR) == currentYear
                    }

                    val unpaidCycles = if (isPaidThisCurrentMonth) 0 else (elapsedCycles - paidCyclesCount).coerceAtLeast(1)
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
                                        Text(
                                            text = stringResource(R.string.arrival_date_format, formattedArrival),
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
                                    Text(
                                        text = if (unpaidCycles > 1) stringResource(R.string.accumulated_total_label) else stringResource(R.string.bill_due_day_format, dueDay),
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
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = arrivalTimestamp
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = arrivalTimestamp
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            arrivalTimestamp = cal.timeInMillis
                            billDueDay = utcCal.get(Calendar.DAY_OF_MONTH)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
