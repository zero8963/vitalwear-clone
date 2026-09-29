package com.example.vitalwearclonev1

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import com.example.vitalwearclonev1.monster.CareManager
import com.example.vitalwearclonev1.monster.CareTuning
import androidx.health.connect.client.PermissionController
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.card.StandaloneImportCardActivity
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.example.vitalwearclonev1.game.EducationalGameActivity
import com.example.vitalwearclonev1.lab.DigimonLabActivity
import com.example.vitalwearclonev1.ui.EvolutionAnimation
import com.example.vitalwearclonev1.ui.EvolutionRequest
import com.example.vitalwearclonev1.monster.BattleOpponent
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.example.vitalwearclonev1.sensor.PhoneGpsManager
import com.example.vitalwearclonev1.ui.OnlineMultiplayerScreen
import com.github.cfogrady.vb.dim.card.BemCard
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class PhoneMainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {
    private val activeOpponent = mutableStateOf<BattleOpponent?>(null)
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Timber for debugging
        if (timber.log.Timber.treeCount == 0) {
            timber.log.Timber.plant(timber.log.Timber.DebugTree())
        }

        Wearable.getMessageClient(this).addListener(this)

        val permissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)

        val toRequest = permissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            requestPermissions(toRequest.toTypedArray(), 1001)
        }



        setContent {
            PhoneMainScreen(activeOpponent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        timber.log.Timber.d("Message received: ${messageEvent.path}")
        if (messageEvent.path == "/P2P_BATTLE_REQ" || messageEvent.path == "/P2P_BATTLE_ACK") {
            try {
                val data = messageEvent.data ?: return
                val dis = DataInputStream(ByteArrayInputStream(data))
                val cardName = readString(dis)
                val charId = dis.readInt()
                val atk = dis.readInt()
                val hp = dis.readInt()
                val spd = dis.readInt()
                val def = dis.readInt()
                val seed = try { dis.readLong() } catch (e: Exception) {
                    timber.log.Timber.w("No seed in message, using current time")
                    System.currentTimeMillis()
                }
                val isBem = try { dis.readByte().toInt() == 1 } catch (e: Exception) { false }
                val dimId = try { dis.readInt() } catch (e: Exception) { 0 }

                timber.log.Timber.d("P2P Battle message parsed: $cardName, seed: $seed, isBem: $isBem, dimId: $dimId")

                val isInitiator = messageEvent.path == "/P2P_BATTLE_ACK"

                // If it's a request, send back our stats so the other device can also battle
                if (messageEvent.path == "/P2P_BATTLE_REQ") {
                    val context = this
                    scope.launch(Dispatchers.IO) {
                        try {
                            val monsterManager = PhoneMonsterManager(context)
                            val myState = monsterManager.getCurrentMonster()
                            if (myState != null) {
                                timber.log.Timber.d("Sending P2P ACK back to watch")
                                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                                val messageClient = Wearable.getMessageClient(context)
                                val payload = ByteArrayOutputStream().use { bos ->
                                    val dos = DataOutputStream(bos)
                                    val cardManager = com.example.vitalwearclonev1.card.CardManager(context)
                                    val myCard = cardManager.getCard(myState.cardName)
                                    val dimIdAck = myCard?.header?.dimId ?: 0
                                    dos.write(myState.cardName.toByteArray(java.nio.charset.Charset.defaultCharset()))
                                    dos.writeByte(0)
                                    dos.writeInt(myState.characterId)
                                    dos.writeInt(myState.baseAp + myState.attackBonus)
                                    dos.writeInt(myState.baseHp + myState.healthBonus)
                                    dos.writeInt(myState.speedBonus)
                                    dos.writeInt(myState.defenseBonus)
                                    dos.writeLong(seed)
                                    dos.writeByte(if (myState.isBem) 1 else 0)
                                    dos.writeInt(dimIdAck)
                                    dos.flush()
                                    bos.toByteArray()
                                }
                                for (node in nodes) {
                                    messageClient.sendMessage(node.id, "/P2P_BATTLE_ACK", payload).await()
                                }
                            }
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "Error sending ACK")
                        }
                    }
                }

                activeOpponent.value = BattleOpponent(cardName, charId, atk, hp, spd, def, seed = seed, isInitiator = isInitiator, isBem = isBem, dimId = dimId)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Error parsing challenge")
            }
        } else if (messageEvent.path == "/REQUEST_HEALTH_SYNC") {
            scope.launch {
                val healthSyncManager = com.example.vitalwearclonev1.communication.PhoneHealthSyncManager(this@PhoneMainActivity)
                if (healthSyncManager.hasAllPermissions()) {
                    healthSyncManager.syncNow()
                }
            }
        }
    }

    private fun readString(inputStream: java.io.InputStream): String {
        val bytes = mutableListOf<Byte>()
        var b = inputStream.read()
        while (b != 0 && b != -1) {
            bytes.add(b.toByte())
            b = inputStream.read()
        }
        return String(bytes.toByteArray(), java.nio.charset.Charset.defaultCharset())
    }
}

