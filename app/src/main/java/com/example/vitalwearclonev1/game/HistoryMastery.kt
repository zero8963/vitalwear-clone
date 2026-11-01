package com.example.vitalwearclonev1.game

import android.content.Context

/**
 * SharedPreferences wrapper for HistoryMasteryState (2026-11-01).
 * Deliberately separate from FormulaMasteryManager and
 * SpellingMasteryManager (own prefs file and key) and from
 * LessonHistoryManager (seen-only, clears on pool exhaustion —
 * never use it for mastery).
 */
class HistoryMasteryManager(context: Context) {
    private val prefs = context.getSharedPreferences("history_mastery_prefs", Context.MODE_PRIVATE)
    private val state = HistoryMasteryState().also { it.restore(prefs.getStringSet(KEY, emptySet()) ?: emptySet()) }

    fun level(cardId: String): Int = state.level(cardId)

    fun isMasteredCard(cardId: String): Boolean = state.isMasteredCard(cardId)

    fun isUnitMastered(unit: HistoryUnit): Boolean = state.isUnitMastered(unit)

    fun masteredUnitIds(units: List<HistoryUnit> = HISTORY_UNITS): Set<String> = state.masteredUnitIds(units)

    fun recordAnswer(cardId: String, form: HistoryForm, correct: Boolean) {
        state.record(cardId, form, correct)
        prefs.edit().putStringSet(KEY, state.serialize()).apply()
    }

    /** Units currently open for a learner entering with [entryBand]. */
    fun unlockedUnits(entryBand: HistoryBand): List<HistoryUnit> =
        computeUnlockedHistoryUnits(entryBand, masteredUnitIds())

    companion object {
        private const val KEY = "history_mastery"
    }
}
