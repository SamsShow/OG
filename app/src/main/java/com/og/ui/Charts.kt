package com.og.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.og.data.MuscleGroup
import com.og.domain.Fit
import com.og.ui.theme.Og
import kotlin.math.abs
import kotlin.math.roundToInt

data class ChartPoint(val x: Long, val y: Double, val label: String)

private val axisStyle = TextStyle(fontSize = 10.sp, color = Og.InkMuted)
private val valueStyle = TextStyle(fontSize = 12.sp, color = Og.Ink)

/**
 * One series over time, plus its own least-squares fit as a dashed neutral line —
 * the fit is the same entity, so it never takes a second series colour.
 * Tap a point to read it; on touch that is the crosshair.
 */
@Composable
fun TrendLineChart(
    points: List<ChartPoint>,
    unit: String,
    modifier: Modifier = Modifier,
    fit: Fit? = null,
    goal: Double? = null,
    /** Marks points the creatine water-weight window makes unreliable. */
    confounded: (Long) -> Boolean = { false },
) {
    if (points.size < 2) {
        EmptyHint("Two entries needed before a trend can be drawn.")
        return
    }
    val measurer = rememberTextMeasurer()
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    val xs = points.map { it.x }
    val minX = xs.min().toFloat()
    val maxX = xs.max().toFloat()
    val ysAll = points.map { it.y } + listOfNotNull(goal)
    var minY = ysAll.min()
    var maxY = ysAll.max()
    val pad = ((maxY - minY) * 0.15).coerceAtLeast(0.5)
    minY -= pad
    maxY += pad

    Canvas(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(points) {
                detectTapGestures { tap ->
                    val left = 40f
                    val right = size.width - 12f
                    val span = (maxX - minX).coerceAtLeast(1f)
                    val idx = points.indices.minByOrNull { i ->
                        val px = left + (points[i].x - minX) / span * (right - left)
                        abs(px - tap.x)
                    }
                    selected = if (selected == idx) null else idx
                }
            },
    ) {
        val left = 40f
        val right = size.width - 12f
        val top = 18f
        val bottom = size.height - 22f
        val spanX = (maxX - minX).coerceAtLeast(1f)
        val spanY = (maxY - minY).coerceAtLeast(1e-6)

        fun px(x: Long) = left + (x - minX) / spanX * (right - left)
        fun py(y: Double) = (bottom - (y - minY) / spanY * (bottom - top)).toFloat()

        // recessive grid: three hairlines, no box
        for (f in listOf(0f, 0.5f, 1f)) {
            val y = top + f * (bottom - top)
            drawLine(Og.Hairline, Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }
        drawText(
            measurer, "${maxY.roundToInt()}", topLeft = Offset(0f, top - 6f), style = axisStyle,
        )
        drawText(
            measurer, "${minY.roundToInt()}", topLeft = Offset(0f, bottom - 6f), style = axisStyle,
        )

        // goal line — a baseline, labelled, so it is never mistaken for a series
        if (goal != null) {
            val gy = py(goal)
            drawLine(
                Og.InkMuted, Offset(left, gy), Offset(right, gy), strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 6f)),
            )
            drawText(
                measurer, "goal", topLeft = Offset(right - 26f, gy - 14f),
                style = axisStyle,
            )
        }

        // fitted trend, dashed and neutral
        if (fit != null) {
            val y0 = fit.slopePerDay * minX + fit.intercept
            val y1 = fit.slopePerDay * maxX + fit.intercept
            drawLine(
                Og.InkSecondary.copy(alpha = 0.55f),
                Offset(px(minX.toLong()), py(y0)),
                Offset(px(maxX.toLong()), py(y1)),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)),
            )
        }

        // the series — 2px line
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = px(p.x)
            val y = py(p.y)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Og.Accent, style = Stroke(width = 2f, cap = StrokeCap.Round))

        // markers ≥8px; confounded points are hollow so they read as "do not trust"
        points.forEachIndexed { i, p ->
            val c = Offset(px(p.x), py(p.y))
            val isOut = confounded(p.x)
            drawCircle(Og.Surface, radius = 5.5f, center = c)
            if (isOut) {
                drawCircle(Og.InkMuted, radius = 4.5f, center = c, style = Stroke(width = 2f))
            } else {
                drawCircle(Og.Accent, radius = 4.5f, center = c)
            }
            if (i == selected) {
                drawLine(Og.Hairline, Offset(c.x, top), Offset(c.x, bottom), strokeWidth = 1f)
                drawCircle(Og.Ink, radius = 6.5f, center = c, style = Stroke(width = 2f))
            }
        }

        // selective direct labels only: first, last, and any tapped point
        val show = listOfNotNull(0, points.lastIndex, selected).distinct()
        show.forEach { i ->
            val p = points[i]
            val c = Offset(px(p.x), py(p.y))
            val text = "${trim(p.y)}$unit"
            val m = measurer.measure(text, valueStyle)
            val tx = (c.x - m.size.width / 2).coerceIn(left, right - m.size.width)
            drawText(measurer, text, topLeft = Offset(tx, (c.y - 24f).coerceAtLeast(0f)), style = valueStyle)
        }

        drawText(measurer, points.first().label, topLeft = Offset(left, bottom + 6f), style = axisStyle)
        val lastLabel = measurer.measure(points.last().label, axisStyle)
        drawText(
            measurer, points.last().label,
            topLeft = Offset(right - lastLabel.size.width, bottom + 6f), style = axisStyle,
        )
    }
}

