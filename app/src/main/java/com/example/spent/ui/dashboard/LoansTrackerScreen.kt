package com.app.spent.ui.dashboard

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansTrackerScreen(
viewModel: LoansTrackerViewModel,
onNavigateBack: () -> Unit
) {
  val state by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  val transactions = state.transactions
  val currencySymbol = state.currencySymbol

  LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
      when (effect) {
        is LoansTrackerUiEffect.ShowSnackbar -> {
          snackbarHostState.showSnackbar(effect.message)
        }
      }
    }
  }

  var isAddingNewDebt by remember { mutableStateOf(false) }
  var loanActionType by remember { mutableStateOf("NEW") } // "NEW" vs "PAY"
  var selectedDebtType by remember { mutableStateOf("CARD") } // "DIRECT", "CARD", "BANK", "INTEREST"

  var lenderNameText by remember { mutableStateOf("") }
  var principalAmountText by remember { mutableStateOf("") }

  // Installment Plan
  var isInstallmentPlan by remember { mutableStateOf(false) }
  var installmentAmountText by remember { mutableStateOf("") }
  var installmentDurationMonths by remember { mutableIntStateOf(12) }

  // Filter debt transactions
  val debtTransactions = transactions.filter { tx ->
    tx.note.contains("Debt (", ignoreCase = true) ||
    tx.note.contains("Loan", ignoreCase = true) ||
    tx.note.contains("Préstamo", ignoreCase = true) ||
    tx.note.contains("Debt Repayment", ignoreCase = true) ||
    tx.note.contains("Debt Installment", ignoreCase = true) ||
    tx.note.contains("Pago Préstamo", ignoreCase = true)
  }

  val totalLoansReceived = transactions
  .filter { it.type == "INCOME" && (it.note.contains("Loan", ignoreCase = true) || it.note.contains("Préstamo", ignoreCase = true) || it.note.contains("Debt (", ignoreCase = true)) }
  .sumOf { it.amount }
  val totalLoansPaid = transactions
  .filter { it.type == "EXPENSE" && (it.note.contains("Loan Payment", ignoreCase = true) || it.note.contains("Pago Préstamo", ignoreCase = true) || it.note.contains("Debt Repayment", ignoreCase = true) || it.note.contains("Debt Installment", ignoreCase = true)) }
  .sumOf { it.amount }
  val netLoanRemaining = (totalLoansReceived - totalLoansPaid).coerceAtLeast(0.0)

  val debtTypeOptions = listOf(
  "DIRECT" to (stringResource(R.string.debt_type_direct) to Icons.Default.Handshake),
  "CARD" to (stringResource(R.string.debt_type_card) to Icons.Default.CreditCard),
  "BANK" to (stringResource(R.string.debt_type_bank) to Icons.Default.AccountBalance),
  "INTEREST" to (stringResource(R.string.debt_type_interest) to Icons.Default.Percent)
  )
  val currentDebtTypeLabel = debtTypeOptions.find { it.first == selectedDebtType }?.second?.first ?: "Debt"

  Scaffold(
  snackbarHost = { SnackbarHost(snackbarHostState) },
  containerColor = MaterialTheme.colorScheme.background,
  topBar = {
    TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    title = {
      Text(
      text = stringResource(R.string.loans_tracker_screen_title),
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
      if (!isAddingNewDebt) {
        IconButton(onClick = { isAddingNewDebt = true }) {
          Icon(
          imageVector = Icons.Default.Add,
          contentDescription = stringResource(R.string.register_loan),
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
      // New Debt / Loan Action Card (When Activated)
      item {
        AnimatedVisibility(visible = isAddingNewDebt) {
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
                text = if (loanActionType == "NEW") stringResource(R.string.register_loan) else stringResource(R.string.record_payment),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
                )
                IconButton(
                onClick = { isAddingNewDebt = false },
                modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Cancel",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              // Action Toggle (Take Loan vs Repay Loan)
              Row(
              modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surface)
              .padding(4.dp),
              horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                Button(
                onClick = { loanActionType = "NEW" },
                colors = ButtonDefaults.buttonColors(
                containerColor = if (loanActionType == "NEW") MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (loanActionType == "NEW") Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
                ) {
                  Text(stringResource(R.string.register_loan), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                onClick = { loanActionType = "PAY" },
                colors = ButtonDefaults.buttonColors(
                containerColor = if (loanActionType == "PAY") ExpenseRed else Color.Transparent,
                contentColor = if (loanActionType == "PAY") Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
                ) {
                  Text(stringResource(R.string.record_payment), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }

              if (loanActionType == "NEW") {
                // Debt Type Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                  text = stringResource(R.string.category_label),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth()
                  ) {
                    items(debtTypeOptions) { (key, data) ->
                      val (title, icon) = data
                      val isSelected = selectedDebtType == key
                      FilterChip(
                      selected = isSelected,
                      onClick = { selectedDebtType = key },
                      label = { Text(title, fontSize = 12.sp) },
                      leadingIcon = {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                      },
                      shape = RoundedCornerShape(12.dp)
                      )
                    }
                  }
                }

                OutlinedTextField(
                value = lenderNameText,
                onValueChange = { lenderNameText = it },
                label = { Text(stringResource(R.string.lender_entity_label)) },
                placeholder = { Text(stringResource(R.string.lender_entity_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
                )

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
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
                )

                // Installment Plan Switch
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
                    text = stringResource(R.string.installment_plan_toggle),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                    )
                    Text(
                    text = "Auto-scheduled as Fixed Bill",
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
                  shape = RoundedCornerShape(14.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface
                  ),
                  modifier = Modifier.fillMaxWidth()
                  )

                  Text(
                  text = stringResource(R.string.installment_duration_months),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold
                  )
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
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
                )

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
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
                )
              }

              val parsedAmount = principalAmountText.toDoubleOrNull() ?: 0.0
              val isValid = parsedAmount > 0 && lenderNameText.isNotBlank()

              Button(
              onClick = {
                if (isValid) {
                  if (loanActionType == "NEW") {
                    val note = "Debt ($currentDebtTypeLabel): ${lenderNameText.trim()}"
                    viewModel.onIntent(LoansTrackerUiIntent.AddDebtLoanTransaction(parsedAmount, "INCOME", note))

                    if (isInstallmentPlan) {
                      val monthlyInstallment = installmentAmountText.toDoubleOrNull() ?: (parsedAmount / installmentDurationMonths)
                      if (monthlyInstallment > 0) {
                        viewModel.onIntent(
                        LoansTrackerUiIntent.AddDebtInstallmentPlan(
                        installmentAmount = monthlyInstallment,
                        durationMonths = installmentDurationMonths,
                        note = "Debt Installment ($currentDebtTypeLabel): ${lenderNameText.trim()}"
                        )
                        )
                      }
                    }
                  } else {
                    val note = "Debt Repayment: ${lenderNameText.trim()}"
                    viewModel.onIntent(LoansTrackerUiIntent.AddDebtLoanTransaction(parsedAmount, "EXPENSE", note))
                  }

                  lenderNameText = ""
                  principalAmountText = ""
                  installmentAmountText = ""
                  isAddingNewDebt = false
                }
              },
              enabled = isValid,
              modifier = Modifier
              .fillMaxWidth()
              .height(48.dp),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
              containerColor = if (loanActionType == "NEW") MaterialTheme.colorScheme.primary else ExpenseRed
              )
              ) {
                Text(
                text = if (loanActionType == "NEW") stringResource(R.string.save_debt) else stringResource(R.string.record_payment),
                fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // Summary Card
      item {
        Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(
          modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text(
            text = stringResource(R.string.outstanding_loan_debt),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
            )

            Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                text = stringResource(R.string.total_income),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                text = "$currencySymbol${"%.2f".format(totalLoansReceived)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IncomeGreen
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                text = stringResource(R.string.total_spent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                text = "$currencySymbol${"%.2f".format(totalLoansPaid)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ExpenseRed
                )
              }
            }

            Box(
            modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
              Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                text = stringResource(R.string.outstanding_loan_debt),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
                )
                Text(
                text = "$currencySymbol${"%.2f".format(netLoanRemaining)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (netLoanRemaining > 0) ExpenseRed else IncomeGreen
                )
              }
            }
          }
        }
      }

      // Loan History Section Header
      item {
        Text(
        text = stringResource(R.string.recent_activity),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
        )
      }

      if (debtTransactions.isEmpty()) {
        item {
          Box(
          modifier = Modifier
          .fillMaxWidth()
          .padding(30.dp),
          contentAlignment = Alignment.Center
          ) {
            Text(
            text = "No debt or loan records registered yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        items(debtTransactions, key = { it.id }) { tx ->
          val isLoanIncome = tx.type == "INCOME"
          val formattedDate = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))

          Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Row(
            modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
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
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isLoanIncome) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
                ) {
                  Icon(
                  imageVector = if (isLoanIncome) Icons.Default.Handshake else Icons.Default.CreditCard,
                  contentDescription = null,
                  tint = if (isLoanIncome) IncomeGreen else ExpenseRed,
                  modifier = Modifier.size(18.dp)
                  )
                }
                Column {
                  Text(
                  text = tx.note.ifBlank { if (isLoanIncome) "Loan Borrowed" else "Loan Repayment" },
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                  )
                  Text(
                  text = formattedDate,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Text(
              text = "${if (isLoanIncome) "+" else "-"}$currencySymbol${"%.2f".format(tx.amount)}",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Bold,
              color = if (isLoanIncome) IncomeGreen else ExpenseRed
              )
            }
          }
        }
      }
    }
  }
}
