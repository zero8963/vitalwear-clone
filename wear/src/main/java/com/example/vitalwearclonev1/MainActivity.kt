package com.example.vitalwearclonev1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.example.vitalwearclonev1.common.communication.ChannelTypes
import com.example.vitalwearclonev1.ui.EvolutionAnimation
import com.example.vitalwearclonev1.ui.EvolutionRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import kotlin.random.Random
import com.example.vitalwearclonev1.monster.BattleOpponent
import com.example.vitalwearclonev1.monster.MonsterManager
import com.example.vitalwearclonev1.sensor.VitalSensorManager
import com.github.cfogrady.vb.dim.card.BemCard
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.ServiceConnection
import android.os.IBinder
import androidx.wear.ambient.AmbientModeSupport
import com.example.vitalwearclonev1.sensor.VitalForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var vitalService: VitalForegroundService? = null
    private val _serviceBound = mutableStateOf(false)
    private val _isAmbient = mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
            val binder = service as VitalForegroundService.VitalBinder
            vitalService = binder.getService()
            _serviceBound.value = true
            Timber.d("Vital Service Bound")
        }

        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            vitalService = null
            _serviceBound.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AmbientModeSupport.attach(this)
        
        // Request Permissions first
        val permissions = mutableListOf(
            android.Manifest.permission.ACTIVITY_RECOGNITION,
            android.Manifest.permission.BODY_SENSORS
        )
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add("android.permission.POST_NOTIFICATIONS")
        }
        
        val allGranted = permissions.all { checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
        
        if (allGranted) {
            startVitalService()
        } else {
            requestPermissions(permissions.toTypedArray(), 101)
        }

        // Check for starting battle trigger from background
        if (intent.action?.startsWith("com.example.vitalwearclonev1.TRIGGER") == true) {
            // Give the UI a moment to register its receiver
            Handler(Looper.getMainLooper()).postDelayed({
                sendBroadcast(intent)
            }, 1000)
        }

        setContent {
            VitalWearApp(vitalService, _serviceBound.value, _isAmbient.value)
        }
    }

    private fun startVitalService() {
        val intent = Intent(this, VitalForegroundService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startVitalService()
            }
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = object : AmbientModeSupport.AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            super.onEnterAmbient(ambientDetails)
            _isAmbient.value = true
        }
        override fun onExitAmbient() {
            super.onExitAmbient()
            _isAmbient.value = false
        }
        override fun onUpdateAmbient() {
            super.onUpdateAmbient()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Re-broadcast for the UI to pick up
        if (intent.action?.startsWith("com.example.vitalwearclonev1.TRIGGER") == true) {
            sendBroadcast(intent)
        }
    }
}

