package com.og.data

enum class MuscleGroup(val label: String) {
    BACK("Back"),
    CHEST("Chest"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    LEGS("Legs"),
    CORE("Core"),
    ;

    /** Every muscle in this group, for picking targets on a user-added lift. */
    val muscles: List<Muscle> get() = Muscle.entries.filter { it.group == this }

    /** Fallback so a custom lift always targets something and never counts for nothing. */
    val defaultMuscle: Muscle get() = muscles.first()
}

enum class Muscle(val label: String, val group: MuscleGroup) {
    LATS("Lats", MuscleGroup.BACK),
    TRAPS_UPPER("Upper Traps", MuscleGroup.BACK),
    TRAPS_MID("Mid Traps / Rhomboids", MuscleGroup.BACK),
    LOWER_BACK("Lower Back", MuscleGroup.BACK),

    CHEST_UPPER("Upper Chest", MuscleGroup.CHEST),
    CHEST_MID("Mid Chest", MuscleGroup.CHEST),
    CHEST_LOWER("Lower Chest", MuscleGroup.CHEST),

    FRONT_DELT("Front Delts", MuscleGroup.SHOULDERS),
    SIDE_DELT("Side Delts", MuscleGroup.SHOULDERS),
    REAR_DELT("Rear Delts", MuscleGroup.SHOULDERS),

    BICEPS("Biceps", MuscleGroup.ARMS),
    TRICEPS("Triceps", MuscleGroup.ARMS),
    FOREARMS("Forearms", MuscleGroup.ARMS),

    QUADS("Quads", MuscleGroup.LEGS),
    HAMSTRINGS("Hamstrings", MuscleGroup.LEGS),
    GLUTES("Glutes", MuscleGroup.LEGS),
    CALVES("Calves", MuscleGroup.LEGS),

    ABS("Abs", MuscleGroup.CORE),
    OBLIQUES("Obliques", MuscleGroup.CORE),
}

/** Muscles that visibly drive the user's stated goals, used to weight coverage warnings. */
val VTaperMuscles = setOf(Muscle.LATS, Muscle.SIDE_DELT, Muscle.TRAPS_MID, Muscle.CHEST_UPPER)
