package com.example.spent.ui.dashboard.components.dialogs

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.spent.R
import com.example.spent.ui.theme.IncomeGreen

@Composable
fun SavingsTrackerDialog(
    totalSaved: Double,
    savingsGoal: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onDeposit: (amount: Double, note: String) -> Unit,
    onUpdateGoal: (goal: Double) -> Unit
) {
    val defaultNote = stringResource(R.string.deposit_into_savings)
    var depositAmountText by remember { mutableStateOf("") }
    var depositNoteText by remember { mutableStateOf(defaultNote) }
    var isEditingGoal by remember { mutableStateOf(false) }
    var newGoalText by remember { mutableStateOf(if (savingsGoal > 0) "%.2f".format(savingsGoal) else "") }

    val savingsProgress = if (savingsGoal > 0) (totalSaved / savingsGoal).toFloat().coerceIn(0f, 1f) else 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(IncomeGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = stringResource(R.string.savings_tracker_title),
                    tint = IncomeGreen,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = { Text(stringResource(R.string.savings_tracker_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // Overview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.total_saved),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currencySymbol${"%.2f".format(totalSaved)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.savings_goal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currencySymbol${"%.2f".format(savingsGoal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { savingsProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = IncomeGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.savings_progress_text, (savingsProgress * 100).toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isEditingGoal) {
                    Text(
                        text = stringResource(R.string.add_funds_to_savings),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = depositAmountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                depositAmountText = input
                            }
                        },
                        label = { Text(stringResource(R.string.deposit_amount_label, currencySymbol)) },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = depositNoteText,
                        onValueChange = { depositNoteText = it },
                        label = { Text(stringResource(R.string.note_goal_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { isEditingGoal = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.edit_target_goal))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.update_target_savings_goal),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newGoalText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                newGoalText = input
                            }
                        },
                        label = { Text(stringResource(R.string.goal_target_label, currencySymbol)) },
                        placeholder = { Text("1000.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { isEditingGoal = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.back_to_deposit))
                    }
                }
            }
        },
        confirmButton = {
            if (!isEditingGoal) {
                val parsed = depositAmountText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (parsed > 0) {
                            onDeposit(parsed, depositNoteText.ifBlank { defaultNote })
                            onDismiss()
                        }
                    },
                    enabled = parsed > 0
                ) {
                    Text(stringResource(R.string.deposit_funds))
                }
            } else {
                val parsedGoal = newGoalText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (parsedGoal >= 0) {
                            onUpdateGoal(parsedGoal)
                            isEditingGoal = false
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(R.string.save_goal))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
