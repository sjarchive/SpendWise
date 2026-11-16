package com.spendwise.app.ui.components

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
import com.spendwise.app.ui.util.DateUtils

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
    // Animate a 0..1 progress Float and interpolate the amounts in Double.
    // Animating the amount itself as a Float loses precision above ~16.7M
    // (Float mantissa is 24-bit), so a 99,999,999.99 balance counted up as
    // 100,000,000 and visibly snapped at the end of the animation.
    val progress = remember { Animatable(1f) }

    LaunchedEffect(dataVersion) {
        // Tab switch or initial composition: do not run the counter animation
        if (dataVersion == versionSeen) {
            previousAmount = amount
            progress.snapTo(1f)
            return@LaunchedEffect
        }

        // Data actually changed (add / edit / delete transaction): run the counter animation!
        if (progress.isRunning) progress.stop()
        progress.snapTo(0f)
        versionSeen = dataVersion
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = animationDurationMillis,
                easing = FastOutSlowInEasing
            )
        )
        // Animation finished: the counter now sits exactly on the new amount.
        previousAmount = amount
    }

    val displayValue = if (progress.isRunning) {
        // Interpolate in Double between the last settled value and the target.
        previousAmount + (amount - previousAmount) * progress.value.toDouble()
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
