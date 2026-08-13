package com.example.spent.ui.dashboard.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardQuickTools(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onAddDebtLoanTransaction: (amount: Double, type: String, note: String) -> Unit
) {
    val context = LocalContext.current
    var showDebtDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "Extra Tools & Shortcuts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Borrowed / Debt Tool
            item {
                Card(
                    modifier = Modifier
                        .width(170.dp)
                        .height(100.dp)
                        .clickable { showDebtDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = "Lent / Debt",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Lent / Debt",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Track money owed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2. Recurring Payments Tool
            item {
                Card(
                    modifier = Modifier
                        .width(170.dp)
                        .height(100.dp)
                        .clickable {
                            val activeCount = transactions.count { it.recurringRuleId != null }
                            android.widget.Toast.makeText(
                                context,
                                "Active Recurring Rules: $activeCount transactions auto-logged",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Recurring",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Recurring Payments",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "View auto-schedules",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 3. Export to Excel / CSV Tool
            item {
                Card(
                    modifier = Modifier
                        .width(170.dp)
                        .height(100.dp)
                        .clickable { exportToExcelCsv(context, transactions, categories) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Excel Export",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Export Excel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Download .csv / excel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for Lent / Debt Entry
    if (showDebtDialog) {
        var debtPersonName by remember { mutableStateOf("") }
        var debtAmountText by remember { mutableStateOf("") }
        var debtType by remember { mutableStateOf("LENT") } // LENT (i gave money) vs OWED (i owe money)

        AlertDialog(
            onDismissRequest = { showDebtDialog = false },
            title = { Text("Log Lent / Debt Record", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { debtType = "LENT" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("I Lent Money", fontWeight = if (debtType == "LENT") FontWeight.Bold else FontWeight.Normal)
                        }
                        TextButton(
                            onClick = { debtType = "OWED" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("I Owe Money", fontWeight = if (debtType == "OWED") FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = debtPersonName,
                        onValueChange = { debtPersonName = it },
                        label = { Text("Person Name") },
                        placeholder = { Text("e.g. John, Maria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = debtAmountText,
                        onValueChange = { debtAmountText = it },
                        label = { Text("Amount") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val parsedAmount = debtAmountText.toDoubleOrNull() ?: 0.0
                TextButton(
                    onClick = {
                        if (parsedAmount > 0 && debtPersonName.isNotBlank()) {
                            val note = if (debtType == "LENT") "Lent to $debtPersonName" else "Owed to $debtPersonName"
                            val type = if (debtType == "LENT") "EXPENSE" else "INCOME"
                            onAddDebtLoanTransaction(parsedAmount, type, note)
                            showDebtDialog = false
                        }
                    },
                    enabled = parsedAmount > 0 && debtPersonName.isNotBlank()
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDebtDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun exportToExcelCsv(
    context: Context,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>
) {
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val header = "transaction_id,timestamp_iso,date,type,amount,category_name,note\n"
    val csvBody = transactions.joinToString("\n") { tx ->
        val catName = categories.find { it.id == tx.categoryId }?.name ?: "General"
        val isoDate = isoFormat.format(Date(tx.timestamp))
        val dateOnly = dateFormat.format(Date(tx.timestamp))
        val escapedNote = "\"${tx.note.replace("\"", "\"\"")}\""
        "${tx.id},$isoDate,$dateOnly,${tx.type},${tx.amount},$catName,$escapedNote"
    }

    val fullCsv = header + csvBody

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Spent_Export_${System.currentTimeMillis()}.csv")
        putExtra(Intent.EXTRA_TEXT, fullCsv)
    }

    val shareIntent = Intent.createChooser(sendIntent, "Export Spent Transactions (Excel/CSV)")
    context.startActivity(shareIntent)
}
