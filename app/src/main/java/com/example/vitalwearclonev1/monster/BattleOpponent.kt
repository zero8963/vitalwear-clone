package com.example.vitalwearclonev1.monster

data class BattleOpponent(
    val cardName: String,
    val characterId: Int,
    val atk: Int,
    val hp: Int,
    val spd: Int,
    val def: Int,
    val name: String = "Rival Digimon",
    val seed: Long = System.currentTimeMillis(),
    val isInitiator: Boolean = false,
    val isBem: Boolean = false,
    val dimId: Int = 0,
    val dimHash: String? = null,
    val remoteSpriteUrls: Map<String, String>? = null
)
