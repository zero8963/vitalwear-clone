package com.example.vitalwearclonev1.fitness

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Fitness (2026-10-07): SharedPreferences-backed workout log.
 * One entry per logged workout: date, template name (or "Custom"), the
 * muscle groups hit, and a timestamp. Stored as a simple delimited string
 * per date key — no new dependencies.
 */
data class LoggedWorkout(
    val date: LocalDate,
    val templateName: String,
    val muscles: Set<MuscleGroup>,
    val timestamp: Long,
    // Fitness (2026-10-27): per-exercise log detail from the Exercises
    // tab commit flow (sets x reps x weight in lb). Null on every entry
    // written before this change — decode keeps those readable.
    val exerciseName: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val weightLb: Float? = null
) {
    /** One-line summary of the logged effort, e.g. "3x10 @ 50 lb". */
    fun effortText(): String? {
        if (sets == null || reps == null) return null
        val weight = weightLb?.let { " @ ${formatWeightLb(it)} lb" } ?: " (bodyweight)"
        return "${sets}x$reps$weight"
    }
}

/** Weight with no trailing ".0" — 50 lb, not 50.0 lb. */
fun formatWeightLb(weightLb: Float): String =
    if (weightLb == weightLb.toInt().toFloat()) weightLb.toInt().toString()
    else weightLb.toString()

class WorkoutLogStore(private val context: Context) {

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun prefs() =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun keyFor(date: LocalDate) = "log_${date.format(dateFmt)}"

    fun logWorkout(date: LocalDate, templateName: String, muscles: Set<MuscleGroup>) {
        if (muscles.isEmpty()) return
        val existing = getWorkoutsForDate(date).toMutableList()
        existing.add(LoggedWorkout(date, templateName, muscles, System.currentTimeMillis()))
        prefs().edit().putString(keyFor(date), encode(existing)).apply()
        val dates = getLoggedDateStrings().toMutableSet()
        dates.add(date.format(dateFmt))
        prefs().edit().putString(INDEX_KEY, dates.joinToString(",")).apply()
    }

    fun getWorkoutsForDate(date: LocalDate): List<LoggedWorkout> =
        decode(date, prefs().getString(keyFor(date), null))

    /**
     * Fitness (2026-10-27): per-exercise commit from the Exercises tab.
     * One entry per committed exercise — same log, same credit path as
     * the template/sheet flow, plus the sets x reps x weight detail.
     */
    fun logExercise(
        date: LocalDate,
        exercise: Exercise,
        muscles: Set<MuscleGroup>,
        sets: Int,
        reps: Int,
        weightLb: Float?
    ) {
        if (muscles.isEmpty()) return
        val existing = getWorkoutsForDate(date).toMutableList()
        existing.add(
            LoggedWorkout(
                date, exercise.name, muscles, System.currentTimeMillis(),
                exerciseName = exercise.name, sets = sets, reps = reps,
                weightLb = weightLb
            )
        )
        prefs().edit().putString(keyFor(date), encode(existing)).apply()
        val dates = getLoggedDateStrings().toMutableSet()
        dates.add(date.format(dateFmt))
        prefs().edit().putString(INDEX_KEY, dates.joinToString(",")).apply()
    }

    /** Most recent per-exercise entry that hit [muscle], or null. */
    fun getLastLoggedWorkoutForMuscle(muscle: MuscleGroup): LoggedWorkout? {
        var best: LoggedWorkout? = null
        for (ds in getLoggedDateStrings()) {
            val date = try { LocalDate.parse(ds, dateFmt) } catch (e: Exception) { continue }
            for (w in getWorkoutsForDate(date)) {
                if (w.muscles.contains(muscle) &&
                    (best == null || w.timestamp > best.timestamp)
                ) best = w
            }
        }
        return best
    }

    /**
     * Distinct exercise names logged in the last [daysBack] days, most
     * recent first — quick re-log chips on the Exercises tab. Only
     * per-exercise commits have names; template/sheet logs are skipped.
     */
    fun getRecentExerciseNames(limit: Int = 8, daysBack: Long = 14): List<String> {
        val cutoff = LocalDate.now().minusDays(daysBack)
        val seen = linkedSetOf<String>()
        val dates = getLoggedDateStrings().mapNotNull { ds ->
            try { LocalDate.parse(ds, dateFmt) } catch (e: Exception) { null }
        }.filter { !it.isBefore(cutoff) }.sortedDescending()
        for (date in dates) {
            val names = getWorkoutsForDate(date)
                .sortedByDescending { it.timestamp }
                .mapNotNull { it.exerciseName }
            for (name in names) {
                seen.add(name)
                if (seen.size >= limit) return seen.toList()
            }
        }
        return seen.toList()
    }