@Composable
fun VitalWearApp(service: VitalForegroundService?, isBound: Boolean, isAmbient: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val monsterManager = remember { MonsterManager(context) }
    val cardManager = remember { CardManager(context) }
    
    // Use service's sensor manager if bound
    val sensorManager = remember(isBound) { 
        service?.sensorManager ?: VitalSensorManager.getInstance(context) 
    }
    
    val scope = rememberCoroutineScope()
    
    val monsterState = remember { mutableStateOf(monsterManager.getCurrentMonster()) }
    val backgroundSprite = remember { mutableStateOf<Bitmap?>(null) }
    val idleSprites = remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val currentFrame = remember { mutableIntStateOf(0) }
    val evolutionRequest = remember { mutableStateOf<EvolutionRequest?>(null) }
    // Cache of idle frames per character id, so the evolution sequence can
    // still show the old form after the sprite loader has moved on.
    val framesByCharId = remember { mutableStateMapOf<Int, List<Bitmap>>() }
    // Persists the last-seen character per card so evolutions that happened
    // while the app was closed still play their sequence on next open.
    val evoPrefs = remember { context.getSharedPreferences("evo_anim_prefs", Context.MODE_PRIVATE) }
    
    val currentTime = remember { mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())) }
    val isPhoneConnected = remember { mutableStateOf<Boolean?>(null) }
    val lastRefreshTime = remember { mutableStateOf(0L) }
    val currentScreen = remember { mutableStateOf("GAME") } // "GAME", "MENU", "BATTLE", "TRAINING", "LAB", "SYNC", "WORKOUTS", "STORAGE"
    val selectedExercise = remember { mutableStateOf("") }

    val liveSteps by sensorManager.stepCount.collectAsState()
    val liveCalories by sensorManager.calories.collectAsState()
    val lastSteps = remember { mutableIntStateOf(liveSteps) }
    
    val totalSteps = liveSteps
    val totalCalories = liveCalories
    
    val activeOpponent = remember { mutableStateOf<BattleOpponent?>(null) }

    val isCardMissing = remember { mutableStateOf(false) }

    // Sprite helpers for the evolution sequence (kept as locals so both the
    // connection loop and the dev-jump hook can use them).
    // Loads the idle frames for a character id off the main thread.
    suspend fun loadWearFrames(cardName: String, characterId: Int): List<Bitmap> =
        withContext(Dispatchers.IO) {
            try {
                val card = cardManager.getCard(cardName) ?: return@withContext emptyList()
                val isBem = card is BemCard
                val sprites = card.spriteData.sprites
                val indices = monsterManager.getIdleSpriteIndices(characterId, sprites.size, isBem)
                indices.mapNotNull { SpriteBitmapHandler.getBitmap(sprites[it]) }
            } catch (e: Exception) {
                emptyList()
            }
        }

    // Portrait frame for the evolution reveal: the new form's battle pose,
    // shown large like the original hardware's splash before the play sprites.
    suspend fun loadWearPortrait(cardName: String, characterId: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val card = cardManager.getCard(cardName) ?: return@withContext null
                val isBem = card is BemCard
                val sprites = card.spriteData.sprites
                val idx = monsterManager.getPortraitSpriteIndex(characterId, isBem, sprites)
                SpriteBitmapHandler.getBitmap(sprites[idx])
            } catch (e: Exception) {
                null
            }
        }

    // Plays the evolution sequence for an explicit form change (dev chips).
    // Prefers the cached frames for the old form, loading from the card if needed.
    suspend fun playEvolutionSequence(oldCharId: Int, newCharId: Int, cardName: String) {
        val oldFrames = framesByCharId[oldCharId]?.takeIf { it.isNotEmpty() }
            ?: loadWearFrames(cardName, oldCharId)
        val newFrames = loadWearFrames(cardName, newCharId)
        if (newFrames.isNotEmpty()) {
            val portrait = loadWearPortrait(cardName, newCharId)
            evolutionRequest.value = EvolutionRequest(oldFrames, newFrames, portrait)
        }
    }

    // Connection Polling Loop
    LaunchedEffect(Unit) {
        while(true) {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                isPhoneConnected.value = nodes.isNotEmpty()
            } catch (e: Exception) {
                isPhoneConnected.value = false
            }
            delay(10000)
        }
    }

    LaunchedEffect(Unit) {
        // tracking should start via Service, but we can call it here as well if needed
        sensorManager.startTracking()
    }

    // Refresh UI state periodically to reflect background updates from Service.
    // Also detects evolutions (the service evolves in the background) and plays
    // the classic black + white-light digivolution sequence for them.
    LaunchedEffect(Unit) {
        while (true) {
            val fresh = monsterManager.getCurrentMonster()
            if (fresh != null) {
                val lastSeenKey = "last_char_" + fresh.cardName
                val lastSeen = evoPrefs.getInt(lastSeenKey, -1)
                when {
                    lastSeen == -1 || lastSeen == fresh.characterId -> {
                        // First sighting, or nothing changed.
                        if (lastSeen == -1) framesByCharId.clear()
                        evoPrefs.edit().putInt(lastSeenKey, fresh.characterId).apply()
                        monsterState.value = fresh
                    }
                    else -> {
                        // The Digimon changed form in the background: digivolution!
                        // Prefer the cached frames for the old form; if the app was
                        // closed when it happened, load them fresh from the card.
                        val cachedOld = framesByCharId[lastSeen]?.takeIf { it.isNotEmpty() }
                        evoPrefs.edit().putInt(lastSeenKey, fresh.characterId).apply()
                        monsterState.value = fresh
                        if (!isAmbient && currentScreen.value == "GAME" && evolutionRequest.value == null) {
                            scope.launch {
                                val oldFrames = cachedOld ?: loadWearFrames(fresh.cardName, lastSeen)
                                val newFrames = loadWearFrames(fresh.cardName, fresh.characterId)
                                if (newFrames.isNotEmpty()) {
                                    val portrait = loadWearPortrait(fresh.cardName, fresh.characterId)
                                    evolutionRequest.value = EvolutionRequest(oldFrames, newFrames, portrait)
                                }
                            }
                        }
                    }
                }
            }
            delay(5000)
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

    // High Precision Clock
    LaunchedEffect(monsterState.value?.is24HourFormat, isAmbient) {
        val is24h = monsterState.value?.is24HourFormat ?: true
        val pattern = if (is24h) (if (isAmbient) "hh:mm a" else "HH:mm:ss") else "hh:mm a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        while(true) {
            currentTime.value = sdf.format(Date())
            if (isAmbient) {
                // Update once per minute in ambient mode to save battery
                val now = System.currentTimeMillis()
                val secondsRemaining = 60 - ((now / 1000) % 60)
                delay(secondsRemaining * 1000)
            } else {
                delay(1000)
            }
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.example.vitalwearclonev1.CARD_IMPORTED" -> {
                        monsterState.value = monsterManager.getCurrentMonster()
                        lastRefreshTime.value = intent.getLongExtra("timestamp", System.currentTimeMillis())
                    }
                    "com.example.vitalwearclonev1.TRIGGER_BATTLE" -> {
                        if (monsterState.value != null && currentScreen.value == "GAME") {
                            activeOpponent.value = null 
                            currentScreen.value = "BATTLE"
                        }
                    }
                    "com.example.vitalwearclonev1.TRIGGER_P2P_BATTLE" -> {
                        val opponent = BattleOpponent(
                            cardName = intent.getStringExtra("cardName") ?: "",
                            characterId = intent.getIntExtra("charId", 0),
                            name = "Rival",
                            atk = intent.getIntExtra("atk", 0),
                            hp = intent.getIntExtra("hp", 0),
                            spd = intent.getIntExtra("spd", 0),
                            def = intent.getIntExtra("def", 0),
                            isBoss = false,
                            isInitiator = intent.getBooleanExtra("isInitiator", false),
                            seed = intent.getLongExtra("seed", 0L),
                            dimId = intent.getIntExtra("dimId", 0)
                        )
                        activeOpponent.value = opponent
                        currentScreen.value = "BATTLE"
                    }
                    "com.example.vitalwearclonev1.HEALTH_SYNCED" -> {
                        val steps = intent.getLongExtra("steps", 0L)
                        val calories = intent.getIntExtra("calories", 0)
                        val startOfDay = intent.getLongExtra("startOfDay", 0L)
                        val weightKg = intent.getFloatExtra("weight", 75f)
                        
                        sensorManager.updateBaseHealthData(steps, calories, startOfDay, weightKg)
                        monsterState.value = monsterManager.getCurrentMonster()
                        // Toast removed as per user request to reduce noise
                    }
                    "com.example.vitalwearclonev1.WORKOUT_COMPLETE" -> {
                        monsterState.value = monsterManager.getCurrentMonster()
                        Toast.makeText(context, "Workout Power-up!", Toast.LENGTH_SHORT).show()
                        if (currentScreen.value == "TRAINING") {
                            currentScreen.value = "GAME"
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("com.example.vitalwearclonev1.CARD_IMPORTED")
            addAction("com.example.vitalwearclonev1.TRIGGER_BATTLE")
            addAction("com.example.vitalwearclonev1.TRIGGER_P2P_BATTLE")
            addAction("com.example.vitalwearclonev1.HEALTH_SYNCED")
            addAction("com.example.vitalwearclonev1.WORKOUT_COMPLETE")
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Load Sprites (Only reload if card/character changes)
    LaunchedEffect(monsterState.value?.cardName, monsterState.value?.characterId, lastRefreshTime.value) {
        val state = monsterState.value
        if (state != null) {
            val frames = withContext(Dispatchers.IO) {
                try {
                    val card = cardManager.getCard(state.cardName)
                    if (card != null) {
                        isCardMissing.value = false
                        val isBem = card is BemCard
                        val sprites = card.spriteData.sprites
                        if (sprites.size > 1) {
                            backgroundSprite.value = SpriteBitmapHandler.getBitmap(sprites[1])
                        }
                        val indices = monsterManager.getIdleSpriteIndices(state.characterId, sprites.size, isBem)
                        indices.mapNotNull { SpriteBitmapHandler.getBitmap(sprites[it]) }
                    } else {
                        isCardMissing.value = true
                        emptyList()
                    }
                } catch (e: Exception) {
                    isCardMissing.value = true
                    emptyList()
                }
            }
            idleSprites.value = frames
            framesByCharId[state.characterId] = frames
        }
    }


    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            when (currentScreen.value) {
                "GAME" -> {
                    val pagerState = rememberPagerState(pageCount = { 2 })
                    Scaffold(
                        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
                    ) {
                        HorizontalPager(state = pagerState) { page ->
                            when (page) {
                                0 -> MonsterScreen(
                                    backgroundSprite.value,
                                    if (idleSprites.value.isNotEmpty()) idleSprites.value[currentFrame.intValue % idleSprites.value.size] else null,
                                    currentTime.value,
                                    totalSteps,
                                    totalCalories,
                                    monsterState.value?.cardName ?: "",
                                    isCardMissing.value,
                                    monsterState.value == null,
                                    isAmbient
                                )
                                1 -> MenuScreen(
                                isPhoneConnected.value,
                                monsterState.value,
                                monsterManager,
                                onNavigate = { screen, exercise ->
                                    currentScreen.value = screen
                                    selectedExercise.value = exercise
                                    if (screen == "GAME") {
                                        monsterState.value = monsterManager.getCurrentMonster()
                                    }
                                },
                                onDevJump = { forward ->
                                    val before = monsterManager.getCurrentMonster()
                                    if (before != null) {
                                        // devJump moves exactly one step (clamped at 0): pre-seed
                                        // the detector so it doesn't replay this jump later.
                                        val newId = (if (forward) before.characterId + 1 else before.characterId - 1).coerceAtLeast(0)
                                        evoPrefs.edit().putInt("last_char_" + before.cardName, newId).apply()
                                        monsterManager.devJump(forward)
                                        val after = monsterManager.getCurrentMonster()
                                        monsterState.value = after
                                        if (after != null && after.characterId != before.characterId) {
                                            scope.launch {
                                                playEvolutionSequence(before.characterId, after.characterId, before.cardName)
                                            }
                                        }
                                        currentScreen.value = "GAME"
                                    }
                                }
                            )
                            }
                        }
                    }
                }
                "BATTLE" -> {
                    val seed = activeOpponent.value?.seed ?: System.currentTimeMillis()
                    BattleScreen(monsterState.value, activeOpponent.value, seed, cardManager, monsterManager) { result ->
                        val won = result == "WIN!"
                        monsterManager.recordBattleResult(won)
                        if (won) {
                            if (activeOpponent.value?.isBoss == true) {
                                monsterManager.completeAdventureLevel()
                            }
                        } else if (activeOpponent.value?.isBoss == true) {
                            monsterManager.resetAdventureSteps()
                        }
                        currentScreen.value = "GAME"
                        monsterState.value = monsterManager.getCurrentMonster()
                    }
                }
                "TRAINING" -> TrainingScreen(selectedExercise.value, service) { 
                    currentScreen.value = "GAME"
                    monsterState.value = monsterManager.getCurrentMonster()
                }
                "LAB" -> LabScreen(cardManager, monsterManager) { 
                    currentScreen.value = "GAME"
                    monsterState.value = monsterManager.getCurrentMonster()
                }
                "SYNC" -> SyncScreen(sensorManager, monsterManager) {
                    currentScreen.value = "GAME"
                    monsterState.value = monsterManager.getCurrentMonster()
                }
                "WORKOUTS" -> WorkoutListScreen(onBack = { currentScreen.value = "GAME" }) { ex ->
                    selectedExercise.value = ex
                    currentScreen.value = "TRAINING"
                }
                "STORAGE" -> StorageScreen(
                    monsterManager,
                    onNavigate = { screen, exercise ->
                        currentScreen.value = screen
                        selectedExercise.value = exercise
                        if (screen == "GAME") {
                            monsterState.value = monsterManager.getCurrentMonster()
                        }
                    }
                ) {
                    currentScreen.value = "GAME"
                    monsterState.value = monsterManager.getCurrentMonster()
                }
            }

            // Evolution sequence overlay: black + white-light digivolution.
            evolutionRequest.value?.let { req ->
                EvolutionAnimation(
                    oldFrames = req.oldFrames,
                    newFrames = req.newFrames,
                    portraitFrame = req.portraitFrame,
                    spriteSize = 160.dp,
                    onFinished = {
                        evolutionRequest.value = null
                        if (req.newFrames.isNotEmpty()) idleSprites.value = req.newFrames
                        monsterState.value = monsterManager.getCurrentMonster()
                    }
                )
            }
        }
    }
}

@Composable
fun SyncScreen(sensorManager: VitalSensorManager, monsterManager: MonsterManager, onExit: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val steps by sensorManager.stepCount.collectAsState()
    val cals by sensorManager.calories.collectAsState()
    
    Box(Modifier.fillMaxSize().background(Color(0, 30, 50)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(10.dp)) {
            Text("HEALTH SYNC", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            
            Text("Steps: $steps", color = Color.White, fontSize = 14.sp)
            Text("Calories: $cals", color = Color.White, fontSize = 14.sp)
            
            Spacer(Modifier.height(16.dp))
            
            Button(onClick = { 
                scope.launch { 
                    requestHealthSync(context)
                }
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text("Refresh Data", fontSize = 12.sp)
            }
            
            Spacer(Modifier.height(8.dp))
            
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text("Back", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MonsterScreen(background: Bitmap?, monster: Bitmap?, time: String, steps: Int, calories: Int, cardName: String, isCardMissing: Boolean, isWaiting: Boolean, isAmbient: Boolean) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isAmbient) {
            background?.let { Image(it.asImageBitmap(), "BG", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
        }
        
        if (isCardMissing) {
            Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("CARD REQUIRED", color = Color.Red, fontWeight = FontWeight.Bold)
                Text(cardName, color = Color.White, fontSize = 12.sp)
                Text("Please import card on phone", color = Color.Gray, fontSize = 10.sp)
            }
        } else {
            if (isAmbient) {
                // Simplistic static monster for ambient mode
                monster?.let { Image(it.asImageBitmap(), "Monster", Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).size(72.dp)) }
            } else {
                monster?.let { Image(it.asImageBitmap(), "Monster", Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).size(72.dp)) }
            }
        }

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val stepsText = String.format(Locale.getDefault(), "%04d", steps)
            val calsText = String.format(Locale.getDefault(), "%04d", calories)
            Text(time, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("© $stepsText", fontSize = 16.sp, color = Color.White)
            Text("Cals $calsText", fontSize = 16.sp, color = Color.White)
        }
        if (isWaiting) Text("Waiting for DIM...", Modifier.align(Alignment.TopCenter).padding(top = 20.dp), Color.Cyan, 12.sp)
    }
}

@Composable
fun MenuScreen(phoneConnected: Boolean?, monsterState: MonsterManager.MonsterState?, monsterManager: MonsterManager, onNavigate: (String, String) -> Unit, onDevJump: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    ScalingLazyColumn(Modifier.fillMaxSize().background(Color.Black), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Text("MENU", Modifier.padding(vertical = 10.dp), Color.Cyan, fontWeight = FontWeight.Bold) }
        
        item {
            Chip(label = { Text("Character Lab") }, onClick = { onNavigate("LAB", "") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0, 100, 150)))
        }

        item {
            Chip(label = { Text("Workouts") }, onClick = { onNavigate("WORKOUTS", "") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0, 120, 60)))
        }

        item {
            Chip(label = { Text("Storage") }, onClick = { onNavigate("STORAGE", "") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), colors = ChipDefaults.primaryChipColors(backgroundColor = Color(80, 60, 130)))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Chip(label = { Text("Dev:Prev", fontSize = 10.sp) }, onClick = {
                    onDevJump(false)
                }, modifier = Modifier.weight(1f), colors = ChipDefaults.primaryChipColors(backgroundColor = Color.DarkGray))
                Chip(label = { Text("Dev:Next", fontSize = 10.sp) }, onClick = {
                    onDevJump(true)
                }, modifier = Modifier.weight(1f), colors = ChipDefaults.primaryChipColors(backgroundColor = Color.DarkGray))
            }
        }

        item {
            Chip(
                label = { Text(if (monsterState?.isEvolutionPaused == true) "Resume Evolution" else "Pause Evolution") }, 
                onClick = { 
                    monsterManager.toggleEvolutionPause()
                    onNavigate("GAME", "") // Refresh
                }, 
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), 
                colors = ChipDefaults.primaryChipColors(backgroundColor = if (monsterState?.isEvolutionPaused == true) Color(100, 0, 0) else Color.DarkGray)
            )
        }

        item {
            Chip(
                label = { Text(if (monsterState?.isAdventureMode == true) "Stop Adventure" else "Start Adventure") },
                secondaryLabel = { if(monsterState?.isAdventureMode == true) Text("Stage ${monsterState.adventureLevel + 1} (${monsterState.adventureSteps}/500)") },
                onClick = {
                    monsterManager.toggleAdventureMode()
                    onNavigate("GAME", "")
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                colors = ChipDefaults.primaryChipColors(backgroundColor = if (monsterState?.isAdventureMode == true) Color(0, 80, 0) else Color.DarkGray)
            )
        }

        item {
            Chip(
                label = { Text("Clock Format") },
                secondaryLabel = { Text(if (monsterState?.is24HourFormat == true) "24-Hour (Military)" else "12-Hour (AM/PM)") },
                onClick = {
                    monsterManager.toggleClockFormat()
                    onNavigate("GAME", "") // Refresh
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color.DarkGray)
            )
        }

        item {
            Chip(
                label = { Text("Send to Phone") },
                onClick = { 
                    monsterState?.let { 
                        scope.launch { sendCharacterToPhone(context, it) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(150, 100, 0))
            )
        }

        item {
            Chip(
                label = { Text("P2P Challenge (Host)") },
                onClick = { scope.launch { hostP2P(context, monsterState) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(80, 0, 80))
            )
        }

        item {
            Chip(
                label = { Text("Challenge Phone") },
                onClick = { scope.launch { challengePhone(context, monsterState) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0, 80, 80))
            )
        }

        item {
            Chip(
                label = { Text("Health Sync Status") },
                onClick = { onNavigate("SYNC", "") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0, 100, 80))
            )
        }

        item {
            Chip(
                label = { Text("Status") }, 
                secondaryLabel = { 
                    Column {
                        Text(if (phoneConnected == true) "Phone Connected" else "Phone Offline", color = if (phoneConnected == true) Color.Green else Color.Red)
                        monsterState?.let {
                            if (it.isEvolutionPaused) {
                                Text("EVOLUTION PAUSED", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Text("ATK:+${it.attackBonus} HP:+${it.healthBonus}", fontSize = 10.sp, color = Color.Yellow)
                            Text("SPD:+${it.speedBonus} DEF:+${it.defenseBonus}", fontSize = 10.sp, color = Color.Yellow)
                            Text("Cals: ${it.calories}", fontSize = 10.sp, color = Color.Cyan)
                        }
                    }
                }, 
                onClick = { }, 
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), 
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color.DarkGray)
            )
        }
    }
}

@Composable
fun WorkoutListScreen(onBack: () -> Unit, onPick: (String) -> Unit) {
    ScalingLazyColumn(Modifier.fillMaxSize().background(Color(0, 60, 30)), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Text("WORKOUTS", Modifier.padding(vertical = 10.dp), Color.White, fontWeight = FontWeight.Bold) }
        listOf("Push-ups", "Pull-ups", "Sprints", "Squats").forEach { ex ->
            item {
                Chip(label = { Text(ex) }, onClick = { onPick(ex) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0, 110, 55)))
            }
        }
        item {
            Button(onClick = onBack, Modifier.padding(top = 10.dp)) { Text("Back") }
        }
    }
}

