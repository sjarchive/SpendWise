package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entity.relations.TransactionWithCategory
import java.io.File
import java.io.FileWriter

object CsvExportHelper {

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

        transactions.forEach { item ->
            val tx = item.transaction
            val catName = item.category?.name ?: tx.category
            val date = DateUtils.formatDate(tx.dateTime).replace(",", "")
            val time = DateUtils.formatTime(tx.dateTime)
            // RFC 4180: escape double quotes inside quoted fields (notes AND
            // category names — a category like He said "hi" previously broke the row).
            val catEscaped = "\"${catName.replace("\"", "\"\"")}\""
            val noteEscaped = "\"${tx.note.replace("\"", "\"\"")}\""

            stringBuilder.append(
                "${tx.id},$date,$time,${tx.type.name},$catEscaped,${tx.amount},${tx.paymentMethod.displayName},$noteEscaped\n"
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
