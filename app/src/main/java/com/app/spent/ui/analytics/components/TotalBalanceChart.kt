package com.app.spent.ui.analytics.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen

data class TotalBalancePoint(
  val label: String,
  val fullDateLabel: String,
  val totalBalance: Double,
  val delta: Double
)

@Composable
fun TotalBalanceChart(
  transactions: List<TransactionEntity>,
  currencySymbol: String,
  modifier: Modifier = Modifier
) {
  var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.LAST_7_DAYS) }
  var selectedPointIndex by remember { mutableIntStateOf(-1) }

  val points = remember(transactions, selectedTimeframe) {
    computeTotalBalancePoints(transactions, selectedTimeframe)
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

      // 2. Timeframe Filter Tabs
      ChartTimeframeFilterRow(
        selectedTimeframe = selectedTimeframe,
        onSelectTimeframe = { timeframe ->
          selectedTimeframe = timeframe
          selectedPointIndex = -1
        }
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 3. Period Balance Metrics Summary Card
      TotalBalanceSummaryCard(
        currencySymbol = currencySymbol,
        currentBalance = currentTotalBalance,
        startingBalance = startingBalance,
        periodDelta = periodDelta
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 4. Interactive Tooltip (on touch/drag)
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

      // 5. 2D Canvas Cumulative Line Chart
      TotalBalanceCanvasChartArea(
        points = points,
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
private fun ChartTimeframeFilterRow(
  selectedTimeframe: ChartTimeframe,
  onSelectTimeframe: (ChartTimeframe) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    TimeframeChip(
      text = stringResource(R.string.timeframe_7d),
      isSelected = selectedTimeframe == ChartTimeframe.LAST_7_DAYS,
      onClick = { onSelectTimeframe(ChartTimeframe.LAST_7_DAYS) }
    )
    TimeframeChip(
      text = stringResource(R.string.timeframe_30d),
      isSelected = selectedTimeframe == ChartTimeframe.LAST_30_DAYS,
      onClick = { onSelectTimeframe(ChartTimeframe.LAST_30_DAYS) }
    )
    TimeframeChip(
      text = stringResource(R.string.timeframe_6m),
      isSelected = selectedTimeframe == ChartTimeframe.LAST_6_MONTHS,
      onClick = { onSelectTimeframe(ChartTimeframe.LAST_6_MONTHS) }
    )
  }
}

@Composable
private fun TimeframeChip(
  text: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  FilterChip(
    selected = isSelected,
    onClick = onClick,
    label = {
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
      )
    },
    colors = FilterChipDefaults.filterChipColors(
      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
      selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    )
  )
}

@Composable
private fun TotalBalanceSummaryCard(
  currencySymbol: String,
  currentBalance: Double,
  startingBalance: Double,
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
      // Current Total Balance
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

      // Period Delta Badge
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
        if (point.delta != 0.0) {
          Text(
            text = "($deltaSign$currencySymbol%.0f)".format(abs(point.delta)),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = deltaColor
          )
        }
      }
    }
  }
}

