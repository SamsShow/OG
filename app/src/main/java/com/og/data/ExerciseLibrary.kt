package com.og.data

import com.og.data.Muscle.ABS
import com.og.data.Muscle.BICEPS
import com.og.data.Muscle.CALVES
import com.og.data.Muscle.CHEST_LOWER
import com.og.data.Muscle.CHEST_MID
import com.og.data.Muscle.CHEST_UPPER
import com.og.data.Muscle.FOREARMS
import com.og.data.Muscle.FRONT_DELT
import com.og.data.Muscle.GLUTES
import com.og.data.Muscle.HAMSTRINGS
import com.og.data.Muscle.LATS
import com.og.data.Muscle.LOWER_BACK
import com.og.data.Muscle.OBLIQUES
import com.og.data.Muscle.QUADS
import com.og.data.Muscle.REAR_DELT
import com.og.data.Muscle.SIDE_DELT
import com.og.data.Muscle.TRAPS_MID
import com.og.data.Muscle.TRAPS_UPPER
import com.og.data.Muscle.TRICEPS

enum class Equipment(val label: String) {
    BARBELL("Barbell"),
    DUMBBELL("Dumbbell"),
    MACHINE("Machine"),
    CABLE("Cable"),
    BODYWEIGHT("Bodyweight"),
}

data class Exercise(
    val id: String,
    val name: String,
    val group: MuscleGroup,
    val primary: List<Muscle>,
    val secondary: List<Muscle> = emptyList(),
    val equipment: Equipment,
    /** Shown on the exercise card. Only present where it changes how the lift should be used. */
    val note: String? = null,
    /** Pre-fills the weight field the first time this lift is logged. */
    val seedKg: Double? = null,
) {
    val allMuscles: List<Muscle> get() = primary + secondary
}

object ExerciseLibrary {

