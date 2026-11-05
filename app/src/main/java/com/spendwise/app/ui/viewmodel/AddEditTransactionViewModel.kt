package com.spendwise.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.domain.model.PaymentMethod
import com.spendwise.app.domain.model.TransactionType
import com.spendwise.app.domain.repository.FinanceRepository
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
    val showTimePicker: Boolean = false,
    /**
     * True when the user has edited any field away from the loaded values.
     * The screen uses it to warn before an accidental back-press discards
     * a half-filled form. Auto-selected fields (e.g. the default category in
     * add mode) never count as changes.
     */
    val hasUnsavedChanges: Boolean = false
)

/**
 * The field values captured when the form finished loading. `categoryId == null`
 * means the category selection is not tracked (add mode auto-selects one).
 */
private data class DraftSnapshot(
    val amountText: String,
    val note: String,
    val type: TransactionType,
    val paymentMethod: PaymentMethod,
    val selectedDateTime: Long,
    val categoryId: Long?
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

    /** Loaded form values; null until loadInitialData finishes. */
    private var draftSnapshot: DraftSnapshot? = null

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

            // Capture the pristine form now that the edit-mode row (if any) is
            // loaded. In add mode the category is auto-selected later, so it is
            // not tracked — only genuine user edits set hasUnsavedChanges.
            snapshotDraft()
        }
    }

    private fun snapshotDraft() {
        val s = _uiState.value
        draftSnapshot = DraftSnapshot(
            amountText = s.amountText,
            note = s.note,
            type = s.type,
            paymentMethod = s.paymentMethod,
            selectedDateTime = s.selectedDateTime,
            categoryId = if (s.isEditMode) s.selectedCategory?.id else null
        )
    }

    private fun isDirty(state: AddEditTransactionUiState): Boolean {
        val snap = draftSnapshot ?: return false
        if (state.amountText != snap.amountText) return true
        if (state.note != snap.note) return true
        if (state.type != snap.type) return true
        if (state.paymentMethod != snap.paymentMethod) return true
        if (state.selectedDateTime != snap.selectedDateTime) return true
        if (snap.categoryId != null && state.selectedCategory?.id != snap.categoryId) return true
        return false
    }

    /** Applies a state transformation and refreshes the dirty flag in one place. */
    private fun mutate(transform: (AddEditTransactionUiState) -> AddEditTransactionUiState) {
        _uiState.update { state ->
            val next = transform(state)
            next.copy(hasUnsavedChanges = isDirty(next))
        }
    }

    fun onTypeChange(newType: TransactionType) {
        if (_uiState.value.type == newType) return
        mutate {
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

        // Cap the integer part at 9 digits (₹999,999,999.99). Without this a
        // pasted 20-digit string parsed to 1e20 and broke every downstream
        // display / stat.
        if (parts[0].length > 9) return

        mutate {
            it.copy(
                amountText = sanitized,
                amountError = null
            )
        }
    }

    fun onCategorySelected(category: CategoryEntity) {
        mutate {
            it.copy(
                selectedCategory = category,
                categoryError = null
            )
        }
    }

    fun onDateTimeSelected(timestamp: Long) {
        mutate {
            it.copy(
                selectedDateTime = timestamp
            )
        }
    }

    fun onNoteChange(newNote: String) {
        if (newNote.length <= 150) {
            mutate { it.copy(note = newNote) }
        }
    }

    fun onPaymentMethodSelected(method: PaymentMethod) {
        mutate { it.copy(paymentMethod = method) }
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
