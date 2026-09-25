package com.example.vitalwearclonev1.gridbattle

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.github.cfogrady.vb.dim.card.BemCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sin

/**
 * Real-time grid battle screen, player vs AI (2026-09-25).
 *
 * Drives [BattleEngine] on a ~60fps loop and renders its snapshot:
 * 6x3 grid, sprite fighters, projectiles, HP bars, d-pad + buster/sword/
 * chip controls, chip-select overlay, and the victory/defeat flow.
 */
data class BattleSetup(
    val ownerId: String,
    val playerCardName: String,
    val playerCharId: Int,
    val displayName: String,
    val config: BattleConfig,
    val enemyCardName: String,
    val enemyCharId: Int
)

fun chipElementColor(e: ChipElement): Color = when (e) {
    ChipElement.FIRE -> Color(0xFFFF6B35)
    ChipElement.WATER -> Color(0xFF35A7FF)
    ChipElement.ELEC -> Color(0xFFFFD935)
    ChipElement.WOOD -> Color(0xFF7BFF6B)
    ChipElement.SWORD -> Color(0xFFFF8A9E)
    ChipElement.WIND -> Color(0xFF7BFFE0)
    ChipElement.CURSOR -> Color(0xFFC77BFF)
    ChipElement.BREAK -> Color(0xFFB0B0B0)
    ChipElement.PLUS -> Color(0xFF7BFFB0)
    ChipElement.NULL -> Color(0xFF35E0FF)
}

private fun charBaseIndex(characterId: Int, isBem: Boolean): Int {
    if (isBem) return 54 + (characterId * 14)
    var currentIdx = 10
    for (i in 0 until characterId) {
        currentIdx += when (i) { 0 -> 6; 1 -> 7; else -> 14 }
    }
    return currentIdx
}

