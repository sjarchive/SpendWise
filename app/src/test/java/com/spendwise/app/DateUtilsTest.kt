package com.spendwise.app

import com.spendwise.app.ui.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the chart helpers added for trend-chart axis/label support.
 */
class DateUtilsTest {

    @Test
    fun `compact format keeps whole rupees under 1000`() {
        assertEquals("₹0", DateUtils.formatCurrencyCompact(0.0))
        assertEquals("₹450", DateUtils.formatCurrencyCompact(450.0))
        assertEquals("₹999", DateUtils.formatCurrencyCompact(999.49))
    }

    @Test
    fun `compact format rounds 999_6 up to 1k`() {
        assertEquals("₹1k", DateUtils.formatCurrencyCompact(999.6))
    }

    @Test
    fun `compact format thousands use k suffix with trailing zero trimmed`() {
        assertEquals("₹1k", DateUtils.formatCurrencyCompact(1000.0))
        assertEquals("₹1.2k", DateUtils.formatCurrencyCompact(1200.0))
        assertEquals("₹12.5k", DateUtils.formatCurrencyCompact(12500.0))
        assertEquals("₹150k", DateUtils.formatCurrencyCompact(150000.0))
    }

    @Test
    fun `compact format millions use M suffix`() {
        assertEquals("₹1.2M", DateUtils.formatCurrencyCompact(1_240_000.0))
        assertEquals("₹2M", DateUtils.formatCurrencyCompact(2_000_000.0))
    }

    @Test
    fun `compact format respects custom currency symbol`() {
        assertEquals("$1.2k", DateUtils.formatCurrencyCompact(1200.0, symbol = "$"))
        assertEquals("€450", DateUtils.formatCurrencyCompact(450.0, symbol = "€"))
    }

    @Test
    fun `nice axis max gives three clean steps`() {
        assertEquals(12.0, DateUtils.niceAxisMax(10.0), 1e-9)
        assertEquals(600.0, DateUtils.niceAxisMax(500.0), 1e-9)
        assertEquals(1200.0, DateUtils.niceAxisMax(1000.0), 1e-9)
        assertEquals(3000.0, DateUtils.niceAxisMax(3000.0), 1e-9)
        assertEquals(150000.0, DateUtils.niceAxisMax(123456.0), 1e-9)
    }

    @Test
    fun `nice axis max handles degenerate input`() {
        assertEquals(10.0, DateUtils.niceAxisMax(0.0), 1e-9)
        assertEquals(10.0, DateUtils.niceAxisMax(-5.0), 1e-9)
    }

    @Test
    fun `nice axis max never truncates the data`() {
        val samples = listOf(10.0, 37.5, 99.0, 451.25, 999.0, 1234.0, 5000.0, 123456.0)
        samples.forEach { max ->
            assertTrue(
                "niceAxisMax($max) = ${DateUtils.niceAxisMax(max)} is below the data max",
                DateUtils.niceAxisMax(max) >= max
            )
        }
    }

    @Test
    fun `nice axis max ticks are integral for chart range`() {
        // Chart maxes are coerced to >= 10, so roughStep >= 3.33 and every
        // chosen step is an integer multiple of a power of ten — the tick
        // values passed to formatCurrencyCompact must stay integral.
        val samples = listOf(10.0, 37.5, 99.0, 451.25, 999.0, 1234.0, 5000.0, 123456.0)
        samples.forEach { max ->
            val niceMax = DateUtils.niceAxisMax(max)
            assertEquals("niceAxisMax($max) is not integral", niceMax, niceMax.toLong().toDouble(), 1e-9)
        }
    }
}
