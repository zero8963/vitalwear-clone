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
    val timestamp: Long
)

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
     * Fitness (2026-10-08): dates on which in-app exercises were logged
     * (workout picker or import card). The Health Connect auto-credit
     * path skips these days — the in-app log takes precedence, so the
     * same workout is never credited twice.
     */
    fun markInAppExercisesLogged(date: LocalDate) {
        val cur = prefs().getStringSet(IN_APP_EXERCISE_DATES_KEY, emptySet())
            .orEmpty().toMutableSet()
        cur.add(date.format(dateFmt))
        prefs().edit().putStringSet(IN_APP_EXERCISE_DATES_KEY, cur).apply()
    }

    fun hasInAppExercises(date: LocalDate): Boolean =
        prefs().getStringSet(IN_APP_EXERCISE_DATES_KEY, emptySet()).orEmpty()
            .contains(date.format(dateFmt))

    private fun encode(list: List<LoggedWorkout>): String =
        list.joinToString(";") { w ->
            // template names are app-controlled; strip delimiters defensively
            val safeName = w.templateName.replace("|", "").replace(";", "")
            "$safeName|${w.muscles.joinToString(",") { it.name }}|${w.timestamp}"
        }

    private fun decode(date: LocalDate, raw: String?): List<LoggedWorkout> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size != 3) return@mapNotNull null
            val muscles = parts[1].split(",").mapNotNull { name ->
                try { MuscleGroup.valueOf(name) } catch (e: Exception) { null }
            }.toSet()
            if (muscles.isEmpty()) return@mapNotNull null
            LoggedWorkout(date, parts[0], muscles, parts[2].toLongOrNull() ?: 0L)
        }
    }

    companion object {
        private const val PREFS_NAME = "fitness_log_prefs"
        private const val INDEX_KEY = "logged_dates_index"
        private const val IMPORTED_SESSIONS_KEY = "imported_hc_sessions"
        private const val IN_APP_EXERCISE_DATES_KEY = "in_app_exercise_dates"
    }
}
