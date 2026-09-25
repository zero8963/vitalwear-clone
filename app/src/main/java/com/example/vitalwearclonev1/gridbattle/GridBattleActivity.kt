package com.example.vitalwearclonev1.gridbattle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(20, 0, 40))
            ) {
                when (screen) {
                    "compendium" -> CompendiumScreen()
                    // Future screens:
                    // "navicust" -> NaviCustScreen(onBack = { screen = "compendium" })
                    // "battle" -> GridBattleScreen(onExit = { screen = "compendium" })
                    else -> CompendiumScreen()
                }
            }
        }
    }
}
