package com.example.vitalwearclonev1.monster

import kotlin.math.max

/**
 * The DIM "life expectancy" system: a Digimon that isn't cared for properly
 * burns through its lifespan and eventually self-deletes (dies).
 *
 * Care mistakes, per the design:
 *  - Too many battles in a row with no real-world activity (overworked)
 *  - Going too long with no activity at all (neglected)
 * Every battle also costs a little lifespan (wear and tear).
 *
 * Pure Kotlin — no Android dependencies — so the watch module can share it.
 * All tuning knobs live in [CareTuning]; persistence lives in
 * PhoneMonsterManager, which converts to/from [CareState].
 */
data class CareState(
    val maxLifespanHours: Double,
    val lifespanHoursRemaining: Double,
    val careMistakes: Int,
    val consecutiveBattles: Int,
    /** Epoch day (System.currentTimeMillis() / 86400000) of the last active day. */
    val lastActiveDay: Long
)

object CareTuning {
    /** Base lifespan granted per stage, in hours. */
    const val BASE_LIFESPAN_HOURS = 336.0 // 14 days
    /** Extra lifespan per stage index (higher stages live a bit longer). */
    const val LIFESPAN_PER_STAGE_HOURS = 24.0
    /** Battles in a row with no activity before it counts as overwork. */
    const val MAX_CONSECUTIVE_BATTLES = 5
    /** Hours with no activity before it counts as neglect. */
    const val INACTIVITY_LIMIT_HOURS = 36.0
    /** Lifespan burned per care mistake, in hours. */
    const val MISTAKE_PENALTY_HOURS = 24.0
    /** Lifespan burned per battle (wear and tear), in hours. */
    const val BATTLE_COST_HOURS = 0.5
    /** Steps that earn one Vital Point. */
    const val STEPS_PER_VITAL_POINT = 500
    /** Daily steps that count as "looked after" for the day. */
    const val DAILY_STEP_GOAL = 4000
}

object CareManager {

    data class TickResult(
        val state: CareState,
        val died: Boolean,
        val newMistakes: Int,
        val warnings: List<String>
    )

    fun initialForStage(stage: Int, todayEpochDay: Long): CareState {
        val max = CareTuning.BASE_LIFESPAN_HOURS + stage * CareTuning.LIFESPAN_PER_STAGE_HOURS
        return CareState(
            maxLifespanHours = max,
            lifespanHoursRemaining = max,
            careMistakes = 0,
            consecutiveBattles = 0,
            lastActiveDay = todayEpochDay
        )
    }

    /**
     * Advance the clock by [elapsedHours] of real time. Neglect is penalized
     * at most once per call so a long gap (phone off for days) stings but
     * doesn't insta-kill — callers should also clamp elapsed time.
     */
    fun tickTime(state: CareState, elapsedHours: Double, todayEpochDay: Long): TickResult {
        if (elapsedHours <= 0) return TickResult(state, false, 0, emptyList())
        var s = state.copy(lifespanHoursRemaining = state.lifespanHoursRemaining - elapsedHours)
        var mistakes = 0
        val warnings = mutableListOf<String>()
        val hoursSinceActive = (todayEpochDay - state.lastActiveDay) * 24.0
        if (hoursSinceActive >= CareTuning.INACTIVITY_LIMIT_HOURS && s.lifespanHoursRemaining > 0) {
            s = applyMistake(s)
            mistakes++
            warnings.add("Your Digimon is feeling neglected — get some activity in!")
        }
        if (s.lifespanHoursRemaining <= 0) {
            return TickResult(s.copy(lifespanHoursRemaining = 0.0), true, mistakes, warnings)
        }
        return TickResult(s, false, mistakes, warnings)
    }

    /** Record one battle. Wear-and-tear always costs a little life; overwork is a mistake. */
    fun recordBattle(state: CareState): TickResult {
        var s = state.copy(
            consecutiveBattles = state.consecutiveBattles + 1,
            lifespanHoursRemaining = state.lifespanHoursRemaining - CareTuning.BATTLE_COST_HOURS
        )
        var mistakes = 0
        val warnings = mutableListOf<String>()
        if (s.consecutiveBattles > CareTuning.MAX_CONSECUTIVE_BATTLES) {
            s = applyMistake(s)
            mistakes++
            warnings.add("Too many battles without rest — your Digimon is overworked!")
        }
        val died = s.lifespanHoursRemaining <= 0
        return TickResult(if (died) s.copy(lifespanHoursRemaining = 0.0) else s, died, mistakes, warnings)
    }

    /**
     * Record real-world activity. Hitting the daily step goal resets the
     * overwork counter and marks today as an active day.
     */
    fun recordActivity(state: CareState, stepsToday: Int, todayEpochDay: Long): CareState {
        if (stepsToday < CareTuning.DAILY_STEP_GOAL) return state
        return state.copy(consecutiveBattles = 0, lastActiveDay = todayEpochDay)
    }

    private fun applyMistake(s: CareState): CareState = s.copy(
        careMistakes = s.careMistakes + 1,
        lifespanHoursRemaining = max(0.0, s.lifespanHoursRemaining - CareTuning.MISTAKE_PENALTY_HOURS)
    )
}
