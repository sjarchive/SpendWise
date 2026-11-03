package com.example.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.ui.components.AnimatedAmountText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PillShape
import com.example.ui.util.DateUtils
import kotlin.math.atan2

@Immutable
data class DonutSlice(
    val id: Long,
    val name: String,
    val amount: Double,
    val color: Color,
    val percentage: Float
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    totalAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    dataVersion: Long = 0L
) {
    var selectedSlice by remember { mutableStateOf<DonutSlice?>(null) }
    var versionSeen by rememberSaveable { mutableLongStateOf(dataVersion) }
    val animatedProgress = remember { Animatable(1f) }

    LaunchedEffect(dataVersion) {
        if (dataVersion == versionSeen) {
            animatedProgress.snapTo(1f)
            return@LaunchedEffect
        }
        versionSeen = dataVersion
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("donut_chart_component"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

            Canvas(
                modifier = Modifier
                    .size(220.dp)
                    .pointerInput(slices) {
                        detectTapGestures { tapOffset ->
                            if (slices.isEmpty()) return@detectTapGestures
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVec = tapOffset - center
                            val touchDistance = touchVec.getDistance()

                            // Ignore taps on the hollow center or outside the ring -
                            // previously a tap anywhere along the slice's angle
                            // (even in the middle hole) selected it.
                            val outerRadius = minOf(size.width, size.height) / 2f
                            val innerRadius = outerRadius - 32.dp.toPx() * 1.3f
                            if (touchDistance < innerRadius || touchDistance > outerRadius) {
                                return@detectTapGestures
                            }

                            var angle = Math.toDegrees(atan2(touchVec.y.toDouble(), touchVec.x.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            // Angle offset by -90 because we start from the top
                            val normalizedAngle = (angle + 90f) % 360f

                            var accumulated = 0f
                            for (slice in slices) {
                                val sweep = (slice.percentage / 100f) * 360f
                                if (normalizedAngle in accumulated..(accumulated + sweep)) {
                                    selectedSlice = if (selectedSlice?.id == slice.id) null else slice
                                    break
                                }
                                accumulated += sweep
                            }
                        }
                    }
            ) {
                val strokeWidth = 32.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val topLeft = Offset(
                    x = (size.width - radius * 2) / 2f,
                    y = (size.height - radius * 2) / 2f
                )
                val arcSize = Size(radius * 2, radius * 2)

                if (slices.isEmpty() || totalAmount <= 0.0) {
                    drawArc(
                        color = emptyRingColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    var currentAngle = -90f
                    val totalSweepToAnimate = 360f * animatedProgress.value

                    slices.forEach { slice ->
                        val sweep = (slice.percentage / 100f) * 360f
                        val isHighlighted = selectedSlice == null || selectedSlice?.id == slice.id
                        val animatedSweep = sweep.coerceAtMost((totalSweepToAnimate - (currentAngle + 90f)).coerceAtLeast(0f))

                        if (animatedSweep > 0f) {
                            val actualStroke = if (selectedSlice?.id == slice.id) strokeWidth * 1.25f else strokeWidth
                            drawArc(
                                color = if (isHighlighted) slice.color else slice.color.copy(alpha = 0.35f),
                                startAngle = currentAngle,
                                sweepAngle = (animatedSweep - 2f).coerceAtLeast(1f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = actualStroke, cap = StrokeCap.Round)
                            )
                        }
                        currentAngle += sweep
                    }
                }
            }

            // Center Label (Selected Category or Total)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (selectedSlice != null) {
                    Text(
                        text = selectedSlice!!.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = DateUtils.formatCurrency(selectedSlice!!.amount, currencySymbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${String.format("%.1f", selectedSlice!!.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = selectedSlice!!.color
                    )
                } else {
                    Text(
                        text = "Total Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedAmountText(
                        amount = totalAmount,
                        currencySymbol = currencySymbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        dataVersion = dataVersion
                    )
                    Text(
                        text = "${slices.size} categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend Items
        if (slices.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEach { slice ->
                    val isSelected = selectedSlice?.id == slice.id

                    Surface(
                        shape = PillShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(PillShape)
                            .clickable {
                                selectedSlice = if (isSelected) null else slice
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${slice.name} (${String.format("%.0f", slice.percentage)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
