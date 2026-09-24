package com.example.vitalwearclonev1.communication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.github.cfogrady.vb.dim.card.Card
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.net.URL

class SpriteSyncManager(private val context: Context) {
    private val storage = FirebaseStorage.getInstance()
    private val monsterManager = PhoneMonsterManager(context)

    suspend fun uploadSprites(card: Card<*, *, *, *, *, *>, characterId: Int, isBem: Boolean, dimHash: String): Map<String, String> = withContext(Dispatchers.IO) {
        val sprites = card.spriteData.sprites
        val keys = mapOf(
            "IDLE1" to monsterManager.getIdleSpriteIndices(characterId, isBem)[0],
            "IDLE2" to monsterManager.getIdleSpriteIndices(characterId, isBem)[1],
            "ATK" to monsterManager.getBattleSpriteIndex(characterId, isBem),
            "WIN" to monsterManager.getWinSpriteIndex(characterId, isBem),
            "LOSE" to monsterManager.getLoseSpriteIndex(characterId, isBem)
        )

        val urls = mutableMapOf<String, String>()
        for ((key, index) in keys) {
            if (index < sprites.size) {
                val bitmap = SpriteBitmapHandler.getBitmap(sprites[index])
                if (bitmap != null) {
                    val url = uploadBitmap(bitmap, "sprites/$dimHash/$characterId/$key.png")
                    if (url != null) {
                        urls[key] = url
                    }
                }
            }
        }
        urls
    }

    private suspend fun uploadBitmap(bitmap: Bitmap, path: String): String? = withContext(Dispatchers.IO) {
        try {
            val ref = storage.reference.child(path)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val data = baos.toByteArray()
            
            ref.putBytes(data).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Timber.e(e, "Error uploading sprite to $path")
            null
        }
    }

    suspend fun downloadSprites(remoteUrls: Map<String, String>): Map<String, Bitmap?> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Bitmap?>()
        for ((key, urlString) in remoteUrls) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection()
                connection.doInput = true
                connection.connect()
                val input = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(input)
                result[key] = bitmap
            } catch (e: Exception) {
                Timber.e(e, "Error downloading sprite from $urlString")
                result[key] = null
            }
        }
        result
    }
}
