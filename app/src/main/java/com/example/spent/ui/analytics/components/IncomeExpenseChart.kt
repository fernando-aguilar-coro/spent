package com.app.spent.ui.analytics.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
import androidx.compose.material.icons.automirrored.filled.ShowChart
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
import kotlin.math.abs
import kotlin.math.max
enum class ChartTimeframe {
  LAST_7_DAYS,
  LAST_30_DAYS,
  LAST_6_MONTHS
}

data class TimeSeriesPoint(
val label: String,
val fullDateLabel: String,
val income: Double,
val expense: Double
)

private val ChartIncomeGreen = Color(0xFF10B981)
private val ChartExpenseRed = Color(0xFFEF4444)

@Composable
fun IncomeExpenseChart(
transactions: List<TransactionEntity>,
currencySymbol: String,
modifier: Modifier = Modifier
) {
  var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.LAST_7_DAYS) }
  var selectedPointIndex by remember { mutableIntStateOf(-1) }

  val points = remember(transactions, selectedTimeframe) {
    computeTimeSeriesPoints(transactions, selectedTimeframe)
  }

  val totalPeriodIncome = remember(points) { points.sumOf { it.income } }
  val totalPeriodExpense = remember(points) { points.sumOf { it.expense } }
  val netPeriodBalance = totalPeriodIncome - totalPeriodExpense

  val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
  val textPaintColor = MaterialTheme.colorScheme.onSurfaceVariant.hashCode()
  val guidelineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

  Card(
  modifier = modifier.fillMaxWidth(),
  shape = RoundedCornerShape(20.dp),
  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header
      ChartHeader()

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

      // Period Summary Overview Card
      ChartPeriodSummaryCard(
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
          ChartInteractiveTooltip(
          point = points[selectedPointIndex],
          currencySymbol = currencySymbol
          )
        }
      }

      // 2D Canvas Chart Area
      ChartCanvasArea(
      points = points,
      currencySymbol = currencySymbol,
      selectedPointIndex = selectedPointIndex,
      gridLineColor = gridLineColor,
      textPaintColor = textPaintColor,
      guidelineColor = guidelineColor,
      onPointSelected = { selectedPointIndex = it }
      )
    }
  }
}

@Composable
private fun ChartHeader() {
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
        imageVector = Icons.AutoMirrored.Filled.ShowChart,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
        text = stringResource(R.string.chart_income_vs_expenses),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
        )
        Text(
        text = stringResource(R.string.chart_income_vs_expenses_desc),
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
private fun ChartPeriodSummaryCard(
currencySymbol: String,
totalIncome: Double,
totalExpense: Double,
netBalance: Double
) {
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
        fontWeight = FontWeight.Bold,
        color = ChartIncomeGreen
        )
      }

      Column {
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
        fontWeight = FontWeight.Bold,
        color = ChartExpenseRed
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
        text = stringResource(R.string.chart_net),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val netSign = if (netBalance >= 0) "+" else "-"
        val netColor = if (netBalance >= 0) ChartIncomeGreen else ChartExpenseRed
        Text(
        text = "$netSign$currencySymbol%.2f".format(abs(netBalance)),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = netColor
        )
      }
    }
  }
}

@Composable
private fun ChartInteractiveTooltip(
point: TimeSeriesPoint,
currencySymbol: String
) {
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
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
        text = "↑ $currencySymbol%.2f".format(point.income),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = ChartIncomeGreen
        )
        Text(
        text = "↓ $currencySymbol%.2f".format(point.expense),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = ChartExpenseRed
        )
      }
    }
  }
}