@Composable
private fun TotalBalanceCanvasChartArea(
  points: List<TotalBalancePoint>,
  currencySymbol: String,
  selectedPointIndex: Int,
  lineColor: Color,
  gridLineColor: Color,
  zeroBaselineColor: Color,
  textPaintColor: Int,
  guidelineColor: Color,
  onPointSelected: (Int) -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(185.dp)
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
    } else {
      val minVal = points.minOf { it.totalBalance }
      val maxVal = points.maxOf { it.totalBalance }

      val rawSpan = maxVal - minVal
      val margin = if (rawSpan == 0.0) max(10.0, abs(maxVal) * 0.2) else rawSpan * 0.15

      val effectiveMin = minVal - margin
      val effectiveMax = maxVal + margin
      val scaleRange = max(1.0, effectiveMax - effectiveMin)

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(points) {
            detectTapGestures(
              onTap = { offset ->
                val leftPadding = 48.dp.toPx()
                val rightPadding = 16.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                if (points.size > 1 && offset.x in leftPadding..(size.width - rightPadding)) {
                  val stepX = chartWidth / (points.size - 1)
                  val index = ((offset.x - leftPadding + (stepX / 2f)) / stepX).toInt().coerceIn(0, points.size - 1)
                  onPointSelected(if (selectedPointIndex == index) -1 else index)
                }
              }
            )
          }
          .pointerInput(points) {
            detectDragGestures(
              onDragStart = { offset ->
                val leftPadding = 48.dp.toPx()
                val rightPadding = 16.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                if (points.size > 1 && offset.x in leftPadding..(size.width - rightPadding)) {
                  val stepX = chartWidth / (points.size - 1)
                  val index = ((offset.x - leftPadding + (stepX / 2f)) / stepX).toInt().coerceIn(0, points.size - 1)
                  onPointSelected(index)
                }
              },
              onDrag = { change, _ ->
                change.consume()
                val leftPadding = 48.dp.toPx()
                val rightPadding = 16.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                if (points.size > 1) {
                  val stepX = chartWidth / (points.size - 1)
                  val index = ((change.position.x - leftPadding + (stepX / 2f)) / stepX).toInt().coerceIn(0, points.size - 1)
                  onPointSelected(index)
                }
              }
            )
          }
      ) {
        val leftPadding = 48.dp.toPx()
        val rightPadding = 16.dp.toPx()
        val topPadding = 16.dp.toPx()
        val bottomPadding = 24.dp.toPx()

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        // 1. Draw Grid Lines (3 horizontal lines)
        val steps = 3
        for (i in 0..steps) {
          val yVal = effectiveMax - (scaleRange * i / steps)
          val yPos = topPadding + (chartHeight * i / steps)

          drawLine(
            color = gridLineColor,
            start = Offset(leftPadding, yPos),
            end = Offset(size.width - rightPadding, yPos),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
          )

          // Y-axis label
          val labelText = formatCompactAmount(yVal, currencySymbol)
          drawContext.canvas.nativeCanvas.drawText(
            labelText,
            8.dp.toPx(),
            yPos + 4.dp.toPx(),
            android.graphics.Paint().apply {
              color = textPaintColor
              textSize = 10.sp.toPx()
              textAlign = android.graphics.Paint.Align.LEFT
              isAntiAlias = true
            }
          )
        }

        // 2. Zero baseline if within range
        if (effectiveMin < 0 && effectiveMax > 0) {
          val zeroFraction = (effectiveMax - 0.0) / scaleRange
          val zeroY = topPadding + (chartHeight * zeroFraction.toFloat()).coerceIn(0f, chartHeight)
          drawLine(
            color = zeroBaselineColor,
            start = Offset(leftPadding, zeroY),
            end = Offset(size.width - rightPadding, zeroY),
            strokeWidth = 1.5.dp.toPx()
          )
        }

        if (points.size < 2) return@Canvas

        val stepX = chartWidth / (points.size - 1)
        val coordinates = points.mapIndexed { index, point ->
          val fraction = ((effectiveMax - point.totalBalance) / scaleRange).toFloat().coerceIn(0f, 1f)
          val x = leftPadding + (index * stepX)
          val y = topPadding + (fraction * chartHeight)
          Offset(x, y)
        }

        // 3. Draw Gradient Fill Path
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

        // 4. Draw Smooth Spline Stroke
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
          style = Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round
          )
        )

        // 5. Draw X-axis tick labels (sparse)
        val labelStep = when (points.size) {
          7 -> 1
          30 -> 6
          6 -> 1
          else -> max(1, points.size / 5)
        }

        points.forEachIndexed { index, point ->
          if (index % labelStep == 0 || index == points.size - 1) {
            val xPos = coordinates[index].x
            drawContext.canvas.nativeCanvas.drawText(
              point.label,
              xPos,
              size.height - 4.dp.toPx(),
              android.graphics.Paint().apply {
                color = textPaintColor
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
              }
            )
          }
        }

        // 6. Draw Selected Highlight Point & Vertical Guideline
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

