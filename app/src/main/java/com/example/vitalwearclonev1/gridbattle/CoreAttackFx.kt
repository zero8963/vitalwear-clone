package com.example.vitalwearclonev1.gridbattle

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated attack effects for NaviCust Core programs (2026-09-25).
 *
 * When a Buster Core / Sword Edge is installed, the fighter's attack animation
 * is REPLACED by the element effect below instead of the DIM-programmed one.
 * Driven by progress 0f -> 1f (same contract as AttackEffectCanvas).
 * The future grid-battle engine reuses these via the same animKeys.
 */
object CoreAttackFxKeys {
    const val SHOT_FLAME = "shot_flame"
    const val SHOT_TIDE = "shot_tide"
    const val SHOT_VOLT = "shot_volt"
    const val SHOT_THORN = "shot_thorn"
    const val SLASH_CINDER = "slash_cinder"
    const val SLASH_REEF = "slash_reef"
    const val SLASH_STORM = "slash_storm"
    const val SLASH_BRAMBLE = "slash_bramble"
}

fun coreAttackFxColor(animKey: String): Color = when (animKey) {
    CoreAttackFxKeys.SHOT_FLAME, CoreAttackFxKeys.SLASH_CINDER -> Color(0xFFFF6B35)
    CoreAttackFxKeys.SHOT_TIDE, CoreAttackFxKeys.SLASH_REEF -> Color(0xFF35A7FF)
    CoreAttackFxKeys.SHOT_VOLT, CoreAttackFxKeys.SLASH_STORM -> Color(0xFFFFD935)
    else -> Color(0xFF7BFF6B)
}

private fun prand(i: Int, salt: Int): Float {
    val x = (i * 2654435761L + salt * 40503L) % 1000L
    return ((x + 1000L) % 1000L) / 1000f
}

@Composable
fun CoreAttackFx(animKey: String, progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val p = progress.coerceIn(0f, 1f)
        when (animKey) {
            CoreAttackFxKeys.SHOT_FLAME -> drawShotFlame(p)
            CoreAttackFxKeys.SHOT_TIDE -> drawShotTide(p)
            CoreAttackFxKeys.SHOT_VOLT -> drawShotVolt(p)
            CoreAttackFxKeys.SHOT_THORN -> drawShotThorn(p)
            CoreAttackFxKeys.SLASH_CINDER -> drawSlash(p, Color(0xFFFF6B35), Color(0xFFFFD935))
            CoreAttackFxKeys.SLASH_REEF -> drawSlash(p, Color(0xFF35A7FF), Color(0xFFB0E8FF))
            CoreAttackFxKeys.SLASH_STORM -> drawSlash(p, Color(0xFFFFD935), Color.White)
            CoreAttackFxKeys.SLASH_BRAMBLE -> drawSlash(p, Color(0xFF4CAF50), Color(0xFFB0FF9E))
        }
    }
}

private fun DrawScope.drawShotFlame(p: Float) {
    val c = center
    val maxR = size.minDimension / 2f
    // Expanding heat ring
    drawCircle(Color(0xFFFF6B35).copy(alpha = (1f - p) * 0.8f), radius = maxR * (0.2f + 0.8f * p), center = c, style = Stroke(width = 6f * (1f - p) + 2f))
    // Flame tongues licking outward
    repeat(10) { i ->
        val ang = (i * 36f + prand(i, 7) * 20f) * Math.PI.toFloat() / 180f
        val len = maxR * (0.25f + 0.75f * p) * (0.7f + prand(i, 13) * 0.6f)
        val tip = Offset(c.x + cos(ang) * len, c.y + sin(ang) * len)
        val mid = Offset(c.x + cos(ang) * len * 0.55f, c.y + sin(ang) * len * 0.55f)
        val w = 14f * (1f - p) + 4f
        drawLine(Color(0xFFFF3D00).copy(alpha = 0.9f), mid, tip, strokeWidth = w * 1.6f)
        drawLine(Color(0xFFFFD935).copy(alpha = 0.95f), mid, tip, strokeWidth = w * 0.7f)
    }
    // Hot core
    drawCircle(Color(0xFFFFF3C4).copy(alpha = (1f - p)), radius = maxR * 0.22f * (1f - p * 0.5f), center = c)
}

