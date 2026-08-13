package com.og.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.og.data.Muscle
import com.og.ui.theme.Og

enum class BodyView(val label: String) { FRONT("Front"), BACK("Back") }

/**
 * Geometry lives in a fixed 200 x 440 space, authored for the left half and mirrored so
 * the figure cannot drift out of symmetry.
 *
 * Each muscle is a LIST of parts, not one blob, because that is what makes the diagram
 * readable: abs are eight separate blocks, quads are three heads, biceps and triceps two
 * each, calves two. Parts of the same muscle light up together but stay visually distinct,
 * so you can see the shape of what a lift actually hits.
 */
private object Body {
    const val W = 200f
    const val H = 440f

    fun pts(vararg xy: Float): List<Offset> =
        xy.toList().chunked(2).map { Offset(it[0], it[1]) }

    fun mirror(p: List<Offset>): List<Offset> = p.map { Offset(W - it.x, it.y) }

    /**
     * Muscular build: 124 units across the deltoids against a 52-unit waist — a 2.4 ratio,
     * which is roughly a lean, developed physique rather than the mannequin this replaced.
     * Arms hang clear of the ribcage so the lats and serratus stay visible.
     */
    private val halfOutline = pts(
        101f, 8f, 113f, 12f, 119f, 28f, 118f, 46f, 112f, 58f,       // skull and jaw
        115f, 66f, 122f, 76f,                                        // thick neck into trap
        138f, 84f, 155f, 92f, 166f, 104f, 168f, 122f,                // trap slope, deltoid cap
        165f, 144f, 161f, 172f, 158f, 198f,                          // upper arm to elbow
        156f, 216f, 152f, 242f, 148f, 264f,                          // forearm to wrist
        152f, 276f, 146f, 294f, 137f, 290f,                          // hand
        140f, 266f, 143f, 242f, 145f, 216f, 147f, 196f,              // back up the inner arm
        145f, 168f, 143f, 142f, 138f, 108f,                          // armpit apex
        141f, 140f, 142f, 160f, 135f, 188f, 125f, 216f,              // lat flare into the waist
        130f, 238f, 136f, 254f,                                      // hip
        139f, 280f, 138f, 304f, 132f, 334f, 127f, 356f,              // thigh to knee
        131f, 386f, 123f, 412f, 115f, 426f,                          // calf to ankle
        125f, 434f, 121f, 440f, 103f, 440f,                          // foot
        102f, 410f, 105f, 362f, 107f, 314f, 101f, 272f,              // back up the inner leg
    )

    val outline: List<Offset> = halfOutline + mirror(halfOutline).reversed()

    val front: List<Pair<Muscle, List<List<Offset>>>> = listOf(
        Muscle.TRAPS_UPPER to listOf(
            pts(98f, 66f, 98f, 84f, 78f, 86f, 66f, 82f, 82f, 70f),
        ),
        // Deltoid heads, drawn oversized and trimmed by the silhouette clip so they fill
        // the shoulder cap completely instead of leaving a gap at the edge.
        Muscle.SIDE_DELT to listOf(
            pts(50f, 86f, 34f, 94f, 28f, 118f, 36f, 142f, 48f, 136f, 52f, 110f),
        ),
        Muscle.FRONT_DELT to listOf(
            pts(51f, 86f, 53f, 110f, 49f, 136f, 63f, 142f, 71f, 118f, 67f, 92f),
        ),
        // Pec heads fanned along their fibre direction — clavicular sweeping up and out,
        // sternal across, lower tucking up under the ribs. Banding them flat made the
        // chest read as one bar across the torso.
        Muscle.CHEST_UPPER to listOf(
            pts(97f, 90f, 97f, 107f, 80f, 113f, 68f, 107f, 63f, 96f, 74f, 88f, 88f, 87f),
        ),
        Muscle.CHEST_MID to listOf(
            pts(97f, 108f, 97f, 127f, 82f, 131f, 68f, 125f, 61f, 112f, 66f, 105f, 80f, 114f),
        ),
        Muscle.CHEST_LOWER to listOf(
            pts(97f, 128f, 97f, 151f, 86f, 157f, 71f, 151f, 62f, 134f, 66f, 125f, 81f, 132f),
        ),
        // Two bicep heads, sized to the thicker arm.
        Muscle.BICEPS to listOf(
            pts(48f, 142f, 34f, 146f, 38f, 188f, 48f, 196f, 50f, 172f),
            pts(49f, 142f, 50f, 172f, 48f, 196f, 56f, 194f, 58f, 170f, 58f, 144f),
        ),
        // Brachioradialis and the flexor mass.
        Muscle.FOREARMS to listOf(
            pts(54f, 200f, 42f, 206f, 46f, 250f, 54f, 264f, 56f, 238f),
            pts(55f, 200f, 56f, 238f, 55f, 264f, 60f, 262f, 62f, 240f, 62f, 206f),
        ),
        // Four rows of rectus abdominis per side — eight blocks across the midsection.
        Muscle.ABS to listOf(
            pts(97f, 156f, 97f, 174f, 85f, 173f, 84f, 157f),
            pts(97f, 178f, 97f, 196f, 85f, 195f, 84f, 179f),
            pts(97f, 200f, 97f, 218f, 86f, 216f, 85f, 201f),
            pts(97f, 222f, 97f, 240f, 88f, 237f, 87f, 223f),
        ),
        // Serratus fingers over the ribs, then the oblique wall.
        Muscle.OBLIQUES to listOf(
            pts(82f, 138f, 77f, 150f, 79f, 162f, 84f, 156f, 84f, 142f),
            pts(83f, 166f, 86f, 196f, 88f, 222f, 79f, 210f, 76f, 180f),
        ),
        // Rectus femoris, vastus lateralis, and the teardrop.
        Muscle.QUADS to listOf(
            pts(97f, 262f, 97f, 330f, 86f, 334f, 86f, 266f),
            pts(85f, 266f, 73f, 270f, 66f, 300f, 74f, 330f, 85f, 334f),
            pts(97f, 332f, 97f, 350f, 87f, 346f, 86f, 335f),
        ),
        Muscle.CALVES to listOf(
            pts(80f, 364f, 72f, 374f, 70f, 400f, 78f, 410f, 82f, 388f),
            pts(84f, 364f, 82f, 388f, 79f, 410f, 88f, 411f, 92f, 386f, 90f, 366f),
        ),
    )

