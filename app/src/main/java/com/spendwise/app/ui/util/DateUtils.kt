package com.spendwise.app.ui.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

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

    /**
     * Compact amount for chart axes/labels: ₹450 / ₹1.2k / ₹1M (no paise).
     * Rounds first so 999.6 renders as ₹1k, not ₹1000. Locale.US keeps the
     * decimal separator a dot so the k/M suffix always reads correctly.
     */
    fun formatCurrencyCompact(amount: Double, symbol: String = "₹"): String {
        val v = if (amount < 0) -amount else amount
        val rounded = round(v)
        fun scaled(divisor: Double, suffix: String): String {
            // Trim ".0" from the NUMBER before appending the suffix, so
            // 1000.0 → "₹1k" (not "₹1.0k").
            val text = String.format(Locale.US, "%.1f", rounded / divisor)
            return "$symbol${if (text.endsWith(".0")) text.dropLast(2) else text}$suffix"
        }
        return when {
            rounded >= 1_000_000.0 -> scaled(1_000_000.0, "M")
            rounded >= 1_000.0 -> scaled(1_000.0, "k")
            else -> "$symbol${rounded.toLong()}"
        }
    }

    /**
     * Round a chart maximum up to 3 × a "nice" step (1/1.5/2/2.5/3/4/5/6/8 × 10^n)
     * so Y-axis ticks land on clean values (₹200 / ₹400 / ₹600) instead of
     * ₹333-style fractions. Always >= the data max, so no bar is clipped.
     */
    fun niceAxisMax(maxValue: Double): Double {
        if (maxValue <= 0.0) return 10.0
        val roughStep = maxValue / 3.0
        val magnitude = 10.0.pow(floor(log10(roughStep)))
        val multipliers = doubleArrayOf(1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)
        val step = (multipliers.firstOrNull { it * magnitude >= roughStep } ?: 10.0) * magnitude
        return step * 3.0
    }
}
