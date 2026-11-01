package com.example.vitalwearclonev1.game

import android.content.Context

/**
 * SharedPreferences wrapper for FormulaMasteryState (2026-11-01).
 * This is deliberately NOT LessonHistoryManager: that store only tracks
 * seen/not-seen, and clearing it on pool exhaustion would erase mastery.
 */
class FormulaMasteryManager(context: Context) {
    private val prefs = context.getSharedPreferences("formula_mastery_prefs", Context.MODE_PRIVATE)
    private val state = FormulaMasteryState().also { it.restore(prefs.getStringSet(KEY, emptySet()) ?: emptySet()) }

    fun level(cardId: String): Int = state.level(cardId)

    fun isMasteredCard(cardId: String): Boolean = state.isMasteredCard(cardId)

    fun isUnitMastered(unit: FormulaUnit): Boolean = state.isUnitMastered(unit)

    fun masteredUnitIds(units: List<FormulaUnit> = FORMULA_UNITS): Set<String> = state.masteredUnitIds(units)

    fun recordAnswer(cardId: String, form: FormulaForm, correct: Boolean) {
        state.record(cardId, form, correct)
        prefs.edit().putStringSet(KEY, state.serialize()).apply()
    }

    /** Units currently open for a learner entering with [entryBand]. */
    fun unlockedUnits(entryBand: FormulaBand): List<FormulaUnit> =
        computeUnlockedUnits(entryBand, masteredUnitIds())

    companion object {
        private const val KEY = "formula_mastery"
    }
}
