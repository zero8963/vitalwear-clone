package com.example.vitalwearclonev1.communication

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.card.DimCardAdapter
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.example.vitalwearclonev1.gridbattle.charBaseIndex
import com.example.vitalwearclonev1.lab.LabStorage
import com.example.vitalwearclonev1.lab.StoredMonster
import com.github.cfogrady.vb.dim.card.BemCard
import timber.log.Timber

/**
 * "Adopt bracelet backup as partner": turn an extracted bracelet character
 * into a functional DigiLab partner.
 *
 * The bracelet blob carries no species ID (that's the pending diff-hunt), so
 * the user picks the species from the app's imported DIM/BEM card roster —
 * the same roster + sprite-thumbnail pattern as the Attack FX picker. The
 * pick is remembered per backup (SharedPreferences), but nothing is ever
 * prefilled: the app cannot know which Digimon a blob was.
 *
 * Stat model note: StoredMonster's attack/calories/speed/defense are
 * *bonus* fields. The wake flow (DigimonLabActivity.onSetHome ->
 * PhoneMonsterManager.setCurrentMonster) re-derives the species' base HP/AP
 * from the DIM card via applyCardBaseStats, and battles use
 * base + bonus. Seeding base stats into the bonus fields would double-count
 * (an adopted twin would run at 2x a hatched one), so an adopted partner
 * starts at 0 bonuses — exactly like a fresh hatch — and the card supplies
 * the base stats on wake. Mood comes from the bracelet's mental (0-100).
 *
 * Purely app-side: never touches the bracelet, never alters the backup.
 */
object VBBraceletAdopt {

    data class AdoptCandidate(
        val cardName: String,
        val charId: Int,
        val stage: Int,
        val attribute: Int,
        val sprite: Bitmap?
    ) {
        val label: String get() = "$cardName · #$charId"
    }

    /** Vital Bracelet stage order, index = stage int. */
    val STAGE_NAMES = listOf("Baby I", "Baby II", "Child", "Adult", "Perfect", "Ultimate")

    private const val PREFS = "bracelet_adopt_species"

    /**
     * Every card character across imported DIM/BEM cards.
     * Call off the main thread (reads card files + decodes sprites).
     */
    fun loadCandidates(context: Context): List<AdoptCandidate> {
        val out = mutableListOf<AdoptCandidate>()
        try {
            val cm = CardManager(context)
            for (name in cm.listCards().filter { it.isNotBlank() }) {
                try {
                    val card = cm.getCard(name) ?: continue
                    val count = try {
                        card.characterStats.characterEntries.size
                    } catch (e: Exception) { 0 }
                    if (count <= 0) continue
                    val isBem = card is BemCard
                    val sprites = try { card.spriteData.sprites } catch (e: Exception) { null }
                    for (i in 0 until count) {
                        val stats = try {
                            DimCardAdapter.getBaseStats(card, i)
                        } catch (e: Exception) { null }
                        val bmp = try {
                            val base = charBaseIndex(i, isBem)
                            sprites?.getOrNull(base + 1)?.let { SpriteBitmapHandler.getBitmap(it) }
                        } catch (e: Exception) { null }
                        out.add(
                            AdoptCandidate(
                                cardName = name,
                                charId = i,
                                stage = stats?.stage?.coerceIn(0, 5) ?: 2,
                                attribute = stats?.attribute ?: 0,
                                sprite = bmp
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.w(e, "VBBraceletAdopt skipping unreadable card $name")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "VBBraceletAdopt: no cards readable")
        }
        return out
    }

    fun rememberChoice(context: Context, backupId: String, cardName: String, charId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(backupId, "$cardName#$charId")
            .apply()
    }

    /** The species previously chosen for this backup, or null. */
    fun rememberedChoice(context: Context, backupId: String): Pair<String, Int>? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(backupId, null) ?: return null
        val cardName = raw.substringBeforeLast("#")
        val charId = raw.substringAfterLast("#").toIntOrNull() ?: return null
        if (cardName.isBlank()) return null
        return cardName to charId
    }

    /**
     * Build the StoredMonster and add it to the DigiLab. A fresh arrival:
     * level 1, no xp, no training bonuses, mood from the bracelet.
     */
    fun adopt(
        context: Context,
        backup: VBBraceletBackups.Backup,
        candidate: AdoptCandidate,
        nickname: String,
        stage: Int
    ): StoredMonster {
        val mental = backup.fields["mental"]?.coerceIn(0, 100) ?: 50
        val monster = StoredMonster(
            name = candidate.cardName,
            charId = candidate.charId,
            stage = stage.coerceIn(0, 5),
            attack = 0,
            calories = 0,
            speed = 0,
            defense = 0,
            xp = 0,
            level = 1,
            rawPayload = "bracelet-adopt:${backup.id}",
            nickname = nickname.ifBlank { null },
            currentWins = 0,
            winsRequired = 0,
            timeAlive = 0,
            evolutionTime = 3600,
            attribute = candidate.attribute,
            mood = mental
        )
        LabStorage.addMonster(context, monster)
        rememberChoice(context, backup.id, candidate.cardName, candidate.charId)
        val display = nickname.ifBlank { "Digimon" }
        Toast.makeText(context, "$display joined your DigiLab!", Toast.LENGTH_LONG).show()
        Timber.d(
            "VBBraceletAdopt: backup ${backup.id} adopted as " +
                    "${candidate.cardName}#${candidate.charId} stage=$stage mood=$mental"
        )
        return monster
    }
}
