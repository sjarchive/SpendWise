package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Theme-aware variants of the fixed financial accent colors.
 *
 * The app previously hard-coded light pastel containers (mint / rose / amber)
 * with dark text on them, which clashed badly in dark mode. These helpers
 * blend the accent color over the current surface so the same code reads
 * correctly in both light and dark themes.
 */

/** Whether the currently applied app color scheme is dark (respects in-app theme override). */
@Composable
fun isAppInDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Container color for income pills / cards (blends over whatever surface it sits on). */
@Composable
fun incomeContainerColor(): Color =
    IncomeGreen.copy(alpha = if (isAppInDarkTheme()) 0.20f else 0.14f)

/** Container color for expense pills / cards. */
@Composable
fun expenseContainerColor(): Color =
    ExpenseRed.copy(alpha = if (isAppInDarkTheme()) 0.20f else 0.12f)

/** Income accent with enough contrast on dark surfaces. */
@Composable
fun incomeAccentColor(): Color =
    if (isAppInDarkTheme()) Color(0xFF34D399) else IncomeGreen

/** Expense accent with enough contrast on dark surfaces. */
@Composable
fun expenseAccentColor(): Color =
    if (isAppInDarkTheme()) Color(0xFFFB7185) else ExpenseRed

/** Amber-tinted container for budget warning cards. */
@Composable
fun amberContainerColor(): Color =
    WarningAmber.copy(alpha = if (isAppInDarkTheme()) 0.22f else 0.16f)

/** Readable amber text color on amber-tinted containers. */
@Composable
fun amberTextColor(): Color =
    if (isAppInDarkTheme()) Color(0xFFFCD34D) else Color(0xFFB45309)
