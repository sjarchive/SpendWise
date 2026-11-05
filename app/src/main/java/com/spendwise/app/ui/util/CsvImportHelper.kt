package com.spendwise.app.ui.util

import androidx.compose.runtime.Immutable
import com.spendwise.app.domain.model.PaymentMethod
import com.spendwise.app.domain.model.TransactionType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
data class ParsedTransactionItem(
    val amount: Double,
    val type: TransactionType,
    val categoryName: String,
    val dateTime: Long,
    val note: String,
    val paymentMethod: PaymentMethod
)

@Immutable
data class CsvParseResult(
    val totalRowsRead: Int,
    val validTransactions: List<ParsedTransactionItem>,
    val uniqueCategoryNames: Set<Pair<String, TransactionType>>,
    val totalExpense: Double,
    val totalIncome: Double,
    val earliestDate: Long?,
    val latestDate: Long?,
    val parsingErrors: List<String>
)

object CsvImportHelper {

    private val datePatterns = listOf(
        "MMM dd yyyy",
        "MMM d yyyy",
        "MMM dd, yyyy",
        "MMM d, yyyy",
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "dd-MM-yyyy",
        "d MMM yyyy",
        "dd MMM yyyy"
    )

    private val timePatterns = listOf(
        "hh:mm a",
        "h:mm a",
        "hh:mma",
        "h:mma",
        "HH:mm:ss",
        "HH:mm"
    )

    /**
     * Parses a CSV input stream into a list of parsed transaction records.
     */
    fun parseCsv(inputStream: InputStream): CsvParseResult {
        val validItems = mutableListOf<ParsedTransactionItem>()
        val errors = mutableListOf<String>()
        val categoryNames = mutableSetOf<Pair<String, TransactionType>>()

        val reader = BufferedReader(InputStreamReader(inputStream))
        val rows = readCsvRows(reader)

        if (rows.isEmpty()) {
            return CsvParseResult(
                totalRowsRead = 0,
                validTransactions = emptyList(),
                uniqueCategoryNames = emptySet(),
                totalExpense = 0.0,
                totalIncome = 0.0,
                earliestDate = null,
                latestDate = null,
                parsingErrors = listOf("File is empty.")
            )
        }

        var headerRowIndex = -1
        var dateCol = -1
        var timeCol = -1
        var typeCol = -1
        var catCol = -1
        var amountCol = -1
        var methodCol = -1
        var noteCol = -1

        // Check if row 0 has headers
        val firstRow = rows[0]
        if (isHeaderRow(firstRow)) {
            headerRowIndex = 0
            for (i in firstRow.indices) {
                val h = firstRow[i].lowercase(Locale.ROOT).trim()
                when {
                    h == "date" || h == "transaction date" -> dateCol = i
                    h == "time" || h == "transaction time" -> timeCol = i
                    h == "datetime" || h == "date & time" || h == "timestamp" -> {
                        if (dateCol == -1) dateCol = i
                    }
                    h == "type" || h == "transaction type" -> typeCol = i
                    h == "category" || h == "category name" -> catCol = i
                    h == "amount" || h == "total" || h == "value" || h == "sum" -> amountCol = i
                    h == "payment method" || h == "paymentmethod" || h == "payment mode" || h == "method" -> methodCol = i
                    h == "note" || h == "notes" || h == "description" || h == "remark" || h == "memo" -> noteCol = i
                }
            }
        } else {
            // Default SpendWise column indices if no headers:
            // 0: ID, 1: Date, 2: Time, 3: Type, 4: Category, 5: Amount, 6: Payment Method, 7: Note
            dateCol = 1
            timeCol = 2
            typeCol = 3
            catCol = 4
            amountCol = 5
            methodCol = 6
            noteCol = 7
        }

        // If amount column wasn't identified by name in header row, guess from structure
        if (amountCol == -1) {
            amountCol = if (firstRow.size >= 6) 5 else 0
        }

        val startIndex = if (headerRowIndex >= 0) 1 else 0

        for (rowIdx in startIndex until rows.size) {
            val row = rows[rowIdx]
            if (row.all { it.isBlank() }) continue // Skip completely blank lines

            try {
                // Parse amount
                val amountStr = if (amountCol >= 0 && amountCol < row.size) row[amountCol] else ""
                val cleanedAmountStr = amountStr.replace("[^0-9.-]".toRegex(), "").trim()
                val parsedAmount = cleanedAmountStr.toDoubleOrNull()
                if (parsedAmount == null) {
                    errors.add("Row ${rowIdx + 1}: Invalid amount '$amountStr'")
                    continue
                }

                val absAmount = kotlin.math.abs(parsedAmount)
                if (absAmount <= 0.0) {
                    errors.add("Row ${rowIdx + 1}: Amount cannot be zero or negative")
                    continue
                }

                // Parse Type (negative amounts without an explicit INCOME marker are
                // treated as expenses)
                val typeStr = if (typeCol >= 0 && typeCol < row.size) row[typeCol].trim() else ""
                val isIncome = typeStr.equals("INCOME", ignoreCase = true) ||
                    typeStr.equals("Credit", ignoreCase = true) ||
                    typeStr.equals("In", ignoreCase = true)
                val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

                // Parse Category
                val rawCategory = if (catCol >= 0 && catCol < row.size) row[catCol].trim() else ""
                val categoryName = if (rawCategory.isNotBlank()) {
                    rawCategory
                } else {
                    if (type == TransactionType.EXPENSE) "Other" else "Other Income"
                }

                // Parse Date & Time
                val dateStr = if (dateCol >= 0 && dateCol < row.size) row[dateCol].trim() else ""
                val timeStr = if (timeCol >= 0 && timeCol < row.size) row[timeCol].trim() else ""
                val timestamp = parseDateTime(dateStr, timeStr)
                if (timestamp == null) {
                    // Do NOT silently import with today's date — that corrupts the
                    // record's real date and the user never finds out. Skip the row
                    // and surface it in the preview error list instead.
                    errors.add("Row ${rowIdx + 1}: Unrecognized date '$dateStr'")
                    continue
                }

                // Parse Payment Method
                val methodStr = if (methodCol >= 0 && methodCol < row.size) row[methodCol].trim() else ""
                val paymentMethod = when {
                    methodStr.equals("UPI", ignoreCase = true) -> PaymentMethod.UPI
                    methodStr.contains("card", ignoreCase = true) -> PaymentMethod.CARD
                    methodStr.contains("bank", ignoreCase = true) || methodStr.contains("transfer", ignoreCase = true) -> PaymentMethod.BANK
                    else -> PaymentMethod.CASH
                }

                // Parse Note
                val note = if (noteCol >= 0 && noteCol < row.size) row[noteCol].trim() else ""

                val item = ParsedTransactionItem(
                    amount = absAmount,
                    type = type,
                    categoryName = categoryName,
                    dateTime = timestamp,
                    note = note,
                    paymentMethod = paymentMethod
                )
                validItems.add(item)
                categoryNames.add(Pair(categoryName, type))
            } catch (e: Exception) {
                errors.add("Row ${rowIdx + 1}: ${e.message ?: "Failed to parse"}")
            }
        }

        var totalExpense = 0.0
        var totalIncome = 0.0
        var earliest: Long? = null
        var latest: Long? = null

        for (item in validItems) {
            if (item.type == TransactionType.EXPENSE) {
                totalExpense += item.amount
            } else {
                totalIncome += item.amount
            }

            if (earliest == null || item.dateTime < earliest) {
                earliest = item.dateTime
            }
            if (latest == null || item.dateTime > latest) {
                latest = item.dateTime
            }
        }

        return CsvParseResult(
            totalRowsRead = rows.size - (if (headerRowIndex >= 0) 1 else 0),
            validTransactions = validItems,
            uniqueCategoryNames = categoryNames,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            earliestDate = earliest,
            latestDate = latest,
            parsingErrors = errors
        )
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        val lower = row.map { it.lowercase(Locale.ROOT).trim() }
        return lower.any {
            it in listOf("amount", "date", "category", "type", "payment method", "note", "id")
        }
    }

