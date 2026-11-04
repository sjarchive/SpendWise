package com.example.domain.repository

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.relations.BudgetWithCategory
import com.example.data.local.entity.relations.TransactionWithCategory
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    /**
     * Synchronous, possibly-stale snapshot of the whole categories table, or
     * `null` until the first database read has completed. ViewModels read it at
     * init so forms render with their final layout on the very first frame —
     * without it, asynchronously-loaded category chips pop in a beat later and
     * shove the fields below them (date, payment method, note) down the screen.
     */
    fun peekCategoriesSnapshot(): List<CategoryEntity>?

    fun getCategoryById(id: Long): Flow<CategoryEntity?>
    suspend fun getCategoryByIdSuspend(id: Long): CategoryEntity?
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun deleteCustomCategoryById(id: Long)

    // Transactions
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsWithCategory(): Flow<List<TransactionWithCategory>>
    fun getTransactionWithCategoryById(id: Long): Flow<TransactionWithCategory?>
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionWithCategory>>
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionWithCategory>>
    fun getTotalExpenseBetween(startDate: Long, endDate: Long): Flow<Double>
    fun getTotalIncomeBetween(startDate: Long, endDate: Long): Flow<Double>
    fun getTotalExpense(): Flow<Double>
    fun getTotalIncome(): Flow<Double>
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteTransactionById(id: Long)
    suspend fun reassignTransactionCategory(oldCategoryId: Long, newCategoryId: Long, newCategoryName: String)

    // Budgets
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    fun getBudgetsWithCategory(): Flow<List<BudgetWithCategory>>
    fun getOverallBudget(): Flow<BudgetEntity?>
    fun getBudgetForCategory(categoryId: Long): Flow<BudgetEntity?>
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
    suspend fun deleteBudgetById(id: Long)

    // Category query
    suspend fun getCategoryByName(name: String): CategoryEntity?

    // Seeding check
    suspend fun ensureDefaultCategoriesSeeded()
}
