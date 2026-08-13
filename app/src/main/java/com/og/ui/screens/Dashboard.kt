package com.og.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.og.data.MealPlan
import com.og.domain.Analytics
import com.og.ui.BodyDiagram
import com.og.ui.BodyView
import com.og.ui.ForestCard
import com.og.ui.HeatLegend
import com.og.ui.IconBadge
import com.og.ui.LinearMeter
import com.og.ui.OgCard
import com.og.ui.RingMeter
import com.og.ui.SectionTitle
import com.og.ui.Status
import com.og.ui.StatusBadge
import com.og.ui.UiState
import com.og.ui.theme.Motion
import com.og.ui.theme.Og
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    state: UiState,
    onOpenTrain: () -> Unit,
    onOpenFuel: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val score = state.score ?: return
    val date = LocalDate.ofEpochDay(state.today)
    val graded = score.graded.isNotEmpty()

    // Counters climb rather than snap, so a change is something you notice happening.
    val shownScore by animateIntAsState(
        if (graded) score.total else 0, Motion.counter(), label = "score",
    )
    val scoreFill by animateFloatAsState(
        if (graded) score.total / 100f else 0f, Motion.spring(), label = "scoreFill",
    )
    val shownProtein by animateIntAsState(
        state.proteinToday.roundToInt(), Motion.counter(), label = "protein",
    )

    LazyColumn(
        Modifier.fillMaxSize().background(Og.Canvas),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEEE")).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Og.InkMuted,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        date.format(DateTimeFormatter.ofPattern("d MMMM")),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Og.Ink,
                    )
                }
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Og.Lime)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Filled.LocalFireDepartment, null,
                        tint = Og.Forest, modifier = Modifier.size(15.dp),
                    )
                    Text(
                        "${state.streak} day streak",
                        style = MaterialTheme.typography.labelLarge,
                        color = Og.Forest,
                    )
                }
            }
        }

        // ---- hero: the one number the screen leads with ----
        item {
            ForestCard(onClick = onOpenStats, padding = 22.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("OG SCORE", style = MaterialTheme.typography.labelSmall, color = Og.OnForestMuted)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (graded) "$shownScore" else "—",
                            style = MaterialTheme.typography.displayLarge,
                            color = if (graded) Og.Lime else Og.OnForestMuted,
                        )
                    }
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Og.ForestSoft)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            if (graded) score.label.uppercase() else "NOT YET GRADED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Og.OnForest,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Og.ForestSoft),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(scoreFill)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Og.Lime),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (!graded) {
                        "Log a workout or tick off a meal and the score starts grading itself."
                    } else {
                        "Graded on ${score.graded.sumOf { it.weight }} of 100 points — " +
                            "${score.pending.joinToString(", ") { it.label.lowercase() }} still need data."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.OnForestMuted,
                )
            }
        }

        // ---- today's numbers, as icon rows ----
        item {
            OgCard(padding = 8.dp) {
                ActivityRow(
                    Icons.Filled.EggAlt, Og.Lime, Og.Forest,
                    "Protein", "of ${state.proteinTarget} g",
                    "$shownProtein", onOpenFuel,
                )
                ActivityRow(
                    Icons.Filled.Bolt, Og.Forest, Og.Lime,
                    "Sessions", "of ${state.profile?.weeklyWorkoutTarget ?: 5} this week",
                    "${state.sets.filter { it.day > state.today - 7 }.map { it.day }.distinct().size}",
                    onOpenTrain,
                )
                ActivityRow(
                    Icons.Filled.MonitorWeight, Og.AccentSoft, Og.Accent,
                    "Weight", "kilograms",
                    state.measurements.maxByOrNull { it.day }?.weightKg?.let { "%.1f".format(it) } ?: "—",
                    onOpenStats,
                )
            }
        }

        // ---- today's workout ----
        item {
            val session = state.todaySession
            OgCard(onClick = onOpenTrain, padding = 20.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle(if (session == null) "Today · rest" else "Today's workout")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            session?.name ?: "Rest day",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Og.Ink,
                        )
                        Text(
                            session?.focus ?: "Recover. The growth happens now.",
                            style = MaterialTheme.typography.titleMedium,
                            color = Og.Accent,
                        )
                    }
                    if (state.trainedToday) {
                        StatusBadge(Status.GOOD, "Done")
                    } else if (session != null) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Og.Lime),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward, null,
                                tint = Og.Forest, modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                if (session != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(session.why, style = MaterialTheme.typography.bodyMedium, color = Og.InkMuted)
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        session.exercises.forEach { ex ->
                            val done = state.sets.any { it.day == state.today && it.exerciseId == ex.id }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (done) Og.Accent else Og.Hairline),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    ex.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (done) Og.Ink else Og.InkSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- protein meter ----
        item {
            OgCard(onClick = onOpenFuel, padding = 20.dp) {
                SectionTitle("Fuel", trailing = "${state.kcalToday.roundToInt()} kcal")
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingMeter(
                        progress = (state.proteinToday / state.proteinTarget).toFloat(),
                        modifier = Modifier.size(112.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$shownProtein",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Og.Ink,
                            )
                            Text(
                                "of ${state.proteinTarget} g",
                                style = MaterialTheme.typography.labelSmall,
                                color = Og.InkMuted,
                            )
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        val remaining = state.proteinRemaining
                        Text(
                            if (remaining <= 0) "Target hit" else "${remaining.roundToInt()} g to go",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (remaining <= 0) Og.Accent else Og.Ink,
                        )
                        Spacer(Modifier.height(4.dp))
                        val nextMeal = MealPlan.scheduled.firstOrNull { meal ->
                            state.meals.none { it.day == state.today && it.mealId == meal.id && it.completed }
                        }
                        Text(
                            nextMeal?.let { "Next: ${it.slot} · ${it.time}" } ?: "Every meal ticked off.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Og.InkMuted,
                        )
                        if (state.suggestTopUp) {
                            Spacer(Modifier.height(10.dp))
                            StatusBadge(
                                Status.NEUTRAL,
                                "${state.proteinRemaining.roundToInt()} g short — 2nd scoop covers it",
                            )
                        }
                    }
                }
            }
        }

        // ---- muscles worked this week ----
        item {
            OgCard(padding = 20.dp) {
                SectionTitle("Muscles this week")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyDiagram(state.heat, BodyView.FRONT, Modifier.weight(1f))
                    BodyDiagram(state.heat, BodyView.BACK, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                HeatLegend()
                val neglected = Analytics.neglectedGoalMuscles(state.sets, state.today)
                if (neglected.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    StatusBadge(
                        Status.NEUTRAL,
                        "Untouched: ${neglected.joinToString(", ") { it.label }}",
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "These are the muscles your V-taper goal depends on most.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Og.InkMuted,
                    )
                }
            }
        }

        // ---- score breakdown ----
        item {
            OgCard(onClick = onOpenStats, padding = 20.dp) {
                SectionTitle("What's driving the score")
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    score.components.forEach { c ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(c.label, style = MaterialTheme.typography.titleMedium, color = Og.Ink)
                                Text(
                                    c.value?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (c.value == null) Og.InkMuted else Og.Ink,
                                )
                            }
                            Spacer(Modifier.height(7.dp))
                            LinearMeter(
                                progress = (c.value ?: 0.0).toFloat(),
                                height = 6.dp,
                                color = when {
                                    c.value == null -> Og.Inset
                                    c.value >= 0.7 -> Og.Accent
                                    c.value >= 0.4 -> Og.InkSecondary
                                    else -> Og.Critical
                                },
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(c.detail, style = MaterialTheme.typography.bodyMedium, color = Og.InkMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    icon: ImageVector,
    badge: Color,
    badgeTint: Color,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, container = badge, tint = badgeTint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Og.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Og.InkMuted)
        }
        Text(value, style = MaterialTheme.typography.headlineMedium, color = Og.Ink)
    }
}
