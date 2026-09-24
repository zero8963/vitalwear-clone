package com.example.vitalwearclonev1.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The Bracelet's attack effect library. Each character on a DIM card programs
 * two attacks (small = regular hit, big = critical hit); the attack IDs pick
 * the effect from this library, so a modded DIM chooses its Digimon's attack
 * appearance by setting those IDs. IDs 8 and 12 are faithful recreations of
 * the Bracelet's own crescent-slash attacks; any other unknown ID wraps
 * around the generic 8-effect set.
 */
const val ATTACK_EFFECT_COUNT = 8

/** IDs recreated 1:1 from the Bracelet's attack list (verified against a DIM editor). */
private fun resolveAttackId(attackId: Int): Int = when (attackId) {
    8, 12 -> attackId
    else -> floorMod(attackId, ATTACK_EFFECT_COUNT)
}

/** Signature color of an attack effect, used for projectile tints and accents. */
fun attackEffectColor(attackId: Int): Color = when (resolveAttackId(attackId)) {
    0 -> Color(0xFFFF4D6D) // volt crash: pink-red
    1 -> Color(0xFF39FF6A) // dragon wave: green
    2 -> Color(0xFFFF7A1A) // inferno burst: orange
    3 -> Color(0xFF4DD2FF) // photon beam: blue
    4 -> Color(0xFFB8FF2E) // sonic slashes: lime
    5 -> Color(0xFF7DF9FF) // glacier shards: cyan
    6 -> Color(0xFFB366FF) // abyss pulse: purple
    8, 12 -> Color(0xFF8FE9FF) // crescent slash: ice cyan
    else -> Color(0xFFFFC93D) // meteor impact: gold
}

private fun floorMod(a: Int, b: Int) = ((a % b) + b) % b

/**
 * Full-screen attack effect. [progress] runs 0 -> 1 over the cutscene.
 */
@Composable
fun AttackEffectCanvas(attackId: Int, progress: Float, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    Canvas(modifier) {
        when (resolveAttackId(attackId)) {
            0 -> drawVoltCrash(p, attackId)
            1 -> drawDragonWave(p, attackId)
            2 -> drawInfernoBurst(p, attackId)
            3 -> drawPhotonBeam(p)
            4 -> drawSonicSlashes(p)
            5 -> drawGlacierShards(p, attackId)
            6 -> drawAbyssPulse(p)
            8 -> drawCrescentSlash(p, twin = true)
            12 -> drawCrescentSlash(p, twin = false)
            else -> drawMeteorImpact(p, attackId)
        }
    }
}

/** Jagged lightning bolts crashing down over a blood-red pulse. */
private fun DrawScope.drawVoltCrash(p: Float, seed: Int) {
    drawRect(Color(0xFF3D0000).copy(alpha = 0.25f + 0.35f * p))
    val rnd = Random(seed * 31 + 1)
    repeat(3) { i ->
        val path = Path()
        var x = size.width * (0.25f + 0.25f * i) + (rnd.nextFloat() - 0.5f) * size.width * 0.1f
        var y = -20f
        path.moveTo(x, y)
        val segs = 7
        repeat(segs) { s ->
            x += (rnd.nextFloat() - 0.5f) * size.width * 0.18f
            y += size.height / segs
            path.lineTo(x, y)
        }
        val boltP = (p * 1.4f - i * 0.2f).coerceIn(0f, 1f)
        if (boltP > 0f) {
            drawPath(path, Color(0xFFFF4D6D).copy(alpha = boltP), style = Stroke(width = 14f, cap = StrokeCap.Round))
            drawPath(path, Color.White.copy(alpha = boltP * 0.9f), style = Stroke(width = 5f, cap = StrokeCap.Round))
        }
    }
    if (p > 0.7f) drawRect(Color.White.copy(alpha = (p - 0.7f) * 2f))
}

/** Coiling green energy waves sweeping across the screen. */
private fun DrawScope.drawDragonWave(p: Float, seed: Int) {
    drawRect(Color(0xFF00260F).copy(alpha = 0.3f + 0.3f * p))
    val rnd = Random(seed * 31 + 7)
    val phase = rnd.nextFloat() * 6.28f
    repeat(3) { i ->
        val waveP = (p * 1.5f - i * 0.25f).coerceIn(0f, 1f)
        if (waveP > 0f) {
            val yBase = size.height * (0.3f + 0.2f * i)
            val xOff = (1f - waveP) * size.width * 1.2f
            val path = Path()
            repeat(40) { s ->
                val f = s / 40f
                val x = f * size.width * 1.2f - size.width * 0.1f + xOff
                val y = yBase + sin(f * PI * 3 + phase).toFloat() * size.height * 0.08f
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFF39FF6A).copy(alpha = 0.85f * waveP), style = Stroke(width = 26f, cap = StrokeCap.Round))
            drawPath(path, Color(0xFFD6FFDF).copy(alpha = 0.9f * waveP), style = Stroke(width = 10f, cap = StrokeCap.Round))
        }
    }
}

