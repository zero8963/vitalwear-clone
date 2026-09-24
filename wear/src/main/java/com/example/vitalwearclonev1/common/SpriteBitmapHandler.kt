package com.example.vitalwearclonev1.common

import android.graphics.Bitmap
import com.github.cfogrady.vb.dim.sprite.SpriteData
import java.nio.ByteBuffer

object SpriteBitmapHandler {

    fun getBitmap(sprite: SpriteData.Sprite): Bitmap? {
        val width = sprite.width
        val height = sprite.height
        if (width <= 0 || height <= 0) return null

        // Use the library's built-in BGRA conversion
        val bgraData = sprite.bgra ?: return null
        if (bgraData.size < width * height * 4) return null

        // Swap Red and Blue channels (BGRA -> RGBA)
        val rgbaData = ByteArray(bgraData.size)
        for (i in 0 until (width * height)) {
            val base = i * 4
            rgbaData[base] = bgraData[base + 2]     // R
            rgbaData[base + 1] = bgraData[base + 1] // G
            rgbaData[base + 2] = bgraData[base]     // B
            rgbaData[base + 3] = bgraData[base + 3] // A
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val buffer = ByteBuffer.wrap(rgbaData)
        
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }
}