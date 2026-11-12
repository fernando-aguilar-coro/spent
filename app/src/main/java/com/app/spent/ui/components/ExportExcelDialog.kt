package com.app.spent.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.util.DataExportHelper

@Composable
fun ExportExcelDialog(
  transactions: List<TransactionEntity>,
  categories: List<CategoryEntity>,
  holdings: List<AssetHoldingEntity> = emptyList(),
  currencySymbol: String = "",
  currencyCode: String = "",
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val defaultFileName = remember { DataExportHelper.getDefaultXlsxFileName() }

  var dateFrom by remember { mutableStateOf<Long?>(null) }
  var dateTo by remember { mutableStateOf<Long?>(null) }
  var detailed by remember { mutableStateOf(false) }
  var includeHoldings by remember { mutableStateOf(true) }

  val filteredTransactions = remember(transactions, dateFrom, dateTo) {
    val endOfToDay = dateTo?.let {
      Calendar.getInstance().apply {
        timeInMillis = it
        add(Calendar.DAY_OF_YEAR, 1)
        add(Calendar.MILLISECOND, -1)
      }.timeInMillis
    }
    DataExportHelper.filterByDateRange(transactions, dateFrom, endOfToDay)
  }
  val effectiveHoldings = if (includeHoldings) holdings else emptyList()

  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  ) { uri ->
    if (uri != null) {
      DataExportHelper.writeExcelToUri(context, uri, filteredTransactions, categories, effectiveHoldings)
      onDismiss()
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Description,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp)
        )
      }
    },
    title = {
      Text(
        text = stringResource(R.string.tool_export_excel_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Export ${filteredTransactions.size} of ${transactions.size} records" +
            if (effectiveHoldings.isNotEmpty()) " + ${effectiveHoldings.size} holdings" else "",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Date range filters
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ExportDateSelector(
            label = stringResource(R.string.export_date_from),
            selectedMillis = dateFrom,
            onSelected = { dateFrom = it },
            onCleared = { dateFrom = null },
            modifier = Modifier.weight(1f)
          )
          ExportDateSelector(
            label = stringResource(R.string.export_date_to),
            selectedMillis = dateTo,
            onSelected = { dateTo = it },
            onCleared = { dateTo = null },
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Detail level
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = !detailed,
            onClick = { detailed = false },
            label = { Text(stringResource(R.string.export_detail_simple)) }
          )
          FilterChip(
            selected = detailed,
            onClick = { detailed = true },
            label = { Text(stringResource(R.string.export_detail_detailed)) }
          )
        }

        // Include portfolio snapshot
        if (holdings.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { includeHoldings = !includeHoldings }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = includeHoldings,
              onCheckedChange = { includeHoldings = it }
            )
            Text(
              text = stringResource(R.string.export_include_holdings, holdings.size),
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Option 1: Copy to Clipboard (messaging apps)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              val text = DataExportHelper.buildWhatsAppText(
                transactions = filteredTransactions,
                categories = categories,
                holdings = effectiveHoldings,
                currencySymbol = currencySymbol,
                currencyCode = currencyCode,
                detailed = detailed,
                labels = DataExportHelper.ExportLabels(
                  income = stringResource(R.string.export_label_income),
                  expenses = stringResource(R.string.export_label_expenses),
                  total = stringResource(R.string.export_label_total),
                  portfolio = stringResource(R.string.export_label_portfolio),
                  portfolioTotal = stringResource(R.string.export_label_portfolio_total)
                )
              )
              if (text.isBlank()) {
                android.widget.Toast.makeText(context, "Nothing to copy", android.widget.Toast.LENGTH_SHORT).show()
              } else {
                DataExportHelper.copyTextToClipboard(context, text, stringResource(R.string.export_copied_toast))
                onDismiss()
              }
            },
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.export_copy_clipboard_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = stringResource(R.string.export_copy_clipboard_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Option 2: Save As to Device Storage
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { createDocumentLauncher.launch(defaultFileName) },
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Save to Device (Save As...)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Choose folder on device storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Option 3: Share via Apps
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              DataExportHelper.shareExcelFile(context, filteredTransactions, categories, effectiveHoldings)
              onDismiss()
            },
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Share with Apps",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Send via WhatsApp, Drive, Gmail, etc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.btn_close))
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDateSelector(
  label: String,
  selectedMillis: Long?,
  onSelected: (Long?) -> Unit,
  onCleared: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showPicker by remember { mutableStateOf(false) }
  val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

  Card(
    modifier = modifier
      .clickable { showPicker = true },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = selectedMillis?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.export_all_dates),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
        if (selectedMillis != null) {
          TextButton(onClick = onCleared) {
            Text(stringResource(R.string.export_clear), fontSize = 12.sp)
          }
        }
      }
    }
  }

  if (showPicker) {
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = selectedMillis ?: System.currentTimeMillis()
    )
    DatePickerDialog(
      onDismissRequest = { showPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { utcMillis ->
              val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = utcMillis
              }
              val localCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
              }
              onSelected(localCal.timeInMillis)
            }
            showPicker = false
          }
        ) {
          Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPicker = false }) {
          Text(stringResource(R.string.btn_cancel))
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }
}