/** Columns for a single measure over time: one hue, rounded top ends on the baseline, 2px gaps. */
@Composable
fun VolumeBars(bars: List<Pair<String, Double>>, modifier: Modifier = Modifier, unit: String = "kg") {
    if (bars.isEmpty()) {
        EmptyHint("No training volume logged yet.")
        return
    }
    // A single column is not a chart — show the number and say when the trend arrives.
    if (bars.size == 1) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${trim(bars[0].second)}",
                style = MaterialTheme.typography.headlineMedium,
                color = Og.Ink,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                unit,
                style = MaterialTheme.typography.labelMedium,
                color = Og.InkMuted,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        EmptyHint("One period logged so far — the trend appears once there are two.")
        return
    }
    val measurer = rememberTextMeasurer()
    val max = bars.maxOf { it.second }.coerceAtLeast(1.0)

    Canvas(modifier.fillMaxWidth().height(150.dp)) {
        val top = 20f
        val bottom = size.height - 32f
        val gap = 2f
        val slot = size.width / bars.size
        val barW = (slot - gap * 2).coerceAtMost(46f)

        bars.forEachIndexed { i, (label, value) ->
            val h = ((value / max) * (bottom - top)).toFloat()
            val x = i * slot + (slot - barW) / 2
            val isLast = i == bars.lastIndex
            drawRoundRectTopped(
                x = x, y = bottom - h, w = barW, h = h,
                color = if (isLast) Og.Accent else Og.HeatWeek.copy(alpha = 0.55f),
            )
            val m = measurer.measure(label, axisStyle)
            drawText(
                measurer, label,
                topLeft = Offset(x + barW / 2 - m.size.width / 2, bottom + 4f),
                style = axisStyle,
            )
        }
        // direct-label the newest column only, never every bar
        val lastVal = bars.last().second
        val text = "${trim(lastVal)} $unit"
        val m = measurer.measure(text, valueStyle)
        val lastH = ((lastVal / max) * (bottom - top)).toFloat()
        val lastX = bars.lastIndex * slot + (slot - barW) / 2
        drawText(
            measurer, text,
            topLeft = Offset(
                (lastX + barW / 2 - m.size.width / 2).coerceIn(0f, size.width - m.size.width),
                (bottom - lastH - 18f).coerceAtLeast(0f),
            ),
            style = valueStyle,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectTopped(
    x: Float, y: Float, w: Float, h: Float, color: Color,
) {
    val r = 4f.coerceAtMost(h)
    val path = Path().apply {
        moveTo(x, y + h)
        lineTo(x, y + r)
        quadraticTo(x, y, x + r, y)
        lineTo(x + w - r, y)
        quadraticTo(x + w, y, x + w, y + r)
        lineTo(x + w, y + h)
        close()
    }
    drawPath(path, color)
}

/**
 * Sessions per muscle group against the weekly target. This is a delta-to-baseline
 * question, so every row shows the same baseline and shortfalls carry an icon and
 * words — not just a colour.
 */
@Composable
fun GroupBalance(
    sessions: Map<MuscleGroup, Int>,
    target: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MuscleGroup.entries.forEach { group ->
            val n = sessions[group] ?: 0
            val short = n < target
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    group.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkSecondary,
                    modifier = Modifier.width(78.dp),
                )
                LinearMeter(
                    progress = (n.toFloat() / target).coerceAtMost(1f),
                    color = if (short) Og.InkSecondary else Og.Accent,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "$n/$target",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (short) Og.InkSecondary else Og.Ink,
                    modifier = Modifier.width(30.dp),
                )
            }
        }
        val thin = MuscleGroup.entries.filter { (sessions[it] ?: 0) < target }
        if (thin.isNotEmpty()) {
            StatusBadge(Status.WARNING, "Behind on ${thin.joinToString(", ") { it.label }}")
        } else {
            StatusBadge(Status.GOOD, "Every group hit $target+ times this week")
        }
    }
}

private fun trim(v: Double): String =
    if (abs(v - v.roundToInt()) < 0.05) "${v.roundToInt()}" else String.format("%.1f", v)