@Composable
fun StorageScreen(monsterManager: MonsterManager, onNavigate: (String, String) -> Unit, onExit: () -> Unit) {
    val active = remember { mutableStateOf(monsterManager.getCurrentMonster()) }
    val stored = remember { mutableStateOf(monsterManager.getStoredMonster()) }
    fun refresh() {
        active.value = monsterManager.getCurrentMonster()
        stored.value = monsterManager.getStoredMonster()
    }

    ScalingLazyColumn(Modifier.fillMaxSize().background(Color(0, 40, 60)), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Text("STORAGE", Modifier.padding(vertical = 10.dp), Color.White, fontWeight = FontWeight.Bold) }

        item {
            val a = active.value
            Chip(
                label = { Text(if (a != null) "Partner: ${a.cardName}" else "Active: Empty") },
                secondaryLabel = { if (a != null) Text("Char #${a.characterId} - Stage ${a.stage}", fontSize = 10.sp) },
                onClick = { },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0, 90, 140))
            )
        }

        item {
            val s = stored.value
            val asleepFor = if (s != null && monsterManager.getStoredAt() > 0) {
                val mins = (System.currentTimeMillis() - monsterManager.getStoredAt()) / 60000
                " - asleep ${mins}m"
            } else ""
            Chip(
                label = { Text(if (s != null) "Stored: ${s.cardName}$asleepFor" else "Stored: Empty") },
                secondaryLabel = { if (s != null) Text("Char #${s.characterId} - Stage ${s.stage}", fontSize = 10.sp) },
                onClick = { },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(70, 50, 110))
            )
        }

        if (active.value != null && stored.value == null) {
            item {
                Chip(
                    label = { Text("Put Partner to Sleep") },
                    secondaryLabel = { Text("then pick a new partner", fontSize = 10.sp) },
                    onClick = {
                        monsterManager.storeCurrentMonster()
                        refresh()
                        onNavigate("LAB", "") // Pick the next partner from saved cards
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0, 120, 90))
                )
            }
        }

        if (stored.value != null) {
            item {
                Chip(
                    label = { Text(if (active.value != null) "Swap: Wake Stored" else "Wake Stored Partner") },
                    onClick = {
                        monsterManager.wakeStoredMonster()
                        refresh()
                        onExit()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(120, 80, 0))
                )
            }
            item {
                Chip(
                    label = { Text("Release Stored") },
                    onClick = {
                        monsterManager.clearStoredMonster()
                        refresh()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(120, 20, 20))
                )
            }
        }

        item {
            Button(onClick = onExit, Modifier.padding(top = 10.dp)) { Text("Back") }
        }
    }
}

