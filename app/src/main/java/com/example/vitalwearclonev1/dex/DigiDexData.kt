package com.example.vitalwearclonev1.dex

import android.content.Context
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.card.DimCardAdapter
import com.example.vitalwearclonev1.communication.VBBraceletSpeciesMap
import timber.log.Timber

/**
 * DigiDex data layer (2026-09-29).
 *
 * Shows the evolution trees of installed DIM/BEM cards so the user can plan
 * evolutions: which Digimon each card contains, what each one evolves into,
 * and the bracelet requirements (VP, trophies, battles, win rate) for each path.
 *
 * Names come from the device-verified species maps where available; unknown
 * slots fall back to "Slot N".
 */
object DigiDexData {

    data class DexEvolution(
        val toIndex: Int,
        val toName: String,
        val requiredVitalPoints: Int,
        val requiredTrophies: Int,
        val requiredBattles: Int,
        val requiredWinRate: Int,
        val hoursRequired: Int
    )

    data class DexEntry(
        val index: Int,
        val name: String,
        val stage: Int,
        val evolutions: List<DexEvolution>
    )

    data class DexCard(
        val cardName: String,
        val entries: List<DexEntry>
    )

    /** Impulse City roster: b9 (roster index) -> name, from device-verified map. */
    private val IMPULSE_CITY_NAMES: Map<Int, String> =
        VBBraceletSpeciesMap.IMPULSE_CITY.entries.associate { (name, bytes) -> bytes.b9 to name }

    /** Impulse City is DIM ID 1. */
    private const val IMPULSE_CITY_DIM_ID = 1

    private fun isImpulseCity(cardName: String, dimId: Int?): Boolean {
        // Prefer the DIM ID from the card header (robust); fall back to name match.
        if (dimId == IMPULSE_CITY_DIM_ID) return true
        return cardName.contains("impulse", ignoreCase = true)
    }

    private fun nameFor(cardName: String, dimId: Int?, index: Int): String {
        if (isImpulseCity(cardName, dimId)) {
            IMPULSE_CITY_NAMES[index]?.let { return it }
        }
        return "Slot $index"
    }

    private fun stageFor(cardName: String, dimId: Int?, index: Int): Int {
        if (isImpulseCity(cardName, dimId)) {
            IMPULSE_CITY_NAMES[index]?.let { name ->
                return VBBraceletSpeciesMap.IMPULSE_CITY[name]?.stage ?: -1
            }
        }
        return -1
    }

    fun loadCard(context: Context, cardName: String): DexCard? {
        val card = try {
            CardManager(context).getCard(cardName)
        } catch (t: Throwable) {
            Timber.e(t, "DigiDex: failed to load card $cardName")
            null
        } ?: return null

        val count = try {
            DimCardAdapter.getCharacterCount(card)
        } catch (t: Throwable) {
            Timber.e(t, "DigiDex: failed to get character count for $cardName")
            return null
        }

        val dimId = try {
            card.header.dimId
        } catch (t: Throwable) {
            Timber.w(t, "DigiDex: couldn't read dimId for $cardName")
            null
        }

        val entries = (0 until count).map { index ->
            val paths = try {
                DimCardAdapter.getEvolutionPaths(card, index)
            } catch (t: Throwable) {
                Timber.e(t, "DigiDex: failed to get paths for $cardName#$index")
                emptyList()
            }
            DexEntry(
                index = index,
                name = nameFor(cardName, dimId, index),
                stage = stageFor(cardName, dimId, index),
                evolutions = paths.map { path ->
                    DexEvolution(
                        toIndex = path.toIndex,
                        toName = nameFor(cardName, dimId, path.toIndex),
                        requiredVitalPoints = path.requiredVitalValues,
                        requiredTrophies = path.requiredTrophies,
                        requiredBattles = path.requiredBattles,
                        requiredWinRate = path.requiredWinRatio,
                        hoursRequired = path.hoursUntilEvolution
                    )
                }
            )
        }
        return DexCard(cardName, entries)
    }

    fun listCards(context: Context): List<String> {
        return try {
            CardManager(context).listCards()
        } catch (t: Throwable) {
            Timber.e(t, "DigiDex: failed to list cards")
            emptyList()
        }
    }

    /** Human-readable stage name. */
    fun stageName(stage: Int): String = when (stage) {
        0 -> "Baby I"
        1 -> "Baby II"
        2 -> "Child"
        3 -> "Adult"
        4 -> "Perfect"
        5 -> "Ultimate"
        else -> ""
    }
}
