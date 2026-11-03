package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AnimatedAmountText
import com.example.ui.components.AnimatedTransactionCount
import com.example.ui.components.charts.DonutChart
import com.example.ui.components.charts.TrendChart
import com.example.ui.theme.CardShape
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PillShape
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.amberContainerColor
import com.example.ui.theme.amberTextColor
import com.example.ui.util.CategoryIconHelper
import com.example.ui.util.DateUtils
import com.example.ui.viewmodel.BudgetWarningLevel
import com.example.ui.viewmodel.InsightsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    currencySymbol: String,
    onNavigateToBudgets: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("insights_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Financial Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: Monthly Overall Budget Alert Banner (if budget exists)
                if (uiState.overallBudgetProgress != null) {
                    val budget = uiState.overallBudgetProgress!!
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_insights_budget_banner"),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = when (budget.warningLevel) {
                                    BudgetWarningLevel.EXCEEDED_100 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    BudgetWarningLevel.APPROACHING_80 -> amberContainerColor()
                                    else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Monthly Budget",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TextButton(onClick = onNavigateToBudgets) {
                                        Text("View Limits", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AnimatedAmountText(
                                            amount = budget.spentAmount,
                                            currencySymbol = currencySymbol,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            dataVersion = uiState.transactionsDataVersion
                                        )
                                        Text(
                                            text = " spent of ${DateUtils.formatCurrency(budget.targetAmount, currencySymbol)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "${String.format("%.0f", budget.percentage)}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when (budget.warningLevel) {
                                            BudgetWarningLevel.EXCEEDED_100 -> ExpenseRed
                                            BudgetWarningLevel.APPROACHING_80 -> amberTextColor()
                                            BudgetWarningLevel.NORMAL -> IncomeGreen
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { (budget.percentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(PillShape),
                                    color = when (budget.warningLevel) {
                                        BudgetWarningLevel.EXCEEDED_100 -> ExpenseRed
                                        BudgetWarningLevel.APPROACHING_80 -> WarningAmber
                                        BudgetWarningLevel.NORMAL -> MaterialTheme.colorScheme.primary
                                    },
                                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )

                                if (budget.warningLevel != BudgetWarningLevel.NORMAL) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (budget.warningLevel == BudgetWarningLevel.EXCEEDED_100)
                                            "Monthly spending has exceeded the target budget!"
                                        else
                                            "80%+ of the target budget consumed this month.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (budget.warningLevel == BudgetWarningLevel.EXCEEDED_100) ExpenseRed else amberTextColor(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Donut Chart (Spending by Category)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_category_spending_donut"),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Spending by Category",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Current Month Breakdown",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            DonutChart(
                                slices = uiState.donutSlices,
                                totalAmount = uiState.totalExpense,
                                currencySymbol = currencySymbol,
                                dataVersion = uiState.transactionsDataVersion
                            )
                        }
                    }
                }

                // Section 3: Trend Chart (Daily and Monthly Trends via Compose Canvas)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Spending Trends",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TrendChart(
                            dataPoints = uiState.trendDataPoints,
                            selectedPeriod = uiState.trendPeriod,
                            onPeriodSelected = viewModel::onTrendPeriodChanged,
                            currencySymbol = currencySymbol,
                            dataVersion = uiState.transactionsDataVersion
                        )
                    }
                }

                // Section 4: Top Categories Breakdown
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Expense Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (uiState.topCategories.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Text(
                                text = "No category data available yet. Start logging expenses to see category rankings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(uiState.topCategories, key = { it.category.id }) { item ->
                        val catColor = Color(item.category.color)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("top_category_item_${item.category.name}"),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(catColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getIcon(item.category.icon),
                                                contentDescription = item.category.name,
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = item.category.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AnimatedTransactionCount(
                                                    count = item.count,
                                                    dataVersion = uiState.transactionsDataVersion,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = " • ${String.format(java.util.Locale.US, "%.1f", item.percentage)}%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    AnimatedAmountText(
                                        amount = item.amount,
                                        currencySymbol = currencySymbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        dataVersion = uiState.transactionsDataVersion
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(PillShape),
                                    color = catColor,
                                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
