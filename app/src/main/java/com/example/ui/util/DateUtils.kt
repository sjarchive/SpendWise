package com.example.ui.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    private val dayHeaderFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        val localDate = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return localDate.format(dateFormatter)
    }

    fun formatTime(timestamp: Long): String {
        val localTime = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return localTime.format(timeFormatter)
    }

    fun formatDateTime(timestamp: Long): String {
        val localDateTime = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return localDateTime.format(dateTimeFormatter)
    }

    fun formatDayHeader(timestamp: Long): String {
        val zone = ZoneId.systemDefault()
        val targetDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)

        return when (targetDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> targetDate.format(dayHeaderFormatter)
        }
    }

    fun getCurrentMonthRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)
        val startOfMonth = now.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
        return Pair(startOfMonth, endOfMonth)
    }

    fun formatCurrency(amount: Double, symbol: String = "₹"): String {
        // Render negative amounts as -₹500.00 (sign before the symbol), not ₹-500.00.
        return if (amount < 0) {
            "-$symbol${String.format(Locale.getDefault(), "%,.2f", -amount)}"
        } else {
            "$symbol${String.format(Locale.getDefault(), "%,.2f", amount)}"
        }
    }
}
