package com.example.vitalwearclonev1.fitness

/**
 * Fitness (2026-10-07): the 12 tracked muscle groups. Logging is per
 * group, not per exercise — Health Connect can't tell us "chest", so the
 * user taps the groups they worked (or picks a template that pre-fills
 * them).
 */
enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    ABS("Abs"),
    OBLIQUES("Obliques"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves");
}

/**
 * One-tap workout templates. Selecting one pre-fills the body map in the
 * log sheet; the user then taps to add/remove groups (the "Both" choice).
 */
enum class WorkoutTemplate(val displayName: String, val muscles: Set<MuscleGroup>) {
    PUSH(
        "Push",
        setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
    ),
    PULL(
        "Pull",
        setOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
    ),
    LEGS(
        "Legs",
        setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
    ),
    UPPER(
        "Upper",
        setOf(
            MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
            MuscleGroup.BICEPS, MuscleGroup.TRICEPS
        )
    ),
    CORE(
        "Core",
        setOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES)
    ),
    FULL_BODY(
        "Full Body",
        MuscleGroup.values().toSet()
    );
}