@Composable
fun LabScreen(cardManager: CardManager, monsterManager: MonsterManager, onExit: () -> Unit) {
    val cards = remember { cardManager.listCards() }
    ScalingLazyColumn(Modifier.fillMaxSize().background(Color(0, 50, 80)), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Text("SAVED CARDS", Modifier.padding(vertical = 10.dp), Color.White, fontWeight = FontWeight.Bold) }
        cards.forEach { cardName ->
            item {
                Chip(
                    label = { Text(cardName) },
                    onClick = { 
                        monsterManager.setCurrentMonster(cardName, 0)
                        onExit()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0, 80, 150))
                )
            }
        }
        item {
            Button(onClick = onExit, Modifier.padding(top = 10.dp)) { Text("Back") }
        }
    }
}

enum class BattlePhase { INTRO, ATTACK, RESULT }

@Composable
fun BattleScreen(state: MonsterManager.MonsterState?, opponent: BattleOpponent?, seed: Long = System.currentTimeMillis(), cardManager: CardManager, monsterManager: MonsterManager, onExit: (String) -> Unit) {
    val mySprites = remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }
    val enemySprites = remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }
    val battlePhase = remember { mutableStateOf(BattlePhase.INTRO) }
    val win = remember { mutableStateOf<Boolean?>(null) }
    val enemyName = remember { mutableStateOf(opponent?.name ?: "Wild Digimon") }
    val battleRandom = remember { Random(seed) }

    val myOffset = remember { Animatable(0f) }
    val enemyOffset = remember { Animatable(0f) }
    val currentFrame = remember { mutableIntStateOf(0) }
    val attackFrame = remember { mutableIntStateOf(0) }
    val isMyAttacking = remember { mutableStateOf(false) }
    val isEnemyAttacking = remember { mutableStateOf(false) }

    // Health States
    val myMaxHP = remember { ((state?.baseHp ?: 500) + (state?.healthBonus ?: 0)).toFloat() }
    val enemyMaxHP = remember { ((opponent?.hp ?: 0) + 500).toFloat() }
    var myCurrentHP by remember { mutableFloatStateOf(myMaxHP) }
    var enemyCurrentHP by remember { mutableFloatStateOf(enemyMaxHP) }
    
    var battleLog by remember { mutableStateOf("READY?") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                if (state != null) {
                    val card = cardManager.getCard(state.cardName)
                    card?.let {
                        val isBem = it is BemCard
                        val sprites = it.spriteData.sprites
                        val myIdle1 = SpriteBitmapHandler.getBitmap(sprites[monsterManager.getIdleSpriteIndices(state.characterId, sprites.size, isBem)[0]])
                        val myIdle2 = SpriteBitmapHandler.getBitmap(sprites[monsterManager.getIdleSpriteIndices(state.characterId, sprites.size, isBem)[1]])
                        val myAtk = SpriteBitmapHandler.getBitmap(sprites[monsterManager.getBattleSpriteIndex(state.characterId, sprites.size, isBem)])
                        val myWin = SpriteBitmapHandler.getBitmap(sprites[monsterManager.getWinSpriteIndex(state.characterId, sprites.size, isBem)])
                        val myLose = SpriteBitmapHandler.getBitmap(sprites[monsterManager.getLoseSpriteIndex(state.characterId, sprites.size, isBem)])
                        mySprites.value = mapOf("IDLE1" to myIdle1, "IDLE2" to myIdle2, "ATK" to myAtk, "WIN" to myWin, "LOSE" to myLose)

                        val enCard = if (opponent != null) (cardManager.getCard(opponent.cardName) ?: cardManager.getCardById(opponent.dimId)) else null
                        if (enCard == null && opponent != null) {
                            Timber.w("Rival card not found on watch (ID: ${opponent.dimId}, Name: ${opponent.cardName}). Falling back to my card: ${state.cardName}")
                        }
                        
                        val enemyCard = enCard ?: it
                        val cardIsBem = enemyCard is BemCard
                        val enSprites = enemyCard.spriteData.sprites
                        val finalEnemyId = opponent?.characterId ?: 0
                        
                        // Safe index calculation
                        val idleIndices = monsterManager.getIdleSpriteIndices(finalEnemyId, enSprites.size, cardIsBem)

                        val enIdle1 = SpriteBitmapHandler.getBitmap(enSprites[if (idleIndices[0] < enSprites.size) idleIndices[0] else 0])
                        val enIdle2 = SpriteBitmapHandler.getBitmap(enSprites[if (idleIndices.size > 1 && idleIndices[1] < enSprites.size) idleIndices[1] else 0])
                        val enAtk = SpriteBitmapHandler.getBitmap(enSprites[monsterManager.getBattleSpriteIndex(finalEnemyId, enSprites.size, cardIsBem).let { if (it < enSprites.size) it else 0 }])
                        val enWin = SpriteBitmapHandler.getBitmap(enSprites[monsterManager.getWinSpriteIndex(finalEnemyId, enSprites.size, cardIsBem).let { if (it < enSprites.size) it else 0 }])
                        val enLose = SpriteBitmapHandler.getBitmap(enSprites[monsterManager.getLoseSpriteIndex(finalEnemyId, enSprites.size, cardIsBem).let { if (it < enSprites.size) it else 0 }])
                        
                        enemySprites.value = mapOf("IDLE1" to enIdle1, "IDLE2" to enIdle2, "ATK" to enAtk, "WIN" to enWin, "LOSE" to enLose)
                    }
                }
            } catch (e: Exception) { }
        }
        
        delay(1500)

        // Multi-round Battle Loop
        var round = 1
        while (myCurrentHP > 0 && enemyCurrentHP > 0 && round <= 10) {
            battlePhase.value = BattlePhase.ATTACK
            
            val r1 = battleRandom.nextFloat()
            val r2 = battleRandom.nextInt(20, 50)
            val r3 = battleRandom.nextFloat()
            val r4 = battleRandom.nextInt(20, 50)

            val myRoll = if (opponent?.isInitiator == true) r1 else r3
            val myDmgRoll = if (opponent?.isInitiator == true) r2 else r4
            val enRoll = if (opponent?.isInitiator == true) r3 else r1
            val enDmgRoll = if (opponent?.isInitiator == true) r4 else r2

            // Turn 1: Player Attacks
            val myDodgeChance = ((opponent?.spd ?: 0) / 5000f).coerceIn(0.05f, 0.4f)
            if (myRoll > myDodgeChance) {
                val damage = ((state?.attackBonus ?: 0) / 4 + myDmgRoll).toFloat()
                enemyCurrentHP = (enemyCurrentHP - damage).coerceAtLeast(0f)
                battleLog = "HIT! -$damage"
            } else {
                battleLog = "DODGED!"
            }
            
            launch {
                isMyAttacking.value = true
                myOffset.animateTo(40f, tween(200, easing = LinearEasing))
                myOffset.animateTo(0f, tween(100))
                isMyAttacking.value = false
            }
            delay(300)
            
            if (enemyCurrentHP <= 0) break
            delay(800)

            // Turn 2: Enemy Attacks
            val enDodgeChance = ((state?.speedBonus ?: 0) / 5000f).coerceIn(0.05f, 0.4f)
            if (enRoll > enDodgeChance) {
                val damage = ((opponent?.atk ?: 0) / 4 + enDmgRoll).toFloat()
                myCurrentHP = (myCurrentHP - damage).coerceAtLeast(0f)
                battleLog = "ENEMY HIT!"
            } else {
                battleLog = "YOU DODGED!"
            }

            launch {
                isEnemyAttacking.value = true
                enemyOffset.animateTo(-40f, tween(200, easing = LinearEasing))
                enemyOffset.animateTo(0f, tween(100))
                isEnemyAttacking.value = false
            }
            delay(300)
            
            round++
            delay(1000)
        }

        val isWin = myCurrentHP > 0
        win.value = isWin
        battleLog = if (isWin) "VICTORY!" else "DEFEAT..."
        battlePhase.value = BattlePhase.RESULT
        
        delay(2500)
        onExit(if (isWin) "WIN!" else "LOSE...")
    }

    LaunchedEffect(Unit) {
        while(true) {
            currentFrame.intValue = (currentFrame.intValue + 1) % 2
            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            attackFrame.intValue = (attackFrame.intValue + 1) % 2
            delay(100)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF220000)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(battleLog, fontSize = 18.sp, color = Color.Yellow, fontWeight = FontWeight.ExtraBold)
            
            Spacer(Modifier.height(10.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                // Player Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WatchHealthBar(myCurrentHP, myMaxHP)
                    val myBitmap = when {
                        isMyAttacking.value -> if (attackFrame.intValue == 0) mySprites.value["ATK"] else mySprites.value["IDLE1"]
                        battlePhase.value == BattlePhase.RESULT -> if (win.value == true) mySprites.value["WIN"] else mySprites.value["LOSE"]
                        battlePhase.value == BattlePhase.ATTACK -> mySprites.value["IDLE1"]
                        else -> if (currentFrame.intValue == 0) mySprites.value["IDLE1"] else mySprites.value["IDLE2"]
                    }
                    myBitmap?.let { Image(it.asImageBitmap(), "Me", Modifier.size(60.dp).offset(x = myOffset.value.dp)) }
                }
                
                Text(" VS ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                // Enemy Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WatchHealthBar(enemyCurrentHP, enemyMaxHP)
                    val enemyBitmap = when {
                        isEnemyAttacking.value -> if (attackFrame.intValue == 0) enemySprites.value["ATK"] else enemySprites.value["IDLE1"]
                        battlePhase.value == BattlePhase.RESULT -> if (win.value == false) enemySprites.value["WIN"] else enemySprites.value["LOSE"]
                        battlePhase.value == BattlePhase.ATTACK -> enemySprites.value["IDLE1"]
                        else -> if (currentFrame.intValue == 0) enemySprites.value["IDLE1"] else enemySprites.value["IDLE2"]
                    }
                    enemyBitmap?.let { Image(it.asImageBitmap(), "Enemy", Modifier.size(60.dp).offset(x = enemyOffset.value.dp)) }
                }
            }
            Text(enemyName.value, color = Color.Cyan, fontSize = 10.sp)
        }
    }
}

