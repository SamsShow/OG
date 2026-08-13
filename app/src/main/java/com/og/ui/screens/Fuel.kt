package com.og.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.og.data.Meal
import com.og.data.MealKind
import com.og.data.MealPlan
import com.og.ui.Chip
import com.og.ui.IconBadge
import com.og.ui.OgField
import com.og.ui.PrimaryButton
import com.og.ui.QuietButton
import com.og.ui.LinearMeter
import com.og.ui.OgCard
import com.og.ui.OgViewModel
import com.og.ui.RingMeter
import com.og.ui.SectionTitle
import com.og.ui.Status
import com.og.ui.StatusBadge
import com.og.ui.UiState
import com.og.ui.theme.Motion
import com.og.ui.theme.Og
import kotlin.math.roundToInt

private val servingOptions = listOf(0.5, 0.75, 1.0, 1.25, 1.5)

private val Meal.icon: ImageVector
    get() = when (id) {
        MealPlan.morning.id -> Icons.Filled.WbTwilight
        MealPlan.afternoon.id -> Icons.Filled.LightMode
        MealPlan.evening.id -> Icons.Filled.LocalDrink
        MealPlan.night.id -> Icons.Filled.DinnerDining
        MealPlan.creatine.id -> Icons.Filled.Medication
        else -> Icons.Filled.LocalDrink
    }

@Composable
fun FuelScreen(state: UiState, vm: OgViewModel) {
    val target = state.proteinTarget
    val max = state.profile?.proteinMax ?: 140
    val shownProtein by animateIntAsState(
        state.proteinToday.roundToInt(), Motion.counter(), label = "proteinBig",
    )

    LazyColumn(
        Modifier.fillMaxSize().background(Og.Canvas),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Fuel", style = MaterialTheme.typography.displayMedium, color = Og.Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                "Target $target–$max g protein. The plan delivers ${MealPlan.plannedProteinG.roundToInt()} g at full portions.",
                style = MaterialTheme.typography.bodyLarge,
                color = Og.InkSecondary,
            )
        }

        item {
            OgCard(padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingMeter(
                        progress = (state.proteinToday / target).toFloat(),
                        modifier = Modifier.size(126.dp),
                        stroke = 15.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$shownProtein",
                                style = MaterialTheme.typography.displayMedium,
                                color = Og.Ink,
                            )
                            Text("grams", style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        val remaining = state.proteinRemaining
                        Text(
                            if (remaining <= 0) "Target hit" else "${remaining.roundToInt()} g left",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (remaining <= 0) Og.Accent else Og.Ink,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${state.kcalToday.roundToInt()} kcal today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Og.InkMuted,
                        )
                        Spacer(Modifier.height(10.dp))
                        val done = state.meals.count {
                            it.day == state.today && it.completed &&
                                MealPlan[it.mealId]?.kind != MealKind.ADAPTIVE
                        }
                        Text(
                            "$done of ${MealPlan.completionCount} items ticked",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Og.InkMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearMeter(done.toFloat() / MealPlan.completionCount, height = 5.dp)
                    }
                }
            }
        }

        for (meal in MealPlan.scheduled) {
            item(key = meal.id) { MealCard(meal, state, vm) }
        }

        item { ExtrasCard(state, vm) }

        item {
            val suggested = state.suggestTopUp
            val logged = state.meals.firstOrNull {
                it.day == state.today && it.mealId == MealPlan.wheyTopUp.id
            }
            if (suggested || logged?.completed == true) {
                MealCard(MealPlan.wheyTopUp, state, vm, highlight = suggested)
            } else {
                OgCard(padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = Og.InkMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Second scoop not needed — the day's meals cover your target.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Og.InkMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: Meal,
    state: UiState,
    vm: OgViewModel,
    highlight: Boolean = false,
) {
    val log = state.meals.firstOrNull { it.day == state.today && it.mealId == meal.id }
    val completed = log?.completed == true
    val servings = log?.servings ?: 1.0
    var showSwaps by remember { mutableStateOf(false) }

    OgCard(padding = 16.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // The badge pops when you tick it — the only confirmation the action gets.
            AnimatedContent(
                targetState = completed,
                transitionSpec = {
                    (fadeIn(tween(Motion.FADE_IN)) + scaleIn(Motion.spring(), initialScale = 0.6f))
                        .togetherWith(
                            fadeOut(tween(Motion.FADE_OUT)) +
                                scaleOut(Motion.spring(), targetScale = 0.6f),
                        )
                },
                label = "mealTick",
            ) { done ->
                IconBadge(
                    icon = if (done) Icons.Filled.Check else meal.icon,
                    container = if (done) Og.Accent else Og.Inset,
                    tint = if (done) Color.White else Og.Ink,
                    contentDescription = if (done) "Completed" else "Mark ${meal.slot} eaten",
                    modifier = Modifier.clickable { vm.setMeal(meal.id, servings, !done) },
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.slot, style = MaterialTheme.typography.titleMedium, color = Og.Ink)
                Text(meal.time, style = MaterialTheme.typography.labelMedium, color = Og.InkMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (meal.proteinG > 0) {
                    Text(
                        "${(meal.proteinG * servings).roundToInt()} g",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (completed) Og.Accent else Og.InkSecondary,
                    )
                }
                Text(
                    "${(meal.kcal * servings).roundToInt()} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = Og.InkMuted,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        meal.items.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(item.qty, style = MaterialTheme.typography.bodyMedium, color = Og.InkMuted)
            }
        }

        if (highlight) {
            Spacer(Modifier.height(10.dp))
            StatusBadge(Status.WARNING, "${state.proteinRemaining.roundToInt()} g short today")
        }

        if (meal.note != null) {
            Spacer(Modifier.height(10.dp))
            Text(meal.note, style = MaterialTheme.typography.bodyMedium, color = Og.InkMuted)
        }

        // A supplement is taken or it isn't — a portion multiplier makes no sense for it.
        if (meal.kind != MealKind.SUPPLEMENT) {
            Spacer(Modifier.height(12.dp))
            Text("PORTION EATEN", style = MaterialTheme.typography.labelSmall, color = Og.InkMuted)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                servingOptions.forEach { s ->
                    Chip(
                        text = if (s == 1.0) "Full" else "×$s",
                        selected = servings == s,
                        onClick = { vm.setMeal(meal.id, s, completed) },
                    )
                }
            }
        }

        if (meal.swaps.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clickable { showSwaps = !showSwaps },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.SwapHoriz, null, tint = Og.InkMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    if (showSwaps) "Hide swaps" else "Cheaper swaps",
                    style = MaterialTheme.typography.labelMedium,
                    color = Og.InkMuted,
                )
            }
            AnimatedVisibility(showSwaps) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    meal.swaps.forEach {
                        Text("· $it", style = MaterialTheme.typography.bodyMedium, color = Og.InkSecondary)
                    }
                }
            }
        }
    }
}

