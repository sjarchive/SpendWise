package com.spendwise.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.relations.TransactionWithCategory
import com.spendwise.app.data.preferences.PreferencesManager
import com.spendwise.app.domain.repository.FinanceRepository
import com.spendwise.app.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val repository: FinanceRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesManager.themeMode
    val dynamicColor: StateFlow<Boolean> = preferencesManager.dynamicColor
    val currencySymbol: StateFlow<String> = preferencesManager.currencySymbol
    val currencyName: StateFlow<String> = preferencesManager.currencyName

    val allTransactions: StateFlow<List<TransactionWithCategory>> =
        repository.getTransactionsWithCategory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun setThemeMode(mode: ThemeMode) {
        preferencesManager.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        preferencesManager.setDynamicColor(enabled)
    }

    fun setCurrency(symbol: String, name: String) {
        preferencesManager.setCurrency(symbol, name)
    }
}