private fun DrawScope.drawShotTide(p: Float) {
    val c = center
    val maxR = size.minDimension / 2f
    // Triple expanding ripple rings
    listOf(0f, 0.25f, 0.5f).forEachIndexed { idx, off ->
        val rp = (p - off).coerceIn(0f, 1f)
        if (rp > 0f) {
            drawCircle(
                Color(0xFF35A7FF).copy(alpha = (1f - rp) * 0.85f),
                radius = maxR * (0.15f + 0.85f * rp),
                center = c,
                style = Stroke(width = 10f * (1f - rp) + 2f)
            )
        }
    }
    // Spray droplets flying outward
    repeat(8) { i ->
        val ang = (i * 45f + prand(i, 21) * 25f) * Math.PI.toFloat() / 180f
        val d = maxR * p * (0.5f + prand(i, 33) * 0.5f)
        drawCircle(
            Color(0xFFB0E8FF).copy(alpha = (1f - p) * 0.9f + 0.1f),
            radius = 7f * (1f - p) + 2f,
            center = Offset(c.x + cos(ang) * d, c.y + sin(ang) * d)
        )
    }
}

private fun DrawScope.drawShotVolt(p: Float) {
    val c = center
    val maxR = size.minDimension / 2f
    // Jagged lightning bolts radiating out
    repeat(6) { i ->
        val baseAng = (i * 60f + prand(i, 41) * 30f) * Math.PI.toFloat() / 180f
        var prev = c
        val segs = 4
        repeat(segs) { s ->
            val frac = (s + 1).toFloat() / segs
            val jitter = (prand(i * 10 + s, 55) - 0.5f) * 0.5f
            val ang = baseAng + jitter
            val d = maxR * frac * (0.3f + 0.7f * p)
            val next = Offset(c.x + cos(ang) * d, c.y + sin(ang) * d)
            val alpha = (1f - p * 0.6f)
            drawLine(Color(0xFFFFD935).copy(alpha = alpha), prev, next, strokeWidth = 9f)
            drawLine(Color.White.copy(alpha = alpha), prev, next, strokeWidth = 3.5f)
            prev = next
        }
    }
    drawCircle(Color.White.copy(alpha = (1f - p) * 0.9f), radius = maxR * 0.14f, center = c)
}

private fun DrawScope.drawShotThorn(p: Float) {
    val c = center
    val maxR = size.minDimension / 2f
    // Green spikes bursting outward
    repeat(8) { i ->
        val ang = (i * 45f + prand(i, 61) * 15f) * Math.PI.toFloat() / 180f
        val len = maxR * (0.3f + 0.7f * p) * (0.75f + prand(i, 71) * 0.5f)
        val tip = Offset(c.x + cos(ang) * len, c.y + sin(ang) * len)
        val base = Offset(c.x + cos(ang) * len * 0.35f, c.y + sin(ang) * len * 0.35f)
        drawLine(Color(0xFF2E7D32).copy(alpha = 0.95f), base, tip, strokeWidth = 12f * (1f - p) + 3f)
        // Leaf at the tip
        drawCircle(Color(0xFF7BFF6B).copy(alpha = (1f - p * 0.5f)), radius = 8f * (1f - p) + 3f, center = tip)
    }
    // Spore puff ring
    drawCircle(Color(0xFF7BFF6B).copy(alpha = (1f - p) * 0.5f), radius = maxR * (0.2f + 0.6f * p), center = c, style = Stroke(width = 4f))
}

private fun DrawScope.drawSlash(p: Float, main: Color, hot: Color) {
    val c = center
    val maxR = size.minDimension / 2f
    val radius = maxR * (0.45f + 0.55f * p)
    val sweepStart = -55f
    val sweep = 110f * p
    val alpha = 1f - p
    // Wide colored slash arc
    drawArc(
        color = main.copy(alpha = alpha * 0.9f),
        startAngle = sweepStart,
        sweepAngle = sweep.coerceAtLeast(1f),
        useCenter = false,
        topLeft = Offset(c.x - radius, c.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 16f * (1f - p) + 5f)
    )
    // Hot inner edge
    drawArc(
        color = hot.copy(alpha = alpha),
        startAngle = sweepStart,
        sweepAngle = (sweep * 0.7f).coerceAtLeast(1f),
        useCenter = false,
        topLeft = Offset(c.x - radius * 0.8f, c.y - radius * 0.8f),
        size = Size(radius * 1.6f, radius * 1.6f),
        style = Stroke(width = 6f * (1f - p) + 2f)
    )
    // Trailing sparks along the arc
    repeat(6) { i ->
        val a = (sweepStart + prand(i, 83) * sweep.coerceAtLeast(1f)) * Math.PI.toFloat() / 180f
        val d = radius * (0.85f + prand(i, 97) * 0.3f)
        drawCircle(
            hot.copy(alpha = alpha * 0.9f),
            radius = 6f * (1f - p) + 2f,
            center = Offset(c.x + cos(a) * d, c.y + sin(a) * d)
        )
    }
}