@Composable
fun WatchHealthBar(current: Float, max: Float) {
    val progress = (current / max).coerceIn(0f, 1f)
    val color = when {
        progress > 0.5f -> Color.Green
        progress > 0.2f -> Color.Yellow
        else -> Color.Red
    }
    Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.DarkGray)) {
        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(color))
    }
}

@Composable
fun TrainingScreen(exercise: String, service: VitalForegroundService?, onExit: () -> Unit) {
    val countdown by (service?.workoutCountdown?.collectAsState() ?: remember { mutableStateOf(0) })
    val completed = remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        service?.startWorkout(exercise, 30)
    }

    LaunchedEffect(countdown) {
        if (countdown <= 0 && service != null) {
            // Service will send broadcast which is handled in VitalWearApp
        }
    }
    
    Box(Modifier.fillMaxSize().background(Color(0xFF004400)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(exercise.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            if (countdown <= 0) {
                Text("POWER UP!", fontSize = 24.sp, color = Color.Yellow, fontWeight = FontWeight.ExtraBold)
                LaunchedEffect(Unit) {
                    delay(1500)
                    onExit()
                }
            } else {
                Text("${countdown}s", fontSize = 40.sp, color = Color.Green)
                Button(onClick = onExit, Modifier.padding(top = 10.dp)) { Text("Cancel") }
            }
        }
    }
}

