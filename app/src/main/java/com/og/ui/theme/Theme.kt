package com.og.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.og.R

/**
 * Lime + deep forest on a pale mint canvas.
 *
 * Validated with the data-viz checker: the heat ramp is one hue, monotone in lightness,
 * lightest step clearing 2:1 on the white card it is drawn on; Accent and Critical clear
 * 3:1 on both surfaces and separate under simulated colour-vision deficiency (ΔE 8.6
 * deutan, 33.3 normal).
 *
 * Two rules the palette depends on:
 *  - [Lime] is a FILL, never a data mark. At 1.29:1 on white it is invisible as a line or
 *    dot; it only ever appears behind dark ink (13.8:1) or on forest (11.0:1).
 *  - There is no amber "warning". Under deutan, gold and yellow-green converge no matter
 *    how they are stepped, so the middle state is neutral grey carrying an icon and a word.
 */
object Og {
    // surfaces
    val Canvas = Color(0xFFEDF4E6)
    val Surface = Color(0xFFFFFFFF)
    val Forest = Color(0xFF14301F)
    val ForestSoft = Color(0xFF1D4029)
    val Inset = Color(0xFFE8EFE2)
    val Hairline = Color(0xFFDCE6D4)

    // brand fill
    val Lime = Color(0xFFC7F24C)
    val LimeDim = Color(0xFFB2DE3A)

    // ink
    val Ink = Color(0xFF0E1A12)
    val InkSecondary = Color(0xFF55605A)
    val InkMuted = Color(0xFF8A948D)
    val OnForest = Color(0xFFF2F7EE)
    val OnForestMuted = Color(0xFF9DB0A3)

    // single-hue sequential ramp for muscle heat, drawn on the white card
    val BodyFill = Color(0xFFEBF0E8)
    val MuscleIdle = Color(0xFFDCE5D8)
    val HeatWeek = Color(0xFF63C877)
    val HeatRecent = Color(0xFF1E9440)
    val HeatToday = Color(0xFF14512A)

    val Accent = Color(0xFF1E9440)
    val AccentSoft = Color(0xFFE0F2E4)
    val Critical = Color(0xFFB0184A)
    val CriticalSoft = Color(0xFFFBE7EE)

    fun heatColor(heat: Float): Color = when {
        heat <= 0f -> MuscleIdle
        heat < 0.5f -> HeatWeek
        heat < 0.85f -> HeatRecent
        else -> HeatToday
    }

    fun scoreColor(score: Int): Color = when {
        score >= 70 -> Accent
        score >= 55 -> InkSecondary
        else -> Critical
    }
}

/**
 * Shared motion. One spring for anything that moves position or size, one short tween for
 * anything that only fades, so the whole app accelerates and settles the same way.
 */
object Motion {
    const val FADE_IN = 200
    const val FADE_OUT = 140

    fun <T> spring() = androidx.compose.animation.core.spring<T>(
        dampingRatio = 0.82f,
        stiffness = 420f,
    )

    /** Counters settle a little slower so the number is readable while it climbs. */
    fun <T> counter() = androidx.compose.animation.core.spring<T>(
        dampingRatio = 1f,
        stiffness = 90f,
    )
}

private val scheme = lightColorScheme(
    primary = Og.Ink,
    onPrimary = Color.White,
    secondary = Og.Accent,
    onSecondary = Color.White,
    background = Og.Canvas,
    onBackground = Og.Ink,
    surface = Og.Surface,
    onSurface = Og.Ink,
    surfaceVariant = Og.Inset,
    onSurfaceVariant = Og.InkSecondary,
    outline = Og.Hairline,
    error = Og.Critical,
    onError = Color.White,
)

/**
 * One variable font file, instanced across the weight axis. Every style below carries the
 * family explicitly — Material only applies a default family to styles it knows about.
 */
private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Jakarta = FontFamily(
    jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800),
)

private fun style(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Normal,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Jakarta,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

private val typography = Typography(
    displayLarge = style(66, 64, FontWeight.ExtraBold, -3.6),
    displayMedium = style(37, 40, FontWeight.ExtraBold, -1.6),
    headlineMedium = style(25, 29, FontWeight.ExtraBold, -1.0),
    titleLarge = style(18, 23, FontWeight.Bold, -0.5),
    titleMedium = style(15, 20, FontWeight.Bold, -0.2),
    bodyLarge = style(15, 22, FontWeight.Medium, -0.1),
    bodyMedium = style(13, 19, FontWeight.Medium, 0.0),
    labelLarge = style(13, 16, FontWeight.Bold, -0.1),
    labelMedium = style(12, 15, FontWeight.SemiBold, 0.0),
    labelSmall = style(10, 13, FontWeight.Bold, 1.0),
)

@Composable
fun OgTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
