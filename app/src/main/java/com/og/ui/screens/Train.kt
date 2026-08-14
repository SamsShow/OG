package com.og.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.og.data.Equipment
import com.og.data.Exercise
import com.og.data.ExerciseLibrary
import com.og.data.Muscle
import com.og.data.MuscleGroup
import com.og.data.SetLog
import com.og.ui.BodyDiagram
import com.og.ui.BodyView
import com.og.ui.Chip
import com.og.ui.EmptyHint
import com.og.ui.IconBadge
import com.og.ui.OgField
import com.og.ui.PrimaryButton
import com.og.ui.QuietButton
import com.og.ui.OgCard
import com.og.ui.SectionTitle
import com.og.ui.StatTile
import com.og.ui.Status
import com.og.ui.StatusBadge
import com.og.ui.UiState
import com.og.ui.theme.Motion
import com.og.ui.theme.Og
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val Equipment.icon: ImageVector
    get() = when (this) {
        Equipment.BARBELL, Equipment.DUMBBELL -> Icons.Filled.FitnessCenter
        Equipment.MACHINE -> Icons.Filled.Tune
        Equipment.CABLE -> Icons.Filled.Cable
        Equipment.BODYWEIGHT -> Icons.Filled.Accessibility
    }

@Composable
fun TrainScreen(state: UiState, vm: com.og.ui.OgViewModel) {
    var selected by remember { mutableStateOf<Exercise?>(null) }

    // Detail pushes in from the right and pops back out the same way, so the back gesture
    // has somewhere to point.
    AnimatedContent(
        targetState = selected,
        transitionSpec = {
            val opening = targetState != null
            val enterFrom = if (opening) 1 else -1
            (
                slideInHorizontally(Motion.spring()) { (it * 0.28f * enterFrom).toInt() } +
                    fadeIn(tween(Motion.FADE_IN))
                ) togetherWith (
                slideOutHorizontally(Motion.spring()) { (-it * 0.14f * enterFrom).toInt() } +
                    fadeOut(tween(Motion.FADE_OUT))
                )
        },
        label = "exercise",
    ) { ex ->
        if (ex != null) {
            ExerciseDetail(ex, state, vm, onBack = { selected = null })
        } else {
            ExerciseList(
                state,
                onPick = { selected = it },
                onResetDay = vm::resetToToday,
                onAdd = vm::addCustomExercise,
            )
        }
    }
}

