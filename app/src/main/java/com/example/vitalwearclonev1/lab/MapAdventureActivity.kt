package com.example.vitalwearclonev1.lab

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.github.cfogrady.vb.dim.card.BemCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.random.Random
import com.example.vitalwearclonev1.ui.AttackEffectCanvas
import com.example.vitalwearclonev1.ui.attackEffectColor
import com.example.vitalwearclonev1.gridbattle.BattleStyle
import com.example.vitalwearclonev1.gridbattle.ChipElement
import com.example.vitalwearclonev1.gridbattle.ChipFolder
import com.example.vitalwearclonev1.gridbattle.ChipLibrary
import com.example.vitalwearclonev1.gridbattle.EffectKind
import com.example.vitalwearclonev1.gridbattle.NaviCustLoadout
import com.example.vitalwearclonev1.gridbattle.AttackFxOverrides
import com.example.vitalwearclonev1.gridbattle.ownerIdFor
import com.example.vitalwearclonev1.common.SoundManager

class MapAdventureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SoundManager.init(this)
        val monsterIndex = intent.getIntExtra("monsterIndex", -1)
        val cardName = intent.getStringExtra("cardName") ?: ""
        val charId = intent.getIntExtra("charId", 0)
        val nickname = intent.getStringExtra("nickname")
        
        val atk = intent.getIntExtra("atk", 0)
        val hp = intent.getIntExtra("cals", 500)
        val spd = intent.getIntExtra("spd", 0)
        val def = intent.getIntExtra("def", 0)
        val xp = intent.getIntExtra("xp", 0)
        val level = intent.getIntExtra("level", 1)
        val initialWins = intent.getIntExtra("wins", 0)
        val winsReq = intent.getIntExtra("winsReq", 0)
        val timeAlive = intent.getLongExtra("timeAlive", 0L)
        val evoTime = intent.getLongExtra("evoTime", 3600L)

        setContent {
            NetworldAdventure(monsterIndex, cardName, charId, nickname, atk, hp, spd, def, xp, level, initialWins, winsReq, timeAlive, evoTime) {
                finish()
            }
        }
    }
}

enum class AdventureState { EXPLORING, BATTLE, RESULT, TRAVELING }

enum class BattleProgramType(val displayName: String, val color: Color, val icon: String) {
    CANNON("CANNON", Color.Cyan, "💥"),
    SWORD("SWORD", Color.Red, "⚔️"),
    RECOVER("RECOVER", Color.Green, "💊"),
    SHIELD("SHIELD", Color.Yellow, "🛡️"),
    MYSTERY_DATA("MYSTERY DATA", Color(0xFF9C27B0), "💎")
}

data class AdventureItem(
    val id: Int,
    val type: BattleProgramType,
    val gridX: Int,
    val gridY: Int
)

data class GridEntity(
    val id: Int,
    var gridX: Int,
    var gridY: Int,
    var hp: Float,
    var maxHp: Float,
    val isEnemy: Boolean = true,
    val spriteIdx: Int = 0,
    var isDead: Boolean = false,
    var nextMoveTime: Long = 0,
    var lastAttackTime: Long = 0,
    var isHurt: Boolean = false
)

data class GridProjectile(
    val id: Int,
    var gridX: Float,
    var gridY: Int,
    val speed: Float,
    val damage: Float,
    val isPlayer: Boolean,
    var isDead: Boolean = false,
    val isBig: Boolean = false,
    val attackId: Int = 0,
    val element: ChipElement? = null
)

