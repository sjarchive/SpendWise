package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.relations.TransactionWithCategory
import com.example.domain.model.TransactionType
import com.example.domain.repository.FinanceRepository
import com.example.ui.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val groupedTransactions: Map<String, List<TransactionWithCategory>> = emptyMap(),
    val currentMonthLabel: String = "",
    val errorMessage: String? = null,
    /**
     * Incremented only when transactions data actually changes (add / edit / delete),
     * so that the transaction amount counter runs only on data mutations, not when
     * switching between tabs.
     */
    val transactionsDataVersion: Long = 0L
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Guards against stacking duplicate infinite collectors if this is ever re-triggered. */
    private var dashboardJob: Job? = null

    init {
        val monthLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        _uiState.update { it.copy(currentMonthLabel = monthLabel) }
        loadDashboardData()
    }

    private fun loadDashboardData() {
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            financeRepository.ensureDefaultCategoriesSeeded()

            val (startOfMonth, endOfMonth) = DateUtils.getCurrentMonthRange()
            var previousTransactions: List<TransactionWithCategory>? = null

            financeRepository.getTransactionsWithCategory()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load transactions"
                        )
                    }
                }
                .collect { allTransactions ->
                    val currentMonthTransactions = allTransactions.filter {
                        it.transaction.dateTime in startOfMonth..endOfMonth
                    }

                    val income = currentMonthTransactions
                        .filter { it.transaction.type == TransactionType.INCOME }
                        .sumOf { it.transaction.amount }

                    val expense = currentMonthTransactions
                        .filter { it.transaction.type == TransactionType.EXPENSE }
                        .sumOf { it.transaction.amount }

                    val balance = income - expense

                    val recent = allTransactions.take(50)
                    val grouped = recent.groupBy { transactionWithCategory ->
                        DateUtils.formatDayHeader(transactionWithCategory.transaction.dateTime)
                    }

                    val isDataMutation = previousTransactions != null && previousTransactions != allTransactions
                    previousTransactions = allTransactions

                    _uiState.update { state ->
                        val newVersion = if (isDataMutation) {
                            state.transactionsDataVersion + 1
                        } else {
                            state.transactionsDataVersion
                        }
                        state.copy(
                            isLoading = false,
                            totalBalance = balance,
                            totalIncome = income,
                            totalExpense = expense,
                            recentTransactions = recent,
                            groupedTransactions = grouped,
                            transactionsDataVersion = newVersion,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
