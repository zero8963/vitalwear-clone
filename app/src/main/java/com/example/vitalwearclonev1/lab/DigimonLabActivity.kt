package com.example.vitalwearclonev1.lab

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.example.vitalwearclonev1.common.communication.ChannelTypes
import com.github.cfogrady.vb.dim.card.BemCard
import com.google.android.gms.wearable.Wearable
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.nio.charset.Charset

data class StoredMonster(
    val name: String,
    val charId: Int,
    val stage: Int,
    val attack: Int,
    val calories: Int, 
    val speed: Int,
    val defense: Int,
    val xp: Int = 0,
    val level: Int = 1,
    val rawPayload: String? = null,
    val nickname: String? = null,
    val currentWins: Int = 0,
    val winsRequired: Int = 0,
    val timeAlive: Long = 0,
    val evolutionTime: Long = 3600,
    val attribute: Int = 0,
    val mood: Int = 0,
    val steps: Int = 0,
    val bp: Int = 0,
    val sp: Int = 0,
    val winRatio: Int = 0,
    val trophies: Int = 0,
    /** Lifetime losses (app-side battle record). Seeded from the bracelet at adoption. */
    val losses: Int = 0
)

object LabStorage {
    private val _monsters = MutableStateFlow<List<StoredMonster>>(emptyList())
    val monsters = _monsters
    private var isLoaded = false

    fun addMonster(context: Context, monster: StoredMonster) {
        if (!isLoaded) load(context)
        val current = _monsters.value.toMutableList()
        current.add(monster)
        _monsters.value = current
        save(context, current)
    }

