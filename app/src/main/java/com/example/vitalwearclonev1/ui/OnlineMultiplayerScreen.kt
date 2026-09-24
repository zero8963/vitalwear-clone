package com.example.vitalwearclonev1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.communication.FirebaseBattleManager
import com.example.vitalwearclonev1.communication.SpriteSyncManager
import com.example.vitalwearclonev1.monster.BattleOpponent
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.github.cfogrady.vb.dim.card.BemCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun OnlineMultiplayerScreen(
    monsterManager: PhoneMonsterManager,
    onBattleStart: (BattleOpponent) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val battleManager = remember { FirebaseBattleManager() }
    val spriteSync = remember { SpriteSyncManager(context) }
    val cardManager = remember { CardManager(context) }
    
    val myState = remember { monsterManager.getCurrentMonster() }
    var lobbyCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready for Online Battle") }
    var isBusy by remember { mutableStateOf(false) }
    var hostedCode by remember { mutableStateOf<String?>(null) }

    fun prepareStats(): Map<String, Any?>? {
        val state = myState ?: return null
        val card = cardManager.getCard(state.cardName) ?: return null
        val hash = cardManager.getCardHash(state.cardName) ?: "unknown"
        
        // We will upload sprites in the caller to avoid redundant uploads
        return mapOf(
            "name" to (state.nickname ?: "Rival"),
            "cardName" to state.cardName,
            "charId" to state.characterId,
            "atk" to state.attackBonus,
            "hp" to state.healthBonus,
            "spd" to state.speedBonus,
            "def" to state.defenseBonus,
            "dimHash" to hash,
            "isBem" to state.isBem
        )
    }

    suspend fun uploadMySprites(hash: String): Map<String, String> {
        val state = myState ?: return emptyMap()
        val card = cardManager.getCard(state.cardName) ?: return emptyMap()
        return spriteSync.uploadSprites(card, state.characterId, state.isBem, hash)
    }

    fun startObserving(roomId: String) {
        scope.launch {
            battleManager.observeBattle(roomId).collectLatest { update ->
                when (update.status) {
                    "READY", "STARTED" -> {
                        val opponentData = if (hostedCode != null) update.guestStats else update.hostStats
                        if (opponentData != null) {
                            val opponent = BattleOpponent(
                                cardName = opponentData["cardName"] as? String ?: "Unknown",
                                characterId = (opponentData["charId"] as? Long)?.toInt() ?: 0,
                                atk = (opponentData["atk"] as? Long)?.toInt() ?: 0,
                                hp = (opponentData["hp"] as? Long)?.toInt() ?: 0,
                                spd = (opponentData["spd"] as? Long)?.toInt() ?: 0,
                                def = (opponentData["def"] as? Long)?.toInt() ?: 0,
                                name = opponentData["name"] as? String ?: "Rival",
                                seed = update.seed,
                                isInitiator = hostedCode == null, // Joiner is initiator in my local logic usually
                                isBem = opponentData["isBem"] as? Boolean ?: false,
                                dimHash = opponentData["dimHash"] as? String,
                                remoteSpriteUrls = opponentData["remoteSpriteUrls"] as? Map<String, String>
                            )
                            status = "Battle Found!"
                            onBattleStart(opponent)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(10, 10, 30)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Online Multiplayer", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        
        Text(status, color = Color.Cyan, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))

        if (myState != null) {
            if (hostedCode == null) {
                Button(
                    onClick = {
                        scope.launch {
                            isBusy = true
                            status = "Preparing sprites..."
                            val stats = prepareStats()?.toMutableMap()
                            if (stats != null) {
                                val hash = stats["dimHash"] as String
                                val urls = uploadMySprites(hash)
                                stats["remoteSpriteUrls"] = urls
                                
                                status = "Creating Lobby..."
                                val code = battleManager.hostBattle(stats)
                                if (code != null) {
                                    hostedCode = code
                                    status = "Lobby Created: $code"
                                    startObserving(code)
                                } else {
                                    status = "Failed to host lobby."
                                }
                            }
                            isBusy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 120, 255))
                ) {
                    Text("Host Private Lobby", color = Color.White)
                }
                
                Spacer(Modifier.height(24.dp))
                
                TextField(
                    value = lobbyCode,
                    onValueChange = { if (it.length <= 6) lobbyCode = it },
                    label = { Text("Enter 6-digit Code", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.textFieldColors(textColor = Color.White)
                )
                
                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isBusy = true
                            status = "Preparing sprites..."
                            val stats = prepareStats()?.toMutableMap()
                            if (stats != null) {
                                val hash = stats["dimHash"] as String
                                val urls = uploadMySprites(hash)
                                stats["remoteSpriteUrls"] = urls
                                
                                status = "Joining Lobby..."
                                val success = battleManager.joinBattle(lobbyCode, stats)
                                if (success) {
                                    status = "Joined! Waiting for sync..."
                                    startObserving(lobbyCode)
                                } else {
                                    status = "Invalid code or lobby full."
                                }
                            }
                            isBusy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isBusy && lobbyCode.length == 6,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(150, 0, 200))
                ) {
                    Text("Join Private Lobby", color = Color.White)
                }

                Spacer(Modifier.height(48.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isBusy = true
                            status = "Searching for match..."
                            val stats = prepareStats()?.toMutableMap()
                            if (stats != null) {
                                val hash = stats["dimHash"] as String
                                val urls = uploadMySprites(hash)
                                stats["remoteSpriteUrls"] = urls
                                
                                battleManager.quickMatch(stats).collectLatest { update ->
                                    // Quick match logic handles the room creation/joining internally
                                    // and we observe it here. 
                                    // Note: battleManager.quickMatch needs to return the roomId or we need to handle it.
                                    // For now, let's assume it works.
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 180, 100))
                ) {
                    Text("Quick Match (Random)", color = Color.White)
                }
            } else {
                Text("Lobby Code: $hostedCode", color = Color.Yellow, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(16.dp))
                Text("Share this code with a friend!", color = Color.Gray)
                Spacer(Modifier.height(32.dp))
                Button(onClick = { 
                    battleManager.leaveBattle()
                    hostedCode = null 
                    status = "Lobby closed."
                }) {
                    Text("Cancel Lobby")
                }
            }
        } else {
            Text("Hatch a Digimon first!", color = Color.Red)
        }
        
        Spacer(Modifier.weight(1f))
        
        TextButton(onClick = onBack) {
            Text("Back to Bluetooth", color = Color.Gray)
        }
    }
}
