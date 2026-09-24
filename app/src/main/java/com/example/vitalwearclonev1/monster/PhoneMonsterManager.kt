package com.example.vitalwearclonev1.monster

import android.content.Context
import android.content.SharedPreferences
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.card.DimCardAdapter
import com.example.vitalwearclonev1.card.DigimonBaseStats
import com.github.cfogrady.vb.dim.sprite.SpriteData
import timber.log.Timber

class PhoneMonsterManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("phone_monster_prefs", Context.MODE_PRIVATE)

    data class MonsterState(
        val cardName: String,
        val characterId: Int,
        val stage: Int,
        val timeAlive: Long = 0,
        val evolutionTime: Long = 3600, // 1 hour for phone app testing
        val attackBonus: Int = 0,
        val healthBonus: Int = 0,
        val speedBonus: Int = 0,
        val defenseBonus: Int = 0,
        val isEvolutionPaused: Boolean = false,
        val currentWins: Int = 0,
        val winsRequired: Int = 0,
        val xp: Int = 0,
        val level: Int = 1,
        val rawPayload: String? = null,
        val nickname: String? = null,
        val isBem: Boolean = false,
        val attribute: Int = 0,
        val mood: Int = 50,
        val steps: Int = 0,
        val bp: Int = 0,
        val sp: Int = 0,
        val winRatio: Int = 0,
        val trophies: Int = 0,
        // --- Evolution engine (DIM-tree) state ---
        val losses: Int = 0,
        val vitalPoints: Int = 0,
        val stageBattles: Int = 0,
        val stageWins: Int = 0,
        val stageVitalPoints: Int = 0,
        val stageTrophies: Int = 0,
        // --- DIM-baked base stats (applied on load) ---
        val baseHp: Int = 500,
        val baseAp: Int = 0,
        // --- Care / lifespan state ---
        val lifespanHoursRemaining: Double = 0.0,
        val maxLifespanHours: Double = 0.0,
        val careMistakes: Int = 0,
        val consecutiveBattles: Int = 0,
        val lastActiveDay: Long = 0L,
        val lastCareTick: Long = 0L,
        val lastSyncedSteps: Int = 0
    )

    fun getCurrentMonster(): MonsterState? {
        val cardName = prefs.getString("current_card", null) ?: return null
        val characterId = prefs.getInt("current_character_id", -1)
        val stage = prefs.getInt("current_stage", 0)
        val timeAlive = prefs.getLong("current_time_alive", 0)
        val evolutionTime = prefs.getLong("current_evolution_time", 3600)
        val attackBonus = prefs.getInt("current_attack_bonus", 0)
        val healthBonus = prefs.getInt("current_health_bonus", 0)
        val speedBonus = prefs.getInt("current_speed_bonus", 0)
        val defenseBonus = prefs.getInt("current_defense_bonus", 0)
        val isPaused = prefs.getBoolean("current_evolution_paused", false)
        val currentWins = prefs.getInt("current_wins", 0)
        val winsRequired = prefs.getInt("wins_required", 0)
        val xp = prefs.getInt("current_xp", 0)
        val level = prefs.getInt("current_level", 1)
        val raw = prefs.getString("current_raw_payload", null)
        val nickname = prefs.getString("current_nickname", null)
        val isBem = prefs.getBoolean("current_is_bem", false)
        val attribute = prefs.getInt("current_attribute", 0)
        val mood = prefs.getInt("current_mood", 50)
        val steps = prefs.getInt("current_steps", 0)
        val bp = prefs.getInt("current_bp", 0)
        val sp = prefs.getInt("current_sp", 0)
        val winRatio = prefs.getInt("current_win_ratio", 0)
        val trophies = prefs.getInt("current_trophies", 0)
        val losses = prefs.getInt("current_losses", 0)
        val vitalPoints = prefs.getInt("current_vital_points", 0)
        val stageBattles = prefs.getInt("current_stage_battles", 0)
        val stageWins = prefs.getInt("current_stage_wins", 0)
        val stageVitalPoints = prefs.getInt("current_stage_vital_points", 0)
        val stageTrophies = prefs.getInt("current_stage_trophies", 0)
        val baseHp = prefs.getInt("current_base_hp", 500)
        val baseAp = prefs.getInt("current_base_ap", 0)
        val lifespanHoursRemaining = prefs.getFloat("current_lifespan_remaining", 0f).toDouble()
        val maxLifespanHours = prefs.getFloat("current_max_lifespan", 0f).toDouble()
        val careMistakes = prefs.getInt("current_care_mistakes", 0)
        val consecutiveBattles = prefs.getInt("current_care_consecutive_battles", 0)
        val lastActiveDay = prefs.getLong("current_last_active_day", 0L)
        val lastCareTick = prefs.getLong("current_last_care_tick", 0L)
        val lastSyncedSteps = prefs.getInt("current_last_synced_steps", 0)
        
        if (characterId == -1) return null
        return MonsterState(cardName, characterId, stage, timeAlive, evolutionTime, attackBonus, healthBonus, speedBonus, defenseBonus, isPaused, currentWins, winsRequired, xp, level, raw, nickname, isBem, attribute, mood, steps, bp, sp, winRatio, trophies, losses, vitalPoints, stageBattles, stageWins, stageVitalPoints, stageTrophies, baseHp, baseAp, lifespanHoursRemaining, maxLifespanHours, careMistakes, consecutiveBattles, lastActiveDay, lastCareTick, lastSyncedSteps)
    }

    fun setCurrentMonster(
        cardName: String, 
        characterId: Int, 
        stage: Int = 0,
        atk: Int = 0,
        hp: Int = 0,
        spd: Int = 0,
        def: Int = 0,
        wins: Int = 0,
        winsReq: Int = 0,
        xp: Int = 0,
        level: Int = 1,
        raw: String? = null,
        nickname: String? = null,
        isBem: Boolean = false,
        timeAlive: Long = 0,
        evolutionTime: Long = 3600,
        attribute: Int = 0,
        mood: Int = 50,
        steps: Int = 0,
        bp: Int = 0,
        sp: Int = 0,
        winRatio: Int = 0,
        trophies: Int = 0
    ) {
        val previous = getCurrentMonster()
        val isFresh = previous == null || previous.cardName != cardName || previous.characterId != characterId
        prefs.edit()
            .putString("current_card", cardName)
            .putInt("current_character_id", characterId)
            .putInt("current_stage", stage)
            .putLong("current_time_alive", timeAlive)
            .putLong("current_evolution_time", evolutionTime)
            .putInt("current_attack_bonus", atk)
            .putInt("current_health_bonus", hp)
            .putInt("current_speed_bonus", spd)
            .putInt("current_defense_bonus", def)
            .putBoolean("current_evolution_paused", false)
            .putInt("current_wins", wins)
            .putInt("wins_required", winsReq)
            .putInt("current_xp", xp)
            .putInt("current_level", level)
            .putString("current_raw_payload", raw)
            .putString("current_nickname", nickname)
            .putBoolean("current_is_bem", isBem)
            .putInt("current_attribute", attribute)
            .putInt("current_mood", mood)
            .putInt("current_steps", steps)
            .putInt("current_bp", bp)
            .putInt("current_sp", sp)
            .putInt("current_win_ratio", winRatio)
            .putInt("current_trophies", trophies)
            .apply()

        // Fresh monster (first load, hatch, or card/character change): seed the
        // care clock and pull the DIM-baked base stats for this form.
        if (isFresh) {
            val today = System.currentTimeMillis() / 86400000L
            persistCare(CareManager.initialForStage(stage, today))
            prefs.edit().putLong("current_last_care_tick", System.currentTimeMillis()).apply()
            applyCardBaseStats(cardName, characterId)
            Timber.d("Initialized care + base stats for fresh monster $cardName#$characterId")
        }
    }

    fun updateNickname(nickname: String) {
        prefs.edit().putString("current_nickname", nickname).apply()
    }

    fun updateAging(seconds: Long) {
        val current = getCurrentMonster() ?: return
        prefs.edit()
            .putLong("current_time_alive", current.timeAlive + seconds)
            .apply()
        // Piggyback the care clock on the aging loop so lifespan advances in
        // real time, even across app restarts.
        tickCare()
    }

    fun toggleEvolutionPause() {
        val current = getCurrentMonster() ?: return
        prefs.edit()
            .putBoolean("current_evolution_paused", !current.isEvolutionPaused)
            .apply()
    }

    /**
     * Records a finished battle — win OR loss. Updates lifetime + per-stage
     * counters, recomputes win ratio, and charges the care system.
     * @return true if the Digimon died of poor care during this battle.
     */
    fun recordBattleResult(won: Boolean): Boolean {
        val current = getCurrentMonster() ?: return false
        val careTick = CareManager.recordBattle(toCareState(current))
        persistCare(careTick.state)

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
            onDigimonDeath("overwork")
            return true
        }
        return false
    }

    /** Legacy win hook — now routes through the full battle recorder. */
    fun addWin() {
        recordBattleResult(true)
    }

    fun addLoss() {
        recordBattleResult(false)
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

    /**
     * Feed the phone's step counter into the system. Steps convert to Vital
     * Points, and hitting the daily goal counts as "looked after" for care.
     * Call this periodically with the day's total step count (e.g. from
     * PhoneGpsManager). Safe to call often — only the delta is converted.
     */
    fun syncStepsToVitalPoints(totalStepsToday: Int) {
        val current = getCurrentMonster() ?: return
        val delta = (totalStepsToday - current.lastSyncedSteps).coerceAtLeast(0)
        val vp = delta / CareTuning.STEPS_PER_VITAL_POINT
        val today = System.currentTimeMillis() / 86400000L

        var care = toCareState(current)
        if (totalStepsToday >= CareTuning.DAILY_STEP_GOAL) {
            care = CareManager.recordActivity(care, totalStepsToday, today)
        }
        persistCare(care)

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
     * @return true if the Digimon died of neglect (monster is cleared).
     */
    fun tickCare(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val current = getCurrentMonster() ?: return false
        if (current.lastCareTick == 0L) {
            prefs.edit().putLong("current_last_care_tick", nowMillis).apply()
            return false
        }
        val elapsedHours = (nowMillis - current.lastCareTick) / 3600000.0
        // Clamp absurd gaps (phone off for weeks) so one tick can't insta-kill.
        val clamped = elapsedHours.coerceIn(0.0, 72.0)
        val today = nowMillis / 86400000L
        val tick = CareManager.tickTime(toCareState(current), clamped, today)
        persistCare(tick.state)
        prefs.edit().putLong("current_last_care_tick", nowMillis).apply()
        for (warning in tick.warnings) Timber.w(warning)
        if (tick.died) {
            onDigimonDeath("neglect")
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
        persistCare(CareManager.initialForStage(current.stage + 1, today))

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
    fun applyCardBaseStats(cardName: String, characterId: Int): DigimonBaseStats? {
        return try {
            val card = CardManager(context).getCard(cardName) ?: return null
            val stats = DimCardAdapter.getBaseStats(card, characterId) ?: return null
            prefs.edit()
                .putInt("current_base_hp", stats.hp)
                .putInt("current_base_ap", stats.ap)
                .putInt("current_attribute", stats.attribute)
                .apply()
            Timber.d("Applied DIM base stats $cardName#$characterId: HP=${stats.hp} AP=${stats.ap}")
            stats
        } catch (t: Throwable) {
            Timber.e(t, "applyCardBaseStats failed")
            null
        }
    }

    /**
     * The DIM "self delete": the monster dies and is wiped, but owned cards
     * and dev settings survive.
     */
    fun clearMonster() {
        val keys = listOf(
            "current_card", "current_character_id", "current_stage",
            "current_time_alive", "current_evolution_time",
            "current_attack_bonus", "current_health_bonus", "current_speed_bonus", "current_defense_bonus",
            "current_evolution_paused", "current_wins", "wins_required",
            "current_xp", "current_level", "current_raw_payload", "current_nickname",
            "current_is_bem", "current_attribute", "current_mood", "current_steps",
            "current_bp", "current_sp", "current_win_ratio", "current_trophies",
            "current_losses", "current_vital_points",
            "current_stage_battles", "current_stage_wins",
            "current_stage_vital_points", "current_stage_trophies",
            "current_base_hp", "current_base_ap",
            "current_lifespan_remaining", "current_max_lifespan",
            "current_care_mistakes", "current_care_consecutive_battles",
            "current_last_active_day", "current_last_care_tick", "current_last_synced_steps"
        )
        val edit = prefs.edit()
        keys.forEach { edit.remove(it) }
        edit.apply()
    }

    private fun onDigimonDeath(cause: String) {
        Timber.w("Digimon died ($cause) — clearing monster")
        clearMonster()
    }

    private fun toCareState(current: MonsterState): CareState = CareState(
        maxLifespanHours = current.maxLifespanHours,
        lifespanHoursRemaining = current.lifespanHoursRemaining,
        careMistakes = current.careMistakes,
        consecutiveBattles = current.consecutiveBattles,
        lastActiveDay = current.lastActiveDay
    )

    private fun persistCare(state: CareState) {
        prefs.edit()
            .putFloat("current_lifespan_remaining", state.lifespanHoursRemaining.toFloat())
            .putFloat("current_max_lifespan", state.maxLifespanHours.toFloat())
            .putInt("current_care_mistakes", state.careMistakes)
            .putInt("current_care_consecutive_battles", state.consecutiveBattles)
            .putLong("current_last_active_day", state.lastActiveDay)
            .apply()
    }

    fun evolve() {
        val current = getCurrentMonster() ?: return
        
        val nextId = current.characterId + 1
        
        // Check if next character is a secret one (usually index 16+ on DIMs)
        if (!current.isBem && nextId >= 16) {
            if (!isCardSecretUnlocked(current.cardName)) {
                Timber.d("Secret evolution $nextId is locked for card ${current.cardName}")
                return
            }
        }
        
        // Evolution limit: allow up to 18 for DIMs if secret is unlocked, otherwise 16.
        // BEMs have a much higher limit.
        val maxId = if (current.isBem) 32 else 18
        
        if (nextId >= maxId) {
            Timber.d("Reached final evolution for card ${current.cardName}")
            return
        }
        
        val nextStage = current.stage + 1
        
        // Randomize next requirement
        val nextWinsRequired = if (nextStage > 0) (3..15).random() else 0

        prefs.edit()
            .putInt("current_character_id", nextId)
            .putInt("current_stage", nextStage)
            .putLong("current_time_alive", 0)
            .putInt("current_stage_battles", 0)
            .putInt("current_stage_wins", 0)
            .putInt("current_stage_vital_points", 0)
            .putInt("current_stage_trophies", 0)
            .putInt("wins_required", nextWinsRequired)
            .apply()

        // Fresh form: reseed care clock + pull the new form's DIM base stats.
        val today = System.currentTimeMillis() / 86400000L
        persistCare(CareManager.initialForStage(nextStage, today))
        prefs.edit().putLong("current_last_care_tick", System.currentTimeMillis()).apply()
        applyCardBaseStats(current.cardName, nextId)
    }

    fun forceEvolve() {
        evolve()
    }

    fun reverseEvolve() {
        val current = getCurrentMonster() ?: return
        if (current.characterId <= 0) return
        
        val nextId = current.characterId - 1
        prefs.edit()
            .putInt("current_character_id", nextId)
            .putInt("current_stage", (current.stage - 1).coerceAtLeast(0))
            .putLong("current_time_alive", 0)
            .apply()
    }

    fun setBonusStats(atk: Int, hp: Int, spd: Int, def: Int) {
        prefs.edit()
            .putInt("current_attack_bonus", atk)
            .putInt("current_health_bonus", hp)
            .putInt("current_speed_bonus", spd)
            .putInt("current_defense_bonus", def)
            .apply()
    }

    fun updateBonusStats(atkDelta: Int, hpDelta: Int, spdDelta: Int, defDelta: Int) {
        val current = getCurrentMonster() ?: return
        setBonusStats(
            current.attackBonus + atkDelta,
            current.healthBonus + hpDelta,
            current.speedBonus + spdDelta,
            current.defenseBonus + defDelta
        )
    }

    fun applyWorkoutPowerUp(completionRatio: Float) {
        val current = getCurrentMonster() ?: return
        val baseBonus = (50 * completionRatio).toInt()
        val random = kotlin.random.Random(System.currentTimeMillis())
        
        updateBonusStats(
            atkDelta = baseBonus + random.nextInt(0, 50),
            hpDelta = baseBonus + random.nextInt(0, 50),
            spdDelta = baseBonus + random.nextInt(0, 50),
            defDelta = baseBonus + random.nextInt(0, 50)
        )
    }

    fun addXp(xpGain: Int) {
        val current = getCurrentMonster() ?: return
        var newXp = current.xp + xpGain
        var newLevel = current.level
        var nextThreshold = 1000 + (newLevel - 1) * 500
        
        while (newXp >= nextThreshold) {
            newXp -= nextThreshold
            newLevel++
            nextThreshold = 1000 + (newLevel - 1) * 500
            // Level up reward
            updateBonusStats(100, 100, 100, 100)
        }
        
        prefs.edit()
            .putInt("current_xp", newXp)
            .putInt("current_level", newLevel)
            .apply()
    }

    fun isDevModeEnabled(): Boolean {
        return prefs.getBoolean("dev_mode_enabled", false)
    }

    fun setDevModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dev_mode_enabled", enabled).apply()
    }

    fun getIdleSpriteIndices(characterId: Int, isBem: Boolean): List<Int> {
        val baseIdx = getCharacterBaseIndex(characterId, isBem)
        return listOf(baseIdx + 1, baseIdx + 2)
    }

    fun getBattleSpriteIndex(characterId: Int, isBem: Boolean): Int {
        val baseIdx = getCharacterBaseIndex(characterId, isBem)
        return if (isBem || characterId >= 2) baseIdx + 11 else baseIdx + 3
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

    fun getWinSpriteIndex(characterId: Int, isBem: Boolean): Int {
        val baseIdx = getCharacterBaseIndex(characterId, isBem)
        return if (isBem || characterId >= 2) baseIdx + 9 else baseIdx + 4
    }

    fun getLoseSpriteIndex(characterId: Int, isBem: Boolean): Int {
        val baseIdx = getCharacterBaseIndex(characterId, isBem)
        return if (isBem || characterId >= 2) baseIdx + 10 else baseIdx + 5
    }

    fun isCardSecretUnlocked(cardName: String): Boolean {
        val unlocked = prefs.getStringSet("unlocked_dim_secrets", emptySet()) ?: emptySet()
        return unlocked.contains(cardName)
    }

    fun unlockCardSecret(cardName: String) {
        val unlocked = prefs.getStringSet("unlocked_dim_secrets", emptySet())?.toMutableSet() ?: mutableSetOf()
        unlocked.add(cardName)
        prefs.edit().putStringSet("unlocked_dim_secrets", unlocked).apply()
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
