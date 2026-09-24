package com.example.vitalwearclonev1.game

import android.content.Context

/**
 * Persists which lesson titles a learner has already seen, per subject (mode) and grade.
 * This is what stops lesson mode from repeating the same handful of lessons every time
 * the screen is reopened: the "seen" memory now survives app restarts, not just the
 * current session. When every lesson in a pool has been seen, the caller can start a
 * fresh cycle via [clearHistory].
 */
class LessonHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("lesson_history_prefs", Context.MODE_PRIVATE)

    fun getSeenTitles(mode: String, grade: String): Set<String> {
        return prefs.getStringSet(key(mode, grade), emptySet()) ?: emptySet()
    }

    fun markTitleSeen(mode: String, grade: String, title: String) {
        val updated = getSeenTitles(mode, grade).toMutableSet().apply { add(title) }
        prefs.edit().putStringSet(key(mode, grade), updated).apply()
    }

    fun clearHistory(mode: String, grade: String) {
        prefs.edit().remove(key(mode, grade)).apply()
    }

    private fun key(mode: String, grade: String) = "seen_${mode}_${grade}"
}
