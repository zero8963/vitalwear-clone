package com.example.vitalwearclonev1.card

import android.content.Context
import com.github.cfogrady.vb.dim.card.Card
import com.github.cfogrady.vb.dim.card.DimReader
import com.github.cfogrady.vb.dim.card.DimWriter
import timber.log.Timber
import java.io.*
import java.security.MessageDigest

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

    fun getCardHash(name: String): String? {
        val cardFile = File(cardsDir, "$name.bin")
        if (!cardFile.exists()) return null
        return try {
            calculateHash(cardFile)
        } catch (e: Exception) {
            Timber.e(e, "Error calculating hash for card $name")
            null
        }
    }

    fun getCardByHash(hash: String): Card<*, *, *, *, *, *>? {
        val files = cardsDir.listFiles() ?: return null
        for (file in files) {
            if (file.extension == "bin") {
                val currentHash = try { calculateHash(file) } catch (e: Exception) { null }
                if (currentHash == hash) {
                    return getCard(file.nameWithoutExtension)
                }
            }
        }
        return null
    }

    private fun calculateHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }


    fun listCards(): List<String> {
        return cardsDir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
    }

    fun deleteCard(name: String): Boolean {
        val cardFile = File(cardsDir, "$name.bin")
        return if (cardFile.exists()) {
            val deleted = cardFile.delete()
            if (deleted) {
                Timber.d("Deleted card $name")
            } else {
                Timber.e("Failed to delete card $name")
            }
            deleted
        } else {
            Timber.w("Card file $name.bin not found for deletion")
            false
        }
    }
}
