package com.og.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.og.data.ExerciseLibrary
import com.og.data.Measurement
import com.og.data.Muscle
import com.og.domain.Analytics
import com.og.domain.Confidence
import com.og.domain.Projection
import com.og.ui.BodyDiagram
import com.og.ui.BodyView
import com.og.ui.Chip
import com.og.ui.EmptyHint
import com.og.ui.OgField
import com.og.ui.PrimaryButton
import com.og.ui.QuietButton
import com.og.ui.HeatLegend
import com.og.ui.LinearMeter
import com.og.ui.OgCard
import com.og.ui.OgViewModel
import com.og.ui.SectionTitle
import com.og.ui.Status
import com.og.ui.StatusBadge
import com.og.ui.UiState
import com.og.ui.theme.Og
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BodyScreen(state: UiState, vm: OgViewModel) {
    var view by remember { mutableStateOf(BodyView.FRONT) }
    var selected by remember { mutableStateOf<Muscle?>(null) }
    var showForm by remember { mutableStateOf(false) }

    val latest = state.measurements.maxByOrNull { it.day }
    val first = state.measurements.minByOrNull { it.day }

    LazyColumn(
        Modifier.fillMaxSize().background(Og.Canvas),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Body", style = MaterialTheme.typography.displayMedium, color = Og.Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                "Tap a muscle to see when you last trained it.",
                style = MaterialTheme.typography.bodyLarge,
                color = Og.InkSecondary,
            )
        }

        item {
            OgCard(padding = 18.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyView.entries.forEach { v ->
                        Chip(v.label, view == v) { view = v }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BodyDiagram(
                        heat = state.heat,
                        view = view,
                        selected = selected,
                        onMuscleTap = { selected = if (selected == it) null else it },
                        callouts = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(14.dp))
                HeatLegend()

                val m = selected
                AnimatedVisibility(m != null) {
                    if (m != null) {
                        Column(Modifier.padding(top = 16.dp)) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Og.Hairline))
                            Spacer(Modifier.height(14.dp))
                            Text(m.label, style = MaterialTheme.typography.titleLarge, color = Og.Ink)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                m.group.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Og.Accent,
                            )
                            Spacer(Modifier.height(10.dp))

                            val recency = Analytics.muscleRecency(state.sets, state.today)[m]
                            val setsThisWeek = state.sets.count {
                                it.day > state.today - 7 &&
                                    ExerciseLibrary[it.exerciseId]?.allMuscles?.contains(m) == true
                            }
                            Text(
                                when (recency) {
                                    null -> "Never trained since you started tracking."
                                    0L -> "Trained today · $setsThisWeek sets this week"
                                    1L -> "Trained yesterday · $setsThisWeek sets this week"
                                    else -> "Last trained $recency days ago · $setsThisWeek sets this week"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Og.InkSecondary,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("BEST EXERCISES", style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
                            Spacer(Modifier.height(6.dp))
                            ExerciseLibrary.all.filter { m in it.primary }.take(4).forEach { ex ->
                                Text(
                                    "· ${ex.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Og.InkSecondary,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- goals ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Goal progress")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Projections come from your own trend line, not a formula. They move as your data moves.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkMuted,
                )
                Spacer(Modifier.height(16.dp))

                val vPoints = state.measurements.mapNotNull { meas ->
                    Analytics.vTaper(meas)?.let { meas.day to it }
                }
                val bfPoints = state.measurements.mapNotNull { meas ->
                    meas.bodyFatPct?.let { meas.day to it }
                }
                val waistPoints = state.measurements.mapNotNull { meas ->
                    meas.waistCm?.let { meas.day to it }
                }

                GoalRow(
                    title = "V-taper (shoulders ÷ waist)",
                    projection = Analytics.project("V-taper", "", vPoints, Analytics.GOLDEN_RATIO),
                    format = { "%.2f".format(it) },
                    start = first?.let { Analytics.vTaper(it) },
                )
                Spacer(Modifier.height(18.dp))
                GoalRow(
                    title = "Body fat",
                    projection = Analytics.project("Body fat", "%", bfPoints, Analytics.TARGET_BODY_FAT),
                    format = { "%.1f%%".format(it) },
                    start = first?.bodyFatPct,
                )
                if (waistPoints.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    val waistGoal = (first?.waistCm ?: 0.0) - 5.0
                    GoalRow(
                        title = "Waist",
                        projection = Analytics.project("Waist", " cm", waistPoints, waistGoal),
                        format = { "%.1f cm".format(it) },
                        start = first?.waistCm,
                    )
                }
            }
        }

        // ---- measurements ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle(
                    "Measurements",
                    trailing = latest?.let {
                        LocalDate.ofEpochDay(it.day).format(DateTimeFormatter.ofPattern("d MMM"))
                    },
                )
                Spacer(Modifier.height(12.dp))
                if (latest == null) {
                    EmptyHint("Nothing logged yet.")
                } else {
                    val rows = listOf(
                        "Weight" to (latest.weightKg to "kg"),
                        "Body fat" to (latest.bodyFatPct to "%"),
                        "Skeletal muscle" to (latest.smmKg to "kg"),
                        "Shoulders" to (latest.shouldersCm to "cm"),
                        "Chest" to (latest.chestCm to "cm"),
                        "Waist" to (latest.waistCm to "cm"),
                        "Arm" to (latest.armCm to "cm"),
                        "Thigh" to (latest.thighCm to "cm"),
                    ).filter { it.second.first != null }

                    rows.forEach { (label, pair) ->
                        val (value, unit) = pair
                        val startValue = when (label) {
                            "Weight" -> first?.weightKg
                            "Body fat" -> first?.bodyFatPct
                            "Skeletal muscle" -> first?.smmKg
                            "Shoulders" -> first?.shouldersCm
                            "Chest" -> first?.chestCm
                            "Waist" -> first?.waistCm
                            "Arm" -> first?.armCm
                            else -> first?.thighCm
                        }
                        val delta = if (startValue != null && value != null) value - startValue else null
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Og.InkSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${trimNum(value!!)} $unit",
                                style = MaterialTheme.typography.titleMedium,
                                color = Og.Ink,
                            )
                            if (delta != null && abs(delta) > 0.05) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    (if (delta > 0) "+" else "") + trimNum(delta),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Og.InkMuted,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (showForm) {
                    QuietButton("Cancel", onClick = { showForm = false })
                } else {
                    PrimaryButton("Log measurements", onClick = { showForm = true })
                }

                AnimatedVisibility(showForm) {
                    MeasurementForm(latest) { m ->
                        vm.saveMeasurement(m.copy(day = state.today))
                        showForm = false
                    }
                }
            }
        }

        if (state.profile?.creatineStartDay != null) {
            item {
                OgCard(padding = 16.dp) {
                    StatusBadge(Status.NEUTRAL, "Creatine started")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scale readings in the ${Analytics.CREATINE_WATER_WEEKS} weeks after " +
                            LocalDate.ofEpochDay(state.profile.creatineStartDay)
                                .format(DateTimeFormatter.ofPattern("d MMM")) +
                            " are held out of the weight trend — that rise is water in muscle, not fat. " +
                            "Waist and the V-taper ratio stay reliable throughout.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Og.InkMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalRow(
    title: String,
    projection: Projection,
    format: (Double) -> String,
    start: Double?,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Og.Ink)
            Text(
                if (projection.current == 0.0) "—" else format(projection.current),
                style = MaterialTheme.typography.titleMedium,
                color = Og.Ink,
            )
        }
        Spacer(Modifier.height(8.dp))
        val progress = if (start != null && abs(projection.goal - start) > 1e-6) {
            ((projection.current - start) / (projection.goal - start)).toFloat().coerceIn(0f, 1f)
        } else 0f
        LinearMeter(progress, height = 6.dp)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "goal ${format(projection.goal)}",
                style = MaterialTheme.typography.labelMedium,
                color = Og.InkMuted,
            )
            if (projection.weeks != null) {
                Text(
                    "~${projection.weeks.roundToInt()} weeks",
                    style = MaterialTheme.typography.labelMedium,
                    color = when (projection.confidence) {
                        Confidence.MODERATE -> Og.Accent
                        Confidence.LOW -> Og.InkSecondary
                        Confidence.NONE -> Og.InkMuted
                    },
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(projection.message, style = MaterialTheme.typography.bodyMedium, color = Og.InkMuted)
    }
}

@Composable
private fun MeasurementForm(previous: Measurement?, onSave: (Measurement) -> Unit) {
    var weight by remember { mutableStateOf(previous?.weightKg?.let { trimNum(it) } ?: "") }
    var bodyFat by remember { mutableStateOf(previous?.bodyFatPct?.let { trimNum(it) } ?: "") }
    var smm by remember { mutableStateOf(previous?.smmKg?.let { trimNum(it) } ?: "") }
    var shoulders by remember { mutableStateOf(previous?.shouldersCm?.let { trimNum(it) } ?: "") }
    var chest by remember { mutableStateOf(previous?.chestCm?.let { trimNum(it) } ?: "") }
    var waist by remember { mutableStateOf(previous?.waistCm?.let { trimNum(it) } ?: "") }
    var hips by remember { mutableStateOf(previous?.hipsCm?.let { trimNum(it) } ?: "") }
    var arm by remember { mutableStateOf(previous?.armCm?.let { trimNum(it) } ?: "") }
    var thigh by remember { mutableStateOf(previous?.thighCm?.let { trimNum(it) } ?: "") }

    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OgField("Weight", weight, { weight = it }, Modifier.weight(1f), "kg")
            OgField("Body fat", bodyFat, { bodyFat = it }, Modifier.weight(1f), "%")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OgField("Muscle", smm, { smm = it }, Modifier.weight(1f), "kg")
            OgField("Shoulders", shoulders, { shoulders = it }, Modifier.weight(1f), "cm")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OgField("Chest", chest, { chest = it }, Modifier.weight(1f), "cm")
            OgField("Waist", waist, { waist = it }, Modifier.weight(1f), "cm")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OgField("Hips", hips, { hips = it }, Modifier.weight(1f), "cm")
            OgField("Arm", arm, { arm = it }, Modifier.weight(1f), "cm")
        }
        OgField("Thigh", thigh, { thigh = it }, Modifier.fillMaxWidth(), "cm")

        PrimaryButton("Save", onClick = {
            onSave(
                Measurement(
                    day = 0,
                    weightKg = weight.toDoubleOrNull(),
                    bodyFatPct = bodyFat.toDoubleOrNull(),
                    smmKg = smm.toDoubleOrNull(),
                    shouldersCm = shoulders.toDoubleOrNull(),
                    chestCm = chest.toDoubleOrNull(),
                    waistCm = waist.toDoubleOrNull(),
                    hipsCm = hips.toDoubleOrNull(),
                    armCm = arm.toDoubleOrNull(),
                    thighCm = thigh.toDoubleOrNull(),
                ),
            )
        })
    }
}
