package com.app.spent.ui.fixedbills.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.ui.theme.ExpenseRed

data class QuickPreset(
    @StringRes val titleResId: Int,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun AddFixedBillFormCard(
    categories: List<CategoryEntity>,
    currencySymbol: String,
    arrivalTimestamp: Long,
    billDueDay: Int,
    onOpenDatePicker: () -> Unit,
    onCloseForm: () -> Unit,
    onSaveBill: (name: String, amount: Double, dueDay: Int, categoryId: String, arrivalTimestamp: Long) -> Unit
) {
    var selectedPresetName by remember { mutableStateOf<String?>(null) }
    var customBillNotesText by remember { mutableStateOf("") }
    var billAmountText by remember { mutableStateOf("") }

    val selectedCategoryId = remember(categories) {
        val utilCat = categories.find { it.id == "cat_utilities" || it.name.contains("Utility", ignoreCase = true) || it.name.contains("Servicio", ignoreCase = true) }
        utilCat?.id ?: categories.firstOrNull()?.id ?: "cat_general"
    }

    val quickPresets = remember {
        listOf(
            QuickPreset(R.string.preset_electricity, Icons.Default.Bolt, Color(0xFFF59E0B)),
            QuickPreset(R.string.preset_water, Icons.Default.WaterDrop, Color(0xFF3B82F6)),
            QuickPreset(R.string.preset_gas, Icons.Default.LocalFireDepartment, Color(0xFFEF4444)),
            QuickPreset(R.string.preset_internet, Icons.Default.Wifi, Color(0xFF8B5CF6)),
            QuickPreset(R.string.preset_phone, Icons.Default.PhoneAndroid, Color(0xFF10B981)),
            QuickPreset(R.string.preset_rent, Icons.Default.Home, Color(0xFF6366F1)),
            QuickPreset(R.string.preset_streaming, Icons.Default.Tv, Color(0xFFEC4899))
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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

            // 1. Quick Presets Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.quick_presets_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickPresets) { preset ->
                        val presetTitle = stringResource(preset.titleResId)
                        val isSelected = selectedPresetName == presetTitle
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPresetName = if (isSelected) null else presetTitle
                            },
                            label = {
                                Text(
                                    text = presetTitle,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = preset.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else preset.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 2. Custom Bill Name or Description
            OutlinedTextField(
                value = customBillNotesText,
                onValueChange = { customBillNotesText = it },
                label = {
                    Text(
                        if (selectedPresetName != null) stringResource(R.string.custom_bill_notes_label)
                        else stringResource(R.string.bill_name_label)
                    )
                },
                placeholder = {
                    Text(if (selectedPresetName != null) "e.g. Account #1234 or Main House" else "e.g. Electricity, Water, Rent")
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Amount Input Field
            OutlinedTextField(
                value = billAmountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^[0-9+×÷\\-\\.\\,\\s]*$"))) {
                        billAmountText = input
                    }
                },
                label = { Text(stringResource(R.string.bill_amount_label)) },
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Arrival Date
            val formattedArrivalDate = remember(arrivalTimestamp) {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(arrivalTimestamp))
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

            // 5. Billing Cadence
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
                        onSaveBill(fullName, parsedAmount, billDueDay, selectedCategoryId, arrivalTimestamp)
                        selectedPresetName = null
                        customBillNotesText = ""
                        billAmountText = ""
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
