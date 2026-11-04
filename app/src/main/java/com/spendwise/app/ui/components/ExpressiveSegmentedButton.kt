package com.spendwise.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.model.TransactionType
import com.spendwise.app.ui.theme.ExpenseRed
import com.spendwise.app.ui.theme.IncomeGreen
import com.spendwise.app.ui.theme.PillShape
import com.spendwise.app.ui.util.Haptics

@Composable
fun ExpressiveTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
            .testTag("type_segmented_group")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expense Option
            val isExpense = selectedType == TransactionType.EXPENSE
            val expenseBgColor by animateColorAsState(
                targetValue = if (isExpense) ExpenseRed else Color.Transparent,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "expenseBg"
            )
            val expenseTextColor by animateColorAsState(
                targetValue = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "expenseText"
            )
            val expenseRadius by animateDpAsState(
                targetValue = if (isExpense) 24.dp else 16.dp,
                label = "expenseRadius"
            )
            val expenseIconScale by animateFloatAsState(
                targetValue = if (isExpense) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "expenseIconScale"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(expenseRadius))
                    .background(expenseBgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.RadioButton,
                        onClick = {
                            Haptics.tick(view)
                            onTypeSelected(TransactionType.EXPENSE)
                        }
                    )
                    .testTag("toggle_expense"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Expense",
                        tint = expenseTextColor,
                        modifier = Modifier
                            .size(18.dp)
                            // Draw-phase read: the bounce no longer recomposes the toggle.
                            .graphicsLayer {
                                scaleX = expenseIconScale
                                scaleY = expenseIconScale
                            }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = expenseTextColor
                    )
                }
            }

            // Income Option
            val isIncome = selectedType == TransactionType.INCOME
            val incomeBgColor by animateColorAsState(
                targetValue = if (isIncome) IncomeGreen else Color.Transparent,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "incomeBg"
            )
            val incomeTextColor by animateColorAsState(
                targetValue = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "incomeText"
            )
            val incomeRadius by animateDpAsState(
                targetValue = if (isIncome) 24.dp else 16.dp,
                label = "incomeRadius"
            )
            val incomeIconScale by animateFloatAsState(
                targetValue = if (isIncome) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "incomeIconScale"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(incomeRadius))
                    .background(incomeBgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.RadioButton,
                        onClick = {
                            Haptics.tick(view)
                            onTypeSelected(TransactionType.INCOME)
                        }
                    )
                    .testTag("toggle_income"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Income",
                        tint = incomeTextColor,
                        modifier = Modifier
                            .size(18.dp)
                            // Draw-phase read: the bounce no longer recomposes the toggle.
                            .graphicsLayer {
                                scaleX = incomeIconScale
                                scaleY = incomeIconScale
                            }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                        color = incomeTextColor
                    )
                }
            }
        }
    }
}