private fun formatCompactAmount(value: Double, currency: String): String {
  val absVal = abs(value)
  val sign = if (value < 0) "-" else ""
  return when {
    absVal >= 1_000_000 -> "$sign$currency%.1fM".format(absVal / 1_000_000)
    absVal >= 1_000 -> "$sign$currency%.1fk".format(absVal / 1_000)
    else -> "$sign$currency%.0f".format(absVal)
  }
}

private fun computeTotalBalancePoints(
  transactions: List<TransactionEntity>,
  timeframe: ChartTimeframe
): List<TotalBalancePoint> {
  val locale = Locale.getDefault()
  val sortedTransactions = transactions.sortedBy { it.timestamp }

  fun balanceAt(timestampEnd: Long): Double {
    return sortedTransactions
      .filter { it.timestamp <= timestampEnd && it.type != "SAVING" }
      .sumOf { if (it.type == "INCOME") it.amount else -it.amount }
  }

  fun deltaBetween(startMs: Long, endMs: Long): Double {
    return sortedTransactions
      .filter { it.timestamp in startMs..endMs && it.type != "SAVING" }
      .sumOf { if (it.type == "INCOME") it.amount else -it.amount }
  }

  return when (timeframe) {
    ChartTimeframe.LAST_7_DAYS -> {
      val dayFormat = SimpleDateFormat("EEE", locale)
      val fullDateFormat = SimpleDateFormat("EEE, MMM d", locale)
      val list = mutableListOf<TotalBalancePoint>()

      for (i in 6 downTo 0) {
        val cal = Calendar.getInstance().apply {
          add(Calendar.DAY_OF_YEAR, -i)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        val endMs = startMs + 24L * 60 * 60 * 1000 - 1

        list.add(
          TotalBalancePoint(
            label = dayFormat.format(cal.time),
            fullDateLabel = fullDateFormat.format(cal.time),
            totalBalance = balanceAt(endMs),
            delta = deltaBetween(startMs, endMs)
          )
        )
      }
      list
    }

    ChartTimeframe.LAST_30_DAYS -> {
      val dayFormat = SimpleDateFormat("d", locale)
      val fullDateFormat = SimpleDateFormat("MMM d, yyyy", locale)
      val list = mutableListOf<TotalBalancePoint>()

      for (i in 29 downTo 0) {
        val cal = Calendar.getInstance().apply {
          add(Calendar.DAY_OF_YEAR, -i)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        val endMs = startMs + 24L * 60 * 60 * 1000 - 1

        list.add(
          TotalBalancePoint(
            label = dayFormat.format(cal.time),
            fullDateLabel = fullDateFormat.format(cal.time),
            totalBalance = balanceAt(endMs),
            delta = deltaBetween(startMs, endMs)
          )
        )
      }
      list
    }

    ChartTimeframe.LAST_6_MONTHS -> {
      val monthFormat = SimpleDateFormat("MMM", locale)
      val fullMonthFormat = SimpleDateFormat("MMMM yyyy", locale)
      val list = mutableListOf<TotalBalancePoint>()

      for (i in 5 downTo 0) {
        val cal = Calendar.getInstance().apply {
          add(Calendar.MONTH, -i)
          set(Calendar.DAY_OF_MONTH, 1)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        val endCal = (cal.clone() as Calendar).apply {
          set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
          set(Calendar.HOUR_OF_DAY, 23)
          set(Calendar.MINUTE, 59)
          set(Calendar.SECOND, 59)
          set(Calendar.MILLISECOND, 999)
        }
        val endMs = endCal.timeInMillis

        list.add(
          TotalBalancePoint(
            label = monthFormat.format(cal.time),
            fullDateLabel = fullMonthFormat.format(cal.time),
            totalBalance = balanceAt(endMs),
            delta = deltaBetween(startMs, endMs)
          )
        )
      }
      list
    }
  }
}