suspend fun challengePhone(context: Context, state: MonsterManager.MonsterState?) {
    if (state == null) return
    withContext(Dispatchers.IO) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val messageClient = Wearable.getMessageClient(context)
            
            val payload = java.io.ByteArrayOutputStream().use { bos ->
                val dos = java.io.DataOutputStream(bos)
                val cardManager = com.example.vitalwearclonev1.card.CardManager(context)
                val myCard = cardManager.getCard(state.cardName)
                val dimIdLocal = myCard?.header?.dimId ?: 0
                val isBem = myCard is com.github.cfogrady.vb.dim.card.BemCard
                dos.write(state.cardName.toByteArray(java.nio.charset.Charset.defaultCharset()))
                dos.writeByte(0)
                dos.writeInt(state.characterId)
                dos.writeInt(state.attackBonus)
                dos.writeInt(state.healthBonus)
                dos.writeInt(state.speedBonus)
                dos.writeInt(state.defenseBonus)
                dos.writeLong(System.currentTimeMillis())
                dos.writeByte(if (isBem) 1 else 0)
                dos.writeInt(dimIdLocal)
                dos.flush()
                bos.toByteArray()
            }

            for (node in nodes) {
                messageClient.sendMessage(node.id, "/P2P_BATTLE_REQ", payload).await()
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, "Challenge Sent to Phone!", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Challenge failed!", Toast.LENGTH_SHORT).show() }
        }
    }
}

