package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.entity.relations.TransactionWithCategory
import com.example.ui.components.SpendWiseBottomBar
import com.example.ui.components.SpendWiseExtendedFab
import com.example.ui.navigation.Screen
import com.example.ui.navigation.SpendWiseNavGraph
import com.example.ui.theme.SpendWiseTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SpendWiseApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val currencyName by viewModel.currencyName.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    SpendWiseAppContent(
        themeMode = themeMode,
        onThemeModeSelected = viewModel::setThemeMode,
        dynamicColorEnabled = dynamicColor,
        onDynamicColorToggled = viewModel::setDynamicColor,
        currencySymbol = currencySymbol,
        currencyName = currencyName,
        onCurrencySelected = viewModel::setCurrency,
        allTransactions = allTransactions
    )
}

@Composable
fun SpendWiseAppContent(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorToggled: (Boolean) -> Unit,
    currencySymbol: String,
    currencyName: String,
    onCurrencySelected: (String, String) -> Unit,
    allTransactions: List<TransactionWithCategory> = emptyList()
) {
    SpendWiseTheme(
        themeMode = themeMode,
        dynamicColor = dynamicColorEnabled
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Determine if bottom bar and FAB should be shown
        val isTopLevelDestination = currentRoute in Screen.bottomNavItems.map { it.route }
        val showFab = currentRoute == Screen.Home.route || currentRoute == Screen.Transactions.route

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            bottomBar = {
                // Slide the bar away smoothly instead of removing it abruptly,
                // which used to make the whole layout jump.
                AnimatedVisibility(
                    visible = isTopLevelDestination,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                ) {
                    SpendWiseBottomBar(
                        currentRoute = currentRoute,
                        onNavigateToRoute = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                SpendWiseExtendedFab(
                    visible = showFab,
                    onClick = {
                        navController.navigate(Screen.AddTransaction.createRoute())
                    }
                )
            }
        ) { innerPadding ->
            SpendWiseNavGraph(
                navController = navController,
                currentThemeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
                dynamicColorEnabled = dynamicColorEnabled,
                onDynamicColorToggled = onDynamicColorToggled,
                currencySymbol = currencySymbol,
                currencyName = currencyName,
                onCurrencySelected = onCurrencySelected,
                allTransactions = allTransactions,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            )
        }
    }
}