/**
 * Anything eaten outside the plan. The plan is fixed; eating is not, so without this the
 * protein total quietly under-reports every time you snack or eat out.
 */
@Composable
private fun ExtrasCard(state: UiState, vm: OgViewModel) {
    val todays = state.extras.filter { it.day == state.today }
    var open by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }

    OgCard(padding = 16.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.AddCircleOutline, container = Og.Inset, tint = Og.Ink)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Anything else", style = MaterialTheme.typography.titleMedium, color = Og.Ink)
                Text(
                    "Snacks, meals out, extra helpings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkMuted,
                )
            }
            if (todays.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${todays.sumOf { it.proteinG }.roundToInt()} g",
                        style = MaterialTheme.typography.titleMedium,
                        color = Og.Accent,
                    )
                    Text(
                        "${todays.sumOf { it.kcal }.roundToInt()} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = Og.InkMuted,
                    )
                }
            }
        }

        todays.forEach { extra ->
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    extra.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Og.InkSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${extra.proteinG.roundToInt()} g · ${extra.kcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkMuted,
                )
                IconButton(onClick = { vm.deleteExtra(extra.id) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Close, "Remove ${extra.label}", tint = Og.InkMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        AnimatedVisibility(open) {
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OgField("What was it", label, { label = it }, numeric = false, placeholder = "Protein bar")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OgField("Protein", protein, { protein = it }, Modifier.weight(1f), "g")
                    OgField("Calories", kcal, { kcal = it }, Modifier.weight(1f), "kcal")
                }
                PrimaryButton("Add to today", onClick = {
                    val p = protein.toDoubleOrNull() ?: 0.0
                    val k = kcal.toDoubleOrNull() ?: 0.0
                    if (p > 0 || k > 0) {
                        vm.addExtra(label, p, k)
                        label = ""; protein = ""; kcal = ""
                        open = false
                    }
                })
            }
        }

        Spacer(Modifier.height(12.dp))
        QuietButton(if (open) "Cancel" else "Log something extra", onClick = { open = !open })
    }
}