@Composable
fun GridBattleScreen(setup: BattleSetup, onExit: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var fightId by remember { mutableStateOf(0) }
    val engine = remember(setup, fightId) { BattleEngine(setup.config) }
    var snap by remember(setup, fightId) { mutableStateOf(engine.snapshot()) }

    var playerBmp by remember { mutableStateOf<Bitmap?>(null) }
    var enemyBmp by remember { mutableStateOf<Bitmap?>(null) }

    // Attack FX state: NaviCust core animation key, or a default flash.
    var coreFx by remember { mutableStateOf<String?>(null) }
    var defaultFx by remember { mutableStateOf(false) }
    val fxProg = remember { Animatable(0f) }

    fun playAttackFx(animKey: String?) {
        scope.launch {
            if (animKey != null) coreFx = animKey else defaultFx = true
            fxProg.snapTo(0f)
            fxProg.animateTo(1f, tween(450))
            coreFx = null
            defaultFx = false
        }
    }

    // Load fighter sprites (same approach as adventure mode).
    LaunchedEffect(setup) {
        withContext(Dispatchers.IO) {
            try {
                val cm = CardManager(context)
                val card = cm.getCard(setup.playerCardName)
                val isBem = card is BemCard
                val base = charBaseIndex(setup.playerCharId, isBem)
                playerBmp = card?.spriteData?.sprites?.getOrNull(base + 1)
                    ?.let { SpriteBitmapHandler.getBitmap(it) }

                val names = cm.listCards().filter { it != setup.playerCardName }
                val eName = setup.enemyCardName.ifBlank { names.randomOrNull() ?: setup.playerCardName }
                val eCard = cm.getCard(eName) ?: card
                val eIsBem = eCard is BemCard
                val entries = try {
                    eCard?.characterStats?.characterEntries?.size ?: 1
                } catch (e: Exception) { 1 }
                val eChar = setup.enemyCharId.coerceIn(0, maxOf(0, entries - 1))
                val eBase = charBaseIndex(eChar, eIsBem)
                enemyBmp = eCard?.spriteData?.sprites?.getOrNull(eBase + 1)
                    ?.let { SpriteBitmapHandler.getBitmap(it) }
            } catch (e: Exception) {
                // Sprites stay null; fighters render as fallback shapes.
            }
        }
    }

    // Game loop.
    LaunchedEffect(engine) {
        var last = System.nanoTime()
        while (snap.winner == null) {
            delay(16)
            val now = System.nanoTime()
            val dt = ((now - last) / 1e9f).coerceAtMost(0.1f)
            last = now
            engine.update(dt)
            snap = engine.snapshot()
        }
    }

    Box(Modifier.fillMaxSize().background(Color(5, 5, 20))) {
        Column(Modifier.fillMaxSize()) {
            // ---- HUD ----
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(setup.displayName, color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    LinearProgressIndicator(
                        progress = (snap.playerHp / snap.playerMaxHp).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = Color.Green, backgroundColor = Color(60, 0, 0)
                    )
                    Text(
                        "${snap.playerHp.toInt()} / ${snap.playerMaxHp.toInt()}",
                        color = Color.White, fontSize = 10.sp
                    )
                    if (snap.charging || snap.charge > 0f) {
                        LinearProgressIndicator(
                            progress = snap.charge,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color.Yellow, backgroundColor = Color(40, 40, 0)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(setup.config.enemyName, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    LinearProgressIndicator(
                        progress = (snap.enemyHp / snap.enemyMaxHp).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = Color.Red, backgroundColor = Color(60, 0, 0)
                    )
                    Text(
                        "${snap.enemyHp.toInt()} / ${snap.enemyMaxHp.toInt()}",
                        color = Color.White, fontSize = 10.sp
                    )
                }
            }

            // ---- Arena ----
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                val wPx = constraints.maxWidth.toFloat()
                val hPx = constraints.maxHeight.toFloat()
                val cell = min(wPx / 6f, hPx / 3f)
                val ox = (wPx - cell * 6f) / 2f
                val oy = (hPx - cell * 3f) / 2f
                val fxSizePx = cell * 2.5f

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
                    if (defaultFx) {
                        val p = fxProg.value
                        drawCircle(
                            Color.White.copy(alpha = (1f - p) * 0.8f),
                            radius = cell * (0.3f + 1.1f * p),
                            center = Offset(ox + (snap.px + 0.5f) * cell, oy + (snap.py + 0.5f) * cell),
                            style = Stroke(width = 10f * (1f - p) + 2f)
                        )
                    }
                }

                // NaviCust core attack animation overlay, centered on the player.
                if (coreFx != null) {
                    Box(
                        Modifier
                            .size(with(density) { fxSizePx.toDp() })
                            .offset {
                                IntOffset(
                                    (ox + (snap.px + 0.5f) * cell - fxSizePx / 2f).toInt(),
                                    (oy + (snap.py + 0.5f) * cell - fxSizePx / 2f).toInt()
                                )
                            }
                    ) {
                        CoreAttackFx(animKey = coreFx!!, progress = fxProg.value, modifier = Modifier.fillMaxSize())
                    }
                }
            }

            // ---- Chip gauge ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CHIP", color = Color.Gray, fontSize = 10.sp)
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = snap.gauge,
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (snap.canCustom) Color.Yellow else Color(0, 120, 200),
                    backgroundColor = Color(30, 30, 50)
                )
            }
            Spacer(Modifier.height(4.dp))

            // ---- Controls ----
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // D-pad
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { engine.movePlayer(0, -1) }, modifier = Modifier.size(46.dp)) { Text("▲", fontSize = 16.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { engine.movePlayer(-1, 0) }, modifier = Modifier.size(46.dp)) { Text("◀", fontSize = 16.sp) }
                        Spacer(Modifier.width(46.dp))
                        Button(onClick = { engine.movePlayer(1, 0) }, modifier = Modifier.size(46.dp)) { Text("▶", fontSize = 16.sp) }
                    }
                    Button(onClick = { engine.movePlayer(0, 1) }, modifier = Modifier.size(46.dp)) { Text("▼", fontSize = 16.sp) }
                }
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Sword
                    Box(
                        Modifier.size(58.dp)
                            .background(Color(160, 30, 30), RoundedCornerShape(29.dp))
                            .clickable {
                                val r = engine.playerSword()
                                if (r != null) playAttackFx(r.animKey)
                            },
                        contentAlignment = Alignment.Center
                    ) { Text("SWD", color = Color.White, fontWeight = FontWeight.Bold) }
                    // Buster (press & hold to charge)
                    Box(
                        Modifier.size(64.dp)
                            .background(
                                if (snap.charging) Color(0, 150, 200) else Color(0, 100, 160),
                                RoundedCornerShape(32.dp)
                            )
                            .pointerInput(engine) {
                                detectTapGestures(
                                    onPress = {
                                        engine.setCharging(true)
                                        tryAwaitRelease()
                                        engine.setCharging(false)
                                        val shot = engine.releaseBuster()
                                        if (shot != null) playAttackFx(shot.animKey)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) { Text("BST", color = Color.White, fontWeight = FontWeight.Bold) }
                    // Chip custom
                    val pulse = if (snap.canCustom) 0.55f + 0.45f * sin(snap.time * 8f) else 1f
                    Box(
                        Modifier.size(58.dp)
                            .background(
                                Color(150, 120, 0).copy(alpha = if (snap.canCustom) pulse else 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = snap.canCustom) { engine.openCustom() },
                        contentAlignment = Alignment.Center
                    ) { Text("CHIP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                }
            }
        }

        // ---- Chip select overlay ----
        if (snap.paused && snap.hand.isNotEmpty()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp)
                        .background(Color(15, 15, 40), RoundedCornerShape(12.dp))
                        .border(2.dp, Color.Yellow, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CHIP SELECT", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("First pick sets the code — match it!", color = Color.Gray, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    snap.hand.forEachIndexed { i, chip ->
                        val sel = i in snap.selected
                        val col = chipElementColor(chip.element)
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (sel) col.copy(alpha = 0.35f) else Color(30, 30, 55),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(2.dp, if (sel) Color.Yellow else col, RoundedCornerShape(8.dp))
                                .clickable { engine.toggleChipSelect(i) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(chip.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${chip.tier} • ${chip.element} • ${chip.damage}${if (chip.hits > 1) "x${chip.hits}" else ""}",
                                    color = Color.LightGray, fontSize = 10.sp
                                )
                            }
                            Text(
                                chip.codes.joinToString(""),
                                color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { engine.fireSelected() },
                            enabled = snap.selected.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 140, 0))
                        ) { Text("GO!", color = Color.White) }
                        Button(
                            onClick = { engine.cancelCustom() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(120, 120, 120))
                        ) { Text("CANCEL", color = Color.White) }
                    }
                }
            }
        }

        // ---- Result overlay ----
        if (snap.winner != null) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (snap.winner == 0) "VICTORY!" else "DEFEAT",
                        color = if (snap.winner == 0) Color.Yellow else Color.Red,
                        fontSize = 44.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                fightId++
                                coreFx = null
                                defaultFx = false
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 120, 200))
                        ) { Text("REMATCH", color = Color.White) }
                        Button(
                            onClick = onExit,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(90, 90, 90))
                        ) { Text("EXIT", color = Color.White) }
                    }
                }
            }
        }
    }
}
