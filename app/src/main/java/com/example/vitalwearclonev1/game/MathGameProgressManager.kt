package com.example.vitalwearclonev1.game

import android.content.Context
import android.content.SharedPreferences

class MathGameProgressManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("math_game_prefs", Context.MODE_PRIVATE)

    fun getHighestClearedFloor(mode: String, grade: String): Int {
        return prefs.getInt("${mode}_${grade}_highest_cleared_floor", 0)
    }

    fun markFloorCleared(mode: String, grade: String, floor: Int) {
        val current = getHighestClearedFloor(mode, grade)
        if (floor > current) {
            prefs.edit().putInt("${mode}_${grade}_highest_cleared_floor", floor).apply()
        }
    }

    fun isFloorUnlocked(mode: String, grade: String, floor: Int): Boolean {
        if (floor <= 1) return true
        return floor <= getHighestClearedFloor(mode, grade) + 1
    }
}
