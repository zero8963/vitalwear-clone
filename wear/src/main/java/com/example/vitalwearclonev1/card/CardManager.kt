package com.example.vitalwearclonev1.card

import android.content.Context
import com.github.cfogrady.vb.dim.card.Card
import com.github.cfogrady.vb.dim.card.DimReader
import com.github.cfogrady.vb.dim.card.DimWriter
import timber.log.Timber
import java.io.*

class CardManager(private val context: Context) {

    private val cardsDir = File(context.filesDir, "cards")

    init {
        if (!cardsDir.exists()) {
            cardsDir.mkdirs()
        }
    }

    fun saveCard(name: String, card: Card<*, *, *, *, *, *>) {
        val cardFile = File(cardsDir, "$name.bin")
        try {
            FileOutputStream(cardFile).use { fos ->
                DimWriter().writeCard(card, fos)
            }
            Timber.d("Saved card $name to ${cardFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Error saving card $name")
        }
    }

    fun getCard(name: String): Card<*, *, *, *, *, *>? {
        val cardFile = File(cardsDir, "$name.bin")
        if (!cardFile.exists()) {
            // Try case-insensitive fallback
            val files = cardsDir.listFiles()
            val match = files?.find { it.nameWithoutExtension.equals(name, ignoreCase = true) }
            if (match != null) {
                Timber.d("Found case-insensitive match for $name: ${match.nameWithoutExtension}")
                return getCard(match.nameWithoutExtension)
            }
            return null
        }
        
        return try {
            FileInputStream(cardFile).use { fis ->
                // Attempt to read as standard DIM first, then BEM
                try {
                    DimReader().readCard(fis, false)
                } catch (t: Throwable) {
                    FileInputStream(cardFile).use { fis2 ->
                        DimReader().readCard(fis2, true)
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Error reading card $name")
            null
        }
    }

    fun getCardById(dimId: Int): Card<*, *, *, *, *, *>? {
        val files = cardsDir.listFiles() ?: return null
        for (file in files) {
            if (file.extension == "bin") {
                val card = getCard(file.nameWithoutExtension)
                if (card?.header?.dimId == dimId) {
                    return card
                }
            }
        }
        return null
    }


    fun listCards(): List<String> {
        return cardsDir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
    }
}