package com.example.vitalwearclonev1.communication

import timber.log.Timber

/**
 * Evolution requirements tracker for VB classic (product 2).
 *
 * Requirement numbers come from the official Vital Bracelet Arena dex
 * (Namakemon → Ultimate "Change Requirements", screenshotted 2026-09-27).
 * The dex is the VBBE app, so its "PP" maps 1:1 to classic trophies —
 * everything else (vitals, battles, win rate, 1440 min) is identical.
 *
 *   Path A: 12 trophies, 3000 vitals, 15 battles, 70% win rate
 *   Path B: 10 trophies, 2500 vitals, 10 battles, 50% win rate
 *   Path C:  6 trophies, 2000 vitals, 10 battles, 50% win rate
 *
 * The bracelet grants the HIGHEST path the Digimon qualifies for (highest
 * vital requirement met first), so the tracker shows all three side by
 * side — aim at exactly one tier or you'll overshoot into another.
 *
 * All current values are read live from the backup's decrypted blob
 * (offsets verified on the real Hero 2026-09-27), so this works for any
 * backup, old or new. Read-only: never writes anything.
 */
object VBBraceletEvoTracker {

    data class EvoPath(
        val id: String,
        /** Short label, e.g. "12🏆 path". Dex names are ??? silhouettes. */
        val label: String,
        val trophies: Int,
        val vitals: Int,
        val battles: Int,
        val winRate: Int
    )

    val PATHS = listOf(
        EvoPath("a", "12\ud83c\udfc6 path", trophies = 12, vitals = 3000, battles = 15, winRate = 70),
        EvoPath("b", "10\ud83c\udfc6 path", trophies = 10, vitals = 2500, battles = 10, winRate = 50),
        EvoPath("c", "6\ud83c\udfc6 path", trophies = 6, vitals = 2000, battles = 10, winRate = 50)
    )

    data class Current(
        val trophies: Int,
        val vitals: Int,
        val wins: Int,
        val losses: Int,
        val winRate: Int,
        /** Next-timer minutes remaining. Null when the field isn't parseable. */
        val timerMinutes: Int?
    ) {
        val battles: Int get() = wins + losses
    }

    data class Requirement(
        val label: String,
        val current: Int,
        val target: Int,
        val suffix: String = ""
    ) {
        val met: Boolean get() = current >= target
        /** How many more needed (0 when met). */
        val deficit: Int get() = (target - current).coerceAtLeast(0)
    }

    data class PathReport(
        val path: EvoPath,
        val requirements: List<Requirement>,
        val allMet: Boolean
    )

    /** Read the tracker's current values straight from a decrypted blob. */
    fun readCurrent(plain: ByteArray, productId: Int): Current? {
        if (productId != 2) return null
        if (plain.size != VBBraceletData.DATA_SIZE) return null
        return try {
            fun f(id: String): Int = VBBraceletData.readField(
                plain, VBBraceletData.FIELDS.first { it.id == id }
            )
            val wins = f("braceletWins")
            val losses = f("braceletLosses")
            val total = wins + losses
            Current(
                trophies = f("braceletTrophies"),
                vitals = f("vital"),
                wins = wins,
                losses = losses,
                winRate = if (total > 0) wins * 100 / total else f("braceletWinRate"),
                timerMinutes = f("nextTimer")
            )
        } catch (e: Exception) {
            Timber.w(e, "VBBraceletEvoTracker: blob parse failed")
            null
        }
    }

    fun report(current: Current, path: EvoPath): PathReport {
        val reqs = listOf(
            Requirement("Trophies \ud83c\udfc6", current.trophies, path.trophies),
            Requirement("Vitals", current.vitals, path.vitals),
            Requirement("Battles", current.battles, path.battles),
            Requirement("Win rate", current.winRate, path.winRate, "%")
        )
        return PathReport(path, reqs, reqs.all { it.met })
    }

    fun reportAll(current: Current): List<PathReport> = PATHS.map { report(current, it) }
}
