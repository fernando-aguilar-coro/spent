package com.example.spent.ui.settings.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportCsvCard(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { exportTransactionsToCsv(context, transactions, categories) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = "Export CSV",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Export Data (CSV)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Share or save transactions to CSV file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun exportTransactionsToCsv(
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

    val shareIntent = Intent.createChooser(sendIntent, "Export Spent CSV Data")
    context.startActivity(shareIntent)
}
