package com.app.spent.util

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.TransactionEntity

object DataExportHelper {

  data class ExportLabels(
    val income: String = "Income",
    val expenses: String = "Expenses",
    val total: String = "Total",
    val portfolio: String = "Portfolio",
    val portfolioTotal: String = "Portfolio total"
  )

  /**
   * Generates a standard default filename for transaction exports.
   */
  fun getDefaultXlsxFileName(): String {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return "Spent_Transactions_$timeStamp.xlsx"
  }

  /**
   * Exports transactions directly to a selected Content URI (e.g. from ACTION_CREATE_DOCUMENT / Storage Access Framework).
   */
  fun filterByDateRange(
    transactions: List<TransactionEntity>,
    fromMillis: Long?,
    toMillis: Long?
  ): List<TransactionEntity> {
    return transactions.filter { tx ->
      (fromMillis == null || tx.timestamp >= fromMillis) &&
        (toMillis == null || tx.timestamp <= toMillis)
    }
  }

  /**
   * Builds a plain-text, messaging-app friendly summary (e.g. WhatsApp).
   * Transactions are grouped by month; each row keeps few columns:
   * dd/MM  ±amount  Category  (detailed mode adds time, type and note).
   */
  fun buildWhatsAppText(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    holdings: List<AssetHoldingEntity> = emptyList(),
    currencySymbol: String = "",
    currencyCode: String = "",
    detailed: Boolean = false,
    labels: ExportLabels = ExportLabels()
  ): String {
    val categoryMap = categories.associateBy({ it.id }, { it.name })
    val dayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val sorted = transactions.sortedBy { it.timestamp }

    val sb = StringBuilder()
    var currentMonthKey: String? = null
    var totalIncome = 0.0
    var totalExpense = 0.0

    sorted.forEach { tx ->
      val date = Date(tx.timestamp)
      val monthKey = monthFormat.format(date)
      if (monthKey != currentMonthKey) {
        if (currentMonthKey != null) sb.append('\n')
        sb.append('*').append(monthKey.replaceFirstChar { it.uppercase() }).append("*\n")
        currentMonthKey = monthKey
      }

      val isIncome = tx.type.equals("INCOME", ignoreCase = true)
      val signed = if (isIncome) tx.amount else -tx.amount
      if (isIncome) totalIncome += tx.amount else totalExpense += tx.amount

      val amountStr = formatSignedAmount(signed, currencySymbol)
      val categoryName = categoryMap[tx.categoryId] ?: "General"
      val row = if (detailed) {
        val typeStr = tx.type.lowercase().replaceFirstChar { it.uppercase() }
        val note = tx.note.trim()
        buildString {
          append(dayFormat.format(date))
          append(' ').append(timeFormat.format(date))
          append("  ").append(amountStr)
          append("  ").append(typeStr)
          append("  ").append(categoryName)
          if (note.isNotEmpty()) append(" — ").append(note)
        }
      } else {
        "${dayFormat.format(date)}  $amountStr  $categoryName"
      }
      sb.append(row).append('\n')
    }

    if (sorted.isNotEmpty()) {
      sb.append("------------------------\n")
      sb.append("${labels.income} : ").append(formatSignedAmount(totalIncome, currencySymbol)).append('\n')
      sb.append("${labels.expenses} : ").append(formatSignedAmount(-totalExpense, currencySymbol)).append('\n')
      sb.append("${labels.total} : ").append(
        formatSignedAmount(totalIncome - totalExpense, currencySymbol)
      ).append('\n')
    }

    if (holdings.isNotEmpty()) {
      sb.append('\n').append('*').append(labels.portfolio).append("*\n")
      var portfolioTotal = 0.0
      holdings.forEach { holding ->
        val unitPrice = holding.manualPriceOverride ?: holding.averageBuyPrice
        val line = if (unitPrice != null && unitPrice > 0.0) {
          val total = holding.quantity * unitPrice
          portfolioTotal += total
          "${holding.symbol} · ${holding.name} — ${formatQty(holding.quantity)} × ${formatPlain(unitPrice, currencySymbol)} = ${formatPlain(total, currencySymbol)} $currencyCode"
        } else {
          "${holding.symbol} · ${holding.name} — ${formatQty(holding.quantity)} units"
        }
        sb.append(line).append('\n')
      }
      if (portfolioTotal > 0.0) {
        sb.append("${labels.portfolioTotal} : ${formatPlain(portfolioTotal, currencySymbol)} $currencyCode\n")
      }
    }

    return sb.toString().trimEnd()
  }

