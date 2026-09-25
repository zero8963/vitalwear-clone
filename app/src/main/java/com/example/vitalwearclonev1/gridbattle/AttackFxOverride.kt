package com.example.vitalwearclonev1.gridbattle

import android.content.Context

/**
 * Per-Digimon attack-animation skin: borrow another card/character's base
 * (DIM-programmed small/big) attack animations without changing partners.
 * Only the animation changes — damage still uses your Digimon's own stats.
 *
 * Priority wherever attacks render: NaviCust Core program > this override > own attacks.
 */
data class AttackFxOverride(val cardName: String, val charId: Int)

object AttackFxOverrides {
    private const val PREFS = "gridbattle_attackfx"

    fun load(context: Context, ownerId: String): AttackFxOverride? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(keyFor(ownerId), null) ?: return null
        val cardName = raw.substringBefore("|")
        val charId = raw.substringAfter("|", "").toIntOrNull() ?: return null
        if (cardName.isBlank()) return null
        return AttackFxOverride(cardName, charId)
    }

    /** Pass null to clear back to the partner's own attacks. */
    fun save(context: Context, ownerId: String, override: AttackFxOverride?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (override == null) {
            prefs.edit().remove(keyFor(ownerId)).apply()
        } else {
            prefs.edit().putString(keyFor(ownerId), "${override.cardName}|${override.charId}").apply()
        }
    }

    private fun keyFor(ownerId: String) = "fx_$ownerId"
}
