package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle

/**
 * A transaction count label that gives a small "pop" when the underlying
 * transaction data changes (add / update / delete), and stays perfectly
 * still otherwise — re-entering the screen, filtering or searching must not
 * animate it.
 *
 * `dataVersion` comes from the ViewModel and increments only on real data
 * changes, so the pulse is keyed on that instead of on `count` (filtering
 * changes the count too, but should not animate) or on recompositions.
 */
@Composable
fun AnimatedTransactionCount(
    count: Int,
    dataVersion: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    var versionSeen by remember { mutableLongStateOf(dataVersion) }
    val pulse = remember { Animatable(1f) }

    LaunchedEffect(dataVersion) {
        // First composition: show the count as-is, no animation.
        if (dataVersion == versionSeen) return@LaunchedEffect
        versionSeen = dataVersion

        if (pulse.isRunning) pulse.stop()
        pulse.snapTo(1f)
        pulse.animateTo(
            targetValue = 1.12f,
            animationSpec = tween(durationMillis = 110)
        )
        pulse.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Text(
        text = if (count == 1) "1 transaction" else "$count transactions",
        style = style,
        color = color,
        modifier = modifier.graphicsLayer {
            transformOrigin = TransformOrigin.Center
            scaleX = pulse.value
            scaleY = pulse.value
        }
    )
}
