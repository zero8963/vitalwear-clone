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
 * Screens switch on a simple state so NaviCust and the battle itself can
 * slot in later: "compendium" | "navicust" | "battle".
 */
class GridBattleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var screen by remember { mutableStateOf("compendium") }
            // The active Digimon owns this grid-battle profile (chips folder +
            // NaviCust loadout are saved per Digimon).
            val monsterManager = remember {
                com.example.vitalwearclonev1.monster.PhoneMonsterManager(this)
            }
            val monster = remember { monsterManager.getCurrentMonster() }
            val ownerId = remember(monster) {
                monster?.let { m ->
                    val base = m.nickname?.takeIf { it.isNotBlank() } ?: m.cardName
                    "${base}_${m.characterId}"
                } ?: "guest"
            }
            val ownerName = monster?.nickname?.takeIf { it.isNotBlank() }
                ?: monster?.cardName ?: "No Digimon"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(20, 0, 40))
            ) {
                TabRow(
                    selectedTabIndex = if (screen == "navicust") 1 else 0,
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
                        text = { Text("NaviCust") }
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
                        // Future screen:
                        // "battle" -> GridBattleScreen(onExit = { screen = "compendium" })
                        else -> CompendiumScreen()
                    }
                }
            }
        }
    }
}
