package com.example.vitalwearclonev1.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.example.vitalwearclonev1.communication.PhoneHealthSyncManager
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Fitness (2026-10-08): Health Connect workout auto-import.
 * Recent exercise sessions show up as suggestion cards on the Body Map
 * tab. Cardio types map to muscles directly; strength-style sessions get
 * a keyword guess from the title when confident, otherwise a one-tap
 * split quick-pick. Confirming logs the muscles for the session's date
 * via the existing WorkoutLogStore path.
 *
 * Honest by design: Health Connect only shares the session type,
 * duration and title — not the per-exercise breakdown — so every card is
 * a smart guess the user confirms, same as the manual flow.
 */
data class SessionSuggestion(
    val key: String,
    val exerciseType: Int,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant,
    /** Null when no confident guess — the card shows the split quick-pick. */
    val guessedMuscles: Set<MuscleGroup>?
)

/** Dedupe key: session start + type is unique per Health Connect session. */
fun sessionKey(record: ExerciseSessionRecord): String =
    "${record.startTime.toEpochMilli()}_${record.exerciseType}"

fun exerciseTypeDisplayName(type: Int): String = when (type) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Run"
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walk"
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hike"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Bike"
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swim"
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Row"
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength"
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Weightlifting"
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "Calisthenics"
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Elliptical"
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "Stairs"
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "Pilates"
    ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> "Boxing"
    ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "Dance"
    else -> "Workout"
}

/**
 * Smart muscle mapping. Cardio types map directly (no title needed).
 * Strength-style sessions get a keyword guess from the title when
 * confident; null means "ask the user" — the card shows the split
 * quick-pick instead.
 */
fun guessMusclesForSession(exerciseType: Int, title: String?): Set<MuscleGroup>? {
    when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL ->
            return setOf(
                MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS,
                MuscleGroup.CALVES, MuscleGroup.GLUTES
            )
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING ->
            return setOf(MuscleGroup.QUADS, MuscleGroup.CALVES, MuscleGroup.GLUTES)
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER ->
            return setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.CHEST)
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE ->
            return setOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.QUADS)
    }
    val t = title?.lowercase().orEmpty()
    if (t.isBlank()) return null
    return when {
        "full" in t || "total body" in t -> WorkoutTemplate.FULL_BODY.muscles
        "leg" in t || "quad" in t || "glute" in t || "hamstring" in t ||
            "squat" in t || "deadlift" in t -> WorkoutTemplate.LEGS.muscles
        "push" in t || "chest" in t -> WorkoutTemplate.PUSH.muscles
        "pull" in t || "back" in t -> WorkoutTemplate.PULL.muscles
        "upper" in t -> WorkoutTemplate.UPPER.muscles
        "core" in t || "abs" in t -> WorkoutTemplate.CORE.muscles
        "shoulder" in t || "arm" in t || "bicep" in t || "tricep" in t ->
            setOf(MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS)
        else -> null
    }
}

@Composable
fun WorkoutImportSection(
    syncManager: PhoneHealthSyncManager,
    store: WorkoutLogStore,
    onImported: () -> Unit
) {
    var suggestions by remember { mutableStateOf<List<SessionSuggestion>?>(null) }

    LaunchedEffect(Unit) {
        val sessions = withContext(Dispatchers.IO) {
            syncManager.getRecentExerciseSessions(7)
        }
        suggestions = sessions
            .filter { !store.isSessionImported(sessionKey(it)) }
            .map {
                SessionSuggestion(
                    key = sessionKey(it),
                    exerciseType = it.exerciseType,
                    title = it.title,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    guessedMuscles = guessMusclesForSession(it.exerciseType, it.title)
                )
            }
    }

    val list = suggestions ?: return
    if (list.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Workouts to log",
            color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold
        )
        Text(
            "From Health Connect (last 7 days). It only shares the workout " +
                "type, duration and title — not each exercise — so these are " +
                "smart guesses. Confirm to log them like a manual entry.",
            color = Color.Gray, fontSize = 11.sp
        )
        list.forEach { s ->
            SessionSuggestionCard(
                suggestion = s,
                onConfirmed = {
                    suggestions = list - s
                    onImported()
                },
                syncManager = syncManager,
                store = store
            )
        }
    }
}

@Composable
private fun SessionSuggestionCard(
    suggestion: SessionSuggestion,
    syncManager: PhoneHealthSyncManager,
    store: WorkoutLogStore,
    onConfirmed: () -> Unit
) {
    val context = LocalContext.current
    var pickedSplit by remember { mutableStateOf<WorkoutTemplate?>(null) }
    val needsSplitPick = suggestion.guessedMuscles == null
    val muscles: Set<MuscleGroup> =
        suggestion.guessedMuscles ?: pickedSplit?.muscles ?: emptySet()

    val zone = remember { ZoneId.systemDefault() }
    val date = remember(suggestion) { LocalDate.ofInstant(suggestion.startTime, zone) }
    val today = remember { LocalDate.now(zone) }
    val whenText = remember(date) {
        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEE MMM d"))
        }
    }
    val timeText = remember(suggestion) {
        suggestion.startTime.atZone(zone)
            .format(DateTimeFormatter.ofPattern("h:mma")).lowercase()
    }
    val mins = remember(suggestion) {
        ChronoUnit.MINUTES.between(suggestion.startTime, suggestion.endTime)
            .coerceAtLeast(1)
    }

    Card(
        backgroundColor = Color(0, 50, 90),
        shape = RoundedCornerShape(16.dp),
        elevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "${exerciseTypeDisplayName(suggestion.exerciseType)} • ${mins} min • $whenText $timeText",
                color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            suggestion.title?.takeIf { it.isNotBlank() }?.let { title ->
                Text("\"$title\"", color = Color.White, fontSize = 13.sp)
            }
            if (needsSplitPick) {
                Text(
                    "Couldn't guess the split from the title — pick one:",
                    color = Color.Gray, fontSize = 12.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkoutTemplate.values().toList().chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { t ->
                                val isPicked = pickedSplit == t
                                Button(
                                    onClick = { pickedSplit = t },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (isPicked) Color.Cyan
                                        else Color(20, 60, 100)
                                    )
                                ) {
                                    Text(
                                        t.displayName,
                                        color = if (isPicked) Color.Black else Color.White,
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (muscles.isNotEmpty()) {
                Text(
                    "Muscles: " + muscles.sortedBy { it.name }
                        .joinToString(", ") { it.displayName },
                    color = Color.LightGray, fontSize = 12.sp
                )
            }
            Button(
                onClick = {
                    // Per-exercise workout credit (2026-10-08): a confirmed
                    // import earns the same trophy/VP credit as a Health
                    // Connect session — unless the auto path already
                    // credited it (no double-counting).
                    if (!syncManager.wasSessionCredited(suggestion.startTime)) {
                        PhoneMonsterManager(context).recordExerciseCompleted()
                    }
                    val name = WorkoutTemplate.values()
                        .firstOrNull { it.muscles == muscles }?.displayName
                        ?: "Custom"
                    store.logWorkout(date, name, muscles)
                    store.markSessionImported(suggestion.key)
                    store.markInAppExercisesLogged(date)
                    onConfirmed()
                },
                enabled = muscles.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 100))
            ) {
                Text("Log these muscles", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}
