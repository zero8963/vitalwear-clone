package com.example.vitalwearclonev1.communication

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.lab.StoredMonster
import kotlinx.coroutines.launch
import timber.log.Timber

class VBSyncActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    enum class SyncState {
        INITIAL,
        WAITING_FOR_DIM,
        SYNCING,
        COMPLETE
    }

    private var nfcAdapter: NfcAdapter? = null
    private var monsterToSync: StoredMonster? = null
    private var isReceiveMode = false
    private val syncStatus = mutableStateOf("Ready to Sync")
    private val currentSyncState = mutableStateOf(SyncState.INITIAL)
    private val syncLog = mutableStateListOf<String>()
    private val isSyncing = mutableStateOf(false)

    private fun updateStatus(msg: String) {
        syncStatus.value = msg
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        syncLog.add(0, "[$timestamp] $msg")
        if (syncLog.size > 10) syncLog.removeLast()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        // Extract monster from intent
        Timber.d("VBSyncActivity onCreate: Intent extras=${intent.extras?.keySet()?.joinToString()}")
        monsterToSync = if (intent.hasExtra("name")) {
            val m = StoredMonster(
                intent.getStringExtra("name") ?: "Unknown",
                intent.getIntExtra("charId", 0),
                intent.getIntExtra("stage", 0),
                intent.getIntExtra("atk", 0),
                intent.getIntExtra("cals", 0),
                intent.getIntExtra("spd", 0),
                intent.getIntExtra("def", 0),
                xp = intent.getIntExtra("xp", 0),
                level = intent.getIntExtra("level", 1),
                rawPayload = intent.getStringExtra("raw"),
                nickname = intent.getStringExtra("nickname"),
                currentWins = intent.getIntExtra("wins", 0),
                winsRequired = intent.getIntExtra("winsReq", 10),
                timeAlive = intent.getLongExtra("timeAlive", 0L),
                evolutionTime = intent.getLongExtra("evoTime", 3600L),
                attribute = intent.getIntExtra("attribute", 0),
                mood = intent.getIntExtra("mood", 0),
                steps = intent.getIntExtra("steps", 0),
                bp = intent.getIntExtra("bp", 0),
                sp = intent.getIntExtra("sp", 0),
                winRatio = intent.getIntExtra("winRatio", 0),
                trophies = intent.getIntExtra("trophies", 0)
            )
            Timber.d("VBSyncActivity: Loaded monsterToSync=${m.name} ID=${m.charId}")
            m
        } else {
            Timber.d("VBSyncActivity: No monster name in intent, entering RECEIVE mode")
            isReceiveMode = true
            null
        }

        if (isReceiveMode) {
            updateStatus("Tap Watch to Receive")
        } else {
            updateStatus("Tap 1: Link Bracelet")
        }

        setContent {
            SyncUI()
        }
    }

    override fun onResume() {
        super.onResume()
        val options = Bundle()
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        nfcAdapter?.enableReaderMode(this, this, 
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, options)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null || isSyncing.value) return
        
        if (!isReceiveMode && monsterToSync == null) return

        val protocol = VBNfcProtocol(tag, this)

        if (isReceiveMode) {
            isSyncing.value = true
            handleReceive(protocol)
        } else {
            handleSinglePassSend(protocol)
        }
    }

    private fun handleSinglePassSend(protocol: VBNfcProtocol) {
        isSyncing.value = true
        
        val cardManager = com.example.vitalwearclonev1.card.CardManager(this)
        val card = cardManager.getCard(monsterToSync!!.name)
        val realId = card?.header?.dimId ?: 0

        val (success, phase) = protocol.performSyncStep(
            isInitialTap = currentSyncState.value == SyncState.INITIAL,
            monster = monsterToSync!!,
            realCardId = realId,
            onProgress = { status ->
                runOnUiThread { updateStatus(status) }
            }
        )

        runOnUiThread {
            isSyncing.value = false
            if (success) {
                if (phase == VBNfcProtocol.SyncPhase.HANDSHAKE) {
                    currentSyncState.value = SyncState.WAITING_FOR_DIM
                    updateStatus("LINK OK. REMOVE PHONE!")
                    updateStatus("NOW: Insert DIM into Bracelet.")
                } else if (phase == VBNfcProtocol.SyncPhase.TRANSFER) {
                    currentSyncState.value = SyncState.COMPLETE
                    Toast.makeText(this, "Sync Successful!", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                if (currentSyncState.value == SyncState.INITIAL) {
                    updateStatus("Tap 1 Failed. Retry.")
                } else {
                    updateStatus("Tap 2 Failed. Retry.")
                }
            }
        }
    }

    private fun handleReceive(protocol: VBNfcProtocol) {
        val result = protocol.executeReceive { status ->
            runOnUiThread { updateStatus(status) }
        }

        runOnUiThread {
            if (result != null) {
                com.example.vitalwearclonev1.lab.LabStorage.addMonster(this, result)
                Toast.makeText(this, "Received ${result.name}!", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Receive Failed. Try Again.", Toast.LENGTH_SHORT).show()
                isSyncing.value = false
                syncStatus.value = "Tap Watch to Retry"
            }
        }
    }

    @Composable
    fun SyncUI() {
        MaterialTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0, 40, 80)).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val title = if (isReceiveMode) "Receive from Watch" else "Vital Hero Sync"
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                
                if (!isReceiveMode) {
                    Text(
                        "WATCH MUST BE IN 'APP -> RECEIVE' MENU",
                        color = Color.Yellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Phase indicator
                if (!isReceiveMode) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        PhaseDot("1", currentSyncState.value != SyncState.INITIAL)
                        Spacer(Modifier.width(16.dp))
                        PhaseDot("DIM", currentSyncState.value == SyncState.WAITING_FOR_DIM || currentSyncState.value == SyncState.SYNCING)
                        Spacer(Modifier.width(16.dp))
                        PhaseDot("2", currentSyncState.value == SyncState.SYNCING || currentSyncState.value == SyncState.COMPLETE)
                    }
                }
                
                Spacer(Modifier.height(40.dp))
                
                Box(
                    modifier = Modifier.size(200.dp).background(Color.Black.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing.value) {
                        CircularProgressIndicator(color = Color.Cyan)
                    } else {
                        val tapText = when(currentSyncState.value) {
                            SyncState.INITIAL -> "TAP 1"
                            SyncState.WAITING_FOR_DIM -> "INSERT DIM"
                            SyncState.SYNCING -> "TAP 2"
                            else -> "DONE"
                        }
                        Text(tapText, color = Color.Cyan, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.height(40.dp))
                Text(syncStatus.value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                
                // Scrollable Sync Log
                Box(modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(120.dp).background(Color.Black.copy(alpha = 0.3f)).padding(8.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(syncLog.size) { index ->
                            Text(syncLog[index], color = Color.LightGray, fontSize = 10.sp)
                        }
                    }
                }

                if (currentSyncState.value == SyncState.WAITING_FOR_DIM) {
                    Button(onClick = { 
                        currentSyncState.value = SyncState.INITIAL 
                        updateStatus("Reset to Tap 1")
                    }, modifier = Modifier.padding(top = 8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) {
                        Text("Stuck? Retry Tap 1", color = Color.White, fontSize = 10.sp)
                    }

                    Text(
                        "After inserting DIM, wait for watch animation, then tap again.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (!isSyncing.value) {
                    Button(onClick = { finish() }, modifier = Modifier.padding(top = 40.dp)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    @Composable
    fun PhaseDot(label: String, active: Boolean) {
        Box(
            modifier = Modifier.size(40.dp).background(if (active) Color.Green else Color.Gray, shape = androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
