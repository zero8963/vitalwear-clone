package com.example.vitalwearclonev1.dex

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DigiDex screen (2026-09-29).
 *
 * Lives in Settings. Pick an installed DIM/BEM card to browse its full
 * evolution tree: every Digimon, what it evolves into, and the bracelet
 * requirements (VP, trophies, battles, win rate) for each path.
 */
@Composable
fun DigiDexScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var cardNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCard by remember { mutableStateOf<String?>(null) }
    var dexCard by remember { mutableStateOf<DigiDexData.DexCard?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            cardNames = DigiDexData.listCards(context)
        }
        if (cardNames.isNotEmpty() && selectedCard == null) {
            selectedCard = cardNames.first()
        }
    }

    LaunchedEffect(selectedCard) {
        val name = selectedCard ?: return@LaunchedEffect
        loading = true
        dexCard = withContext(Dispatchers.IO) {
            DigiDexData.loadCard(context, name)
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0, 20, 40))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(60, 60, 80))
            ) {
                Text("← Back", color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Text("DigiDex", color = Color.Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        if (cardNames.isEmpty()) {
            Text("No DIM cards installed.", color = Color.Gray, fontSize = 14.sp)
        } else {
            // Card picker
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedCard ?: "Select card", color = Color.White)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    cardNames.forEach { name ->
                        DropdownMenuItem(onClick = {
                            selectedCard = name
                            expanded = false
                        }) {
                            Text(name)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (loading) {
                CircularProgressIndicator(color = Color.Cyan)
            } else {
                dexCard?.let { card ->
                    val grouped = card.entries.groupBy { it.stage }.toSortedMap()
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        grouped.forEach { (stage, entries) ->
                            val stageLabel = DigiDexData.stageName(stage)
                                .ifEmpty { "Stage $stage" }
                            Text(
                                stageLabel,
                                color = Color(255, 215, 0),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            entries.forEach { entry ->
                                DexEntryCard(entry)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DexEntryCard(entry: DigiDexData.DexEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0, 40, 80),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                entry.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (entry.evolutions.isEmpty()) {
                Text(
                    "Final form — no further evolutions",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                entry.evolutions.forEach { evo ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "→ ${evo.toName}",
                        color = Color.Green,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    DexRequirementRow("Vital Points", "${evo.requiredVitalPoints} VP")
                    DexRequirementRow("Trophies", "${evo.requiredTrophies}")
                    DexRequirementRow("Battles", "${evo.requiredBattles}")
                    DexRequirementRow("Win Rate", "${evo.requiredWinRate}%")
                    if (evo.hoursRequired > 0) {
                        DexRequirementRow("Time", "${evo.hoursRequired}h")
                    }
                }
            }
        }
    }
}

@Composable
private fun DexRequirementRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color(150, 220, 255), fontSize = 12.sp)
    }
}
