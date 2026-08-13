package com.og.data

import java.time.DayOfWeek

data class Session(
    val name: String,
    val focus: String,
    val why: String,
    val exerciseIds: List<String>,
) {
    val exercises: List<Exercise> get() = exerciseIds.mapNotNull { ExerciseLibrary[it] }
}

/**
 * A five-day split weighted toward back width and upper chest, the two things
 * that actually produce a V-taper. Legs and core get one focused day each.
 */
object TrainingSplit {

    private val pullA = Session(
        name = "Pull A", focus = "Back Width",
        why = "Widest-grip pulling first, while you are fresh. This is the day that moves the taper.",
        exerciseIds = listOf(
            "lat_pulldown", "pull_up", "chest_supported_row",
            "straight_arm_pulldown", "face_pull", "hammer_curl",
        ),
    )

    private val pushA = Session(
        name = "Push A", focus = "Upper Chest",
        why = "Incline before flat. Your upper chest is the gap, so it gets the freshest effort.",
        exerciseIds = listOf(
            "incline_db_press", "cable_fly_low_to_high", "db_shoulder_press",
            "lateral_raise", "tricep_pushdown",
        ),
    )

    private val legs = Session(
        name = "Legs", focus = "Quads, Hamstrings & Core",
        why = "One hard leg day keeps the physique balanced and burns more than any ab circuit will.",
        exerciseIds = listOf(
            "leg_press", "romanian_deadlift", "leg_extension",
            "seated_leg_curl", "calf_raise", "hanging_leg_raise",
        ),
    )

    private val pullB = Session(
        name = "Pull B", focus = "Back Thickness",
        why = "Rowing angles this time. Width makes the V, thickness makes it look real from the side.",
        exerciseIds = listOf(
            "barbell_row", "lat_pulldown_close", "seated_cable_row",
            "rear_delt_fly", "barbell_curl",
        ),
    )

    private val pushB = Session(
        name = "Push B", focus = "Chest & Side Delts",
        why = "Second chest hit of the week, plus the side delts that widen the top of the V.",
        exerciseIds = listOf(
            "incline_barbell_press", "flat_db_press", "cable_lateral_raise",
            "chest_dip", "overhead_tricep_ext",
        ),
    )

    private val accessory = Session(
        name = "Core & Weak Points", focus = "Abs & Taper Detail",
        why = "Short session. Abs plus a third dose of the two muscles your goals depend on.",
        exerciseIds = listOf(
            "cable_crunch", "ab_wheel", "side_plank",
            "lateral_raise", "straight_arm_pulldown",
        ),
    )

    private val byDay: Map<DayOfWeek, Session?> = mapOf(
        DayOfWeek.MONDAY to pullA,
        DayOfWeek.TUESDAY to pushA,
        DayOfWeek.WEDNESDAY to legs,
        DayOfWeek.THURSDAY to pullB,
        DayOfWeek.FRIDAY to pushB,
        DayOfWeek.SATURDAY to accessory,
        DayOfWeek.SUNDAY to null,
    )

    fun forDay(day: DayOfWeek): Session? = byDay[day]

    val allSessions = listOf(pullA, pushA, legs, pullB, pushB, accessory)
}
