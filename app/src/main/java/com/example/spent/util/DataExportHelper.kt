package com.app.spent.util

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.TransactionEntity
object DataExportHelper {

  /**
   * Exports transactions in a native Microsoft Excel (.xlsx) spreadsheet format.
   * Separates Date and Time into distinct columns and excludes internal database IDs.
   * Columns: Date, Time, Type, Amount, Category, Note
   */
  fun exportTransactionsToExcel(
  context: Context,
  transactions: List<TransactionEntity>,
  categories: List<CategoryEntity>
  ) {
    if (transactions.isEmpty()) {
      Toast.makeText(context, "No transactions available to export", Toast.LENGTH_SHORT).show()
      return
    }

    try {
      val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
      val categoryMap = categories.associateBy({ it.id }, { it.name })

      val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
      val xlsxFileName = "Spent_Transactions_$timeStamp.xlsx"
      val xlsxFile = File(exportDir, xlsxFileName)

      // Generate genuine OpenXML (.xlsx) file structure using ZipOutputStream
      generateXlsxFile(
      file = xlsxFile,
      transactions = transactions.sortedByDescending { it.timestamp },
      categoryMap = categoryMap,
      dateFormat = dateFormat,
      timeFormat = timeFormat
      )

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

      val chooser = Intent.createChooser(shareIntent, "Download / Export Transactions (.xlsx)")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "Export error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
    }
  }

  private fun generateXlsxFile(
  file: File,
  transactions: List<TransactionEntity>,
  categoryMap: Map<String, String>,
  dateFormat: SimpleDateFormat,
  timeFormat: SimpleDateFormat
  ) {
    ZipOutputStream(FileOutputStream(file)).use { zip ->
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
      transactions.forEachIndexed { index, tx ->
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
