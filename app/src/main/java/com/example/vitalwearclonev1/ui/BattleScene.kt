package com.example.vitalwearclonev1.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.example.vitalwearclonev1.communication.SpriteSyncManager
import com.example.vitalwearclonev1.monster.BattleOpponent
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.github.cfogrady.vb.dim.card.BemCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class BattlePhase { INTRO, ATTACK, RESULT }

@Composable
fun BattleScene(
    myCardName: String,
    myCharId: Int,
    opponent: BattleOpponent,
    seed: Long = System.currentTimeMillis(),
    onResult: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cardManager = remember { CardManager(context) }
    val monsterManager = remember { PhoneMonsterManager(context) }
    val spriteSync = remember { SpriteSyncManager(context) }

    val mySprites = remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }
    val enemySprites = remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }
    
    val phase = remember { mutableStateOf(BattlePhase.INTRO) }
    val win = remember { mutableStateOf<Boolean?>(null) }
    
    val myOffset = remember { Animatable(0f) }
    val enemyOffset = remember { Animatable(0f) }
    val currentFrame = remember { mutableIntStateOf(0) }
    val attackFrame = remember { mutableIntStateOf(0) }
    val isMyAttacking = remember { mutableStateOf(false) }
    val isEnemyAttacking = remember { mutableStateOf(false) }
    val battleRandom = remember { Random(seed) }

    // Health States
    val myState = remember { monsterManager.getCurrentMonster() }
    val myMaxHP = remember { ((myState?.baseHp ?: 500) + (myState?.healthBonus ?: 0)).toFloat() }
    val enemyMaxHP = remember { opponent.hp.toFloat() }
    var myCurrentHP by remember { mutableFloatStateOf(myMaxHP) }
    var enemyCurrentHP by remember { mutableFloatStateOf(enemyMaxHP) }
    
    var battleLog by remember { mutableStateOf("READY?") }

    // VB-style attack cutscene: null = wide view, "me"/"enemy" = camera on attacker
    val cutscene = remember { mutableStateOf<String?>(null) }
    val myCrit = remember { mutableStateOf(false) }
    val enemyCrit = remember { mutableStateOf(false) }
    val impactFlash = remember { mutableStateOf(false) }
    // DIM-programmed attack IDs: (small = regular, big = critical)
    val myAttackIds = remember { mutableStateOf(Pair(0, 0)) }
    val enemyAttackIds = remember { mutableStateOf(Pair(0, 0)) }
    val effectProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    // Drives the attack effect animation while the cutscene is on screen
    LaunchedEffect(cutscene.value) {
        if (cutscene.value != null) {
            effectProgress.snapTo(0f)
            effectProgress.animateTo(1f, androidx.compose.animation.core.tween(650))
        }
    }

    LaunchedEffect(Unit) {
        var enemyCritChance = 0.15f
        // Load Sprites
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val myCard = cardManager.getCard(myCardName)
            myCard?.let {
                val isBem = it is BemCard
                val s = it.spriteData.sprites
                val atkLegacyIdx = monsterManager.getBattleSpriteIndex(myCharId, isBem)
                myAttackIds.value = monsterManager.getAttackIds(it, myCharId) ?: Pair(0, 0)
                mySprites.value = mapOf(
                    "IDLE1" to SpriteBitmapHandler.getBitmap(s[monsterManager.getIdleSpriteIndices(myCharId, isBem)[0]]),
                    "IDLE2" to SpriteBitmapHandler.getBitmap(s[monsterManager.getIdleSpriteIndices(myCharId, isBem)[1]]),
                    "ATK" to SpriteBitmapHandler.getBitmap(s[atkLegacyIdx]),
                    "WIN" to SpriteBitmapHandler.getBitmap(s[monsterManager.getWinSpriteIndex(myCharId, isBem)]),
                    "LOSE" to SpriteBitmapHandler.getBitmap(s[monsterManager.getLoseSpriteIndex(myCharId, isBem)])
                )
            }
            
            val enCard = cardManager.getCard(opponent.cardName.trim()) 
                ?: (opponent.dimHash?.let { cardManager.getCardByHash(it) })
                ?: cardManager.getCardById(opponent.dimId)
                
            if (enCard != null) {
                // IMPORTANT: Calculate isBem based on the ACTUAL card being used for sprites
                val cardIsBem = enCard is BemCard
                val s = enCard.spriteData.sprites
                
                // Safe index calculation
                val enemyBase = monsterManager.getIdleSpriteIndices(opponent.characterId, cardIsBem)
                val idle1 = if (enemyBase[0] < s.size) enemyBase[0] else 0
                val idle2 = if (enemyBase[1] < s.size) enemyBase[1] else 0
                val atkIdx = monsterManager.getBattleSpriteIndex(opponent.characterId, cardIsBem).let { if (it < s.size) it else 0 }
                val winIdx = monsterManager.getWinSpriteIndex(opponent.characterId, cardIsBem).let { if (it < s.size) it else 0 }
                val loseIdx = monsterManager.getLoseSpriteIndex(opponent.characterId, cardIsBem).let { if (it < s.size) it else 0 }
                enemyAttackIds.value = monsterManager.getAttackIds(enCard, opponent.characterId) ?: Pair(0, 0)
                enemyCritChance = monsterManager.getCritChance(enCard, opponent.characterId)

                enemySprites.value = mapOf(
                    "IDLE1" to SpriteBitmapHandler.getBitmap(s[idle1]),
                    "IDLE2" to SpriteBitmapHandler.getBitmap(s[idle2]),
                    "ATK" to SpriteBitmapHandler.getBitmap(s[atkIdx]),
                    "WIN" to SpriteBitmapHandler.getBitmap(s[winIdx]),
                    "LOSE" to SpriteBitmapHandler.getBitmap(s[loseIdx])
                )
            } else if (opponent.remoteSpriteUrls != null) {
                // Download from Firebase
                timber.log.Timber.d("Loading rival sprites from internet for hash ${opponent.dimHash}")
                enemySprites.value = spriteSync.downloadSprites(opponent.remoteSpriteUrls)
            } else {
                // Fallback to my card
                timber.log.Timber.w("Rival card not found and no remote URLs. Falling back to my card: $myCardName")
                myCard?.let {
                    val cardIsBem = it is BemCard
                    val s = it.spriteData.sprites
                    val enemyBase = monsterManager.getIdleSpriteIndices(opponent.characterId, cardIsBem)
                    val idle1 = if (enemyBase[0] < s.size) enemyBase[0] else 0
                    val idle2 = if (enemyBase[1] < s.size) enemyBase[1] else 0
                    val atkIdx = monsterManager.getBattleSpriteIndex(opponent.characterId, cardIsBem).let { if (it < s.size) it else 0 }
                    val winIdx = monsterManager.getWinSpriteIndex(opponent.characterId, cardIsBem).let { if (it < s.size) it else 0 }
                    val loseIdx = monsterManager.getLoseSpriteIndex(opponent.characterId, cardIsBem).let { if (it < s.size) it else 0 }
                    enemyAttackIds.value = monsterManager.getAttackIds(it, opponent.characterId) ?: Pair(0, 0)
                    enemyCritChance = monsterManager.getCritChance(it, opponent.characterId)

                    enemySprites.value = mapOf(
                        "IDLE1" to SpriteBitmapHandler.getBitmap(s[idle1]),
                        "IDLE2" to SpriteBitmapHandler.getBitmap(s[idle2]),
                        "ATK" to SpriteBitmapHandler.getBitmap(s[atkIdx]),
                        "WIN" to SpriteBitmapHandler.getBitmap(s[winIdx]),
                        "LOSE" to SpriteBitmapHandler.getBitmap(s[loseIdx])
                    )
                }
            }

        }

        val myCritChance = monsterManager.getEffectiveCritChance(myCardName, myCharId)

        // VB-style attack cutscene: camera jumps to the attacker, plays the
        // DIM-programmed attack (small = regular, big = critical), impact
        // flash lands with the damage, then the camera pulls back out.
        suspend fun playAttackCutscene(
            attackerIsMe: Boolean,
            isCrit: Boolean,
            applyDamage: () -> Unit,
            logText: String
        ) {
            if (attackerIsMe) myCrit.value = isCrit else enemyCrit.value = isCrit
            cutscene.value = if (attackerIsMe) "me" else "enemy"
            delay(650)
            applyDamage()
            battleLog = logText
            impactFlash.value = true
            delay(280)
            impactFlash.value = false
            cutscene.value = null
            delay(180)
        }

        delay(1500)
        
        // Multi-round Battle Loop
        var round = 1
        while (myCurrentHP > 0 && enemyCurrentHP > 0 && round <= 10) {
            phase.value = BattlePhase.ATTACK
            
            val r1 = battleRandom.nextFloat()
            val r2 = battleRandom.nextInt(20, 50)
            val r3 = battleRandom.nextFloat()
            val r4 = battleRandom.nextInt(20, 50)

            val myRoll = if (opponent.isInitiator) r1 else r3
            val myDmgRoll = if (opponent.isInitiator) r2 else r4
            val enRoll = if (opponent.isInitiator) r3 else r1
            val enDmgRoll = if (opponent.isInitiator) r4 else r2

            if (opponent.isInitiator) {
                // Local player attacks first
                val myDodgeChance = (opponent.spd / 5000f).coerceIn(0.05f, 0.4f)
                if (myRoll > myDodgeChance) {
                    val isCrit = battleRandom.nextFloat() < myCritChance
                    val rawDmg = (((myState?.baseAp ?: 0) + (myState?.attackBonus ?: 0)) / 4 + myDmgRoll).toFloat()
                    // Player crits use the secret practice bonus (1.5x -> 1.75x max); enemy crits stay at 1.5x.
                    val damage = if (isCrit) rawDmg * monsterManager.getEffectiveCritDamageMult() else rawDmg
                    playAttackCutscene(
                        attackerIsMe = true, isCrit = isCrit,
                        applyDamage = { enemyCurrentHP = (enemyCurrentHP - damage).coerceAtLeast(0f) },
                        logText = (if (isCrit) "CRITICAL! " else "") + "HIT! -$damage"
                    )
                } else {
                    battleLog = "DODGED!"
                    delay(500)
                }

                if (enemyCurrentHP <= 0) break
                delay(800)

                // Enemy attacks second
                val enDodgeChance = ((myState?.speedBonus ?: 0) / 5000f).coerceIn(0.05f, 0.4f)
                if (enRoll > enDodgeChance) {
                    val isCrit = battleRandom.nextFloat() < enemyCritChance
                    val rawDmg = (opponent.atk / 4 + enDmgRoll).toFloat()
                    val damage = if (isCrit) rawDmg * 1.5f else rawDmg
                    playAttackCutscene(
                        attackerIsMe = false, isCrit = isCrit,
                        applyDamage = { myCurrentHP = (myCurrentHP - damage).coerceAtLeast(0f) },
                        logText = (if (isCrit) "CRITICAL! " else "") + "ENEMY HIT! -$damage"
                    )
                } else {
                    battleLog = "YOU DODGED!"
                    delay(500)
                }
            } else {
                // Enemy attacks first
                val enDodgeChance = ((myState?.speedBonus ?: 0) / 5000f).coerceIn(0.05f, 0.4f)
                if (enRoll > enDodgeChance) {
                    val isCrit = battleRandom.nextFloat() < enemyCritChance
                    val rawDmg = (opponent.atk / 4 + enDmgRoll).toFloat()
                    val damage = if (isCrit) rawDmg * 1.5f else rawDmg
                    playAttackCutscene(
                        attackerIsMe = false, isCrit = isCrit,
                        applyDamage = { myCurrentHP = (myCurrentHP - damage).coerceAtLeast(0f) },
                        logText = (if (isCrit) "CRITICAL! " else "") + "ENEMY HIT! -$damage"
                    )
                } else {
                    battleLog = "YOU DODGED!"
                    delay(500)
                }

                if (myCurrentHP <= 0) break
                delay(800)

                // Local player attacks second
                val myDodgeChance = (opponent.spd / 5000f).coerceIn(0.05f, 0.4f)
                if (myRoll > myDodgeChance) {
                    val isCrit = battleRandom.nextFloat() < myCritChance
                    val rawDmg = (((myState?.baseAp ?: 0) + (myState?.attackBonus ?: 0)) / 4 + myDmgRoll).toFloat()
                    val damage = if (isCrit) rawDmg * monsterManager.getEffectiveCritDamageMult() else rawDmg
                    playAttackCutscene(
                        attackerIsMe = true, isCrit = isCrit,
                        applyDamage = { enemyCurrentHP = (enemyCurrentHP - damage).coerceAtLeast(0f) },
                        logText = (if (isCrit) "CRITICAL! " else "") + "HIT! -$damage"
                    )
                } else {
                    battleLog = "DODGED!"
                    delay(500)
                }
            }

            round++
            delay(1000)
        }

        val isWin = myCurrentHP > 0
        win.value = isWin
        battleLog = if (isWin) "VICTORY!" else "DEFEAT..."
        phase.value = BattlePhase.RESULT
        
        delay(3000)
        onResult(isWin)
    }

    LaunchedEffect(Unit) {
        while(true) {
            currentFrame.intValue = (currentFrame.intValue + 1) % 2
            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            attackFrame.intValue = (attackFrame.intValue + 1) % 3
            delay(100)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = battleLog,
                color = Color.Yellow, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
            )

            if ((myState?.criticalRemainingMs ?: 0L) > 0L && phase.value != BattlePhase.RESULT) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "⚠ CRITICAL: losing this battle will kill your Digimon!",
                    color = Color.Red, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(40.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                // My Digimon Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HealthBar(myCurrentHP, myMaxHP)
                    Spacer(Modifier.height(8.dp))
                    val myImg = when {
                        isMyAttacking.value -> if (attackFrame.intValue == 0) mySprites.value["ATK"] else mySprites.value["IDLE1"]
                        phase.value == BattlePhase.RESULT -> if (win.value == true) mySprites.value["WIN"] else mySprites.value["LOSE"]
                        phase.value == BattlePhase.ATTACK -> mySprites.value["IDLE1"] // Show idle between turns
                        else -> if (currentFrame.intValue == 0) mySprites.value["IDLE1"] else mySprites.value["IDLE2"]
                    }
                    myImg?.let { Image(it.asImageBitmap(), "Me", Modifier.size(100.dp).offset(x = myOffset.value.dp)) }
                }

                Text(" VS ", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                
                // Rival Digimon Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HealthBar(enemyCurrentHP, enemyMaxHP)
                    Spacer(Modifier.height(8.dp))
                    val enImg = when {
                        isEnemyAttacking.value -> if (attackFrame.intValue == 0) enemySprites.value["ATK"] else enemySprites.value["IDLE1"]
                        phase.value == BattlePhase.RESULT -> if (win.value == false) enemySprites.value["WIN"] else enemySprites.value["LOSE"]
                        phase.value == BattlePhase.ATTACK -> enemySprites.value["IDLE1"]
                        else -> if (currentFrame.intValue == 0) enemySprites.value["IDLE1"] else enemySprites.value["IDLE2"]
                    }
                    enImg?.let { Image(it.asImageBitmap(), "Rival", Modifier.size(100.dp).offset(x = enemyOffset.value.dp)) }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            Text(opponent.name, color = Color.Cyan, fontSize = 18.sp)
        }

        // ---- VB-style attack cutscene: full-screen camera on the attacker ----
        cutscene.value?.let { side ->
            val attackerIsMe = side == "me"
            val sprites = if (attackerIsMe) mySprites.value else enemySprites.value
            val isCritNow = if (attackerIsMe) myCrit.value else enemyCrit.value
            // DIM-programmed attack: small ID for regular hits, big ID for crits
            val ids = if (attackerIsMe) myAttackIds.value else enemyAttackIds.value
            val attackId = if (isCritNow) ids.second else ids.first
            Box(
                Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // The Bracelet's attack effect, full screen
                AttackEffectCanvas(
                    attackId = attackId,
                    progress = effectProgress.value,
                    modifier = Modifier.fillMaxSize()
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isCritNow) {
                        Text(
                            "CRITICAL!", color = Color.Red, fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    // The attacker lunges through its attack pose while the effect plays
                    val poseBmp = if (attackFrame.intValue % 3 == 2) sprites["ATK"] else sprites["IDLE1"]
                    poseBmp?.let { Image(it.asImageBitmap(), "Attacking", Modifier.size(200.dp)) }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (attackerIsMe) "My attack!" else "Rival attack!",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (impactFlash.value) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.35f)))
        }
    }
}

@Composable
fun HealthBar(current: Float, max: Float) {
    val progress = (current / max).coerceIn(0f, 1f)
    val color = when {
        progress > 0.5f -> Color.Green
        progress > 0.2f -> Color.Yellow
        else -> Color.Red
    }
    
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.DarkGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(color)
        )
    }
}