/**
 * Adds a lift the built-in library is missing. The muscle choices are filtered to the
 * chosen group, so a custom lift always resolves to real muscles and shows up on the body
 * diagram and in coverage rather than counting for nothing.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AddExerciseCard(
    preset: MuscleGroup?,
    onAdd: (String, MuscleGroup, List<Muscle>, Equipment) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var group by remember(preset) { mutableStateOf(preset ?: MuscleGroup.CHEST) }
    var muscles by remember { mutableStateOf(setOf<Muscle>()) }
    var equipment by remember { mutableStateOf(Equipment.MACHINE) }

    OgCard(padding = 16.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Add, container = Og.Inset, tint = Og.Ink, size = 44.dp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Add an exercise", style = MaterialTheme.typography.titleMedium, color = Og.Ink)
                Text(
                    "Missing a lift? Add it with the muscles it hits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkMuted,
                )
            }
        }

        if (open) {
            Spacer(Modifier.height(16.dp))
            OgField("Name", name, { name = it }, numeric = false, placeholder = "Pec Deck")

            Spacer(Modifier.height(16.dp))
            Text("MUSCLE GROUP", style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MuscleGroup.entries.forEach { g ->
                    Chip(g.label, group == g) {
                        group = g
                        muscles = emptySet() // stale picks from the old group would be wrong
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("TARGETS", style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                group.muscles.forEach { m ->
                    Chip(m.label, m in muscles) {
                        muscles = if (m in muscles) muscles - m else muscles + m
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("EQUIPMENT", style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Equipment.entries.forEach { e ->
                    Chip(e.label, equipment == e) { equipment = e }
                }
            }

            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                "Add to library",
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, group, muscles.toList(), equipment)
                        name = ""
                        muscles = emptySet()
                        open = false
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        QuietButton(if (open) "Cancel" else "New exercise", onClick = { open = !open })
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ExerciseList(
    state: UiState,
    onPick: (Exercise) -> Unit,
    onResetDay: () -> Unit,
    onAdd: (String, MuscleGroup, List<Muscle>, Equipment) -> Unit,
) {
    var group by remember { mutableStateOf<MuscleGroup?>(null) }
    val session = state.todaySession

    val list = when (group) {
        null -> session?.exercises ?: ExerciseLibrary.all
        else -> ExerciseLibrary.byGroup(group!!)
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Og.Canvas),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 132.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Train", style = MaterialTheme.typography.displayMedium, color = Og.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                if (group == null) {
                    session?.let { "${it.name} · ${it.focus}" } ?: "Rest day — browse by muscle group"
                } else {
                    "${list.size} exercises"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Og.InkSecondary,
            )
        }

        // Logging into a past day is easy to forget you asked for, so it is never silent.
        if (!state.loggingForToday) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Og.Lime)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Logging to ${
                            LocalDate.ofEpochDay(state.selectedDay)
                                .format(DateTimeFormatter.ofPattern("EEE d MMM"))
                        }",
                        style = MaterialTheme.typography.titleMedium,
                        color = Og.Forest,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Back to today",
                        style = MaterialTheme.typography.labelLarge,
                        color = Og.Forest,
                        modifier = Modifier.clickable { onResetDay() },
                    )
                }
            }
        }

        item {
            // Wraps rather than scrolls horizontally: with seven filters, Legs and Core
            // fell off the right edge and the exercises looked like they did not exist.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Not "Today" — it follows the selected day, which may be in the past.
                Chip("Suggested", group == null) { group = null }
                MuscleGroup.entries.forEach { g ->
                    Chip(
                        "${g.label}  ${ExerciseLibrary.byGroup(g).size}",
                        group == g,
                    ) { group = g }
                }
            }
        }

        if (list.isEmpty()) {
            item { EmptyHint("Nothing scheduled today. Pick a muscle group to train anyway.") }
        }

        item { AddExerciseCard(group, onAdd) }

        items(list, key = { it.id }) { exercise ->
            val setsToday = state.sets.filter { it.day == state.today && it.exerciseId == exercise.id }
            val trained = setsToday.isNotEmpty()
            OgCard(onClick = { onPick(exercise) }, padding = 12.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = exercise.equipment.icon,
                        container = if (trained) Og.Lime else Og.Inset,
                        tint = if (trained) Og.Forest else Og.InkSecondary,
                        size = 46.dp,
                    )
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = Og.Ink)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            exercise.primary.joinToString(", ") { it.label },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Og.InkMuted,
                        )
                    }
                    if (trained) {
                        StatusBadge(Status.GOOD, "${setsToday.size} sets")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetail(
    exercise: Exercise,
    state: UiState,
    vm: com.og.ui.OgViewModel,
    onBack: () -> Unit,
) {
    val setsToday = state.sets.filter { it.day == state.today && it.exerciseId == exercise.id }
    val allSets = state.sets.filter { it.exerciseId == exercise.id }
    val lastDay = allSets.map { it.day }.filter { it != state.today }.maxOrNull()
    val lastSets = allSets.filter { it.day == lastDay }

    val bestLast = lastSets.maxOfOrNull { if (it.weightKg > 0) it.weightKg * it.reps else it.reps.toDouble() } ?: 0.0
    val bestToday = setsToday.maxOfOrNull { if (it.weightKg > 0) it.weightKg * it.reps else it.reps.toDouble() } ?: 0.0

    // Today's last set first, so logging set 2 doesn't mean retyping the weight.
    val suggested = setsToday.lastOrNull()?.weightKg
        ?: lastSets.maxByOrNull { it.weightKg }?.weightKg
        ?: exercise.seedKg
        ?: 0.0
    var weight by remember(exercise.id) { mutableStateOf(if (suggested > 0) trimNum(suggested) else "") }
    var reps by remember(exercise.id) { mutableStateOf("8") }
    var view by remember { mutableStateOf(preferredView(exercise)) }

    val heat = remember(exercise.id) {
        buildMap {
            exercise.secondary.forEach { put(it, 0.45f) }
            exercise.primary.forEach { put(it, 1f) }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Og.Canvas),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Og.Ink)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge, color = Og.Ink)
                    Text(
                        "${exercise.group.label} · ${exercise.equipment.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Og.InkMuted,
                    )
                }
            }
        }

        // ---- what it actually trains ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Targets", trailing = view.label)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(96.dp).clickable { view = if (view == BodyView.FRONT) BodyView.BACK else BodyView.FRONT }) {
                        BodyDiagram(heat, view, Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("PRIMARY", style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
                        Spacer(Modifier.height(6.dp))
                        FlowChips(exercise.primary, Og.Accent)
                        if (exercise.secondary.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("ALSO WORKS", style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
                            Spacer(Modifier.height(6.dp))
                            FlowChips(exercise.secondary, Og.InkMuted)
                        }
                    }
                }
                if (exercise.note != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(exercise.note, style = MaterialTheme.typography.bodyMedium, color = Og.InkSecondary)
                }
            }
        }

        // ---- log a set ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle(
                    "Log a set",
                    trailing = lastDay?.let {
                        "last ${LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMM"))}"
                    },
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OgField("Weight", weight, { weight = it }, Modifier.weight(1f), "kg")
                    OgField("Reps", reps, { reps = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Add set", onClick = {
                    val w = weight.toDoubleOrNull() ?: 0.0
                    val r = reps.toIntOrNull() ?: 0
                    if (r > 0) vm.addSet(exercise.id, w, r)
                })

                if (lastSets.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    val delta = bestToday - bestLast
                    when {
                        bestToday == 0.0 -> Text(
                            "Last session's best: ${formatBest(lastSets)}. Beat it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Og.InkMuted,
                        )
                        delta > 0 -> StatusBadge(
                            Status.GOOD,
                            "Up ${((delta / bestLast) * 100).roundToInt()}% on last session",
                        )
                        delta == 0.0 -> StatusBadge(Status.NEUTRAL, "Matched last session")
                        else -> StatusBadge(
                            Status.WARNING,
                            "Down ${((-delta / bestLast) * 100).roundToInt()}% — add a set to catch up",
                        )
                    }
                }
            }
        }

        if (setsToday.isNotEmpty()) {
            item {
                OgCard(padding = 18.dp) {
                    SectionTitle("Today", trailing = "${setsToday.size} sets")
                    Spacer(Modifier.height(8.dp))
                    setsToday.forEachIndexed { i, s ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${i + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Og.InkMuted,
                                modifier = Modifier.width(22.dp),
                            )
                            Text(
                                setLabel(s),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Og.Ink,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { vm.deleteSet(s.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Close, "Delete set", tint = Og.InkMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ---- history ----
        item {
            val byDay = allSets.filter { it.day != state.today }.groupBy { it.day }.toSortedMap(reverseOrder())
            OgCard(padding = 18.dp) {
                SectionTitle("History")
                Spacer(Modifier.height(8.dp))
                if (byDay.isEmpty()) {
                    EmptyHint("No previous sessions for this lift yet.")
                } else {
                    byDay.entries.take(8).forEach { (day, sets) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM")),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Og.InkMuted,
                                modifier = Modifier.width(62.dp),
                            )
                            Text(
                                sets.joinToString("   ") { setLabel(it) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Og.InkSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowChips(muscles: List<Muscle>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        muscles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { m ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                    ) {
                        Text(m.label, style = MaterialTheme.typography.labelMedium, color = color)
                    }
                }
            }
        }
    }
}

private fun preferredView(exercise: Exercise): BodyView {
    val backMuscles = setOf(
        Muscle.LATS, Muscle.TRAPS_MID, Muscle.LOWER_BACK, Muscle.REAR_DELT,
        Muscle.TRICEPS, Muscle.GLUTES, Muscle.HAMSTRINGS,
    )
    return if (exercise.primary.count { it in backMuscles } > 0) BodyView.BACK else BodyView.FRONT
}

private fun setLabel(s: SetLog): String =
    if (s.weightKg > 0) "${trimNum(s.weightKg)} kg × ${s.reps}" else "${s.reps} reps"

private fun formatBest(sets: List<SetLog>): String =
    sets.maxByOrNull { if (it.weightKg > 0) it.weightKg * it.reps else it.reps.toDouble() }
        ?.let { setLabel(it) } ?: "—"

internal fun trimNum(v: Double): String =
    if (v == v.roundToInt().toDouble()) "${v.roundToInt()}" else String.format("%.1f", v)
