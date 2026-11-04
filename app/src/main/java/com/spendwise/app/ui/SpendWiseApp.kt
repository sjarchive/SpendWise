package com.spendwise.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import com.spendwise.app.data.local.entity.relations.TransactionWithCategory
import com.spendwise.app.ui.components.SpendWiseBottomBar
import com.spendwise.app.ui.components.SpendWiseExtendedFab
import com.spendwise.app.ui.navigation.Screen
import com.spendwise.app.ui.navigation.SpendWiseNavGraph
import com.spendwise.app.ui.theme.SpendWiseTheme
import com.spendwise.app.ui.theme.ThemeMode
import com.spendwise.app.ui.viewmodel.MainViewModel

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
                // Slide-in smoothly when returning to a top-level tab; the exit is
                // intentionally instant (see below) so sub-screens get full height at once.
                AnimatedVisibility(
                    visible = isTopLevelDestination,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ),
                    // Exit must be INSTANT. An animated exit keeps the bar occupying
                    // layout space until the animation ends, so the incoming
                    // sub-screen starts with a shorter content area and its bottom
                    // content (e.g. the last category card) renders clipped for a
                    // moment before "popping" to full size once the bar is removed.
                    exit = ExitTransition.None
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
