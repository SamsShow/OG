package com.og.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.og.data.ExerciseLibrary
import com.og.data.MuscleGroup
import com.og.ui.Chip
import com.og.ui.EmptyHint
import com.og.ui.OgCard
import com.og.ui.OgViewModel
import com.og.ui.PrimaryButton
import com.og.ui.QuietButton
import com.og.ui.SectionTitle
import com.og.ui.UiState
import com.og.ui.theme.Og
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
private val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM")

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CalendarScreen(state: UiState, vm: OgViewModel, onOpenTrain: () -> Unit) {
    val today = LocalDate.ofEpochDay(state.today)
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val selected = LocalDate.ofEpochDay(state.selectedDay)
    val calendar = state.calendar

    LazyColumn(
        Modifier.fillMaxSize().background(Og.Canvas),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Calendar", style = MaterialTheme.typography.displayMedium, color = Og.Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                "Tap any day to see or set what you trained. Nothing is locked to a weekday.",
                style = MaterialTheme.typography.bodyLarge,
                color = Og.InkSecondary,
            )
        }

        item {
            OgCard(padding = 16.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month", tint = Og.Ink)
                    }
                    Text(
                        month.format(monthFormat),
                        style = MaterialTheme.typography.titleLarge,
                        color = Og.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month", tint = Og.Ink)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                        Text(
                            d,
                            style = MaterialTheme.typography.labelSmall,
                            color = Og.InkMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                MonthGrid(
                    month = month,
                    today = today,
                    selected = selected,
                    calendar = calendar,
                    onPick = { vm.selectDay(it.toEpochDay()) },
                )

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot(Og.AccentSoft, "1 group")
                    LegendDot(Og.HeatWeek, "2 groups")
                    LegendDot(Og.HeatRecent, "3+")
                }
            }
        }

        // ---- the selected day ----
        item {
            val day = state.selectedDay
            val groups = state.groupsOn(day)
            val setsThatDay = state.sets.filter { it.day == day }

            OgCard(padding = 18.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (day) {
                                state.today -> "Today"
                                state.today - 1 -> "Yesterday"
                                else -> selected.format(dayFormat)
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = Og.Ink,
                        )
                        Text(
                            if (groups.isEmpty()) "Nothing recorded" else groups.joinToString(" · ") { it.label },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (groups.isEmpty()) Og.InkMuted else Og.Accent,
                        )
                    }
                    if (day != state.today) {
                        QuietButton("Today", onClick = { vm.resetToToday() }, modifier = Modifier.width(96.dp))
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionTitle("What did you train")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap any group. Mix as many as you like — this is a record of what happened, not a plan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkMuted,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MuscleGroup.entries.forEach { g ->
                        Chip(g.label, g in groups) { vm.toggleDayGroup(day, g) }
                    }
                }

                // Groups that came from logged sets cannot be untagged by hand, so say so.
                val fromSets = setsThatDay
                    .flatMap { ExerciseLibrary[it.exerciseId]?.allMuscles.orEmpty() }
                    .map { it.group }
                    .toSet()
                if (fromSets.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${fromSets.joinToString(", ") { it.label }} " +
                            "${if (fromSets.size == 1) "comes" else "come"} from logged sets and stays on.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Og.InkMuted,
                    )
                }

                Spacer(Modifier.height(20.dp))
                SectionTitle("Sets logged", trailing = if (setsThatDay.isEmpty()) null else "${setsThatDay.size}")
                Spacer(Modifier.height(10.dp))

                if (setsThatDay.isEmpty()) {
                    EmptyHint("No sets logged for this day.")
                } else {
                    setsThatDay.groupBy { it.exerciseId }.forEach { (id, logs) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ExerciseLibrary[id]?.name ?: id,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Og.Ink,
                                )
                                Text(
                                    logs.joinToString("  ") {
                                        if (it.weightKg > 0) "${trimNum(it.weightKg)}×${it.reps}" else "${it.reps}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Og.InkMuted,
                                )
                            }
                            Text(
                                "${logs.sumOf { it.weightKg * it.reps }.roundToInt()} kg",
                                style = MaterialTheme.typography.labelLarge,
                                color = Og.InkSecondary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    if (day == state.today) "Log exercises" else "Log exercises for this day",
                    onClick = { vm.selectDay(day); onOpenTrain() },
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    calendar: Map<Long, Set<MuscleGroup>>,
    onPick: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    // Monday-first, matching the rest of the app's week handling.
    val lead = first.dayOfWeek.value - 1
    val cells = lead + month.lengthOfMonth()
    val rows = (cells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val index = r * 7 + c
                    val dayOfMonth = index - lead + 1
                    if (dayOfMonth < 1 || dayOfMonth > month.lengthOfMonth()) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = month.atDay(dayOfMonth)
                        DayCell(
                            date = date,
                            groups = calendar[date.toEpochDay()].orEmpty(),
                            isToday = date == today,
                            isSelected = date == selected,
                            isFuture = date.isAfter(today),
                            modifier = Modifier.weight(1f),
                            onClick = { onPick(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    groups: Set<MuscleGroup>,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // Intensity rides the validated sequential ramp: more groups, darker step.
    val fill = when {
        isSelected -> Og.Ink
        groups.isEmpty() -> Color.Transparent
        groups.size == 1 -> Og.AccentSoft
        groups.size == 2 -> Og.HeatWeek
        else -> Og.HeatRecent
    }
    val ink = when {
        isSelected -> Color.White
        groups.size >= 3 -> Color.White
        isFuture -> Og.InkMuted
        else -> Og.Ink
    }

    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.5.dp, Og.Ink, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.labelLarge,
            color = ink,
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Og.InkMuted)
    }
}
