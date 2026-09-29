package com.example.vitalwearclonev1.monster

import com.example.vitalwearclonev1.card.EvolutionPath

/**
 * Everything the evolution tree cares about, measured for the CURRENT stage.
 * The engine compares this against each [EvolutionPath]'s requirements.
 *
 * Pure Kotlin — no Android dependencies — so the watch module can share it.
 */
data class PerformanceSnapshot(
    val vitalPoints: Int,
    val trophies: Int,
    val battles: Int,
    val wins: Int,
    /** Hours the monster has been at its current stage. */
    val hoursAtStage: Double
) {
    val winRatioPercent: Int
        get() = if (battles > 0) (wins * 100 / battles) else 0
}

data class RequirementProgress(
    val label: String,
    val current: Number,
    val required: Number,
    val unit: String = ""
) {
    val met: Boolean
        get() = current.toDouble() >= required.toDouble()

    override fun toString(): String = "$label: $current/$required$unit"
}

data class EvolutionCandidate(
    val path: EvolutionPath,
    val progress: List<RequirementProgress>
) {
    val requirementsMet: Boolean
        get() = progress.all { it.met }
}

/**
 * Evaluates a card's real evolution tree against the player's performance.
 * A monster may have MULTIPLE available evolutions (branching tree) — the UI
 * can offer the choice instead of forcing characterId + 1.
 *
 * Timer is bypassed by default (ignoreTimer=true): the user decides when to
 * digivolve, so the bracelet's 1440-minute gate doesn't apply. The Time row
 * still shows for reference but never blocks evolution.
 */
object EvolutionEngine {

    fun evaluate(
        paths: List<EvolutionPath>,
        snap: PerformanceSnapshot,
        ignoreTimer: Boolean = true
    ): List<EvolutionCandidate> {
        return paths.map { path ->
            val timeProgress = RequirementProgress(
                "Time",
                (snap.hoursAtStage * 10).toInt() / 10.0,
                path.hoursUntilEvolution.toDouble(),
                "h"
            )
            val progress = listOf(
                RequirementProgress("Vital Points", snap.vitalPoints, path.requiredVitalValues, " VP"),
                RequirementProgress("Trophies", snap.trophies, path.requiredTrophies),
                RequirementProgress("Battles", snap.battles, path.requiredBattles),
                RequirementProgress("Win Ratio", snap.winRatioPercent, path.requiredWinRatio, "%"),
                // Timer never blocks when ignored — zero the requirement so
                // it's always met, shown as informational only in the UI.
                if (ignoreTimer) timeProgress.copy(required = 0) else timeProgress
            )
            EvolutionCandidate(path, progress)
        }
    }

    /** Only the evolutions whose requirements are fully met. */
    fun availableEvolutions(
        paths: List<EvolutionPath>,
        snap: PerformanceSnapshot,
        ignoreTimer: Boolean = true
    ): List<EvolutionCandidate> = evaluate(paths, snap, ignoreTimer).filter { it.requirementsMet }

    /**
     * True when the monster is at the end of its evolution tree — no path
     * leads onward from the current species. Used to lock the evolve option
     * and prevent accidental writes past the final form (sprite breakage).
     */
    fun isAtMaxEvolution(paths: List<EvolutionPath>): Boolean =
        paths.isEmpty() || paths.none { it.hasNextStage }
}
