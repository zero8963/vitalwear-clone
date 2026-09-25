package com.example.vitalwearclonev1.gridbattle

import android.content.Context

/**
 * Chip Folder (deck) for Grid Battle mode (2026-09-25).
 *
 * The player programs a 30-chip deck before battle; chips are drawn and used
 * as power-ups during the real-time grid fight. Rules are Battle Network-like
 * and deliberately balance-tunable — exact numbers get a tuning pass with
 * playtesters later.
 *
 * Folder rules:
 * - Exactly [FOLDER_SIZE] chips to be battle-ready (an incomplete folder can
 *   be saved, but [validate] reports it and battle will refuse it later).
 * - Max [MAX_COPIES] copies of the same chip id.
 * - Max [MAX_MEGA] MEGA chips.
 * - Max [MAX_GIGA] GIGA chips.
 */
data class ChipFolder(val chipIds: List<Int>) {

    companion object {
        const val FOLDER_SIZE = 30
        const val MAX_COPIES = 4
        const val MAX_MEGA = 5
        const val MAX_GIGA = 2

        private const val PREFS = "chipfolder_prefs"

        private fun key(ownerId: String) =
            "folder_" + ownerId.replace(Regex("[^A-Za-z0-9_]"), "_")

        fun load(context: Context, ownerId: String): ChipFolder {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(key(ownerId), "") ?: ""
            if (raw.isBlank()) return ChipFolder(emptyList())
            val ids = raw.split(",").mapNotNull { tok ->
                try {
                    val id = tok.toInt()
                    if (ChipLibrary.byId(id) == null) null else id
                } catch (e: Exception) {
                    null
                }
            }
            return ChipFolder(ids)
        }
    }

    fun save(context: Context, ownerId: String) {
        val s = chipIds.joinToString(",")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(ownerId), s)
            .apply()
    }

    /** Chips resolved against the library, in folder order. */
    fun chips(): List<BattleChip> = chipIds.mapNotNull { ChipLibrary.byId(it) }

    fun totalMb(): Int = chips().sumOf { it.mb }

    /** Human-readable problems; empty means the folder is battle-ready. */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()
        if (chipIds.size != FOLDER_SIZE) {
            problems += "Folder needs exactly $FOLDER_SIZE chips " +
                "(currently ${chipIds.size})."
        }
        chipIds.groupingBy { it }.eachCount()
            .filter { it.value > MAX_COPIES }
            .forEach { (id, count) ->
                val name = ChipLibrary.byId(id)?.name ?: "Chip #$id"
                problems += "\"$name\" appears $count times (max $MAX_COPIES)."
            }
        val megas = chips().count { it.tier == ChipTier.MEGA }
        if (megas > MAX_MEGA) problems += "Too many MEGA chips: $megas (max $MAX_MEGA)."
        val gigas = chips().count { it.tier == ChipTier.GIGA }
        if (gigas > MAX_GIGA) problems += "Too many GIGA chips: $gigas (max $MAX_GIGA)."
        return problems
    }

    val isBattleReady: Boolean get() = validate().isEmpty()

    /**
     * Why [chipId] can't be added right now, or null when it's fine.
     * Used to disable the picker's Add buttons with a kid-readable reason.
     */
    fun addBlockReason(chipId: Int): String? {
        val chip = ChipLibrary.byId(chipId) ?: return "Unknown chip"
        if (chipIds.size >= FOLDER_SIZE) return "Folder full ($FOLDER_SIZE)"
        val copies = chipIds.count { it == chipId }
        if (copies >= MAX_COPIES) return "Max $MAX_COPIES copies"
        if (chip.tier == ChipTier.MEGA &&
            chips().count { it.tier == ChipTier.MEGA } >= MAX_MEGA
        ) return "Max $MAX_MEGA megas"
        if (chip.tier == ChipTier.GIGA &&
            chips().count { it.tier == ChipTier.GIGA } >= MAX_GIGA
        ) return "Max $MAX_GIGA gigas"
        return null
    }
}
