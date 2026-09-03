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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val ChartIncomeGreen = Color(0xFF10B981)
private val ChartExpenseRed = Color(0xFFEF4444)

@Composable
fun NetSavingsChart(
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
        ChartTimelineHelper.computeNetSavingsPoints(transactions, activeInterval)
    }

    val totalPeriodIncome = remember(points) { points.sumOf { it.income } }
    val totalPeriodExpense = remember(points) { points.sumOf { it.expense } }
    val netPeriodBalance = totalPeriodIncome - totalPeriodExpense

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
            NetSavingsHeader()

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

            // 3. Period Net Summary Overview Card
            NetPeriodSummaryCard(
                currencySymbol = currencySymbol,
                totalIncome = totalPeriodIncome,
                totalExpense = totalPeriodExpense,
                netBalance = netPeriodBalance
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Interactive Tooltip (on touch)
            AnimatedVisibility(
                visible = selectedPointIndex in points.indices,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (selectedPointIndex in points.indices) {
                    NetInteractiveTooltip(
                        point = points[selectedPointIndex],
                        currencySymbol = currencySymbol
                    )
                }
            }

            // 5. Binance-Style Horizontally Scrollable Canvas with Sticky Y-Axis
            NetSavingsScrollableChartArea(
                points = points,
                interval = activeInterval,
                currencySymbol = currencySymbol,
                selectedPointIndex = selectedPointIndex,
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
private fun NetSavingsHeader() {
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
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.chart_net_savings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.chart_net_savings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NetPeriodSummaryCard(
    currencySymbol: String,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double
) {
    val isPositive = netBalance >= 0
    val netColor = if (isPositive) ChartIncomeGreen else ChartExpenseRed

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(netColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.chart_legend_net),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val netSign = if (isPositive) "+" else "-"
                Text(
                    text = "$netSign$currencySymbol%.2f".format(abs(netBalance)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = netColor
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ChartIncomeGreen, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.chart_legend_income),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "+$currencySymbol%.2f".format(totalIncome),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ChartIncomeGreen
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ChartExpenseRed, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.chart_legend_expense),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "-$currencySymbol%.2f".format(totalExpense),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ChartExpenseRed
                )
            }
        }
    }
}

@Composable
private fun NetInteractiveTooltip(
    point: NetSavingsPoint,
    currencySymbol: String
) {
    val isPositive = point.net >= 0
    val netColor = if (isPositive) ChartIncomeGreen else ChartExpenseRed
    val netSign = if (isPositive) "+" else "-"

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
                    text = "In: +$currencySymbol%.0f".format(point.income),
                    style = MaterialTheme.typography.labelSmall,
                    color = ChartIncomeGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Out: -$currencySymbol%.0f".format(point.expense),
                    style = MaterialTheme.typography.labelSmall,
                    color = ChartExpenseRed,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$netSign$currencySymbol%.2f".format(abs(point.net)),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = netColor
                )
            }
        }
    }
}

