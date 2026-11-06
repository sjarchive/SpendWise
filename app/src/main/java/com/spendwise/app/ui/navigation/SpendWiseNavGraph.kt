package com.spendwise.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.spendwise.app.data.local.entity.relations.TransactionWithCategory
import com.spendwise.app.ui.screens.AddEditTransactionScreen
import com.spendwise.app.ui.screens.BackupRestoreScreen
import com.spendwise.app.ui.screens.BudgetsScreen
import com.spendwise.app.ui.screens.CategoriesManagementScreen
import com.spendwise.app.ui.screens.HomeScreen
import com.spendwise.app.ui.screens.InsightsScreen
import com.spendwise.app.ui.screens.SettingsScreen
import com.spendwise.app.ui.screens.TransactionsScreen
import com.spendwise.app.ui.theme.ThemeMode

private fun getTabIndex(route: String?): Int {
    if (route == null) return -1
    return when {
        route.startsWith(Screen.Home.route) -> 0
        route.startsWith(Screen.Transactions.route) -> 1
        route.startsWith(Screen.Insights.route) -> 2
        route.startsWith(Screen.Settings.route) -> 3
        else -> -1
    }
}

/**
 * Tab-switch enter animation: Left or right slide based on direction (only for bottom 4 tabs)
 */
private fun tabSlideIn(fromRight: Boolean): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> if (fromRight) fullWidth else -fullWidth },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

/**
 * Tab-switch exit animation: Left or right slide based on direction (only for bottom 4 tabs)
 */
private fun tabSlideOut(toLeft: Boolean): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> if (toLeft) -fullWidth else fullWidth },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeOut(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

/**
 * Fade-only enter transition for all menus, buttons, options and sub-screens (no left/right slide)
 */
private fun fadeEnter(): EnterTransition =
    fadeIn(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

/**
 * Fade-only exit transition for all menus, buttons, options and sub-screens (no left/right slide)
 */
private fun fadeExit(): ExitTransition =
    fadeOut(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

@Composable
fun SpendWiseNavGraph(
    navController: NavHostController,
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorToggled: (Boolean) -> Unit,
    currencySymbol: String,
    currencyName: String,
    onCurrencySelected: (String, String) -> Unit,
    allTransactions: List<TransactionWithCategory> = emptyList(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1) {
                tabSlideIn(fromRight = targetIndex > initialIndex)
            } else {
                fadeEnter()
            }
        },
        exitTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1) {
                tabSlideOut(toLeft = targetIndex > initialIndex)
            } else {
                fadeExit()
            }
        },
        popEnterTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1) {
                tabSlideIn(fromRight = targetIndex > initialIndex)
            } else {
                fadeEnter()
            }
        },
        popExitTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1) {
                tabSlideOut(toLeft = targetIndex > initialIndex)
            } else {
                fadeExit()
            }
        }
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                currencySymbol = currencySymbol,
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.AddTransaction.createRoute(transactionId))
                }
            )
        }

        composable(route = Screen.Transactions.route) {
            TransactionsScreen(
                currencySymbol = currencySymbol,
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.AddTransaction.createRoute(transactionId))
                }
            )
        }

        composable(route = Screen.Insights.route) {
            InsightsScreen(
                currencySymbol = currencySymbol,
                onNavigateToBudgets = {
                    navController.navigate(Screen.Budgets.route)
                }
            )
        }

        composable(route = Screen.Budgets.route) {
            BudgetsScreen(
                currencySymbol = currencySymbol,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                currentThemeMode = currentThemeMode,
                onThemeModeSelected = onThemeModeSelected,
                dynamicColorEnabled = dynamicColorEnabled,
                onDynamicColorToggled = onDynamicColorToggled,
                currentCurrencySymbol = currencySymbol,
                currentCurrencyName = currencyName,
                onCurrencySelected = onCurrencySelected,
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                },
                onNavigateToBackupRestore = {
                    navController.navigate(Screen.BackupRestore.route)
                },
                transactions = allTransactions
            )
        }

        composable(route = Screen.BackupRestore.route) {
            BackupRestoreScreen(
                currencySymbol = currencySymbol,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Categories.route) {
            CategoriesManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            AddEditTransactionScreen(
                currencySymbol = currencySymbol,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
