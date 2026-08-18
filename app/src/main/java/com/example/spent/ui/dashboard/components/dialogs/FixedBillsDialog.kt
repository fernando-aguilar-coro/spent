package com.app.spent.ui.dashboard.components.dialogs

import java.util.Calendar

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen
@Composable
fun FixedBillsDialog(
recurringRules: List<RecurringRuleEntity>,
transactions: List<TransactionEntity>,
categories: List<CategoryEntity>,
currencySymbol: String,
onDismiss: () -> Unit,
onAddBill: (name: String, amount: Double, dueDay: Int, categoryId: String) -> Unit,
onDeleteBill: (ruleId: String) -> Unit,
onPayBill: (amount: Double, name: String, categoryId: String) -> Unit
) {
  var isAddingNewBill by remember { mutableStateOf(false) }

  var billNameText by remember { mutableStateOf("") }
  var billAmountText by remember { mutableStateOf("") }
  var billDueDay by remember { mutableStateOf(10) }
  var selectedCategoryId by remember {
    val utilCat = categories.find { it.id == "cat_utilities" || it.name.contains("Utility", ignoreCase = true) || it.name.contains("Servicio", ignoreCase = true) }
    mutableStateOf(utilCat?.id ?: categories.firstOrNull()?.id ?: "cat_general")
  }

  val quickPresets = listOf(
  stringResource(R.string.preset_electricity),
  stringResource(R.string.preset_water),
  stringResource(R.string.preset_internet),
  stringResource(R.string.preset_gas),
  stringResource(R.string.preset_rent)
  )

  val currentCal = Calendar.getInstance()
  val currentMonth = currentCal.get(Calendar.MONTH)
  val currentYear = currentCal.get(Calendar.YEAR)

  // Filter rules that represent recurring bills
  val billRules = recurringRules.filter {
    it.endDate == null || it.endDate >= System.currentTimeMillis()
  }

  AlertDialog(
  onDismissRequest = onDismiss,
  icon = {
    Box(
    modifier = Modifier
    .size(48.dp)
    .clip(CircleShape)
    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
    contentAlignment = Alignment.Center
    ) {
      Icon(
      imageVector = Icons.Default.Bolt,
      contentDescription = "Bills",
      tint = MaterialTheme.colorScheme.secondary,
      modifier = Modifier.size(26.dp)
      )
    }
  },
  title = {
    Text(
    text = if (isAddingNewBill) stringResource(R.string.add_fixed_bill_title) else stringResource(R.string.fixed_bills_dialog_title),
    fontWeight = FontWeight.Bold
    )
  },
  text = {
    if (!isAddingNewBill) {
      Column {
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (billRules.isEmpty()) {
          Box(
          modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
          contentAlignment = Alignment.Center
          ) {
            Text(
            text = "No fixed bills registered yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(
          modifier = Modifier
          .fillMaxWidth()
          .height(260.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(billRules, key = { it.id }) { rule ->
              val cleanName = rule.note.removePrefix("Bill: ").removePrefix("Factura: ").ifEmpty { "Bill" }

              // Check if paid in this calendar month
              val isPaidThisMonth = transactions.any { tx ->
                tx.type == "EXPENSE" &&
                (tx.recurringRuleId == rule.id || tx.note.contains(cleanName, ignoreCase = true)) &&
                Calendar.getInstance().apply { timeInMillis = tx.timestamp }.get(Calendar.MONTH) == currentMonth &&
                Calendar.getInstance().apply { timeInMillis = tx.timestamp }.get(Calendar.YEAR) == currentYear
              }

              val dueCal = Calendar.getInstance().apply { timeInMillis = rule.startDate }
              val dueDay = dueCal.get(Calendar.DAY_OF_MONTH)

              Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
              ) {
                Row(
                modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                    text = cleanName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                    )
                    Text(
                    text = "$currencySymbol${"%.2f".format(rule.amount)} • ${stringResource(R.string.bill_due_day_format, dueDay)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                      imageVector = if (isPaidThisMonth) Icons.Default.CheckCircle else Icons.Default.Schedule,
                      contentDescription = null,
                      tint = if (isPaidThisMonth) IncomeGreen else MaterialTheme.colorScheme.tertiary,
                      modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                      text = if (isPaidThisMonth) stringResource(R.string.bill_status_paid) else stringResource(R.string.bill_status_pending),
                      style = MaterialTheme.typography.labelSmall,
                      color = if (isPaidThisMonth) IncomeGreen else MaterialTheme.colorScheme.tertiary
                      )
                    }
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isPaidThisMonth) {
                      TextButton(
                      onClick = {
                        onPayBill(rule.amount, cleanName, rule.categoryId)
                      }
                      ) {
                        Text(stringResource(R.string.pay_bill_btn), style = MaterialTheme.typography.labelMedium)
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
                }
              }
            }
          }
        }
      }
    } else {
      // Add Bill Form
      Column {
        Text(
        text = "Quick Presets",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(quickPresets) { preset ->
            FilterChip(
            selected = billNameText.equals(preset, ignoreCase = true),
            onClick = { billNameText = preset },
            label = { Text(preset, style = MaterialTheme.typography.bodySmall) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
        value = billNameText,
        onValueChange = { billNameText = it },
        label = { Text(stringResource(R.string.bill_name_label)) },
        placeholder = { Text(stringResource(R.string.bill_name_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

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
        modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
        text = stringResource(R.string.bill_due_day_label),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf(1, 5, 10, 15, 20, 25, 28, 30)) { day ->
            FilterChip(
            selected = billDueDay == day,
            onClick = { billDueDay = day },
            label = { Text("Day $day") }
            )
          }
        }
      }
    }
  },
  confirmButton = {
    if (!isAddingNewBill) {
      Button(onClick = { isAddingNewBill = true }) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(stringResource(R.string.btn_new))
      }
    } else {
      val parsedAmount = billAmountText.toDoubleOrNull() ?: 0.0
      Button(
      onClick = {
        if (billNameText.isNotBlank() && parsedAmount > 0) {
          onAddBill(billNameText.trim(), parsedAmount, billDueDay, selectedCategoryId)
          billNameText = ""
          billAmountText = ""
          isAddingNewBill = false
        }
      },
      enabled = billNameText.isNotBlank() && parsedAmount > 0
      ) {
        Text(stringResource(R.string.save_bill_btn))
      }
    }
  },
  dismissButton = {
    if (isAddingNewBill) {
      TextButton(onClick = { isAddingNewBill = false }) {
        Text(stringResource(R.string.btn_cancel))
      }
    } else {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.btn_close))
      }
    }
  }
  )
}