    /** Every date string (yyyy-MM-dd) that has at least one logged workout. */
    fun getLoggedDateStrings(): Set<String> {
        val raw = prefs().getString(INDEX_KEY, null) ?: return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    /** Most recent date the given muscle was trained, or null if never. */
    fun getLastTrainedDate(muscle: MuscleGroup): LocalDate? {
        var best: LocalDate? = null
        for (ds in getLoggedDateStrings()) {
            val date = try { LocalDate.parse(ds, dateFmt) } catch (e: Exception) { continue }
            val hit = getWorkoutsForDate(date).any { it.muscles.contains(muscle) }
            if (hit && (best == null || date.isAfter(best))) best = date
        }
        return best
    }

    /**
     * Fitness (2026-10-08): Health Connect auto-import bookkeeping.
     * Imported session keys ("startMs_exerciseType") so re-opening the
     * tab never re-suggests an already-imported session.
     */
    fun isSessionImported(sessionKey: String): Boolean =
        prefs().getStringSet(IMPORTED_SESSIONS_KEY, emptySet()).orEmpty()
            .contains(sessionKey)

    fun markSessionImported(sessionKey: String) {
        val cur = prefs().getStringSet(IMPORTED_SESSIONS_KEY, emptySet())
            .orEmpty().toMutableSet()
        cur.add(sessionKey)
        prefs().edit().putStringSet(IMPORTED_SESSIONS_KEY, cur).apply()
    }

    /**
     * Fitness (2026-10-08, time-overlap matching): timestamps (epoch ms)
     * of in-app logs that granted workout credit (exercise picker saves,
     * import-card confirms). Each one covers a session window
     * [timestamp - IN_APP_LOG_WINDOW_MS, timestamp]. The Health Connect
     * auto-credit path denies credit to sessions overlapping one of these
     * windows — same workout, already counted — while non-overlapping
     * sessions (e.g. a morning run vs an afternoon in-app gym log) each
     * get their full credit. Replaces the old day-granularity mark, which
     * wrongly suppressed a whole day's sessions.
     */
    fun markInAppExerciseLog(timestampMs: Long = System.currentTimeMillis()) {
        val cur = prefs().getStringSet(IN_APP_LOG_TIMESTAMPS_KEY, emptySet())
            .orEmpty().toMutableSet()
        cur.add(timestampMs.toString())
        // Bound the set so it can't grow forever; windows only matter
        // for recent sessions anyway (auto-credit scans 48h).
        val trimmed = cur.mapNotNull { it.toLongOrNull() }
            .sortedDescending().take(200).map { it.toString() }.toSet()
        prefs().edit().putStringSet(IN_APP_LOG_TIMESTAMPS_KEY, trimmed).apply()
    }

    /** All recorded in-app credit-granting log timestamps (epoch ms). */
    fun getInAppExerciseLogTimestamps(): List<Long> =
        prefs().getStringSet(IN_APP_LOG_TIMESTAMPS_KEY, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }

    /**
     * True when [sessionStartMs, sessionEndMs] overlaps any in-app log
     * window — i.e. the session is (probably) the same workout the user
     * already logged in the app. Overlap test: sessionStart <= winEnd &&
     * sessionEnd >= winStart.
     */
    fun hasOverlappingInAppLog(sessionStartMs: Long, sessionEndMs: Long): Boolean =
        getInAppExerciseLogTimestamps().any { ts ->
            val winStart = ts - IN_APP_LOG_WINDOW_MS
            sessionStartMs <= ts && sessionEndMs >= winStart
        }

    private fun encode(list: List<LoggedWorkout>): String =
        list.joinToString(";") { w ->
            // template names are app-controlled; strip delimiters defensively
            val safeName = w.templateName.replace("|", "").replace(";", "")
            val base = "$safeName|${w.muscles.joinToString(",") { it.name }}|${w.timestamp}"
            // Per-exercise detail rides as extra fields; entries without
            // detail keep the legacy 3-part shape, which decode still reads.
            if (w.exerciseName == null) base
            else {
                val safeExercise = w.exerciseName.replace("|", "").replace(";", "")
                val sets = w.sets?.toString().orEmpty()
                val reps = w.reps?.toString().orEmpty()
                val weight = w.weightLb?.toString().orEmpty()
                "$base|$safeExercise|$sets|$reps|$weight"
            }
        }

    private fun decode(date: LocalDate, raw: String?): List<LoggedWorkout> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size != 3 && parts.size != 7) return@mapNotNull null
            val muscles = parts[1].split(",").mapNotNull { name ->
                try { MuscleGroup.valueOf(name) } catch (e: Exception) { null }
            }.toSet()
            if (muscles.isEmpty()) return@mapNotNull null
            if (parts.size == 3) {
                LoggedWorkout(date, parts[0], muscles, parts[2].toLongOrNull() ?: 0L)
            } else {
                LoggedWorkout(
                    date, parts[0], muscles, parts[2].toLongOrNull() ?: 0L,
                    exerciseName = parts[3].ifBlank { null },
                    sets = parts[4].toIntOrNull(),
                    reps = parts[5].toIntOrNull(),
                    weightLb = parts[6].toFloatOrNull()
                )
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "fitness_log_prefs"
        private const val INDEX_KEY = "logged_dates_index"
        private const val IMPORTED_SESSIONS_KEY = "imported_hc_sessions"
        private const val IN_APP_LOG_TIMESTAMPS_KEY = "in_app_exercise_log_timestamps"

        /**
         * Fitness (2026-10-08): every credit-granting in-app log is treated
         * as covering a workout session window of this length ending at the
         * moment it was logged. 2h comfortably covers a gym session logged
         * right after (or during) the workout.
         */
        const val IN_APP_LOG_WINDOW_MS = 2L * 60L * 60L * 1000L
    }
}