    /**
     * Parses date + time strings to epoch millis. Returns null when the date
     * cannot be recognized (the caller reports it and skips the row) — a missing
     * time alone still defaults to noon.
     */
    private fun parseDateTime(dateStr: String, timeStr: String): Long? {
        val zone = ZoneId.systemDefault()

        // 1. Check if dateStr itself is a raw epoch millis
        dateStr.toLongOrNull()?.let {
            if (it > 100000000000L) return it // Likely milliseconds
        }

        // 2. Parse LocalDate
        var localDate: LocalDate? = null
        val cleanDate = dateStr.replace(",", " ").replace("\\s+".toRegex(), " ").trim()

        if (cleanDate.isNotBlank()) {
            for (pattern in datePatterns) {
                try {
                    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
                    localDate = LocalDate.parse(cleanDate, formatter)
                    break
                } catch (_: Exception) {}
            }
        }

        if (localDate == null) return null

        // 3. Parse LocalTime
        var localTime: LocalTime? = null
        val cleanTime = timeStr.trim()
        if (cleanTime.isNotBlank()) {
            for (pattern in timePatterns) {
                try {
                    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
                    localTime = LocalTime.parse(cleanTime, formatter)
                    break
                } catch (_: Exception) {}
            }
        }

        if (localTime == null) {
            localTime = LocalTime.of(12, 0)
        }

        val ldt = LocalDateTime.of(localDate, localTime)
        return ldt.atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * Reads all rows from a CSV BufferedReader while respecting RFC 4180 quotes.
     */
    private fun readCsvRows(reader: BufferedReader): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var line: String? = reader.readLine()
        val currentTokens = mutableListOf<String>()
        val currentToken = StringBuilder()
        var inQuotes = false

        while (line != null) {
            var i = 0
            while (i < line.length) {
                val c = line[i]
                if (c == '\"') {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        // Escaped double quote
                        currentToken.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                } else if (c == ',' && !inQuotes) {
                    currentTokens.add(currentToken.toString().trim())
                    currentToken.clear()
                } else {
                    currentToken.append(c)
                }
                i++
            }

            if (!inQuotes) {
                currentTokens.add(currentToken.toString().trim())
                currentToken.clear()
                rows.add(currentTokens.toList())
                currentTokens.clear()
            } else {
                // Multi-line field within quotes: append newline character
                currentToken.append("\n")
            }

            line = reader.readLine()
        }

        // In case last line ended while still in quotes or with remaining token
        if (currentToken.isNotEmpty() || currentTokens.isNotEmpty()) {
            currentTokens.add(currentToken.toString().trim())
            rows.add(currentTokens.toList())
        }

        return rows
    }
}
