package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.PaymentMethod
import com.example.domain.model.TransactionType
import com.example.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AddEditTransactionUiState(
    val isEditMode: Boolean = false,
    val transactionId: Long = -1L,
    val amountText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val availableCategories: List<CategoryEntity> = emptyList(),
    val selectedCategory: CategoryEntity? = null,
    val selectedDateTime: Long = System.currentTimeMillis(),
    val note: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    /**
     * Active collector for the currently selected transaction type. Switching
     * type cancels the previous collection so stale collectors for the other
     * type cannot keep running and overwrite [AddEditTransactionUiState.availableCategories]
     * / [AddEditTransactionUiState.selectedCategory] with the wrong type's data.
     */
    private var categoriesJob: Job? = null

    init {
        val transactionId = savedStateHandle.get<Long>("transactionId") ?: -1L
        loadInitialData(transactionId)
    }

    private fun loadInitialData(transactionId: Long) {
        viewModelScope.launch {
            financeRepository.ensureDefaultCategoriesSeeded()

            if (transactionId > 0) {
                val existing = financeRepository.getTransactionWithCategoryById(transactionId).firstOrNull()
                if (existing != null) {
                    val tx = existing.transaction
                    _uiState.update {
                        it.copy(
                            isEditMode = true,
                            transactionId = tx.id,
                            amountText = if (tx.amount % 1.0 == 0.0) tx.amount.toLong().toString() else tx.amount.toString(),
                            type = tx.type,
                            selectedDateTime = tx.dateTime,
                            note = tx.note,
                            paymentMethod = tx.paymentMethod,
                            selectedCategory = existing.category
                        )
                    }
                }
            }

            loadCategoriesForType(_uiState.value.type)
        }
    }

    fun onTypeChange(newType: TransactionType) {
        if (_uiState.value.type == newType) return
        _uiState.update {
            it.copy(
                type = newType,
                selectedCategory = null,
                categoryError = null
            )
        }
        loadCategoriesForType(newType)
    }

    private fun loadCategoriesForType(type: TransactionType) {
        categoriesJob?.cancel()
        // Apply the warm in-process snapshot (if the repository has finished
        // its first DB read) synchronously so the first frame already shows
        // the final chip layout — without this, the chips pop in once Room's
        // async emission lands and shove the note field and other content
        // below the category row down the screen. The live query below then
        // takes over and keeps the list fresh.
        applyCategories(
            financeRepository.peekCategoriesSnapshot()
                ?.filter { it.type == type }
                .orEmpty()
        )
        categoriesJob = viewModelScope.launch {
            financeRepository.getCategoriesByType(type).collect { categories ->
                applyCategories(categories)
            }
        }
    }

    private fun applyCategories(categories: List<CategoryEntity>) {
        _uiState.update { state ->
            val currentSelection = state.selectedCategory
            val newSelection = if (currentSelection != null && categories.any { it.id == currentSelection.id }) {
                currentSelection
            } else {
                categories.firstOrNull()
            }
            state.copy(
                availableCategories = categories,
                selectedCategory = newSelection
            )
        }
    }

    fun onAmountChange(rawAmount: String) {
        // Allow only digits and up to one decimal point with 2 decimals
        val sanitized = rawAmount.filter { it.isDigit() || it == '.' }
        if (sanitized.count { it == '.' } > 1) return

        val parts = sanitized.split('.')
        if (parts.size == 2 && parts[1].length > 2) return

        _uiState.update {
            it.copy(
                amountText = sanitized,
                amountError = null
            )
        }
    }

    fun onCategorySelected(category: CategoryEntity) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                categoryError = null
            )
        }
    }

    fun onDateTimeSelected(timestamp: Long) {
        _uiState.update {
            it.copy(
                selectedDateTime = timestamp
            )
        }
    }

    fun onNoteChange(newNote: String) {
        if (newNote.length <= 150) {
            _uiState.update { it.copy(note = newNote) }
        }
    }

    fun onPaymentMethodSelected(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun setShowDatePicker(show: Boolean) {
        _uiState.update { it.copy(showDatePicker = show) }
    }

    fun setShowTimePicker(show: Boolean) {
        _uiState.update { it.copy(showTimePicker = show) }
    }

    fun setShowDeleteConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmDialog = show) }
    }

    fun saveTransaction() {
        // Guard against double-taps before the saving state has been applied.
        val current = _uiState.value
        if (current.isSaving || current.isSaved) return

        val state = _uiState.value
        val amount = state.amountText.toDoubleOrNull()

        var hasError = false
        var amountErrorMsg: String? = null
        var categoryErrorMsg: String? = null

        if (amount == null || amount <= 0.0) {
            amountErrorMsg = "Please enter a valid amount greater than 0"
            hasError = true
        }

        if (state.selectedCategory == null) {
            categoryErrorMsg = "Please select a category"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    amountError = amountErrorMsg,
                    categoryError = categoryErrorMsg
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val category = state.selectedCategory!!

            val transactionEntity = TransactionEntity(
                id = if (state.isEditMode) state.transactionId else 0,
                amount = amount!!,
                type = state.type,
                categoryId = category.id,
                category = category.name,
                dateTime = state.selectedDateTime,
                note = state.note.trim(),
                paymentMethod = state.paymentMethod
            )

            if (state.isEditMode) {
                financeRepository.updateTransaction(transactionEntity)
            } else {
                financeRepository.insertTransaction(transactionEntity)
            }

            _uiState.update {
                it.copy(
                    isSaving = false,
                    isSaved = true
                )
            }
        }
    }

    fun deleteTransaction() {
        val state = _uiState.value
        if (!state.isEditMode || state.transactionId <= 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteConfirmDialog = false) }
            financeRepository.deleteTransactionById(state.transactionId)
            _uiState.update {
                it.copy(
                    isDeleting = false,
                    isDeleted = true
                )
            }
        }
    }
}
