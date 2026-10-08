package com.example.vitalwearclonev1.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize

/**
 * Fitness (2026-10-07): stylized front/back body silhouette on Canvas with
 * the 12 muscle groups as tappable rounded regions. Deliberately abstract,
 * not anatomical art.
 *
 * VIEW mode: regions color-coded by recovery state (tap reports the muscle
 * via onMuscleTap, e.g. to show its status line).
 * LOG mode: tap toggles selection; selected groups glow cyan.
 */
enum class BodyMapMode { VIEW, LOG }

private data class MuscleRegion(val muscle: MuscleGroup, val rect: Rect)

// Normalized coords 0..1 (y down). Mirrored pairs map to the same muscle.
private fun r(l: Float, t: Float, rr: Float, b: Float) = Rect(l, t, rr, b)

private fun frontRegions(): List<MuscleRegion> = listOf(
    MuscleRegion(MuscleGroup.CHEST, r(0.38f, 0.20f, 0.62f, 0.33f)),
    MuscleRegion(MuscleGroup.SHOULDERS, r(0.27f, 0.13f, 0.42f, 0.20f)),
    MuscleRegion(MuscleGroup.SHOULDERS, r(0.58f, 0.13f, 0.73f, 0.20f)),
    MuscleRegion(MuscleGroup.BICEPS, r(0.26f, 0.22f, 0.34f, 0.35f)),
    MuscleRegion(MuscleGroup.BICEPS, r(0.66f, 0.22f, 0.74f, 0.35f)),
    MuscleRegion(MuscleGroup.FOREARMS, r(0.25f, 0.37f, 0.33f, 0.50f)),
    MuscleRegion(MuscleGroup.FOREARMS, r(0.67f, 0.37f, 0.75f, 0.50f)),
    MuscleRegion(MuscleGroup.ABS, r(0.43f, 0.35f, 0.57f, 0.47f)),
    MuscleRegion(MuscleGroup.OBLIQUES, r(0.36f, 0.35f, 0.43f, 0.47f)),
    MuscleRegion(MuscleGroup.OBLIQUES, r(0.57f, 0.35f, 0.64f, 0.47f)),
    MuscleRegion(MuscleGroup.QUADS, r(0.38f, 0.52f, 0.49f, 0.70f)),
    MuscleRegion(MuscleGroup.QUADS, r(0.51f, 0.52f, 0.62f, 0.70f)),
    MuscleRegion(MuscleGroup.CALVES, r(0.39f, 0.74f, 0.48f, 0.88f)),
    MuscleRegion(MuscleGroup.CALVES, r(0.52f, 0.74f, 0.61f, 0.88f))
)

private fun backRegions(): List<MuscleRegion> = listOf(
    MuscleRegion(MuscleGroup.BACK, r(0.37f, 0.20f, 0.63f, 0.36f)),
    MuscleRegion(MuscleGroup.SHOULDERS, r(0.27f, 0.13f, 0.42f, 0.20f)),
    MuscleRegion(MuscleGroup.SHOULDERS, r(0.58f, 0.13f, 0.73f, 0.20f)),
    MuscleRegion(MuscleGroup.TRICEPS, r(0.26f, 0.22f, 0.34f, 0.35f)),
    MuscleRegion(MuscleGroup.TRICEPS, r(0.66f, 0.22f, 0.74f, 0.35f)),
    MuscleRegion(MuscleGroup.FOREARMS, r(0.25f, 0.37f, 0.33f, 0.50f)),
    MuscleRegion(MuscleGroup.FOREARMS, r(0.67f, 0.37f, 0.75f, 0.50f)),
    MuscleRegion(MuscleGroup.GLUTES, r(0.39f, 0.50f, 0.495f, 0.60f)),
    MuscleRegion(MuscleGroup.GLUTES, r(0.505f, 0.50f, 0.61f, 0.60f)),
    MuscleRegion(MuscleGroup.HAMSTRINGS, r(0.38f, 0.61f, 0.49f, 0.74f)),
    MuscleRegion(MuscleGroup.HAMSTRINGS, r(0.51f, 0.61f, 0.62f, 0.74f)),
    MuscleRegion(MuscleGroup.CALVES, r(0.39f, 0.74f, 0.48f, 0.88f)),
    MuscleRegion(MuscleGroup.CALVES, r(0.52f, 0.74f, 0.61f, 0.88f))
)

