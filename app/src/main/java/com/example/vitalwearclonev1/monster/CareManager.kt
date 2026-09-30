package com.example.vitalwearclonev1.monster

/**
 * The DIM care system: poor care racks up care mistakes (tracked, and they
 * trigger warnings) but can never kill a Digimon on its own — there is no
 * lifespan clock anymore. The only death is losing a battle while in
 * critical condition (see below).
 *
 * Care mistakes, per the design:
 *  - Too many battles in a row with no real-world activity (overworked)
 *  - Going too long with no activity at all (neglected)
 *
 * Loss-streak / critical system:
 *  - Straight losses are tracked. At 3 the poor-condition skull shows.
 *  - At 5 straight losses the Digimon goes CRITICAL: a 30-minute healing
 *    window opens (persisted as [CareState.criticalRemainingMs]).
 *  - Resting out the full window heals it; each completed exercise shaves
 *    15 minutes off; winning a battle heals it instantly.
 *  - Losing a battle while critical kills it on the spot (self-delete).
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
    val lastActiveDay: Long,
    /** Straight battle losses in a row. Resets on any win or full heal. */
    val consecutiveLosses: Int = 0,
    /**
     * Critical-condition healing time left, in milliseconds.
     * Greater than zero means the Digimon is CRITICAL. Ticks down with real
     * elapsed time in [CareManager.tickTime]; frozen automatically whenever
     * the caller stops ticking (e.g. a stored/sleeping partner).
     */
    val criticalRemainingMs: Long = 0L
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
    /** Straight losses before the poor-condition skull warning appears. */
    const val SKULL_WARNING_LOSSES = 3
    /** Straight losses that push the Digimon into CRITICAL condition. */
    const val CRITICAL_LOSS_STREAK = 5
    /** Critical healing window: rest this long and it recovers. */
    const val CRITICAL_HEAL_WINDOW_MS = 30 * 60 * 1000L
    /** Each completed exercise shaves this much off the critical timer. */
    const val EXERCISE_HEAL_MS = 15 * 60 * 1000L
    /** Vital Points earned per completed workout (bracelet evolution logic). */
    const val WORKOUT_VITAL_POINTS = 100
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

    /** True while the Digimon is in critical condition (healing window open). */
    fun isCritical(state: CareState): Boolean = state.criticalRemainingMs > 0

    /**
     * True when the poor-condition skull should show: from 3 straight losses
     * as a warning, and it stays up through critical.
     */
    fun showSkull(state: CareState): Boolean =
        state.consecutiveLosses >= CareTuning.SKULL_WARNING_LOSSES || isCritical(state)

    /** Same check from raw values, for UI holding a MonsterState. */
    fun showSkull(consecutiveLosses: Int, criticalRemainingMs: Long): Boolean =
        consecutiveLosses >= CareTuning.SKULL_WARNING_LOSSES || criticalRemainingMs > 0

    /** "24:37" style rendering of the critical countdown for UI. */
    fun formatCriticalMs(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    /**
     * Advance the clock by [elapsedHours] of real time. Neglect is penalized
     * at most once per call so a long gap (phone off for days) only warns —
     * it can never kill. Callers should also clamp elapsed time.
     *
     * Rest also heals critical: the 30-minute window ticks down with real
     * elapsed time, and hitting zero recovers the Digimon fully.
     */
    fun tickTime(state: CareState, elapsedHours: Double, todayEpochDay: Long): TickResult {
        if (elapsedHours <= 0) return TickResult(state, false, 0, emptyList())
        // No lifespan clock: time passing never harms the Digimon. Time only
        // heals critical and watches for neglect (warnings only, never death).
        var s = state
        var mistakes = 0
        val warnings = mutableListOf<String>()
        if (s.criticalRemainingMs > 0) {
            val left = s.criticalRemainingMs - (elapsedHours * 3600000.0).toLong()
            s = if (left <= 0) {
                warnings.add("Your Digimon has recovered from critical condition!")
                s.copy(consecutiveLosses = 0, criticalRemainingMs = 0L)
            } else {
                s.copy(criticalRemainingMs = left)
            }
        }
        val hoursSinceActive = (todayEpochDay - state.lastActiveDay) * 24.0
        if (hoursSinceActive >= CareTuning.INACTIVITY_LIMIT_HOURS) {
            s = applyMistake(s)
            mistakes++
            warnings.add("Your Digimon is feeling neglected — get some activity in!")
        }
        return TickResult(s, false, mistakes, warnings)
    }

    /**
     * Record one finished battle. Overwork (too many battles with no
     * real-world activity) is a care mistake that warns. Wins reset the loss
     * streak and heal critical instantly; losses build the streak — the 5th
     * straight loss opens the critical window, and losing while critical
     * kills on the spot UNLESS the Digimon has a prior evolution to bounce
     * back to (de-digivolve safety net, handled by PhoneMonsterManager —
     * trophies and wins reset, another 6-loss chain kills). Nothing else here
     * can kill.
     */
    fun recordBattle(state: CareState, won: Boolean): TickResult {
        var s = state.copy(
            consecutiveBattles = state.consecutiveBattles + 1
        )
        var mistakes = 0
        val warnings = mutableListOf<String>()
        if (s.consecutiveBattles > CareTuning.MAX_CONSECUTIVE_BATTLES) {
            s = applyMistake(s)
            mistakes++
            warnings.add("Too many battles without rest — your Digimon is overworked!")
        }
        if (won) {
            if (s.consecutiveLosses > 0 || s.criticalRemainingMs > 0) {
                warnings.add("Victory! Your Digimon's condition is back to normal.")
            }
            s = s.copy(consecutiveLosses = 0, criticalRemainingMs = 0L)
        } else {
            if (s.criticalRemainingMs > 0) {
                warnings.add("Your Digimon lost while in critical condition…")
                return TickResult(s, died = true, mistakes, warnings)
            }
            val losses = s.consecutiveLosses + 1
            s = if (losses >= CareTuning.CRITICAL_LOSS_STREAK) {
                warnings.add("CRITICAL CONDITION! Rest 30 minutes or exercise to heal — losing another battle will kill your Digimon!")
                s.copy(consecutiveLosses = losses, criticalRemainingMs = CareTuning.CRITICAL_HEAL_WINDOW_MS)
            } else {
                if (losses >= CareTuning.SKULL_WARNING_LOSSES) {
                    warnings.add("Your Digimon is in poor condition ($losses straight losses) — win a battle soon!")
                }
                s.copy(consecutiveLosses = losses)
            }
        }
        // Battles never kill on their own now — only losing while critical does,
        // and that returns early above.
        return TickResult(s, false, mistakes, warnings)
    }

    /**
     * A completed exercise shaves 15 minutes off the critical healing timer;
     * two exercises heal it fully. No-op when the Digimon isn't critical.
     */
    fun recordExercise(state: CareState): CareState {
        if (state.criticalRemainingMs <= 0) return state
        val left = state.criticalRemainingMs - CareTuning.EXERCISE_HEAL_MS
        return if (left <= 0) state.copy(consecutiveLosses = 0, criticalRemainingMs = 0L)
        else state.copy(criticalRemainingMs = left)
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
        careMistakes = s.careMistakes + 1
    )
}
