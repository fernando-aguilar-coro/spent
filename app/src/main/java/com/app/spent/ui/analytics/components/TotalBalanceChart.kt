package com.app.spent.ui.analytics.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.analytics.ChartInterval
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen
import kotlin.math.abs
import kotlin.math.max

@Composable
fun TotalBalanceChart(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    selectedInterval: ChartInterval = ChartInterval.DAY,
    onSelectInterval: (ChartInterval) -> Unit = {}
) {
    var internalInterval by remember { mutableStateOf(selectedInterval) }
    val activeInterval = if (selectedInterval != internalInterval) {
        internalInterval = selectedInterval
        selectedInterval
    } else internalInterval

    var selectedPointIndex by remember { mutableIntStateOf(-1) }

    val points = remember(transactions, activeInterval) {
        ChartTimelineHelper.computeTotalBalancePoints(transactions, activeInterval)
    }

    val currentTotalBalance = remember(points) { points.lastOrNull()?.totalBalance ?: 0.0 }
    val startingBalance = remember(points) { points.firstOrNull()?.let { it.totalBalance - it.delta } ?: 0.0 }
    val periodDelta = currentTotalBalance - startingBalance

    val primaryChartColor = MaterialTheme.colorScheme.primary
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val zeroBaselineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val textPaintColor = MaterialTheme.colorScheme.onSurfaceVariant.hashCode()
    val guidelineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header
            TotalBalanceHeader()

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Binance-style Interval Tabs (Day / Week / Month)
            ChartIntervalFilterRow(
                selectedInterval = activeInterval,
                onSelectInterval = { interval ->
                    internalInterval = interval
                    selectedPointIndex = -1
                    onSelectInterval(interval)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Period Balance Metrics Summary Card
            TotalBalanceSummaryCard(
                currencySymbol = currencySymbol,
                currentBalance = currentTotalBalance,
                periodDelta = periodDelta
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Interactive Tooltip (on touch)
            AnimatedVisibility(
                visible = selectedPointIndex in points.indices,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (selectedPointIndex in points.indices) {
                    TotalBalanceInteractiveTooltip(
                        point = points[selectedPointIndex],
                        currencySymbol = currencySymbol
                    )
                }
            }

            // 5. Binance-Style Horizontally Scrollable Canvas with Sticky Y-Axis
            TotalBalanceScrollableChartArea(
                points = points,
                interval = activeInterval,
                currencySymbol = currencySymbol,
                selectedPointIndex = selectedPointIndex,
                lineColor = primaryChartColor,
                gridLineColor = gridLineColor,
                zeroBaselineColor = zeroBaselineColor,
                textPaintColor = textPaintColor,
                guidelineColor = guidelineColor,
                onPointSelected = { selectedPointIndex = it }
            )
        }
    }
}

@Composable
private fun TotalBalanceHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.chart_total_balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.chart_total_balance_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TotalBalanceSummaryCard(
    currencySymbol: String,
    currentBalance: Double,
    periodDelta: Double
) {
    val isPositive = periodDelta >= 0
    val deltaColor = if (isPositive) IncomeGreen else ExpenseRed

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.chart_current_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$currencySymbol%.2f".format(currentBalance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.chart_period_change),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = deltaColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    val sign = if (isPositive) "+" else "-"
                    Text(
                        text = "$sign$currencySymbol%.2f".format(abs(periodDelta)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = deltaColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalBalanceInteractiveTooltip(
    point: TotalBalancePoint,
    currencySymbol: String
) {
    val isDeltaPositive = point.delta >= 0
    val deltaColor = if (isDeltaPositive) IncomeGreen else ExpenseRed
    val deltaSign = if (isDeltaPositive) "+" else "-"

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = point.fullDateLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currencySymbol%.2f".format(point.totalBalance),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$deltaSign$currencySymbol%.2f".format(abs(point.delta)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = deltaColor
                )
            }
        }
    }
}

@Composable
private fun TotalBalanceScrollableChartArea(
    points: List<TotalBalancePoint>,
    interval: ChartInterval,
    currencySymbol: String,
    selectedPointIndex: Int,
    lineColor: Color,
    gridLineColor: Color,
    zeroBaselineColor: Color,
    textPaintColor: Int,
    guidelineColor: Color,
    onPointSelected: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (points.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.chart_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@BoxWithConstraints
        }

        val yAxisWidth = 46.dp
        val availableChartWidth = maxWidth - yAxisWidth

        val pointWidth = when (interval) {
            ChartInterval.DAY -> 44.dp
            ChartInterval.WEEK -> 56.dp
            ChartInterval.MONTH -> 64.dp
        }
        val calculatedWidth = pointWidth * points.size
        val scrollableContentWidth = max(availableChartWidth, calculatedWidth)

        val scrollState = rememberScrollState()

        // Auto-scroll to the latest date (right edge) on initial load or interval change
        LaunchedEffect(points.size, interval) {
            scrollState.scrollTo(scrollState.maxValue)
        }

        val minVal = points.minOf { it.totalBalance }
        val maxVal = points.maxOf { it.totalBalance }
        val rawSpan = maxVal - minVal
        val margin = if (rawSpan == 0.0) max(10.0, abs(maxVal) * 0.2) else rawSpan * 0.15

        val effectiveMin = minVal - margin
        val effectiveMax = maxVal + margin
        val scaleRange = max(1.0, effectiveMax - effectiveMin)

        Row(modifier = Modifier.fillMaxSize()) {
            // 1. Sticky Y-Axis on the left
            Canvas(
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
            ) {
                val topPadding = 16.dp.toPx()
                val bottomPadding = 26.dp.toPx()
                val chartHeight = size.height - topPadding - bottomPadding

                val steps = 3
                for (i in 0..steps) {
                    val yVal = effectiveMax - (scaleRange * i / steps)
                    val yPos = topPadding + (chartHeight * i / steps)

                    val labelText = ChartTimelineHelper.formatCompactAmount(yVal, currencySymbol)
                    drawContext.canvas.nativeCanvas.drawText(
                        labelText,
                        2.dp.toPx(),
                        yPos + 3.5.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = textPaintColor
                            textSize = 9.5.sp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                            isAntiAlias = true
                        }
                    )
                }
            }

            // 2. Horizontally Scrollable Timeline
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(scrollableContentWidth)
                        .fillMaxHeight()
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                val stepX = size.width / points.size
                                val index = (offset.x / stepX).toInt().coerceIn(0, points.size - 1)
                                onPointSelected(if (selectedPointIndex == index) -1 else index)
                            }
                        }
                ) {
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 26.dp.toPx()
                    val chartHeight = size.height - topPadding - bottomPadding

                    // Draw Horizontal Grid Lines across full scrollable width
                    val steps = 3
                    for (i in 0..steps) {
                        val yPos = topPadding + (chartHeight * i / steps)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, yPos),
                            end = Offset(size.width, yPos),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // Draw Zero Baseline if crossing zero
                    if (effectiveMin < 0 && effectiveMax > 0) {
                        val zeroFraction = (effectiveMax - 0.0) / scaleRange
                        val zeroY = topPadding + (chartHeight * zeroFraction.toFloat()).coerceIn(0f, chartHeight)
                        drawLine(
                            color = zeroBaselineColor,
                            start = Offset(0f, zeroY),
                            end = Offset(size.width, zeroY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    if (points.isEmpty()) return@Canvas

                    val stepX = size.width / points.size
                    val coordinates = points.mapIndexed { index, point ->
                        val fraction = ((effectiveMax - point.totalBalance) / scaleRange).toFloat().coerceIn(0f, 1f)
                        val x = (index + 0.5f) * stepX
                        val y = topPadding + (fraction * chartHeight)
                        Offset(x, y)
                    }

                    // Draw Gradient Fill Path
                    if (coordinates.size > 1) {
                        val fillPath = Path().apply {
                            moveTo(coordinates.first().x, size.height - bottomPadding)
                            lineTo(coordinates.first().x, coordinates.first().y)
                            for (i in 1 until coordinates.size) {
                                val p0 = coordinates[i - 1]
                                val p1 = coordinates[i]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                            lineTo(coordinates.last().x, size.height - bottomPadding)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.35f),
                                    lineColor.copy(alpha = 0.02f)
                               ),
                                startY = topPadding,
                                endY = size.height - bottomPadding
                            )
                        )

                        // Draw Smooth Spline Curve
                        val strokePath = Path().apply {
                            moveTo(coordinates.first().x, coordinates.first().y)
                            for (i in 1 until coordinates.size) {
                                val p0 = coordinates[i - 1]
                                val p1 = coordinates[i]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }

                        drawPath(
                            path = strokePath,
                            color = lineColor,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    } else if (coordinates.size == 1) {
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = coordinates.first()
                        )
                    }

                    // Draw Points and X-Axis Date Labels
                    points.forEachIndexed { index, point ->
                        val xPos = coordinates[index].x

                        // X-axis date label
                        drawContext.canvas.nativeCanvas.drawText(
                            point.label,
                            xPos,
                            size.height - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = textPaintColor
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }

                    // Selected Point Guideline & Highlight
                    if (selectedPointIndex in coordinates.indices) {
                        val selectedCoord = coordinates[selectedPointIndex]

                        drawLine(
                            color = guidelineColor,
                            start = Offset(selectedCoord.x, topPadding),
                            end = Offset(selectedCoord.x, size.height - bottomPadding),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        drawCircle(
                            color = lineColor.copy(alpha = 0.25f),
                            radius = 9.dp.toPx(),
                            center = selectedCoord
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = selectedCoord
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = selectedCoord
                        )
                    }
                }
            }
        }
    }
}
