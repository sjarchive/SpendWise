package com.example.data.repository

import com.example.data.local.DefaultCategories
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.relations.BudgetWithCategory
import com.example.data.local.entity.relations.TransactionWithCategory
import com.example.di.ApplicationScope
import com.example.domain.model.TransactionType
import com.example.domain.repository.FinanceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) : FinanceRepository {

    /**
     * Default-category seeding / legacy-name migration only needs to run once per
     * process. Caching the result avoids a database round-trip every time a screen
     * ViewModel is created (Home / Transactions / Budgets / Insights / Categories
     * all call this from init), which keeps tab switches snappier.
     */
    @Volatile
    private var seedingVerified = false

    /**
     * In-memory snapshot of the categories table, kept warm for the whole
     * process by the collector started in [init]. Screen ViewModels read it
     * synchronously via [peekCategoriesSnapshot] so category-backed forms
     * render their final layout on the first frame — otherwise the chips
     * (loaded asynchronously from Room) pop in a beat later and reflow the
     * fields below them, which reads as a laggy, jumpy form.
     */
    @Volatile
    private var categoriesSnapshot: List<CategoryEntity>? = null

    init {
        applicationScope.launch {
            ensureDefaultCategoriesSeeded()
            getAllCategories().collect { categoriesSnapshot = it }
        }
    }

    override fun peekCategoriesSnapshot(): List<CategoryEntity>? = categoriesSnapshot

    // --- Categories ---

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    override fun getCategoryById(id: Long): Flow<CategoryEntity?> =
        categoryDao.getCategoryById(id)

    override suspend fun getCategoryByIdSuspend(id: Long): CategoryEntity? =
        withContext(ioDispatcher) {
            categoryDao.getCategoryByIdSuspend(id)
        }

    override suspend fun insertCategory(category: CategoryEntity): Long =
        withContext(ioDispatcher) {
            categoryDao.insertCategory(category)
        }

    override suspend fun updateCategory(category: CategoryEntity) =
        withContext(ioDispatcher) {
            categoryDao.updateCategory(category)
        }

    override suspend fun deleteCategory(category: CategoryEntity) =
        withContext(ioDispatcher) {
            categoryDao.deleteCategory(category)
        }

    override suspend fun deleteCustomCategoryById(id: Long) =
        withContext(ioDispatcher) {
            categoryDao.deleteCustomCategoryById(id)
        }

    override suspend fun getCategoryByName(name: String): CategoryEntity? =
        withContext(ioDispatcher) {
            categoryDao.getCategoryByName(name)
        }

    // --- Transactions ---

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    override fun getTransactionsWithCategory(): Flow<List<TransactionWithCategory>> =
        transactionDao.getTransactionsWithCategory()

    override fun getTransactionWithCategoryById(id: Long): Flow<TransactionWithCategory?> =
        transactionDao.getTransactionWithCategoryById(id)

    override fun getTransactionsBetween(
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionWithCategory>> =
        transactionDao.getTransactionsBetween(startDate, endDate)

    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionWithCategory>> =
        transactionDao.getTransactionsByType(type)

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionWithCategory>> =
        transactionDao.getTransactionsByCategory(categoryId)

    override fun getTotalExpenseBetween(startDate: Long, endDate: Long): Flow<Double> =
        transactionDao.getTotalExpenseBetween(startDate, endDate).map { it ?: 0.0 }

    override fun getTotalIncomeBetween(startDate: Long, endDate: Long): Flow<Double> =
        transactionDao.getTotalIncomeBetween(startDate, endDate).map { it ?: 0.0 }

    override fun getTotalExpense(): Flow<Double> =
        transactionDao.getTotalExpense().map { it ?: 0.0 }

    override fun getTotalIncome(): Flow<Double> =
        transactionDao.getTotalIncome().map { it ?: 0.0 }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long =
        withContext(ioDispatcher) {
            transactionDao.insertTransaction(transaction)
        }

    override suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> =
        withContext(ioDispatcher) {
            transactionDao.insertTransactions(transactions)
        }

    override suspend fun updateTransaction(transaction: TransactionEntity) =
        withContext(ioDispatcher) {
            transactionDao.updateTransaction(transaction)
        }

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        withContext(ioDispatcher) {
            transactionDao.deleteTransaction(transaction)
        }

    override suspend fun deleteTransactionById(id: Long) =
        withContext(ioDispatcher) {
            transactionDao.deleteTransactionById(id)
        }

    override suspend fun reassignTransactionCategory(
        oldCategoryId: Long,
        newCategoryId: Long,
        newCategoryName: String
    ) = withContext(ioDispatcher) {
        transactionDao.reassignCategory(oldCategoryId, newCategoryId, newCategoryName)
    }

    // --- Budgets ---

    override fun getAllBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getAllBudgets()

    override fun getBudgetsWithCategory(): Flow<List<BudgetWithCategory>> =
        budgetDao.getBudgetsWithCategory()

    override fun getOverallBudget(): Flow<BudgetEntity?> =
        budgetDao.getOverallBudget()

    override fun getBudgetForCategory(categoryId: Long): Flow<BudgetEntity?> =
        budgetDao.getBudgetForCategory(categoryId)

    override suspend fun insertBudget(budget: BudgetEntity): Long =
        withContext(ioDispatcher) {
            budgetDao.insertBudget(budget)
        }

    override suspend fun updateBudget(budget: BudgetEntity) =
        withContext(ioDispatcher) {
            budgetDao.updateBudget(budget)
        }

    override suspend fun deleteBudget(budget: BudgetEntity) =
        withContext(ioDispatcher) {
            budgetDao.deleteBudget(budget)
        }

    override suspend fun deleteBudgetById(id: Long) =
        withContext(ioDispatcher) {
            budgetDao.deleteBudgetById(id)
        }

    // --- Seeding ---

    override suspend fun ensureDefaultCategoriesSeeded() {
        if (seedingVerified) return
        withContext(ioDispatcher) {
            if (seedingVerified) return@withContext
            val count = categoryDao.getCategoryCount()
            if (count == 0) {
                categoryDao.insertCategories(DefaultCategories.allDefaultCategories)
            } else {
                val freelance = categoryDao.getCategoryByName("Freelance")
                if (freelance != null) {
                    // The categories table has a UNIQUE(name, type) index. Renaming
                    // 'Freelance' would violate it (and crash) if the user already
                    // has their own 'Pocket Money' category of the same type, so
                    // skip the rename in that case and keep 'Freelance' as-is.
                    val conflicting = categoryDao.getCategoryByName("Pocket Money")
                    val hasConflict = conflicting != null && conflicting.type == freelance.type
                    if (!hasConflict) {
                        categoryDao.updateCategory(
                            freelance.copy(name = "Pocket Money", icon = "savings")
                        )
                    }
                }
            }
            seedingVerified = true
        }
    }
}