suspend fun hostP2P(context: Context, state: MonsterManager.MonsterState?) {
    if (state == null) return
    withContext(Dispatchers.IO) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val messageClient = Wearable.getMessageClient(context)
            
            val payload = java.io.ByteArrayOutputStream().use { bos ->
                val dos = java.io.DataOutputStream(bos)
                val cardManager = com.example.vitalwearclonev1.card.CardManager(context)
                val myCard = cardManager.getCard(state.cardName)
                val dimIdLocal = myCard?.header?.dimId ?: 0
                val isBem = myCard is com.github.cfogrady.vb.dim.card.BemCard
                dos.write(state.cardName.toByteArray(java.nio.charset.Charset.defaultCharset()))
                dos.writeByte(0)
                dos.writeInt(state.characterId)
                dos.writeInt(state.attackBonus)
                dos.writeInt(state.healthBonus)
                dos.writeInt(state.speedBonus)
                dos.writeInt(state.defenseBonus)
                dos.writeLong(System.currentTimeMillis())
                dos.writeByte(if (isBem) 1 else 0)
                dos.writeInt(dimIdLocal)
                dos.flush()
                bos.toByteArray()
            }

            for (node in nodes) {
                messageClient.sendMessage(node.id, "/P2P_BATTLE_REQ", payload).await()
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, "Battle Request Sent!", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Host failed!", Toast.LENGTH_SHORT).show() }
        }
    }
}

