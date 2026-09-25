package com.example.vitalwearclonev1.gridbattle

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.vitalwearclonev1.monster.PhoneMonsterManager

/**
 * Phone-to-phone PvP lobby (2026-09-25).
 *
 * No accounts, no servers: one phone hosts, the other joins from the
 * discovered list. Both phones link directly — no internet needed.
 * Bluetooth, WiFi, and Location must all be switched on (Location is
 * what lets the phones hear each other's discovery beacons).
 */
@Composable
fun PvpLobbyScreen(
    ownerId: String,
    monster: PhoneMonsterManager.MonsterState,
    net: PvpNetManager,
    onHostBattle: (BattleSetup, PvpFighterInfo) -> Unit,
    onGuestBattle: (host: PvpFighterInfo, me: PvpFighterInfo) -> Unit
) {
    val context = LocalContext.current
    val me = remember(ownerId) { buildLocalFighterInfo(context, ownerId, monster) }

    // choose | hosting | discovering | connecting | waiting
    var phase by remember { mutableStateOf("choose") }
    var found by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var status by remember { mutableStateOf("") }
    var goingToBattle by remember { mutableStateOf(false) }

    // Phone-to-phone needs these three radios actually switched on — the
    // #1 reason two phones can't see each other is Location turned off.
    data class RadioIssue(val label: String, val settingsAction: String)

    fun checkRadios(): List<RadioIssue> {
        val issues = mutableListOf<RadioIssue>()
        val bt = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (bt == null || !bt.isEnabled) {
            issues += RadioIssue("Bluetooth is off", Settings.ACTION_BLUETOOTH_SETTINGS)
        }
        @Suppress("DEPRECATION")
        val wifiOn = context.applicationContext
            .getSystemService(WifiManager::class.java)?.isWifiEnabled == true
        if (!wifiOn) {
            issues += RadioIssue("WiFi is off", Settings.ACTION_WIFI_SETTINGS)
        }
        val loc = context.getSystemService(LocationManager::class.java)
        val locOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            loc?.isLocationEnabled == true
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            ) != Settings.Secure.LOCATION_MODE_OFF
        }
        if (!locOn) {
            issues += RadioIssue(
                "Location is off (needed to find nearby phones)",
                Settings.ACTION_LOCATION_SOURCE_SETTINGS
            )
        }
        return issues
    }

    var radios by remember { mutableStateOf(listOf<RadioIssue>()) }
    fun openSettings(action: String) {
        try {
            context.startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) { }
    }
    // Re-check every time we come back to the picker (e.g. from Settings).
    LaunchedEffect(phase) {
        if (phase == "choose") radios = checkRadios()
    }

    val neededPerms = remember {
        mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()
    }
    var permsOk by remember { mutableStateOf(false) }
    var afterPerms by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        permsOk = grants.values.all { it }
        if (permsOk) afterPerms?.invoke()
        afterPerms = null
    }
    LaunchedEffect(Unit) {
        permsOk = neededPerms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun withPerms(action: () -> Unit) {
        val go = {
            radios = checkRadios()
            if (radios.isNotEmpty()) {
                status = "Turn on: " + radios.joinToString(", ") { it.label.substringBefore(" (") }
                phase = "choose"
            } else {
                status = ""
                action()
            }
        }
        if (permsOk) go()
        else {
            afterPerms = go
            permLauncher.launch(neededPerms)
        }
    }

    fun cancel() {
        net.listener = null
        net.stopAll()
        phase = "choose"
        found = emptyList()
        status = ""
    }

    fun startHosting() {
        val my = me ?: return
        phase = "hosting"
        status = "Waiting for a challenger..."
        net.listener = object : PvpNetManager.Listener {
            override fun onMessage(fromEndpoint: String, msg: String) {
                if (!msg.startsWith("HELLO|")) return
                try {
                    val guest = parseFighterInfo(msg)
                    // Reply BEFORE tearing anything down — the guest is stuck
                    // on "Waiting for the host..." until this arrives.
                    net.send(my.toWelcomeWire())
                    net.stopAdvertising()
                    goingToBattle = true
                    net.listener = null
                    val setup = BattleSetup(
                        ownerId = ownerId,
                        playerCardName = my.cardName,
                        playerCharId = my.charId,
                        displayName = my.name,
                        config = my.hostConfig(guest),
                        enemyCardName = guest.cardName,
                        enemyCharId = guest.charId
                    )
                    onHostBattle(setup, guest)
                } catch (e: Exception) {
                    status = "Challenger sent bad data — still waiting..."
                }
            }

            override fun onEndpointFound(endpointId: String, name: String) {}
            override fun onEndpointLost(endpointId: String) {}
            override fun onConnected(endpointId: String) {}
            override fun onDisconnected(endpointId: String) {}
            override fun onRadioError(msg: String) { status = msg }
        }
        net.startAdvertising(my.name)
    }

    fun startDiscovery() {
        phase = "discovering"
        found = emptyList()
        status = ""
        net.listener = object : PvpNetManager.Listener {
            override fun onMessage(fromEndpoint: String, msg: String) {
                if (!msg.startsWith("WELCOME|")) return
                try {
                    val host = parseFighterInfo(msg)
                    net.stopDiscovery()
                    goingToBattle = true
                    net.listener = null
                    onGuestBattle(host, me!!)
                } catch (e: Exception) {
                    status = "Host sent bad data."
                    phase = "discovering"
                }
            }

            override fun onEndpointFound(endpointId: String, name: String) {
                if (found.none { it.first == endpointId }) {
                    found = found + (endpointId to name)
                }
            }

            override fun onEndpointLost(endpointId: String) {
                found = found.filter { it.first != endpointId }
            }

            override fun onConnected(endpointId: String) {
                if (phase == "connecting") {
                    phase = "waiting"
                    status = "Connected! Waiting for the host..."
                    me?.let { net.send(it.toWire()) }
                }
            }

            override fun onDisconnected(endpointId: String) {
                if (phase == "connecting" || phase == "waiting") {
                    status = "Connection dropped. Pick a host again."
                    phase = "discovering"
                }
            }
            override fun onRadioError(msg: String) { status = msg }
        }
        net.startDiscovery()
    }

    DisposableEffect(Unit) {
        onDispose {
            // The battle screens take over the connection; only tear down
            // if we're leaving the lobby some other way.
            net.listener = null
            if (!goingToBattle) net.stopAll()
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(20, 0, 40)).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("VS PLAYER", color = Color.Yellow, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Phone-to-phone over WiFi — no internet needed", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            if (me == null) {
                Text(
                    "Build a battle-ready 30-chip folder first (Folder tab), then come back.",
                    color = Color(0xFFFF8A80), fontSize = 15.sp, textAlign = TextAlign.Center
                )
            } else {
                Text(me.name, color = Color.Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("HP ${me.maxHp} • ATK ${me.atkStat}", color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))

                when (phase) {
                    "choose" -> {
                        Text(
                            "One phone hosts, the other joins.\nNo internet needed — just stay close together.",
                            color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        // Radio readiness: tap a red row to open its setting.
                        listOf(
                            "Bluetooth" to radios.none { it.settingsAction == Settings.ACTION_BLUETOOTH_SETTINGS },
                            "WiFi" to radios.none { it.settingsAction == Settings.ACTION_WIFI_SETTINGS },
                            "Location" to radios.none { it.settingsAction == Settings.ACTION_LOCATION_SOURCE_SETTINGS }
                        ).forEach { (label, ok) ->
                            val issue = radios.firstOrNull {
                                (label == "Bluetooth" && it.settingsAction == Settings.ACTION_BLUETOOTH_SETTINGS) ||
                                (label == "WiFi" && it.settingsAction == Settings.ACTION_WIFI_SETTINGS) ||
                                (label == "Location" && it.settingsAction == Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            }
                            Button(
                                onClick = { issue?.let { openSettings(it.settingsAction) } },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (ok) Color(0, 90, 0) else Color(140, 30, 30)
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text(
                                    (if (ok) "✓ " else "✗ ") + label + (if (ok) " on" else " OFF — tap to fix"),
                                    color = Color.White, fontSize = 14.sp
                                )
                            }
                        }
                        if (status.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(status, color = Color(0xFFFF8A80), fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { withPerms { startHosting() } },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 140, 0)),
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        ) { Text("HOST BATTLE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { withPerms { startDiscovery() } },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 100, 180)),
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        ) { Text("JOIN BATTLE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    }
                    "hosting" -> {
                        Text(status, color = Color.Yellow, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your partner's name is showing on the other phone now.",
                            color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { cancel() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(90, 90, 90)),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("CANCEL", color = Color.White, fontSize = 18.sp) }
                    }
                    "discovering" -> {
                        Text("SCANNING FOR HOSTS...", color = Color.Yellow, fontSize = 16.sp)
                        if (status.isNotBlank()) {
                            Text(status, color = Color(0xFFFF8A80), fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (found.isEmpty()) {
                            Text(
                                "No hosts found yet.\nMake sure the other phone tapped HOST BATTLE.",
                                color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center
                            )
                        } else {
                            found.forEach { (id, name) ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = {
                                            phase = "connecting"
                                            status = "Connecting to $name..."
                                            net.requestConnection(id, me.name)
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 140, 0))
                                    ) { Text("FIGHT", color = Color.White) }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { cancel() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(90, 90, 90)),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("CANCEL", color = Color.White, fontSize = 18.sp) }
                    }
                    "connecting", "waiting" -> {
                        Text(status, color = Color.Yellow, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { cancel() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(90, 90, 90)),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("CANCEL", color = Color.White, fontSize = 18.sp) }
                    }
                }
            }
        }
    }
}