    private val builtIn: List<Exercise> = listOf(
        // ---------- BACK : the priority group for width and the V taper ----------
        Exercise(
            "lat_pulldown", "Lat Pulldown (Wide Grip)", MuscleGroup.BACK,
            primary = listOf(LATS),
            secondary = listOf(TRAPS_MID, BICEPS, REAR_DELT),
            equipment = Equipment.CABLE,
            note = "Your main width builder. Wide grip, drive elbows down, lean back slightly.",
            seedKg = 45.0,
        ),
        Exercise(
            "pull_up", "Pull-Up", MuscleGroup.BACK,
            primary = listOf(LATS),
            secondary = listOf(BICEPS, TRAPS_MID, ABS),
            equipment = Equipment.BODYWEIGHT,
            note = "Log added weight only. Bodyweight sets count as 0 kg.",
        ),
        Exercise(
            "chin_up", "Chin-Up", MuscleGroup.BACK,
            primary = listOf(LATS, BICEPS),
            secondary = listOf(TRAPS_MID),
            equipment = Equipment.BODYWEIGHT,
        ),
        Exercise(
            "lat_pulldown_close", "Close-Grip Lat Pulldown", MuscleGroup.BACK,
            primary = listOf(LATS),
            secondary = listOf(BICEPS, TRAPS_MID),
            equipment = Equipment.CABLE,
            note = "Hits lower lats — adds thickness near the waist, sharpening the taper.",
        ),
        Exercise(
            "straight_arm_pulldown", "Straight-Arm Pulldown", MuscleGroup.BACK,
            primary = listOf(LATS),
            equipment = Equipment.CABLE,
            note = "Pure lat isolation with the biceps taken out. Best finisher for width.",
        ),
        Exercise(
            "barbell_row", "Barbell Row", MuscleGroup.BACK,
            primary = listOf(LATS, TRAPS_MID),
            secondary = listOf(REAR_DELT, LOWER_BACK, BICEPS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "pendlay_row", "Pendlay Row", MuscleGroup.BACK,
            primary = listOf(TRAPS_MID, LATS),
            secondary = listOf(REAR_DELT, LOWER_BACK),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "dumbbell_row", "One-Arm Dumbbell Row", MuscleGroup.BACK,
            primary = listOf(LATS),
            secondary = listOf(TRAPS_MID, BICEPS, REAR_DELT),
            equipment = Equipment.DUMBBELL,
            note = "Log the weight of one dumbbell.",
        ),
        Exercise(
            "seated_cable_row", "Seated Cable Row", MuscleGroup.BACK,
            primary = listOf(TRAPS_MID, LATS),
            secondary = listOf(REAR_DELT, BICEPS),
            equipment = Equipment.CABLE,
        ),
        Exercise(
            "t_bar_row", "T-Bar Row", MuscleGroup.BACK,
            primary = listOf(TRAPS_MID, LATS),
            secondary = listOf(REAR_DELT, LOWER_BACK, BICEPS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "chest_supported_row", "Chest-Supported Row", MuscleGroup.BACK,
            primary = listOf(TRAPS_MID),
            secondary = listOf(LATS, REAR_DELT, BICEPS),
            equipment = Equipment.MACHINE,
            note = "Chest pad removes lower-back fatigue, so the back gets the full stimulus.",
        ),
        Exercise(
            "shrug", "Barbell Shrug", MuscleGroup.BACK,
            primary = listOf(TRAPS_UPPER),
            secondary = listOf(FOREARMS),
            equipment = Equipment.BARBELL,
            note = "Go easy. Heavy upper traps square off the shoulders and blunt the V.",
        ),
        Exercise(
            "deadlift", "Deadlift", MuscleGroup.BACK,
            primary = listOf(LOWER_BACK, GLUTES, HAMSTRINGS),
            secondary = listOf(TRAPS_UPPER, LATS, FOREARMS, QUADS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "rack_pull", "Rack Pull", MuscleGroup.BACK,
            primary = listOf(LOWER_BACK, TRAPS_UPPER),
            secondary = listOf(GLUTES, LATS, FOREARMS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "back_extension", "Back Extension", MuscleGroup.BACK,
            primary = listOf(LOWER_BACK),
            secondary = listOf(GLUTES, HAMSTRINGS),
            equipment = Equipment.BODYWEIGHT,
        ),

        // ---------- CHEST : upper-chest bias, the second stated goal ----------
        Exercise(
            "incline_db_press", "Incline Dumbbell Press", MuscleGroup.CHEST,
            primary = listOf(CHEST_UPPER),
            secondary = listOf(FRONT_DELT, TRICEPS),
            equipment = Equipment.DUMBBELL,
            note = "Log one dumbbell's weight. 30° bench — steeper turns it into a shoulder press.",
            seedKg = 22.5,
        ),
        Exercise(
            "incline_barbell_press", "Incline Barbell Press", MuscleGroup.CHEST,
            primary = listOf(CHEST_UPPER),
            secondary = listOf(FRONT_DELT, TRICEPS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "cable_fly_low_to_high", "Low-to-High Cable Fly", MuscleGroup.CHEST,
            primary = listOf(CHEST_UPPER),
            equipment = Equipment.CABLE,
            note = "The cleanest upper-chest isolation there is. Pair it with incline pressing.",
        ),
        Exercise(
            "incline_db_fly", "Incline Dumbbell Fly", MuscleGroup.CHEST,
            primary = listOf(CHEST_UPPER),
            equipment = Equipment.DUMBBELL,
        ),
        Exercise(
            "flat_barbell_bench", "Barbell Bench Press", MuscleGroup.CHEST,
            primary = listOf(CHEST_MID),
            secondary = listOf(TRICEPS, FRONT_DELT),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "flat_db_press", "Flat Dumbbell Press", MuscleGroup.CHEST,
            primary = listOf(CHEST_MID),
            secondary = listOf(TRICEPS, FRONT_DELT),
            equipment = Equipment.DUMBBELL,
            note = "Log one dumbbell's weight.",
        ),
        Exercise(
            "machine_chest_press", "Machine Chest Press", MuscleGroup.CHEST,
            primary = listOf(CHEST_MID),
            secondary = listOf(TRICEPS, FRONT_DELT),
            equipment = Equipment.MACHINE,
        ),
        Exercise(
            "cable_fly_mid", "Cable Fly", MuscleGroup.CHEST,
            primary = listOf(CHEST_MID),
            equipment = Equipment.CABLE,
        ),
        Exercise(
            "pec_deck", "Pec Deck", MuscleGroup.CHEST,
            primary = listOf(CHEST_MID),
            equipment = Equipment.MACHINE,
        ),
        Exercise(
            "pushup", "Push-Up", MuscleGroup.CHEST,
            primary = listOf(CHEST_MID),
            secondary = listOf(TRICEPS, FRONT_DELT, ABS),
            equipment = Equipment.BODYWEIGHT,
        ),
        Exercise(
            "chest_dip", "Chest Dip", MuscleGroup.CHEST,
            primary = listOf(CHEST_LOWER),
            secondary = listOf(TRICEPS, FRONT_DELT),
            equipment = Equipment.BODYWEIGHT,
            note = "Lean the torso forward to bias chest over triceps.",
        ),
        Exercise(
            "decline_press", "Decline Bench Press", MuscleGroup.CHEST,
            primary = listOf(CHEST_LOWER),
            secondary = listOf(TRICEPS),
            equipment = Equipment.BARBELL,
        ),

        // ---------- SHOULDERS : side delts widen the top of the V ----------
        Exercise(
            "lateral_raise", "Lateral Raise", MuscleGroup.SHOULDERS,
            primary = listOf(SIDE_DELT),
            equipment = Equipment.DUMBBELL,
            note = "Widens the top of the V. Light weight, high reps, no swinging.",
        ),
        Exercise(
            "cable_lateral_raise", "Cable Lateral Raise", MuscleGroup.SHOULDERS,
            primary = listOf(SIDE_DELT),
            equipment = Equipment.CABLE,
            note = "Constant tension through the whole range — better than dumbbells here.",
        ),
        Exercise(
            "ohp", "Overhead Press", MuscleGroup.SHOULDERS,
            primary = listOf(FRONT_DELT),
            secondary = listOf(SIDE_DELT, TRICEPS, ABS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "db_shoulder_press", "Dumbbell Shoulder Press", MuscleGroup.SHOULDERS,
            primary = listOf(FRONT_DELT),
            secondary = listOf(SIDE_DELT, TRICEPS),
            equipment = Equipment.DUMBBELL,
            note = "Log one dumbbell's weight.",
        ),
        Exercise(
            "arnold_press", "Arnold Press", MuscleGroup.SHOULDERS,
            primary = listOf(FRONT_DELT, SIDE_DELT),
            secondary = listOf(TRICEPS),
            equipment = Equipment.DUMBBELL,
        ),
        Exercise(
            "rear_delt_fly", "Rear Delt Fly", MuscleGroup.SHOULDERS,
            primary = listOf(REAR_DELT),
            secondary = listOf(TRAPS_MID),
            equipment = Equipment.DUMBBELL,
        ),
        Exercise(
            "face_pull", "Face Pull", MuscleGroup.SHOULDERS,
            primary = listOf(REAR_DELT),
            secondary = listOf(TRAPS_MID),
            equipment = Equipment.CABLE,
            note = "Counters all the pressing. Keeps shoulders healthy and posture upright.",
        ),
        Exercise(
            "upright_row", "Upright Row", MuscleGroup.SHOULDERS,
            primary = listOf(SIDE_DELT, TRAPS_UPPER),
            secondary = listOf(BICEPS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "front_raise", "Front Raise", MuscleGroup.SHOULDERS,
            primary = listOf(FRONT_DELT),
            equipment = Equipment.DUMBBELL,
            note = "Rarely needed — pressing already covers front delts.",
        ),

        // ---------- ARMS ----------
        Exercise(
            "barbell_curl", "Barbell Curl", MuscleGroup.ARMS,
            primary = listOf(BICEPS), secondary = listOf(FOREARMS), equipment = Equipment.BARBELL,
        ),
        Exercise(
            "dumbbell_curl", "Dumbbell Curl", MuscleGroup.ARMS,
            primary = listOf(BICEPS), secondary = listOf(FOREARMS), equipment = Equipment.DUMBBELL,
            note = "Log one dumbbell's weight.",
        ),
        Exercise(
            "hammer_curl", "Hammer Curl", MuscleGroup.ARMS,
            primary = listOf(BICEPS, FOREARMS), equipment = Equipment.DUMBBELL,
            note = "Builds the brachialis, which pushes the bicep up and makes the arm look thicker.",
        ),
        Exercise(
            "incline_db_curl", "Incline Dumbbell Curl", MuscleGroup.ARMS,
            primary = listOf(BICEPS), equipment = Equipment.DUMBBELL,
        ),
        Exercise(
            "preacher_curl", "Preacher Curl", MuscleGroup.ARMS,
            primary = listOf(BICEPS), equipment = Equipment.MACHINE,
        ),
        Exercise(
            "cable_curl", "Cable Curl", MuscleGroup.ARMS,
            primary = listOf(BICEPS), secondary = listOf(FOREARMS), equipment = Equipment.CABLE,
        ),
        Exercise(
            "close_grip_bench", "Close-Grip Bench Press", MuscleGroup.ARMS,
            primary = listOf(TRICEPS), secondary = listOf(CHEST_MID, FRONT_DELT),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "skullcrusher", "Skullcrusher", MuscleGroup.ARMS,
            primary = listOf(TRICEPS), equipment = Equipment.BARBELL,
        ),
        Exercise(
            "tricep_pushdown", "Triceps Pushdown", MuscleGroup.ARMS,
            primary = listOf(TRICEPS), equipment = Equipment.CABLE,
        ),
        Exercise(
            "overhead_tricep_ext", "Overhead Triceps Extension", MuscleGroup.ARMS,
            primary = listOf(TRICEPS), equipment = Equipment.CABLE,
            note = "Stretches the long head — the part that adds visible arm size.",
        ),
        Exercise(
            "tricep_dip", "Triceps Dip", MuscleGroup.ARMS,
            primary = listOf(TRICEPS), secondary = listOf(CHEST_LOWER, FRONT_DELT),
            equipment = Equipment.BODYWEIGHT,
            note = "Stay upright to keep tension on the triceps.",
        ),
        Exercise(
            "wrist_curl", "Wrist Curl", MuscleGroup.ARMS,
            primary = listOf(FOREARMS), equipment = Equipment.DUMBBELL,
        ),
        Exercise(
            "farmers_carry", "Farmer's Carry", MuscleGroup.ARMS,
            primary = listOf(FOREARMS), secondary = listOf(TRAPS_UPPER, ABS, OBLIQUES),
            equipment = Equipment.DUMBBELL,
            note = "Log weight per hand and reps as seconds carried.",
        ),

        // ---------- LEGS ----------
        Exercise(
            "leg_press", "Leg Press", MuscleGroup.LEGS,
            primary = listOf(QUADS, GLUTES), secondary = listOf(HAMSTRINGS),
            equipment = Equipment.MACHINE,
            seedKg = 100.0,
        ),
        Exercise(
            "back_squat", "Barbell Back Squat", MuscleGroup.LEGS,
            primary = listOf(QUADS, GLUTES),
            secondary = listOf(HAMSTRINGS, LOWER_BACK, ABS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "front_squat", "Front Squat", MuscleGroup.LEGS,
            primary = listOf(QUADS), secondary = listOf(GLUTES, ABS, LOWER_BACK),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "hack_squat", "Hack Squat", MuscleGroup.LEGS,
            primary = listOf(QUADS), secondary = listOf(GLUTES),
            equipment = Equipment.MACHINE,
        ),
        Exercise(
            "bulgarian_split_squat", "Bulgarian Split Squat", MuscleGroup.LEGS,
            primary = listOf(QUADS, GLUTES), secondary = listOf(HAMSTRINGS),
            equipment = Equipment.DUMBBELL,
        ),
        Exercise(
            "lunge", "Walking Lunge", MuscleGroup.LEGS,
            primary = listOf(QUADS, GLUTES), secondary = listOf(HAMSTRINGS),
            equipment = Equipment.DUMBBELL,
        ),
        Exercise(
            "leg_extension", "Leg Extension", MuscleGroup.LEGS,
            primary = listOf(QUADS), equipment = Equipment.MACHINE,
        ),
        Exercise(
            "romanian_deadlift", "Romanian Deadlift", MuscleGroup.LEGS,
            primary = listOf(HAMSTRINGS, GLUTES), secondary = listOf(LOWER_BACK, FOREARMS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "leg_curl", "Lying Leg Curl", MuscleGroup.LEGS,
            primary = listOf(HAMSTRINGS), secondary = listOf(CALVES),
            equipment = Equipment.MACHINE,
        ),
        Exercise(
            "seated_leg_curl", "Seated Leg Curl", MuscleGroup.LEGS,
            primary = listOf(HAMSTRINGS), equipment = Equipment.MACHINE,
        ),
        Exercise(
            "hip_thrust", "Hip Thrust", MuscleGroup.LEGS,
            primary = listOf(GLUTES), secondary = listOf(HAMSTRINGS),
            equipment = Equipment.BARBELL,
        ),
        Exercise(
            "calf_raise", "Standing Calf Raise", MuscleGroup.LEGS,
            primary = listOf(CALVES), equipment = Equipment.MACHINE,
        ),
        Exercise(
            "seated_calf_raise", "Seated Calf Raise", MuscleGroup.LEGS,
            primary = listOf(CALVES), equipment = Equipment.MACHINE,
        ),

        // ---------- CORE ----------
        Exercise(
            "hanging_leg_raise", "Hanging Leg Raise", MuscleGroup.CORE,
            primary = listOf(ABS), secondary = listOf(OBLIQUES, FOREARMS),
            equipment = Equipment.BODYWEIGHT,
            note = "Log reps; leave weight at 0 unless you hold a dumbbell.",
        ),
        Exercise(
            "cable_crunch", "Cable Crunch", MuscleGroup.CORE,
            primary = listOf(ABS), equipment = Equipment.CABLE,
            note = "The only ab movement you can progressively overload cleanly.",
        ),
        Exercise(
            "crunch", "Crunch", MuscleGroup.CORE,
            primary = listOf(ABS), equipment = Equipment.BODYWEIGHT,
        ),
        Exercise(
            "ab_wheel", "Ab Wheel Rollout", MuscleGroup.CORE,
            primary = listOf(ABS), secondary = listOf(OBLIQUES, LOWER_BACK),
            equipment = Equipment.BODYWEIGHT,
        ),
        Exercise(
            "plank", "Plank", MuscleGroup.CORE,
            primary = listOf(ABS), secondary = listOf(OBLIQUES),
            equipment = Equipment.BODYWEIGHT,
            note = "Log reps as seconds held.",
        ),
        Exercise(
            "side_plank", "Side Plank", MuscleGroup.CORE,
            primary = listOf(OBLIQUES), secondary = listOf(ABS),
            equipment = Equipment.BODYWEIGHT,
            note = "Log reps as seconds held.",
        ),
        Exercise(
            "russian_twist", "Russian Twist", MuscleGroup.CORE,
            primary = listOf(OBLIQUES), secondary = listOf(ABS),
            equipment = Equipment.BODYWEIGHT,
        ),
        Exercise(
            "mountain_climber", "Mountain Climber", MuscleGroup.CORE,
            primary = listOf(ABS), secondary = listOf(OBLIQUES, QUADS),
            equipment = Equipment.BODYWEIGHT,
        ),
    )

    private val builtInById = builtIn.associateBy { it.id }

    /**
     * User-added lifts, mirrored here from the database on load.
     *
     * Deliberately a mutable registry rather than a constructor argument: [Analytics] and
     * several composables look exercises up statically, and a set logged against a custom
     * lift has to resolve its muscles everywhere or it would silently count for nothing.
     */
    @Volatile private var custom: Map<String, Exercise> = emptyMap()

    fun setCustom(list: List<Exercise>) {
        custom = list.associateBy { it.id }
    }

    val all: List<Exercise> get() = builtIn + custom.values.sortedBy { it.name }

    operator fun get(id: String): Exercise? = builtInById[id] ?: custom[id]

    fun byGroup(group: MuscleGroup): List<Exercise> = all.filter { it.group == group }

    fun forMuscle(muscle: Muscle): List<Exercise> =
        all.filter { muscle in it.primary } + all.filter { muscle in it.secondary }

    fun isCustom(id: String): Boolean = id in custom
}
