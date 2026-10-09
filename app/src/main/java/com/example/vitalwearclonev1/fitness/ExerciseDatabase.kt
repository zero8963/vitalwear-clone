package com.example.vitalwearclonev1.fitness

/**
 * Fitness (2026-10-08): exercise → muscle mapping for auto-tracking.
 * Picking exercises in the log sheet auto-selects their muscles on the
 * body map (primary + secondary both marked; secondary-only groups get a
 * lighter shade). Manual map taps still override afterwards.
 *
 * Mapped to the same 12 MuscleGroup buckets the log already uses, so no
 * log-schema change is needed — the final set just flows through the
 * existing WorkoutLogStore path.
 */
data class Exercise(
    val name: String,
    val primary: Set<MuscleGroup>,
    val secondary: Set<MuscleGroup>
)

/** Built-in exercise database (~55 common lifts, bodyweight, machines, bands, core). */
val EXERCISES: List<Exercise> = listOf(
    // ---- Barbell / dumbbell compounds ----
    Exercise(
        "Barbell Bench Press",
        setOf(MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Dumbbell Bench Press",
        setOf(MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Incline Dumbbell Press",
        setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Dumbbell Floor Press",
        setOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Dumbbell Fly",
        setOf(MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Dumbbell Pullover",
        setOf(MuscleGroup.CHEST, MuscleGroup.BACK),
        setOf(MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Barbell Squat",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Dumbbell Goblet Squat",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Deadlift",
        setOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.FOREARMS, MuscleGroup.QUADS)
    ),
    Exercise(
        "Romanian Deadlift",
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.BACK, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Overhead Press",
        setOf(MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST)
    ),
    Exercise(
        "Dumbbell Shoulder Press",
        setOf(MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Barbell Row",
        setOf(MuscleGroup.BACK),
        setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Dumbbell Row",
        setOf(MuscleGroup.BACK),
        setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Dumbbell Shrug",
        setOf(MuscleGroup.BACK),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Barbell Curl",
        setOf(MuscleGroup.BICEPS),
        setOf(MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Dumbbell Curl",
        setOf(MuscleGroup.BICEPS),
        setOf(MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Hammer Curl",
        setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
        emptySet()
    ),
    Exercise(
        "Skull Crusher",
        setOf(MuscleGroup.TRICEPS),
        emptySet()
    ),
    Exercise(
        "Overhead Triceps Extension",
        setOf(MuscleGroup.TRICEPS),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Dumbbell Lateral Raise",
        setOf(MuscleGroup.SHOULDERS),
        emptySet()
    ),
    Exercise(
        "Dumbbell Front Raise",
        setOf(MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.CHEST)
    ),
    Exercise(
        "Dumbbell Lunge",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES)
    ),
    Exercise(
        "Dumbbell Step-Up",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Bulgarian Split Squat",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Farmer's Carry",
        setOf(MuscleGroup.FOREARMS),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.GLUTES)
    ),
    // ---- Bodyweight ----
    Exercise(
        "Pull-Up",
        setOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
        setOf(MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Chin-Up",
        setOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
        setOf(MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Push-Up",
        setOf(MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Diamond Push-Up",
        setOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Dip",
        setOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Inverted Row",
        setOf(MuscleGroup.BACK),
        setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Bodyweight Squat",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Walking Lunge",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES)
    ),
    Exercise(
        "Jump Rope",
        setOf(MuscleGroup.CALVES),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.QUADS)
    ),
    Exercise(
        "Burpee",
        setOf(MuscleGroup.CHEST, MuscleGroup.QUADS),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
    ),
    // ---- Machines / cables (commercial gym) ----
    Exercise(
        "Lat Pulldown",
        setOf(MuscleGroup.BACK),
        setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Seated Cable Row",
        setOf(MuscleGroup.BACK),
        setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Leg Press",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES)
    ),
    Exercise(
        "Cable Fly",
        setOf(MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Cable Triceps Pushdown",
        setOf(MuscleGroup.TRICEPS),
        emptySet()
    ),
    Exercise(
        "Leg Curl",
        setOf(MuscleGroup.HAMSTRINGS),
        setOf(MuscleGroup.CALVES)
    ),
    Exercise(
        "Leg Extension",
        setOf(MuscleGroup.QUADS),
        emptySet()
    ),
    Exercise(
        "Calf Raise (Machine)",
        setOf(MuscleGroup.CALVES),
        emptySet()
    ),
    Exercise(
        "Chest Press Machine",
        setOf(MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Hip Thrust",
        setOf(MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS)
    ),
    Exercise(
        "Cable Woodchopper",
        setOf(MuscleGroup.OBLIQUES, MuscleGroup.ABS),
        setOf(MuscleGroup.SHOULDERS)
    ),
    // ---- Bands (home gym) ----
    Exercise(
        "Band Chest Press",
        setOf(MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Band Row",
        setOf(MuscleGroup.BACK),
        setOf(MuscleGroup.BICEPS)
    ),
    Exercise(
        "Band Pull-Apart",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK),
        emptySet()
    ),
    Exercise(
        "Band Bicep Curl",
        setOf(MuscleGroup.BICEPS),
        setOf(MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Band Squat",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        emptySet()
    ),
    // ---- Core ----
    Exercise(
        "Ab Roller",
        setOf(MuscleGroup.ABS),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK)
    ),
    Exercise(
        "Plank",
        setOf(MuscleGroup.ABS),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.GLUTES)
    ),
    Exercise(
        "Hanging Leg Raise",
        setOf(MuscleGroup.ABS),
        setOf(MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Russian Twist",
        setOf(MuscleGroup.OBLIQUES, MuscleGroup.ABS),
        emptySet()
    ),
    Exercise(
        "Side Plank",
        setOf(MuscleGroup.OBLIQUES),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Dead Bug",
        setOf(MuscleGroup.ABS),
        emptySet()
    ),
    Exercise(
        "Mountain Climber",
        setOf(MuscleGroup.ABS, MuscleGroup.QUADS),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST)
    )
)
