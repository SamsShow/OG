package com.og.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.og.domain.Analytics
import com.og.ui.ChartPoint
import com.og.ui.EmptyHint
import com.og.ui.GroupBalance
import com.og.ui.OgCard
import com.og.ui.SectionTitle
import com.og.ui.StatTile
import com.og.ui.Status
import com.og.ui.StatusBadge
import com.og.ui.TrendLineChart
import com.og.ui.UiState
import com.og.ui.VolumeBars
import com.og.ui.theme.Og
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dayFmt = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun StatsScreen(state: UiState) {
    val profile = state.profile ?: return
    val score = state.score ?: return

    LazyColumn(
        Modifier.fillMaxSize().background(Og.Canvas),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Stats", style = MaterialTheme.typography.displayMedium, color = Og.Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                "How consistent you've been, and whether it's working.",
                style = MaterialTheme.typography.bodyLarge,
                color = Og.InkSecondary,
            )
        }

        item {
            val avgProtein = ((state.today - 7) until state.today)
                .filter { it >= profile.startedOnDay }
                .map { Analytics.proteinOn(it, state.meals) }
                .let { if (it.isEmpty()) 0.0 else it.average() }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Score",
                    if (score.graded.isEmpty()) "—" else "${score.total}",
                    modifier = Modifier.weight(1f),
                )
                StatTile("Streak", "${state.streak}", unit = "d", modifier = Modifier.weight(1f))
                StatTile(
                    "Protein avg",
                    "${avgProtein.roundToInt()}",
                    unit = "g",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ---- weekly volume ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Weekly training volume", trailing = "kg lifted")
                Spacer(Modifier.height(12.dp))
                val weeks = Analytics.weeklyVolume(state.sets).takeLast(8)
                VolumeBars(
                    weeks.map { (weekStart, volume) ->
                        LocalDate.ofEpochDay(weekStart).format(DateTimeFormatter.ofPattern("d/M")) to volume
                    },
                )
                if (weeks.size >= 2) {
                    Spacer(Modifier.height(10.dp))
                    val prev = weeks[weeks.size - 2].second
                    val now = weeks.last().second
                    val pct = if (prev > 0) ((now - prev) / prev * 100).roundToInt() else 0
                    StatusBadge(
                        if (pct >= 0) Status.GOOD else Status.WARNING,
                        if (pct >= 0) "Up $pct% on last week" else "Down ${-pct}% on last week",
                    )
                }
            }
        }

        // ---- coverage ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Muscle coverage", trailing = "last 7 days")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Two sessions a week per group is the bar. Anything short is where progress leaks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkMuted,
                )
                Spacer(Modifier.height(14.dp))
                GroupBalance(state.groupSessions, target = 2)
            }
        }

        // ---- progressive overload ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Progressive overload", trailing = "vs previous session")
                Spacer(Modifier.height(12.dp))
                val rows = Analytics.overload(state.sets, state.today - 42)
                if (rows.isEmpty()) {
                    EmptyHint("Train the same lift twice and the comparison appears here.")
                } else {
                    rows.take(10).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                row.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Og.InkSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            StatusBadge(
                                if (row.improved) Status.GOOD else Status.WARNING,
                                (if (row.deltaPct >= 0) "+" else "") + "${row.deltaPct.roundToInt()}%",
                            )
                        }
                    }
                }
            }
        }

        // ---- body weight ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Body weight", trailing = "kg")
                Spacer(Modifier.height(12.dp))
                val all = state.measurements.mapNotNull { m -> m.weightKg?.let { m.day to it } }
                val clean = Analytics.cleanWeightSeries(state.measurements, profile)
                TrendLineChart(
                    points = all.map { ChartPoint(it.first, it.second, label(it.first)) },
                    unit = " kg",
                    fit = Analytics.fit(clean),
                    confounded = { Analytics.isCreatineConfounded(it, profile) },
                )
                if (all.size > clean.size) {
                    Spacer(Modifier.height(10.dp))
                    StatusBadge(
                        Status.NEUTRAL,
                        "${all.size - clean.size} readings held out of the trend",
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hollow points fall inside the creatine water-weight window. They are still shown, but the trend line ignores them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Og.InkMuted,
                    )
                }
            }
        }

        // ---- waist ----
        item {
            val waist = state.measurements.mapNotNull { m -> m.waistCm?.let { m.day to it } }
            if (waist.size >= 2) {
                OgCard(padding = 18.dp) {
                    SectionTitle("Waist", trailing = "cm")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The most honest fat-loss signal you have — it does not move with water the way the scale does.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Og.InkMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    TrendLineChart(
                        points = waist.map { ChartPoint(it.first, it.second, label(it.first)) },
                        unit = " cm",
                        fit = Analytics.fit(waist),
                    )
                }
            }
        }

        // ---- v-taper ----
        item {
            val v = state.measurements.mapNotNull { m -> Analytics.vTaper(m)?.let { m.day to it } }
            if (v.size >= 2) {
                OgCard(padding = 18.dp) {
                    SectionTitle("V-taper ratio", trailing = "shoulders ÷ waist")
                    Spacer(Modifier.height(12.dp))
                    TrendLineChart(
                        points = v.map { ChartPoint(it.first, it.second, label(it.first)) },
                        unit = "",
                        fit = Analytics.fit(v),
                        goal = Analytics.GOLDEN_RATIO,
                    )
                }
            }
        }

        // ---- protein ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Protein", trailing = "last 14 days")
                Spacer(Modifier.height(12.dp))
                val days = ((state.today - 13)..state.today).filter { it >= profile.startedOnDay }
                VolumeBars(
                    days.map { day ->
                        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d")) to
                            Analytics.proteinOn(day, state.meals)
                    },
                    unit = "g",
                )
                Spacer(Modifier.height(10.dp))
                val hit = days.count { Analytics.proteinOn(it, state.meals) >= profile.proteinMin }
                StatusBadge(
                    if (hit >= days.size * 0.7) Status.GOOD else Status.WARNING,
                    "$hit of ${days.size} days at or above ${profile.proteinMin} g",
                )
            }
        }

        // ---- score detail ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Score components")
                Spacer(Modifier.height(12.dp))
                score.components.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.label, style = MaterialTheme.typography.bodyLarge, color = Og.Ink)
                            Text(c.detail, style = MaterialTheme.typography.bodyMedium, color = Og.InkMuted)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            c.value?.let { "${(it * c.weight).roundToInt()}/${c.weight}" } ?: "—/${c.weight}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (c.value == null) Og.InkMuted else Og.Ink,
                        )
                    }
                }
            }
        }
    }
}

private fun label(day: Long): String = LocalDate.ofEpochDay(day).format(dayFmt)