@Composable
private fun NetSavingsScrollableChartArea(
    points: List<NetSavingsPoint>,
    interval: ChartInterval,
    currencySymbol: String,
    selectedPointIndex: Int,
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

        // Compute bounds based on both income/expense and net values
        val maxIncomeExpense = points.maxOfOrNull { max(it.income, it.expense) } ?: 0.0
        val maxNet = points.maxOfOrNull { it.net } ?: 0.0
        val minNet = points.minOfOrNull { it.net } ?: 0.0

        val maxVal = max(max(maxIncomeExpense, maxNet), 10.0)
        val minVal = min(minNet, 0.0)

        val rawSpan = maxVal - minVal
        val margin = if (rawSpan == 0.0) 10.0 else rawSpan * 0.15

        val effectiveMin = minVal - (if (minVal < 0) margin else 0.0)
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

                    // Draw Horizontal Grid Lines
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

                    // Zero Baseline
                    val zeroRatio = ((0.0 - effectiveMin) / scaleRange).toFloat().coerceIn(0f, 1f)
                    val zeroY = topPadding + chartHeight * (1f - zeroRatio)

                    drawLine(
                        color = zeroBaselineColor,
                        start = Offset(0f, zeroY),
                        end = Offset(size.width, zeroY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    if (points.isEmpty()) return@Canvas

                    val stepX = size.width / points.size
                    val barWidth = (stepX * 0.28f).coerceIn(4.dp.toPx(), 14.dp.toPx())
                    val barGap = 2.dp.toPx()

                    val netOffsets = mutableListOf<Offset>()

                    // Draw Income & Expense bars for each interval
                    points.forEachIndexed { idx, pt ->
                        val centerX = (idx + 0.5f) * stepX

                        // Income Bar (drawn upwards from zero line)
                        if (pt.income > 0) {
                            val incRatio = (pt.income / scaleRange).toFloat().coerceIn(0f, 1f)
                            val incBarHeight = incRatio * chartHeight
                            val incTop = zeroY - incBarHeight
                            val incLeft = centerX - barWidth - (barGap / 2f)

                            drawRoundRect(
                                color = ChartIncomeGreen.copy(alpha = 0.75f),
                                topLeft = Offset(incLeft, incTop),
                                size = Size(barWidth, incBarHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }

                        // Expense Bar (drawn downwards from zero line or upwards as cost)
                        if (pt.expense > 0) {
                            val expRatio = (pt.expense / scaleRange).toFloat().coerceIn(0f, 1f)
                            val expBarHeight = expRatio * chartHeight
                            val expTop = zeroY - expBarHeight
                            val expLeft = centerX + (barGap / 2f)

                            drawRoundRect(
                                color = ChartExpenseRed.copy(alpha = 0.75f),
                                topLeft = Offset(expLeft, expTop),
                                size = Size(barWidth, expBarHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }

                        // Calculate Net Point Coordinate
                        val normNetY = ((pt.net - effectiveMin) / scaleRange).toFloat().coerceIn(0f, 1f)
                        val netY = topPadding + chartHeight * (1f - normNetY)
                        netOffsets.add(Offset(centerX, netY))

                        // X-Axis Date Label
                        drawContext.canvas.nativeCanvas.drawText(
                            pt.label,
                            centerX,
                            size.height - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = textPaintColor
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }

                    // Draw Net Savings Line Curve connecting points
                    if (netOffsets.size > 1) {
                        val netCurvePath = Path().apply {
                            moveTo(netOffsets.first().x, netOffsets.first().y)
                            for (i in 1 until netOffsets.size) {
                                val p0 = netOffsets[i - 1]
                                val p1 = netOffsets[i]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }

                        val avgNet = points.map { it.net }.average()
                        val netCurveColor = if (avgNet >= 0) ChartIncomeGreen else ChartExpenseRed

                        drawPath(
                            path = netCurvePath,
                            color = netCurveColor,
                            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Draw Dots on Net Line
                    points.forEachIndexed { idx, pt ->
                        val offset = netOffsets[idx]
                        val isSelected = selectedPointIndex == idx
                        val dotRadius = if (isSelected) 5.5.dp.toPx() else 3.dp.toPx()
                        val pointColor = if (pt.net >= 0) ChartIncomeGreen else ChartExpenseRed

                        if (pt.income > 0 || pt.expense > 0 || isSelected) {
                            drawCircle(color = Color.White, radius = dotRadius + 1.5.dp.toPx(), center = offset)
                            drawCircle(
                                color = pointColor,
                                radius = dotRadius,
                                center = offset
                            )
                        }
                    }

                    // Selected Point Guideline & Highlights
                    if (selectedPointIndex in netOffsets.indices) {
                        val selectedCoord = netOffsets[selectedPointIndex]

                        drawLine(
                            color = guidelineColor,
                            start = Offset(selectedCoord.x, topPadding),
                            end = Offset(selectedCoord.x, size.height - bottomPadding),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        drawCircle(
                            color = guidelineColor.copy(alpha = 0.25f),
                            radius = 9.dp.toPx(),
                            center = selectedCoord
                        )
                    }
                }
            }
        }
    }
}
