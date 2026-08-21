package com.app.spent.ui.analytics.components

import java.text.SimpleDateFormat
import java.util.Calendar
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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

enum class ChartTimeframe {
  LAST_7_DAYS,
  LAST_30_DAYS,
  LAST_6_MONTHS
}

data class NetSavingsPoint(
  val label: String,
  val fullDateLabel: String,
  val income: Double,
  val expense: Double,
  val net: Double = income - expense
)

private val ChartIncomeGreen = Color(0xFF10B981)
private val ChartExpenseRed = Color(0xFFEF4444)

@Composable
fun NetSavingsChart(
  transactions: List<TransactionEntity>,
  currencySymbol: String,
  modifier: Modifier = Modifier
) {
  var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.LAST_7_DAYS) }
  var selectedPointIndex by remember { mutableIntStateOf(-1) }

  val points = remember(transactions, selectedTimeframe) {
    computeNetSavingsPoints(transactions, selectedTimeframe)
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
      // Header
      NetSavingsHeader()

      Spacer(modifier = Modifier.height(14.dp))

      // Timeframe Filter Tabs
      ChartTimeframeFilterRow(
        selectedTimeframe = selectedTimeframe,
        onSelectTimeframe = { timeframe ->
          selectedTimeframe = timeframe
          selectedPointIndex = -1
        }
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Period Net Summary Overview Card
      NetPeriodSummaryCard(
        currencySymbol = currencySymbol,
        totalIncome = totalPeriodIncome,
        totalExpense = totalPeriodExpense,
        netBalance = netPeriodBalance
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Interactive Tooltip (on touch/drag)
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

      // 2D Canvas Net Savings Chart Area
      NetCanvasChartArea(
        points = points,
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
          text = "$netSign$currencySymbol%.2f".format(abs(point.net)),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = netColor
        )
        Text(
          text = "(↑ $currencySymbol%.0f | ↓ $currencySymbol%.0f)".format(point.income, point.expense),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
      }
    }
  }
}

@Composable
private fun NetCanvasChartArea(
  points: List<NetSavingsPoint>,
  currencySymbol: String,
  selectedPointIndex: Int,
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
    val hasNonZeroData = points.any { it.income > 0 || it.expense > 0 }

    if (points.isEmpty() || !hasNonZeroData) {
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
      val minNet = points.minOfOrNull { it.net } ?: 0.0
      val maxNet = points.maxOfOrNull { it.net } ?: 0.0

      // Calculate dynamic scale bounds
      val effectiveMin = when {
        minNet < 0 -> minNet * 1.2
        else -> 0.0
      }
      val effectiveMax = when {
        maxNet > 0 -> maxNet * 1.2
        else -> 0.0
      }

      val scaleRange = max(10.0, effectiveMax - effectiveMin)

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
                val leftPadding = 48.dp.toPx()
                val rightPadding = 16.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                if (points.size > 1 && change.position.x in leftPadding..(size.width - rightPadding)) {
                  val stepX = chartWidth / (points.size - 1)
                  val index = ((change.position.x - leftPadding + (stepX / 2f)) / stepX).toInt().coerceIn(0, points.size - 1)
                  onPointSelected(index)
                }
              }
            )
          }
      ) {
        render2DNetSavingsChart(
          points = points,
          effectiveMin = effectiveMin,
          effectiveMax = effectiveMax,
          scaleRange = scaleRange,
          currencySymbol = currencySymbol,
          selectedPointIndex = selectedPointIndex,
          gridLineColor = gridLineColor,
          zeroBaselineColor = zeroBaselineColor,
          textPaintColor = textPaintColor,
          guidelineColor = guidelineColor
        )
      }
    }
  }
}

