package com.example.vitalwearclonev1.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.random.Random

/**
 * Data for one evolution sequence: the old form's idle frames, the new form's
 * idle frames, and a "portrait" frame (the new form's high-detail sheet portrait) shown large
 * as a splash reveal before settling into the play sprites.
 */
data class EvolutionRequest(
    val oldFrames: List<Bitmap>,
    val newFrames: List<Bitmap>,
    val portraitFrame: Bitmap? = null
)

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * Original-hardware style digivolution sequence (~4.8s):
 * black fades in -> white light builds behind the old Digimon -> a light band
 * sweeps across -> the old sprite washes white and dissolves -> full-screen
 * flash -> PORTRAIT REVEAL: the new form shown large and flashy for a beat ->
 * it settles down into the normal play sprite -> overlay fades away.
 */
@Composable
fun EvolutionAnimation(
    oldFrames: List<Bitmap>,
    newFrames: List<Bitmap>,
    portraitFrame: Bitmap? = null,
    onFinished: () -> Unit,
    spriteSize: Dp = 220.dp
) {
    val totalMs = 4800
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(totalMs, easing = LinearEasing))
        onFinished()
    }
    val p = anim.value

    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(300)
            tick++
        }
    }

    // ---- Phase curves (p in 0..1) ----
    val blackAlpha = smoothstep(0f, 0.15f, p)
    val lightBuild = smoothstep(0.08f, 0.38f, p)

    // Sweeping light band: travels left -> right, visible mid-sequence.
    val bandT = smoothstep(0.28f, 0.50f, p)
    val bandAlpha = (1f - kotlin.math.abs(bandT - 0.5f) * 2f).coerceIn(0f, 1f)

    // Old sprite: washes toward white, then dissolves.
    val oldWash = smoothstep(0.38f, 0.56f, p)
    val oldAlpha = 1f - smoothstep(0.42f, 0.56f, p)

    // Full-screen flash, peaking at p = 0.60.
    val flashAlpha = (1f - kotlin.math.abs(p - 0.60f) / 0.08f).coerceIn(0f, 1f)

    // Portrait reveal: fades in right after the flash, holds, then hands off.
    val portraitAlpha = smoothstep(0.60f, 0.68f, p) * (1f - smoothstep(0.76f, 0.86f, p))
    // Slow dramatic push-in while the portrait holds.
    val portraitZoom = lerp(1.30f, 1.65f, smoothstep(0.60f, 0.84f, p))

    // Play sprite: scales down from large to normal as the portrait hands off.
    val spriteIn = smoothstep(0.80f, 0.90f, p)
    val spriteScale = lerp(1.35f, 1.0f, smoothstep(0.80f, 0.92f, p))

    val overlayAlpha = 1f - smoothstep(0.93f, 1f, p)

    val portraitBitmap = portraitFrame ?: newFrames.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha }
            .background(Color.Black.copy(alpha = blackAlpha))
    ) {
        // Matrix-style binary rain: the digital world wrapping around the Digimon
        // while it is enveloped in the white light.
        BinaryRain(progress = p)

        // White radial light blooming behind the old Digimon.
        Canvas(Modifier.fillMaxSize()) {
            if (lightBuild > 0.01f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f * lightBuild),
                            Color.White.copy(alpha = 0.28f * lightBuild),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.45f * (0.6f + 0.4f * lightBuild)
                    ),
                    size = size
                )
            }
        }

        // Sweeping light band.
        Canvas(Modifier.fillMaxSize()) {
            if (bandAlpha > 0.01f) {
                rotate(18f) {
                    val cx = size.width * lerp(-0.3f, 1.3f, bandT)
                    drawRect(
                        color = Color.White.copy(alpha = 0.55f * bandAlpha),
                        topLeft = Offset(cx - size.width * 0.12f, -size.height * 0.3f),
                        size = Size(size.width * 0.24f, size.height * 1.6f)
                    )
                }
            }
        }

        // Old sprite: washes white, then dissolves.
        if (oldFrames.isNotEmpty() && oldAlpha > 0.01f) {
            Image(
                bitmap = oldFrames[tick % oldFrames.size].asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(spriteSize)
                    .graphicsLayer { alpha = oldAlpha },
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.lighting(
                    multiply = Color.White,
                    add = Color.White.copy(alpha = oldWash * 0.9f)
                )
            )
        }

        // ---- PORTRAIT REVEAL: the new form, big and flashy ----
        if (portraitBitmap != null && portraitAlpha > 0.01f) {
            // Soft glow behind the portrait.
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f * portraitAlpha),
                            Color.White.copy(alpha = 0.12f * portraitAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.55f
                    ),
                    size = size
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.8f)
                    .graphicsLayer {
                        alpha = portraitAlpha
                        scaleX = portraitZoom
                        scaleY = portraitZoom
                    }
            ) {
                Image(
                    bitmap = portraitBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // New form's play sprite: settles from large to normal size.
        if (newFrames.isNotEmpty() && spriteIn > 0.01f) {
            Image(
                bitmap = newFrames[tick % newFrames.size].asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(spriteSize)
                    .graphicsLayer {
                        alpha = spriteIn
                        scaleX = spriteScale
                        scaleY = spriteScale
                    },
                contentScale = ContentScale.Fit
            )
        }

        // Full-screen white flash over everything.
        if (flashAlpha > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha))
            )
        }
    }
}

