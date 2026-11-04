package com.spendwise.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.domain.model.TransactionType
import com.spendwise.app.domain.repository.FinanceRepository
import com.spendwise.app.ui.components.charts.ChartDataPoint
import com.spendwise.app.ui.components.charts.DonutSlice
import com.spendwise.app.ui.components.charts.TrendPeriod
import com.spendwise.app.ui.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@Immutable
data class TopCategoryItem(
    val category: CategoryEntity,
    val amount: Double,
    val percentage: Float,
    val count: Int
)

@Immutable
data class InsightsUiState(
    val isLoading: Boolean = true,
    val totalExpense: Double = 0.0,
    val donutSlices: List<DonutSlice> = emptyList(),
    val topCategories: List<TopCategoryItem> = emptyList(),
    val trendPeriod: TrendPeriod = TrendPeriod.DAILY_7_DAYS,
    val trendDataPoints: List<ChartDataPoint> = emptyList(),
    val overallBudgetProgress: OverallBudgetProgress? = null,
    val categoryBudgets: List<CategoryBudgetProgress> = emptyList(),
    /**
     * Incremented only when transactions data actually changes (add / edit / delete),
     * so that the counters and charts run only on data mutations, not when
     * switching between tabs.
     */
    val transactionsDataVersion: Long = 0L
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _trendPeriod = MutableStateFlow(TrendPeriod.DAILY_7_DAYS)
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            financeRepository.ensureDefaultCategoriesSeeded()

            val (startOfMonth, endOfMonth) = DateUtils.getCurrentMonthRange()
            var previousTransactions: List<com.spendwise.app.data.local.entity.relations.TransactionWithCategory>? = null

            combine(
                financeRepository.getTransactionsBetween(startOfMonth, endOfMonth),
                financeRepository.getTransactionsWithCategory(),
                financeRepository.getAllCategories(),
                financeRepository.getAllBudgets(),
                _trendPeriod
            ) { monthTransactions, allTransactions, categories, budgets, period ->
                val isDataMutation = previousTransactions != null && previousTransactions != allTransactions
                previousTransactions = allTransactions

                val newVersion = if (isDataMutation) {
                    _uiState.value.transactionsDataVersion + 1
                } else {
                    _uiState.value.transactionsDataVersion
                }

                calculateAnalytics(
                    monthTransactions = monthTransactions,
                    allTransactions = allTransactions,
                    categories = categories,
                    budgets = budgets,
                    period = period,
                    dataVersion = newVersion
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onTrendPeriodChanged(period: TrendPeriod) {
        _trendPeriod.value = period
        _uiState.update { it.copy(trendPeriod = period) }
    }

    private fun calculateAnalytics(
        monthTransactions: List<com.spendwise.app.data.local.entity.relations.TransactionWithCategory>,
        allTransactions: List<com.spendwise.app.data.local.entity.relations.TransactionWithCategory>,
        categories: List<CategoryEntity>,
        budgets: List<com.spendwise.app.data.local.entity.BudgetEntity>,
        period: TrendPeriod,
        dataVersion: Long = 0L
    ): InsightsUiState {
        val monthExpenses = monthTransactions.filter { it.transaction.type == TransactionType.EXPENSE }
        val totalExpense = monthExpenses.sumOf { it.transaction.amount }

        // 1. Donut Slices & Top Categories
        val categoryExpenses = monthExpenses.groupBy { it.transaction.categoryId }
        val topList = mutableListOf<TopCategoryItem>()
        val slices = mutableListOf<DonutSlice>()

        categoryExpenses.forEach { (catId, txList) ->
            val category = categories.firstOrNull { it.id == catId } ?: CategoryEntity(
                id = catId,
                name = txList.firstOrNull()?.transaction?.category ?: "Other",
                icon = "category",
                color = 0xFF64748BL
            )
            val sum = txList.sumOf { it.transaction.amount }
            val percent = if (totalExpense > 0) ((sum / totalExpense) * 100f).toFloat() else 0f

            topList.add(
                TopCategoryItem(
                    category = category,
                    amount = sum,
                    percentage = percent,
                    count = txList.size
                )
            )

            slices.add(
                DonutSlice(
                    id = category.id,
                    name = category.name,
                    amount = sum,
                    color = Color(category.color),
                    percentage = percent
                )
            )
        }

        topList.sortByDescending { it.amount }
        slices.sortByDescending { it.amount }

        // 2. Trend Data Points (Daily vs Monthly)
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)
        val trendPoints = mutableListOf<ChartDataPoint>()

        if (period == TrendPeriod.DAILY_7_DAYS) {
            val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
            for (i in 6 downTo 0) {
                val targetDay = now.minusDays(i.toLong())
                val dayStart = targetDay.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = targetDay.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

                val daySum = allTransactions
                    .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.dateTime in dayStart..dayEnd }
                    .sumOf { it.transaction.amount }

                val label = if (i == 0) "Today" else targetDay.format(dayFormatter)
                trendPoints.add(ChartDataPoint(label = label, amount = daySum))
            }
        } else {
            // Monthly Trend (Last 6 Months)
            val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
            for (i in 5 downTo 0) {
                val ym = YearMonth.now(zone).minusMonths(i.toLong())
                val mStart = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val mEnd = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

                val monthSum = allTransactions
                    .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.dateTime in mStart..mEnd }
                    .sumOf { it.transaction.amount }

                trendPoints.add(ChartDataPoint(label = ym.format(monthFormatter), amount = monthSum))
            }
        }

        // 3. Budgets Progress with 80% and 100% warnings
        val overallBudgetEntity = budgets.firstOrNull { it.categoryId == null }
        val overallProgress = if (overallBudgetEntity != null && overallBudgetEntity.monthlyAmount > 0) {
            val percent = ((totalExpense / overallBudgetEntity.monthlyAmount) * 100f).toFloat()
            val warning = when {
                percent >= 100f -> BudgetWarningLevel.EXCEEDED_100
                percent >= 80f -> BudgetWarningLevel.APPROACHING_80
                else -> BudgetWarningLevel.NORMAL
            }
            OverallBudgetProgress(
                budgetId = overallBudgetEntity.id,
                targetAmount = overallBudgetEntity.monthlyAmount,
                spentAmount = totalExpense,
                remainingAmount = (overallBudgetEntity.monthlyAmount - totalExpense).coerceAtLeast(0.0),
                percentage = percent,
                warningLevel = warning
            )
        } else null

        val catBudgets = budgets.filter { it.categoryId != null }
        val catProgressList = catBudgets.mapNotNull { budget ->
            val cat = categories.firstOrNull { it.id == budget.categoryId } ?: return@mapNotNull null
            val spent = monthExpenses.filter { it.transaction.categoryId == cat.id }.sumOf { it.transaction.amount }
            val percent = if (budget.monthlyAmount > 0) ((spent / budget.monthlyAmount) * 100f).toFloat() else 0f
            val warning = when {
                percent >= 100f -> BudgetWarningLevel.EXCEEDED_100
                percent >= 80f -> BudgetWarningLevel.APPROACHING_80
                else -> BudgetWarningLevel.NORMAL
            }
            CategoryBudgetProgress(
                category = cat,
                budget = budget,
                spentAmount = spent,
                budgetAmount = budget.monthlyAmount,
                percentage = percent,
                warningLevel = warning
            )
        }

        return InsightsUiState(
            isLoading = false,
            totalExpense = totalExpense,
            donutSlices = slices,
            topCategories = topList,
            trendPeriod = period,
            trendDataPoints = trendPoints,
            overallBudgetProgress = overallProgress,
            categoryBudgets = catProgressList,
            transactionsDataVersion = dataVersion
        )
    }
}
