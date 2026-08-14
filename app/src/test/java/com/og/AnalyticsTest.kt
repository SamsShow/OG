package com.og

import com.og.data.DayLog
import com.og.data.ExtraIntake
import com.og.data.MuscleGroup
import com.og.data.MealLog
import com.og.data.MealPlan
import com.og.data.Measurement
import com.og.data.Profile
import com.og.data.SetLog
import com.og.domain.Analytics
import com.og.domain.Confidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Covers the parts that would fail silently: the least-squares fit, the refusal
 * paths in the projection, score-weight redistribution, and the creatine hold-out.
 */
class AnalyticsTest {

    private fun blank(day: Long) = Measurement(day, null, null, null, null, null, null, null, null, null)

    // ---------------------------------------------------------------- fit

    @Test
    fun `fit recovers a known line`() {
        val f = Analytics.fit(listOf(0L to 10.0, 7L to 12.0, 14L to 14.0))
        assertNotNull(f)
        assertEquals(2.0, f!!.slopePerWeek, 1e-9)
        assertEquals(10.0, f.intercept, 1e-9)
        assertEquals(1.0, f.r2, 1e-9)
        assertEquals(3, f.n)
        assertEquals(14L, f.spanDays)
    }

    @Test
    fun `fit evaluates on the absolute epoch-day axis`() {
        // Non-zero start: intercept must still be the value at day 0, not at the first point.
        val f = Analytics.fit(listOf(100L to 50.0, 110L to 55.0, 120L to 60.0))!!
        assertEquals(0.5, f.slopePerDay, 1e-9)
        assertEquals(50.0, f.slopePerDay * 100 + f.intercept, 1e-9)
        assertEquals(60.0, f.slopePerDay * 120 + f.intercept, 1e-9)
    }

    @Test
    fun `fit refuses too few points or too short a span`() {
        assertNull(Analytics.fit(listOf(0L to 1.0, 30L to 2.0)))
        assertNull(Analytics.fit(listOf(0L to 1.0, 6L to 2.0, 13L to 3.0)))
    }

    // ---------------------------------------------------------------- projection

    @Test
    fun `projection estimates weeks from the user's own slope`() {
        val p = Analytics.project("Waist", " cm", listOf(0L to 90.0, 14L to 88.0, 28L to 86.0), goal = 85.0)
        assertEquals(1.0, p.weeks!!, 1e-9)
        assertEquals(86.0, p.current, 1e-9)
    }

    @Test
    fun `projection refuses a number when trending away from the goal`() {
        val p = Analytics.project("Waist", " cm", listOf(0L to 86.0, 14L to 88.0, 28L to 90.0), goal = 85.0)
        assertNull(p.weeks)
        assertTrue(p.message.contains("away", ignoreCase = true))
    }

    @Test
    fun `projection refuses a number on a flat trend`() {
        val p = Analytics.project("Waist", " cm", listOf(0L to 88.0, 14L to 88.0, 28L to 88.0), goal = 85.0)
        assertNull(p.weeks)
    }

    @Test
    fun `projection refuses a number without enough history`() {
        val p = Analytics.project("Waist", " cm", listOf(0L to 90.0, 5L to 89.0), goal = 85.0)
        assertNull(p.weeks)
        assertEquals(Confidence.NONE, p.confidence)
    }

    // ---------------------------------------------------------------- creatine hold-out

    @Test
    fun `creatine window is held out of the weight series`() {
        val profile = Profile(creatineStartDay = 100L)
        val measurements = listOf(99L, 100L, 120L, 128L, 130L).map { blank(it).copy(weightKg = 69.0) }
        val clean = Analytics.cleanWeightSeries(measurements, profile).map { it.first }
        assertEquals(listOf(99L, 128L, 130L), clean)
        assertTrue(Analytics.isCreatineConfounded(100L, profile))
        assertTrue(Analytics.isCreatineConfounded(127L, profile))
        assertTrue(!Analytics.isCreatineConfounded(128L, profile))
    }

    @Test
    fun `no creatine date means nothing is held out`() {
        val measurements = listOf(1L, 2L, 3L).map { blank(it).copy(weightKg = 69.0) }
        assertEquals(3, Analytics.cleanWeightSeries(measurements, Profile()).size)
    }

