package com.example.vitalwearclonev1.gridbattle

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.vitalwearclonev1.common.SoundManager

/**
 * PvP guest screen (2026-09-25).
 *
 * The host runs the real [BattleEngine]; this screen renders the host's
 * snapshots (mirrored so the guest is on the left) and sends input packets.
 * Chip select, gauge and charge are managed locally with the same tuning
 * as the engine — the host is authoritative for what actually lands.
 */
class GuestChipHand(deckIds: List<Int>) {
    private var deck = deckIds.shuffled().toMutableList()
    private val discards = mutableListOf<Int>()
    val hand = mutableListOf<BattleChip>()
    val selected = mutableSetOf<Int>()
    var open = false
        private set

    /** Bumped on every mutation so Compose recomposes. */
    var version by mutableStateOf(0)
        private set

    fun openCustom(): Boolean {
        if (open) return false
        if (deck.size < 5) {
            deck.addAll(discards.shuffled())
            discards.clear()
        }
        if (deck.isEmpty()) return false
        repeat(minOf(5, deck.size)) {
            ChipLibrary.byId(deck.removeAt(0))?.let { hand.add(it) }
        }
        selected.clear()
        open = true
        version++
        return true
    }

    fun cancel() {
        if (!open) return
        deck.addAll(0, hand.map { it.id })
        hand.clear()
        selected.clear()
        open = false
        version++
    }

    /** Same code-matching rules as the engine. */
    fun toggle(index: Int) {
        if (!open || index !in hand.indices) return
        if (index in selected) {
            selected.remove(index)
            version++
            return
        }
        val chip = hand[index]
        if (selected.isEmpty()) {
            selected.add(index)
            version++
            return
        }
        val first = hand[selected.first()]
        val reqCode: Char? = first.codes.firstOrNull { it != '*' }
        val ok = selected.any { hand[it].id == chip.id } ||
            '*' in chip.codes || '*' in first.codes ||
            reqCode == null || reqCode in chip.codes
        if (ok) {
            selected.add(index)
            version++
        }
    }

    /** Returns the selected chip ids and closes the hand. */
    fun fire(): List<Int> {
        if (!open || selected.isEmpty()) return emptyList()
        val ordered = selected.sorted()
        hand.forEachIndexed { i, c -> if (i !in selected) deck.add(c.id) }
        discards.addAll(ordered.map { hand[it].id })
        val ids = ordered.map { hand[it].id }
        hand.clear()
        selected.clear()
        open = false
        version++
        return ids
    }

    fun reset(deckIds: List<Int>) {
        deck = deckIds.shuffled().toMutableList()
        discards.clear()
        hand.clear()
        selected.clear()
        open = false
        version++
    }
}