/**
 * Falling wall of green binary code (0s and 1s), Matrix-style. Each column is
 * deterministic so glyphs stay stable frame to frame while the streams fall.
 */
@Composable
private fun BinaryRain(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val glyphPx = with(density) { 19.sp.toPx() }
    val colWidthPx = with(density) { 30.dp.toPx() }
    val paint = remember {
        Paint().apply {
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
    }

    // Fades in with the black background, dissolves before the white flash.
    val rainAlpha = smoothstep(0.02f, 0.12f, progress) * (1f - smoothstep(0.45f, 0.58f, progress))
    if (rainAlpha <= 0.01f) return

    // Seconds into the ~4.8s sequence; drives the falling motion.
    val t = progress * 4.8f

    Canvas(modifier.fillMaxSize()) {
        paint.textSize = glyphPx
        val cols = (size.width / colWidthPx).toInt().coerceAtLeast(1)
        val glyphH = glyphPx * 1.6f
        for (c in 0 until cols) {
            val rnd = Random(c * 1000003L + 17L)
            val speed = 140f + rnd.nextFloat() * 260f
            val streamLen = 7 + rnd.nextInt(9)
            val travel = size.height + streamLen * glyphH
            val phase = rnd.nextFloat() * travel
            val headY = (phase + t * speed) % travel - streamLen * glyphH
            val brightness = 0.45f + rnd.nextFloat() * 0.55f
            val x = c * colWidthPx + colWidthPx * 0.25f
            for (i in 0 until streamLen) {
                // i = 0 is the head: lowest on screen, leading the fall.
                val y = headY + (streamLen - 1 - i) * glyphH
                if (y < -glyphH || y > size.height + glyphH) continue
                val fade = 1f - i.toFloat() / streamLen
                val a = (rainAlpha * brightness * fade * fade).coerceIn(0f, 1f)
                if (a <= 0.02f) continue
                val bit = if (bitAt(c, i) == 1) "1" else "0"
                paint.color = if (i == 0) android.graphics.Color.rgb(200, 255, 200)
                              else android.graphics.Color.rgb(0, 220, 70)
                paint.alpha = (a * 255).toInt()
                drawIntoCanvas { it.nativeCanvas.drawText(bit, x, y, paint) }
            }
        }
    }
}

// Deterministic pseudo-random bit per column/row so glyphs don't flicker.
private fun bitAt(col: Int, row: Int): Int {
    var h = col * 374761393 + row * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return h and 1
}