/** Radial fire burst with rising embers. */
private fun DrawScope.drawInfernoBurst(p: Float, seed: Int) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val maxR = size.minDimension * 0.7f
    repeat(3) { i ->
        val rp = (p * 1.6f - i * 0.3f).coerceIn(0f, 1f)
        if (rp > 0f) drawCircle(
            Color(0xFFFF7A1A).copy(alpha = 0.7f * (1f - rp)),
            radius = maxR * rp, center = c, style = Stroke(width = 30f * (1f - rp) + 6f)
        )
    }
    drawCircle(Color(0xFFFFD23D).copy(alpha = (1f - p) * 0.9f), radius = maxR * 0.5f * (1f - p * 0.5f), center = c)
    val rnd = Random(seed * 31 + 13)
    repeat(24) { _ ->
        val ang = rnd.nextFloat() * 2f * PI
        val dist = maxR * rnd.nextFloat() * p
        val pos = Offset(
            c.x + (cos(ang) * dist).toFloat(),
            c.y + (sin(ang) * dist).toFloat() - p * size.height * 0.15f
        )
        drawCircle(Color(0xFFFFB03D).copy(alpha = (1f - p) * 0.9f), radius = 4f + rnd.nextFloat() * 8f, center = pos)
    }
}

/** Charging beam that fires full-width. */
private fun DrawScope.drawPhotonBeam(p: Float) {
    val cy = size.height / 2f
    drawRect(Color(0xFF001A2E).copy(alpha = 0.35f))
    if (p < 0.35f) {
        val w = size.width * (p / 0.35f)
        drawLine(Color(0xFF4DD2FF), Offset(0f, cy), Offset(w, cy), strokeWidth = 8f, cap = StrokeCap.Round)
        drawCircle(Color.White.copy(alpha = p / 0.35f), radius = 14f, center = Offset(0f, cy))
    } else {
        val bp = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
        drawLine(Color(0xFF4DD2FF).copy(alpha = 0.5f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 64f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.95f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 22f, cap = StrokeCap.Round)
        if (bp < 0.4f) drawRect(Color.White.copy(alpha = (1f - bp / 0.4f) * 0.5f))
    }
}

/** Three crescent slash arcs sweeping across. */
private fun DrawScope.drawSonicSlashes(p: Float) {
    drawRect(Color(0xFF1A2600).copy(alpha = 0.3f))
    val d = size.minDimension * 0.55f
    repeat(3) { i ->
        val sp = (p * 1.8f - i * 0.4f).coerceIn(0f, 1f)
        if (sp > 0f) {
            val xC = size.width * (0.15f + 0.7f * sp)
            val yC = size.height * (0.25f + 0.25f * i)
            drawArc(
                Color(0xFFB8FF2E).copy(alpha = 0.9f * (1f - sp * 0.5f)),
                startAngle = -60f, sweepAngle = 120f, useCenter = false,
                topLeft = Offset(xC - d / 2f, yC - d / 2f), size = Size(d, d),
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )
        }
    }
}

/** Crystalline ice shards bursting outward. */
private fun DrawScope.drawGlacierShards(p: Float, seed: Int) {
    val c = Offset(size.width / 2f, size.height / 2f)
    drawRect(Color(0xFF00333D).copy(alpha = 0.3f + 0.25f * p))
    val rnd = Random(seed * 31 + 41)
    repeat(14) { i ->
        val ang = (i / 14f) * 2f * PI + rnd.nextFloat()
        val dist = size.minDimension * 0.45f * p * (0.5f + rnd.nextFloat() * 0.5f)
        val pos = Offset(c.x + (cos(ang) * dist).toFloat(), c.y + (sin(ang) * dist).toFloat())
        val r = 10f + rnd.nextFloat() * 16f
        val path = Path().apply {
            moveTo(pos.x, pos.y - r)
            lineTo(pos.x + r * 0.6f, pos.y + r)
            lineTo(pos.x - r * 0.6f, pos.y + r)
            close()
        }
        drawPath(path, Color(0xFF7DF9FF).copy(alpha = 0.85f))
        drawPath(path, Color.White.copy(alpha = 0.5f), style = Stroke(width = 2f))
    }
    if (p > 0.6f) drawCircle(Color.White.copy(alpha = (p - 0.6f) * 1.2f), radius = 30f, center = c)
}

/** Dark implosion rings over a violet pulse. */
private fun DrawScope.drawAbyssPulse(p: Float) {
    val c = Offset(size.width / 2f, size.height / 2f)
    drawRect(Color(0xFF12001F).copy(alpha = 0.35f + 0.3f * p))
    repeat(4) { i ->
        val rp = (p * 1.5f - i * 0.22f).coerceIn(0f, 1f)
        if (rp > 0f) drawCircle(
            Color(0xFFB366FF).copy(alpha = 0.75f * (1f - rp)),
            radius = size.minDimension * 0.55f * rp, center = c, style = Stroke(width = 22f)
        )
    }
    drawCircle(Color(0xFF2A0050).copy(alpha = 0.8f * (1f - p)), radius = size.minDimension * 0.3f * (1f - p) + 10f, center = c)
}

/** Meteor streaks raining down into a shockwave. */
private fun DrawScope.drawMeteorImpact(p: Float, seed: Int) {
    val rnd = Random(seed * 31 + 53)
    drawRect(Color(0xFF2E1500).copy(alpha = 0.3f))
    repeat(8) { _ ->
        val sp = (p * 2f - rnd.nextFloat() * 0.9f).coerceIn(0f, 1f)
        if (sp > 0f && sp < 1f) {
            val x = size.width * rnd.nextFloat()
            val y = size.height * sp
            drawLine(Color(0xFFFFC93D), Offset(x, y - 90f), Offset(x - 30f, y), strokeWidth = 10f, cap = StrokeCap.Round)
        }
    }
    val ip = (p * 1.4f - 0.4f).coerceIn(0f, 1f)
    if (ip > 0f) {
        val c = Offset(size.width / 2f, size.height * 0.7f)
        drawCircle(Color(0xFFFFC93D).copy(alpha = 0.8f * (1f - ip)), radius = size.minDimension * 0.6f * ip, center = c, style = Stroke(width = 26f))
        drawCircle(Color.White.copy(alpha = (1f - ip) * 0.9f), radius = 60f * (1f - ip) + 12f, center = c)
    }
}

/** Cyan crescent slash, recreated from the Bracelet's own attack list:
 *  ID 12 = single crescent (small attack), ID 8 = twin crescents (big). */
private fun DrawScope.drawCrescentSlash(p: Float, twin: Boolean) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val minD = size.minDimension
    // cold background pulse, fading as the slash travels
    drawRect(Color(0xFF062A38).copy(alpha = 0.6f * (1f - p)))
    val delays = if (twin) listOf(0f, 0.45f) else listOf(0f)
    for (delay in delays) {
        val lp = ((p - delay * 0.5f) / (1f - delay * 0.5f)).coerceIn(0f, 1f)
        if (lp <= 0f) continue
        val alpha = 1f - lp
        // the crescent sweeps diagonally across as it fades
        val travel = lp * minD * 0.55f
        val ox = cx - travel * 0.5f
        val oy = cy - travel * 0.35f
        val r = minD * (0.34f + 0.30f * lp)
        val sweepStart = -70f + lp * 140f
        // layered crescent: cyan glow, pale mid, white-hot core
        val layers = listOf(
            Triple(Color(0xFF2ED9FF), 0.16f, 0.55f),
            Triple(Color(0xFF9BEBFF), 0.10f, 0.80f),
            Triple(Color(0xFFFFFFFF), 0.055f, 1f)
        )
        for ((color, wFrac, aFrac) in layers) {
            drawArc(
                color = color.copy(alpha = alpha * aFrac),
                startAngle = sweepStart,
                sweepAngle = 75f,
                useCenter = false,
                topLeft = Offset(ox - r, oy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = minD * wFrac, cap = StrokeCap.Round)
            )
        }
        // speed lines trailing the slash
        for (i in 0 until 6) {
            val a = Math.toRadians((sweepStart + 100.0 + i * 14.0).toDouble()).toFloat()
            drawLine(
                Color(0xFFBFF3FF).copy(alpha = alpha * 0.5f),
                Offset(ox + cos(a) * r * 0.9f, oy + sin(a) * r * 0.9f),
                Offset(ox + cos(a) * r * 1.35f, oy + sin(a) * r * 1.35f),
                strokeWidth = minD * 0.012f,
                cap = StrokeCap.Round
            )
        }
    }
    // opening flash
    if (p < 0.25f) drawRect(Color.White.copy(alpha = 0.5f * (1f - p / 0.25f)))
}
