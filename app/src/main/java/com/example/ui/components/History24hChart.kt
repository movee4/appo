package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistoryDataPoint
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldRed
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.NumberFormat
import java.util.Locale

@Composable
fun History24hChart(
    historyPoints: List<HistoryDataPoint>,
    modifier: Modifier = Modifier
) {
    if (historyPoints.isEmpty()) return

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val maxQueries = remember(historyPoints) {
        val highest = historyPoints.maxOfOrNull { it.totalQueries } ?: 100
        ((highest / 200) + 1) * 200
    }.coerceAtLeast(100)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberCardDark)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("history_24h_chart")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "استعلامات آخر 24 ساعة (/api/history)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "رسم بياني حي لمعدل الاستعلامات والمحجوب منها على مدار اليوم",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                )
            }

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الإجمالي", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark, fontSize = 10.sp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ShieldRed)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المحجوب", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark, fontSize = 10.sp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected tooltip / callout if user tapped
        val activePoint = if (selectedIndex in historyPoints.indices) historyPoints[selectedIndex] else historyPoints.lastOrNull()
        if (activePoint != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الوقت: ${activePoint.hourLabel}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimaryDark,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "إجمالي: ${numberFormat.format(activePoint.totalQueries)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "محجوب: ${numberFormat.format(activePoint.blockedQueries)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ShieldRed,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Canvas Area & Line Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(historyPoints) {
                        detectTapGestures { offset ->
                            val stepX = size.width / (historyPoints.size - 1).coerceAtLeast(1)
                            val tappedIndex = (offset.x / stepX).toInt().coerceIn(0, historyPoints.lastIndex)
                            selectedIndex = tappedIndex
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val count = historyPoints.size
                if (count < 2) return@Canvas

                val stepX = width / (count - 1)

                // Draw background grid lines (0%, 50%, 100%)
                val gridColor = Color(0xFF1E2D4A).copy(alpha = 0.5f)
                drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, height * 0.5f), Offset(width, height * 0.5f), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1.dp.toPx())

                val totalPath = Path()
                val totalAreaPath = Path()

                val blockedPath = Path()
                val blockedAreaPath = Path()

                historyPoints.forEachIndexed { index, point ->
                    val x = index * stepX
                    val yTotal = height - ((point.totalQueries.toFloat() / maxQueries.toFloat()) * height).coerceIn(0f, height)
                    val yBlocked = height - ((point.blockedQueries.toFloat() / maxQueries.toFloat()) * height).coerceIn(0f, height)

                    if (index == 0) {
                        totalPath.moveTo(x, yTotal)
                        totalAreaPath.moveTo(x, height)
                        totalAreaPath.lineTo(x, yTotal)

                        blockedPath.moveTo(x, yBlocked)
                        blockedAreaPath.moveTo(x, height)
                        blockedAreaPath.lineTo(x, yBlocked)
                    } else {
                        val prevX = (index - 1) * stepX
                        val prevPoint = historyPoints[index - 1]
                        val prevYTotal = height - ((prevPoint.totalQueries.toFloat() / maxQueries.toFloat()) * height).coerceIn(0f, height)
                        val prevYBlocked = height - ((prevPoint.blockedQueries.toFloat() / maxQueries.toFloat()) * height).coerceIn(0f, height)

                        val cx = (prevX + x) / 2f
                        totalPath.cubicTo(cx, prevYTotal, cx, yTotal, x, yTotal)
                        totalAreaPath.cubicTo(cx, prevYTotal, cx, yTotal, x, yTotal)

                        blockedPath.cubicTo(cx, prevYBlocked, cx, yBlocked, x, yBlocked)
                        blockedAreaPath.cubicTo(cx, prevYBlocked, cx, yBlocked, x, yBlocked)
                    }

                    if (index == count - 1) {
                        totalAreaPath.lineTo(x, height)
                        totalAreaPath.close()

                        blockedAreaPath.lineTo(x, height)
                        blockedAreaPath.close()
                    }
                }

                // Draw Area fills
                val totalAreaGradient = Brush.verticalGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
                drawPath(totalAreaPath, totalAreaGradient)

                val blockedAreaGradient = Brush.verticalGradient(
                    colors = listOf(ShieldRed.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
                drawPath(blockedAreaPath, blockedAreaGradient)

                // Draw lines
                drawPath(
                    totalPath,
                    color = NeonCyan,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                drawPath(
                    blockedPath,
                    color = ShieldRed,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw selected indicator line and point
                if (selectedIndex in 0 until count) {
                    val selX = selectedIndex * stepX
                    val pt = historyPoints[selectedIndex]
                    val selYTotal = height - ((pt.totalQueries.toFloat() / maxQueries.toFloat()) * height).coerceIn(0f, height)
                    val selYBlocked = height - ((pt.blockedQueries.toFloat() / maxQueries.toFloat()) * height).coerceIn(0f, height)

                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(selX, 0f),
                        end = Offset(selX, height),
                        strokeWidth = 1.dp.toPx()
                    )

                    drawCircle(color = NeonCyan, radius = 4.dp.toPx(), center = Offset(selX, selYTotal))
                    drawCircle(color = ShieldRed, radius = 4.dp.toPx(), center = Offset(selX, selYBlocked))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Time labels below chart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelIndices = listOf(0, 6, 12, 18, historyPoints.lastIndex)
            labelIndices.forEach { idx ->
                if (idx in historyPoints.indices) {
                    Text(
                        text = historyPoints[idx].hourLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMutedDark,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}
