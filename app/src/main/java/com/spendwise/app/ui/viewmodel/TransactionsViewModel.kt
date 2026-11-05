package com.spendwise.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.local.entity.relations.TransactionWithCategory
import com.spendwise.app.domain.model.TransactionType
import com.spendwise.app.domain.repository.FinanceRepository
import com.spendwise.app.ui.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

enum class DateRangeFilter(val label: String) {
    ALL("All Time"),
    THIS_MONTH("This Month"),
    LAST_7_DAYS("Last 7 Days")
}

/** Sort orders for the transactions list. */
enum class TransactionSortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST("Highest Amount"),
    LOWEST("Lowest Amount")
}

@Immutable
data class TransactionsUiState(
    val isLoading: Boolean = true,
    val allTransactions: List<TransactionWithCategory> = emptyList(),
    val filteredTransactions: List<TransactionWithCategory> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedCategoryId: Long? = null,
    val selectedDateRange: DateRangeFilter = DateRangeFilter.ALL,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.NEWEST,
    val recentlyDeletedTransaction: TransactionEntity? = null,
    /**
     * Incremented only when the underlying transaction data actually changes
     * (add / update / delete), so the UI can animate on data changes alone
     * instead of re-animating on filter changes or screen re-entries.
     */
    val transactionsDataVersion: Long = 0L
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            financeRepository.ensureDefaultCategoriesSeeded()

            combine(
                financeRepository.getTransactionsWithCategory(),
                financeRepository.getAllCategories()
            ) { transactions, categories ->
                Pair(transactions, categories)
            }.collect { (transactions, categories) ->
                _uiState.update { state ->
                    val filtered = applyFilters(
                        transactions = transactions,
                        search = state.searchQuery,
                        type = state.selectedType,
                        categoryId = state.selectedCategoryId,
                        dateRange = state.selectedDateRange,
                        sortOrder = state.sortOrder
                    )
                    val dataVersion = if (transactions != state.allTransactions) {
                        // Room re-emitted with new transaction content (a write
                        // happened). Categories-only emissions keep the same count.
                        state.transactionsDataVersion + 1
                    } else {
                        state.transactionsDataVersion
                    }
                    state.copy(
                        isLoading = false,
                        allTransactions = transactions,
                        availableCategories = categories,
                        filteredTransactions = filtered,
                        transactionsDataVersion = dataVersion
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = applyFilters(
                transactions = state.allTransactions,
                search = query,
                type = state.selectedType,
                categoryId = state.selectedCategoryId,
                dateRange = state.selectedDateRange,
                sortOrder = state.sortOrder
            )
            state.copy(searchQuery = query, filteredTransactions = filtered)
        }
    }

    fun onTypeSelected(type: TransactionType?) {
        _uiState.update { state ->
            val newType = if (state.selectedType == type) null else type
            val filtered = applyFilters(
                transactions = state.allTransactions,
                search = state.searchQuery,
                type = newType,
                categoryId = state.selectedCategoryId,
                dateRange = state.selectedDateRange,
                sortOrder = state.sortOrder
            )
            state.copy(selectedType = newType, filteredTransactions = filtered)
        }
    }

    fun onCategorySelected(categoryId: Long?) {
        _uiState.update { state ->
            val newCatId = if (state.selectedCategoryId == categoryId) null else categoryId
            val filtered = applyFilters(
                transactions = state.allTransactions,
                search = state.searchQuery,
                type = state.selectedType,
                categoryId = newCatId,
                dateRange = state.selectedDateRange,
                sortOrder = state.sortOrder
            )
            state.copy(selectedCategoryId = newCatId, filteredTransactions = filtered)
        }
    }

    fun onDateRangeSelected(dateRange: DateRangeFilter) {
        _uiState.update { state ->
            val filtered = applyFilters(
                transactions = state.allTransactions,
                search = state.searchQuery,
                type = state.selectedType,
                categoryId = state.selectedCategoryId,
                dateRange = dateRange,
                sortOrder = state.sortOrder
            )
            state.copy(selectedDateRange = dateRange, filteredTransactions = filtered)
        }
    }

    fun onSortOrderSelected(sortOrder: TransactionSortOrder) {
        _uiState.update { state ->
            val filtered = applyFilters(
                transactions = state.allTransactions,
                search = state.searchQuery,
                type = state.selectedType,
                categoryId = state.selectedCategoryId,
                dateRange = state.selectedDateRange,
                sortOrder = sortOrder
            )
            state.copy(sortOrder = sortOrder, filteredTransactions = filtered)
        }
    }

    /** Cycles Newest → Oldest → Highest → Lowest for the compact sort button. */
    fun onSortCycle() {
        val current = _uiState.value.sortOrder
        val next = TransactionSortOrder.entries[(current.ordinal + 1) % TransactionSortOrder.entries.size]
        onSortOrderSelected(next)
    }

    fun deleteTransaction(
        transactionWithCategory: TransactionWithCategory,
        onShowUndo: (TransactionEntity) -> Unit
    ) {
        val tx = transactionWithCategory.transaction
        viewModelScope.launch {
            financeRepository.deleteTransaction(tx)
            _uiState.update { it.copy(recentlyDeletedTransaction = tx) }
            onShowUndo(tx)
        }
    }

    fun undoDelete() {
        val deleted = _uiState.value.recentlyDeletedTransaction ?: return
        viewModelScope.launch {
            financeRepository.insertTransaction(deleted)
            _uiState.update { it.copy(recentlyDeletedTransaction = null) }
        }
    }

    private fun applyFilters(
        transactions: List<TransactionWithCategory>,
        search: String,
        type: TransactionType?,
        categoryId: Long?,
        dateRange: DateRangeFilter,
        sortOrder: TransactionSortOrder
    ): List<TransactionWithCategory> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)

        val (startThreshold, endThreshold) = when (dateRange) {
            DateRangeFilter.ALL -> Pair(0L, Long.MAX_VALUE)
            DateRangeFilter.THIS_MONTH -> DateUtils.getCurrentMonthRange()
            DateRangeFilter.LAST_7_DAYS -> {
                // Today counts as day 1: minusDays(6) yields exactly 7 calendar
                // days (today + the 6 before it). The old minusDays(7) silently
                // included an 8th day.
                val start = now.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
                Pair(start, Long.MAX_VALUE)
            }
        }

        val filtered = transactions.filter { item ->
            val tx = item.transaction
            val cat = item.category

            val matchesSearch = if (search.isBlank()) true else {
                val amountText = tx.amount.toString()
                val amountText2 = String.format(Locale.US, "%.2f", tx.amount)
                tx.note.contains(search, ignoreCase = true) ||
                tx.category.contains(search, ignoreCase = true) ||
                cat?.name?.contains(search, ignoreCase = true) == true ||
                tx.paymentMethod.displayName.contains(search, ignoreCase = true) ||
                // Amount matching: "500" finds ₹500.00 and ₹1,500.50 alike.
                amountText.contains(search, ignoreCase = true) ||
                amountText2.contains(search, ignoreCase = true)
            }

            val matchesType = type == null || tx.type == type
            val matchesCategory = categoryId == null || tx.categoryId == categoryId
            val matchesDateRange = tx.dateTime in startThreshold..endThreshold

            matchesSearch && matchesType && matchesCategory && matchesDateRange
        }

        return when (sortOrder) {
            TransactionSortOrder.NEWEST -> filtered.sortedByDescending { it.transaction.dateTime }
            TransactionSortOrder.OLDEST -> filtered.sortedBy { it.transaction.dateTime }
            TransactionSortOrder.HIGHEST -> filtered.sortedByDescending { it.transaction.amount }
            TransactionSortOrder.LOWEST -> filtered.sortedBy { it.transaction.amount }
        }
    }
}
