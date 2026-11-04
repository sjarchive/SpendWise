package com.spendwise.app

import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.local.entity.relations.TransactionWithCategory
import com.spendwise.app.domain.model.PaymentMethod
import com.spendwise.app.domain.model.TransactionType
import com.spendwise.app.ui.util.CsvExportHelper
import com.spendwise.app.ui.util.CsvImportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvHelperTest {

    @Test
    fun testExportAndImportCycle() {
        val cat1 = CategoryEntity(id = 1, name = "Food", icon = "restaurant", color = 0xFFF97316L, isDefault = true, type = TransactionType.EXPENSE)
        val cat2 = CategoryEntity(id = 9, name = "Salary", icon = "payments", color = 0xFF22C55EL, isDefault = true, type = TransactionType.INCOME)

        val tx1 = TransactionEntity(
            id = 1,
            amount = 150.50,
            type = TransactionType.EXPENSE,
            categoryId = 1,
            category = "Food",
            dateTime = 1762000000000L,
            note = "Dinner with friends",
            paymentMethod = PaymentMethod.UPI
        )
        val tx2 = TransactionEntity(
            id = 2,
            amount = 50000.0,
            type = TransactionType.INCOME,
            categoryId = 9,
            category = "Salary",
            dateTime = 1761900000000L,
            note = "Monthly paycheck",
            paymentMethod = PaymentMethod.BANK
        )

        val transactions = listOf(
            TransactionWithCategory(tx1, cat1),
            TransactionWithCategory(tx2, cat2)
        )

        val csvString = CsvExportHelper.generateCsv(transactions)
        assertTrue(csvString.contains("Dinner with friends"))
        assertTrue(csvString.contains("Monthly paycheck"))

        val inputStream = ByteArrayInputStream(csvString.toByteArray(Charsets.UTF_8))
        val parseResult = CsvImportHelper.parseCsv(inputStream)

        assertEquals(2, parseResult.validTransactions.size)
        assertEquals(150.50, parseResult.validTransactions[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parseResult.validTransactions[0].type)
        assertEquals("Food", parseResult.validTransactions[0].categoryName)
        assertEquals(PaymentMethod.UPI, parseResult.validTransactions[0].paymentMethod)
        assertEquals("Dinner with friends", parseResult.validTransactions[0].note)

        assertEquals(50000.0, parseResult.validTransactions[1].amount, 0.001)
        assertEquals(TransactionType.INCOME, parseResult.validTransactions[1].type)
        assertEquals("Salary", parseResult.validTransactions[1].categoryName)
        assertEquals(PaymentMethod.BANK, parseResult.validTransactions[1].paymentMethod)
        assertEquals("Monthly paycheck", parseResult.validTransactions[1].note)
    }

    @Test
    fun testExternalCsvImport() {
        val sampleCsv = """
            Date,Time,Type,Category,Amount,Payment Method,Note
            2026-10-15,14:30,Expense,Shopping,2999.00,Card,"New shoes, running"
            2026-10-16,09:15,Income,Bonus,10000,UPI,"Performance bonus"
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charsets.UTF_8))
        val parseResult = CsvImportHelper.parseCsv(inputStream)

        assertEquals(2, parseResult.validTransactions.size)
        assertEquals(2999.00, parseResult.validTransactions[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parseResult.validTransactions[0].type)
        assertEquals("Shopping", parseResult.validTransactions[0].categoryName)
        assertEquals(PaymentMethod.CARD, parseResult.validTransactions[0].paymentMethod)
        assertEquals("New shoes, running", parseResult.validTransactions[0].note)

        assertEquals(10000.0, parseResult.validTransactions[1].amount, 0.001)
        assertEquals(TransactionType.INCOME, parseResult.validTransactions[1].type)
        assertEquals("Bonus", parseResult.validTransactions[1].categoryName)
        assertEquals(PaymentMethod.UPI, parseResult.validTransactions[1].paymentMethod)
    }

    @Test
    fun testExportEscapesQuotesInCategoryAndNote() {
        val cat = CategoryEntity(
            id = 1, name = "He said \"hi\"", icon = "category",
            color = 0xFF64748BL, isDefault = false, type = TransactionType.EXPENSE
        )
        val tx = TransactionEntity(
            id = 1, amount = 12.0, type = TransactionType.EXPENSE, categoryId = 1,
            category = cat.name, dateTime = 1762000000000L,
            note = "Note with \"quotes\" and, comma", paymentMethod = PaymentMethod.CASH
        )

        val csvString = CsvExportHelper.generateCsv(listOf(TransactionWithCategory(tx, cat)))

        // RFC 4180: embedded double quotes must be doubled inside quoted fields
        assertTrue(csvString.contains("\"He said \"\"hi\"\"\""))
        assertTrue(csvString.contains("\"Note with \"\"quotes\"\" and, comma\""))

        // And the exported row must round-trip back to the original values
        val parseResult = CsvImportHelper.parseCsv(ByteArrayInputStream(csvString.toByteArray(Charsets.UTF_8)))
        assertEquals(1, parseResult.validTransactions.size)
        assertEquals("He said \"hi\"", parseResult.validTransactions[0].categoryName)
        assertEquals("Note with \"quotes\" and, comma", parseResult.validTransactions[0].note)
        assertEquals(12.0, parseResult.validTransactions[0].amount, 0.001)
    }
}
