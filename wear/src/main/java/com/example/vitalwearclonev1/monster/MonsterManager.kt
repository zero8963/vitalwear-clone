package com.example.vitalwearclonev1.monster

import android.content.Context
import android.content.SharedPreferences
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.card.DimCardAdapter
import com.example.vitalwearclonev1.card.DigimonBaseStats
import timber.log.Timber
import com.github.cfogrady.vb.dim.card.BemCard
import com.github.cfogrady.vb.dim.card.Card
import com.github.cfogrady.vb.dim.sprite.SpriteData

class MonsterManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("monster_prefs", Context.MODE_PRIVATE)

    // One-shot guard for the base-stat migration in getMonster (per process).
    private var baseStatsMigrationDone = false

    companion object {
        /** DIM stats are uint16 (up to 65535). Dividing by 100 maps them onto
         *  the app's battle balance (the old flat-500-HP scale) so training
         *  bonuses stay the main progression and the card's species differences
         *  survive proportionally. */
        // Two card families, two scales: BEMs store stats in the thousands,
        // classic DIMs in single digits. Each is normalized into the same
        // battle range so training stays dominant on both.
        private const val BEM_STAT_SCALE = 100
        private const val DIM_STAT_SCALE = 2
        /** Bump when seeding/normalization changes; triggers a re-seed. */
        private const val BASE_STATS_VERSION = 3
    }

    data class MonsterState(
        val cardName: String,
        val characterId: Int, // Index in CharacterStats
        val stage: Int, // 0=Egg, 1=Phase1, etc.
        val steps: Int = 0,
        val calories: Int = 0,
        val currentWins: Int = 0,
        val attribute: Int = 0,
        val mood: Int = 0,
        val bp: Int = 0,
        val sp: Int = 0,
        val winRatio: Int = 0,
        val trophies: Int = 0,
        val timeAlive: Long = 0, // In seconds
        val evolutionTime: Long = 43200, // 12 hours for testing
        val attackBonus: Int = 0,
        val healthBonus: Int = 0,
        val speedBonus: Int = 0,
        val defenseBonus: Int = 0,
        val isEvolutionPaused: Boolean = false,
        val adventureLevel: Int = 0,
        val adventureSteps: Int = 0,
        val isAdventureMode: Boolean = false,
        val is24HourFormat: Boolean = true,
        val losses: Int = 0,
        val vitalPoints: Int = 0,
        val stageBattles: Int = 0,
        val stageWins: Int = 0,
        val stageVitalPoints: Int = 0,
        val stageTrophies: Int = 0,
        val baseHp: Int = 500,
        val baseAp: Int = 0,
        val maxLifespanHours: Double = 0.0,
        val lifespanHoursRemaining: Double = 0.0,
        val careMistakes: Int = 0,
        val consecutiveBattles: Int = 0,
        val lastActiveDay: Long = 0,
        val lastCareTick: Long = 0,
        val lastSyncedSteps: Int = 0,
        val consecutiveLosses: Int = 0,
        val criticalRemainingMs: Long = 0L
    )

    fun getCurrentMonster(): MonsterState? = getMonster("current_")

    // ----- Monster storage (sleep) -----
    // The sleeping partner lives under the "stored_" key prefix and is fully
    // frozen: the background service only ages/evolves the active ("current_")
    // monster, so a stored Digimon gains no steps, no time, and no evolution
    // progress while it sleeps.

    fun getStoredMonster(): MonsterState? = getMonster("stored_")

    fun hasStoredMonster(): Boolean = prefs.contains("stored_card")

    fun getStoredAt(): Long = prefs.getLong("stored_stored_at", 0L)

    /** Puts the active partner to sleep in storage, freeing the active slot. */
    fun storeCurrentMonster() {
        val active = getCurrentMonster() ?: return
        writeMonster("stored_", active)
        prefs.edit().putLong("stored_stored_at", System.currentTimeMillis()).apply()
        clearSlot("current_")
        Timber.d("Active monster stored (sleeping): ${active.cardName} #${active.characterId}")
    }

    /** Wakes the stored partner: swaps it with the active one, if any. */
    fun wakeStoredMonster() {
        val stored = getStoredMonster() ?: return
        val active = getCurrentMonster()
        if (active != null) {
            writeMonster("stored_", active)
            prefs.edit().putLong("stored_stored_at", System.currentTimeMillis()).apply()
        } else {
            clearSlot("stored_")
        }
        writeMonster("current_", stored)
        Timber.d("Stored monster woken: ${stored.cardName} #${stored.characterId}")
    }

    /** Releases the stored partner without waking it. */
    fun clearStoredMonster() {
        clearSlot("stored_")
        prefs.edit().remove("stored_stored_at").apply()
    }

    private fun getMonster(prefix: String): MonsterState? {
        val cardName = prefs.getString(prefix + "card", null) ?: return null
        val characterId = prefs.getInt(prefix + "character_id", -1)
        if (characterId == -1) return null
        // Base-stat migration (2026-09-23): v0 = pre-DIM-stats save
        // (500/0 defaults); v1 = raw card values (up to 65535, ~16k damage).
        // v2 normalizes. Active monster only, never the frozen stored one.
        if (prefix == "current_" && !baseStatsMigrationDone) {
            baseStatsMigrationDone = true
            migrateBaseStats(cardName, characterId)
        }
        return MonsterState(
            cardName,
            characterId,
            prefs.getInt(prefix + "stage", 0),
            prefs.getInt(prefix + "steps", 0),
            prefs.getInt(prefix + "calories", 0),
            prefs.getInt(prefix + "wins", 0),
            prefs.getInt(prefix + "attribute", 0),
            prefs.getInt(prefix + "mood", 0),
            prefs.getInt(prefix + "bp", 0),
            prefs.getInt(prefix + "sp", 0),
            prefs.getInt(prefix + "win_ratio", 0),
            prefs.getInt(prefix + "trophies", 0),
            prefs.getLong(prefix + "time_alive", 0),
            prefs.getLong(prefix + "evolution_time", 43200),
            prefs.getInt(prefix + "attack_bonus", 0),
            prefs.getInt(prefix + "health_bonus", 0),
            prefs.getInt(prefix + "speed_bonus", 0),
            prefs.getInt(prefix + "defense_bonus", 0),
            prefs.getBoolean(prefix + "evolution_paused", false),
            prefs.getInt(prefix + "adv_level", 0),
            prefs.getInt(prefix + "adv_steps", 0),
            prefs.getBoolean(prefix + "adv_mode", false),
            prefs.getBoolean(prefix + "clock_24h", true),
            prefs.getInt(prefix + "losses", 0),
            prefs.getInt(prefix + "vital_points", 0),
            prefs.getInt(prefix + "stage_battles", 0),
            prefs.getInt(prefix + "stage_wins", 0),
            prefs.getInt(prefix + "stage_vital_points", 0),
            prefs.getInt(prefix + "stage_trophies", 0),
            prefs.getInt(prefix + "base_hp", 500),
            prefs.getInt(prefix + "base_ap", 0),
            prefs.getFloat(prefix + "max_lifespan", 0f).toDouble(),
            prefs.getFloat(prefix + "lifespan_remaining", 0f).toDouble(),
            prefs.getInt(prefix + "care_mistakes", 0),
            prefs.getInt(prefix + "care_consecutive_battles", 0),
            prefs.getLong(prefix + "last_active_day", 0L),
            prefs.getLong(prefix + "last_care_tick", 0L),
            prefs.getInt(prefix + "last_synced_steps", 0),
            prefs.getInt(prefix + "consecutive_losses", 0),
            prefs.getLong(prefix + "critical_remaining_ms", 0L)
        )
    }

    private fun writeMonster(prefix: String, state: MonsterState) {
        prefs.edit()
            .putString(prefix + "card", state.cardName)
            .putInt(prefix + "character_id", state.characterId)
            .putInt(prefix + "stage", state.stage)
            .putInt(prefix + "steps", state.steps)
            .putInt(prefix + "calories", state.calories)
            .putInt(prefix + "wins", state.currentWins)
            .putInt(prefix + "attribute", state.attribute)
            .putInt(prefix + "mood", state.mood)
            .putInt(prefix + "bp", state.bp)
            .putInt(prefix + "sp", state.sp)
            .putInt(prefix + "win_ratio", state.winRatio)
            .putInt(prefix + "trophies", state.trophies)
            .putLong(prefix + "time_alive", state.timeAlive)
            .putLong(prefix + "evolution_time", state.evolutionTime)
            .putInt(prefix + "attack_bonus", state.attackBonus)
            .putInt(prefix + "health_bonus", state.healthBonus)
            .putInt(prefix + "speed_bonus", state.speedBonus)
            .putInt(prefix + "defense_bonus", state.defenseBonus)
            .putBoolean(prefix + "evolution_paused", state.isEvolutionPaused)
            .putInt(prefix + "adv_level", state.adventureLevel)
            .putInt(prefix + "adv_steps", state.adventureSteps)
            .putBoolean(prefix + "adv_mode", state.isAdventureMode)
            .putBoolean(prefix + "clock_24h", state.is24HourFormat)
            .putInt(prefix + "losses", state.losses)
            .putInt(prefix + "vital_points", state.vitalPoints)
            .putInt(prefix + "stage_battles", state.stageBattles)
            .putInt(prefix + "stage_wins", state.stageWins)
            .putInt(prefix + "stage_vital_points", state.stageVitalPoints)
            .putInt(prefix + "stage_trophies", state.stageTrophies)
            .putInt(prefix + "base_hp", state.baseHp)
            .putInt(prefix + "base_ap", state.baseAp)
            .putFloat(prefix + "max_lifespan", state.maxLifespanHours.toFloat())
            .putFloat(prefix + "lifespan_remaining", state.lifespanHoursRemaining.toFloat())
            .putInt(prefix + "care_mistakes", state.careMistakes)
            .putInt(prefix + "care_consecutive_battles", state.consecutiveBattles)
            .putLong(prefix + "last_active_day", state.lastActiveDay)
            .putLong(prefix + "last_care_tick", state.lastCareTick)
            .putInt(prefix + "last_synced_steps", state.lastSyncedSteps)
            .putInt(prefix + "consecutive_losses", state.consecutiveLosses)
            .putLong(prefix + "critical_remaining_ms", state.criticalRemainingMs)
            .apply()
    }

    private fun clearSlot(prefix: String) {
        prefs.edit()
            .remove(prefix + "card")
            .remove(prefix + "character_id")
            .remove(prefix + "stage")
            .remove(prefix + "steps")
            .remove(prefix + "calories")
            .remove(prefix + "wins")
            .remove(prefix + "attribute")
            .remove(prefix + "mood")
            .remove(prefix + "bp")
            .remove(prefix + "sp")
            .remove(prefix + "win_ratio")
            .remove(prefix + "trophies")
            .remove(prefix + "time_alive")
            .remove(prefix + "evolution_time")
            .remove(prefix + "attack_bonus")
            .remove(prefix + "health_bonus")
            .remove(prefix + "speed_bonus")
            .remove(prefix + "defense_bonus")
            .remove(prefix + "adv_level")
            .remove(prefix + "adv_steps")
            .remove(prefix + "adv_mode")
            .remove(prefix + "clock_24h")
            .remove(prefix + "losses")
            .remove(prefix + "vital_points")
            .remove(prefix + "stage_battles")
            .remove(prefix + "stage_wins")
            .remove(prefix + "stage_vital_points")
            .remove(prefix + "stage_trophies")
            .remove(prefix + "base_hp")
            .remove(prefix + "base_ap")
            .remove(prefix + "max_lifespan")
            .remove(prefix + "lifespan_remaining")
            .remove(prefix + "care_mistakes")
            .remove(prefix + "care_consecutive_battles")
            .remove(prefix + "last_active_day")
            .remove(prefix + "last_care_tick")
            .remove(prefix + "last_synced_steps")
            .remove(prefix + "consecutive_losses")
            .remove(prefix + "critical_remaining_ms")
            .remove(prefix + "last_neglect_penalty")
            .remove(prefix + "evolution_paused")
            .remove(prefix + "adv_level")
            .remove(prefix + "adv_steps")
            .remove(prefix + "adv_mode")
            .remove(prefix + "clock_24h")
            .apply()
    }

    fun setCurrentMonster(
        cardName: String, 
        characterId: Int, 
        stage: Int = 0,
        atk: Int = 0,
        hp: Int = 0,
        spd: Int = 0,
        def: Int = 0,
        attribute: Int = 0,
        mood: Int = 0,
        bp: Int = 0,
        sp: Int = 0,
        winRatio: Int = 0,
        trophies: Int = 0
    ) {
        prefs.edit()
            .putString("current_card", cardName)
            .putInt("current_character_id", characterId)
            .putInt("current_stage", stage)
            .putInt("current_steps", 0)
            .putInt("current_calories", 0)
            .putInt("current_wins", 0)
            .putInt("current_attribute", attribute)
            .putInt("current_mood", mood)
            .putInt("current_bp", bp)
            .putInt("current_sp", sp)
            .putInt("current_win_ratio", winRatio)
            .putInt("current_trophies", trophies)
            .putLong("current_time_alive", 0)
            .putLong("current_evolution_time", 43200) // 12 hours
            .putInt("current_attack_bonus", atk)
            .putInt("current_health_bonus", hp)
            .putInt("current_speed_bonus", spd)
            .putInt("current_defense_bonus", def)
            .putBoolean("current_evolution_paused", false)
            .putBoolean("current_is_expired", false)
            .putInt("current_adv_level", 0)
            .putInt("current_adv_steps", 0)
            .putBoolean("current_adv_mode", false)
            .putInt("current_losses", 0)
            .putInt("current_vital_points", 0)
            .putInt("current_stage_battles", 0)
            .putInt("current_stage_wins", 0)
            .putInt("current_stage_vital_points", 0)
            .putInt("current_stage_trophies", 0)
            .putInt("current_last_synced_steps", 0)
            .putLong("current_last_neglect_penalty", 0L)
            .apply()

        // Fresh partner: seed the care clock and pull the DIM-baked base
        // stats for this form.
        val today = System.currentTimeMillis() / 86400000L
        persistCare("current_", CareManager.initialForStage(stage, today))
        prefs.edit().putLong("current_last_care_tick", System.currentTimeMillis()).apply()
        applyCardBaseStats(cardName, characterId)
    }

    fun addTrainingBonus(exercise: String, multiplier: Float = 1.0f) {
        val current = getCurrentMonster() ?: return
        val edit = prefs.edit()
        val random = kotlin.random.Random(System.currentTimeMillis())
        val base = (50 * multiplier).toInt()

        // Randomly boost 2-4 stats
        val statsToBoost = listOf("ATK", "HP", "SPD", "DEF").shuffled().take(random.nextInt(2, 5))
        
        if (statsToBoost.contains("ATK")) edit.putInt("current_attack_bonus", current.attackBonus + base + random.nextInt(0, 50))
        if (statsToBoost.contains("HP")) edit.putInt("current_health_bonus", current.healthBonus + base + random.nextInt(0, 50))
        if (statsToBoost.contains("SPD")) edit.putInt("current_speed_bonus", current.speedBonus + base + random.nextInt(0, 50))
        if (statsToBoost.contains("DEF")) edit.putInt("current_defense_bonus", current.defenseBonus + base + random.nextInt(0, 50))

        // Add calories for completion
        edit.putInt("current_calories", current.calories + base)
        edit.apply()
    }

    fun updateStats(deltaSteps: Int, deltaCalories: Int, addSeconds: Long = 0) {
        val current = getCurrentMonster() ?: return
        prefs.edit()
            .putInt("current_steps", current.steps + deltaSteps)
            .putInt("current_calories", current.calories + deltaCalories)
            .putLong("current_time_alive", current.timeAlive + addSeconds)
            .apply()
    }

    fun syncHealthData(totalSteps: Int, totalCalories: Int) {
        val current = getCurrentMonster() ?: return
        
        // We trust the health sync source (phone) to provide the absolute current day totals.
        // If the new values are significantly lower, it indicates a day reset or a clean sync.
        // We remove the coerceAtLeast(current.steps) to allow 24-hour resets to function.
        
        prefs.edit()
            .putInt("current_steps", totalSteps)
            .putInt("current_calories", totalCalories)
            .apply()
        Timber.d("Monster stats synced: Steps=$totalSteps, Cals=$totalCalories")
    }

    fun toggleEvolutionPause() {
        val current = getCurrentMonster() ?: return
        prefs.edit()
            .putBoolean("current_evolution_paused", !current.isEvolutionPaused)
            .apply()
    }

    fun toggleAdventureMode() {
        val current = getCurrentMonster() ?: return
        prefs.edit()
            .putBoolean("current_adv_mode", !current.isAdventureMode)
            .apply()
    }

    fun toggleClockFormat() {
        val current = getCurrentMonster() ?: return
        prefs.edit()
            .putBoolean("current_clock_24h", !current.is24HourFormat)
            .apply()
    }

    fun updateAdventureSteps(deltaSteps: Int) {
        val current = getCurrentMonster() ?: return
        prefs.edit()
            .putInt("current_adv_steps", current.adventureSteps + deltaSteps)
            .apply()
    }

    fun resetAdventureSteps() {
        prefs.edit()
            .putInt("current_adv_steps", 0)
            .apply()
    }

    fun completeAdventureLevel() {
        val current = getCurrentMonster() ?: return
        prefs.edit()
            .putInt("current_adv_level", current.adventureLevel + 1)
            .putInt("current_adv_steps", 0)
            .apply()
    }

    /** Legacy win hook — now routes through the full battle recorder. */
    fun addWin() {
        recordBattleResult(true)
    }

    /**
     * Records a finished battle — win OR loss. Updates lifetime + per-stage
     * counters, recomputes win ratio, and charges the care system.
     * @return true if the Digimon died of poor care during this battle.
     */
    fun recordBattleResult(won: Boolean): Boolean {
        val current = getCurrentMonster() ?: return false
        val careTick = CareManager.recordBattle(toCareState(current), won)
        persistCare("current_", careTick.state)

        val newWins = current.currentWins + if (won) 1 else 0
        val newLosses = current.losses + if (won) 0 else 1
        val total = newWins + newLosses
        val winRatio = if (total > 0) newWins * 100 / total else 0

        prefs.edit()
            .putInt("current_wins", newWins)
            .putInt("current_losses", newLosses)
            .putInt("current_win_ratio", winRatio)
            .putInt("current_stage_battles", current.stageBattles + 1)
            .putInt("current_stage_wins", current.stageWins + if (won) 1 else 0)
            .apply()

        for (warning in careTick.warnings) Timber.w(warning)
        if (careTick.died) {
            val cause = if (!won && current.criticalRemainingMs > 0) "critical" else "overwork"
            Timber.w("Digimon died ($cause) — clearing active monster")
            clearSlot("current_")
            prefs.edit()
                .putBoolean("current_is_expired", true)
                .putString("current_death_cause", cause)
                .apply()
            return true
        }
        return false
    }

    /** Earn Vital Points (lifetime + current stage). VP comes from exercise/activity. */
    fun addVitalPoints(points: Int) {
        val current = getCurrentMonster() ?: return
        if (points <= 0) return
        prefs.edit()
            .putInt("current_vital_points", current.vitalPoints + points)
            .putInt("current_stage_vital_points", current.stageVitalPoints + points)
            .apply()
    }

    fun addTrophy() {
        val current = getCurrentMonster() ?: return
        prefs.edit()
            .putInt("current_trophies", current.trophies + 1)
            .putInt("current_stage_trophies", current.stageTrophies + 1)
            .apply()
    }

    /** Why the active monster died: "neglect", "overwork", or "critical". */
    fun getDeathCause(): String = prefs.getString("current_death_cause", "neglect") ?: "neglect"

    /**
     * A completed exercise shaves 15 minutes off the critical healing timer
     * (two exercises heal it fully). No-op when not critical. Only ever
     * touches the ACTIVE slot — a stored partner's timer stays frozen.
     * @return true if this exercise healed the Digimon out of critical.
     */
    fun recordExerciseCompleted(): Boolean {
        val current = getCurrentMonster() ?: return false
        if (current.criticalRemainingMs <= 0) return false
        val care = CareManager.recordExercise(toCareState(current))
        persistCare("current_", care)
        Timber.d("Exercise completed during critical: ${CareManager.formatCriticalMs(care.criticalRemainingMs)} left")
        return care.criticalRemainingMs <= 0
    }

    /**
     * Feed the watch's step counter into the system. Steps convert to Vital
     * Points, and hitting the daily goal counts as "looked after" for care.
     * Safe to call often — only the delta is converted, and a lower total is
     * treated as the midnight counter reset.
     */
    fun syncStepsToVitalPoints(totalStepsToday: Int) {
        val current = getCurrentMonster() ?: return
        val today = System.currentTimeMillis() / 86400000L
        val lastSynced = current.lastSyncedSteps
        val delta = if (totalStepsToday < lastSynced) totalStepsToday
            else (totalStepsToday - lastSynced).coerceAtLeast(0)
        val vp = delta / CareTuning.STEPS_PER_VITAL_POINT

        var care = toCareState(current)
        if (care.maxLifespanHours <= 0) care = CareManager.initialForStage(current.stage, today)
        if (totalStepsToday >= CareTuning.DAILY_STEP_GOAL) {
            care = CareManager.recordActivity(care, totalStepsToday, today)
        }
        persistCare("current_", care)

        prefs.edit()
            .putInt("current_last_synced_steps", totalStepsToday)
            .putInt("current_steps", totalStepsToday)
            .putInt("current_vital_points", current.vitalPoints + vp)
            .putInt("current_stage_vital_points", current.stageVitalPoints + vp)
            .apply()
        if (vp > 0) Timber.d("Steps +$delta -> +$vp VP")
    }

    /**
     * Advance the care clock by real elapsed time since the last tick.
     * Seeds a fresh clock for pre-system monsters so they don't insta-die.
     * @return true if the Digimon died of neglect (active monster cleared).
     */
    fun tickCare(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val current = getCurrentMonster() ?: return false
        var care = toCareState(current)
        val today = nowMillis / 86400000L
        if (care.maxLifespanHours <= 0) {
            care = CareManager.initialForStage(current.stage, today)
        }
        if (current.lastCareTick == 0L) {
            persistCare("current_", care)
            prefs.edit().putLong("current_last_care_tick", nowMillis).apply()
            return false
        }
        val elapsedHours = (nowMillis - current.lastCareTick) / 3600000.0
        // Clamp absurd gaps (watch off for weeks) so one tick can't insta-kill.
        val clamped = elapsedHours.coerceIn(0.0, 72.0)
        var tick = CareManager.tickTime(care, clamped, today)
        // The service ticks every few seconds: a neglect penalty may only land
        // once per 24h, otherwise one bad stretch insta-kills through repeats.
        val lastNeglect = prefs.getLong("current_last_neglect_penalty", 0L)
        if (tick.newMistakes > 0 && nowMillis - lastNeglect < 86400000L) {
            tick = tick.copy(
                state = tick.state.copy(
                    careMistakes = tick.state.careMistakes - tick.newMistakes,
                    lifespanHoursRemaining = (tick.state.lifespanHoursRemaining +
                        tick.newMistakes * CareTuning.MISTAKE_PENALTY_HOURS)
                        .coerceAtMost(tick.state.maxLifespanHours)
                ),
                newMistakes = 0,
                warnings = emptyList()
            )
        } else if (tick.newMistakes > 0) {
            prefs.edit().putLong("current_last_neglect_penalty", nowMillis).apply()
        }
        persistCare("current_", tick.state)
        prefs.edit().putLong("current_last_care_tick", nowMillis).apply()
        for (warning in tick.warnings) Timber.w(warning)
        if (tick.died) {
            Timber.w("Digimon died (neglect) — clearing active monster")
            clearSlot("current_")
            prefs.edit().putBoolean("current_is_expired", true).apply()
            return true
        }
        return false
    }

    /**
     * Evolve along the card's REAL tree to [toIndex] (not just +1).
     * Resets per-stage performance counters and reseeds the care clock.
     */
    fun evolveTo(toIndex: Int, hoursUntilEvolution: Int): Boolean {
        val current = getCurrentMonster() ?: return false
        val today = System.currentTimeMillis() / 86400000L
        persistCare("current_", CareManager.initialForStage(current.stage + 1, today))

        prefs.edit()
            .putInt("current_character_id", toIndex)
            .putInt("current_stage", current.stage + 1)
            .putLong("current_time_alive", 0)
            .putLong("current_evolution_time", hoursUntilEvolution * 3600L)
            .putInt("current_stage_battles", 0)
            .putInt("current_stage_wins", 0)
            .putInt("current_stage_vital_points", 0)
            .putInt("current_stage_trophies", 0)
            .putLong("current_last_care_tick", System.currentTimeMillis())
            .apply()

        applyCardBaseStats(current.cardName, toIndex)
        Timber.d("Evolved ${current.cardName}: ${current.characterId} -> $toIndex")
        return true
    }

    /**
     * All evolution options for the current monster, evaluated against the
     * card's real requirements and this stage's performance. Empty = no
     * options on this card (or card missing).
     */
    fun getEvolutionCandidates(): List<EvolutionCandidate> {
        val current = getCurrentMonster() ?: return emptyList()
        val card = try {
            CardManager(context).getCard(current.cardName)
        } catch (t: Throwable) {
            Timber.e(t, "getEvolutionCandidates: failed to load card")
            null
        } ?: return emptyList()

        val paths = try {
            DimCardAdapter.getEvolutionPaths(card, current.characterId)
        } catch (t: Throwable) {
            Timber.e(t, "getEvolutionCandidates: failed to read evolution tree")
            emptyList()
        }
        if (paths.isEmpty()) return emptyList()

        val snap = PerformanceSnapshot(
            vitalPoints = current.stageVitalPoints,
            trophies = current.stageTrophies,
            battles = current.stageBattles,
            wins = current.stageWins,
            hoursAtStage = current.timeAlive / 3600.0
        )
        return EvolutionEngine.evaluate(paths, snap)
    }

    /**
     * Reads the DIM-baked base stats for [characterId] on [cardName] and stores
     * them on the monster so battles use the card's real HP/AP.
     */
    /**
     * Re-seeds stored base stats when the seeding scheme changes. Safe to run
     * often: no-ops once the stored version is current, and only marks the
     * version when the card actually loaded.
     */
    private fun migrateBaseStats(cardName: String, characterId: Int) {
        if (characterId == -1) return
        if (prefs.getInt("current_base_stats_version", 0) >= BASE_STATS_VERSION) return
        if (applyCardBaseStats(cardName, characterId) != null) {
            prefs.edit().putInt("current_base_stats_version", BASE_STATS_VERSION).apply()
            Timber.d("Base stats migrated to v$BASE_STATS_VERSION for $cardName#$characterId")
        }
    }

    private fun normalizeBaseStat(raw: Int, isBem: Boolean): Int {
        return if (isBem) raw / BEM_STAT_SCALE else raw * DIM_STAT_SCALE
    }

    fun applyCardBaseStats(cardName: String, characterId: Int): DigimonBaseStats? {
        return try {
            val card = CardManager(context).getCard(cardName) ?: return null
            val stats = DimCardAdapter.getBaseStats(card, characterId) ?: return null
            val isBem = card is BemCard
            val normHp = normalizeBaseStat(stats.hp, isBem)
            val normAp = normalizeBaseStat(stats.ap, isBem)
            prefs.edit()
                .putInt("current_base_hp", normHp)
                .putInt("current_base_ap", normAp)
                .putInt("current_attribute", stats.attribute)
                .apply()
            Timber.d("Applied DIM base stats $cardName#$characterId: HP=$normHp AP=$normAp")
            stats
        } catch (t: Throwable) {
            Timber.e(t, "applyCardBaseStats failed")
            null
        }
    }

    /**
     * The DIM card programs two attacks per character: the small (regular)
     * attack and the big (critical) attack. These IDs select the attack's
     * appearance from the Bracelet's effect library -- so a modded DIM picks
     * its Digimon's attack visuals by setting these IDs.
     * Returns (smallAttackId, bigAttackId).
     */
    fun getAttackIds(card: Card<*, *, *, *, *, *>, characterId: Int): Pair<Int, Int>? {
        return try {
            val stats = DimCardAdapter.getBaseStats(card, characterId) ?: return null
            Pair(stats.smallAttackId, stats.bigAttackId)
        } catch (e: Exception) {
            null
        }
    }

    /** Name-based convenience: loads the card first, then delegates. */
    fun getAttackIds(cardName: String, characterId: Int): Pair<Int, Int>? {
        val card = CardManager(context).getCard(cardName) ?: return null
        return getAttackIds(card, characterId)
    }

    /**
     * Critical-hit chance from the card's programmed big-attack pool chance.
     * The DIM stores each attack's selection odds; the big attack lands as a
     * crit. Clamped to a sane band; 15% when the card has no data.
     */
    fun getCritChance(card: Card<*, *, *, *, *, *>, characterId: Int): Float {
        return try {
            val stats = DimCardAdapter.getBaseStats(card, characterId) ?: return 0.15f
            if (stats.secondPoolBattleChance <= 0) return 0.15f
            (stats.secondPoolBattleChance / 100f).coerceIn(0.05f, 0.35f)
        } catch (e: Exception) { 0.15f }
    }

    /** Name-based convenience: loads the card first, then delegates. */
    fun getCritChance(cardName: String, characterId: Int): Float {
        val card = CardManager(context).getCard(cardName) ?: return 0.15f
        return getCritChance(card, characterId)
    }

    private fun toCareState(current: MonsterState): CareState = CareState(
        maxLifespanHours = current.maxLifespanHours,
        lifespanHoursRemaining = current.lifespanHoursRemaining,
        careMistakes = current.careMistakes,
        consecutiveBattles = current.consecutiveBattles,
        lastActiveDay = current.lastActiveDay,
        consecutiveLosses = current.consecutiveLosses,
        criticalRemainingMs = current.criticalRemainingMs
    )

    private fun persistCare(prefix: String, state: CareState) {
        prefs.edit()
            .putFloat(prefix + "lifespan_remaining", state.lifespanHoursRemaining.toFloat())
            .putFloat(prefix + "max_lifespan", state.maxLifespanHours.toFloat())
            .putInt(prefix + "care_mistakes", state.careMistakes)
            .putInt(prefix + "care_consecutive_battles", state.consecutiveBattles)
            .putLong(prefix + "last_active_day", state.lastActiveDay)
            .putInt(prefix + "consecutive_losses", state.consecutiveLosses)
            .putLong(prefix + "critical_remaining_ms", state.criticalRemainingMs)
            .apply()
    }

    fun evolve(nextCharacterId: Int) {
        val current = getCurrentMonster() ?: return
        if (nextCharacterId >= 16) {
            Timber.d("Reached max evolution for this version")
            return
        }
        // Carry over bonuses
        setCurrentMonster(
            current.cardName, 
            nextCharacterId, 
            current.stage + 1,
            current.attackBonus,
            current.healthBonus,
            current.speedBonus,
            current.defenseBonus
        )
    }

    fun restoreMonster(cardName: String, characterId: Int, stage: Int, atk: Int, hp: Int, spd: Int, def: Int, 
                       attr: Int = 0, mood: Int = 0, steps: Int = 0, bp: Int = 0, sp: Int = 0, winRatio: Int = 0, trophies: Int = 0) {
        Timber.d("Restoring: $cardName ID:$characterId Stage:$stage ATK:$atk HP:$hp SPD:$spd DEF:$def")
        prefs.edit()
            .putString("current_card", cardName)
            .putInt("current_character_id", characterId)
            .putInt("current_stage", stage)
            .putInt("current_attack_bonus", atk)
            .putInt("current_health_bonus", hp)
            .putInt("current_speed_bonus", spd)
            .putInt("current_defense_bonus", def)
            .putInt("current_attribute", attr)
            .putInt("current_mood", mood)
            .putInt("current_steps", steps)
            .putInt("current_bp", bp)
            .putInt("current_sp", sp)
            .putInt("current_win_ratio", winRatio)
            .putInt("current_trophies", trophies)
            .putBoolean("current_is_expired", false)
            .putLong("current_time_alive", 0) 
            .apply()

        // The phone doesn't transfer care data, so seed a fresh clock and
        // pull the DIM-baked base stats for this form.
        val today = System.currentTimeMillis() / 86400000L
        prefs.edit()
            .putInt("current_losses", 0)
            .putInt("current_vital_points", 0)
            .putInt("current_stage_battles", 0)
            .putInt("current_stage_wins", 0)
            .putInt("current_stage_vital_points", 0)
            .putInt("current_stage_trophies", 0)
            .putInt("current_last_synced_steps", 0)
            .putLong("current_last_neglect_penalty", 0L)
            .putLong("current_last_care_tick", System.currentTimeMillis())
            .apply()
        persistCare("current_", CareManager.initialForStage(stage, today))
        applyCardBaseStats(cardName, characterId)
    }

    fun isExpired(): Boolean {
        return prefs.getBoolean("current_is_expired", false)
    }

    fun devJump(forward: Boolean) {
        val current = getCurrentMonster() ?: return
        if (forward) {
            val candidates = getEvolutionCandidates()
            val pick = if (candidates.isNotEmpty()) {
                val available = candidates.filter { it.requirementsMet }
                if (available.isNotEmpty()) {
                    available.maxByOrNull { it.path.requiredTrophies * 1000 + it.path.requiredVitalValues }!!
                } else {
                    candidates.maxByOrNull { c -> c.progress.count { it.met } } ?: candidates.first()
                }
            } else null
            
            if (pick != null) {
                evolveTo(pick.path.toIndex, pick.path.hoursUntilEvolution)
                return
            }
        }
        
        var nextId = if (forward) current.characterId + 1 else current.characterId - 1
        if (nextId < 0) nextId = 0
        
        val newStage = if (nextId == 0) 0 else (if (nextId == 1) 1 else 3)
        prefs.edit()
            .putInt("current_character_id", nextId)
            .putInt("current_stage", newStage) 
            .putInt("current_stage_battles", 0)
            .putInt("current_stage_wins", 0)
            .putInt("current_stage_vital_points", 0)
            .putInt("current_stage_trophies", 0)
            .putLong("current_last_care_tick", System.currentTimeMillis())
            .apply()
        val today = System.currentTimeMillis() / 86400000L
        persistCare("current_", CareManager.initialForStage(newStage, today))
        applyCardBaseStats(current.cardName, nextId)
    }

    fun hasMonster(): Boolean {
        return prefs.contains("current_card")
    }

    fun getIdleSpriteIndices(characterId: Int, maxSprites: Int, isBem: Boolean): List<Int> {
        val baseIdx = getCharacterBaseIndex(characterId, isBem)
        val frame1 = baseIdx + 1
        val frame2 = baseIdx + 2
        
        val result = mutableListOf<Int>()
        if (frame1 < maxSprites) result.add(frame1)
        if (frame2 < maxSprites) result.add(frame2)
        
        if (result.isEmpty() && maxSprites > 0) result.add(0)
        return result
    }

    fun getBattleSpriteIndex(characterId: Int, maxSprites: Int, isBem: Boolean): Int {
        val baseIdx = getCharacterBaseIndex(characterId, isBem)
        val attackIdx = if (isBem || characterId >= 2) baseIdx + 11 else baseIdx + 3
        return if (attackIdx < maxSprites) attackIdx else (if (maxSprites > 0) 0 else 0)
    }

    // Portrait shown large during the evolution reveal. Each sprite sheet has its
    // high-detail portrait attached at the end of the character's block, and it is
    // the largest sprite there (custom sheets may pack tighter than the stock
    // 14/7/6 block sizes, so we pick by size instead of a fixed offset). Ties
    // fall through to the last sprite, i.e. the portrait at the end of the list.
    fun getPortraitSpriteIndex(characterId: Int, isBem: Boolean, sprites: List<SpriteData.Sprite>): Int {
        val baseIdx = getCharacterBaseIndex(characterId, isBem)
        val stride = if (isBem) 14 else when (characterId) {
            0 -> 6
            1 -> 7
            else -> 14
        }
        val end = minOf(baseIdx + stride, sprites.size)
        if (baseIdx >= end) return 0
        var best = baseIdx
        var bestArea = -1
        for (i in baseIdx until end) {
            val area = sprites[i].width * sprites[i].height
            if (area >= bestArea) {
                bestArea = area
                best = i
            }
        }
        return best
    }

    fun getWinSpriteIndex(characterId: Int, maxSprites: Int, isBem: Boolean): Int {
        val baseIdx = getCharacterBaseIndex(characterId, isBem)
        val winIdx = if (isBem || characterId >= 2) baseIdx + 9 else baseIdx + 4
        return if (winIdx < maxSprites) winIdx else 0
    }

    fun getLoseSpriteIndex(characterId: Int, maxSprites: Int, isBem: Boolean): Int {
        val baseIdx = getCharacterBaseIndex(characterId, isBem)
        val loseIdx = if (isBem || characterId >= 2) baseIdx + 10 else baseIdx + 5
        return if (loseIdx < maxSprites) loseIdx else 0
    }

    private fun getCharacterBaseIndex(characterId: Int, isBem: Boolean): Int {
        if (isBem) {
            return 54 + (characterId * 14)
        } else {
            var currentIdx = 10
            for (i in 0 until characterId) {
                currentIdx += when(i) {
                    0 -> 6
                    1 -> 7
                    else -> 14
                }
            }
            return currentIdx
        }
    }
}