@Composable
fun PvpGuestScreen(
    guestInfo: PvpFighterInfo,
    hostInfo: PvpFighterInfo,
    net: PvpNetManager,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var netSnap by remember { mutableStateOf<NetSnapshot?>(null) }
    var endWinner by remember { mutableStateOf<Int?>(null) } // 0 = host won, 1 = guest won
    var peerLeft by remember { mutableStateOf(false) }
    var rematchSent by remember { mutableStateOf(false) }

    var gauge by remember { mutableStateOf(0f) }
    var charging by remember { mutableStateOf(false) }
    var charge by remember { mutableStateOf(0f) }
    val chipHand = remember { GuestChipHand(guestInfo.deck) }

    // Attack FX state (same behavior as the solo screen).
    var coreFx by remember { mutableStateOf<String?>(null) }
    var fxAtPlayer by remember { mutableStateOf(true) }
    var defaultFx by remember { mutableStateOf(false) }
    val fxProg = remember { Animatable(0f) }

    fun playAttackFx(animKey: String?, atPlayer: Boolean = true) {
        scope.launch {
            fxAtPlayer = atPlayer
            if (animKey != null) coreFx = animKey else defaultFx = true
            fxProg.snapTo(0f)
            fxProg.animateTo(1f, tween(450))
            coreFx = null
            defaultFx = false
        }
    }

    fun doExit() {
        try { net.send("BYE") } catch (e: Exception) { }
        onExit()
    }

    // ---- Net listener ----
    DisposableEffect(net) {
        net.listener = object : PvpNetManager.Listener {
            override fun onMessage(fromEndpoint: String, msg: String) {
                when {
                    msg.startsWith("SNAP|") -> {
                        try { netSnap = parseSnapshot(msg) } catch (e: Exception) { }
                    }
                    msg.startsWith("END|") -> {
                        endWinner = msg.removePrefix("END|").toIntOrNull()
                    }
                    msg == "RESTART" -> {
                        netSnap = null
                        endWinner = null
                        peerLeft = false
                        rematchSent = false
                        gauge = 0f
                        charge = 0f
                        charging = false
                        chipHand.reset(guestInfo.deck)
                        coreFx = null
                        defaultFx = false
                    }
                    msg == "BYE" -> peerLeft = true
                }
            }

            override fun onEndpointFound(endpointId: String, name: String) {}
            override fun onEndpointLost(endpointId: String) {}
            override fun onConnected(endpointId: String) {}
            override fun onDisconnected(endpointId: String) { peerLeft = true }
        }
        onDispose { net.listener = null }
    }

    // ---- Host attack FX: play each snapshot's events once, at the host's fighter ----
    var lastFxTime by remember { mutableStateOf(-1f) }
    LaunchedEffect(netSnap) {
        val s = netSnap ?: return@LaunchedEffect
        if (s.time > lastFxTime) {
            lastFxTime = s.time
            s.fx.forEach { ev -> playAttackFx(ev.animKey, atPlayer = false) }
        }
    }

    // ---- Game SFX, 2026-09-26 ----
    LaunchedEffect(Unit) { SoundManager.init(context) }
    LaunchedEffect(endWinner) {
        val w = endWinner ?: return@LaunchedEffect
        SoundManager.play(if (w == 1) "win" else "lose")
    }

    // ---- Local gauge + charge timers (same tuning as the engine) ----
    LaunchedEffect(Unit) {
        var last = System.nanoTime()
        while (true) {
            delay(16)
            val now = System.nanoTime()
            val dt = ((now - last) / 1e9f).coerceAtMost(0.1f)
            last = now
            if (!chipHand.open && endWinner == null) gauge = min(1f, gauge + dt / 5f)
            if (charging) charge = min(1f, charge + dt / 0.9f * guestInfo.chargeRate)
        }
    }

    // ---- Sprites: me on the left, the host on the right ----
    var playerBmp by remember { mutableStateOf<Bitmap?>(null) }
    var enemyBmp by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(guestInfo, hostInfo) {
        withContext(Dispatchers.IO) {
            try {
                val cm = CardManager(context)
                val myCard = cm.getCard(guestInfo.cardName)
                val myIsBem = myCard is BemCard
                playerBmp = myCard?.spriteData?.sprites
                    ?.getOrNull(charBaseIndex(guestInfo.charId, myIsBem) + 1)
                    ?.let { SpriteBitmapHandler.getBitmap(it) }
                val hostCard = cm.getCard(hostInfo.cardName) ?: myCard
                val hostIsBem = hostCard is BemCard
                val entries = try {
                    hostCard?.characterStats?.characterEntries?.size ?: 1
                } catch (e: Exception) { 1 }
                enemyBmp = hostCard?.spriteData?.sprites
                    ?.getOrNull(charBaseIndex(hostInfo.charId.coerceIn(0, maxOf(0, entries - 1)), hostIsBem) + 1)
                    ?.let { SpriteBitmapHandler.getBitmap(it) }
            } catch (e: Exception) {
                // Sprites stay null; fighters render as fallback shapes.
            }
        }
    }

    val view = netSnap?.toGuestView(guestInfo.maxHp, hostInfo.maxHp)
    // Subscribe to chip-hand mutations.
    @Suppress("UNUSED_EXPRESSION") chipHand.version

    Box(Modifier.fillMaxSize().background(Color(5, 5, 20))) {
        if (view == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CONNECTED", color = Color(0xFF7BFF6B), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Waiting for battle data...", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                // ---- HUD ----
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(guestInfo.name, color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LinearProgressIndicator(
                            progress = (view.playerHp / view.playerMaxHp).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = Color.Green, backgroundColor = Color(60, 0, 0)
                        )
                        Text("${view.playerHp.toInt()} / ${view.playerMaxHp.toInt()}", color = Color.White, fontSize = 10.sp)
                        if (charging || charge > 0f) {
                            LinearProgressIndicator(
                                progress = charge,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color.Yellow, backgroundColor = Color(40, 40, 0)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(hostInfo.name, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LinearProgressIndicator(
                            progress = (view.enemyHp / view.enemyMaxHp).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = Color.Red, backgroundColor = Color(60, 0, 0)
                        )
                        Text("${view.enemyHp.toInt()} / ${view.enemyMaxHp.toInt()}", color = Color.White, fontSize = 10.sp)
                    }
                }

                // ---- Arena ----
                BattleArenaView(
                    snap = view,
                    playerBmp = playerBmp,
                    enemyBmp = enemyBmp,
                    fx = ArenaFx(
                        coreKey = coreFx,
                        defaultFlash = defaultFx,
                        progress = fxProg.value,
                        atPlayer = fxAtPlayer
                    ),
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)
                )

                // ---- Chip gauge ----
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CHIP", color = Color.Gray, fontSize = 10.sp)
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = gauge,
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (gauge >= 1f) Color.Yellow else Color(0, 120, 200),
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { net.send("IN|m:0,-1") }, modifier = Modifier.size(46.dp)) { Text("▲", fontSize = 16.sp) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { net.send("IN|m:-1,0") }, modifier = Modifier.size(46.dp)) { Text("◀", fontSize = 16.sp) }
                            Spacer(Modifier.width(46.dp))
                            Button(onClick = { net.send("IN|m:1,0") }, modifier = Modifier.size(46.dp)) { Text("▶", fontSize = 16.sp) }
                        }
                        Button(onClick = { net.send("IN|m:0,1") }, modifier = Modifier.size(46.dp)) { Text("▼", fontSize = 16.sp) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(58.dp)
                                .background(Color(160, 30, 30), RoundedCornerShape(29.dp))
                                .clickable {
                                    net.send("IN|s")
                                    playAttackFx(guestInfo.swordAnimKey, atPlayer = true)
                                    SoundManager.play("sword")
                                },
                            contentAlignment = Alignment.Center
                        ) { Text("SWD", color = Color.White, fontWeight = FontWeight.Bold) }
                        Box(
                            Modifier.size(64.dp)
                                .background(
                                    if (charging) Color(0, 150, 200) else Color(0, 100, 160),
                                    RoundedCornerShape(32.dp)
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            charging = true
                                            net.send("IN|c:1")
                                            tryAwaitRelease()
                                            charging = false
                                            val chargedShot = charge > 0.7f
                                            charge = 0f
                                            net.send("IN|c:0")
                                            net.send("IN|b")
                                            playAttackFx(guestInfo.busterAnimKey, atPlayer = true)
                                            SoundManager.play(if (chargedShot) "buster_charged" else "buster")
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) { Text("BST", color = Color.White, fontWeight = FontWeight.Bold) }
                        val pulse = if (gauge >= 1f) 0.55f + 0.45f * sin(view.time * 8f) else 1f
                        Box(
                            Modifier.size(58.dp)
                                .background(
                                    Color(150, 120, 0).copy(alpha = if (gauge >= 1f) pulse else 0.35f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = gauge >= 1f && !chipHand.open) { chipHand.openCustom() },
                            contentAlignment = Alignment.Center
                        ) { Text("CHIP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    }
                }
            }
        }

        // ---- Chip select overlay ----
        if (chipHand.open && chipHand.hand.isNotEmpty()) {
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
                    chipHand.hand.forEachIndexed { i, chip ->
                        val sel = i in chipHand.selected
                        val col = chipElementColor(chip.element)
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (sel) col.copy(alpha = 0.35f) else Color(30, 30, 55),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(2.dp, if (sel) Color.Yellow else col, RoundedCornerShape(8.dp))
                                .clickable { chipHand.toggle(i) }
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
                            Text(chip.codes.joinToString(""), color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val kinds = chipHand.selected.sorted()
                                    .mapNotNull { chipHand.hand.getOrNull(it)?.effectKind?.name }
                                    .distinct()
                                val ids = chipHand.fire()
                                if (ids.isNotEmpty()) {
                                    net.send("IN|chip:" + ids.joinToString(","))
                                    gauge = 0f
                                    kinds.forEach { SoundManager.play(SoundManager.forEffectKind(it)) }
                                }
                            },
                            enabled = chipHand.selected.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 140, 0))
                        ) { Text("GO!", color = Color.White) }
                        Button(
                            onClick = { chipHand.cancel() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(120, 120, 120))
                        ) { Text("CANCEL", color = Color.White) }
                    }
                }
            }
        }

        // ---- Peer-left overlay ----
        if (peerLeft && endWinner == null) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OPPONENT LEFT", color = Color.Yellow, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("The other phone disconnected.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { onExit() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(90, 90, 90))
                    ) { Text("EXIT", color = Color.White) }
                }
            }
        }

        // ---- Result overlay ----
        if (endWinner != null) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (endWinner == 1) "VICTORY!" else "DEFEAT",
                        color = if (endWinner == 1) Color.Yellow else Color.Red,
                        fontSize = 44.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(24.dp))
                    if (rematchSent) {
                        Text("Waiting for host...", color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    net.send("REQ_REMATCH")
                                    rematchSent = true
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 120, 200))
                            ) { Text("REMATCH", color = Color.White) }
                            Button(
                                onClick = { doExit() },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(90, 90, 90))
                            ) { Text("EXIT", color = Color.White) }
                        }
                    }
                    if (rematchSent) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { doExit() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(90, 90, 90))
                        ) { Text("EXIT", color = Color.White) }
                    }
                }
            }
        }
    }
}
