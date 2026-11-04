package com.spendwise.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.spendwise.app.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("spendwise_preferences", Context.MODE_PRIVATE)

    companion object {
        const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        const val KEY_CURRENCY_NAME = "currency_name"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"

        val availableCurrencies = listOf(
            CurrencyOption("INR", "₹", "Indian Rupee (₹)"),
            CurrencyOption("USD", "$", "US Dollar ($)"),
            CurrencyOption("EUR", "€", "Euro (€)"),
            CurrencyOption("GBP", "£", "British Pound (£)"),
            CurrencyOption("JPY", "¥", "Japanese Yen (¥)"),
            CurrencyOption("CAD", "C$", "Canadian Dollar (C$)"),
            CurrencyOption("AUD", "A$", "Australian Dollar (A$)"),
            CurrencyOption("AED", "AED", "UAE Dirham (AED)")
        )
    }

    private val _currencySymbol = MutableStateFlow(
        prefs.getString(KEY_CURRENCY_SYMBOL, "₹") ?: "₹"
    )
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _currencyName = MutableStateFlow(
        prefs.getString(KEY_CURRENCY_NAME, "Indian Rupee (₹)") ?: "Indian Rupee (₹)"
    )
    val currencyName: StateFlow<String> = _currencyName.asStateFlow()

    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(
        prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
    )
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    fun setCurrency(symbol: String, name: String) {
        prefs.edit()
            .putString(KEY_CURRENCY_SYMBOL, symbol)
            .putString(KEY_CURRENCY_NAME, name)
            .apply()
        _currencySymbol.value = symbol
        _currencyName.value = name
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
        _themeMode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_DYNAMIC_COLOR, enabled)
            .apply()
        _dynamicColor.value = enabled
    }
}
