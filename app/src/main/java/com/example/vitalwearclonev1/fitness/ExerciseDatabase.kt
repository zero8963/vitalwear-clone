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
    // ---- Core bodyweight (2026-10-27, user's request) ----
    Exercise(
        "Sit-Ups",
        setOf(MuscleGroup.ABS),
        // No hip-flexor group exists in the 12 tracked groups; the
        // rectus femoris (part of QUADS) is a hip flexor, so QUADS is
        // the closest tracked group. Excludable via the muscle chips.
        setOf(MuscleGroup.QUADS)
    ),
    Exercise(
        "Crunches",
        setOf(MuscleGroup.ABS),
        emptySet()
    ),
    Exercise(
        "Russian Twists",
        setOf(MuscleGroup.OBLIQUES),
        setOf(MuscleGroup.ABS)
    ),
    Exercise(
        "Sit-Up + Russian Twist Combo",
        setOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES),
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
    ),
    // ---- More lifts (commercial gym) ----
    Exercise(
        "Front Squat",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Hack Squat",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES)
    ),
    Exercise(
        "Smith Machine Squat",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Trap Bar Deadlift",
        setOf(MuscleGroup.BACK, MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Sumo Deadlift",
        setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
        setOf(MuscleGroup.BACK, MuscleGroup.QUADS)
    ),
    Exercise(
        "Good Morning",
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.BACK)
    ),
    Exercise(
        "Barbell Lunge",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES)
    ),
    Exercise(
        "Incline Barbell Press",
        setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Close-Grip Bench Press",
        setOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Machine Shoulder Press",
        setOf(MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Push Press",
        setOf(MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.TRICEPS, MuscleGroup.QUADS, MuscleGroup.GLUTES)
    ),
    Exercise(
        "Pec Deck Fly",
        setOf(MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Chest-Supported Row",
        setOf(MuscleGroup.BACK),
        setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Preacher Curl",
        setOf(MuscleGroup.BICEPS),
        setOf(MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Face Pull",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK),
        setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Rear Delt Fly",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK),
        emptySet()
    ),
    Exercise(
        "Cable Lateral Raise",
        setOf(MuscleGroup.SHOULDERS),
        emptySet()
    ),
    Exercise(
        "Kettlebell Swing",
        setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK)
    ),
    Exercise(
        "Wrist Curl",
        setOf(MuscleGroup.FOREARMS),
        emptySet()
    ),
    Exercise(
        "Cable Crunch",
        setOf(MuscleGroup.ABS),
        setOf(MuscleGroup.OBLIQUES)
    ),
    Exercise(
        "Hanging Knee Raise",
        setOf(MuscleGroup.ABS),
        setOf(MuscleGroup.FOREARMS)
    ),
    // ---- Cardio / machines (Health Connect exercise types) ----
    Exercise(
        "Running",
        setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
        setOf(MuscleGroup.GLUTES)
    ),
    Exercise(
        "Treadmill Running",
        setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
        setOf(MuscleGroup.GLUTES)
    ),
    Exercise(
        "Walking",
        setOf(MuscleGroup.CALVES, MuscleGroup.QUADS),
        setOf(MuscleGroup.GLUTES)
    ),
    Exercise(
        "Hiking",
        setOf(MuscleGroup.QUADS, MuscleGroup.CALVES, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Biking",
        setOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
        setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Stationary Bike",
        setOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
        setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Elliptical",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Rowing",
        setOf(MuscleGroup.BACK, MuscleGroup.QUADS),
        setOf(MuscleGroup.BICEPS, MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Rowing Machine",
        setOf(MuscleGroup.BACK, MuscleGroup.QUADS),
        setOf(MuscleGroup.BICEPS, MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Stair Climbing",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Stair Climber Machine",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
        setOf(MuscleGroup.HAMSTRINGS)
    ),
    Exercise(
        "Swimming (Pool)",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK),
        setOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS)
    ),
    Exercise(
        "Swimming (Open Water)",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK),
        setOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS)
    ),
    Exercise(
        "High-Intensity Interval Training",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ABS)
    ),
    Exercise(
        "Boot Camp",
        setOf(MuscleGroup.QUADS, MuscleGroup.CHEST),
        setOf(MuscleGroup.GLUTES, MuscleGroup.SHOULDERS, MuscleGroup.ABS)
    ),
    Exercise(
        "Calisthenics",
        setOf(MuscleGroup.CHEST, MuscleGroup.BACK),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.ABS, MuscleGroup.QUADS)
    ),
    Exercise(
        "Yoga",
        setOf(MuscleGroup.ABS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.QUADS, MuscleGroup.BACK)
    ),
    Exercise(
        "Pilates",
        setOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES),
        setOf(MuscleGroup.GLUTES, MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Stretching",
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK)
    ),
    // ---- Sports (Health Connect exercise types) ----
    Exercise(
        "Basketball",
        setOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST)
    ),
    Exercise(
        "Soccer",
        setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
        setOf(MuscleGroup.GLUTES)
    ),
    Exercise(
        "Tennis",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.FOREARMS),
        setOf(MuscleGroup.QUADS, MuscleGroup.CALVES, MuscleGroup.ABS)
    ),
    Exercise(
        "Badminton",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.FOREARMS),
        setOf(MuscleGroup.QUADS, MuscleGroup.CALVES)
    ),
    Exercise(
        "Squash",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.FOREARMS),
        setOf(MuscleGroup.QUADS, MuscleGroup.ABS)
    ),
    Exercise(
        "Racquetball",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.FOREARMS),
        setOf(MuscleGroup.QUADS, MuscleGroup.ABS)
    ),
    Exercise(
        "Table Tennis",
        setOf(MuscleGroup.FOREARMS, MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.ABS)
    ),
    Exercise(
        "Golf",
        setOf(MuscleGroup.OBLIQUES, MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.FOREARMS, MuscleGroup.BACK)
    ),
    Exercise(
        "Baseball",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.OBLIQUES),
        setOf(MuscleGroup.FOREARMS, MuscleGroup.QUADS)
    ),
    Exercise(
        "Softball",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.OBLIQUES),
        setOf(MuscleGroup.FOREARMS, MuscleGroup.QUADS)
    ),
    Exercise(
        "Volleyball",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.QUADS),
        setOf(MuscleGroup.CALVES, MuscleGroup.ABS)
    ),
    Exercise(
        "Rugby",
        setOf(MuscleGroup.QUADS, MuscleGroup.CHEST),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.GLUTES)
    ),
    Exercise(
        "American Football",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Hockey",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK)
    ),
    Exercise(
        "Ice Skating",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
        setOf(MuscleGroup.ABS)
    ),
    Exercise(
        "Skiing",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES)
    ),
    Exercise(
        "Snowboarding",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.CALVES, MuscleGroup.ABS)
    ),
    Exercise(
        "Snowshoeing",
        setOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
        setOf(MuscleGroup.GLUTES)
    ),
    Exercise(
        "Surfing",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK),
        setOf(MuscleGroup.ABS, MuscleGroup.QUADS)
    ),
    Exercise(
        "Rock Climbing",
        setOf(MuscleGroup.FOREARMS, MuscleGroup.BACK),
        setOf(MuscleGroup.BICEPS, MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Paddling",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK),
        setOf(MuscleGroup.ABS, MuscleGroup.FOREARMS)
    ),
    Exercise(
        "Sailing",
        setOf(MuscleGroup.FOREARMS, MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.ABS, MuscleGroup.BACK)
    ),
    Exercise(
        "Horseback Riding",
        setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        setOf(MuscleGroup.ABS, MuscleGroup.BACK)
    ),
    Exercise(
        "Gymnastics",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.ABS),
        setOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADS)
    ),
    Exercise(
        "Dancing",
        setOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
        setOf(MuscleGroup.GLUTES, MuscleGroup.ABS)
    ),
    Exercise(
        "Boxing",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
        setOf(MuscleGroup.CHEST, MuscleGroup.ABS, MuscleGroup.QUADS)
    ),
    Exercise(
        "Kickboxing",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.QUADS),
        setOf(MuscleGroup.TRICEPS, MuscleGroup.ABS, MuscleGroup.GLUTES)
    ),
    Exercise(
        "Martial Arts",
        setOf(MuscleGroup.QUADS, MuscleGroup.SHOULDERS),
        setOf(MuscleGroup.ABS, MuscleGroup.CHEST, MuscleGroup.GLUTES)
    ),
    Exercise(
        "Mixed Martial Arts",
        setOf(MuscleGroup.CHEST, MuscleGroup.QUADS),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.ABS)
    ),
    Exercise(
        "Fencing",
        setOf(MuscleGroup.QUADS, MuscleGroup.FOREARMS),
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.CALVES)
    ),
    Exercise(
        "Handball",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.QUADS),
        setOf(MuscleGroup.FOREARMS, MuscleGroup.CALVES)
    ),
    Exercise(
        "Cricket",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.FOREARMS),
        setOf(MuscleGroup.QUADS)
    ),
    Exercise(
        "Frisbee",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.FOREARMS),
        setOf(MuscleGroup.QUADS)
    ),
    Exercise(
        "Scuba Diving",
        setOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
        setOf(MuscleGroup.SHOULDERS)
    ),
    Exercise(
        "Water Polo",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK),
        setOf(MuscleGroup.QUADS, MuscleGroup.ABS)
    ),
    Exercise(
        "Wheelchair",
        setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
        setOf(MuscleGroup.CHEST, MuscleGroup.FOREARMS)
    )
)
