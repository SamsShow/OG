package com.og.domain

import com.og.data.ExerciseLibrary
import com.og.data.ExtraIntake
import com.og.data.MealKind
import com.og.data.MealLog
import com.og.data.MealPlan
import com.og.data.Measurement
import com.og.data.Muscle
import com.og.data.MuscleGroup
import com.og.data.Profile
import com.og.data.SetLog
import com.og.data.TrainingSplit
import com.og.data.VTaperMuscles
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------------------------------------------------------------- score

data class ScoreComponent(
    val label: String,
    val weight: Int,
    /** 0..1, or null when there is not yet enough data to grade it honestly. */
    val value: Double?,
    val detail: String,
)

data class Score(
    val total: Int,
    val components: List<ScoreComponent>,
) {
    val graded = components.filter { it.value != null }
    val pending = components.filter { it.value == null }
    val label: String
        get() = when {
            graded.isEmpty() -> "No data yet"
            total >= 85 -> "Dialled in"
            total >= 70 -> "On track"
            total >= 55 -> "Slipping"
            else -> "Off plan"
        }
}

// ---------------------------------------------------------------- trends

data class Fit(
    val slopePerDay: Double,
    /** Value at epoch day 0 — evaluate the line as slopePerDay * day + intercept. */
    val intercept: Double,
    val r2: Double,
    val n: Int,
    val spanDays: Long,
) {
    val slopePerWeek get() = slopePerDay * 7
}

enum class Confidence { NONE, LOW, MODERATE }

data class Projection(
    val label: String,
    val current: Double,
    val goal: Double,
    val unit: String,
    /** Null whenever a number would be dishonest. [message] always says why. */
    val weeks: Double?,
    val message: String,
    val confidence: Confidence,
    val fit: Fit?,
)

object Analytics {

    const val GOLDEN_RATIO = 1.618
    const val TARGET_BODY_FAT = 12.0

    /** Weeks after starting creatine during which scale weight is water-confounded. */
    const val CREATINE_WATER_WEEKS = 4L

    // ------------------------------------------------------------ daily nutrition

    fun proteinOn(
        day: Long,
        meals: List<MealLog>,
        extras: List<ExtraIntake> = emptyList(),
    ): Double =
        meals.filter { it.day == day && it.completed }
            .sumOf { (MealPlan[it.mealId]?.proteinG ?: 0.0) * it.servings } +
            extras.filter { it.day == day }.sumOf { it.proteinG }

    fun kcalOn(
        day: Long,
        meals: List<MealLog>,
        extras: List<ExtraIntake> = emptyList(),
    ): Double =
        meals.filter { it.day == day && it.completed }
            .sumOf { (MealPlan[it.mealId]?.kcal ?: 0.0) * it.servings } +
            extras.filter { it.day == day }.sumOf { it.kcal }

    fun scheduledCompletedOn(day: Long, meals: List<MealLog>): Int =
        meals.count { it.day == day && it.completed && MealPlan[it.mealId]?.kind != MealKind.ADAPTIVE }

    /**
     * The second whey scoop is only worth suggesting once the day is genuinely short
     * and there is no scheduled meal left that would close the gap on its own.
     */
    fun shouldSuggestTopUp(
        day: Long,
        meals: List<MealLog>,
        profile: Profile,
        extras: List<ExtraIntake> = emptyList(),
    ): Boolean {
        val already = meals.any { it.day == day && it.mealId == MealPlan.wheyTopUp.id && it.completed }
        if (already) return false
        val eaten = proteinOn(day, meals, extras)
        val stillComing = MealPlan.scheduled
            .filter { meal -> meals.none { it.day == day && it.mealId == meal.id && it.completed } }
            .sumOf { it.proteinG }
        return eaten + stillComing < profile.proteinMin
    }

    // ------------------------------------------------------------ training

    private fun musclesOf(set: SetLog): List<Muscle> =
        ExerciseLibrary[set.exerciseId]?.allMuscles ?: emptyList()

    /** Days since each muscle was last trained. Absent key = never trained. */
    fun muscleRecency(sets: List<SetLog>, today: Long): Map<Muscle, Long> {
        val out = mutableMapOf<Muscle, Long>()
        for (set in sets) {
            val age = today - set.day
            for (m in musclesOf(set)) {
                val prev = out[m]
                if (prev == null || age < prev) out[m] = age
            }
        }
        return out
    }

