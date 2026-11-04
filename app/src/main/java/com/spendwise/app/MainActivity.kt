package com.spendwise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.spendwise.app.ui.SpendWiseApp
import com.spendwise.app.ui.theme.SpendWiseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendWiseApp()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    com.spendwise.app.ui.SpendWiseAppContent(
        themeMode = com.spendwise.app.ui.theme.ThemeMode.SYSTEM,
        onThemeModeSelected = {},
        dynamicColorEnabled = true,
        onDynamicColorToggled = {},
        currencySymbol = "₹",
        currencyName = "Indian Rupee (₹)",
        onCurrencySelected = { _, _ -> },
        allTransactions = emptyList()
    )
}

