package com.example.vitalwearclonev1.game

import android.content.Context

/**
 * SharedPreferences wrapper for SpellingMasteryState (2026-11-01).
 * Deliberately separate from FormulaMasteryManager (own prefs file
 * and key) and from LessonHistoryManager (seen-only, clears on pool
 * exhaustion — never use it for mastery).
 */
class SpellingMasteryManager(context: Context) {
    private val prefs = context.getSharedPreferences("spelling_mastery_prefs", Context.MODE_PRIVATE)
    private val state = SpellingMasteryState().also { it.restore(prefs.getStringSet(KEY, emptySet()) ?: emptySet()) }

    fun level(cardId: String): Int = state.level(cardId)

    fun isMasteredCard(cardId: String): Boolean = state.isMasteredCard(cardId)

    fun isUnitMastered(unit: SpellingUnit): Boolean = state.isUnitMastered(unit)

    fun masteredUnitIds(units: List<SpellingUnit> = SPELLING_UNITS): Set<String> = state.masteredUnitIds(units)

    fun recordAnswer(cardId: String, form: SpellingForm, correct: Boolean) {
        state.record(cardId, form, correct)
        prefs.edit().putStringSet(KEY, state.serialize()).apply()
    }

    /** Units currently open for a learner entering with [entryBand]. */
    fun unlockedUnits(entryBand: SpellingBand): List<SpellingUnit> =
        computeUnlockedSpellingUnits(entryBand, masteredUnitIds())

    companion object {
        private const val KEY = "spelling_mastery"
    }
}
