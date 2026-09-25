package com.example.vitalwearclonev1.gridbattle

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun tierColor(tier: ChipTier): Color = when (tier) {
    ChipTier.STANDARD -> Color(0, 200, 220)
    ChipTier.MEGA -> Color(255, 150, 0)
    ChipTier.GIGA -> Color(190, 40, 180)
}

/**
 * Chip Folder (deck) builder (2026-09-25).
 *
 * Program the 30-chip deck used as battle power-ups. Saved per Digimon;
 * incomplete folders can be saved but are flagged until battle-ready.
 */
@Composable
fun FolderScreen(ownerId: String, ownerName: String) {
    val context = LocalContext.current
    var folderIds by remember {
        mutableStateOf(ChipFolder.load(context, ownerId).chipIds)
    }
    var search by remember { mutableStateOf("") }
    var tierFilter by remember { mutableStateOf<ChipTier?>(null) }
    var justSaved by remember { mutableStateOf(false) }

    val folder = remember(folderIds) { ChipFolder(folderIds) }
    val problems = remember(folderIds) { folder.validate() }

    val libraryChips = remember(search, tierFilter) {
        ChipLibrary.chips.filter { chip ->
            (tierFilter == null || chip.tier == tierFilter) &&
                (search.isBlank() || chip.name.contains(search, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(20, 0, 40))
            .padding(12.dp)
    ) {
        // ---- Header ----
        Text(
            "Chip Folder — $ownerName",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${folderIds.size}/${ChipFolder.FOLDER_SIZE} chips",
                color = Color.Cyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Total ${folder.totalMb()} MB",
                color = Color(200, 200, 220),
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        if (problems.isEmpty()) {
            Text(
                "Battle-ready! This deck is legal.",
                color = Color(120, 255, 170),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            problems.forEach { p ->
                Text("! $p", color = Color(255, 120, 120), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        // ---- Chip picker ----
        Text("Add chips:", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        TextField(
            value = search,
            onValueChange = { search = it; justSaved = false },
            placeholder = { Text("Search chips...", color = Color(150, 150, 170)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.White,
                backgroundColor = Color(35, 15, 60),
                focusedIndicatorColor = Color.Cyan,
                unfocusedIndicatorColor = Color(90, 60, 130),
                cursorColor = Color.Cyan
            )
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                null to "All",
                ChipTier.STANDARD to "Std",
                ChipTier.MEGA to "Mega",
                ChipTier.GIGA to "Giga"
            ).forEach { (tier, label) ->
                TextButton(
                    onClick = { tierFilter = tier },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (tierFilter == tier) Color.Cyan else Color(170, 170, 190)
                    )
                ) {
                    Text(
                        label,
                        fontWeight = if (tierFilter == tier) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0, 0, 0, 120), RoundedCornerShape(8.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(libraryChips, key = { it.id }) { chip ->
                val blockReason = folder.addBlockReason(chip.id)
                Card(
                    backgroundColor = Color(30, 10, 55),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(tierColor(chip.tier), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        chip.tier.name.take(4),
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    chip.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "DMG ${chip.dmgLabel()}  •  ${chip.codes.joinToString(" ")}  •  ${chip.mb}MB",
                                color = Color(180, 180, 200),
                                fontSize = 12.sp
                            )
                        }
                        if (blockReason == null) {
                            Button(
                                onClick = {
                                    folderIds = folderIds + chip.id
                                    justSaved = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0, 150, 100)
                                )
                            ) {
                                Text("Add", color = Color.White, fontSize = 13.sp)
                            }
                        } else {
                            Text(
                                blockReason,
                                color = Color(255, 170, 120),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- Current folder ----
        Text(
            "Your folder (${folderIds.size}):",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0, 0, 0, 120), RoundedCornerShape(8.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (folderIds.isEmpty()) {
                item {
                    Text(
                        "Empty — add chips above!",
                        color = Color(150, 150, 170),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            itemsIndexed(folder.chips(), key = { index, chip -> "$index-${chip.id}" }) { index, chip ->
                Card(
                    backgroundColor = Color(25, 35, 70),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}.",
                            color = Color(150, 150, 170),
                            fontSize = 12.sp,
                            modifier = Modifier.width(28.dp)
                        )
                        Box(
                            modifier = Modifier
                                .background(tierColor(chip.tier), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                chip.tier.name.take(4),
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                chip.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "DMG ${chip.dmgLabel()}  •  ${chip.mb}MB",
                                color = Color(180, 180, 200),
                                fontSize = 12.sp
                            )
                        }
                        TextButton(
                            onClick = {
                                folderIds = folderIds.toMutableList()
                                    .also { it.removeAt(index) }
                                justSaved = false
                            }
                        ) {
                            Text("X", color = Color(255, 120, 120), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                ChipFolder(folderIds).save(context, ownerId)
                justSaved = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 100, 200))
        ) {
            Text(
                if (justSaved) "Saved!" else "Save Folder",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (justSaved && problems.isNotEmpty()) {
            Text(
                "Saved, but the folder isn't battle-ready yet — fix the issues above.",
                color = Color(255, 200, 120),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun BattleChip.dmgLabel(): String =
    if (hits > 1) "$damage x$hits" else damage.toString()
