package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.relations.TransactionWithCategory
import com.example.domain.model.TransactionType
import com.example.domain.repository.FinanceRepository
import com.example.ui.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class DateRangeFilter(val label: String) {
    ALL("All Time"),
    THIS_MONTH("This Month"),
    LAST_7_DAYS("Last 7 Days")
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
                        dateRange = state.selectedDateRange
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
                dateRange = state.selectedDateRange
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
                dateRange = state.selectedDateRange
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
                dateRange = state.selectedDateRange
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
                dateRange = dateRange
            )
            state.copy(selectedDateRange = dateRange, filteredTransactions = filtered)
        }
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
        dateRange: DateRangeFilter
    ): List<TransactionWithCategory> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)

        val (startThreshold, endThreshold) = when (dateRange) {
            DateRangeFilter.ALL -> Pair(0L, Long.MAX_VALUE)
            DateRangeFilter.THIS_MONTH -> DateUtils.getCurrentMonthRange()
            DateRangeFilter.LAST_7_DAYS -> {
                val start = now.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
                Pair(start, Long.MAX_VALUE)
            }
        }

        return transactions.filter { item ->
            val tx = item.transaction
            val cat = item.category

            val matchesSearch = if (search.isBlank()) true else {
                tx.note.contains(search, ignoreCase = true) ||
                tx.category.contains(search, ignoreCase = true) ||
                cat?.name?.contains(search, ignoreCase = true) == true ||
                tx.paymentMethod.displayName.contains(search, ignoreCase = true)
            }

            val matchesType = type == null || tx.type == type
            val matchesCategory = categoryId == null || tx.categoryId == categoryId
            val matchesDateRange = tx.dateTime in startThreshold..endThreshold

            matchesSearch && matchesType && matchesCategory && matchesDateRange
        }
    }
}