@Composable
fun PhoneMainScreen(activeOpponent: MutableState<BattleOpponent?>) {
    val context = LocalContext.current
    val monsterManager = remember { PhoneMonsterManager(context) }
    val isExpiredState = remember { mutableStateOf(monsterManager.isExpired()) }
    val healthSyncManager = remember { com.example.vitalwearclonev1.communication.PhoneHealthSyncManager(context) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted: Set<String> ->
        if (granted.containsAll(healthSyncManager.permissions)) {
            healthSyncManager.startPeriodicSync()
        }
    }

    LaunchedEffect(Unit) {
        if (healthSyncManager.getSdkStatus() == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
            if (!healthSyncManager.hasAllPermissions()) {
                try {
                    healthPermissionLauncher.launch(healthSyncManager.permissions)
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to launch Health Connect permissions")
                }
            } else {
                healthSyncManager.startPeriodicSync()
            }
        } else {
            timber.log.Timber.w("Health Connect SDK not available, skipping auto-sync startup")
        }
    }


    val isPhoneConnected = remember { mutableStateOf<Boolean?>(null) }

    // Connection Polling Loop & Daily Reset Check
    // Battery fix (2026-09-25): was polling every 5s and — worse — calling
    // startTracking() each time, which leaked a new HIGH_ACCURACY GPS callback
    // on every pass (hundreds of live GPS listeners after an hour). Now it only
    // runs the cheap day-reset check and polls every 30s.
    LaunchedEffect(Unit) {
        while(true) {
            try {
                isExpiredState.value = monsterManager.isExpired()
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                isPhoneConnected.value = nodes.isNotEmpty()

                // Day rollover check only — does NOT register for location updates.
                com.example.vitalwearclonev1.sensor.PhoneGpsManager(context).checkDayReset()

            } catch (e: Exception) {
                isPhoneConnected.value = false
            }
            delay(30000)
        }
    }


    Scaffold(
        bottomBar = {
            BottomNavigation(
                backgroundColor = Color(0, 50, 100),
                contentColor = Color.White,
                modifier = Modifier.height(72.dp)
            ) {
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Home, "Home", modifier = Modifier.size(28.dp)) },
                    label = { Text("Home", fontSize = 12.sp) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.List, "Lab", modifier = Modifier.size(28.dp)) },
                    label = { Text("Lab", fontSize = 12.sp) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Send, "Transfer", modifier = Modifier.size(28.dp)) },
                    label = { Text("Transfer", fontSize = 12.sp) },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Share, "Multiplayer", modifier = Modifier.size(28.dp)) },
                    label = { Text("Multi", fontSize = 12.sp) },
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Star, "Game", modifier = Modifier.size(28.dp)) },
                    label = { Text("Game", fontSize = 12.sp) },
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Settings, "Settings", modifier = Modifier.size(28.dp)) },
                    label = { Text("Settings", fontSize = 12.sp) },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val btManager = remember { com.example.vitalwearclonev1.communication.BluetoothBattleManager(context) }

            if (isExpiredState.value && selectedTab != 1 && selectedTab != 2 && selectedTab != 3) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Your Digimon has expired", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        val deathCause = monsterManager.getDeathCause()
                        val deathText = when (deathCause) {
                            "critical" -> "It lost its final battle while in critical condition."
                            "overwork" -> "It was overworked in battle."
                            "age" -> "It lived a full life and passed away of old age."
                            else -> "It passed away due to neglect."
                        }
                        Text("$deathText Please visit the Lab to hatch a new egg or select a different partner.", color = Color.LightGray, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { selectedTab = 1 }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 80, 150))) {
                            Text("Go to Lab", color = Color.White)
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> HomeScreen(monsterManager, isPhoneConnected.value)
                    1 -> {
                        LaunchedEffect(Unit) {
                            context.startActivity(Intent(context, DigimonLabActivity::class.java))
                            selectedTab = 0
                            isExpiredState.value = false // Just in case they select something
                        }
                    }
                    2 -> {
                        LaunchedEffect(Unit) {
                            context.startActivity(Intent(context, StandaloneImportCardActivity::class.java))
                            selectedTab = 0
                        }
                    }
                    3 -> SettingsScreen(monsterManager, healthPermissionLauncher)
                    4 -> {
                        var isOnline by remember { mutableStateOf(false) }
                        if (isOnline) {
                            OnlineMultiplayerScreen(
                                monsterManager = monsterManager,
                                onBattleStart = { opponent ->
                                    activeOpponent.value = opponent
                                },
                                onBack = { isOnline = false }
                            )
                        } else {
                            MultiplayerScreen(monsterManager, btManager) { opponent: BattleOpponent ->
                                activeOpponent.value = opponent
                            }
                            // Add button to switch to Online
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                                Button(onClick = { isOnline = true }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(10, 10, 30))) {
                                    Text("Go Online (Internet)", color = Color.White)
                                }
                            }
                        }
                    }
                    5 -> GameMenuScreen { mode, isLesson ->
                        val intent = Intent(context, EducationalGameActivity::class.java)
                        intent.putExtra("EXTRA_MODE", mode)
                        intent.putExtra("EXTRA_IS_LESSON", isLesson)
                        context.startActivity(intent)
                        selectedTab = 0
                    }
                }
            }
        }
        activeOpponent.value?.let { opponent ->
            P2PBattleOverlay(opponent, monsterManager) {
                activeOpponent.value = null
            }
        }
    }
}