private fun DrawScope.render2DNetSavingsChart(
  points: List<NetSavingsPoint>,
  effectiveMin: Double,
  effectiveMax: Double,
  scaleRange: Double,
  currencySymbol: String,
  selectedPointIndex: Int,
  gridLineColor: Color,
  zeroBaselineColor: Color,
  textPaintColor: Int,
  guidelineColor: Color
) {
  val leftPadding = 48.dp.toPx()
  val rightPadding = 16.dp.toPx()
  val topPadding = 16.dp.toPx()
  val bottomPadding = 24.dp.toPx()

  val chartWidth = size.width - leftPadding - rightPadding
  val chartHeight = size.height - topPadding - bottomPadding

  if (chartWidth <= 0 || chartHeight <= 0 || points.isEmpty()) return

  val gridCount = 3
  val textPaint = android.graphics.Paint().apply {
    color = textPaintColor
    textSize = 10.sp.toPx()
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.RIGHT
  }
  val xLabelPaint = android.graphics.Paint().apply {
    color = textPaintColor
    textSize = 9.sp.toPx()
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.CENTER
  }

  // Draw grid lines and Y-axis labels
  for (i in 0..gridCount) {
    val y = topPadding + (chartHeight * (gridCount - i) / gridCount)
    val value = effectiveMin + (scaleRange * i / gridCount)

    drawLine(
      color = gridLineColor,
      start = Offset(leftPadding, y),
      end = Offset(size.width - rightPadding, y),
      strokeWidth = 1.dp.toPx(),
      pathEffect = if (i > 0) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
    )

    val labelText = when {
      abs(value) < 1.0 -> "$currencySymbol 0"
      value >= 1000 -> "+$currencySymbol%.0fk".format(value / 1000)
      value <= -1000 -> "-$currencySymbol%.0fk".format(abs(value) / 1000)
      value > 0 -> "+$currencySymbol%.0f".format(value)
      else -> "-$currencySymbol%.0f".format(abs(value))
    }

    drawContext.canvas.nativeCanvas.drawText(
      labelText,
      leftPadding - 6.dp.toPx(),
      y + 4.dp.toPx(),
      textPaint
    )
  }

  // Draw Zero Baseline if range spans across zero
  val zeroRatio = ((0.0 - effectiveMin) / scaleRange).toFloat().coerceIn(0f, 1f)
  val zeroY = topPadding + chartHeight * (1f - zeroRatio)

  if (effectiveMin < 0 && effectiveMax > 0) {
    drawLine(
      color = zeroBaselineColor,
      start = Offset(leftPadding, zeroY),
      end = Offset(size.width - rightPadding, zeroY),
      strokeWidth = 1.5.dp.toPx()
    )
  }

  val n = points.size
  val stepX = if (n > 1) chartWidth / (n - 1) else chartWidth

  val netOffsets = points.mapIndexed { idx, pt ->
    val x = leftPadding + (idx * stepX)
    val normY = ((pt.net - effectiveMin) / scaleRange).toFloat().coerceIn(0f, 1f)
    val y = topPadding + chartHeight * (1f - normY)
    Offset(x, y)
  }

  // Smooth path & gradient area fill
  val netCurvePath = createSmoothPath(netOffsets)
  val netFillPath = Path().apply {
    addPath(netCurvePath)
    if (netOffsets.isNotEmpty()) {
      lineTo(netOffsets.last().x, zeroY)
      lineTo(netOffsets.first().x, zeroY)
      close()
    }
  }

  val avgNet = points.map { it.net }.average()
  val gradientColor = if (avgNet >= 0) ChartIncomeGreen else ChartExpenseRed

  drawPath(
    path = netFillPath,
    brush = Brush.verticalGradient(
      colors = listOf(
        gradientColor.copy(alpha = 0.28f),
        gradientColor.copy(alpha = 0.05f)
      ),
      startY = min(topPadding, zeroY),
      endY = max(topPadding + chartHeight, zeroY)
    )
  )

  // Net Savings Curve Outline
  drawPath(
    path = netCurvePath,
    color = gradientColor,
    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
  )

  val labelInterval = when {
    n <= 7 -> 1
    n <= 15 -> 2
    else -> max(1, n / 6)
  }

  // Points & Selection Indicators
  points.forEachIndexed { idx, pt ->
    val offset = netOffsets[idx]
    val isSelected = selectedPointIndex == idx
    val dotRadius = if (isSelected) 5.5.dp.toPx() else 3.5.dp.toPx()
    val pointColor = if (pt.net >= 0) ChartIncomeGreen else ChartExpenseRed

    if (pt.income > 0 || pt.expense > 0 || isSelected) {
      drawCircle(color = Color.White, radius = dotRadius + 1.5.dp.toPx(), center = offset)
      drawCircle(
        color = pointColor,
        radius = dotRadius,
        center = offset
      )
    }

    if (idx % labelInterval == 0 || idx == n - 1) {
      drawContext.canvas.nativeCanvas.drawText(
        pt.label,
        offset.x,
        topPadding + chartHeight + 16.dp.toPx(),
        xLabelPaint
      )
    }
  }

  // Guideline on active point
  if (selectedPointIndex in netOffsets.indices) {
    val activeX = netOffsets[selectedPointIndex].x
    drawLine(
      color = guidelineColor,
      start = Offset(activeX, topPadding),
      end = Offset(activeX, topPadding + chartHeight),
      strokeWidth = 1.5.dp.toPx(),
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
    )
  }
}

