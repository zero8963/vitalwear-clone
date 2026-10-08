package com.example.vitalwearclonev1.fitness

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Fitness (2026-10-07): pure recovery logic — no Android, no storage.
 * Hours since a muscle was last trained drive its state; the Coach tab
 * turns states into "hit these" / "let these rest" suggestions.
 */
enum class RecoveryState { FRIED, RECOVERING, READY, NEVER }

data class MuscleStatus(
    val muscle: MuscleGroup,
    val state: RecoveryState,
    val hoursSinceTrained: Long?,
    val daysSinceTrained: Long?
)

data class CoachSuggestion(val title: String, val detail: String)

object RecoveryEngine {

    /** Trained <24h ago: muscles need ~48h before the same group goes hard again. */
    private const val FRIED_HOURS = 24L

    /** 24-72h: recovering — light work ok, heavy work waits. */
    private const val RECOVERED_HOURS = 72L

    /** "Hit these" only lists READY muscles idle this long — avoids nagging
     *  about a group trained 3 days ago that simply isn't due yet. */
    private const val DUE_DAYS = 5L

    fun getStatuses(
        store: WorkoutLogStore,
        today: LocalDate = LocalDate.now()
    ): List<MuscleStatus> {
        return MuscleGroup.values().map { muscle ->
            val last = store.getLastTrainedDate(muscle)
            if (last == null) {
                MuscleStatus(muscle, RecoveryState.NEVER, null, null)
            } else {
                val hours = ChronoUnit.HOURS.between(last.atStartOfDay(), today.atStartOfDay())
                val days = ChronoUnit.DAYS.between(last, today)
                val state = when {
                    hours < FRIED_HOURS -> RecoveryState.FRIED
                    hours < RECOVERED_HOURS -> RecoveryState.RECOVERING
                    else -> RecoveryState.READY
                }
                MuscleStatus(muscle, state, hours, days)
            }
        }
    }

    /**
     * Returns (hitThese, letRest).
     * hitThese: never trained, or READY and idle 5+ days.
     * letRest: FRIED, or anything trained less than 48h ago.
     */
    fun getSuggestions(statuses: List<MuscleStatus>): Pair<List<CoachSuggestion>, List<CoachSuggestion>> {
        val hit = statuses
            .filter { s ->
                s.state == RecoveryState.NEVER ||
                    (s.state == RecoveryState.READY && (s.daysSinceTrained ?: 0L) >= DUE_DAYS)
            }
            .sortedByDescending { it.daysSinceTrained ?: Long.MAX_VALUE }
            .map { s ->
                val whenText = if (s.daysSinceTrained == null) "never logged"
                else if (s.daysSinceTrained == 1L) "yesterday"
                else "${s.daysSinceTrained} days ago"
                CoachSuggestion(
                    s.muscle.displayName,
                    "Last trained $whenText — good to hit today."
                )
            }
        val rest = statuses
            .filter { s ->
                s.state == RecoveryState.FRIED ||
                    (s.hoursSinceTrained != null && s.hoursSinceTrained < 48L)
            }
            .sortedBy { it.hoursSinceTrained ?: 0L }
            .map { s ->
                val whenText = if ((s.daysSinceTrained ?: 0L) <= 1L) "recently" else "${s.daysSinceTrained} days ago"
                CoachSuggestion(
                    s.muscle.displayName,
                    "Trained $whenText — let it recover."
                )
            }
        return Pair(hit, rest)
    }

    /** One-line recommendation for the top of the Coach tab. */
    fun getHeadline(statuses: List<MuscleStatus>): String {
        val (hit, rest) = getSuggestions(statuses)
        val top = hit.firstOrNull()
        return when {
            top != null -> {
                val idle = statuses.firstOrNull { it.muscle.displayName == top.title }
                    ?.daysSinceTrained
                if (idle == null) "${top.title} hasn't been logged yet — today's the day."
                else "${top.title} is recovered and hasn't been hit in $idle days — today's the day."
            }
            rest.isNotEmpty() ->
                "Everything's still recovering — good day for a walk or mobility work."
            else -> "Log your first workout to get personalized suggestions."
        }
    }
}