@Composable
private fun ChartCanvasArea(
points: List<TimeSeriesPoint>,
currencySymbol: String,
selectedPointIndex: Int,
gridLineColor: Color,
textPaintColor: Int,
guidelineColor: Color,
onPointSelected: (Int) -> Unit
) {
  Box(
  modifier = Modifier
  .fillMaxWidth()
  .height(180.dp)
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
      val maxAmount = remember(points) {
        val peak = points.maxOfOrNull { max(it.income, it.expense) } ?: 0.0
        if (peak <= 0.0) 100.0 else peak * 1.15
      }

      Canvas(
      modifier = Modifier
      .fillMaxSize()
      .pointerInput(points) {
        detectTapGestures(
        onTap = { offset ->
          val leftPadding = 40.dp.toPx()
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
          val leftPadding = 40.dp.toPx()
          val rightPadding = 16.dp.toPx()
          val chartWidth = size.width - leftPadding - rightPadding
          if (points.size > 1 && offset.x in leftPadding..(size.width - rightPadding)) {
            val stepX = chartWidth / (points.size - 1)
            val index = ((offset.x - leftPadding + (stepX / 2f)) / stepX).toInt().coerceIn(0, points.size - 1)
            onPointSelected(index)
          }
        },
        onDrag = { change, _ ->
          val leftPadding = 40.dp.toPx()
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
        render2DChart(
        points = points,
        maxAmount = maxAmount,
        currencySymbol = currencySymbol,
        selectedPointIndex = selectedPointIndex,
        gridLineColor = gridLineColor,
        textPaintColor = textPaintColor,
        guidelineColor = guidelineColor
        )
      }
    }
  }
}

private fun DrawScope.render2DChart(
points: List<TimeSeriesPoint>,
maxAmount: Double,
currencySymbol: String,
selectedPointIndex: Int,
gridLineColor: Color,
textPaintColor: Int,
guidelineColor: Color
) {
  val leftPadding = 40.dp.toPx()
  val rightPadding = 16.dp.toPx()
  val topPadding = 14.dp.toPx()
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

  // Grid lines & Y-axis labels
  for (i in 0..gridCount) {
    val y = topPadding + (chartHeight * (gridCount - i) / gridCount)
    val value = (maxAmount * i / gridCount)

    drawLine(
    color = gridLineColor,
    start = Offset(leftPadding, y),
    end = Offset(size.width - rightPadding, y),
    strokeWidth = 1.dp.toPx(),
    pathEffect = if (i > 0) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
    )

    val labelText = if (value >= 1000) {
      "$currencySymbol%.0fk".format(value / 1000)
    } else {
      "$currencySymbol%.0f".format(value)
    }
    drawContext.canvas.nativeCanvas.drawText(
    labelText,
    leftPadding - 6.dp.toPx(),
    y + 4.dp.toPx(),
    textPaint
    )
  }

  val n = points.size
  val stepX = if (n > 1) chartWidth / (n - 1) else chartWidth

  val incomeOffsets = points.mapIndexed { idx, pt ->
    val x = leftPadding + (idx * stepX)
    val normY = (pt.income / maxAmount).coerceIn(0.0, 1.0).toFloat()
    val y = topPadding + chartHeight * (1f - normY)
    Offset(x, y)
  }

  val expenseOffsets = points.mapIndexed { idx, pt ->
    val x = leftPadding + (idx * stepX)
    val normY = (pt.expense / maxAmount).coerceIn(0.0, 1.0).toFloat()
    val y = topPadding + chartHeight * (1f - normY)
    Offset(x, y)
  }

  // Smooth paths & gradient area fills
  val incomePath = createSmoothPath(incomeOffsets)
  val incomeFillPath = Path().apply {
    addPath(incomePath)
    if (incomeOffsets.isNotEmpty()) {
      lineTo(incomeOffsets.last().x, topPadding + chartHeight)
      lineTo(incomeOffsets.first().x, topPadding + chartHeight)
      close()
    }
  }

  val expensePath = createSmoothPath(expenseOffsets)
  val expenseFillPath = Path().apply {
    addPath(expensePath)
    if (expenseOffsets.isNotEmpty()) {
      lineTo(expenseOffsets.last().x, topPadding + chartHeight)
      lineTo(expenseOffsets.first().x, topPadding + chartHeight)
      close()
    }
  }

  drawPath(
  path = incomeFillPath,
  brush = Brush.verticalGradient(
  colors = listOf(ChartIncomeGreen.copy(alpha = 0.22f), Color.Transparent),
  startY = topPadding,
  endY = topPadding + chartHeight
  )
  )
  drawPath(
  path = expenseFillPath,
  brush = Brush.verticalGradient(
  colors = listOf(ChartExpenseRed.copy(alpha = 0.22f), Color.Transparent),
  startY = topPadding,
  endY = topPadding + chartHeight
  )
  )

  // Curve outlines
  drawPath(
  path = incomePath,
  color = ChartIncomeGreen,
  style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
  )
  drawPath(
  path = expensePath,
  color = ChartExpenseRed,
  style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
  )

  val labelInterval = when {
    n <= 7 -> 1
    n <= 15 -> 2
    else -> max(1, n / 6)
  }

  points.forEachIndexed { idx, pt ->
    val incOff = incomeOffsets[idx]
    val expOff = expenseOffsets[idx]
    val isSelected = selectedPointIndex == idx
    val dotRadius = if (isSelected) 5.dp.toPx() else 3.dp.toPx()

    if (pt.income > 0 || isSelected) {
      drawCircle(color = Color.White, radius = dotRadius, center = incOff)
      drawCircle(
      color = ChartIncomeGreen,
      radius = dotRadius,
      center = incOff,
      style = Stroke(width = 2.dp.toPx())
      )
    }

    if (pt.expense > 0 || isSelected) {
      drawCircle(color = Color.White, radius = dotRadius, center = expOff)
      drawCircle(
      color = ChartExpenseRed,
      radius = dotRadius,
      center = expOff,
      style = Stroke(width = 2.dp.toPx())
      )
    }

    if (idx % labelInterval == 0 || idx == n - 1) {
      drawContext.canvas.nativeCanvas.drawText(
      pt.label,
      incOff.x,
      topPadding + chartHeight + 16.dp.toPx(),
      xLabelPaint
      )
    }
  }

  if (selectedPointIndex in incomeOffsets.indices) {
    val activeX = incomeOffsets[selectedPointIndex].x
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

private fun computeTimeSeriesPoints(
transactions: List<TransactionEntity>,
timeframe: ChartTimeframe
): List<TimeSeriesPoint> {
  val locale = Locale.getDefault()

  return when (timeframe) {
    ChartTimeframe.LAST_7_DAYS -> {
      val dayFormat = SimpleDateFormat("EEE", locale)
      val fullDateFormat = SimpleDateFormat("EEE, MMM d", locale)
      val list = mutableListOf<TimeSeriesPoint>()

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
        TimeSeriesPoint(
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
      val list = mutableListOf<TimeSeriesPoint>()

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
        TimeSeriesPoint(
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
      val list = mutableListOf<TimeSeriesPoint>()

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
        TimeSeriesPoint(
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
