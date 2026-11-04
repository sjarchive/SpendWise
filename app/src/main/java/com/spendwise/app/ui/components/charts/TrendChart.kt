package com.spendwise.app.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.ui.theme.CardShape
import com.spendwise.app.ui.theme.PillShape
import com.spendwise.app.ui.util.DateUtils

@Immutable
data class ChartDataPoint(
    val label: String,
    val amount: Double
)

enum class ChartType {
    BAR,
    LINE
}

enum class TrendPeriod {
    DAILY_7_DAYS,
    MONTHLY_6_MONTHS
}

@Composable
fun TrendChart(
    dataPoints: List<ChartDataPoint>,
    selectedPeriod: TrendPeriod,
    onPeriodSelected: (TrendPeriod) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    dataVersion: Long = 0L
) {
    var chartType by remember { mutableStateOf(ChartType.BAR) }
    var versionSeen by rememberSaveable { mutableLongStateOf(dataVersion) }
    var lastChartType by rememberSaveable { mutableStateOf(chartType.name) }
    var lastPeriod by rememberSaveable { mutableStateOf(selectedPeriod.name) }
    val animatedProgress = remember { Animatable(1f) }

    LaunchedEffect(dataVersion, chartType, selectedPeriod) {
        val typeChanged = chartType.name != lastChartType
        val periodChanged = selectedPeriod.name != lastPeriod
        lastChartType = chartType.name
        lastPeriod = selectedPeriod.name

        if (dataVersion == versionSeen && !typeChanged && !periodChanged) {
            animatedProgress.snapTo(1f)
            return@LaunchedEffect
        }
        versionSeen = dataVersion
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trend_chart_card"),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Period Toggle Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedPeriod == TrendPeriod.DAILY_7_DAYS,
                        onClick = { onPeriodSelected(TrendPeriod.DAILY_7_DAYS) },
                        label = { Text("Daily", style = MaterialTheme.typography.labelSmall) },
                        shape = PillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chip_trend_daily")
                    )

                    FilterChip(
                        selected = selectedPeriod == TrendPeriod.MONTHLY_6_MONTHS,
                        onClick = { onPeriodSelected(TrendPeriod.MONTHLY_6_MONTHS) },
                        label = { Text("Monthly", style = MaterialTheme.typography.labelSmall) },
                        shape = PillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chip_trend_monthly")
                    )
                }

                // Bar / Line Switcher
                Row(
                    modifier = Modifier
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = chartType == ChartType.BAR,
                        onClick = { chartType = ChartType.BAR },
                        label = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Bar chart view",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    FilterChip(
                        selected = chartType == ChartType.LINE,
                        onClick = { chartType = ChartType.LINE },
                        label = {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = "Line chart view",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart Area
            val maxAmount = (dataPoints.maxOfOrNull { it.amount } ?: 100.0).coerceAtLeast(10.0)
            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceColor = MaterialTheme.colorScheme.surface
            val guideLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            val textColor = MaterialTheme.colorScheme.onSurfaceVariant
            val textMeasurer = rememberTextMeasurer()

            // Pre-measure axis labels OUTSIDE the draw pass. Measuring text on
            // every animation frame caused visible jank while the chart grew.
            val labelLayouts = remember(textMeasurer, dataPoints, textColor) {
                dataPoints.map { point ->
                    textMeasurer.measure(
                        text = point.label,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = textColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val bottomPadding = 36.dp.toPx()
                    val topPadding = 24.dp.toPx()
                    val chartHeight = size.height - bottomPadding - topPadding
                    val count = dataPoints.size
                    if (count == 0) return@Canvas

                    // Draw subtle horizontal guidelines
                    val levels = 3
                    for (i in 0..levels) {
                        val y = topPadding + (chartHeight / levels) * i
                        drawLine(
                            color = guideLineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val stepX = size.width / count

                    if (chartType == ChartType.BAR) {
                        val barWidth = (stepX * 0.48f).coerceIn(12.dp.toPx(), 36.dp.toPx())

                        dataPoints.forEachIndexed { index, point ->
                            val progressFactor = animatedProgress.value
                            val barHeight = ((point.amount / maxAmount) * chartHeight * progressFactor).toFloat()
                            val x = (index * stepX) + (stepX - barWidth) / 2f
                            val y = topPadding + chartHeight - barHeight

                            // Gradient Bar
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.65f)),
                                    startY = y,
                                    endY = y + barHeight
                                ),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            // X-axis label (pre-measured)
                            val labelResult = labelLayouts[index]
                            val labelX = (index * stepX) + (stepX - labelResult.size.width) / 2f
                            val labelY = size.height - bottomPadding + 8.dp.toPx()
                            drawText(
                                textLayoutResult = labelResult,
                                topLeft = Offset(labelX, labelY)
                            )
                        }
                    } else {
                        // Line Chart
                        val linePoints = dataPoints.mapIndexed { index, point ->
                            val progressFactor = animatedProgress.value
                            val pointHeight = ((point.amount / maxAmount) * chartHeight * progressFactor).toFloat()
                            val x = (index * stepX) + (stepX / 2f)
                            val y = topPadding + chartHeight - pointHeight
                            Offset(x, y)
                        }

                        if (linePoints.isNotEmpty()) {
                            // Gradient Fill Path
                            val fillPath = Path().apply {
                                moveTo(linePoints.first().x, topPadding + chartHeight)
                                linePoints.forEach { pt -> lineTo(pt.x, pt.y) }
                                lineTo(linePoints.last().x, topPadding + chartHeight)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                                    startY = topPadding,
                                    endY = topPadding + chartHeight
                                )
                            )

                            // Stroke Path
                            val strokePath = Path().apply {
                                moveTo(linePoints.first().x, linePoints.first().y)
                                for (i in 1 until linePoints.size) {
                                    lineTo(linePoints[i].x, linePoints[i].y)
                                }
                            }

                            drawPath(
                                path = strokePath,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Circles & X-labels
                            linePoints.forEachIndexed { index, pt ->
                                drawCircle(
                                    color = surfaceColor,
                                    radius = 5.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = 3.5.dp.toPx(),
                                    center = pt
                                )

                                val labelResult = labelLayouts[index]
                                val labelX = (index * stepX) + (stepX - labelResult.size.width) / 2f
                                val labelY = size.height - bottomPadding + 8.dp.toPx()
                                drawText(
                                    textLayoutResult = labelResult,
                                    topLeft = Offset(labelX, labelY)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