    // ---------------------------------------------------------------- nutrition

    @Test
    fun `protein scales with the portion actually eaten and ignores unticked meals`() {
        val meals = listOf(
            MealLog(day = 5, mealId = MealPlan.night.id, servings = 0.5, completed = true),
            MealLog(day = 5, mealId = MealPlan.morning.id, servings = 1.0, completed = false),
        )
        assertEquals(MealPlan.night.proteinG * 0.5, Analytics.proteinOn(5, meals), 1e-9)
    }

    private fun extra(day: Long, p: Double, k: Double) =
        ExtraIntake(day = day, label = "snack", proteinG = p, kcal = k, createdAt = 0)

    @Test
    fun `ad-hoc intake adds to the day's protein and calories`() {
        val meals = listOf(MealLog(day = 5, mealId = MealPlan.morning.id, servings = 1.0, completed = true))
        val extras = listOf(extra(5, 20.0, 200.0), extra(5, 5.0, 90.0))

        assertEquals(MealPlan.morning.proteinG + 25.0, Analytics.proteinOn(5, meals, extras), 1e-9)
        assertEquals(MealPlan.morning.kcal + 290.0, Analytics.kcalOn(5, meals, extras), 1e-9)
    }

    @Test
    fun `ad-hoc intake from another day is not counted`() {
        val extras = listOf(extra(4, 40.0, 400.0), extra(6, 40.0, 400.0))
        assertEquals(0.0, Analytics.proteinOn(5, emptyList(), extras), 1e-9)
    }

    @Test
    fun `enough ad-hoc protein calls off the top-up suggestion`() {
        val profile = Profile(proteinMin = 100)
        // Every scheduled meal skipped: without extras the day cannot reach the floor.
        val skipped = MealPlan.scheduled.map {
            MealLog(day = 5, mealId = it.id, servings = 0.0, completed = true)
        }
        assertTrue(Analytics.shouldSuggestTopUp(5, skipped, profile))

        // A big meal out covers it, so the app should stop pushing a second scoop.
        val bigMealOut = listOf(extra(5, 120.0, 900.0))
        assertTrue(!Analytics.shouldSuggestTopUp(5, skipped, profile, bigMealOut))
    }

    @Test
    fun `the plan as written lands inside the target band`() {
        assertTrue(
            "plan delivers ${MealPlan.plannedProteinG} g",
            MealPlan.plannedProteinG in 100.0..140.0,
        )
    }

    @Test
    fun `top-up is suggested only when the remaining meals cannot close the gap`() {
        val profile = Profile(proteinMin = 100)
        // Nothing eaten yet, but the whole plan is still ahead — no top-up needed.
        assertTrue(!Analytics.shouldSuggestTopUp(5, emptyList(), profile))

        // Night meal skipped and the rest eaten: the day can no longer reach 100 g.
        val short = MealPlan.scheduled.filter { it.id != MealPlan.night.id }
            .map { MealLog(day = 5, mealId = it.id, servings = 1.0, completed = true) } +
            MealLog(day = 5, mealId = MealPlan.night.id, servings = 0.0, completed = true)
        assertTrue(Analytics.shouldSuggestTopUp(5, short, profile))
    }

    // ---------------------------------------------------------------- calendar

    private fun set(day: Long, exerciseId: String) =
        SetLog(exerciseId = exerciseId, day = day, weightKg = 40.0, reps = 8, createdAt = 0)

    @Test
    fun `a day's groups merge logged sets with hand tags`() {
        // lat_pulldown is a Back lift; the user also says they trained core that day.
        val sets = listOf(set(10, "lat_pulldown"))
        val tags = listOf(DayLog(day = 10, groups = "CORE", note = ""))

        val groups = Analytics.groupsOn(10, sets, tags)
        assertTrue("expected back from the logged set", MuscleGroup.BACK in groups)
        assertTrue("expected core from the hand tag", MuscleGroup.CORE in groups)
    }

    @Test
    fun `a hand-tagged day with no sets still counts as trained`() {
        val tags = listOf(DayLog(day = 10, groups = "LEGS,CORE", note = ""))
        val calendar = Analytics.trainingCalendar(emptyList(), tags)

        assertEquals(setOf(MuscleGroup.LEGS, MuscleGroup.CORE), calendar[10])
    }

