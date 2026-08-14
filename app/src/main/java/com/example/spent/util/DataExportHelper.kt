package com.example.spent.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExportHelper {

    /**
     * Exports transactions in a human-friendly format suitable for Microsoft Excel, Google Sheets, or CSV readers.
     * Excludes technical database identifiers (transaction_id, timestamp_iso) and provides clean, structured columns.
     */
    fun exportTransactionsToExcelCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>
    ) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions available to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val categoryMap = categories.associateBy({ it.id }, { it.name })

            // Clean, human-readable column headers (without transaction_id or timestamp_iso)
            val header = "Date,Type,Amount,Category,Note\n"

            val csvContent = buildString {
                // UTF-8 BOM so Excel opens accents and special characters cleanly
                append("\uFEFF")
                append(header)
                transactions.sortedByDescending { it.timestamp }.forEach { tx ->
                    val formattedDate = dateTimeFormat.format(Date(tx.timestamp))
                    val formattedType = tx.type.lowercase().replaceFirstChar { it.uppercase() }
                    val amountStr = String.format(Locale.US, "%.2f", tx.amount)
                    val catName = categoryMap[tx.categoryId] ?: "General"
                    val escapedCat = "\"${catName.replace("\"", "\"\"")}\""
                    val escapedNote = "\"${tx.note.replace("\"", "\"\"")}\""

                    append("$formattedDate,$formattedType,$amountStr,$escapedCat,$escapedNote\n")
                }
            }

            // Save to cache directory for FileProvider sharing
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "Spent_Transactions_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
            val exportFile = File(exportDir, fileName)

            FileOutputStream(exportFile).use { output ->
                output.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, "Exported ${transactions.size} transactions from Spent app.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Download / Export Transactions (Excel & CSV)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
        }
    }
}
