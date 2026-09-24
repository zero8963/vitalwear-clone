package com.example.vitalwearclonev1.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.EmptyComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.vitalwearclonev1.MainActivity
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.example.vitalwearclonev1.monster.CareManager
import com.example.vitalwearclonev1.monster.MonsterManager
import com.github.cfogrady.vb.dim.card.BemCard
import timber.log.Timber

/**
 * Feeds the Digimon watch face: the partner's live sprite as SMALL_IMAGE and a
 * status line (critical countdown / skull warning) as SHORT_TEXT.
 *
 * Both read the same "monster_prefs" save the app and foreground service use,
 * so the face can never disagree with the app — no sync protocol, no drift.
 * Tapping either complication opens the full app.
 */
class DigimonComplicationService : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        return when (request.complicationType) {
            ComplicationType.SMALL_IMAGE -> smallImageData()
            ComplicationType.SHORT_TEXT -> shortTextData()
            else -> EmptyComplicationData()
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        // Picker preview: show the real partner when the cache is warm, so the
        // preview never does card I/O on the calling thread.
        val cached = cachedSprite
        return when (type) {
            ComplicationType.SMALL_IMAGE -> {
                if (cached != null) smallImageData(cached) else EmptyComplicationData()
            }
            ComplicationType.SHORT_TEXT -> shortTextData()
            else -> EmptyComplicationData()
        }
    }

    private fun smallImageData(): ComplicationData {
        val bitmap = loadPartnerBitmap()
        if (bitmap != null) cachedSprite = bitmap
        return smallImageData(bitmap)
    }

    private fun smallImageData(bitmap: Bitmap?): ComplicationData {
        if (bitmap == null) return EmptyComplicationData()
        return SmallImageComplicationData.Builder(
            smallImage = SmallImage.Builder(
                image = Icon.createWithBitmap(bitmap),
                type = SmallImageType.PHOTO,
            ).build(),
            contentDescription = PlainComplicationText.Builder("Digimon partner").build(),
        )
            .setTapAction(openAppPendingIntent())
            .build()
    }

    private fun shortTextData(): ComplicationData {
        val state = try {
            MonsterManager(this).getCurrentMonster()
        } catch (e: Exception) {
            Timber.e(e, "Digimon complication: state read failed")
            null
        }
        val (title, text) = when {
            state == null ->
                "VITALWEAR" to "NO PARTNER"
            state.criticalRemainingMs > 0L ->
                "CRITICAL" to CareManager.formatCriticalMs(state.criticalRemainingMs)
            CareManager.showSkull(state.consecutiveLosses, state.criticalRemainingMs) ->
                "DANGER" to "${state.consecutiveLosses} LOSSES"
            else ->
                "PARTNER" to "READY"
        }
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Digimon status: $title $text").build(),
        )
            .setTitle(PlainComplicationText.Builder(title).build())
            .setTapAction(openAppPendingIntent())
            .build()
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * First idle frame of the active partner, upscaled 4x with no filtering so
     * the pixel art stays crisp in the complication slot.
     */
    private fun loadPartnerBitmap(): Bitmap? {
        return try {
            val monsterManager = MonsterManager(this)
            val state = monsterManager.getCurrentMonster() ?: return null
            val card = CardManager(this).getCard(state.cardName) ?: return null
            val isBem = card is BemCard
            val sprites = card.spriteData.sprites
            val index = monsterManager
                .getIdleSpriteIndices(state.characterId, sprites.size, isBem)
                .firstOrNull() ?: return null
            val frame = SpriteBitmapHandler.getBitmap(sprites[index]) ?: return null
            Bitmap.createScaledBitmap(frame, frame.width * 4, frame.height * 4, false)
        } catch (e: Exception) {
            Timber.e(e, "Digimon complication: sprite load failed")
            null
        }
    }

    companion object {
        @Volatile
        private var cachedSprite: Bitmap? = null
    }
}