@Composable
fun HomeScreen(monsterManager: PhoneMonsterManager, isWatchConnected: Boolean?) {
    val context = LocalContext.current
    val cardManager = remember { CardManager(context) }
    val monsterState = remember { mutableStateOf(monsterManager.getCurrentMonster()) }
    val idleSprites = remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val backgroundBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val currentFrame = remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val evolutionRequest = remember { mutableStateOf<EvolutionRequest?>(null) }

    // Loads the idle frames for a given card/character off the main thread.
    // Used by the sprite loader and to fetch the evolved form before playing
    // the evolution sequence.
    suspend fun loadIdleFrames(cardName: String, characterId: Int): List<Bitmap> =
        withContext(Dispatchers.IO) {
            val card = cardManager.getCard(cardName) ?: return@withContext emptyList()
            val isBem = card is BemCard
            val sprites = card.spriteData.sprites
            val indices = monsterManager.getIdleSpriteIndices(characterId, isBem)
            indices.mapNotNull { idx ->
                if (idx < sprites.size) SpriteBitmapHandler.getBitmap(sprites[idx]) else null
            }
        }

    // Portrait frame for the evolution reveal: the new form's high-detail portrait
    // (last sprite of the character's sheet), shown large like the original
    // hardware's splash before the play sprites.
    suspend fun loadPortraitFrame(cardName: String, characterId: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val card = cardManager.getCard(cardName) ?: return@withContext null
            val isBem = card is BemCard
            val sprites = card.spriteData.sprites
            val idx = monsterManager.getPortraitSpriteIndex(characterId, isBem, sprites)
            if (idx < sprites.size) SpriteBitmapHandler.getBitmap(sprites[idx]) else null
        }

    // Runs any form change (digivolve, force evolve, reverse) through the
    // evolution sequence. Skips the animation if the form didn't actually change
    // (e.g. blocked secret stage / final form).
    suspend fun evolveWithAnimation(doEvolve: () -> Unit) {
        val before = monsterManager.getCurrentMonster()
        val oldFrames = idleSprites.value.toList()
        doEvolve()
        val after = monsterManager.getCurrentMonster()
        monsterState.value = after
        if (before != null && after != null && after.characterId != before.characterId) {
            val newFrames = loadIdleFrames(after.cardName, after.characterId)
            if (newFrames.isNotEmpty()) {
                val portrait = loadPortraitFrame(after.cardName, after.characterId)
                evolutionRequest.value = EvolutionRequest(oldFrames, newFrames, portrait)
            }
        }
    }

    // Refresh state when activity resumes
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                monsterState.value = monsterManager.getCurrentMonster()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // End-of-tree lock state (2026-09-29 perf fix): computed OFF the main
    // thread when the monster's species changes, cached so composition never
    // does disk I/O. The old version called isAtMaxEvolution() synchronously
    // inside remember{} — getCard() reads a .bin from disk, which janked
    // every recomposition.
    val isAtMaxEvolution = remember { mutableStateOf(false) }
    LaunchedEffect(monsterState.value?.cardName, monsterState.value?.characterId) {
        val state = monsterState.value
        isAtMaxEvolution.value = if (state == null) {
            true
        } else {
            withContext(Dispatchers.IO) { monsterManager.isAtMaxEvolution() }
        }
    }

    val canEvolve = remember(monsterState.value, isAtMaxEvolution.value) {
        val state = monsterState.value
        if (state == null) false
        // End-of-tree lock: no onward paths = final form, disable the button
        // so a stray tap can't break the sprite.
        else if (isAtMaxEvolution.value) false
        else if (state.stage == 0) state.timeAlive >= 60
        else state.winsRequired > 0 && state.currentWins >= state.winsRequired
    }

    // Aging Loop (2026-09-29 perf fix): keyed on Unit so it doesn't restart
    // on every state change. The old LaunchedEffect(monsterState.value)
    // cancelled and relaunched the loop each time we wrote state below,
    // churning coroutines every 10 seconds.
    LaunchedEffect(Unit) {
        while (true) {
            val state = monsterState.value
            if (state != null && !state.isEvolutionPaused) {
                monsterManager.updateAging(10)
                val updated = monsterManager.getCurrentMonster()
                if (updated != null) {
                    monsterState.value = updated
                }
            }
            delay(10000)
        }
    }

    // Sprite Loading
    LaunchedEffect(monsterState.value?.cardName, monsterState.value?.characterId) {
        val state = monsterState.value
        if (state != null) {
            val card = withContext(Dispatchers.IO) { cardManager.getCard(state.cardName) }
            if (card != null) {
                val sprites = card.spriteData.sprites

                // Load Background (Index 1)
                if (sprites.size > 1) {
                    backgroundBitmap.value = withContext(Dispatchers.IO) {
                        SpriteBitmapHandler.getBitmap(sprites[1])
                    }
                }
            }
            idleSprites.value = loadIdleFrames(state.cardName, state.characterId)
        }
    }

    // Animation Loop
    LaunchedEffect(idleSprites.value) {
        if (idleSprites.value.size > 1) {
            while (true) {
                currentFrame.intValue = (currentFrame.intValue + 1) % idleSprites.value.size
                delay(500)
            }
        }
    }

    val showHatchDialog = remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var nicknameText by remember { mutableStateOf("") }
    val savedCards = remember { cardManager.listCards() }

    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Name your Digimon") },
            text = {
                TextField(value = nicknameText, onValueChange = { nicknameText = it }, label = { Text("Nickname") })
            },
            confirmButton = {
                Button(onClick = {
                    monsterManager.updateNickname(nicknameText)
                    monsterState.value = monsterManager.getCurrentMonster()
                    showNicknameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = { showNicknameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0, 20, 40))) {
        // DIM Background
        backgroundBitmap.value?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                alpha = 0.9f
            )
        }
        if (monsterState.value == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No Active Digimon", color = Color.White, fontSize = 20.sp)
                Button(onClick = {
                    if (savedCards.isNotEmpty()) {
                        showHatchDialog.value = true
                    } else {
                        Toast.makeText(context, "Go to Transfer to import a DIM first!", Toast.LENGTH_LONG).show()
                    }
                }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Hatch from Saved DIM")
                }
            }

            if (showHatchDialog.value) {
                AlertDialog(
                    onDismissRequest = { showHatchDialog.value = false },
                    title = { Text("Pick a DIM Card") },
                    text = {
                        Column {
                            var hatchQuery by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = hatchQuery,
                                onValueChange = { hatchQuery = it },
                                label = { Text("Search cards") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            val hatchShown = savedCards.filter { it.contains(hatchQuery, ignoreCase = true) }
                            if (hatchShown.isEmpty()) {
                                Text("No cards match.", color = Color.Gray, fontSize = 13.sp)
                            } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                                    items(hatchShown) { card ->
                                        TextButton(
                                            onClick = {
                                                val isBem = cardManager.getCard(card) is BemCard
                                                monsterManager.setCurrentMonster(
                                                    card, 0, isBem = isBem,
                                                    winsReq = monsterManager.rollWinsRequired()
                                                )
                                                monsterState.value = monsterManager.getCurrentMonster()
                                                showHatchDialog.value = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(card, modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHatchDialog.value = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        } else {
            val state = monsterState.value!!
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(32.dp))

                Text(
                    text = state.nickname ?: "Unnamed",
                    color = Color.Green,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    modifier = Modifier.clickable {
                        nicknameText = state.nickname ?: ""
                        showNicknameDialog = true
                    }
                )
                Text(state.cardName, color = Color.Cyan.copy(alpha = 0.7f), fontSize = 14.sp)

                // Connection Status
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(if (isWatchConnected == true) Color.Green else Color.Red, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isWatchConnected == true) "Watch Connected" else "Watch Offline",
                        color = if (isWatchConnected == true) Color.Green else Color.Red,
                        fontSize = 10.sp
                    )
                }

                Box(modifier = Modifier.size(200.dp).padding(20.dp), contentAlignment = Alignment.Center) {
                    if (idleSprites.value.isNotEmpty()) {
                        Image(
                            idleSprites.value[currentFrame.intValue % idleSprites.value.size].asImageBitmap(),
                            "Digimon",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    // Poor-condition skull: appears at 3 straight losses, stays through critical.
                    if (CareManager.showSkull(state.consecutiveLosses, state.criticalRemainingMs)) {
                        Text(
                            text = "☠",
                            color = Color.Black,
                            fontSize = 30.sp,
                            style = TextStyle(shadow = Shadow(Color.White, offset = Offset(1f, 1f), blurRadius = 4f)),
                            modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 4.dp)
                        )
                    }
                }

                // Critical-condition banner with live healing countdown.
                if (state.criticalRemainingMs > 0) {
                    Text(
                        text = "⚠ CRITICAL — ${CareManager.formatCriticalMs(state.criticalRemainingMs)}",
                        color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp
                    )
                    Text(
                        text = "Rest or finish exercises to heal. Losing a battle now will kill your Digimon!",
                        color = Color.Red.copy(alpha = 0.85f), fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                } else if (state.consecutiveLosses >= CareTuning.SKULL_WARNING_LOSSES) {
                    Text(
                        text = "${state.consecutiveLosses} straight losses — win a battle soon!",
                        color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }

                if (state.stage == 0) {
                    val remaining = (60 - state.timeAlive).coerceAtLeast(0)
                    Text("Hatching in: ${remaining}s", color = Color.Yellow, fontWeight = FontWeight.Bold)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        backgroundColor = Color(0, 40, 80),
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Evolution Requirements", color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Wins Needed: ${state.currentWins} / ${state.winsRequired}",
                                color = if (state.currentWins >= state.winsRequired) Color.Green else Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            LinearProgressIndicator(
                                progress = if (state.winsRequired > 0) state.currentWins.toFloat() / state.winsRequired.toFloat() else 0f,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                color = Color.Green,
                                backgroundColor = Color.DarkGray
                            )
                        }
                    }
                }

                if (state.isEvolutionPaused) {
                    Text("EVOLUTION PAUSED", color = Color.Red, fontWeight = FontWeight.Bold)
                }

                // Manual Digivolve Button
                Button(
                    onClick = {
                        scope.launch { evolveWithAnimation { monsterManager.evolve() } }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 8.dp),
                    enabled = canEvolve,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(200, 150, 0),
                        disabledBackgroundColor = Color.Gray.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (state.stage == 0) "HATCH" else "DIGIVOLVE",
                        fontWeight = FontWeight.Bold,
                        color = if (canEvolve) Color.White else Color.DarkGray
                    )
                }

                // Stats Display
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    backgroundColor = Color(0, 0, 0, 160),
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("ATK", state.attackBonus.toString(), Color.Red)
                        StatItem("HP", state.healthBonus.toString(), Color.Green)
                        StatItem("SPD", state.speedBonus.toString(), Color.Cyan)
                        StatItem("DEF", state.defenseBonus.toString(), Color.Yellow)
                    }
                }

                if (monsterManager.isDevModeEnabled()) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        backgroundColor = Color.DarkGray.copy(alpha = 0.5f),
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Developer Controls", color = Color.Gray, fontSize = 10.sp)
                            Row(modifier = Modifier.padding(top = 8.dp)) {
                                Button(
                                    modifier = Modifier.height(48.dp).weight(1f),
                                    onClick = {
                                        scope.launch { evolveWithAnimation { monsterManager.forceEvolve() } }
                                    }
                                ) {
                                    Text("Force Evolve", fontSize = 10.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    modifier = Modifier.height(48.dp).weight(1f),
                                    onClick = {
                                        scope.launch { evolveWithAnimation { monsterManager.reverseEvolve() } }
                                    }
                                ) {
                                    Text("Reverse", fontSize = 10.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                onClick = {
                                    monsterManager.toggleEvolutionPause()
                                    monsterState.value = monsterManager.getCurrentMonster()
                                }
                            ) {
                                Text(if (state.isEvolutionPaused) "Resume Aging" else "Pause Aging", fontSize = 10.sp)
                            }

                            Spacer(Modifier.height(16.dp))
                            Text("Adjust Stat Bonuses", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            var editAtk by remember { mutableStateOf(state.attackBonus.toString()) }
                            var editHp by remember { mutableStateOf(state.healthBonus.toString()) }
                            var editSpd by remember { mutableStateOf(state.speedBonus.toString()) }
                            var editDef by remember { mutableStateOf(state.defenseBonus.toString()) }

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = editAtk,
                                    onValueChange = { editAtk = it },
                                    label = { Text("ATK", fontSize = 8.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.Cyan)
                                )
                                OutlinedTextField(
                                    value = editHp,
                                    onValueChange = { editHp = it },
                                    label = { Text("HP", fontSize = 8.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.Cyan)
                                )
                                OutlinedTextField(
                                    value = editSpd,
                                    onValueChange = { editSpd = it },
                                    label = { Text("SPD", fontSize = 8.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.Cyan)
                                )
                                OutlinedTextField(
                                    value = editDef,
                                    onValueChange = { editDef = it },
                                    label = { Text("DEF", fontSize = 8.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.Cyan)
                                )
                            }

                            Button(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                onClick = {
                                    monsterManager.setBonusStats(
                                        editAtk.toIntOrNull() ?: state.attackBonus,
                                        editHp.toIntOrNull() ?: state.healthBonus,
                                        editSpd.toIntOrNull() ?: state.speedBonus,
                                        editDef.toIntOrNull() ?: state.defenseBonus
                                    )
                                    monsterState.value = monsterManager.getCurrentMonster()
                                    Toast.makeText(context, "Stats Updated!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Apply Stats", fontSize = 10.sp)
                            }

                            Spacer(Modifier.height(16.dp))
                            Text("Secret Crit Stats (Education)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            // 2026-09-27: remember these so the card file isn't re-read on
                            // every recomposition (the DIM parse was stuttering the UI).
                            val effCritPct = remember(state.cardName, state.characterId, state.secretCritChance) {
                                (monsterManager.getEffectiveCritChance(state.cardName, state.characterId) * 100).toInt()
                            }
                            val effCritMult = remember(state.secretCritDamage) {
                                monsterManager.getEffectiveCritDamageMult()
                            }
                            Text(
                                "Crit Chance +${state.secretCritChance}% (lands ${effCritPct}%, cap 50%) · " +
                                "Crit Damage +${state.secretCritDamage}% (x${"%.2f".format(effCritMult)}, cap x1.75)",
                                color = Color.Gray, fontSize = 10.sp
                            )

                            var editCrit by remember { mutableStateOf(state.secretCritChance.toString()) }
                            var editCritDmg by remember { mutableStateOf(state.secretCritDamage.toString()) }

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = editCrit,
                                    onValueChange = { editCrit = it },
                                    label = { Text("CRIT+% ", fontSize = 8.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.Cyan)
                                )
                                OutlinedTextField(
                                    value = editCritDmg,
                                    onValueChange = { editCritDmg = it },
                                    label = { Text("CRITDMG+%", fontSize = 8.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.Cyan)
                                )
                            }

                            Button(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                onClick = {
                                    monsterManager.setSecretCritChanceBonus(editCrit.toIntOrNull() ?: state.secretCritChance)
                                    monsterManager.setSecretCritDamageBonus(editCritDmg.toIntOrNull() ?: state.secretCritDamage)
                                    monsterState.value = monsterManager.getCurrentMonster()
                                    Toast.makeText(context, "Secret Crit Stats Updated!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Apply Crit Stats", fontSize = 10.sp)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            modifier = Modifier.height(56.dp).weight(1f),
                            onClick = {
                                scope.launch { sendP2PChallenge(context, state) }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(150, 0, 150))
                        ) {
                            Text("Challenge Watch", color = Color.White, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            modifier = Modifier.height(56.dp).weight(1f),
                            onClick = {
                                val intent = Intent(context, com.example.vitalwearclonev1.lab.MapAdventureActivity::class.java).apply {
                                    putExtra("monsterIndex", -1)
                                    putExtra("cardName", state.cardName)
                                    putExtra("charId", state.characterId)
                                    putExtra("atk", state.baseAp + state.attackBonus)
                                    putExtra("cals", state.baseHp + state.healthBonus)
                                    putExtra("spd", state.speedBonus)
                                    putExtra("def", state.defenseBonus)
                                    putExtra("nickname", state.nickname)
                                    putExtra("wins", state.currentWins)
                                    putExtra("winsReq", state.winsRequired)
                                    putExtra("timeAlive", state.timeAlive)
                                    putExtra("evoTime", state.evolutionTime)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 100, 0))
                        ) {
                            Text("Adventure", color = Color.White, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = {
                            val monster = monsterState.value!!
                            com.example.vitalwearclonev1.lab.LabStorage.addMonster(
                                context,
                                com.example.vitalwearclonev1.lab.StoredMonster(
                                    monster.cardName,
                                    monster.characterId,
                                    monster.stage,
                                    monster.attackBonus,
                                    monster.healthBonus,
                                    monster.speedBonus,
                                    monster.defenseBonus,
                                    xp = monster.xp,
                                    level = monster.level,
                                    rawPayload = monster.rawPayload,
                                    nickname = monster.nickname,
                                    currentWins = monster.currentWins,
                                    winsRequired = monster.winsRequired,
                                    timeAlive = monster.timeAlive,
                                    evolutionTime = monster.evolutionTime
                                )
                            )
                            val prefs = context.getSharedPreferences("phone_monster_prefs", Context.MODE_PRIVATE)
                            prefs.edit().remove("current_card").apply()
                            monsterState.value = null
                            Toast.makeText(context, "Digimon sent to Lab!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 100, 150))
                    ) {
                        Text("Send to Lab", color = Color.White, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = {
                            context.startActivity(Intent(context, com.example.vitalwearclonev1.workout.WorkoutActivity::class.java))
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(100, 50, 200))
                    ) {
                        Text("Workouts", color = Color.White, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(100.dp))
            }
        }

        // Evolution sequence overlay: black + white-light digivolution.
        evolutionRequest.value?.let { req ->
            EvolutionAnimation(
                oldFrames = req.oldFrames,
                newFrames = req.newFrames,
                portraitFrame = req.portraitFrame,
                onFinished = {
                    evolutionRequest.value = null
                    if (req.newFrames.isNotEmpty()) idleSprites.value = req.newFrames
                    monsterState.value = monsterManager.getCurrentMonster()
                }
            )
        }
    }
}

@Composable
fun P2PBattleOverlay(opponent: BattleOpponent, monsterManager: PhoneMonsterManager, onDismiss: () -> Unit) {
    val myState = remember { monsterManager.getCurrentMonster() }

    if (myState != null) {
        timber.log.Timber.d("Displaying P2P Battle Overlay on phone against ${opponent.cardName}")
        com.example.vitalwearclonev1.ui.BattleScene(
            myCardName = myState.cardName,
            myCharId = myState.characterId,
            opponent = opponent,
            seed = opponent.seed,
            onResult = { isWin ->
                timber.log.Timber.d("P2P Battle finished on phone. Result: $isWin")
                monsterManager.recordBattleResult(isWin)
                onDismiss()
            }
        )
    } else {
        timber.log.Timber.w("P2PBattleOverlay called but myState is null")
        LaunchedEffect(Unit) { onDismiss() }
    }
}

suspend fun sendP2PChallenge(context: Context, state: PhoneMonsterManager.MonsterState) {
    withContext(Dispatchers.IO) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val messageClient = Wearable.getMessageClient(context)

            val payload = ByteArrayOutputStream().use { bos ->
                val dos = DataOutputStream(bos)
                val cardManager = com.example.vitalwearclonev1.card.CardManager(context)
                val myCard = cardManager.getCard(state.cardName)
                val dimIdMsg = myCard?.header?.dimId ?: 0
                dos.write(state.cardName.toByteArray(java.nio.charset.Charset.defaultCharset()))
                dos.writeByte(0)
                dos.writeInt(state.characterId)
                dos.writeInt(state.attackBonus)
                dos.writeInt(state.healthBonus)
                dos.writeInt(state.speedBonus)
                dos.writeInt(state.defenseBonus)
                dos.writeLong(System.currentTimeMillis())
                dos.writeByte(if (state.isBem) 1 else 0)
                dos.writeInt(dimIdMsg)
                dos.flush()
                bos.toByteArray()
            }

            for (node in nodes) {
                messageClient.sendMessage(node.id, "/P2P_BATTLE_REQ", payload).await()
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, "Challenge Sent to Watch!", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Challenge failed!", Toast.LENGTH_SHORT).show() }
        }
    }
}

@Composable
fun MultiplayerScreen(
    monsterManager: PhoneMonsterManager,
    btManager: com.example.vitalwearclonev1.communication.BluetoothBattleManager,
    onBattleStart: (BattleOpponent) -> Unit
) {
    val context = LocalContext.current
    val myState = remember { monsterManager.getCurrentMonster() }
    var status by remember { mutableStateOf("Ready to Battle") }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        btManager.onBattleStarted = { opponent ->
            scope.launch {
                status = "Battle Found! Starting..."
                delay(1000)
                onBattleStart(opponent)
            }
        }
        onDispose { btManager.stop() }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(20, 0, 40)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bluetooth Multiplayer", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        Text(status, color = Color.Cyan, fontSize = 16.sp)
        Spacer(Modifier.height(48.dp))

        if (myState != null) {
            // Grid Battle mode (2026-09-25): separate chip-based grid battles.
            // Does not touch the watch-linked battle structure.
            Button(
                onClick = {
                    context.startActivity(Intent(context, com.example.vitalwearclonev1.gridbattle.GridBattleActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 100))
            ) {
                Text("Grid Battle (Chips)", color = Color.White)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val btManagerSys = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
                    val adapter = btManagerSys.adapter
                    if (adapter?.isEnabled == true) {
                        status = "Hosting... (Waiting for opponent)"
                        btManager.startAdvertising(myState)
                    } else {
                        status = "Please enable Bluetooth!"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 100, 200))
            ) {
                Text("Host Battle", color = Color.White)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val btManagerSys = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
                    val adapter = btManagerSys.adapter
                    if (adapter?.isEnabled == true) {
                        status = "Scanning for rivals..."
                        btManager.startScanning()
                    } else {
                        status = "Please enable Bluetooth!"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(150, 0, 150))
            ) {
                Text("Join Battle", color = Color.White)
            }


        } else {
            Text("Hatch a Digimon to battle!", color = Color.Red)
        }
    }
}

@Composable
fun SettingsScreen(
    monsterManager: PhoneMonsterManager,
    healthPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>
) {
    val context = LocalContext.current
    val healthSyncManager = remember { com.example.vitalwearclonev1.communication.PhoneHealthSyncManager(context) }
    val scope = rememberCoroutineScope()
    val gpsManager = remember { com.example.vitalwearclonev1.sensor.PhoneGpsManager(context) }

    var devMode by remember { mutableStateOf(monsterManager.isDevModeEnabled()) }
    var tapCount by remember { mutableIntStateOf(0) }
    var showDevToggle by remember { mutableStateOf(monsterManager.isDevModeEnabled()) }
    var gpsTracking by remember { mutableStateOf(context.getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE).getBoolean("gps_enabled", false)) }

    LaunchedEffect(Unit) {
        if (gpsTracking && !com.example.vitalwearclonev1.sensor.PhoneTrackingService.isServiceRunning) {
            com.example.vitalwearclonev1.sensor.PhoneTrackingService.start(context)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            com.example.vitalwearclonev1.sensor.PhoneTrackingService.start(context)
            gpsTracking = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0, 30, 60)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))

        // Health Status Info
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            backgroundColor = Color(0, 0, 0, 100)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val status = healthSyncManager.getSdkStatus()
                val statusText = when (status) {
                    androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE -> "Service: Ready"
                    androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE -> "Service: Not Installed"
                    androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Service: Update Required"
                    else -> "Service: Unknown Status"
                }
                Text(statusText, color = if (status == 1) Color.Green else Color.Yellow, fontSize = 14.sp)

                val hasPerms = remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { hasPerms.value = healthSyncManager.hasAllPermissions() }

                Text(
                    text = if (hasPerms.value) "Permissions: Granted" else "Permissions: Missing",
                    color = if (hasPerms.value) Color.Green else Color.Red,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    val status = healthSyncManager.getSdkStatus()
                    if (status == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
                        if (healthSyncManager.hasAllPermissions()) {
                            healthSyncManager.syncNow()
                            Toast.makeText(context, "Health Sync Triggered!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Try to trigger the specific Health Connect permission UI
                            try {
                                healthPermissionLauncher.launch(healthSyncManager.permissions)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Please grant health permissions in your phone settings.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        val msg = when (status) {
                            androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Health Connect app needs an update!"
                            else -> "Health Connect is not supported on this phone."
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 100))
        ) {
            Text("Sync Health to Watch", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                context.startActivity(Intent(context, com.example.vitalwearclonev1.card.ManageCardsActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 80, 150))
        ) {
            Text("Manage Saved DIMs", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        // DEBUG: manual reset for testing the daily step/calorie reset fix
        Button(
            onClick = {
                gpsManager.resetToday()
                Toast.makeText(context, "Steps & calories cleared (debug)", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(150, 60, 0))
        ) {
            Text("DEBUG: Clear Steps & Calories", color = Color.White)
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(255, 255, 255, 10),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("GPS High Accuracy Tracking", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(
                        checked = gpsTracking,
                        onCheckedChange = { enabled ->
                            gpsTracking = enabled
                            context.getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE).edit().putBoolean("gps_enabled", enabled).apply()
                            if (enabled) {
                                locationPermissionLauncher.launch(arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            } else {
                                com.example.vitalwearclonev1.sensor.PhoneTrackingService.stop(context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan)
                    )
                }
                Text("Uses GPS to accurately track steps and speed-based calories. Recommended for devices with inconsistent hardware step sensors.", color = Color.Gray, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        if (showDevToggle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Developer Mode", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = devMode,
                    onCheckedChange = {
                        devMode = it
                        monsterManager.setDevModeEnabled(it)
                    }
                )
            }
            Text("Enables Force Evolve and Pause controls on the Home screen.", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    tapCount++
                    if (tapCount >= 10) {
                        showDevToggle = true
                        Toast.makeText(context, "Developer Mode Unlocked!", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(vertical = 8.dp)
        ) {
            Column {
                Text("App Version", color = Color.White, fontSize = 14.sp)
                Text("v1.0.0-pro (Build 20260825)", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun GameMenuScreen(onModeSelected: (String, Boolean) -> Unit) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(10, 10, 25)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Education Mode", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedSubTab,
            backgroundColor = Color(20, 20, 50),
            contentColor = Color.Cyan
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }) {
                Text("PRACTICE", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) {
                Text("LESSONS", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        val isLesson = selectedSubTab == 1
        val modes = listOf(
            Triple("MATH", "Math", Color(60, 120, 240)),
            Triple("READING", "Reading", Color(240, 120, 60)),
            Triple("HISTORY", "History", Color(120, 60, 240)),
            Triple("SCIENCE", "Science", Color(60, 240, 120)),
            Triple("GEOMETRY", "Geometry", Color(240, 60, 120)),
            Triple("LIFESCIENCE", "Life Science", Color(120, 240, 60))
        )

        val scrollState = rememberScrollState()
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
            modes.forEach { (id, label, color) ->
                val suffix = if (isLesson) " LESSONS" else " PRACTICE"
                GameModeButton(label + suffix, color) { onModeSelected(id, isLesson) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GameModeButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color)
    ) {
        Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
