package com.spendwise.app.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.spendwise.app.data.local.entity.relations.TransactionWithCategory
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvExportHelper {

    /**
     * Export files must be locale-invariant: [DateUtils.formatDate] renders
     * month names with Locale.getDefault(), so on a Hindi/German/etc. device
     * the CSV contained month names the importer (which only tries
     * Locale.ENGLISH patterns) could not parse — an export on a localized
     * device then failed to restore with "Unrecognized date". ISO yyyy-MM-dd
     * + 24h HH:mm are in the importer's pattern list and unambiguous
     * everywhere (also spreadsheet-friendly, unlike the old "Jan 15 2025").
     */
    private val exportDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val exportTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun writeCsvToUri(context: Context, uri: Uri, transactions: List<TransactionWithCategory>): Boolean {
        return try {
            val csvData = generateCsv(transactions)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvData.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generateCsv(transactions: List<TransactionWithCategory>): String {
        val stringBuilder = StringBuilder()
        // Header
        stringBuilder.append("ID,Date,Time,Type,Category,Amount,Payment Method,Note\n")

        val zone = ZoneId.systemDefault()
        transactions.forEach { item ->
            val tx = item.transaction
            val catName = item.category?.name ?: tx.category
            val date = Instant.ofEpochMilli(tx.dateTime).atZone(zone).toLocalDate().format(exportDateFormatter)
            val time = Instant.ofEpochMilli(tx.dateTime).atZone(zone).toLocalTime().format(exportTimeFormatter)
            // RFC 4180: escape double quotes inside quoted fields (notes AND
            // category names — a category like He said "hi" previously broke the row).
            val catEscaped = "\"${catName.replace("\"", "\"\"")}\""
            val noteEscaped = "\"${tx.note.replace("\"", "\"\"")}\""
            // Fixed 2-decimal amount: Double.toString() switches to scientific
            // notation above 10M ("9.9999999E8"), which spreadsheets mangle and
            // made the exported CSV hard to read. Locale.US keeps the dot decimal.
            val amount = String.format(Locale.US, "%.2f", tx.amount)

            stringBuilder.append(
                "${tx.id},$date,$time,${tx.type.name},$catEscaped,$amount,${tx.paymentMethod.displayName},$noteEscaped\n"
            )
        }
        return stringBuilder.toString()
    }

    fun exportAndShareCsv(context: Context, transactions: List<TransactionWithCategory>): Boolean {
        return try {
            val csvData = generateCsv(transactions)
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, "SpendWise_Transactions_${System.currentTimeMillis()}.csv")
            FileWriter(file).use { writer ->
                writer.write(csvData)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "SpendWise Transactions Export")
                putExtra(Intent.EXTRA_TEXT, "Exported ${transactions.size} transactions from SpendWise.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Transactions CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
