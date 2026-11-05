package com.spendwise.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.BudgetEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.relations.TransactionWithCategory
import com.spendwise.app.domain.model.TransactionType
import com.spendwise.app.domain.repository.FinanceRepository
import com.spendwise.app.ui.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BudgetWarningLevel {
    NORMAL,          // Under 80%
    APPROACHING_80,  // 80% to 99.9%
    EXCEEDED_100     // 100% and above
}

@Immutable
data class CategoryBudgetProgress(
    val category: CategoryEntity,
    val budget: BudgetEntity,
    val spentAmount: Double,
    val budgetAmount: Double,
    val percentage: Float,
    val warningLevel: BudgetWarningLevel
)

@Immutable
data class OverallBudgetProgress(
    val budgetId: Long = 0,
    val targetAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val percentage: Float = 0f,
    val warningLevel: BudgetWarningLevel = BudgetWarningLevel.NORMAL
)

@Immutable
data class BudgetsUiState(
    val isLoading: Boolean = true,
    val overallBudget: OverallBudgetProgress? = null,
    val categoryBudgets: List<CategoryBudgetProgress> = emptyList(),
    val availableExpenseCategories: List<CategoryEntity> = emptyList(),
    val showOverallBudgetDialog: Boolean = false,
    val showCategoryBudgetDialog: Boolean = false,
    val budgetAmountInput: String = "",
    val selectedCategoryForBudget: CategoryEntity? = null,
    val inputError: String? = null
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        loadBudgetsAndSpending()
    }

    private fun loadBudgetsAndSpending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            financeRepository.ensureDefaultCategoriesSeeded()

            combine(
                financeRepository.getAllBudgets(),
                // Full transaction list: the current-month window is filtered inside
                // the combine lambda so it is recomputed on every emission (capturing
                // it once kept showing the previous month after a month rollover).
                financeRepository.getTransactionsWithCategory(),
                financeRepository.getCategoriesByType(TransactionType.EXPENSE)
            ) { budgets, allTransactions, categories ->
                val (startOfMonth, endOfMonth) = DateUtils.getCurrentMonthRange()
                val transactions = allTransactions.filter {
                    it.transaction.dateTime in startOfMonth..endOfMonth
                }
                Triple(budgets, transactions, categories)
            }.collect { (budgets, transactions, categories) ->
                val expenseTransactions = transactions.filter { it.transaction.type == TransactionType.EXPENSE }
                val totalExpense = expenseTransactions.sumOf { it.transaction.amount }

                // Overall Budget calculation
                val overallEntity = budgets.firstOrNull { it.categoryId == null }
                val overallProgress = if (overallEntity != null && overallEntity.monthlyAmount > 0) {
                    val percent = ((totalExpense / overallEntity.monthlyAmount) * 100f).toFloat()
                    val warning = when {
                        percent >= 100f -> BudgetWarningLevel.EXCEEDED_100
                        percent >= 80f -> BudgetWarningLevel.APPROACHING_80
                        else -> BudgetWarningLevel.NORMAL
                    }
                    OverallBudgetProgress(
                        budgetId = overallEntity.id,
                        targetAmount = overallEntity.monthlyAmount,
                        spentAmount = totalExpense,
                        remainingAmount = (overallEntity.monthlyAmount - totalExpense).coerceAtLeast(0.0),
                        percentage = percent,
                        warningLevel = warning
                    )
                } else null

                // Category Budgets calculation
                val catBudgets = budgets.filter { it.categoryId != null }
                val catProgressList = catBudgets.mapNotNull { budget ->
                    val category = categories.firstOrNull { it.id == budget.categoryId } ?: return@mapNotNull null
                    val catSpent = expenseTransactions
                        .filter { it.transaction.categoryId == category.id }
                        .sumOf { it.transaction.amount }

                    val percent = if (budget.monthlyAmount > 0) {
                        ((catSpent / budget.monthlyAmount) * 100f).toFloat()
                    } else 0f

                    val warning = when {
                        percent >= 100f -> BudgetWarningLevel.EXCEEDED_100
                        percent >= 80f -> BudgetWarningLevel.APPROACHING_80
                        else -> BudgetWarningLevel.NORMAL
                    }

                    CategoryBudgetProgress(
                        category = category,
                        budget = budget,
                        spentAmount = catSpent,
                        budgetAmount = budget.monthlyAmount,
                        percentage = percent,
                        warningLevel = warning
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        overallBudget = overallProgress,
                        categoryBudgets = catProgressList,
                        availableExpenseCategories = categories
                    )
                }
            }
        }
    }

    fun openOverallBudgetDialog() {
        val currentAmount = _uiState.value.overallBudget?.targetAmount
        _uiState.update {
            it.copy(
                showOverallBudgetDialog = true,
                budgetAmountInput = if (currentAmount != null && currentAmount > 0) {
                    if (currentAmount % 1.0 == 0.0) currentAmount.toLong().toString() else currentAmount.toString()
                } else "",
                inputError = null
            )
        }
    }

    fun closeOverallBudgetDialog() {
        _uiState.update { it.copy(showOverallBudgetDialog = false) }
    }

    fun openCategoryBudgetDialog(existingCatBudget: CategoryBudgetProgress? = null) {
        val cat = existingCatBudget?.category ?: _uiState.value.availableExpenseCategories.firstOrNull()
        val amount = existingCatBudget?.budgetAmount
        _uiState.update {
            it.copy(
                showCategoryBudgetDialog = true,
                selectedCategoryForBudget = cat,
                budgetAmountInput = if (amount != null && amount > 0) {
                    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
                } else "",
                inputError = null
            )
        }
    }

    fun closeCategoryBudgetDialog() {
        _uiState.update { it.copy(showCategoryBudgetDialog = false) }
    }

    fun onBudgetAmountChange(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } > 1) return
        // Same input contract as the Add-Transaction amount field: at most two
        // decimals and a 9-digit integer part (caps at 999,999,999.99).
        val parts = filtered.split('.')
        if (parts.size == 2 && parts[1].length > 2) return
        if (parts[0].length > 9) return
        _uiState.update { it.copy(budgetAmountInput = filtered, inputError = null) }
    }

    fun onCategoryForBudgetSelected(category: CategoryEntity) {
        _uiState.update { it.copy(selectedCategoryForBudget = category) }
    }

    fun saveOverallBudget() {
        val amount = _uiState.value.budgetAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(inputError = "Enter an amount greater than 0") }
            return
        }

        viewModelScope.launch {
            // Read the existing budget straight from the database instead of the
            // (possibly not-yet-loaded) UI state. With id = 0 the insert would
            // silently create a SECOND overall-budget row — SQLite treats NULLs
            // as distinct in the unique index, so nothing would stop it, and
            // firstOrNull { categoryId == null } would then pick rows arbitrarily.
            val existing = financeRepository.getOverallBudget().firstOrNull()
            val budget = BudgetEntity(
                id = existing?.id ?: 0,
                categoryId = null,
                monthlyAmount = amount,
                month = 0,
                year = 0,
                isOverall = true
            )
            financeRepository.insertBudget(budget)
            _uiState.update { it.copy(showOverallBudgetDialog = false) }
        }
    }

    fun deleteOverallBudget() {
        val id = _uiState.value.overallBudget?.budgetId ?: return
        viewModelScope.launch {
            financeRepository.deleteBudgetById(id)
            _uiState.update { it.copy(showOverallBudgetDialog = false) }
        }
    }

    fun saveCategoryBudget() {
        val cat = _uiState.value.selectedCategoryForBudget ?: return
        val amount = _uiState.value.budgetAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(inputError = "Enter an amount greater than 0") }
            return
        }

        viewModelScope.launch {
            // DB-first upsert, same reasoning as saveOverallBudget: the UI state
            // may not list the category's budget yet (slow first load), and an
            // id = 0 insert would duplicate the row.
            val existing = financeRepository.getBudgetForCategory(cat.id).firstOrNull()
            val budget = BudgetEntity(
                id = existing?.id ?: 0,
                categoryId = cat.id,
                monthlyAmount = amount,
                month = 0,
                year = 0,
                isOverall = false
            )
            financeRepository.insertBudget(budget)
            _uiState.update { it.copy(showCategoryBudgetDialog = false) }
        }
    }

    fun deleteCategoryBudget(budgetId: Long) {
        viewModelScope.launch {
            financeRepository.deleteBudgetById(budgetId)
        }
    }

    /** Removes the budget of the category currently picked in the dialog, then closes it. */
    fun deleteCategoryBudgetFromDialog() {
        val cat = _uiState.value.selectedCategoryForBudget ?: return
        val existing = _uiState.value.categoryBudgets.firstOrNull { it.category.id == cat.id } ?: return
        viewModelScope.launch {
            financeRepository.deleteBudgetById(existing.budget.id)
            _uiState.update { it.copy(showCategoryBudgetDialog = false) }
        }
    }
}
