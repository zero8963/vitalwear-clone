package com.example.vitalwearclonev1.card

import com.github.cfogrady.vb.dim.card.Card
import com.github.cfogrady.vb.dim.character.DimStats
import com.github.cfogrady.vb.dim.transformation.DimEvolutionRequirements
import com.github.cfogrady.vb.dim.transformation.TransformationRequirements
import timber.log.Timber

/**
 * A Digimon's baked-in base stats, exactly as written into the DIM/BEM card.
 * These are the values the card author set per character — the app now applies
 * them when a monster loads instead of using flat defaults.
 */
data class DigimonBaseStats(
    val characterIndex: Int,
    val stage: Int,
    val attribute: Int,
    val type: Int,
    val hp: Int,
    val ap: Int,
    val dp: Int,
    val smallAttackId: Int,
    val bigAttackId: Int,
    val firstPoolBattleChance: Int,
    val secondPoolBattleChance: Int,
    val unlockRequired: Boolean,
    val dpStars: Int
)

/**
 * One branch of a card's evolution tree, read from the card's real
 * transformation requirements. fromIndex -> toIndex is NOT always +1;
 * DIM cards branch (e.g. one rookie can become several different champions
 * depending on which requirements the player met).
 */
data class EvolutionPath(
    val fromIndex: Int,
    val toIndex: Int,
    /** Vital Values (VP) — earned through exercise/activity. */
    val requiredVitalValues: Int,
    val requiredTrophies: Int,
    val requiredBattles: Int,
    /** Win ratio as a percent (0-100). */
    val requiredWinRatio: Int,
    /** Minimum hours at the current stage before this evolution unlocks. */
    val hoursUntilEvolution: Int,
    val hasNextStage: Boolean
)

/**
 * Translates the VB-DIM-Reader [Card] model into the plain data classes the
 * evolution engine consumes. Works for both DIM and BEM cards.
 */
object DimCardAdapter {

    fun getBaseStats(card: Card<*, *, *, *, *, *>, characterIndex: Int): DigimonBaseStats? {
        val entries = card.characterStats.characterEntries
        val entry = entries.getOrNull(characterIndex)
        if (entry == null) {
            Timber.w("DimCardAdapter: no stat entry for character $characterIndex")
            return null
        }
        val dimBlock = entry as? DimStats.DimStatBlock
        return DigimonBaseStats(
            characterIndex = characterIndex,
            stage = entry.stage,
            attribute = entry.attribute,
            type = entry.type,
            hp = entry.hp,
            ap = entry.ap,
            dp = entry.dp,
            smallAttackId = entry.smallAttackId,
            bigAttackId = entry.bigAttackId,
            firstPoolBattleChance = entry.firstPoolBattleChance,
            secondPoolBattleChance = entry.secondPoolBattleChance,
            unlockRequired = dimBlock?.isUnlockRequired ?: false,
            dpStars = dimBlock?.dpStars ?: 0
        )
    }

    /**
     * All evolution options out of [characterIndex] on this card. Empty means
     * this character is a dead end (final stage) on this card.
     */
    fun getEvolutionPaths(card: Card<*, *, *, *, *, *>, characterIndex: Int): List<EvolutionPath> {
        val reqs: TransformationRequirements<*> = card.transformationRequirements
        return reqs.transformationEntries
            .filter { it.fromCharacterIndex == characterIndex }
            .map { entry ->
                val dimBlock = entry as? DimEvolutionRequirements.DimEvolutionRequirementBlock
                EvolutionPath(
                    fromIndex = entry.fromCharacterIndex,
                    toIndex = entry.toCharacterIndex,
                    requiredVitalValues = entry.requiredVitalValues,
                    requiredTrophies = entry.requiredTrophies,
                    requiredBattles = entry.requiredBattles,
                    requiredWinRatio = entry.requiredWinRatio,
                    hoursUntilEvolution = dimBlock?.hoursUntilEvolution ?: 1,
                    hasNextStage = dimBlock?.hasNextStage() ?: true
                )
            }
    }

    fun getCharacterCount(card: Card<*, *, *, *, *, *>): Int =
        card.characterStats.characterEntries.size
}
