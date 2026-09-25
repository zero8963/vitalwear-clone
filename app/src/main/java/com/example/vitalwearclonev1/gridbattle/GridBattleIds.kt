package com.example.vitalwearclonev1.gridbattle

/**
 * Shared per-Digimon ownerId for all Grid Battle saves (NaviCust loadouts,
 * chip folders). Every screen — GridBattleActivity tabs AND adventure mode —
 * must build it the same way so they read and write the same profile.
 */
fun ownerIdFor(nickname: String?, cardName: String, charId: Int): String {
    val base = nickname?.takeIf { it.isNotBlank() } ?: cardName
    return "${base}_${charId}"
}