  /**
   * Copies [text] to the system clipboard and confirms with a toast.
   */
  fun copyTextToClipboard(context: Context, text: String, toastMessage: String = "Copied to clipboard") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Spent export", text))
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
  }

  private fun formatSignedAmount(value: Double, currencySymbol: String): String {
    val sign = if (value < 0) "-" else "+"
    val abs = kotlin.math.abs(value)
    return "$sign$currencySymbol${String.format(Locale.US, "%.2f", abs)}"
  }

  private fun formatPlain(value: Double, currencySymbol: String): String {
    return "$currencySymbol${String.format(Locale.US, "%.2f", value)}"
  }

  private fun formatQty(value: Double): String {
    return if (value == value.toLong().toDouble()) {
      value.toLong().toString()
    } else {
      String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
    }
  }

  fun writeExcelToUri(
    context: Context,
    uri: Uri,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    holdings: List<AssetHoldingEntity> = emptyList()
  ): Boolean {
    return try {
      context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        generateXlsxToStream(outputStream, transactions, categories, holdings)
      }
      Toast.makeText(context, "Spreadsheet saved successfully", Toast.LENGTH_SHORT).show()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "Save error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
      false
    }
  }

  /**
   * Shares the spreadsheet with other apps via Android Sharesheet (ACTION_SEND).
   */
  fun shareExcelFile(
    context: Context,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    holdings: List<AssetHoldingEntity> = emptyList()
  ) {
    if (transactions.isEmpty() && holdings.isEmpty()) {
      Toast.makeText(context, "No data available to export", Toast.LENGTH_SHORT).show()
      return
    }

    try {
      val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val xlsxFileName = getDefaultXlsxFileName()
      val xlsxFile = File(exportDir, xlsxFileName)

      FileOutputStream(xlsxFile).use { outputStream ->
        generateXlsxToStream(outputStream, transactions, categories, holdings)
      }

      val fileUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        xlsxFile
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_SUBJECT, xlsxFileName)
        putExtra(Intent.EXTRA_STREAM, fileUri)
        putExtra(Intent.EXTRA_TEXT, "Exported ${transactions.size} transactions from Spent app (.xlsx spreadsheet).")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(shareIntent, "Share Transactions (.xlsx)")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "Export error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
    }
  }

  /**
   * Legacy method for direct export (delegates to shareExcelFile).
   */
  fun exportTransactionsToExcel(
    context: Context,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    holdings: List<AssetHoldingEntity> = emptyList()
  ) {
    shareExcelFile(context, transactions, categories, holdings)
  }

  /**
   * Generates genuine OpenXML (.xlsx) file structure and streams it to the given OutputStream.
   */
  fun generateXlsxToStream(
    outputStream: OutputStream,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    holdings: List<AssetHoldingEntity> = emptyList()
  ) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val categoryMap = categories.associateBy({ it.id }, { it.name })
    val sortedTransactions = transactions.sortedByDescending { it.timestamp }

    ZipOutputStream(outputStream).use { zip ->
      // 1. [Content_Types].xml
      zip.putNextEntry(ZipEntry("[Content_Types].xml"))
      zip.write("""
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
      <Default Extension="xml" ContentType="application/xml"/>
      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
      <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
      </Types>
      """.trimIndent().toByteArray(Charsets.UTF_8))
      zip.closeEntry()

      // 2. _rels/.rels
      zip.putNextEntry(ZipEntry("_rels/.rels"))
      zip.write("""
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
      </Relationships>
      """.trimIndent().toByteArray(Charsets.UTF_8))
      zip.closeEntry()

      // 3. xl/_rels/workbook.xml.rels
      zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
      zip.write("""
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
      </Relationships>
      """.trimIndent().toByteArray(Charsets.UTF_8))
      zip.closeEntry()

      // 4. xl/workbook.xml
      zip.putNextEntry(ZipEntry("xl/workbook.xml"))
      zip.write("""
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
      <sheets>
      <sheet name="Transactions" sheetId="1" r:id="rId1"/>
      </sheets>
      </workbook>
      """.trimIndent().toByteArray(Charsets.UTF_8))
      zip.closeEntry()

      // 5. xl/styles.xml
      zip.putNextEntry(ZipEntry("xl/styles.xml"))
      zip.write("""
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
      <fonts count="2">
      <font><name val="Calibri"/><sz val="11"/></font>
      <font><b/><name val="Calibri"/><sz val="11"/></font>
      </fonts>
      <fills count="2">
      <fill><patternFill patternType="none"/></fill>
      <fill><patternFill patternType="gray125"/></fill>
      </fills>
      <borders count="1">
      <border><left/><right/><top/><bottom/></border>
      </borders>
      <cellStyleXfs count="1">
      <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
      </cellStyleXfs>
      <cellXfs count="2">
      <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
      <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
      </cellXfs>
      </styleSheet>
      """.trimIndent().toByteArray(Charsets.UTF_8))
      zip.closeEntry()

      // 6. xl/worksheets/sheet1.xml
      zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
      val sheetBuilder = StringBuilder()
      sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
      sheetBuilder.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
      sheetBuilder.append("""<cols>""")
      sheetBuilder.append("""<col min="1" max="1" width="14" customWidth="1"/>""") // Date
      sheetBuilder.append("""<col min="2" max="2" width="10" customWidth="1"/>""") // Time
      sheetBuilder.append("""<col min="3" max="3" width="12" customWidth="1"/>""") // Type
      sheetBuilder.append("""<col min="4" max="4" width="14" customWidth="1"/>""") // Amount
      sheetBuilder.append("""<col min="5" max="5" width="20" customWidth="1"/>""") // Category
      sheetBuilder.append("""<col min="6" max="6" width="30" customWidth="1"/>""") // Note
      sheetBuilder.append("""</cols>""")
      sheetBuilder.append("""<sheetData>""")

      // Header Row (Row 1 with style s="1" for bold)
      sheetBuilder.append("""<row r="1">""")
      sheetBuilder.append("""<c r="A1" t="inlineStr" s="1"><is><t>Date</t></is></c>""")
      sheetBuilder.append("""<c r="B1" t="inlineStr" s="1"><is><t>Time</t></is></c>""")
      sheetBuilder.append("""<c r="C1" t="inlineStr" s="1"><is><t>Type</t></is></c>""")
      sheetBuilder.append("""<c r="D1" t="inlineStr" s="1"><is><t>Amount</t></is></c>""")
      sheetBuilder.append("""<c r="E1" t="inlineStr" s="1"><is><t>Category</t></is></c>""")
      sheetBuilder.append("""<c r="F1" t="inlineStr" s="1"><is><t>Note</t></is></c>""")
      sheetBuilder.append("""</row>""")

      // Data Rows (Row 2, 3, ...)
      sortedTransactions.forEachIndexed { index, tx ->
        val rowIndex = index + 2
        val dateStr = dateFormat.format(Date(tx.timestamp))
        val timeStr = timeFormat.format(Date(tx.timestamp))
        val typeStr = tx.type.lowercase().replaceFirstChar { it.uppercase() }
        val amountStr = String.format(Locale.US, "%.2f", tx.amount)
        val catName = escapeXml(categoryMap[tx.categoryId] ?: "General")
        val noteStr = escapeXml(tx.note.ifEmpty { "N/A" })

        sheetBuilder.append("""<row r="$rowIndex">""")
        sheetBuilder.append("""<c r="A$rowIndex" t="inlineStr"><is><t>$dateStr</t></is></c>""")
        sheetBuilder.append("""<c r="B$rowIndex" t="inlineStr"><is><t>$timeStr</t></is></c>""")
        sheetBuilder.append("""<c r="C$rowIndex" t="inlineStr"><is><t>$typeStr</t></is></c>""")
        sheetBuilder.append("""<c r="D$rowIndex"><v>$amountStr</v></c>""")
        sheetBuilder.append("""<c r="E$rowIndex" t="inlineStr"><is><t>$catName</t></is></c>""")
        sheetBuilder.append("""<c r="F$rowIndex" t="inlineStr"><is><t>$noteStr</t></is></c>""")
        sheetBuilder.append("""</row>""")
      }

      // Portfolio holdings snapshot rows (appended after a blank separator row)
      if (holdings.isNotEmpty()) {
        var rowIndex = sortedTransactions.size + 2
        sheetBuilder.append("""<row r="$rowIndex"/>""")
        rowIndex++
        holdings.forEach { holding ->
          val unitPrice = holding.manualPriceOverride ?: holding.averageBuyPrice
          val amount = if (unitPrice != null && unitPrice > 0.0) holding.quantity * unitPrice else holding.quantity
          val amountStr = String.format(Locale.US, "%.2f", amount)
          val catName = escapeXml(holding.name.ifBlank { holding.symbol })
          val noteStr = escapeXml(
            buildString {
              append(formatQty(holding.quantity))
              append(' ').append(holding.symbol)
              if (unitPrice != null && unitPrice > 0.0) {
                append(" @ ").append(String.format(Locale.US, "%.2f", unitPrice)).append(' ').append(holding.currency)
              }
            }
          )
          val dateStr = dateFormat.format(Date(holding.createdAt))

          sheetBuilder.append("""<row r="$rowIndex">""")
          sheetBuilder.append("""<c r="A$rowIndex" t="inlineStr"><is><t>$dateStr</t></is></c>""")
          sheetBuilder.append("""<c r="B$rowIndex" t="inlineStr"><is><t></t></is></c>""")
          sheetBuilder.append("""<c r="C$rowIndex" t="inlineStr"><is><t>HOLDING</t></is></c>""")
          sheetBuilder.append("""<c r="D$rowIndex"><v>$amountStr</v></c>""")
          sheetBuilder.append("""<c r="E$rowIndex" t="inlineStr"><is><t>$catName</t></is></c>""")
          sheetBuilder.append("""<c r="F$rowIndex" t="inlineStr"><is><t>$noteStr</t></is></c>""")
          sheetBuilder.append("""</row>""")
          rowIndex++
        }
      }

      sheetBuilder.append("""</sheetData>""")
      sheetBuilder.append("""</worksheet>""")

      zip.write(sheetBuilder.toString().toByteArray(Charsets.UTF_8))
      zip.closeEntry()
    }
  }

  private fun escapeXml(text: String): String {
    return text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
  }
}
