package com.example.vitalwearclonev1.gridbattle

/**
 * Status-effect payload for the RPG mechanics pass (2026-09-30):
 * burn / poison damage-over-time, flinch (interrupt), and hyper-armor
 * breaking. Shared by battle chips, Digi-Custom coatings, and Net World
 * virus attacks. Currently consumed by the Net World battle engine in
 * MapAdventureActivity; the vs-AI/PvP BattleEngine applies chip damage
 * only (status there is a follow-up slice).
 */
data class HitStatus(
    /** Burn damage per second (already ATK-scaled by the caller). */
    val burnDps: Float = 0f,
    /** How long the burn ticks, in seconds. */
    val burnSecs: Float = 0f,
    /** Poison damage per second (already ATK-scaled by the caller). */
    val poisonDps: Float = 0f,
    /** How long the poison ticks, in seconds. */
    val poisonSecs: Float = 0f,
    /** 0..1 chance the hit tries to interrupt the target. */
    val flinchChance: Float = 0f,
    /** Flinch power — must exceed the target's hyper armor to interrupt. */
    val flinchPower: Float = 0f,
    /** Seconds the target's hyper armor is stripped (0 = no break). */
    val armorBreakSecs: Float = 0f
) {
    val isEmpty: Boolean
        get() = burnSecs <= 0f && poisonSecs <= 0f &&
            (flinchChance <= 0f || flinchPower <= 0f) && armorBreakSecs <= 0f
}

/** Net World virus behaviors (2026-09-30). Rolled per encounter. */
enum class VirusVariant {
    NORMAL,
    /** Applies burn with its shots; shrugs off weak flinch. */
    BURNER,
    /** Applies poison with its shots; shrugs off weak flinch. */
    POISONER,
    /** High HP + hyper armor: cannot be stun-locked, answer with DoT. */
    BRUISER,
    /** Resists buster/sword basics: the answer is battle chips. */
    WARDEN
}
