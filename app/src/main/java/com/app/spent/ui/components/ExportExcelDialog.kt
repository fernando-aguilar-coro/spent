package com.app.spent.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    val exportLabels = DataExportHelper.ExportLabels(
        income = stringResource(R.string.export_label_income),
        expenses = stringResource(R.string.export_label_expenses),
        total = stringResource(R.string.export_label_total),
        portfolio = stringResource(R.string.export_label_portfolio),
        portfolioTotal = stringResource(R.string.export_label_portfolio_total)
    )
    val copiedToastMessage = stringResource(R.string.export_copied_toast)

    var dateFrom by remember { mutableStateOf<Long?>(null) }
    var dateTo by remember { mutableStateOf<Long?>(null) }
    var detailed by remember { mutableStateOf(false) }
    var includeHoldings by remember { mutableStateOf(true) }

    val filteredTransactions = remember(transactions, dateFrom, dateTo) {
        val endOfToDay = dateTo?.let {
            java.util.Calendar.getInstance().apply {
                timeInMillis = it
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                add(java.util.Calendar.MILLISECOND, -1)
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
                ExportSummaryText(
                    filteredCount = filteredTransactions.size,
                    totalCount = transactions.size,
                    holdingsCount = effectiveHoldings.size
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
                ExportDetailLevelChips(
                    detailed = detailed,
                    onSimpleClick = { detailed = false },
                    onDetailedClick = { detailed = true }
                )

                // Include portfolio snapshot
                ExportHoldingsToggle(
                    holdingsCount = holdings.size,
                    includeHoldings = includeHoldings,
                    onToggle = { includeHoldings = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 1: Copy to Clipboard (messaging apps)
                ExportOptionCard(
                    icon = Icons.Default.ContentCopy,
                    titleRes = R.string.export_copy_clipboard_title,
                    descRes = R.string.export_copy_clipboard_desc,
                    iconBackgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    iconTintColor = MaterialTheme.colorScheme.secondary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    onClick = {
                        val text = DataExportHelper.buildWhatsAppText(
                            transactions = filteredTransactions,
                            categories = categories,
                            holdings = effectiveHoldings,
                            currencySymbol = currencySymbol,
                            currencyCode = currencyCode,
                            detailed = detailed,
                            labels = exportLabels
                        )
                        if (text.isBlank()) {
                            android.widget.Toast.makeText(context, "Nothing to copy", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            DataExportHelper.copyTextToClipboard(context, text, copiedToastMessage)
                            onDismiss()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Save As to Device Storage
                ExportOptionCard(
                    icon = Icons.Default.Download,
                    titleRes = R.string.export_save_device_title,
                    descRes = R.string.export_save_device_desc,
                    iconBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    iconTintColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    onClick = { createDocumentLauncher.launch(defaultFileName) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 3: Share via Apps
                ExportOptionCard(
                    icon = Icons.Default.Share,
                    titleRes = R.string.export_share_apps_title,
                    descRes = R.string.export_share_apps_desc,
                    iconBackgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    iconTintColor = MaterialTheme.colorScheme.tertiary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    onClick = {
                        DataExportHelper.shareExcelFile(context, filteredTransactions, categories, effectiveHoldings)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