    /** Heat 0..1 for the body diagram: today is hottest, fades out over a week. */
    fun heat(daysAgo: Long?): Float = when {
        daysAgo == null -> 0f
        daysAgo <= 0L -> 1f
        daysAgo <= 1L -> 0.8f
        daysAgo <= 2L -> 0.62f
        daysAgo <= 4L -> 0.42f
        daysAgo <= 6L -> 0.24f
        else -> 0f
    }

    fun groupSessions(sets: List<SetLog>, fromDay: Long): Map<MuscleGroup, Int> {
        val byGroup = mutableMapOf<MuscleGroup, MutableSet<Long>>()
        for (set in sets) {
            if (set.day < fromDay) continue
            for (m in musclesOf(set)) {
                byGroup.getOrPut(m.group) { mutableSetOf() }.add(set.day)
            }
        }
        return MuscleGroup.entries.associateWith { byGroup[it]?.size ?: 0 }
    }

    fun volumeOn(day: Long, sets: List<SetLog>): Double =
        sets.filter { it.day == day }.sumOf { it.weightKg * it.reps }

    /** Weekly training volume, keyed by the epoch day of each week's Monday. */
    fun weeklyVolume(sets: List<SetLog>): List<Pair<Long, Double>> =
        sets.groupBy { weekStart(it.day) }
            .map { (week, s) -> week to s.sumOf { it.weightKg * it.reps } }
            .sortedBy { it.first }

    fun weekStart(day: Long): Long {
        val date = LocalDate.ofEpochDay(day)
        return day - (date.dayOfWeek.value - 1)
    }

    /**
     * Best working set of a session. Bodyweight lifts are logged at 0 kg, so they
     * fall back to reps — otherwise every bodyweight session would score as zero volume.
     */
    private fun bestMetric(sets: List<SetLog>): Double =
        sets.maxOfOrNull { if (it.weightKg > 0) it.weightKg * it.reps else it.reps.toDouble() } ?: 0.0

    data class OverloadRow(
        val exerciseId: String,
        val name: String,
        val latest: Double,
        val previous: Double,
    ) {
        val improved get() = latest > previous
        val deltaPct get() = if (previous > 0) (latest - previous) / previous * 100 else 0.0
    }

    fun overload(sets: List<SetLog>, fromDay: Long): List<OverloadRow> =
        sets.filter { it.day >= fromDay }
            .groupBy { it.exerciseId }
            .mapNotNull { (id, all) ->
                val byDay = all.groupBy { it.day }.toSortedMap()
                if (byDay.size < 2) return@mapNotNull null
                val days = byDay.keys.toList()
                OverloadRow(
                    exerciseId = id,
                    name = ExerciseLibrary[id]?.name ?: id,
                    latest = bestMetric(byDay.getValue(days.last())),
                    previous = bestMetric(byDay.getValue(days[days.size - 2])),
                )
            }
            .sortedByDescending { it.deltaPct }

    /**
     * Consecutive days of following the plan. A scheduled rest day counts as kept,
     * and today never breaks the streak while it is still in progress.
     */
    fun streak(trainedDays: Set<Long>, today: Long, startedOn: Long): Int {
        fun kept(day: Long) = day in trainedDays ||
            TrainingSplit.forDay(LocalDate.ofEpochDay(day).dayOfWeek) == null

        var day = if (kept(today)) today else today - 1
        var count = 0
        while (day >= startedOn && kept(day)) {
            count++
            day--
        }
        return count
    }

    // ------------------------------------------------------------ measurements

    fun vTaper(m: Measurement): Double? {
        val s = m.shouldersCm ?: return null
        val w = m.waistCm ?: return null
        return if (w > 0) s / w else null
    }

    /**
     * Scale weight, with the creatine loading window removed. Creatine pulls water
     * into muscle and adds 1–2 kg that is neither fat nor muscle; leaving those points
     * in makes every downstream trend read a gain that did not happen.
     */
    fun cleanWeightSeries(
        measurements: List<Measurement>,
        profile: Profile,
    ): List<Pair<Long, Double>> {
        val start = profile.creatineStartDay
        return measurements.mapNotNull { m -> m.weightKg?.let { m.day to it } }
            .filterNot { (day, _) ->
                start != null && day >= start && day < start + CREATINE_WATER_WEEKS * 7
            }
    }

    fun isCreatineConfounded(day: Long, profile: Profile): Boolean {
        val start = profile.creatineStartDay ?: return false
        return day >= start && day < start + CREATINE_WATER_WEEKS * 7
    }

    // ------------------------------------------------------------ curve fitting