@Composable
fun NetworldAdventure(
    monsterIndex: Int,
    cardName: String,
    charId: Int,
    nickname: String?,
    baseAtk: Int,
    baseHp: Int,
    baseSpd: Int,
    baseDef: Int,
    initialXp: Int,
    initialLevel: Int,
    initialWins: Int,
    winsReq: Int,
    timeAlive: Long,
    evoTime: Long,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val cardManager = remember { CardManager(context) }
    val phoneManager = remember { PhoneMonsterManager(context) }
    val scope = rememberCoroutineScope()

    // Grid Battle programs + chip deck, shared per-Digimon with Grid Battle mode (2026-09-25).
    val ownerId = remember(nickname, cardName, charId) { ownerIdFor(nickname, cardName, charId) }
    val naviLoadout = remember(ownerId) {
        val loaded = NaviCustLoadout.load(context, ownerId)
        if (loaded.validate().errors.isNotEmpty()) NaviCustLoadout(emptyList()) else loaded
    }
    val chipFolder = remember(ownerId) { ChipFolder.load(context, ownerId) }
    // Attack FX override: borrow another Digimon's base attack animations (Core programs still win).
    val fxOverride = remember(ownerId) { AttackFxOverrides.load(context, ownerId) }
    val programBonuses = remember(naviLoadout) { naviLoadout.totalBonuses() }
    val battleStyle = remember(naviLoadout) { naviLoadout.style() }
    val glitchPenalty = remember(naviLoadout) { naviLoadout.glitchPenaltyHp() }
    val busterOverride = remember(naviLoadout) { naviLoadout.busterOverride() }
    val swordOverride = remember(naviLoadout) { naviLoadout.swordOverride() }
    // Player attack multiplier from programs (+ BLAZE style bonus).
    val effAtkMult = (1f + programBonuses.attackPct / 100f) *
        (if (battleStyle == BattleStyle.BLAZE) 1.1f else 1f)
    // Buster shot speed from programs (+ GALE style bonus).
    val busterSpeed = 0.45f * (1f + programBonuses.speedPct / 200f) *
        (if (battleStyle == BattleStyle.GALE) 1.15f else 1f)
    // HUD line under the player name: style + core rewires + attack bonus.
    val hudLine = remember(programBonuses, battleStyle, busterOverride, swordOverride) {
        buildList {
            if (battleStyle != BattleStyle.NONE) add("${battleStyle.name} STYLE")
            busterOverride?.let { add("Buster: ${it.displayName()}") }
            swordOverride?.let { add("Sword: ${it.displayName()}") }
            if (programBonuses.attackPct > 0) add("+${programBonuses.attackPct}% ATK")
        }.joinToString(" • ")
    }
    val chipHudHint = if (chipFolder.isBattleReady) null
        else "Build a 30-chip folder for chip power-ups!"

    var adventureState by remember { mutableStateOf(AdventureState.EXPLORING) }
    var encountersEnabled by remember { mutableStateOf(true) }
    
    // Persistent Stats
    var currentAtk by remember { mutableIntStateOf(baseAtk) }
    var currentHpStat by remember { mutableIntStateOf(baseHp) }
    var currentSpd by remember { mutableIntStateOf(baseSpd) }
    var currentDef by remember { mutableIntStateOf(baseDef) }
    var currentXp by remember { mutableIntStateOf(initialXp) }
    var currentLevel by remember { mutableIntStateOf(initialLevel) }
    var currentWins by remember { mutableIntStateOf(initialWins) }

    // Exploration State
    var areaLevel by remember { mutableIntStateOf(1) }
    // 2026-09-26: floor selection — pick a starting floor, higher floors mean
    // stronger viruses (enemy HP/damage already scale with areaLevel).
    var selectedFloor by remember { mutableStateOf<Int?>(null) }
    val highestCleared = remember { highestClearedFloor(context, cardName, charId) }
    var areaName by remember { mutableStateOf("Net Area 1") }
    var mapX by remember { mutableIntStateOf(20) }
    var mapY by remember { mutableIntStateOf(20) }
    val mapSize = 80
    val roads = remember { mutableStateListOf<Pair<Int, Int>>() }
    val mapItems = remember { mutableStateListOf<AdventureItem>() }
    var gatePos by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    // Battle State
    val battlePrograms = remember { mutableStateListOf<BattleProgramType>() }
    // Chip deck (draw pile) + hand, loaded from the saved chip folder per battle.
    val chipDeck = remember { mutableStateListOf<Int>() }
    val chipHand = remember { mutableStateListOf<Int>() }
    val enemies = remember { mutableStateListOf<GridEntity>() }
    val projectiles = remember { mutableStateListOf<GridProjectile>() }
    var playerBattleX by remember { mutableIntStateOf(1) }
    var playerBattleY by remember { mutableIntStateOf(1) }
    var playerHp by remember { mutableStateOf((baseHp + 1000).toFloat()) }
    // NaviCust HP: program bonuses + AQUA style bonus, minus glitch strain.
    val playerMaxHp = (baseHp + 1000 + programBonuses.maxHpBonus - glitchPenalty +
        if (battleStyle == BattleStyle.AQUA) 150 else 0).toFloat()
    
    var gameTime by remember { mutableLongStateOf(0L) }
    var lastBattleResult by remember { mutableStateOf(false) }

    // Visuals
    val playerSprites = remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    val enemySprites = remember { mutableStateListOf<Bitmap>() }
    val bgBitmap = remember { mutableStateOf<Bitmap?>(null) }
    
    var isAttackingAnim by remember { mutableStateOf(false) }
    var isFacingLeft by remember { mutableStateOf(false) }
    // DIM-programmed attack IDs: (small = regular, big = critical)
    val attackIds = remember(cardName, charId, fxOverride) {
        fxOverride?.let { phoneManager.getAttackIds(it.cardName, it.charId) }
            ?: phoneManager.getAttackIds(cardName, charId)
            ?: Pair(0, 0)
    }
    // Localized attack-effect burst shown around the player when firing
    val attackFxId = remember { mutableStateOf<Int?>(null) }
    val attackFxProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    // NaviCust Core program attack animation (replaces the DIM one when a core is installed)
    val coreFxKey = remember { mutableStateOf<String?>(null) }
    val critChance = remember(cardName, charId) { phoneManager.getCritChance(cardName, charId) }

    fun generateArea(lvl: Int) {
        roads.clear()
        val roadSet = mutableSetOf<Pair<Int, Int>>()
        var cx = 20; var cy = 20
        repeat(mapSize * 6) {
            roadSet.add(cx to cy)
            roadSet.add(cx+1 to cy)
            roadSet.add(cx to cy+1)
            roadSet.add(cx+1 to cy+1)
            val dir = Random.nextInt(4)
            if (dir == 0) cx++ else if (dir == 1) cx-- else if (dir == 2) cy++ else cy--
            cx = cx.coerceIn(5, mapSize - 10); cy = cy.coerceIn(5, mapSize - 10)
        }
        roads.addAll(roadSet)

        mapItems.clear()
        val itemRoads = roadSet.shuffled().take(15)
        itemRoads.forEachIndexed { index, road ->
            val type = if (Random.nextInt(100) < 20) BattleProgramType.MYSTERY_DATA
            else BattleProgramType.values().filter { it != BattleProgramType.MYSTERY_DATA }.random()
            mapItems.add(AdventureItem(index, type, road.first, road.second))
        }

        gatePos = roadSet.maxByOrNull { abs(it.first - 20) + abs(it.second - 20) }
        mapX = 20; mapY = 20; areaName = "Floor $lvl \u00b7 Net Area"; areaLevel = lvl
    }

    suspend fun reloadPlayerSprites() {
        withContext(Dispatchers.IO) {
            val card = cardManager.getCard(cardName)
            card?.let {
                val isBem = it is BemCard
                val s = it.spriteData.sprites
                if (s.size > 1) bgBitmap.value = SpriteBitmapHandler.getBitmap(s[1])
                val base = getCharacterBaseIndex(charId, isBem)
                
                val sprites = mutableMapOf<String, Bitmap>()
                SpriteBitmapHandler.getBitmap(s[base + 1])?.let { sprites["IDLE"] = it }
                SpriteBitmapHandler.getBitmap(s[base + 2])?.let { sprites["WALK"] = it }
                val atkIdx = if (isBem || charId >= 2) base + 11 else base + 3
                SpriteBitmapHandler.getBitmap(s[atkIdx])?.let { sprites["ATTACK"] = it }
                playerSprites.value = sprites
            }

            // Load Enemies from all available cards for diversity
            if (enemySprites.isEmpty()) {
                val allCardNames = cardManager.listCards()
                if (allCardNames.isNotEmpty()) {
                    // Try to get at least 15 diverse enemies
                    val poolSize = 15
                    var attempts = 0
                    while (enemySprites.size < poolSize && attempts < 50) {
                        attempts++
                        val randomCardName = allCardNames.random()
                        val enCard = cardManager.getCard(randomCardName) ?: continue
                        val enIsBem = enCard is BemCard
                        val enS = enCard.spriteData.sprites
                        val enStats = enCard.characterStats.characterEntries
                        
                        if (enStats.isNotEmpty()) {
                            val randCharIdx = Random.nextInt(enStats.size)
                            val enBase = getCharacterBaseIndex(randCharIdx, enIsBem)
                            if (enBase + 1 < enS.size) {
                                SpriteBitmapHandler.getBitmap(enS[enBase + 1])?.let { 
                                    if (!enemySprites.contains(it)) {
                                        enemySprites.add(it)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Fallback to local card if no other cards found or pool is small
                if (enemySprites.size < 5) {
                    val card = cardManager.getCard(cardName)
                    card?.let {
                        val isBem = it is BemCard
                        val s = it.spriteData.sprites
                        repeat(10) {
                            val randId = Random.nextInt(15)
                            val enBase = getCharacterBaseIndex(randId, isBem)
                            if (enBase + 1 < s.size) SpriteBitmapHandler.getBitmap(s[enBase + 1])?.let { enemySprites.add(it) }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedFloor) { selectedFloor?.let { generateArea(it) } }

    LaunchedEffect(cardName, charId) {
        reloadPlayerSprites()
    }

    // Battle Engine
    LaunchedEffect(adventureState) {
        if (adventureState != AdventureState.BATTLE) return@LaunchedEffect
        while (adventureState == AdventureState.BATTLE) {
            val dt = 16L
            gameTime += dt
            
            val nextProjectiles = projectiles.map { it.copy(gridX = it.gridX + it.speed) }
                .filter { it.gridX in -0.5f..9.5f && !it.isDead }
                .toMutableList()
            
            enemies.forEachIndexed { i, e ->
                if (!e.isDead) {
                    val hitIndex = nextProjectiles.indexOfFirst { it.isPlayer && abs(it.gridX - e.gridX) < 0.45f && it.gridY == e.gridY }
                    if (hitIndex != -1) {
                        val hit = nextProjectiles[hitIndex]
                        enemies[i] = e.copy(hp = e.hp - hit.damage, isHurt = true)
                        nextProjectiles[hitIndex] = hit.copy(isDead = true)
                        scope.launch { delay(200); if (i < enemies.size) enemies[i] = enemies[i].copy(isHurt = false) }
                        if (enemies[i].hp <= 0) enemies[i] = enemies[i].copy(isDead = true, hp = 0f)
                    }
                }
            }

            val eHitIndex = nextProjectiles.indexOfFirst { !it.isPlayer && abs(it.gridX - playerBattleX) < 0.45f && it.gridY == playerBattleY }
            if (eHitIndex != -1) {
                playerHp -= nextProjectiles[eHitIndex].damage
                nextProjectiles[eHitIndex] = nextProjectiles[eHitIndex].copy(isDead = true)
                SoundManager.play("hit")
                if (playerHp <= 0) { lastBattleResult = false; phoneManager.addLoss(); adventureState = AdventureState.RESULT }
            }

            projectiles.clear(); projectiles.addAll(nextProjectiles.filter { !it.isDead })

            enemies.forEachIndexed { i, e ->
                if (!e.isDead) {
                    if (gameTime > e.nextMoveTime) {
                        val move = Random.nextInt(4)
                        val nx = (e.gridX + (if (move == 0) 1 else if (move == 1) -1 else 0)).coerceIn(3, 5)
                        val ny = (e.gridY + (if (move == 2) 1 else if (move == 3) -1 else 0)).coerceIn(0, 2)
                        if (enemies.none { it.id != e.id && !it.isDead && it.gridX == nx && it.gridY == ny }) {
                            enemies[i] = e.copy(gridX = nx, gridY = ny, nextMoveTime = gameTime + Random.nextLong(1200, 2500))
                        } else {
                            enemies[i] = e.copy(nextMoveTime = gameTime + 500)
                        }
                    }
                    if (gameTime > e.lastAttackTime + Random.nextLong((2000L - areaLevel * 100L).coerceAtLeast(800L), (4500L - areaLevel * 100L).coerceAtLeast(1500L))) {
                        val enemyDamage = (playerMaxHp * 0.12f + currentLevel * 5f) * (1f + areaLevel * 0.05f) *
                            (if (battleStyle == BattleStyle.TERRA) 0.9f else 1f)
                        projectiles.add(GridProjectile(Random.nextInt(10000), e.gridX.toFloat() - 0.5f, e.gridY, -0.25f, enemyDamage, false))
                        enemies[i] = enemies[i].copy(lastAttackTime = gameTime)
                    }
                }
            }
            
            if (enemies.isNotEmpty() && enemies.all { it.isDead }) {
                currentXp += 400
                currentWins++
                phoneManager.addWin()
                val threshold = 1000 + (currentLevel - 1) * 500
                if (currentXp >= threshold) {
                    currentXp -= threshold; currentLevel++
                    currentAtk += 50; currentDef += 50; currentSpd += 50
                    phoneManager.updateBonusStats(50, 50, 50, 50)
                }
                lastBattleResult = true; adventureState = AdventureState.RESULT
            }
            delay(dt)
        }
    }

    fun triggerEncounter() {
        if (!encountersEnabled) return
        if (Random.nextInt(100) < 18) {
            enemies.clear(); projectiles.clear()
            playerBattleX = 1; playerBattleY = 1; playerHp = playerMaxHp

            // Draw the opening chip hand (5 cards) from the saved folder.
            chipDeck.clear()
            chipHand.clear()
            if (chipFolder.isBattleReady) {
                chipDeck.addAll(chipFolder.chipIds.shuffled())
                repeat(5) {
                    if (chipDeck.isNotEmpty()) chipHand.add(chipDeck.removeAt(0))
                }
            }

            val scaledEnemyHp = (playerMaxHp * 0.2f + currentAtk * 0.8f) * (1f + areaLevel * 0.1f)
            
            repeat(Random.nextInt(1, 4)) {
                enemies.add(GridEntity(
                    id = it, gridX = Random.nextInt(3, 6), gridY = Random.nextInt(3),
                    hp = scaledEnemyHp, maxHp = scaledEnemyHp,
                    spriteIdx = Random.nextInt(enemySprites.size.coerceAtLeast(1))
                ))
            }
            adventureState = AdventureState.BATTLE
        }
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (selectedFloor == null) {
                AdventureFloorScreen(
                    highestCleared = highestCleared,
                    onFloorSelected = {
                        SoundManager.play("ui")
                        selectedFloor = it
                    },
                    onExit = onExit
                )
            } else when (adventureState) {
                AdventureState.EXPLORING -> {
                    ExplorationScreen(
                        areaName = areaName, nickname = nickname ?: cardName,
                        mapX = mapX, mapY = mapY, roads = roads, gatePos = gatePos,
                        mapItems = mapItems,
                        playerSprite = playerSprites.value["IDLE"],
                        isFacingLeft = isFacingLeft,
                        encountersEnabled = encountersEnabled,
                        onToggleEncounters = { encountersEnabled = it },
                        onMove = { dx, dy ->
                            val nx = (mapX + dx).coerceIn(0, mapSize - 1)
                            val ny = (mapY + dy).coerceIn(0, mapSize - 1)
                            if (roads.any { it.first == nx && it.second == ny }) {
                                mapX = nx; mapY = ny
                                if (dx < 0) isFacingLeft = true else if (dx > 0) isFacingLeft = false
                                
                                val item = mapItems.find { it.gridX == nx && it.gridY == ny }
                                if (item != null) {
                                    if (item.type == BattleProgramType.MYSTERY_DATA) {
                                        phoneManager.unlockCardSecret(cardName)
                                        Toast.makeText(context, "DIM SECRET UNLOCKED!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        battlePrograms.add(item.type)
                                        Toast.makeText(context, "Found ${item.type.displayName}!", Toast.LENGTH_SHORT).show()
                                    }
                                    mapItems.remove(item)
                                }

                                if (mapX == gatePos?.first && mapY == gatePos?.second) adventureState = AdventureState.TRAVELING
                                else triggerEncounter()
                            }
                        },
                        onExit = onExit
                    )
                }
                AdventureState.BATTLE -> {
                    GridBattleScreen(
                        nickname = nickname ?: cardName,
                        level = currentLevel, hp = playerHp, maxHp = playerMaxHp,
                        hudLine = hudLine, chipHudHint = chipHudHint,
                        playerX = playerBattleX, playerY = playerBattleY,
                        enemies = enemies, projectiles = projectiles,
                        playerSprites = playerSprites.value, enemySprites = enemySprites,
                        isAttacking = isAttackingAnim,
                        attackFxId = attackFxId.value,
                        attackFxProgress = attackFxProgress.value,
                        coreFxKey = coreFxKey.value,
                        battlePrograms = battlePrograms,
                        chipHand = chipHand,
                        onUseProgram = { program ->
                            when (program) {
                                BattleProgramType.CANNON -> {
                                    SoundManager.play("projectile")
                                    projectiles.add(GridProjectile(Random.nextInt(10000), playerBattleX.toFloat() + 0.5f, playerBattleY, 0.6f, (500f + currentAtk) * effAtkMult, true))
                                }
                                BattleProgramType.SWORD -> {
                                    SoundManager.play("sword")
                                    val tx = playerBattleX + 1
                                    enemies.forEachIndexed { i, e -> 
                                        if (!e.isDead && e.gridX == tx && abs(e.gridY - playerBattleY) <= 1) {
                                            enemies[i] = e.copy(hp = e.hp - (800f + currentAtk) * effAtkMult, isHurt = true)
                                            scope.launch { delay(200); if (i < enemies.size) enemies[i] = enemies[i].copy(isHurt = false) }
                                            if (enemies[i].hp <= 0) enemies[i] = enemies[i].copy(isDead = true, hp = 0f)
                                        }
                                    }
                                }
                                BattleProgramType.RECOVER -> {
                                    SoundManager.play("heal")
                                    playerHp = (playerHp + 500f).coerceAtMost(playerMaxHp)
                                }
                                BattleProgramType.SHIELD -> {
                                    SoundManager.play("heal")
                                    playerHp = (playerHp + 200f).coerceAtMost(playerMaxHp)
                                }
                                else -> {}
                            }
                            battlePrograms.remove(program)
                        },
                        onUseChip = { chipId ->
                            val chip = ChipLibrary.byId(chipId) ?: return@GridBattleScreen
                            SoundManager.play(SoundManager.forEffectKind(chip.effectKind.name))
                            when (chip.effectKind) {
                                EffectKind.PROJECTILE -> {
                                    projectiles.add(GridProjectile(Random.nextInt(10000), playerBattleX.toFloat() + 0.5f, playerBattleY, 0.6f, chip.damage * effAtkMult, true, element = chip.element))
                                }
                                EffectKind.SWORD, EffectKind.MELEE -> {
                                    val tx = playerBattleX + 1
                                    enemies.forEachIndexed { i, e ->
                                        if (!e.isDead && e.gridX == tx && abs(e.gridY - playerBattleY) <= 1) {
                                            enemies[i] = e.copy(hp = e.hp - chip.damage * effAtkMult, isHurt = true)
                                            scope.launch { delay(200); if (i < enemies.size) enemies[i] = enemies[i].copy(isHurt = false) }
                                            if (enemies[i].hp <= 0) enemies[i] = enemies[i].copy(isDead = true, hp = 0f)
                                        }
                                    }
                                }
                                EffectKind.LOB -> {
                                    projectiles.add(GridProjectile(Random.nextInt(10000), playerBattleX.toFloat() + 0.5f, playerBattleY, 0.3f, chip.damage * 1.2f * effAtkMult, true, element = chip.element))
                                }
                                EffectKind.BEAM -> {
                                    projectiles.add(GridProjectile(Random.nextInt(10000), playerBattleX.toFloat() + 0.5f, playerBattleY, 0.8f, chip.damage * 1.5f * effAtkMult, true, isBig = true, element = chip.element))
                                }
                                EffectKind.SUMMON -> {
                                    (playerBattleY - 1..playerBattleY + 1).map { it.coerceIn(0, 2) }.distinct().forEach { row ->
                                        projectiles.add(GridProjectile(Random.nextInt(10000), playerBattleX.toFloat() + 0.5f, row, 0.6f, chip.damage * 0.6f * effAtkMult, true, element = chip.element))
                                    }
                                }
                                EffectKind.TRAP -> {
                                    enemies.forEachIndexed { i, e ->
                                        if (!e.isDead && e.gridX >= 3) {
                                            enemies[i] = e.copy(hp = e.hp - chip.damage * effAtkMult, isHurt = true)
                                            scope.launch { delay(200); if (i < enemies.size) enemies[i] = enemies[i].copy(isHurt = false) }
                                            if (enemies[i].hp <= 0) enemies[i] = enemies[i].copy(isDead = true, hp = 0f)
                                        }
                                    }
                                }
                                EffectKind.SUPPORT -> {
                                    playerHp = (playerHp + chip.damage).coerceAtMost(playerMaxHp)
                                }
                            }
                            // Discard the used chip, draw a replacement, reshuffle when spent.
                            chipHand.remove(chipId)
                            if (chipDeck.isNotEmpty()) {
                                chipHand.add(chipDeck.removeAt(0))
                            } else if (chipHand.isEmpty() && chipFolder.isBattleReady) {
                                chipDeck.addAll(chipFolder.chipIds.shuffled())
                                if (chipDeck.isNotEmpty()) chipHand.add(chipDeck.removeAt(0))
                            }
                        },
                        onMove = { dx, dy ->
                            playerBattleX = (playerBattleX + dx).coerceIn(0, 2)
                            playerBattleY = (playerBattleY + dy).coerceIn(0, 2)
                        },
                        onAttack = { type ->
                            isAttackingAnim = true
                            // Roll the DIM-programmed attacks: big attack lands as a crit (1.5x).
                            // Charge programs widen the crit window.
                            val isBig = Random.nextFloat() < critChance + programBonuses.chargePct / 400f
                            val usedAttackId = if (isBig) attackIds.second else attackIds.first
                            val dmgMult = if (isBig) 1.5f else 1f
                            // Core programs REPLACE the DIM-programmed attack animation with their element effect
                            val coreKey = if (type == "SWORD") swordOverride?.animKey else busterOverride?.animKey
                            if (coreKey != null) {
                                coreFxKey.value = coreKey
                                scope.launch {
                                    attackFxProgress.snapTo(0f)
                                    attackFxProgress.animateTo(1f, androidx.compose.animation.core.tween(450))
                                    coreFxKey.value = null
                                }
                            } else {
                                // Flash the DIM-programmed attack effect around the player
                                attackFxId.value = usedAttackId
                                scope.launch {
                                    attackFxProgress.snapTo(0f)
                                    attackFxProgress.animateTo(1f, androidx.compose.animation.core.tween(400))
                                    attackFxId.value = null
                                }
                            }
                            SoundManager.play(if (type == "SWORD") "sword" else if (isBig) "buster_charged" else "buster")
                            if (type == "SWORD") {
                                val tx = playerBattleX + 1
                                enemies.forEachIndexed { i, e ->
                                    if (!e.isDead && e.gridX == tx && abs(e.gridY - playerBattleY) <= 1) {
                                        enemies[i] = e.copy(hp = e.hp - (250f + currentAtk) * dmgMult * effAtkMult, isHurt = true)
                                        scope.launch { delay(200); if (i < enemies.size) enemies[i] = enemies[i].copy(isHurt = false) }
                                        if (enemies[i].hp <= 0) enemies[i] = enemies[i].copy(isDead = true, hp = 0f)
                                    }
                                }
                            } else {
                                projectiles.add(GridProjectile(Random.nextInt(10000), playerBattleX.toFloat() + 0.5f, playerBattleY, busterSpeed, (60f + currentAtk/2) * dmgMult * effAtkMult, true, isBig = isBig, attackId = usedAttackId, element = busterOverride?.element))
                            }
                            scope.launch { delay(250); isAttackingAnim = false }
                        }
                    )
                }
                AdventureState.RESULT -> {
                    LaunchedEffect(lastBattleResult) {
                        SoundManager.play(if (lastBattleResult) "win" else "lose")
                    }
                    ResultScreen(isWin = lastBattleResult, xpGained = 400, onContinue = { 
                        if (lastBattleResult) adventureState = AdventureState.EXPLORING 
                        else {
                            if (monsterIndex != -1) LabStorage.updateMonsterStats(context, monsterIndex, currentAtk, currentHpStat, currentSpd, currentDef, currentXp, currentLevel, wins = currentWins, winsReq = winsReq, time = timeAlive, evo = evoTime)
                            onExit()
                        }
                    })
                }
                AdventureState.TRAVELING -> {
                    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.Cyan)
                            Spacer(Modifier.height(16.dp))
                            Text("ACCESSING NEXT AREA...", color = Color.Cyan, fontWeight = FontWeight.Bold)
                            LaunchedEffect(Unit) {
                                delay(2000)
                                recordFloorCleared(context, cardName, charId, areaLevel)
                                generateArea(areaLevel + 1)
                                adventureState = AdventureState.EXPLORING
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExplorationScreen(areaName: String, nickname: String, mapX: Int, mapY: Int, roads: List<Pair<Int, Int>>, gatePos: Pair<Int, Int>?, mapItems: List<AdventureItem>, playerSprite: Bitmap?, isFacingLeft: Boolean, encountersEnabled: Boolean, onToggleEncounters: (Boolean) -> Unit, onMove: (Int, Int) -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val tileSize = with(density) { 64.dp.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            val offsetX = (size.width / 2) - (mapX * tileSize) - (tileSize / 2)
            val offsetY = (size.height / 2) - (mapY * tileSize) - (tileSize / 2)
            
            // Grid lines
            for(i in 0 until 20) {
                drawLine(Color.Cyan.copy(0.1f), androidx.compose.ui.geometry.Offset(0f, i * 150f), androidx.compose.ui.geometry.Offset(size.width, i * 150f))
                drawLine(Color.Cyan.copy(0.1f), androidx.compose.ui.geometry.Offset(i * 150f, 0f), androidx.compose.ui.geometry.Offset(i * 150f, size.height))
            }

            // Roads
            roads.forEach { (rx, ry) ->
                val tx = offsetX + rx * tileSize; val ty = offsetY + ry * tileSize
                if (tx > -tileSize && tx < size.width && ty > -tileSize && ty < size.height) {
                    val isG = rx == gatePos?.first && ry == gatePos?.second
                    drawRect(color = if (isG) Color(0, 200, 100, 200) else Color(0, 80, 200, 220), topLeft = androidx.compose.ui.geometry.Offset(tx + 4, ty + 4), size = androidx.compose.ui.geometry.Size(tileSize - 8, tileSize - 8))
                    val bc = if (isG) Color.Green else Color.Cyan
                    if (!roads.any { it.first == rx && it.second == ry - 1 }) drawLine(bc, androidx.compose.ui.geometry.Offset(tx, ty), androidx.compose.ui.geometry.Offset(tx + tileSize, ty), 6f)
                    if (!roads.any { it.first == rx && it.second == ry + 1 }) drawLine(bc, androidx.compose.ui.geometry.Offset(tx, ty + tileSize), androidx.compose.ui.geometry.Offset(tx + tileSize, ty + tileSize), 6f)
                    if (!roads.any { it.first == rx - 1 && it.second == ry }) drawLine(bc, androidx.compose.ui.geometry.Offset(tx, ty), androidx.compose.ui.geometry.Offset(tx, ty + tileSize), 6f)
                    if (!roads.any { it.first == rx + 1 && it.second == ry }) drawLine(bc, androidx.compose.ui.geometry.Offset(tx + tileSize, ty), androidx.compose.ui.geometry.Offset(tx + tileSize, ty + tileSize), 6f)
                }
            }

            // Items
            mapItems.forEach { item ->
                val tx = offsetX + item.gridX * tileSize; val ty = offsetY + item.gridY * tileSize
                if (tx > -tileSize && tx < size.width && ty > -tileSize && ty < size.height) {
                    drawCircle(item.type.color, radius = 12f, center = androidx.compose.ui.geometry.Offset(tx + tileSize/2, ty + tileSize/2))
                    drawCircle(Color.White, radius = 14f, center = androidx.compose.ui.geometry.Offset(tx + tileSize/2, ty + tileSize/2), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            playerSprite?.let { Image(it.asImageBitmap(), null, Modifier.size(60.dp).graphicsLayer { scaleX = if (isFacingLeft) 1f else -1f }) }
        }
        Column(Modifier.padding(16.dp)) {
            Text(nickname, color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(areaName, color = Color.Cyan, fontSize = 12.sp)
            
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = encountersEnabled,
                    onCheckedChange = onToggleEncounters,
                    colors = CheckboxDefaults.colors(checkmarkColor = Color.Black, checkedColor = Color.Cyan)
                )
                Text("Virus Encounters", color = Color.White, fontSize = 10.sp)
            }
        }
        
        gatePos?.let { gp ->
            val angle = atan2((gp.second - mapY).toFloat(), (gp.first - mapX).toFloat())
            Icon(
                Icons.Default.Navigation, null,
                Modifier
                    .align(Alignment.Center)
                    .offset(y = (-100).dp)
                    .size(32.dp)
                    .rotate(Math.toDegrees(angle.toDouble()).toFloat() + 90f),
                tint = Color.Green.copy(alpha = 0.6f)
            )
        }

        Button(onClick = onExit, Modifier.align(Alignment.TopEnd).padding(16.dp)) { Text("LOG OUT") }
        Box(Modifier.fillMaxSize().padding(bottom = 80.dp, start = 32.dp), contentAlignment = Alignment.BottomStart) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onMove(0, -1) }, Modifier.size(56.dp).background(Color.DarkGray.copy(0.8f), CircleShape)) { Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White) }
                Row(modifier = Modifier.padding(vertical = 12.dp)) {
                    IconButton(onClick = { onMove(-1, 0) }, Modifier.size(56.dp).background(Color.DarkGray.copy(0.8f), CircleShape)) { Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.White) }
                    Spacer(Modifier.width(64.dp))
                    IconButton(onClick = { onMove(1, 0) }, Modifier.size(56.dp).background(Color.DarkGray.copy(0.8f), CircleShape)) { Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White) }
                }
                IconButton(onClick = { onMove(0, 1) }, Modifier.size(56.dp).background(Color.DarkGray.copy(0.8f), CircleShape)) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White) }
            }
        }
    }
}

@Composable
fun GridBattleScreen(
    nickname: String, level: Int, hp: Float, maxHp: Float, playerX: Int, playerY: Int,
    enemies: List<GridEntity>, projectiles: List<GridProjectile>,
    playerSprites: Map<String, Bitmap>, enemySprites: List<Bitmap>,
    isAttacking: Boolean,
    attackFxId: Int?,
    attackFxProgress: Float,
    coreFxKey: String?,
    battlePrograms: List<BattleProgramType>,
    onUseProgram: (BattleProgramType) -> Unit,
    hudLine: String,
    chipHudHint: String?,
    chipHand: List<Int>,
    onUseChip: (Int) -> Unit,
    onMove: (Int, Int) -> Unit, onAttack: (String) -> Unit
) {
    val density = LocalDensity.current
    val cellSize = 50.dp 
    val cellSizePx = with(density) { cellSize.toPx() }
    Column(Modifier.fillMaxSize().background(Color(0, 0, 15)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Column {
                Text("$nickname LV$level", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (hudLine.isNotEmpty()) Text(hudLine, color = Color.Cyan, fontSize = 9.sp)
                chipHudHint?.let { Text(it, color = Color.Gray, fontSize = 9.sp) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("HP ${hp.toInt()} / ${maxHp.toInt()}", color = Color.White, fontSize = 10.sp)
                LinearProgressIndicator(progress = (hp / maxHp).coerceIn(0f, 1f), Modifier.width(120.dp).height(10.dp).clip(RoundedCornerShape(5.dp)), color = Color.Green, backgroundColor = Color.Red)
            }
        }
        
        // Battle Program Slide Menu
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(60.dp).background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp)).padding(4.dp)) {
            if (battlePrograms.isEmpty() && chipHand.isEmpty()) {
                Text("NO PROGRAMS LOADED", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.Center))
            } else {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chipHand.size) { index ->
                        val chipId = chipHand[index]
                        val chip = ChipLibrary.byId(chipId)
                        if (chip != null) {
                            val chipColor = elementColor(chip.element)
                            Column(
                                Modifier
                                    .width(70.dp)
                                    .fillMaxHeight()
                                    .background(chipColor.copy(0.2f), RoundedCornerShape(4.dp))
                                    .border(1.dp, chipColor, RoundedCornerShape(4.dp))
                                    .clickable { onUseChip(chipId) }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(chip.name, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text("${chip.damage} DMG", color = Color.White, fontSize = 8.sp)
                            }
                        }
                    }
                    items(battlePrograms.size) { index ->
                        val program = battlePrograms[index]
                        Column(
                            Modifier
                                .width(70.dp)
                                .fillMaxHeight()
                                .background(program.color.copy(0.2f), RoundedCornerShape(4.dp))
                                .border(1.dp, program.color, RoundedCornerShape(4.dp))
                                .clickable { onUseProgram(program) }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(program.icon, fontSize = 16.sp)
                            Text(program.displayName, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column {
                repeat(3) { y -> 
                    Row { 
                        repeat(6) { x -> 
                            Box(Modifier.size(cellSize).border(1.dp, if (x < 3) Color.Blue.copy(0.4f) else Color.Red.copy(0.4f)).background(if (x < 3) Color.Blue.copy(0.1f) else Color.Red.copy(0.1f))) 
                        } 
                    } 
                }
            }
            Box(Modifier.size(width = cellSize * 6, height = cellSize * 3)) {
                // Character in Battle: Reverted scaleX = -1f to fix facing backwards issue
                val pBmp = if (isAttacking) {
                    playerSprites["ATTACK"] ?: playerSprites["IDLE"]
                } else playerSprites["IDLE"]
                pBmp?.let {
                    Image(it.asImageBitmap(), null, Modifier.size(cellSize).offset { IntOffset((playerX * cellSizePx).toInt(), (playerY * cellSizePx).toInt()) }.graphicsLayer { scaleX = -1f })
                }
                // DIM-programmed attack effect bursting around the attacker
                attackFxId?.let { fxId ->
                    val fxSize = cellSize * 2.5f
                    val fxPx = with(density) { fxSize.toPx() }
                    Box(
                        Modifier.size(fxSize).offset {
                            IntOffset(
                                (playerX * cellSizePx + cellSizePx / 2 - fxPx / 2).toInt(),
                                (playerY * cellSizePx + cellSizePx / 2 - fxPx / 2).toInt()
                            )
                        }
                    ) {
                        AttackEffectCanvas(attackId = fxId, progress = attackFxProgress, modifier = Modifier.fillMaxSize())
                    }
                }
                // NaviCust Core element attack effect (replaces the DIM animation)
                coreFxKey?.let { key ->
                    val fxSize = cellSize * 2.5f
                    val fxPx = with(density) { fxSize.toPx() }
                    Box(
                        Modifier.size(fxSize).offset {
                            IntOffset(
                                (playerX * cellSizePx + cellSizePx / 2 - fxPx / 2).toInt(),
                                (playerY * cellSizePx + cellSizePx / 2 - fxPx / 2).toInt()
                            )
                        }
                    ) {
                        com.example.vitalwearclonev1.gridbattle.CoreAttackFx(animKey = key, progress = attackFxProgress, modifier = Modifier.fillMaxSize())
                    }
                }
                enemies.forEach { e ->
                    if (!e.isDead) {
                        (enemySprites.getOrNull(e.spriteIdx) ?: playerSprites["IDLE"])?.let {
                            Image(it.asImageBitmap(), null, Modifier.size(cellSize).offset { IntOffset((e.gridX * cellSizePx).toInt(), (e.gridY * cellSizePx).toInt()) }.graphicsLayer { scaleX = 1f; alpha = if (e.isHurt) 0.5f else 1f })
                        }
                        Box(Modifier.width(cellSize).height(4.dp).offset { IntOffset((e.gridX * cellSizePx).toInt(), (e.gridY * cellSizePx).toInt() - 12) }.background(Color.Red)) { Box(Modifier.fillMaxWidth(e.hp / e.maxHp).fillMaxHeight().background(Color.Green)) }
                    }
                }
                projectiles.forEach { p ->
                    val tint = if (p.isPlayer) p.element?.let { elementColor(it) } ?: attackEffectColor(p.attackId) else Color.Magenta
                    Box(Modifier.size(if (p.isBig) 18.dp else 10.dp).offset { IntOffset((p.gridX * cellSizePx).toInt() + 20, (p.gridY * cellSizePx).toInt() + 20) }.background(tint, CircleShape).border(1.dp, Color.White, CircleShape))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 60.dp), Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onMove(0, -1) }, Modifier.background(Color.DarkGray.copy(0.8f), CircleShape)) { Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White) }
                Row(modifier = Modifier.padding(vertical = 10.dp)) {
                    IconButton(onClick = { onMove(-1, 0) }, Modifier.background(Color.DarkGray.copy(0.8f), CircleShape)) { Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.White) }
                    Spacer(Modifier.width(44.dp))
                    IconButton(onClick = { onMove(1, 0) }, Modifier.background(Color.DarkGray.copy(0.8f), CircleShape)) { Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White) }
                }
                IconButton(onClick = { onMove(0, 1) }, Modifier.background(Color.DarkGray.copy(0.8f), CircleShape)) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White) }
            }
            Row {
                Button(onClick = { onAttack("SWORD") }, Modifier.size(75.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red), shape = CircleShape) { Text("SWD", color = Color.White) }
                Spacer(Modifier.width(16.dp))
                Button(onClick = { onAttack("BUSTER") }, Modifier.size(75.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.Cyan), shape = CircleShape) { Text("BST") }
            }
        }
    }
}

@Composable
fun ResultScreen(isWin: Boolean, xpGained: Int, onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.9f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isWin) "VIRUS DELETED" else "CONNECTION LOST", color = if (isWin) Color.Cyan else Color.Red, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            if (isWin) Text("+$xpGained XP", color = Color.Green, fontSize = 20.sp)
            Button(onClick = onContinue, Modifier.padding(top = 32.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) { Text("CONTINUE", color = Color.White) }
        }
    }
}

// Battle-chip / Core element tint for projectiles and chip cards (2026-09-25).
private fun elementColor(element: ChipElement): Color = when (element) {
    ChipElement.FIRE -> Color(0xFFFF6B35)
    ChipElement.WATER -> Color(0xFF29B6F6)
    ChipElement.ELEC -> Color(0xFFFFEB3B)
    ChipElement.WOOD -> Color(0xFF66BB6A)
    ChipElement.SWORD -> Color(0xFFE0E0E0)
    ChipElement.WIND -> Color(0xFF80DEEA)
    ChipElement.CURSOR -> Color(0xFFCE93D8)
    ChipElement.BREAK -> Color(0xFFFF8A65)
    ChipElement.PLUS -> Color(0xFFFFF176)
    ChipElement.NULL -> Color(0xFF00BCD4)
}

private fun getCharacterBaseIndex(characterId: Int, isBem: Boolean): Int {
    if (isBem) return 54 + (characterId * 14)
    var currentIdx = 10
    for (i in 0 until characterId) { currentIdx += when(i) { 0 -> 6; 1 -> 7; else -> 14 } }
    return currentIdx
}

/** Floor-selection progress for the Networld adventure, 2026-09-26. */
private const val ADVENTURE_FLOORS = 20

private fun adventurePrefs(context: Context) =
    context.getSharedPreferences("adventure_prefs", Context.MODE_PRIVATE)

private fun highestClearedFloor(context: Context, cardName: String, charId: Int): Int =
    adventurePrefs(context).getInt("highest_floor_${cardName}_$charId", 0)

private fun recordFloorCleared(context: Context, cardName: String, charId: Int, floor: Int) {
    val p = adventurePrefs(context)
    val key = "highest_floor_${cardName}_$charId"
    if (floor > p.getInt(key, 0)) p.edit().putInt(key, floor).apply()
}

/** Mirrors the enemy scaling already in the adventure: HP x(1+lvl*0.1), dmg x(1+lvl*0.05). */
private fun virusHpMult(floor: Int): String = "\u00d7%.1f".format(1f + floor * 0.1f)
private fun virusDmgMult(floor: Int): String = "\u00d7%.2f".format(1f + floor * 0.05f)

@Composable
fun AdventureFloorScreen(highestCleared: Int, onFloorSelected: (Int) -> Unit, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(5, 5, 20))
            .statusBarsPadding().padding(16.dp)
    ) {
        Text("NETWORLD", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(
            "Pick a floor. Higher floors mean stronger viruses — clear a floor's gate to unlock the next.",
            color = Color.Gray, fontSize = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (highestCleared > 0) "Deepest descent: floor $highestCleared" else "No floors cleared yet",
            color = Color(0xFF9CCC65), fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ADVENTURE_FLOORS) { index ->
                val floor = index + 1
                val unlocked = floor <= highestCleared + 1
                Box(
                    modifier = Modifier
                        .aspectRatio(0.85f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (unlocked) Color(30, 60, 120) else Color.DarkGray)
                        .clickable(enabled = unlocked) { onFloorSelected(floor) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (unlocked) "FLOOR $floor" else "LOCKED",
                            color = if (unlocked) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (unlocked) {
                            Spacer(Modifier.height(4.dp))
                            Text("Virus HP ${virusHpMult(floor)}", color = Color(0xFFFFAB91), fontSize = 10.sp)
                            Text("Virus ATK ${virusDmgMult(floor)}", color = Color(0xFFFFAB91), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
