package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.TransactionType
import com.example.domain.repository.FinanceRepository
import com.example.ui.util.CategoryIconHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedTabType: TransactionType = TransactionType.EXPENSE,
    val showAddEditDialog: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val nameInput: String = "",
    val selectedIconKey: String = "restaurant",
    val selectedColor: Long = 0xFFF97316L,
    val nameError: String? = null,
    val categoryToDelete: CategoryEntity? = null,
    val showDeleteConfirmDialog: Boolean = false
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            financeRepository.ensureDefaultCategoriesSeeded()

            // Render the FIRST frame with the full list: the app-scoped snapshot
            // (kept warm in FinanceRepositoryImpl.init) is applied synchronously
            // so the last rows are never clipped or reflowed in a beat later when
            // the Room emission lands (same pattern as AddEditTransactionViewModel).
            val snapshot = financeRepository.peekCategoriesSnapshot()
            if (!snapshot.isNullOrEmpty()) {
                _uiState.update { it.copy(isLoading = false, categories = snapshot) }
            }

            financeRepository.getAllCategories().collect { list ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        categories = list
                    )
                }
            }
        }
    }

    fun onTabTypeSelected(type: TransactionType) {
        _uiState.update {
            it.copy(
                selectedTabType = type,
                selectedIconKey = if (type == TransactionType.EXPENSE) "restaurant" else "payments"
            )
        }
    }

    fun openAddCategoryDialog() {
        val defaultIcon = if (_uiState.value.selectedTabType == TransactionType.EXPENSE) "shopping_bag" else "payments"
        val defaultColor = CategoryIconHelper.availableColors.first()
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingCategory = null,
                nameInput = "",
                selectedIconKey = defaultIcon,
                selectedColor = defaultColor,
                nameError = null
            )
        }
    }

    fun openEditCategoryDialog(category: CategoryEntity) {
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingCategory = category,
                nameInput = category.name,
                selectedIconKey = category.icon,
                selectedColor = category.color,
                nameError = null
            )
        }
    }

    fun closeAddEditDialog() {
        _uiState.update { it.copy(showAddEditDialog = false) }
    }

    fun onNameChange(name: String) {
        _uiState.update {
            it.copy(
                nameInput = name,
                nameError = null
            )
        }
    }

    fun onIconSelected(iconKey: String) {
        _uiState.update { it.copy(selectedIconKey = iconKey) }
    }

    fun onColorSelected(color: Long) {
        _uiState.update { it.copy(selectedColor = color) }
    }

    fun saveCategory() {
        val state = _uiState.value
        val trimmed = state.nameInput.trim()

        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(nameError = "Category name cannot be empty") }
            return
        }

        // Check for duplicates within same type (excluding self when editing)
        val isDuplicate = state.categories.any {
            it.name.equals(trimmed, ignoreCase = true) &&
            it.type == state.selectedTabType &&
            it.id != state.editingCategory?.id
        }

        if (isDuplicate) {
            _uiState.update { it.copy(nameError = "A category with this name already exists") }
            return
        }

        viewModelScope.launch {
            val editing = state.editingCategory
            if (editing != null) {
                // Update
                val updated = editing.copy(
                    name = trimmed,
                    icon = state.selectedIconKey,
                    color = state.selectedColor
                )
                financeRepository.updateCategory(updated)
            } else {
                // Add new custom category
                val newCategory = CategoryEntity(
                    id = 0,
                    name = trimmed,
                    icon = state.selectedIconKey,
                    color = state.selectedColor,
                    isDefault = false,
                    type = state.selectedTabType
                )
                financeRepository.insertCategory(newCategory)
            }

            _uiState.update { it.copy(showAddEditDialog = false) }
        }
    }

    fun requestDeleteCategory(category: CategoryEntity) {
        if (category.isDefault) return // Default categories cannot be deleted
        _uiState.update {
            it.copy(
                categoryToDelete = category,
                showDeleteConfirmDialog = true
            )
        }
    }

    fun cancelDelete() {
        _uiState.update {
            it.copy(
                categoryToDelete = null,
                showDeleteConfirmDialog = false
            )
        }
    }

    fun confirmDeleteCategory() {
        val target = _uiState.value.categoryToDelete ?: return
        viewModelScope.launch {
            // Reassign the category's transactions to a same-type default category
            // BEFORE deleting. Without this, the FK's SET_DEFAULT would silently
            // move them to categoryId = 1 ("Food", an EXPENSE category) — which
            // would corrupt INCOME transactions' categories.
            val fallbackName = if (target.type == TransactionType.INCOME) "Other Income" else "Other"
            val fallback = financeRepository.getCategoryByName(fallbackName)
                ?: financeRepository.getCategoriesByType(target.type).firstOrNull()?.firstOrNull()
            if (fallback != null && fallback.id != target.id) {
                financeRepository.reassignTransactionCategory(
                    oldCategoryId = target.id,
                    newCategoryId = fallback.id,
                    newCategoryName = fallback.name
                )
            }
            financeRepository.deleteCategory(target)
            _uiState.update {
                it.copy(
                    categoryToDelete = null,
                    showDeleteConfirmDialog = false
                )
            }
        }
    }
}
