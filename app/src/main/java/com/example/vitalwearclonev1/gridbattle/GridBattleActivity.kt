package com.example.vitalwearclonev1.gridbattle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Grid Battle mode host (2026-09-25).
 *
 * A separate battle mode in the phone app: Digimon partners fight real-time
 * on a grid with battle chips. Deliberately separate from the watch-linked
 * battle structure — nothing here talks to the watch.
 *
 * Screens switch on a simple state:
 * "compendium" | "navicust" | "folder" | "lobby" | "battle".
 */
class GridBattleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Deep link from the main UI's Folder / Digi-Custom buttons (2026-09-30).
        val startScreen = intent.getStringExtra("startScreen")
            ?.takeIf { it == "folder" || it == "navicust" }
        setContent {
            var screen by remember { mutableStateOf(startScreen ?: "compendium") }
            // The active Digimon owns this grid-battle profile (chips folder +
            // NaviCust loadout are saved per Digimon).
            val monsterManager = remember {
                com.example.vitalwearclonev1.monster.PhoneMonsterManager(this)
            }
            val monster = remember { monsterManager.getCurrentMonster() }
            val ownerId = remember(monster) {
                monster?.let { m -> ownerIdFor(m.nickname, m.cardName, m.characterId) } ?: "guest"
            }
            val ownerName = monster?.nickname?.takeIf { it.isNotBlank() }
                ?: monster?.cardName ?: "No Digimon"
            var battleSetup by remember { mutableStateOf<BattleSetup?>(null) }
            val pvpNet = remember { PvpNetManager(this) }
            var pvpHostBinding by remember { mutableStateOf<PvpHostBinding?>(null) }
            var pvpMe by remember { mutableStateOf<PvpFighterInfo?>(null) }
            var pvpHostInfo by remember { mutableStateOf<PvpFighterInfo?>(null) }

            DisposableEffect(Unit) {
                onDispose { pvpNet.stopAll() }
            }

            if (screen == "battle" && battleSetup != null) {
                GridBattleScreen(
                    setup = battleSetup!!,
                    onExit = { screen = "lobby" },
                    onResult = { won ->
                        // Link grid-battle results into VP/care (2026-09-30):
                        // win payout scales with foe strength, losses drain.
                        val bs = battleSetup!!
                        val oppPower = (bs.config.enemyMaxHp + bs.config.enemyAtk).toLong()
                        monsterManager.recordBattleResult(won, oppPower)
                    }
                )
            } else if (screen == "pvpHost" && battleSetup != null && pvpHostBinding != null) {
                GridBattleScreen(
                    setup = battleSetup!!,
                    pvpHost = pvpHostBinding!!,
                    onExit = { pvpNet.stopAll(); screen = "pvp" }
                )
            } else if (screen == "pvpGuest" && pvpMe != null && pvpHostInfo != null) {
                PvpGuestScreen(
                    guestInfo = pvpMe!!,
                    hostInfo = pvpHostInfo!!,
                    net = pvpNet,
                    onExit = { pvpNet.stopAll(); screen = "pvp" }
                )
            } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(20, 0, 40))
            ) {
                TabRow(
                    selectedTabIndex = when (screen) {
                        "navicust" -> 1
                        "folder" -> 2
                        "attackfx" -> 3
                        "lobby", "battle" -> 4
                        "pvp", "pvpHost", "pvpGuest" -> 5
                        else -> 0
                    },
                    backgroundColor = Color(0, 50, 100),
                    contentColor = Color.White
                ) {
                    Tab(
                        selected = screen == "compendium",
                        onClick = { screen = "compendium" },
                        text = { Text("Chips") }
                    )
                    Tab(
                        selected = screen == "navicust",
                        onClick = { screen = "navicust" },
                        text = { Text("Digi-Custom") }
                    )
                    Tab(
                        selected = screen == "folder",
                        onClick = { screen = "folder" },
                        text = { Text("Folder") }
                    )
                    Tab(
                        selected = screen == "attackfx",
                        onClick = { screen = "attackfx" },
                        text = { Text("Attack FX") }
                    )
                    Tab(
                        selected = screen == "lobby" || screen == "battle",
                        onClick = { screen = "lobby" },
                        text = { Text("Battle") }
                    )
                    Tab(
                        selected = screen == "pvp" || screen == "pvpHost" || screen == "pvpGuest",
                        onClick = { screen = "pvp" },
                        text = { Text("VS Player") }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    when (screen) {
                        "compendium" -> CompendiumScreen()
                        "navicust" -> if (monster != null) {
                            NaviCustScreen(ownerId = ownerId, ownerName = ownerName)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Hatch a Digimon first!\nYour partner owns the Program Grid.",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        }
                        "folder" -> if (monster != null) {
                            FolderScreen(ownerId = ownerId, ownerName = ownerName)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Hatch a Digimon first!\nYour partner owns the Chip Folder.",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        }
                        "attackfx" -> if (monster != null) {
                            AttackFxPickerScreen(ownerId = ownerId)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Hatch a Digimon first!\nYour partner owns the Attack FX.",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        }
                        "lobby" -> if (monster != null) {
                            BattleLobbyScreen(
                                ownerId = ownerId,
                                monster = monster,
                                onFight = { setup ->
                                    battleSetup = setup
                                    screen = "battle"
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Hatch a Digimon first!\nYour partner fights Grid Battles.",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        }
                        "pvp" -> if (monster != null) {
                            PvpLobbyScreen(
                                ownerId = ownerId,
                                monster = monster,
                                net = pvpNet,
                                onHostBattle = { setup, guest ->
                                    battleSetup = setup
                                    pvpHostBinding = PvpHostBinding(pvpNet, guest)
                                    screen = "pvpHost"
                                },
                                onGuestBattle = { host, my ->
                                    pvpHostInfo = host
                                    pvpMe = my
                                    screen = "pvpGuest"
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Hatch a Digimon first!\nYour partner fights PvP battles.",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        }
                        else -> CompendiumScreen()
                    }
                }
            }
            }
        }
    }
}
