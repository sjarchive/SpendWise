package com.spendwise.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendwise.app.ui.theme.CardShape
import com.spendwise.app.ui.theme.ExpenseRed
import com.spendwise.app.ui.theme.IncomeGreen
import com.spendwise.app.ui.theme.PillShape
import com.spendwise.app.ui.theme.WarningAmber
import com.spendwise.app.ui.theme.amberContainerColor
import com.spendwise.app.ui.theme.amberTextColor
import com.spendwise.app.ui.util.CategoryIconHelper
import com.spendwise.app.ui.util.DateUtils
import com.spendwise.app.ui.viewmodel.BudgetWarningLevel
import com.spendwise.app.ui.viewmodel.BudgetsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    currencySymbol: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("budgets_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Budgets & Limits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("btn_back_budgets")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Overall Monthly Budget Card
                item {
                    val overall = uiState.overallBudget
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openOverallBudgetDialog() }
                            .testTag("card_overall_budget"),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = when (overall?.warningLevel) {
                                BudgetWarningLevel.EXCEEDED_100 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                BudgetWarningLevel.APPROACHING_80 -> amberContainerColor()
                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Monthly Overall Budget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current Month Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (overall != null && overall.targetAmount > 0) {
                                // Progress Info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text(
                                            text = "Spent",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = DateUtils.formatCurrency(overall.spentAmount, currencySymbol),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Target: ${DateUtils.formatCurrency(overall.targetAmount, currencySymbol)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Remaining: ${DateUtils.formatCurrency(overall.remainingAmount, currencySymbol)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (overall.warningLevel == BudgetWarningLevel.EXCEEDED_100) ExpenseRed else IncomeGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val barColor by animateColorAsState(
                                    targetValue = when (overall.warningLevel) {
                                        BudgetWarningLevel.EXCEEDED_100 -> ExpenseRed
                                        BudgetWarningLevel.APPROACHING_80 -> WarningAmber
                                        BudgetWarningLevel.NORMAL -> MaterialTheme.colorScheme.primary
                                    },
                                    label = "budgetBarColor"
                                )

                                LinearProgressIndicator(
                                    progress = { (overall.percentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(PillShape),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Warning Badge Indicator
                                Surface(
                                    shape = PillShape,
                                    color = when (overall.warningLevel) {
                                        BudgetWarningLevel.EXCEEDED_100 -> ExpenseRed.copy(alpha = 0.15f)
                                        BudgetWarningLevel.APPROACHING_80 -> WarningAmber.copy(alpha = 0.15f)
                                        BudgetWarningLevel.NORMAL -> IncomeGreen.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Status icon instead of emoji glyphs, which render
                                        // inconsistently across devices and fonts.
                                        Icon(
                                            imageVector = when (overall.warningLevel) {
                                                BudgetWarningLevel.EXCEEDED_100 -> Icons.Default.Error
                                                BudgetWarningLevel.APPROACHING_80 -> Icons.Default.Warning
                                                BudgetWarningLevel.NORMAL -> Icons.Default.CheckCircle
                                            },
                                            contentDescription = null,
                                            tint = when (overall.warningLevel) {
                                                BudgetWarningLevel.EXCEEDED_100 -> ExpenseRed
                                                BudgetWarningLevel.APPROACHING_80 -> WarningAmber
                                                BudgetWarningLevel.NORMAL -> IncomeGreen
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (overall.warningLevel) {
                                                BudgetWarningLevel.EXCEEDED_100 -> "${String.format("%.0f", overall.percentage)}% Used — Budget Exceeded!"
                                                BudgetWarningLevel.APPROACHING_80 -> "${String.format("%.0f", overall.percentage)}% Used — Approaching 80% Limit"
                                                BudgetWarningLevel.NORMAL -> "${String.format("%.0f", overall.percentage)}% Used — On Track"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (overall.warningLevel) {
                                                BudgetWarningLevel.EXCEEDED_100 -> ExpenseRed
                                                BudgetWarningLevel.APPROACHING_80 -> amberTextColor()
                                                BudgetWarningLevel.NORMAL -> IncomeGreen
                                            }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No overall monthly budget set. Set a budget to track total spending and get alerts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = viewModel::openOverallBudgetDialog,
                                    shape = PillShape,
                                    modifier = Modifier.testTag("btn_set_overall_budget")
                                ) {
                                    Text("Set Monthly Budget")
                                }
                            }
                        }
                    }
                }

                // Section 2: Category Budgets Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Budgets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { viewModel.openCategoryBudgetDialog(null) },
                            modifier = Modifier.testTag("btn_new_category_budget")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Budget")
                        }
                    }
                }

                // Category Budgets List
                if (uiState.categoryBudgets.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No category budgets configured yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Set individual limits for Food, Transport, Shopping or Bills to manage expenses efficiently.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.categoryBudgets, key = { it.budget.id }) { catBudget ->
                        val cat = catBudget.category
                        val catColor = Color(cat.color)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cat_budget_card_${cat.name}"),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = when (catBudget.warningLevel) {
                                    BudgetWarningLevel.EXCEEDED_100 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                    BudgetWarningLevel.APPROACHING_80 -> amberContainerColor()
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(catColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getIcon(cat.icon),
                                                contentDescription = cat.name,
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${DateUtils.formatCurrency(catBudget.spentAmount, currencySymbol)} of ${DateUtils.formatCurrency(catBudget.budgetAmount, currencySymbol)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.openCategoryBudgetDialog(catBudget) },
                                            modifier = Modifier.testTag("btn_edit_budget_${cat.name}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit ${cat.name} budget",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteCategoryBudget(catBudget.budget.id) },
                                            modifier = Modifier.testTag("btn_delete_budget_${cat.name}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete ${cat.name} budget",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val barColor = when (catBudget.warningLevel) {
                                    BudgetWarningLevel.EXCEEDED_100 -> ExpenseRed
                                    BudgetWarningLevel.APPROACHING_80 -> WarningAmber
                                    BudgetWarningLevel.NORMAL -> catColor
                                }

                                LinearProgressIndicator(
                                    progress = { (catBudget.percentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(PillShape),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (catBudget.warningLevel) {
                                            BudgetWarningLevel.EXCEEDED_100 -> "100%+ — Budget exceeded!"
                                            BudgetWarningLevel.APPROACHING_80 -> "80%+ — Approaching limit"
                                            BudgetWarningLevel.NORMAL -> "On track"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (catBudget.warningLevel) {
                                            BudgetWarningLevel.EXCEEDED_100 -> ExpenseRed
                                            BudgetWarningLevel.APPROACHING_80 -> amberTextColor()
                                            BudgetWarningLevel.NORMAL -> IncomeGreen
                                        }
                                    )
                                    Text(
                                        text = "${String.format("%.0f", catBudget.percentage)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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

    // Dialog 1: Set/Edit Overall Budget Dialog
    if (uiState.showOverallBudgetDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closeOverallBudgetDialog,
            title = { Text("Monthly Overall Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter total monthly spending target across all categories:")
                    OutlinedTextField(
                        value = uiState.budgetAmountInput,
                        onValueChange = viewModel::onBudgetAmountChange,
                        label = { Text("Target Amount ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_overall_budget_amount")
                    )
                    if (uiState.inputError != null) {
                        Text(
                            text = uiState.inputError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::saveOverallBudget,
                    modifier = Modifier.testTag("btn_confirm_overall_budget")
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                Row {
                    if (uiState.overallBudget != null) {
                        TextButton(
                            onClick = viewModel::deleteOverallBudget,
                            modifier = Modifier.testTag("btn_delete_overall_budget")
                        ) {
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = viewModel::closeOverallBudgetDialog) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Dialog 2: Set/Edit Category Budget Dialog
    if (uiState.showCategoryBudgetDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closeCategoryBudgetDialog,
            title = { Text("Set Category Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select expense category:")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.availableExpenseCategories.forEach { cat ->
                            val isSelected = uiState.selectedCategoryForBudget?.id == cat.id
                            Surface(
                                shape = PillShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(PillShape)
                                    .clickable { viewModel.onCategoryForBudgetSelected(cat) }
                            ) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = uiState.budgetAmountInput,
                        onValueChange = viewModel::onBudgetAmountChange,
                        label = { Text("Monthly Limit ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_category_budget_amount")
                    )

                    if (uiState.inputError != null) {
                        Text(
                            text = uiState.inputError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::saveCategoryBudget,
                    modifier = Modifier.testTag("btn_confirm_cat_budget")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeCategoryBudgetDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}
