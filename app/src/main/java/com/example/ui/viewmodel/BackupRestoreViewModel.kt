package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.relations.TransactionWithCategory
import com.example.domain.model.TransactionType
import com.example.domain.repository.FinanceRepository
import com.example.ui.util.CsvImportHelper
import com.example.ui.util.CsvParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface BackupRestoreUiState {
    data object Idle : BackupRestoreUiState
    data object ReadingFile : BackupRestoreUiState

    @Immutable
    data class Preview(val result: CsvParseResult, val fileName: String) : BackupRestoreUiState

    data object Importing : BackupRestoreUiState

    @Immutable
    data class Success(val importedCount: Int) : BackupRestoreUiState

    @Immutable
    data class Error(val message: String) : BackupRestoreUiState
}

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    val allTransactions: StateFlow<List<TransactionWithCategory>> =
        repository.getTransactionsWithCategory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _uiState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    fun processCsvUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupRestoreUiState.ReadingFile

            withContext(Dispatchers.IO) {
                try {
                    var fileName = "transactions.csv"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex) ?: "transactions.csv"
                        }
                    }

                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        _uiState.value = BackupRestoreUiState.Error("Unable to open the selected file.")
                        return@withContext
                    }

                    val parseResult = inputStream.use { stream ->
                        CsvImportHelper.parseCsv(stream)
                    }

                    if (parseResult.validTransactions.isEmpty()) {
                        val errMsg = if (parseResult.parsingErrors.isNotEmpty()) {
                            parseResult.parsingErrors.first()
                        } else {
                            "No valid transactions found in file."
                        }
                        _uiState.value = BackupRestoreUiState.Error(errMsg)
                    } else {
                        _uiState.value = BackupRestoreUiState.Preview(parseResult, fileName)
                    }
                } catch (e: Exception) {
                    _uiState.value = BackupRestoreUiState.Error(e.message ?: "Failed to read CSV file.")
                }
            }
        }
    }

    fun confirmImport(previewResult: CsvParseResult) {
        viewModelScope.launch {
            _uiState.value = BackupRestoreUiState.Importing

            withContext(Dispatchers.IO) {
                try {
                    // 1. Ensure all categories exist, or insert missing ones
                    val existingCategories = repository.getAllCategories().first()
                    val categoryMap = mutableMapOf<Pair<String, TransactionType>, Long>()

                    for (cat in existingCategories) {
                        categoryMap[Pair(cat.name.lowercase(), cat.type)] = cat.id
                    }

                    for ((catName, catType) in previewResult.uniqueCategoryNames) {
                        val key = Pair(catName.lowercase(), catType)
                        if (!categoryMap.containsKey(key)) {
                            val newCat = CategoryEntity(
                                name = catName,
                                icon = if (catType == TransactionType.EXPENSE) "category" else "savings",
                                color = if (catType == TransactionType.EXPENSE) 0xFF64748BL else 0xFF22C55EL,
                                isDefault = false,
                                type = catType
                            )
                            val newId = repository.insertCategory(newCat)
                            categoryMap[key] = newId
                        }
                    }

                    // Fallback categories (Default Expense = 1 Food or 8 Other, Default Income = 9 Salary or 12 Other Income)
                    val defaultExpenseId = categoryMap[Pair("other", TransactionType.EXPENSE)]
                        ?: categoryMap[Pair("food", TransactionType.EXPENSE)]
                        ?: 1L
                    val defaultIncomeId = categoryMap[Pair("other income", TransactionType.INCOME)]
                        ?: categoryMap[Pair("salary", TransactionType.INCOME)]
                        ?: 9L

                    // 2. Prepare transaction entities
                    val entities = previewResult.validTransactions.map { item ->
                        val matchedId = categoryMap[Pair(item.categoryName.lowercase(), item.type)]
                            ?: if (item.type == TransactionType.EXPENSE) defaultExpenseId else defaultIncomeId

                        TransactionEntity(
                            id = 0, // Auto-generate new primary key
                            amount = item.amount,
                            type = item.type,
                            categoryId = matchedId,
                            category = item.categoryName,
                            dateTime = item.dateTime,
                            note = item.note,
                            paymentMethod = item.paymentMethod
                        )
                    }

                    repository.insertTransactions(entities)
                    _uiState.value = BackupRestoreUiState.Success(entities.size)
                } catch (e: Exception) {
                    _uiState.value = BackupRestoreUiState.Error(e.message ?: "Failed to import transactions.")
                }
            }
        }
    }

    fun dismissDialog() {
        _uiState.value = BackupRestoreUiState.Idle
    }
}