suspend fun requestHealthSync(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val messageClient = Wearable.getMessageClient(context)
            if (nodes.isEmpty()) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Phone not connected!", Toast.LENGTH_SHORT).show() }
                return@withContext
            }
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/REQUEST_HEALTH_SYNC", null).await()
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, "Sync Requested", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Sync Request Failed", Toast.LENGTH_SHORT).show() }
        }
    }
}

suspend fun sendCharacterToPhone(context: Context, state: MonsterManager.MonsterState) {
    withContext(Dispatchers.IO) {
        try {
            val channelClient = Wearable.getChannelClient(context)
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Phone not connected!", Toast.LENGTH_SHORT).show() }
                return@withContext
            }
            
            for (node in nodes) {
                val channel = channelClient.openChannel(node.id, ChannelTypes.CHARACTER_DATA).await()
                channelClient.getOutputStream(channel).await().use { os ->
                    val output = DataOutputStream(os.buffered())
                    output.write(state.cardName.toByteArray(Charset.defaultCharset()))
                    output.writeByte(0)
                    output.writeInt(state.characterId)
                    output.writeInt(state.stage)
                    output.writeInt(state.attackBonus)
                    output.writeInt(state.calories)
                    output.writeInt(state.speedBonus)
                    output.writeInt(state.defenseBonus)
                    val wins = context.getSharedPreferences("monster_prefs", android.content.Context.MODE_PRIVATE).getInt("current_wins", 0)
                    output.writeInt(wins)
                    output.writeInt(10) // Default winsRequired for watch
                    output.writeLong(state.timeAlive)
                    output.writeLong(state.evolutionTime)
                    output.writeInt(state.attribute)
                    output.writeInt(state.mood)
                    output.writeInt(state.steps)
                    output.writeInt(state.bp)
                    output.writeInt(state.sp)
                    output.writeInt(state.winRatio)
                    output.writeInt(state.trophies)
                    output.flush()
                }
                channelClient.close(channel).await()
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, "Sent to Lab!", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Send failed!", Toast.LENGTH_SHORT).show() }
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

