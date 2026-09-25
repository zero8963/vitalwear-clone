package com.example.vitalwearclonev1.gridbattle

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.sin

/**
 * Shared arena renderer for Grid Battle (2026-09-25).
 *
 * Draws the 6x3 grid, both fighters, projectiles, mines, hit flashes and the
 * attack-FX overlays from a [BattleSnapshot]. Used by the solo screen and
 * the PvP guest screen (which feeds it a mirrored snapshot).
 */
data class ArenaFx(
    val coreKey: String?,
    val defaultFlash: Boolean,
    val progress: Float,
    /** false = anchor FX on the enemy fighter (guest watching the host's attacks). */
    val atPlayer: Boolean = true
)

@Composable
fun BattleArenaView(
    snap: BattleSnapshot,
    playerBmp: Bitmap?,
    enemyBmp: Bitmap?,
    fx: ArenaFx,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val cell = min(wPx / 6f, hPx / 3f)
        val ox = (wPx - cell * 6f) / 2f
        val oy = (hPx - cell * 3f) / 2f
        val fxSizePx = cell * 2.5f
        val fxGx = if (fx.atPlayer) snap.px else snap.ex
        val fxGy = if (fx.atPlayer) snap.py else snap.ey

        Canvas(Modifier.fillMaxSize()) {
            // Panels
            for (gx in 0 until 6) {
                for (gy in 0 until 3) {
                    val base = if (gx < 3) Color(12, 32, 90) else Color(90, 16, 16)
                    drawRect(
                        base,
                        topLeft = Offset(ox + gx * cell, oy + gy * cell),
                        size = androidx.compose.ui.geometry.Size(cell, cell)
                    )
                    drawRect(
                        Color.White.copy(alpha = 0.25f),
                        topLeft = Offset(ox + gx * cell, oy + gy * cell),
                        size = androidx.compose.ui.geometry.Size(cell, cell),
                        style = Stroke(2f)
                    )
                }
            }
            // Mines (blinking diamonds)
            val blink = 0.45f + 0.55f * (0.5f + 0.5f * sin(snap.time * 10f))
            snap.mines.forEach { m ->
                val cx = ox + (m.x + 0.5f) * cell
                val cy = oy + (m.y + 0.5f) * cell
                val r = cell * 0.28f
                val path = Path().apply {
                    moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
                }
                drawPath(path, Color(0xFFFF9800).copy(alpha = blink))
            }
            // Fighters
            fun drawFighter(bmp: Bitmap?, gx: Int, gy: Int, tint: Color, lastHit: Float, faceRight: Boolean) {
                val img = bmp?.asImageBitmap()
                val dst = IntOffset((ox + gx * cell).toInt(), (oy + gy * cell).toInt())
                if (img != null) {
                    if (faceRight) {
                        withTransform({
                            scale(
                                scaleX = -1f, scaleY = 1f,
                                pivot = Offset(dst.x + cell / 2f, dst.y + cell / 2f)
                            )
                        }) {
                            drawImage(
                                img,
                                dstOffset = dst,
                                dstSize = androidx.compose.ui.unit.IntSize(cell.toInt(), cell.toInt())
                            )
                        }
                    } else {
                        drawImage(
                            img,
                            dstOffset = dst,
                            dstSize = androidx.compose.ui.unit.IntSize(cell.toInt(), cell.toInt())
                        )
                    }
                } else {
                    drawCircle(
                        tint,
                        radius = cell * 0.32f,
                        center = Offset(ox + (gx + 0.5f) * cell, oy + (gy + 0.5f) * cell)
                    )
                }
                if (snap.time - lastHit < 0.25f) {
                    drawRect(
                        Color.White.copy(alpha = 0.55f),
                        topLeft = Offset(ox + gx * cell, oy + gy * cell),
                        size = androidx.compose.ui.geometry.Size(cell, cell)
                    )
                }
            }
            drawFighter(playerBmp, snap.px, snap.py, Color.Cyan, snap.lastPlayerHitAt, faceRight = true)
            drawFighter(enemyBmp, snap.ex, snap.ey, Color.Magenta, snap.lastEnemyHitAt, faceRight = false)
            // Projectiles
            snap.projectiles.forEach { p ->
                val cx = ox + p.x * cell
                val cy = oy + (p.y + 0.5f) * cell
                val col = p.element?.let { chipElementColor(it) }
                    ?: if (p.fromPlayer) Color.Cyan else Color.Magenta
                drawCircle(col, radius = if (p.big) 26f else 15f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = if (p.big) 26f else 15f, center = Offset(cx, cy), style = Stroke(3f))
            }
            // Default (non-core) attack flash
            if (fx.defaultFlash) {
                val p = fx.progress
                drawCircle(
                    Color.White.copy(alpha = (1f - p) * 0.8f),
                    radius = cell * (0.3f + 1.1f * p),
                    center = Offset(ox + (fxGx + 0.5f) * cell, oy + (fxGy + 0.5f) * cell),
                    style = Stroke(width = 10f * (1f - p) + 2f)
                )
            }
        }

        // NaviCust core attack animation overlay, centered on the attacker.
        if (fx.coreKey != null) {
            Box(
                Modifier
                    .size(with(density) { fxSizePx.toDp() })
                    .offset {
                        IntOffset(
                            (ox + (fxGx + 0.5f) * cell - fxSizePx / 2f).toInt(),
                            (oy + (fxGy + 0.5f) * cell - fxSizePx / 2f).toInt()
                        )
                    }
            ) {
                CoreAttackFx(animKey = fx.coreKey!!, progress = fx.progress, modifier = Modifier.fillMaxSize())
            }
        }
    }

}
