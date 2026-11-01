package com.example.vitalwearclonev1.game

/**
 * Pure spaced-repetition state for History Cards (2026-11-01).
 * Mirrors SpellingMasteryState; no Android types, unit-testable on
 * the JVM. See HistoryMasteryManager for the SharedPreferences
 * wrapper. CardMastery is shared with the formula/spelling stores
 * (same package, same shape); the stores themselves are separate,
 * so a history reset can never touch formula or spelling mastery.
 *
 * Level rule, with one deliberate extension of the formula/spelling
 * pattern: history has two deep skills, so BOTH ORDER and CAUSE
 * count as performance forms (two consecutive correct ORDER-or-
 * CAUSE answers raise a level). A miss drops a level no matter the
 * form. NAME/DATE correct only refresh last-seen and leave the
 * performance streak intact, so units are mastered through battles
 * (ORDER/CAUSE), not by re-reading lessons.
 */
class HistoryMasteryState {
    private val byCard = mutableMapOf<String, CardMastery>()

    fun get(cardId: String): CardMastery = byCard[cardId] ?: CardMastery()

    fun level(cardId: String): Int = get(cardId).level

    fun isMasteredCard(cardId: String): Boolean = level(cardId) >= 3

    fun isUnitMastered(unit: HistoryUnit): Boolean =
        unit.cardIds.isNotEmpty() && unit.cardIds.all { level(it) >= 2 }

    fun masteredUnitIds(units: List<HistoryUnit>): Set<String> =
        units.filter { isUnitMastered(it) }.map { it.id }.toSet()

    fun record(cardId: String, form: HistoryForm, correct: Boolean, nowMs: Long = System.currentTimeMillis()) {
        val cur = get(cardId)
        val next = if (!correct) {
            cur.copy(
                level = maxOf(0, cur.level - 1),
                misses = cur.misses + 1,
                lastSeenMs = nowMs,
                consecutiveApplies = 0
            )
        } else if (form == HistoryForm.ORDER || form == HistoryForm.CAUSE) {
            val consec = cur.consecutiveApplies + 1
            if (consec >= 2) {
                cur.copy(
                    level = minOf(3, cur.level + 1),
                    lastSeenMs = nowMs,
                    consecutiveApplies = 0
                )
            } else {
                cur.copy(lastSeenMs = nowMs, consecutiveApplies = consec)
            }
        } else {
            cur.copy(lastSeenMs = nowMs)
        }
        byCard[cardId] = next
    }

    /** Serialize as "id:level:misses:lastSeen:consec" entries, for prefs storage. */
    fun serialize(): Set<String> = byCard.map { (id, m) ->
        "$id:${m.level}:${m.misses}:${m.lastSeenMs}:${m.consecutiveApplies}"
    }.toSet()

    fun restore(serialized: Set<String>) {
        byCard.clear()
        for (entry in serialized) {
            val parts = entry.split(":")
            if (parts.size != 5) continue
            val level = parts[1].toIntOrNull() ?: continue
            val misses = parts[2].toIntOrNull() ?: continue
            val lastSeen = parts[3].toLongOrNull() ?: continue
            val consec = parts[4].toIntOrNull() ?: continue
            byCard[parts[0]] = CardMastery(level, misses, lastSeen, consec)
        }
    }
}
