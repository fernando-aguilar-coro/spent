package com.app.spent.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.util.DataExportHelper
@Composable
fun ExportCsvCard(
transactions: List<TransactionEntity>,
categories: List<CategoryEntity>
) {
  val context = LocalContext.current

  Card(
  modifier = Modifier
  .fillMaxWidth()
  .clickable { DataExportHelper.exportTransactionsToExcel(context, transactions, categories) },
  shape = RoundedCornerShape(16.dp),
  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Row(
    modifier = Modifier.padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
      imageVector = Icons.Default.Description,
      contentDescription = "Export Excel (.xlsx)",
      tint = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
        text = stringResource(R.string.tool_export_excel_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
        )
        Text(
        text = stringResource(R.string.tool_export_excel_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