    val back: List<Pair<Muscle, List<List<Offset>>>> = listOf(
        Muscle.TRAPS_UPPER to listOf(
            pts(98f, 64f, 98f, 84f, 74f, 88f, 64f, 80f, 80f, 66f),
        ),
        Muscle.TRAPS_MID to listOf(
            pts(98f, 86f, 98f, 130f, 76f, 126f, 70f, 104f, 76f, 88f),
        ),
        Muscle.REAR_DELT to listOf(
            pts(52f, 86f, 34f, 94f, 28f, 118f, 38f, 142f, 58f, 136f, 62f, 104f),
        ),
        // Lat: the wide upper flare and the taper into the waist — the V, drawn.
        Muscle.LATS to listOf(
            pts(76f, 128f, 98f, 132f, 98f, 164f, 80f, 166f, 68f, 150f, 70f, 132f),
            pts(80f, 167f, 98f, 165f, 98f, 196f, 88f, 205f, 78f, 187f),
        ),
        Muscle.LOWER_BACK to listOf(
            pts(97f, 200f, 97f, 234f, 89f, 231f, 88f, 202f),
        ),
        // Long and lateral triceps heads.
        Muscle.TRICEPS to listOf(
            pts(48f, 142f, 34f, 146f, 38f, 188f, 48f, 196f, 50f, 172f),
            pts(49f, 142f, 50f, 172f, 48f, 196f, 56f, 194f, 58f, 170f, 58f, 144f),
        ),
        Muscle.FOREARMS to listOf(
            pts(54f, 200f, 42f, 206f, 46f, 250f, 54f, 264f, 56f, 238f),
            pts(55f, 200f, 56f, 238f, 55f, 264f, 60f, 262f, 62f, 240f, 62f, 206f),
        ),
        Muscle.GLUTES to listOf(
            pts(97f, 236f, 97f, 278f, 78f, 272f, 68f, 250f, 80f, 236f),
        ),
        // Biceps femoris and the semitendinosus group.
        Muscle.HAMSTRINGS to listOf(
            pts(97f, 282f, 97f, 336f, 87f, 338f, 87f, 286f),
            pts(86f, 286f, 86f, 338f, 74f, 332f, 68f, 300f, 76f, 282f),
        ),
        // Two gastrocnemius heads.
        Muscle.CALVES to listOf(
            pts(97f, 362f, 97f, 404f, 89f, 412f, 85f, 382f, 89f, 364f),
            pts(88f, 364f, 84f, 382f, 88f, 412f, 78f, 408f, 72f, 378f, 78f, 362f),
        ),
    )

    fun regionsFor(view: BodyView) = if (view == BodyView.FRONT) front else back
}

/** Inserts a midpoint between every pair so the spline keeps the authored shape. */
private fun densify(pts: List<Offset>): List<Offset> = buildList {
    for (i in pts.indices) {
        val a = pts[i]
        val b = pts[(i + 1) % pts.size]
        add(a)
        add(Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f))
    }
}

/** Closed Catmull-Rom spline through the points, converted to cubic Beziers. */
private fun spline(pts: List<Offset>, s: Float, dx: Float, dy: Float): Path {
    val p = pts.map { Offset(it.x * s + dx, it.y * s + dy) }
    val n = p.size
    val path = Path()
    if (n < 3) return path
    path.moveTo(p[0].x, p[0].y)
    for (i in 0 until n) {
        val p0 = p[(i - 1 + n) % n]
        val p1 = p[i]
        val p2 = p[(i + 1) % n]
        val p3 = p[(i + 2) % n]
        path.cubicTo(
            p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
            p2.x, p2.y,
        )
    }
    path.close()
    return path
}