    fun removeMonster(context: Context, index: Int) {
        val current = _monsters.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _monsters.value = current
            save(context, current)
        }
    }

    private fun save(context: Context, monsters: List<StoredMonster>) {
        val prefs = context.getSharedPreferences("lab_prefs", Context.MODE_PRIVATE)
        val list = monsters.map { 
            "${it.name}|${it.charId}|${it.stage}|${it.attack}|${it.calories}|${it.speed}|${it.defense}|${it.xp}|${it.level}|${it.rawPayload ?: ""}|${it.nickname ?: ""}|${it.currentWins}|${it.winsRequired}|${it.timeAlive}|${it.evolutionTime}|${it.attribute}|${it.mood}|${it.steps}|${it.bp}|${it.sp}|${it.winRatio}|${it.trophies}|${it.losses}" 
        }
        // Use a Set only for the SharedPreferences storage mechanism if required by API, 
        // but ensure uniqueness by appending index if necessary. 
        // Actually, putStringSet is fine with duplicates IF they are different strings.
        // The bug was .toSet() in Kotlin which removes duplicates from the list before saving.
        prefs.edit().putStringSet("monsters", list.toSet()).apply() 
    }

    fun updateMonsterNickname(context: Context, index: Int, nickname: String) {
        val current = _monsters.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(nickname = nickname)
            _monsters.value = current
            save(context, current)
        }
    }

    fun updateMonsterStats(context: Context, index: Int, atk: Int, cals: Int, spd: Int, def: Int, xp: Int = 0, level: Int = 1, raw: String? = null, wins: Int = 0, winsReq: Int = 0, time: Long = 0, evo: Long = 3600) {
        val current = _monsters.value.toMutableList()
        if (index in current.indices) {
            val old = current[index]
            current[index] = old.copy(attack = atk, calories = cals, speed = spd, defense = def, xp = xp, level = level, rawPayload = raw, currentWins = wins, winsRequired = winsReq, timeAlive = time, evolutionTime = evo)
            _monsters.value = current
            save(context, current)
        }
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("lab_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("monsters", emptySet()) ?: emptySet()
        val loaded = mutableListOf<StoredMonster>()
        for (item in set) {
            val parts = item.split("|")
            if (parts.size >= 22) {
                try {
                    loaded.add(StoredMonster(
                        parts[0], 
                        parts[1].toIntOrNull() ?: 0, 
                        parts[2].toIntOrNull() ?: 0, 
                        parts[3].toIntOrNull() ?: 0, 
                        parts[4].toIntOrNull() ?: 0, 
                        parts[5].toIntOrNull() ?: 0, 
                        parts[6].toIntOrNull() ?: 0,
                        parts[7].toIntOrNull() ?: 0,
                        parts[8].toIntOrNull() ?: 1,
                        parts[9].ifEmpty { null },
                        parts[10].ifEmpty { null },
                        parts[11].toIntOrNull() ?: 0,
                        parts[12].toIntOrNull() ?: 0,
                        parts[13].toLongOrNull() ?: 0L,
                        parts[14].toLongOrNull() ?: 3600L,
                        parts[15].toIntOrNull() ?: 0,
                        parts[16].toIntOrNull() ?: 0,
                        parts[17].toIntOrNull() ?: 0,
                        parts[18].toIntOrNull() ?: 0,
                        parts[19].toIntOrNull() ?: 0,
                        parts[20].toIntOrNull() ?: 0,
                        parts[21].toIntOrNull() ?: 0,
                        losses = parts.getOrNull(22)?.toIntOrNull() ?: 0
                    ))
                } catch (e: Exception) {}
            } else if (parts.size >= 15) {
                try {
                    loaded.add(StoredMonster(
                        parts[0], 
                        parts[1].toIntOrNull() ?: 0, 
                        parts[2].toIntOrNull() ?: 0, 
                        parts[3].toIntOrNull() ?: 0, 
                        parts[4].toIntOrNull() ?: 0, 
                        parts[5].toIntOrNull() ?: 0, 
                        parts[6].toIntOrNull() ?: 0,
                        parts[7].toIntOrNull() ?: 0,
                        parts[8].toIntOrNull() ?: 1,
                        parts[9].ifEmpty { null },
                        parts[10].ifEmpty { null },
                        parts[11].toIntOrNull() ?: 0,
                        parts[12].toIntOrNull() ?: 0,
                        parts[13].toLongOrNull() ?: 0L,
                        parts[14].toLongOrNull() ?: 3600L
                    ))
                } catch (e: Exception) {}
            } else if (parts.size >= 12) {
                try {
                    loaded.add(StoredMonster(
                        parts[0], 
                        parts[1].toIntOrNull() ?: 0, 
                        parts[2].toIntOrNull() ?: 0, 
                        parts[3].toIntOrNull() ?: 0, 
                        parts[4].toIntOrNull() ?: 0, 
                        parts[5].toIntOrNull() ?: 0, 
                        parts[6].toIntOrNull() ?: 0,
                        parts[7].toIntOrNull() ?: 0,
                        parts[8].toIntOrNull() ?: 1,
                        parts[9].ifEmpty { null },
                        parts[10].ifEmpty { null }
                    ))
                } catch (e: Exception) {}
            } else if (parts.size >= 10) {
                 try {
                    loaded.add(StoredMonster(
                        parts[0], 
                        parts[1].toIntOrNull() ?: 0, 
                        parts[2].toIntOrNull() ?: 0, 
                        parts[3].toIntOrNull() ?: 0, 
                        parts[4].toIntOrNull() ?: 0, 
                        parts[5].toIntOrNull() ?: 0, 
                        parts[6].toIntOrNull() ?: 0,
                        parts[7].toIntOrNull() ?: 0,
                        parts[8].toIntOrNull() ?: 1,
                        parts[9].ifEmpty { null }
                    ))
                } catch (e: Exception) {}
            } else if (parts.size >= 9) {
                try {
                    loaded.add(StoredMonster(
                        parts[0], 0, 0, 
                        parts[1].toIntOrNull() ?: 0, 
                        parts[2].toIntOrNull() ?: 0, 
                        parts[3].toIntOrNull() ?: 0, 
                        parts[4].toIntOrNull() ?: 0
                    ))
                } catch (e: Exception) {}
            }
        }
        _monsters.value = loaded
        isLoaded = true
    }
}

class DigimonLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LabStorage.load(this)
        setContent {
            LabUI()
        }
    }
}

@Composable
fun LabUI() {
    val monsters by LabStorage.monsters.collectAsState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().background(Color(0, 50, 100)).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Digimon Lab",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                
                Row {
                    val apkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        uri?.let {
                            context.contentResolver.openInputStream(it)?.use { stream ->
                                try {
                                    val secrets = com.example.vitalwearclonev1.communication.secrets.ApkSecretsImporter().importFromApk(stream)
                                    com.example.vitalwearclonev1.communication.secrets.SecretsManager(context).saveSecrets(secrets)
                                    Toast.makeText(context, "Secrets Imported!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Import Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    Button(onClick = { apkLauncher.launch("application/vnd.android.package-archive") }, 
                        colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
                        modifier = Modifier.padding(end = 8.dp)) {
                        Text("Import APK", color = Color.White, fontSize = 10.sp)
                    }

                    Button(onClick = {
                        val intent = Intent(context, com.example.vitalwearclonev1.communication.VBSyncActivity::class.java)
                        context.startActivity(intent)
                    }, colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = Color.Cyan)) {
                        Text("Receive", color = Color.Black)
                    }

                    Button(onClick = {
                        val intent = Intent(context, com.example.vitalwearclonev1.communication.VBBraceletBackupsActivity::class.java)
                        context.startActivity(intent)
                    }, colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6A4C93)),
                        modifier = Modifier.padding(end = 8.dp)) {
                        Text("Bracelet", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
            // Bracelet character backups - full-width entry (the header
            // "Bracelet" chip is easy to miss; this is the canonical way in).
            run {
                val backups = com.example.vitalwearclonev1.communication.VBBraceletBackups
                    .list(context)
                Button(
                    onClick = {
                        val intent = Intent(context, com.example.vitalwearclonev1.communication.VBBraceletBackupsActivity::class.java)
                        context.startActivity(intent)
                    },
                    colors = androidx.compose.material.ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF6A4C93)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(52.dp)
                ) {
                    Text(
                        "Bracelet Backups" +
                            if (backups.isEmpty()) "" else " (${backups.size})",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            if (monsters.isEmpty()) {
                Text(text = "No monsters stored in the Lab yet.", color = Color.LightGray)
            } else {
                LazyColumn {
                    itemsIndexed(monsters) { index, monster ->
                        MonsterCard(monster, index, onRestore = {
                            scope.launch {
                                sendToWatch(context, monster)
                            }
                        }, onAdventure = {
                            val intent = Intent(context, MapAdventureActivity::class.java).apply {
                                putExtra("monsterIndex", index)
                                putExtra("cardName", monster.name)
                                putExtra("charId", monster.charId)
                                putExtra("atk", monster.attack)
                                putExtra("cals", monster.calories)
                                putExtra("spd", monster.speed)
                                putExtra("def", monster.defense)
                                putExtra("xp", monster.xp)
                                putExtra("level", monster.level)
                                putExtra("raw", monster.rawPayload)
                                putExtra("nickname", monster.nickname)
                                putExtra("wins", monster.currentWins)
                                putExtra("winsReq", monster.winsRequired)
                                putExtra("timeAlive", monster.timeAlive)
                                putExtra("evoTime", monster.evolutionTime)
                                putExtra("attribute", monster.attribute)
                                putExtra("mood", monster.mood)
                                putExtra("steps", monster.steps)
                                putExtra("bp", monster.bp)
                                putExtra("sp", monster.sp)
                                putExtra("winRatio", monster.winRatio)
                                putExtra("trophies", monster.trophies)
                            }
                            context.startActivity(intent)
                        }, onSyncToVB = {
                            Timber.d("Sync to VB clicked for ${monster.name} index=$index")
                            val intent = Intent(context, com.example.vitalwearclonev1.communication.VBSyncActivity::class.java).apply {
                                putExtra("name", monster.name)
                                putExtra("charId", monster.charId)
                                putExtra("stage", monster.stage)
                                putExtra("atk", monster.attack)
                                putExtra("cals", monster.calories)
                                putExtra("spd", monster.speed)
                                putExtra("def", monster.defense)
                                putExtra("raw", monster.rawPayload)
                                putExtra("wins", monster.currentWins)
                                putExtra("winsReq", monster.winsRequired)
                                putExtra("timeAlive", monster.timeAlive)
                                putExtra("evoTime", monster.evolutionTime)
                                putExtra("attribute", monster.attribute)
                                putExtra("mood", monster.mood)
                                putExtra("steps", monster.steps)
                                putExtra("bp", monster.bp)
                                putExtra("sp", monster.sp)
                                putExtra("winRatio", monster.winRatio)
                                putExtra("trophies", monster.trophies)
                            }
                            context.startActivity(intent)
                        }, onSetHome = {
                            val monsterManager = com.example.vitalwearclonev1.monster.PhoneMonsterManager(context)
                            val cardManager = com.example.vitalwearclonev1.card.CardManager(context)
                            val isBem = cardManager.getCard(monster.name) is BemCard
                            monsterManager.setCurrentMonster(
                                monster.name, 
                                monster.charId, 
                                monster.stage,
                                atk = monster.attack,
                                hp = monster.calories, 
                                spd = monster.speed,
                                def = monster.defense,
                                xp = monster.xp,
                                level = monster.level,
                                raw = monster.rawPayload,
                                nickname = monster.nickname,
                                wins = monster.currentWins,
                                winsReq = monster.winsRequired,
                                isBem = isBem,
                                timeAlive = monster.timeAlive,
                                evolutionTime = monster.evolutionTime,
                                attribute = monster.attribute,
                                mood = monster.mood,
                                steps = monster.steps,
                                bp = monster.bp,
                                sp = monster.sp,
                                winRatio = monster.winRatio,
                                trophies = monster.trophies
                            )
                            LabStorage.removeMonster(context, index)
                            Toast.makeText(context, "${monster.nickname ?: monster.name} moved to Home!", Toast.LENGTH_SHORT).show()
                            (context as? ComponentActivity)?.finish()
                        }, onRelease = {
                            val releasedName = monster.nickname ?: monster.name
                            LabStorage.removeMonster(context, index)
                            Toast.makeText(context, "$releasedName was released.", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun MonsterCard(monster: StoredMonster, index: Int, onRestore: () -> Unit, onAdventure: () -> Unit, onSyncToVB: () -> Unit, onSetHome: () -> Unit, onRelease: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val cardManager = remember { CardManager(context) }
    
    var showMatchDialog by remember { mutableStateOf(false) }
    var showSetHomeConfirm by remember { mutableStateOf(false) }
    var showReleaseConfirm by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var nicknameText by remember { mutableStateOf(monster.nickname ?: "") }

    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Set Nickname") },
            text = {
                TextField(value = nicknameText, onValueChange = { nicknameText = it }, label = { Text("Nickname") })
            },
            confirmButton = {
                Button(onClick = {
                    LabStorage.updateMonsterNickname(context, index, nicknameText)
                    showNicknameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = { showNicknameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSetHomeConfirm) {
        AlertDialog(
            onDismissRequest = { showSetHomeConfirm = false },
            title = { Text("Set as Home Monster?") },
            text = { Text("This will move ${monster.nickname ?: monster.name} from the Lab to your Home screen. It will be removed from the Lab list.") },
            confirmButton = {
                Button(onClick = {
                    showSetHomeConfirm = false
                    onSetHome()
                }) { Text("Confirm") }
            },
            dismissButton = {
                Button(onClick = { showSetHomeConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showReleaseConfirm) {
        AlertDialog(
            onDismissRequest = { showReleaseConfirm = false },
            title = { Text("Release Digimon?") },
            text = { Text("This will permanently release ${monster.nickname ?: monster.name} from the Lab. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showReleaseConfirm = false
                        onRelease()
                    },
                    colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White)
                ) { Text("Release") }
            },
            dismissButton = {
                Button(onClick = { showReleaseConfirm = false }) { Text("Cancel") }
            }
        )
    }
    val adoptedBackupId = remember(monster.rawPayload) {
        com.example.vitalwearclonev1.communication.VBBraceletSyncBack.sourceBackupId(monster)
    }
    if (showSyncDialog && adoptedBackupId != null) {
        val preview = remember(monster) {
            com.example.vitalwearclonev1.communication.VBBraceletSyncBack.preview(monster)
        }
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync training to Bracelet?") },
            text = {
                Column {
                    Text(
                        "This patches ${monster.nickname ?: monster.name}'s training into a NEW " +
                            "bracelet backup (the original backup is kept):"
                    )
                    Text("• Mental ← mood ${preview.mental}", fontSize = 13.sp)
                    Text("• Wins ← ${preview.wins}", fontSize = 13.sp)
                    Text("• Losses ← ${preview.losses}", fontSize = 13.sp)
                    Text("• Win rate ← ${preview.winRate}% (recomputed)", fontSize = 13.sp)
                    Text(
                        "Training stat bonuses can't be mapped to bracelet bytes yet — " +
                            "they stay in the app only.",
                        fontSize = 12.sp, color = Color.Yellow
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSyncDialog = false
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                com.example.vitalwearclonev1.communication.VBBraceletSyncBack
                                    .syncToNewBackup(context, monster)
                            }
                            val changed = if (result.changes.isEmpty()) "no values changed"
                            else result.changes.joinToString("; ")
                            Toast.makeText(context, "Synced backup saved: $changed", Toast.LENGTH_LONG).show()
                            val intent = Intent(
                                context,
                                com.example.vitalwearclonev1.communication.VBBraceletCharacterActivity::class.java
                            ).apply {
                                putExtra(
                                    com.example.vitalwearclonev1.communication.VBBraceletCharacterActivity.EXTRA_BACKUP_ID,
                                    result.newBackup.id
                                )
                                putExtra(
                                    com.example.vitalwearclonev1.communication.VBBraceletCharacterActivity.EXTRA_ARM_WRITE,
                                    true
                                )
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "bracelet sync failed")
                            Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Sync & Write…") }
            },
            dismissButton = {
                Button(onClick = { showSyncDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showMatchDialog) {
        val cards = remember { cardManager.listCards() }
        AlertDialog(
            onDismissRequest = { showMatchDialog = false },
            title = { Text("Match with DIM Card") },
            text = {
                Column {
                    Text("This character was received with an unknown ID. Pick the DIM it belongs to:")
                    LazyColumn(Modifier.height(200.dp)) {
                        items(cards.size) { i ->
                            Text(cards[i], modifier = Modifier.fillMaxWidth().clickable {
                                val current = LabStorage.monsters.value.toMutableList()
                                current[index] = current[index].copy(name = cards[i])
                                val prefs = context.getSharedPreferences("lab_prefs", Context.MODE_PRIVATE)
                                val set = current.map { 
                                    "${it.name}|${it.charId}|${it.stage}|${it.attack}|${it.calories}|${it.speed}|${it.defense}|${it.xp}|${it.level}|${it.rawPayload ?: ""}|${it.nickname ?: ""}|${it.currentWins}|${it.winsRequired}|${it.timeAlive}|${it.evolutionTime}|${it.attribute}|${it.mood}|${it.steps}|${it.bp}|${it.sp}|${it.winRatio}|${it.trophies}|${it.losses}" 
                                }.toSet()
                                prefs.edit().putStringSet("monsters", set).apply()
                                LabStorage.load(context)
                                showMatchDialog = false
                            }.padding(8.dp))
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showMatchDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSetHomeConfirm) {
        AlertDialog(
            onDismissRequest = { showSetHomeConfirm = false },
            title = { Text("Set as Home Monster?") },
            text = { Text("This will move ${monster.nickname ?: monster.name} from the Lab to your Home screen. It will be removed from the Lab list.") },
            confirmButton = {
                Button(onClick = {
                    showSetHomeConfirm = false
                    onSetHome()
                }) { Text("Confirm") }
            },
            dismissButton = {
                Button(onClick = { showSetHomeConfirm = false }) { Text("Cancel") }
            }
        )
    }

    val idleSprites = remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val currentFrame = remember { mutableIntStateOf(0) }
    val isInteracting = remember { mutableStateOf(false) }
    val isCardMissing = remember { mutableStateOf(false) }

    val filePickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri).use { inputStream ->
                            val card = try {
                                com.github.cfogrady.vb.dim.card.DimReader().readCard(inputStream!!, false)
                            } catch (e: Exception) {
                                context.contentResolver.openInputStream(uri).use { is2 ->
                                    com.github.cfogrady.vb.dim.card.DimReader().readCard(is2!!, true)
                                }
                            }
                            cardManager.saveCard(monster.name, card)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to load DIM: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                isCardMissing.value = false
            }
        }
    }

    LaunchedEffect(monster.name, monster.charId, isCardMissing.value) {
        withContext(Dispatchers.IO) {
            val card = cardManager.getCard(monster.name)
            if (card != null) {
                isCardMissing.value = false
                val isBem = card is BemCard
                val sprites = card.spriteData.sprites
                val baseIdx = getCharacterBaseIndex(monster.charId, isBem)
                
                val frames = mutableListOf<Bitmap>()
                for (i in 1..2) {
                    if (baseIdx + i < sprites.size) {
                        SpriteBitmapHandler.getBitmap(sprites[baseIdx + i])?.let { frames.add(it) }
                    }
                }
                idleSprites.value = frames
            } else {
                isCardMissing.value = true
                idleSprites.value = emptyList()
            }
        }
    }

    LaunchedEffect(idleSprites.value, isInteracting.value) {
        if (idleSprites.value.size > 1) {
            while (true) {
                currentFrame.intValue = (currentFrame.intValue + 1) % idleSprites.value.size
                delay(if (isInteracting.value) 200 else 500)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = 4.dp,
        backgroundColor = Color(0, 80, 150)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { isInteracting.value = !isInteracting.value }, 
                contentAlignment = Alignment.Center
            ) {
                if (idleSprites.value.isNotEmpty()) {
                    Image(
                        idleSprites.value[currentFrame.intValue % idleSprites.value.size].asImageBitmap(), 
                        "Monster", 
                        modifier = Modifier.size(if (isInteracting.value) 56.dp else 48.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("?", color = Color.White)
                        if (isCardMissing.value) {
                            Text("No DIM", color = Color.Red, fontSize = 8.sp)
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(text = monster.nickname ?: monster.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Green,
                    modifier = Modifier.clickable { showNicknameDialog = true })
                if (monster.nickname != null) {
                    Text(text = "(${monster.name})", fontSize = 12.sp, color = Color.LightGray)
                }
                if (monster.name.startsWith("DIM ")) {
                    Text("Unknown DIM Source", color = Color.Yellow, fontSize = 10.sp)
                    Button(onClick = { showMatchDialog = true }, modifier = Modifier.height(24.dp).padding(top = 4.dp)) {
                        Text("Match DIM", fontSize = 8.sp)
                    }
                }
                Text(text = "Wins: ${monster.currentWins}", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "ATK: ${monster.attack} | Cals: ${monster.calories}", color = Color.White, fontSize = 12.sp)
                Text(text = "SPD: ${monster.speed} | DEF: ${monster.defense}", color = Color.White, fontSize = 12.sp)
                if (isCardMissing.value) {
                    Text(text = "Please Link DIM file", color = Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column {
                Button(onClick = onRestore, modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(text = "Restore", fontSize = 10.sp)
                }
                if (isCardMissing.value) {
                    Button(onClick = { filePickLauncher.launch(arrayOf("*/*")) }, colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = Color(150, 80, 0))) {
                        Text(text = "Link DIM", fontSize = 10.sp, color = Color.White)
                    }
                } else {
                    Button(onClick = onAdventure) {
                        Text(text = "Adventure", fontSize = 10.sp)
                    }
                }
                Button(onClick = onSyncToVB, modifier = Modifier.padding(top = 4.dp), colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = Color.Cyan)) {
                    Text(text = "Sync to VB", fontSize = 10.sp, color = Color.Black)
                }
                if (adoptedBackupId != null) {
                    Button(
                        onClick = { showSyncDialog = true },
                        modifier = Modifier.padding(top = 4.dp),
                        colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6A4C93))
                    ) {
                        Text(text = "Sync Training", fontSize = 10.sp, color = Color.White)
                    }
                }
                Button(onClick = { showSetHomeConfirm = true }, modifier = Modifier.padding(top = 4.dp), colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 80))) {
                    Text(text = "Set Home", fontSize = 10.sp, color = Color.White)
                }
                Button(onClick = { showReleaseConfirm = true }, modifier = Modifier.padding(top = 4.dp), colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = Color(150, 40, 40))) {
                    Text(text = "Release", fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }
}

private fun getCharacterBaseIndex(characterId: Int, isBem: Boolean): Int {
    if (isBem) {
        return 54 + (characterId * 14)
    } else {
        var currentIdx = 10
        for (i in 0 until characterId) {
            currentIdx += when(i) {
                0 -> 6
                1 -> 7
                else -> 14
            }
        }
        return currentIdx
    }
}

suspend fun sendToWatch(context: Context, monster: StoredMonster) {
    withContext(Dispatchers.IO) {
        try {
            val channelClient = Wearable.getChannelClient(context)
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Watch not connected!", Toast.LENGTH_SHORT).show() }
                return@withContext
            }

            for (node in nodes) {
                Timber.d("Restoring to ${node.displayName}: ${monster.name} ID:${monster.charId} ATK:${monster.attack}")
                val channel = channelClient.openChannel(node.id, ChannelTypes.CHARACTER_DATA).await()
                channelClient.getOutputStream(channel).await().use { os ->
                    val output = DataOutputStream(os.buffered())
                    output.write(monster.name.toByteArray(Charset.defaultCharset()))
                    output.writeByte(0)
                    output.writeInt(monster.charId)
                    output.writeInt(monster.stage)
                    output.writeInt(monster.attack)
                    output.writeInt(monster.calories) 
                    output.writeInt(monster.speed)
                    output.writeInt(monster.defense)
                    output.writeInt(monster.currentWins)
                    output.writeInt(monster.winsRequired)
                    output.writeLong(monster.timeAlive)
                    output.writeLong(monster.evolutionTime)
                    output.writeInt(monster.attribute)
                    output.writeInt(monster.mood)
                    output.writeInt(monster.steps)
                    output.writeInt(monster.bp)
                    output.writeInt(monster.sp)
                    output.writeInt(monster.winRatio)
                    output.writeInt(monster.trophies)
                    output.flush()
                }
                channelClient.close(channel).await()
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, "Restored to Watch!", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            Timber.e(e, "Restore failed")
            withContext(Dispatchers.Main) { Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show() }
        }
    }
}
