package com.og.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.og.ui.theme.Og
import kotlin.math.roundToInt

enum class Status { GOOD, WARNING, CRITICAL, NEUTRAL }

val Status.color: Color
    get() = when (this) {
        Status.GOOD -> Og.Accent
        Status.WARNING -> Og.InkSecondary
        Status.CRITICAL -> Og.Critical
        Status.NEUTRAL -> Og.InkSecondary
    }

val Status.tint: Color
    get() = when (this) {
        Status.GOOD -> Og.AccentSoft
        Status.WARNING -> Og.Inset
        Status.CRITICAL -> Og.CriticalSoft
        Status.NEUTRAL -> Og.Inset
    }

val Status.icon: ImageVector
    get() = when (this) {
        Status.GOOD -> Icons.Filled.CheckCircle
        Status.WARNING -> Icons.Filled.WarningAmber
        Status.CRITICAL -> Icons.Filled.ErrorOutline
        Status.NEUTRAL -> Icons.AutoMirrored.Filled.TrendingFlat
    }

@Composable
fun OgCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .shadow(2.dp, shape, ambientColor = Og.Ink.copy(alpha = 0.5f), spotColor = Og.Ink.copy(alpha = 0.5f))
            .clip(shape)
            .background(Og.Surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = Og.InkMuted)
        }
    }
}

/** Status never rides on colour alone — the icon and the word carry it too. */
@Composable
fun StatusBadge(status: Status, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(status.tint)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(status.icon, contentDescription = null, tint = status.color, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = status.color)
    }
}

/** KPI tile: value + optional delta. Not a one-bar bar chart. */
@Composable
fun StatTile(
    label: String,
    value: String,
    unit: String? = null,
    delta: String? = null,
    deltaStatus: Status = Status.NEUTRAL,
    modifier: Modifier = Modifier,
) {
    OgCard(modifier, padding = 14.dp) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Og.InkMuted, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Og.Ink)
            if (unit != null) {
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = Og.InkMuted,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        if (delta != null) {
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(
                    when (deltaStatus) {
                        Status.GOOD -> Icons.AutoMirrored.Filled.TrendingUp
                        Status.CRITICAL -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.AutoMirrored.Filled.TrendingFlat
                    },
                    contentDescription = null,
                    tint = deltaStatus.color,
                    modifier = Modifier.size(13.dp),
                )
                Text(delta, style = MaterialTheme.typography.labelMedium, color = deltaStatus.color)
            }
        }
    }
}

/**
 * A ratio against a limit — a meter on a same-ramp track, not a two-slice pie.
 * The track is the ramp's recessive step so the arc reads as fill, not a second series.
 */
@Composable
fun RingMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 13.dp,
    color: Color = Og.Accent,
    center: @Composable () -> Unit,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1.35f), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = stroke.toPx()
            val arcSize = Size(size.width - w, size.height - w)
            val topLeft = Offset(w / 2, w / 2)

            drawArc(
                color = Og.Inset, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = w, cap = StrokeCap.Round),
            )
            // Past 100% the overflow wraps in the ramp's mid step so "over" stays visible.
            if (animated > 1f) {
                drawArc(
                    color = Og.HeatWeek, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = w, cap = StrokeCap.Round),
                )
            }
            drawArc(
                color = color, startAngle = -90f, sweepAngle = 360f * animated.coerceAtMost(1f),
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = w, cap = StrokeCap.Round),
            )
        }
        center()
    }
}

@Composable
fun LinearMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = Og.Accent,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "meter")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Og.Inset)) {
        Box(Modifier.fillMaxWidth(animated).height(height).clip(CircleShape).background(color))
    }
}

/** The one number a screen leads with. */
@Composable
fun HeroFigure(value: String, caption: String, color: Color = Og.Ink, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.displayLarge, color = color)
        Spacer(Modifier.height(6.dp))
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = Og.InkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Og.InkMuted,
        modifier = modifier.padding(vertical = 6.dp),
    )
}

/** Label above, filled box below — no floating notch label. */
@Composable
fun OgField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    placeholder: String = "—",
    numeric: Boolean = true,
) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
        Spacer(Modifier.height(7.dp))
        BasicTextField(
            value = value,
            onValueChange = { new ->
                onValueChange(if (numeric) new.filter { it.isDigit() || it == '.' } else new)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = Og.Ink),
            cursorBrush = SolidColor(Og.Accent),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { field ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Og.Inset)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = MaterialTheme.typography.titleLarge, color = Og.InkMuted)
                        }
                        field()
                    }
                    if (suffix != null) {
                        Text(suffix, style = MaterialTheme.typography.labelMedium, color = Og.InkMuted)
                    }
                }
            },
        )
    }
}

/** Lime fill with dark ink on top — 13.8:1, and the only safe way to use lime. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Og.Lime,
    content: Color = Og.Ink,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = content)
    }
}

/** Dark card for the one thing a screen leads with. */
@Composable
fun ForestCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .clip(shape)
            .background(Og.Forest)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Circular icon chip used down the left of list rows. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    container: Color = Og.Lime,
    tint: Color = Og.Forest,
    size: Dp = 52.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Og.Hairline), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Og.Ink)
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val container by animateColorAsState(if (selected) Og.Ink else Og.Inset, label = "chip")
    val label by animateColorAsState(
        if (selected) Color.White else Og.InkSecondary,
        label = "chipLabel",
    )
    Box(
        modifier
            .clip(CircleShape)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = label)
    }
}

fun Double.g(): String = "${this.roundToInt()}"
