package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.util.DateUtils

/**
 * A currency [Text] that smoothly counts to its new target amount only when
 * transactions actually change (add / edit / delete), and stays perfectly
 * static otherwise (e.g. switching between tabs, screen recompositions).
 */
@Composable
fun AnimatedAmountText(
    amount: Double,
    currencySymbol: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    prefix: String = "",
    animationDurationMillis: Int = 850,
    dataVersion: Long = 0L
) {
    var versionSeen by rememberSaveable { mutableLongStateOf(dataVersion) }
    var previousAmount by rememberSaveable { mutableDoubleStateOf(amount) }
    val animatedAmount = remember { Animatable(amount.toFloat()) }

    LaunchedEffect(dataVersion) {
        // Tab switch or initial composition: do not run the counter animation
        if (dataVersion == versionSeen) {
            previousAmount = amount
            animatedAmount.snapTo(amount.toFloat())
            return@LaunchedEffect
        }

        // Data actually changed (add / edit / delete transaction): run the counter animation!
        val startVal = previousAmount.toFloat()
        versionSeen = dataVersion
        previousAmount = amount

        if (animatedAmount.isRunning) animatedAmount.stop()
        animatedAmount.snapTo(startVal)
        animatedAmount.animateTo(
            targetValue = amount.toFloat(),
            animationSpec = tween(
                durationMillis = animationDurationMillis,
                easing = FastOutSlowInEasing
            )
        )
    }

    val displayValue = if (animatedAmount.isRunning) {
        animatedAmount.value.toDouble()
    } else {
        amount
    }

    Text(
        text = prefix + DateUtils.formatCurrency(displayValue, currencySymbol),
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}