    /** Ordinary least squares. Refuses to fit fewer than 3 points or under two weeks. */
    fun fit(points: List<Pair<Long, Double>>): Fit? {
        if (points.size < 3) return null
        val sorted = points.sortedBy { it.first }
        val span = sorted.last().first - sorted.first().first
        if (span < 14) return null

        val n = sorted.size
        val x0 = sorted.first().first
        val xs = sorted.map { (it.first - x0).toDouble() }
        val ys = sorted.map { it.second }
        val mx = xs.average()
        val my = ys.average()

        var sxx = 0.0
        var sxy = 0.0
        var syy = 0.0
        for (i in 0 until n) {
            val dx = xs[i] - mx
            val dy = ys[i] - my
            sxx += dx * dx
            sxy += dx * dy
            syy += dy * dy
        }
        if (sxx == 0.0) return null

        val slope = sxy / sxx
        // Shift the intercept back onto the absolute epoch-day axis so callers can
        // evaluate the line directly as slope * day + intercept.
        val intercept = my - slope * mx - slope * x0
        val r2 = if (syy == 0.0) 0.0 else (sxy * sxy) / (sxx * syy)
        return Fit(slope, intercept, r2, n, span)
    }

    private fun confidenceOf(fit: Fit): Confidence = when {
        fit.r2 >= 0.6 && fit.n >= 5 -> Confidence.MODERATE
        fit.r2 >= 0.3 -> Confidence.LOW
        else -> Confidence.NONE
    }

    /**
     * Projects weeks-to-goal from the user's own trend. Every path that cannot produce
     * an honest number returns weeks = null with a message explaining the gap, because
     * a fabricated ETA is worse than none.
     */
    fun project(
        label: String,
        unit: String,
        points: List<Pair<Long, Double>>,
        goal: Double,
    ): Projection {
        val current = points.maxByOrNull { it.first }?.second ?: 0.0
        val f = fit(points)
            ?: return Projection(
                label, current, goal, unit, null,
                "Needs 3+ entries spanning at least 2 weeks before a trend means anything.",
                Confidence.NONE, null,
            )

        val needed = goal - current
        if (abs(needed) < 1e-6) {
            return Projection(label, current, goal, unit, 0.0, "Target reached.", confidenceOf(f), f)
        }
        if (abs(f.slopePerWeek) < 1e-4) {
            return Projection(
                label, current, goal, unit, null,
                "Trend is flat — nothing to project from yet.", Confidence.NONE, f,
            )
        }
        if (needed > 0 != f.slopePerWeek > 0) {
            return Projection(
                label, current, goal, unit, null,
                "Currently moving away from this target.", confidenceOf(f), f,
            )
        }

        val weeks = needed / f.slopePerWeek
        val confidence = confidenceOf(f)
        val message = when (confidence) {
            Confidence.MODERATE -> "Rough projection from your last ${f.n} entries."
            Confidence.LOW -> "Loose estimate — your data is still scattered."
            Confidence.NONE -> "Too scattered to trust. Treat as a guess, not a date."
        }
        return Projection(label, current, goal, unit, weeks, message, confidence, f)
    }

    // ------------------------------------------------------------ the score

    fun score(
        today: Long,
        profile: Profile,
        sets: List<SetLog>,
        meals: List<MealLog>,
        measurements: List<Measurement>,
        extras: List<ExtraIntake> = emptyList(),
    ): Score {
        val components = listOf(
            consistency(today, profile, sets),
            overloadComponent(today, sets),
            proteinComponent(today, profile, meals, extras),
            dietComponent(today, profile, meals),
            coverageComponent(today, sets),
            physiqueComponent(measurements),
        )

        val gradedWeight = components.filter { it.value != null }.sumOf { it.weight }
        val earned = components.sumOf { (it.value ?: 0.0) * it.weight }
        val total = if (gradedWeight == 0) 0 else (earned / gradedWeight * 100).roundToInt()
        return Score(total.coerceIn(0, 100), components)
    }

    private fun consistency(today: Long, profile: Profile, sets: List<SetLog>): ScoreComponent {
        val trained = sets.filter { it.day > today - 7 }.map { it.day }.distinct().size
        val target = profile.weeklyWorkoutTarget
        return if (sets.isEmpty()) {
            ScoreComponent("Consistency", 25, null, "Log your first workout to start grading this.")
        } else {
            ScoreComponent(
                "Consistency", 25,
                (trained.toDouble() / target).coerceAtMost(1.0),
                "$trained of $target sessions in the last 7 days",
            )
        }
    }

