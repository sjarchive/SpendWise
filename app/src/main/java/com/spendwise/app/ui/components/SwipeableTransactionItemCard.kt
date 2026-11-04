package com.spendwise.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.spendwise.app.data.local.entity.relations.TransactionWithCategory
import com.spendwise.app.ui.theme.CardShape
import com.spendwise.app.ui.theme.ExpenseRed
import com.spendwise.app.ui.util.Haptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItemCard(
    transactionWithCategory: TransactionWithCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹",
    onSwiped: () -> Unit = {}
) {
    val view = LocalView.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swiped Right -> Edit
                    onSwiped()
                    onEdit()
                    false // Don't dismiss off screen
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swiped Left -> Delete
                    onSwiped()
                    Haptics.tap(view)
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .clip(CardShape)
            .testTag("swipeable_item_${transactionWithCategory.transaction.id}"),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> ExpenseRed.copy(alpha = 0.85f)
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipeBgColor"
            )

            val density = LocalDensity.current
            val minThresholdPx = with(density) { 64.dp.toPx() }
            val fullThresholdPx = with(density) { 96.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardShape)
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                // The swipe offset is read INSIDE graphicsLayer, so a continuous drag
                // only re-runs the draw phase for the icon. Reading it in composition
                // used to recompose the whole background on every frame; pre-composing
                // the icon also removes the mid-gesture composition hitch when the
                // drag crosses the 64dp threshold. Visuals are unchanged.
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Swipe to Edit",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                val absOffset = kotlin.math.abs(
                                    runCatching { dismissState.requireOffset() }.getOrDefault(0f)
                                )
                                val fraction = if (absOffset <= minThresholdPx) 0f
                                else ((absOffset - minThresholdPx) / (fullThresholdPx - minThresholdPx)).coerceIn(0f, 1f)
                                alpha = fraction
                                scaleX = 0.6f + 0.4f * fraction
                                scaleY = 0.6f + 0.4f * fraction
                            }
                    )
                    SwipeToDismissBoxValue.EndToStart -> Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Swipe to Delete",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                val absOffset = kotlin.math.abs(
                                    runCatching { dismissState.requireOffset() }.getOrDefault(0f)
                                )
                                val fraction = if (absOffset <= minThresholdPx) 0f
                                else ((absOffset - minThresholdPx) / (fullThresholdPx - minThresholdPx)).coerceIn(0f, 1f)
                                alpha = fraction
                                scaleX = 0.6f + 0.4f * fraction
                                scaleY = 0.6f + 0.4f * fraction
                            }
                    )
                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }
        }
    ) {
        TransactionItemCard(
            transactionWithCategory = transactionWithCategory,
            onClick = onEdit,
            currencySymbol = currencySymbol
        )
    }
}