private val SILHOUETTE = Color(0xFF16283A)
private val FRIED_COLOR = Color(0xFFE53935)
private val RECOVERING_COLOR = Color(0xFFFFA000)
private val RESTED_COLOR = Color(0xFF3D5A4C)
private val UNSELECTED_COLOR = Color(0xFF2A3A4A)
private val SELECTED_COLOR = Color(0xFF00BCD4)

private fun DrawScope.drawSilhouette(w: Float, h: Float) {
    // head
    drawCircle(SILHOUETTE, radius = w * 0.058f, center = Offset(w * 0.5f, h * 0.062f))
    // torso
    drawLine(
        SILHOUETTE, Offset(w * 0.5f, h * 0.13f), Offset(w * 0.5f, h * 0.50f),
        strokeWidth = w * 0.20f, cap = StrokeCap.Round
    )
    // arms
    drawLine(
        SILHOUETTE, Offset(w * 0.37f, h * 0.16f), Offset(w * 0.29f, h * 0.50f),
        strokeWidth = w * 0.075f, cap = StrokeCap.Round
    )
    drawLine(
        SILHOUETTE, Offset(w * 0.63f, h * 0.16f), Offset(w * 0.71f, h * 0.50f),
        strokeWidth = w * 0.075f, cap = StrokeCap.Round
    )
    // legs
    drawLine(
        SILHOUETTE, Offset(w * 0.44f, h * 0.52f), Offset(w * 0.43f, h * 0.93f),
        strokeWidth = w * 0.10f, cap = StrokeCap.Round
    )
    drawLine(
        SILHOUETTE, Offset(w * 0.56f, h * 0.52f), Offset(w * 0.57f, h * 0.93f),
        strokeWidth = w * 0.10f, cap = StrokeCap.Round
    )
}

private fun recoveryColor(state: RecoveryState?): Color = when (state) {
    RecoveryState.FRIED -> FRIED_COLOR
    RecoveryState.RECOVERING -> RECOVERING_COLOR
    RecoveryState.READY, RecoveryState.NEVER, null -> RESTED_COLOR
}

@Composable
fun BodyMapView(
    mode: BodyMapMode,
    statuses: Map<MuscleGroup, RecoveryState>,
    selected: Set<MuscleGroup>,
    showFront: Boolean,
    onMuscleTap: (MuscleGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val regions = remember(showFront) { if (showFront) frontRegions() else backRegions() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.62f)
            .onSizeChanged { canvasSize = it.toSize() }
            .pointerInput(regions, mode) {
                detectTapGestures { offset ->
                    if (canvasSize == Size.Zero) return@detectTapGestures
                    val point = Offset(
                        offset.x / canvasSize.width,
                        offset.y / canvasSize.height
                    )
                    regions.firstOrNull { it.rect.contains(point) }?.let {
                        onMuscleTap(it.muscle)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        drawSilhouette(w, h)
        val corner = CornerRadius(w * 0.03f, w * 0.03f)
        for (region in regions) {
            val r = region.rect
            val px = Rect(
                r.left * w, r.top * h, r.right * w, r.bottom * h
            )
            val fill = when (mode) {
                BodyMapMode.VIEW -> recoveryColor(statuses[region.muscle])
                BodyMapMode.LOG ->
                    if (selected.contains(region.muscle)) SELECTED_COLOR else UNSELECTED_COLOR
            }
            drawRoundRect(fill, topLeft = px.topLeft, size = px.size, cornerRadius = corner)
            if (mode == BodyMapMode.LOG && selected.contains(region.muscle)) {
                drawRoundRect(
                    Color.White, topLeft = px.topLeft, size = px.size,
                    cornerRadius = corner, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.008f)
                )
            }
        }
    }
}
