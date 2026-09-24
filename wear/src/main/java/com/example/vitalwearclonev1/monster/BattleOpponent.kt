package com.example.vitalwearclonev1.monster

import android.graphics.Bitmap

data class BattleOpponent(
    val cardName: String,
    val characterId: Int,
    val atk: Int,
    val hp: Int,
    val spd: Int,
    val def: Int,
    val isBoss: Boolean = false,
    val name: String = "Enemy",
    val seed: Long = System.currentTimeMillis(),
    val isInitiator: Boolean = false,
    val isBem: Boolean = false,
    val dimId: Int = 0,
    val dimHash: String? = null,
    val remoteSpriteUrls: Map<String, String>? = null
)
