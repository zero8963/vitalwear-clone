package com.example.vitalwearclonev1.monster

import android.content.Context
import android.content.SharedPreferences
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.card.DimCardAdapter
import com.example.vitalwearclonev1.card.DigimonBaseStats
import com.github.cfogrady.vb.dim.card.BemCard
import com.github.cfogrady.vb.dim.card.Card
import com.github.cfogrady.vb.dim.sprite.SpriteData
import timber.log.Timber

class PhoneMonsterManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("phone_monster_prefs", Context.MODE_PRIVATE)

    // One-shot guard for the base-stat migration in getCurrentMonster (per process).
    private var baseStatsMigrationDone = false

    // 2026-09-27: cache the card's base crit chance per card+character. The
    // DIM parse behind getCritChance(cardName, ...) reads the whole card file
    // off disk, and it was being re-run on UI recompositions (dev menu) and
    // battle setup, stuttering the main thread. Card data never changes at
    // runtime, so a process-lifetime cache is safe.
    private val baseCritCache = mutableMapOf<String, Float>()

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
        val lastSyncedSteps: Int = 0,
        val consecutiveLosses: Int = 0,
        val criticalRemainingMs: Long = 0L,
        // --- Secret education stats (2026-09-27): lesson/practice bonuses ---
        val secretCritChance: Int = 0,
        val secretCritDamage: Int = 0
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
        var winsRequired = prefs.getInt("wins_required", 0)
        // One-time repair: Digimon hatched before the random roll existed came
        // in at 0 wins required, which blocks digivolving entirely (the gate
        // needs winsRequired > 0). Roll one now so nobody stays stuck.
        if (winsRequired <= 0) {
            winsRequired = rollWinsRequired()
            prefs.edit().putInt("wins_required", winsRequired).apply()
            Timber.d("Repaired wins_required=$winsRequired for stuck monster")
        }
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
        // Base-stat migration (2026-09-23): v0 = hatched before DIM stats
        // existed (500/0 defaults, fought with 0 base AP); v1 = seeded with
        // RAW card values (up to 65535, dealt ~16k damage). v2 normalizes.
        if (!baseStatsMigrationDone) {
            baseStatsMigrationDone = true
            migrateBaseStats(cardName, characterId)
        }
        val baseHp = prefs.getInt("current_base_hp", 500)
        val baseAp = prefs.getInt("current_base_ap", 0)
        val lifespanHoursRemaining = prefs.getFloat("current_lifespan_remaining", 0f).toDouble()
        val maxLifespanHours = prefs.getFloat("current_max_lifespan", 0f).toDouble()
        val careMistakes = prefs.getInt("current_care_mistakes", 0)
        val consecutiveBattles = prefs.getInt("current_care_consecutive_battles", 0)
        val lastActiveDay = prefs.getLong("current_last_active_day", 0L)
        val lastCareTick = prefs.getLong("current_last_care_tick", 0L)
        val lastSyncedSteps = prefs.getInt("current_last_synced_steps", 0)
        val consecutiveLosses = prefs.getInt("current_consecutive_losses", 0)
        val criticalRemainingMs = prefs.getLong("current_critical_remaining_ms", 0L)
        val secretCritChance = prefs.getInt("current_secret_crit_chance", 0)
        val secretCritDamage = prefs.getInt("current_secret_crit_damage", 0)
        
        if (characterId == -1) return null
        return MonsterState(cardName, characterId, stage, timeAlive, evolutionTime, attackBonus, healthBonus, speedBonus, defenseBonus, isPaused, currentWins, winsRequired, xp, level, raw, nickname, isBem, attribute, mood, steps, bp, sp, winRatio, trophies, losses, vitalPoints, stageBattles, stageWins, stageVitalPoints, stageTrophies, baseHp, baseAp, lifespanHoursRemaining, maxLifespanHours, careMistakes, consecutiveBattles, lastActiveDay, lastCareTick, lastSyncedSteps, consecutiveLosses, criticalRemainingMs, secretCritChance, secretCritDamage)
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
            .putBoolean("current_is_expired", false)
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
        val careTick = CareManager.recordBattle(toCareState(current), won)
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
            // Name the real killer: critical-loss, genuine overwork mistakes,
            // or a lifespan that simply ran its course (old age) — never blame
            // overwork for a natural end.
            val cause = when {
                !won && current.criticalRemainingMs > 0 -> "critical"
                careTick.newMistakes > 0 -> "overwork"
                else -> "age"
            }
            onDigimonDeath(cause)
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
     * Time ticks can no longer kill — always returns false; only losing
     * while in critical condition (via recordBattleResult) can kill.
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
        // tick.died can no longer happen (no lifespan clock) — kept only as a
        // guard, never as a real path.
        return false
    }

    /**
     * Evolve along the card's REAL tree to [toIndex] (not just +1).
     * Resets per-stage performance counters and reseeds the care clock.
     */
    /**
     * Rolls how many wins this stage needs before digivolving — a fresh random
     * value every stage, never below 1 (0 would block digivolving entirely).
     */
    fun rollWinsRequired(): Int = kotlin.random.Random.nextInt(1, 11)

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
            // New stage, new wins target: reset the counter and roll fresh.
            .putInt("current_wins", 0)
            .putInt("wins_required", rollWinsRequired())
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

    /** Name-based convenience: loads the card first, then delegates. The base
     * chance is cached per card+character so repeated UI reads don't re-parse
     * the DIM file off disk (that was stuttering the dev menu and battles). */
    fun getCritChance(cardName: String, characterId: Int): Float {
        val key = "$cardName|$characterId"
        baseCritCache[key]?.let { return it }
        val card = CardManager(context).getCard(cardName) ?: return 0.15f
        val chance = getCritChance(card, characterId)
        baseCritCache[key] = chance
        return chance
    }

    /**
     * Secret education stats (2026-09-27): hidden crit bonuses earned in the
     * education modes. Lessons grant crit CHANCE, practice floors grant crit
     * DAMAGE. Stored as percentage points on the current monster, like the
     * training bonuses.
     */
    fun getSecretCritChanceBonus(): Int = prefs.getInt("current_secret_crit_chance", 0)
    fun setSecretCritChanceBonus(pct: Int) {
        prefs.edit().putInt("current_secret_crit_chance", pct.coerceAtLeast(0)).apply()
    }
    fun getSecretCritDamageBonus(): Int = prefs.getInt("current_secret_crit_damage", 0)
    fun setSecretCritDamageBonus(pct: Int) {
        prefs.edit().putInt("current_secret_crit_damage", pct.coerceAtLeast(0)).apply()
    }

    /**
     * Effective crit chance: the card's programmed base plus the secret lesson
     * bonus, hard-capped at 50%.
     */
    fun getEffectiveCritChance(cardName: String, characterId: Int): Float {
        return (getCritChance(cardName, characterId) + getSecretCritChanceBonus() / 100f)
            .coerceIn(0f, 0.50f)
    }

    /**
     * Effective crit damage multiplier: 1.5x base plus the secret practice
     * bonus, hard-capped at 1.75x (75% bonus damage).
     */
    fun getEffectiveCritDamageMult(): Float {
        return (1.5f + getSecretCritDamageBonus() / 100f).coerceAtMost(1.75f)
    }

    /**
     * Rolls the secret bonus for one education completion: 1-5%, minus 1% per
     * mistake, never below zero. Returns the granted gain so the UI can report it.
     */
    private fun rollSecretGain(mistakes: Int): Int =
        (kotlin.random.Random.nextInt(1, 6) - mistakes).coerceAtLeast(0)

    /** Call when a lesson's practice question is answered. Returns granted crit-chance %. */
    fun grantLessonCritBonus(mistakes: Int): Int {
        val gain = rollSecretGain(mistakes)
        if (gain > 0) setSecretCritChanceBonus(getSecretCritChanceBonus() + gain)
        return gain
    }

    /** Call when a practice floor is cleared. Returns granted crit-damage %. */
    fun grantPracticeCritBonus(mistakes: Int): Int {
        val gain = rollSecretGain(mistakes)
        if (gain > 0) setSecretCritDamageBonus(getSecretCritDamageBonus() + gain)
        return gain
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
            "current_last_active_day", "current_last_care_tick", "current_last_synced_steps",
            "current_consecutive_losses", "current_critical_remaining_ms", "current_death_cause",
            "current_secret_crit_chance", "current_secret_crit_damage"
        )
        val edit = prefs.edit()
        keys.forEach { edit.remove(it) }
        edit.apply()
        baseCritCache.clear()
    }

    fun isExpired(): Boolean {
        return prefs.getBoolean("current_is_expired", false)
    }

    private fun onDigimonDeath(cause: String) {
        Timber.w("Digimon died ($cause) — clearing monster")
        clearMonster()
        prefs.edit()
            .putBoolean("current_is_expired", true)
            .putString("current_death_cause", cause)
            .apply()
    }

    /** Why the current monster died: "neglect", "overwork", "critical", or "age". */
    fun getDeathCause(): String = prefs.getString("current_death_cause", "neglect") ?: "neglect"

    /**
     * A finished workout always resets the overwork battle counter — regular
     * training protects the Digimon from being worked to death — and heals
     * critical condition when present.
     * @return true if a critical condition was fully healed by this workout.
     */
    fun recordExerciseCompleted(): Boolean {
        val current = getCurrentMonster() ?: return false
        val wasCritical = current.criticalRemainingMs > 0
        // Workouts are real-world activity: the overwork counter resets whether
        // or not the Digimon is critical. recordExercise() is a no-op otherwise.
        var care = toCareState(current).copy(consecutiveBattles = 0)
        care = CareManager.recordExercise(care)
        persistCare(care)
        Timber.d("Workout completed: overwork counter reset; critical ${CareManager.formatCriticalMs(care.criticalRemainingMs)} left")
        return wasCritical && care.criticalRemainingMs <= 0
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

    private fun persistCare(state: CareState) {
        prefs.edit()
            .putFloat("current_lifespan_remaining", state.lifespanHoursRemaining.toFloat())
            .putFloat("current_max_lifespan", state.maxLifespanHours.toFloat())
            .putInt("current_care_mistakes", state.careMistakes)
            .putInt("current_care_consecutive_battles", state.consecutiveBattles)
            .putLong("current_last_active_day", state.lastActiveDay)
            .putInt("current_consecutive_losses", state.consecutiveLosses)
            .putLong("current_critical_remaining_ms", state.criticalRemainingMs)
            .apply()
    }

    fun evolve() {
        val current = getCurrentMonster() ?: return
        
        val candidates = getEvolutionCandidates()
        if (candidates.isEmpty()) {
            Timber.d("No evolution paths found for card ${current.cardName}")
            return
        }
        
        val available = candidates.filter { it.requirementsMet }
        val pick = if (available.isNotEmpty()) {
            available.maxByOrNull { it.path.requiredTrophies * 1000 + it.path.requiredVitalValues }!!
        } else {
            candidates.maxByOrNull { c -> c.progress.count { it.met } } ?: candidates.first()
        }
        
        evolveTo(pick.path.toIndex, pick.path.hoursUntilEvolution)
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
            .putInt("current_wins", 0)
            .putInt("wins_required", rollWinsRequired())
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

    /** Applies the workout power-up and returns the exact deltas as
     *  intArrayOf(atk, hp, spd, def) so the caller can forward them to the
     *  watch — both Digimon get identical gains (2026-09-25). */
    fun applyWorkoutPowerUp(completionRatio: Float): IntArray {
        val current = getCurrentMonster() ?: return intArrayOf(0, 0, 0, 0)
        val baseBonus = (50 * completionRatio).toInt()
        val random = kotlin.random.Random(System.currentTimeMillis())
        val atk = baseBonus + random.nextInt(0, 50)
        val hp = baseBonus + random.nextInt(0, 50)
        val spd = baseBonus + random.nextInt(0, 50)
        val def = baseBonus + random.nextInt(0, 50)

        updateBonusStats(
            atkDelta = atk,
            hpDelta = hp,
            spdDelta = spd,
            defDelta = def
        )
        return intArrayOf(atk, hp, spd, def)
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
