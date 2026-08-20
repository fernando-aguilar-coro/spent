package com.app.spent.ui.savings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.ui.theme.IncomeGreen

@Composable
fun SavingsGoalFormCard(
    hasActiveGoal: Boolean,
    initialName: String,
    initialTotal: Double,
    initialMonthly: Double,
    currencySymbol: String,
    onCancelEdit: () -> Unit,
    onSaveGoal: (name: String, totalGoal: Double, monthlyContribution: Double) -> Unit
) {
    var goalNameText by remember(initialName) { mutableStateOf(initialName) }
    var totalGoalText by remember(initialTotal) {
        mutableStateOf(if (initialTotal > 0) initialTotal.toString() else "")
    }
    var monthlyContributionText by remember(initialMonthly) {
        mutableStateOf(if (initialMonthly > 0) initialMonthly.toString() else "")
    }

    val parsedTotalGoal = totalGoalText.toDoubleOrNull() ?: 0.0
    val parsedMonthly = monthlyContributionText.toDoubleOrNull() ?: 0.0
    val isGoalValid = parsedTotalGoal > 0 && parsedMonthly > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (hasActiveGoal) {
                            stringResource(R.string.edit_target_goal)
                        } else {
                            stringResource(R.string.add_savings_goal_btn)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (hasActiveGoal) {
                    IconButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }
            }

            OutlinedTextField(
                value = goalNameText,
                onValueChange = { goalNameText = it },
                label = { Text(stringResource(R.string.note_goal_name_label)) },
                placeholder = { Text("e.g. Emergency Fund, New Car") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = totalGoalText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        totalGoalText = input
                    }
                },
                label = { Text(stringResource(R.string.goal_target_label, currencySymbol)) },
                placeholder = { Text("5000.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = monthlyContributionText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        monthlyContributionText = input
                    }
                },
                label = { Text(stringResource(R.string.monthly_savings_contribution_label, currencySymbol)) },
                placeholder = { Text("200.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(10.dp)
            ) {
                Text(
                    text = "💡 " + stringResource(R.string.savings_info_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    if (isGoalValid) {
                        val name = goalNameText.trim().ifEmpty { "Savings Goal" }
                        onSaveGoal(name, parsedTotalGoal, parsedMonthly)
                    }
                },
                enabled = isGoalValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.save_goal), fontWeight = FontWeight.Bold)
            }
        }
    }
}