    @Test
    fun `an emptied tag drops the day off the calendar`() {
        val tags = listOf(DayLog(day = 10, groups = "", note = ""))
        assertTrue(Analytics.trainingCalendar(emptyList(), tags).isEmpty())
    }

    @Test
    fun `hand-tagged days count toward muscle coverage`() {
        val tags = listOf(
            DayLog(day = 10, groups = "LEGS", note = ""),
            DayLog(day = 11, groups = "LEGS", note = ""),
        )
        val coverage = Analytics.groupSessions(emptyList(), fromDay = 0, dayLogs = tags)
        assertEquals(2, coverage[MuscleGroup.LEGS])
        assertEquals(0, coverage[MuscleGroup.BACK])
    }

    @Test
    fun `the same day tagged and logged is not counted twice`() {
        val sets = listOf(set(10, "lat_pulldown"), set(10, "barbell_row"))
        val tags = listOf(DayLog(day = 10, groups = "BACK", note = ""))
        val coverage = Analytics.groupSessions(sets, fromDay = 0, dayLogs = tags)
        assertEquals(1, coverage[MuscleGroup.BACK])
    }

    // ---------------------------------------------------------------- streak

    @Test
    fun `streak counts trained days and treats sunday as kept`() {
        var d = LocalDate.of(2026, 8, 1)
        while (d.dayOfWeek != DayOfWeek.SATURDAY) d = d.plusDays(1)
        val saturday = d.toEpochDay()
        val monday = d.minusDays(5).toEpochDay()
        val trained = (0..5).map { saturday - it }.toSet()

        assertEquals(6, Analytics.streak(trained, saturday, startedOn = monday))
    }

    @Test
    fun `a missed today does not break a streak still in progress`() {
        var d = LocalDate.of(2026, 8, 1)
        while (d.dayOfWeek != DayOfWeek.SATURDAY) d = d.plusDays(1)
        val saturday = d.toEpochDay()
        val monday = d.minusDays(5).toEpochDay()
        val trainedThroughFriday = (1..5).map { saturday - it }.toSet()

        assertEquals(5, Analytics.streak(trainedThroughFriday, saturday, startedOn = monday))
    }

    // ---------------------------------------------------------------- score

    @Test
    fun `score is zero and fully pending with no data at all`() {
        val today = LocalDate.of(2026, 8, 14).toEpochDay()
        val s = Analytics.score(today, Profile(startedOnDay = today), emptyList(), emptyList(), emptyList())
        assertEquals(0, s.total)
        assertTrue(s.graded.isEmpty())
        assertEquals(6, s.pending.size)
    }

    @Test
    fun `pending components are excluded from the denominator, not scored as zero`() {
        val today = LocalDate.of(2026, 8, 14).toEpochDay()
        val profile = Profile(startedOnDay = today, weeklyWorkoutTarget = 2)

        // One exercise per muscle group, trained on two days, heavier on the later day.
        val ids = listOf(
            "lat_pulldown", "incline_db_press", "lateral_raise",
            "barbell_curl", "leg_press", "cable_crunch",
        )
        val sets = ids.flatMap { id ->
            listOf(
                SetLog(exerciseId = id, day = today - 2, weightKg = 40.0, reps = 8, createdAt = 1),
                SetLog(exerciseId = id, day = today - 1, weightKg = 45.0, reps = 8, createdAt = 2),
            )
        }

        val s = Analytics.score(today, profile, sets, emptyList(), emptyList())

        // Protein, diet and physique cannot be graded on day one; the three that can
        // are all perfect, so the score is 100 rather than 55.
        assertEquals(listOf("Protein", "Diet Adherence", "Physique Trend"), s.pending.map { it.label })
        assertEquals(100, s.total)
    }

    @Test
    fun `coverage flags a group that was not trained twice`() {
        val today = LocalDate.of(2026, 8, 14).toEpochDay()
        val sets = listOf(
            SetLog(exerciseId = "lat_pulldown", day = today - 1, weightKg = 45.0, reps = 8, createdAt = 1),
        )
        val neglected = Analytics.neglectedGroups(sets, today).map { it.label }
        assertTrue("legs should be flagged", "Legs" in neglected)
        assertTrue("back was trained once, still short of two", "Back" in neglected)
    }
}
