package com.app.spent.ui.dashboard.components.dialogs

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.app.spent.R
import com.app.spent.ui.theme.IncomeGreen
@Composable
fun LoansTrackerDialog(
netLoanRemaining: Double,
totalLoansReceived: Double,
totalLoansPaid: Double,
currencySymbol: String,
onDismiss: () -> Unit,
onAddDebtLoanTransaction: (amount: Double, type: String, note: String) -> Unit,
onAddDebtInstallmentPlan: (installmentAmount: Double, durationMonths: Int, note: String) -> Unit = { _, _, _ -> }
) {
  var loanActionType by remember { mutableStateOf("NEW") } // "NEW" (register debt) vs "PAY" (pay installment)
  var selectedDebtType by remember { mutableStateOf("CARD") } // "DIRECT", "CARD", "BANK", "INTEREST"

  var lenderNameText by remember { mutableStateOf("") }
  var principalAmountText by remember { mutableStateOf("") }
  var interestExtraText by remember { mutableStateOf("") }

  // Installment Plan
  var isInstallmentPlan by remember { mutableStateOf(false) }
  var installmentAmountText by remember { mutableStateOf("") }
  var installmentDurationMonths by remember { mutableStateOf(12) }

  val debtTypeOptions = listOf(
  "DIRECT" to (stringResource(R.string.debt_type_direct) to Icons.Default.Handshake),
  "CARD" to (stringResource(R.string.debt_type_card) to Icons.Default.CreditCard),
  "BANK" to (stringResource(R.string.debt_type_bank) to Icons.Default.AccountBalance),
  "INTEREST" to (stringResource(R.string.debt_type_interest) to Icons.Default.Percent)
  )

  val currentDebtTypeLabel = debtTypeOptions.find { it.first == selectedDebtType }?.second?.first ?: "Debt"

  AlertDialog(
  onDismissRequest = onDismiss,
  icon = {
    Box(
    modifier = Modifier
    .size(48.dp)
    .clip(CircleShape)
    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    contentAlignment = Alignment.Center
    ) {
      Icon(
      imageVector = Icons.Default.AccountBalance,
      contentDescription = stringResource(R.string.loans_tracker_title),
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(26.dp)
      )
    }
  },
  title = { Text(stringResource(R.string.loans_tracker_title), fontWeight = FontWeight.Bold) },
  text = {
    Column {
      // Debt Overview Summary Card
      Box(
      modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .padding(14.dp)
      ) {
        Column {
          Text(
          text = stringResource(R.string.outstanding_loan_debt),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
          text = "$currencySymbol${"%.2f".format(netLoanRemaining)}",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
            text = stringResource(R.string.borrowed_amount, currencySymbol, totalLoansReceived),
            style = MaterialTheme.typography.bodySmall
            )
            Text(
            text = stringResource(R.string.repaid_amount, currencySymbol, totalLoansPaid),
            style = MaterialTheme.typography.bodySmall,
            color = IncomeGreen
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
      ) {
        TextButton(
        onClick = { loanActionType = "NEW" },
        modifier = Modifier.weight(1f)
        ) {
          Text(stringResource(R.string.register_loan), fontWeight = if (loanActionType == "NEW") FontWeight.Bold else FontWeight.Normal)
        }
        TextButton(
        onClick = { loanActionType = "PAY" },
        modifier = Modifier.weight(1f)
        ) {
          Text(stringResource(R.string.pay_installment), fontWeight = if (loanActionType == "PAY") FontWeight.Bold else FontWeight.Normal)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      if (loanActionType == "NEW") {
        // Debt Type Selector Chips
        Text(
        text = "Debt Classification",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(debtTypeOptions) { (key, pair) ->
            val (label, icon) = pair
            FilterChip(
            selected = selectedDebtType == key,
            onClick = { selectedDebtType = key },
            leadingIcon = {
              Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(16.dp))
            },
            label = { Text(label, style = MaterialTheme.typography.bodySmall) }
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
        value = lenderNameText,
        onValueChange = { lenderNameText = it },
        label = { Text(stringResource(R.string.lender_entity_label)) },
        placeholder = { Text(stringResource(R.string.lender_entity_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
        value = principalAmountText,
        onValueChange = { input ->
          if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            principalAmountText = input
          }
        },
        label = { Text(stringResource(R.string.debt_total_capital, currencySymbol)) },
        placeholder = { Text("0.00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Installment Plan Switch
        Row(
        modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
            text = stringResource(R.string.installment_plan_toggle),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
            )
            Text(
            text = "Auto-deducted from Safe Daily Spend",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
          checked = isInstallmentPlan,
          onCheckedChange = { isInstallmentPlan = it }
          )
        }

        if (isInstallmentPlan) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
          value = installmentAmountText,
          onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
              installmentAmountText = input
            }
          },
          label = { Text(stringResource(R.string.installment_monthly_amount, currencySymbol)) },
          placeholder = { Text("50.00") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(6.dp))
          Text(
          text = stringResource(R.string.installment_duration_months),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(4.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(3, 6, 12, 18, 24, 36, 48)) { months ->
              FilterChip(
              selected = installmentDurationMonths == months,
              onClick = { installmentDurationMonths = months },
              label = { Text(stringResource(R.string.installment_months_label, months)) }
              )
            }
          }
        }
      } else {
        // Repayment Form
        OutlinedTextField(
        value = lenderNameText,
        onValueChange = { lenderNameText = it },
        label = { Text(stringResource(R.string.lender_entity_label)) },
        placeholder = { Text("e.g. Visa, Chase, Loan Payment") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
        value = principalAmountText,
        onValueChange = { input ->
          if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            principalAmountText = input
          }
        },
        label = { Text(stringResource(R.string.payment_amount_label, currencySymbol)) },
        placeholder = { Text("0.00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
        )
      }
    }
  },
  confirmButton = {
    val parsedAmount = principalAmountText.toDoubleOrNull() ?: 0.0
    val isValid = parsedAmount > 0 && lenderNameText.isNotBlank()

    Button(
    onClick = {
      if (isValid) {
        if (loanActionType == "NEW") {
          val note = "Debt ($currentDebtTypeLabel): ${lenderNameText.trim()}"
          // Log the loan incoming
          onAddDebtLoanTransaction(parsedAmount, "INCOME", note)

          // If installment plan, schedule recurring rule for fixed bills
          if (isInstallmentPlan) {
            val monthlyInstallment = installmentAmountText.toDoubleOrNull() ?: (parsedAmount / installmentDurationMonths)
            if (monthlyInstallment > 0) {
              onAddDebtInstallmentPlan(
              monthlyInstallment,
              installmentDurationMonths,
              "Debt Installment ($currentDebtTypeLabel): ${lenderNameText.trim()}"
              )
            }
          }
        } else {
          val note = "Debt Repayment: ${lenderNameText.trim()}"
          onAddDebtLoanTransaction(parsedAmount, "EXPENSE", note)
        }
        onDismiss()
      }
    },
    enabled = isValid
    ) {
      Text(if (loanActionType == "NEW") stringResource(R.string.save_debt) else stringResource(R.string.record_payment))
    }
  },
  dismissButton = {
    TextButton(onClick = onDismiss) {
      Text(stringResource(R.string.btn_close))
    }
  }
  )
}
