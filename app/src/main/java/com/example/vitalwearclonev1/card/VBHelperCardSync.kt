package com.example.vitalwearclonev1.card

import android.content.Context
import android.net.Uri
import android.os.Bundle
import timber.log.Timber

/**
 * Ported from original VitalWear-companion logic.
 * Syncs card data to VBHelper database if available.
 */
object VBHelperCardSync {

    private const val AUTHORITY             = "com.github.nacabaro.vbhelper.cardimport"
    private const val METHOD_IMPORT_CARD    = "importCard"
    private const val EXTRA_CARD_BYTES      = "cardBytes"
    private const val EXTRA_CARD_NAME       = "cardName"

    private val URI = Uri.parse("content://$AUTHORITY")

    fun syncCard(context: Context, cardBytes: ByteArray, customName: String) {
        runCatching {
            val extras = Bundle().apply {
                putByteArray(EXTRA_CARD_BYTES, cardBytes)
                putString(EXTRA_CARD_NAME, customName)
            }
            context.contentResolver.call(URI, METHOD_IMPORT_CARD, null, extras)
            Timber.i("Card '$customName' synced to VBHelper database.")
        }.onFailure {
            Timber.w(it, "Failed to sync card '$customName' to VBHelper; skipping.")
        }
    }
}
