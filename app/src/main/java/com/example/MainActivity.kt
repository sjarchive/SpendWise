package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.SpendWiseApp
import com.example.ui.theme.SpendWiseTheme
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
    com.example.ui.SpendWiseAppContent(
        themeMode = com.example.ui.theme.ThemeMode.SYSTEM,
        onThemeModeSelected = {},
        dynamicColorEnabled = true,
        onDynamicColorToggled = {},
        currencySymbol = "₹",
        currencyName = "Indian Rupee (₹)",
        onCurrencySelected = { _, _ -> },
        allTransactions = emptyList()
    )
}