private fun contains(pts: List<Offset>, x: Float, y: Float): Boolean {
    var inside = false
    var j = pts.size - 1
    for (i in pts.indices) {
        val a = pts[i]
        val b = pts[j]
        if ((a.y > y) != (b.y > y) &&
            x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x
        ) inside = !inside
        j = i
    }
    return inside
}

private fun centroid(parts: List<List<Offset>>): Offset {
    val all = parts.flatten()
    return Offset(all.map { it.x }.average().toFloat(), all.map { it.y }.average().toFloat())
}

@Composable
fun BodyDiagram(
    heat: Map<Muscle, Float>,
    view: BodyView,
    modifier: Modifier = Modifier,
    selected: Muscle? = null,
    onMuscleTap: (Muscle) -> Unit = {},
    /** Adds side margins and labels the muscles worked, with a leader to each one. */
    callouts: Boolean = false,
) {
    val regions = Body.regionsFor(view)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Og.Ink)

    val ratio = if (callouts) 430f / Body.H else Body.W / Body.H
    val figureFraction = if (callouts) 0.46f else 1f

    Canvas(
        modifier
            .aspectRatio(ratio)
            .pointerInput(view, regions, callouts) {
                detectTapGestures { tap ->
                    val s = minOf(size.width * figureFraction / Body.W, size.height / Body.H)
                    val dx = (size.width - Body.W * s) / 2f
                    val dy = (size.height - Body.H * s) / 2f
                    val x = (tap.x - dx) / s
                    val y = (tap.y - dy) / s
                    regions.firstOrNull { (_, parts) ->
                        parts.any { contains(it, x, y) || contains(Body.mirror(it), x, y) }
                    }?.let { onMuscleTap(it.first) }
                }
            },
    ) {
        val s = minOf(size.width * figureFraction / Body.W, size.height / Body.H)
        val dx = (size.width - Body.W * s) / 2f
        val dy = (size.height - Body.H * s) / 2f

        val body = spline(Body.outline, s, dx, dy)
        drawPath(body, Og.BodyFill)

        // Clipping to the silhouette means a muscle can never spill outside the body,
        // however the outline is later reshaped.
        clipPath(body) {
            for ((muscle, parts) in regions) {
                val h = heat[muscle] ?: 0f
                val fill = if (h > 0f) Og.heatColor(h) else Og.MuscleIdle
                for (part in parts) {
                    for (poly in listOf(part, Body.mirror(part))) {
                        val path = spline(densify(poly), s, dx, dy)
                        drawPath(path, fill)
                        // A hairline of body colour between parts keeps the heads separate
                        // instead of merging into one slab.
                        drawPath(path, Og.BodyFill, style = Stroke(width = 0.9f * s))
                        if (muscle == selected) {
                            drawPath(path, Og.Ink, style = Stroke(width = 1.1f * s))
                        }
                    }
                }
            }
        }

        if (!callouts) return@Canvas

        val worked = regions
            .filter { (m, _) -> (heat[m] ?: 0f) > 0f }
            .sortedBy { (_, parts) -> centroid(parts).y }
            .take(4)

        worked.forEachIndexed { i, (muscle, parts) ->
            val onLeft = i % 2 == 0
            val c = centroid(if (onLeft) parts else parts.map { Body.mirror(it) })
            val anchor = Offset(c.x * s + dx, c.y * s + dy)

            val h = heat[muscle] ?: 0f
            val whenText = when {
                h >= 0.85f -> "today"
                h >= 0.5f -> "yesterday"
                else -> "this week"
            }
            val text = "${muscle.label} · $whenText"
            val m = measurer.measure(text, labelStyle)
            val padV = 6f
            val dotGap = 20f
            val padRight = 10f
            val pillW = dotGap + m.size.width + padRight
            val pillH = m.size.height + padV * 2

            val pillX = if (onLeft) (dx - 14f - pillW).coerceAtLeast(2f)
            else (dx + Body.W * s + 14f).coerceAtMost(size.width - pillW - 2f)
            val pillY = (anchor.y - pillH / 2).coerceIn(2f, size.height - pillH - 2f)

            val leaderFrom = Offset(if (onLeft) pillX + pillW else pillX, pillY + pillH / 2)
            drawLine(Og.Hairline, leaderFrom, anchor, strokeWidth = 1.5f)
            drawCircle(Og.heatColor(h), radius = 3.5f, center = anchor)

            drawRoundRect(
                color = Og.Inset,
                topLeft = Offset(pillX, pillY),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(pillH / 2),
            )
            drawCircle(
                Og.heatColor(h), radius = 3.5f,
                center = Offset(pillX + 11f, pillY + pillH / 2),
            )
            drawText(measurer, text, topLeft = Offset(pillX + dotGap, pillY + padV), style = labelStyle)
        }
    }
}

/** Scale legend for the sequential ramp, plus the off-ramp resting state. */
@Composable
fun HeatLegend(modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            Og.MuscleIdle to "Rested",
            Og.HeatWeek to "This week",
            Og.HeatRecent to "Yesterday",
            Og.HeatToday to "Today",
        ).forEach { (color, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Text(label, style = MaterialTheme.typography.labelMedium, color = Og.InkMuted)
            }
        }
    }
}