private fun createSmoothPath(points: List<Offset>): Path {
  val path = Path()
  if (points.isEmpty()) return path

  path.moveTo(points.first().x, points.first().y)
  if (points.size == 1) return path

  for (i in 0 until points.size - 1) {
    val current = points[i]
    val next = points[i + 1]

    val controlX1 = current.x + (next.x - current.x) / 2f
    val controlY1 = current.y
    val controlX2 = current.x + (next.x - current.x) / 2f
    val controlY2 = next.y

    path.cubicTo(controlX1, controlY1, controlX2, controlY2, next.x, next.y)
  }

  return path
}

private fun computeNetSavingsPoints(
  transactions: List<TransactionEntity>,
  timeframe: ChartTimeframe
): List<NetSavingsPoint> {
  val locale = Locale.getDefault()

  return when (timeframe) {
    ChartTimeframe.LAST_7_DAYS -> {
      val dayFormat = SimpleDateFormat("EEE", locale)
      val fullDateFormat = SimpleDateFormat("EEE, MMM d", locale)
      val list = mutableListOf<NetSavingsPoint>()

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

        val txsInDay = transactions.filter { it.timestamp in startMs..endMs }
        val inc = txsInDay.filter { it.type == "INCOME" }.sumOf { it.amount }
        val exp = txsInDay.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        list.add(
          NetSavingsPoint(
            label = dayFormat.format(cal.time),
            fullDateLabel = fullDateFormat.format(cal.time),
            income = inc,
            expense = exp
          )
        )
      }
      list
    }

    ChartTimeframe.LAST_30_DAYS -> {
      val dayFormat = SimpleDateFormat("d", locale)
      val fullDateFormat = SimpleDateFormat("MMM d, yyyy", locale)
      val list = mutableListOf<NetSavingsPoint>()

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

        val txsInDay = transactions.filter { it.timestamp in startMs..endMs }
        val inc = txsInDay.filter { it.type == "INCOME" }.sumOf { it.amount }
        val exp = txsInDay.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        list.add(
          NetSavingsPoint(
            label = dayFormat.format(cal.time),
            fullDateLabel = fullDateFormat.format(cal.time),
            income = inc,
            expense = exp
          )
        )
      }
      list
    }

    ChartTimeframe.LAST_6_MONTHS -> {
      val monthFormat = SimpleDateFormat("MMM", locale)
      val fullMonthFormat = SimpleDateFormat("MMMM yyyy", locale)
      val list = mutableListOf<NetSavingsPoint>()

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

        val txsInMonth = transactions.filter { it.timestamp in startMs..endMs }
        val inc = txsInMonth.filter { it.type == "INCOME" }.sumOf { it.amount }
        val exp = txsInMonth.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        list.add(
          NetSavingsPoint(
            label = monthFormat.format(cal.time),
            fullDateLabel = fullMonthFormat.format(cal.time),
            income = inc,
            expense = exp
          )
        )
      }
      list
    }
  }
}