    private fun overloadComponent(today: Long, sets: List<SetLog>): ScoreComponent {
        val rows = overload(sets, today - 42)
        if (rows.isEmpty()) {
            return ScoreComponent(
                "Progressive Overload", 20, null,
                "Needs a second session of the same lift to compare against.",
            )
        }
        val improved = rows.count { it.improved }
        return ScoreComponent(
            "Progressive Overload", 20,
            improved.toDouble() / rows.size,
            "$improved of ${rows.size} lifts beat their last session",
        )
    }

    private fun proteinComponent(
        today: Long,
        profile: Profile,
        meals: List<MealLog>,
        extras: List<ExtraIntake>,
    ): ScoreComponent {
        // Today is excluded — it is still in progress and would drag the average down.
        val days = ((today - 7)..(today - 1)).filter { it >= profile.startedOnDay }
        if (days.isEmpty()) {
            return ScoreComponent("Protein", 20, null, "Grades from tomorrow, once a full day is on record.")
        }
        val ratios = days.map { (proteinOn(it, meals, extras) / profile.proteinMin).coerceAtMost(1.0) }
        val avg = ratios.average()
        val avgG = days.map { proteinOn(it, meals, extras) }.average()
        return ScoreComponent(
            "Protein", 20, avg,
            "${avgG.roundToInt()} g/day average against a ${profile.proteinMin} g floor",
        )
    }

    private fun dietComponent(today: Long, profile: Profile, meals: List<MealLog>): ScoreComponent {
        val days = ((today - 7)..(today - 1)).filter { it >= profile.startedOnDay }
        if (days.isEmpty()) {
            return ScoreComponent("Diet Adherence", 15, null, "Grades from tomorrow.")
        }
        val done = days.sumOf { scheduledCompletedOn(it, meals) }
        val planned = days.size * MealPlan.completionCount
        return ScoreComponent(
            "Diet Adherence", 15,
            (done.toDouble() / planned).coerceAtMost(1.0),
            "$done of $planned planned items ticked off",
        )
    }

    private fun coverageComponent(today: Long, sets: List<SetLog>): ScoreComponent {
        val sessions = groupSessions(sets, today - 7)
        if (sessions.values.all { it == 0 }) {
            return ScoreComponent("Muscle Coverage", 10, null, "No training logged this week.")
        }
        val value = sessions.values.map { (it / 2.0).coerceAtMost(1.0) }.average()
        val thin = sessions.filter { it.value < 2 }.keys.map { it.label }
        return ScoreComponent(
            "Muscle Coverage", 10, value,
            if (thin.isEmpty()) "Every group hit at least twice" else "Light on ${thin.joinToString(", ")}",
        )
    }

    private fun physiqueComponent(measurements: List<Measurement>): ScoreComponent {
        val sorted = measurements.sortedBy { it.day }
        if (sorted.size < 2) {
            return ScoreComponent(
                "Physique Trend", 10, null,
                "Add a second set of measurements to grade progress.",
            )
        }
        val first = sorted.first()
        val last = sorted.last()
        val signals = mutableListOf<Double>()
        val notes = mutableListOf<String>()

        val bfStart = first.bodyFatPct
        val bfNow = last.bodyFatPct
        if (bfStart != null && bfNow != null) {
            val room = bfStart - TARGET_BODY_FAT
            val progress = if (room <= 0) 1.0 else ((bfStart - bfNow) / room).coerceIn(0.0, 1.0)
            signals += progress
            notes += "body fat ${"%.1f".format(bfNow)}%"
        }

        val vStart = vTaper(first)
        val vNow = vTaper(last)
        if (vStart != null && vNow != null) {
            val room = GOLDEN_RATIO - vStart
            val progress = if (room <= 0) 1.0 else ((vNow - vStart) / room).coerceIn(0.0, 1.0)
            signals += progress
            notes += "V-taper ${"%.2f".format(vNow)}"
        }

        if (signals.isEmpty()) {
            return ScoreComponent(
                "Physique Trend", 10, null,
                "Log body fat, or shoulders and waist, to grade this.",
            )
        }
        return ScoreComponent("Physique Trend", 10, signals.average(), notes.joinToString(" · "))
    }

    // ------------------------------------------------------------ coaching hints

    /** Muscles central to the stated goals that have gone untrained for a week. */
    fun neglectedGoalMuscles(sets: List<SetLog>, today: Long): List<Muscle> {
        val recency = muscleRecency(sets, today)
        return VTaperMuscles.filter { (recency[it] ?: Long.MAX_VALUE) > 6 }.sortedBy { it.label }
    }

    fun neglectedGroups(sets: List<SetLog>, today: Long): List<MuscleGroup> =
        groupSessions(sets, today - 7).filter { it.value < 2 }.keys.toList()
}
