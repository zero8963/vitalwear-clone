package com.example.vitalwearclonev1.gridbattle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun tierColor(tier: ChipTier): Color = when (tier) {
    ChipTier.STANDARD -> Color(0, 200, 220)   // cyan
    ChipTier.MEGA -> Color(255, 150, 0)       // orange
    ChipTier.GIGA -> Color(190, 40, 180)      // red/purple
}

private fun elementColor(element: ChipElement): Color = when (element) {
    ChipElement.FIRE -> Color(255, 100, 60)
    ChipElement.WATER -> Color(80, 160, 255)
    ChipElement.ELEC -> Color(255, 220, 60)
    ChipElement.WOOD -> Color(110, 200, 90)
    ChipElement.SWORD -> Color(200, 200, 210)
    ChipElement.WIND -> Color(140, 230, 200)
    ChipElement.CURSOR -> Color(255, 140, 220)
    ChipElement.BREAK -> Color(255, 90, 90)
    ChipElement.PLUS -> Color(120, 255, 170)
    ChipElement.NULL -> Color(150, 150, 170)
}

/**
 * Chip Compendium (2026-09-25): browse/search the full battle-chip library.
 * Pure Compose, Material 2, dark theme matching the rest of the app.
 */
@Composable
fun CompendiumScreen() {
    var search by remember { mutableStateOf("") }
    var tierFilter by remember { mutableStateOf<ChipTier?>(null) }
    var elementFilter by remember { mutableStateOf<ChipElement?>(null) }
    var selectedChip by remember { mutableStateOf<BattleChip?>(null) }

    val filtered = remember(search, tierFilter, elementFilter) {
        ChipLibrary.chips.filter { chip ->
            (tierFilter == null || chip.tier == tierFilter) &&
                (elementFilter == null || chip.element == elementFilter) &&
                (search.isBlank() || chip.name.contains(search.trim(), ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(20, 0, 40))
            .padding(16.dp)
    ) {
        Text(
            "Chip Compendium",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${filtered.size} of ${ChipLibrary.chips.size} chips",
            color = Color.Cyan,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))

        // Search
        TextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search chips...", color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.White,
                backgroundColor = Color(0, 0, 0, 120),
                cursorColor = Color.Cyan,
                focusedIndicatorColor = Color.Cyan,
                unfocusedIndicatorColor = Color.Gray,
                placeholderColor = Color.Gray
            )
        )
        Spacer(Modifier.height(10.dp))

        // Tier filter row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TierFilterButton("All", tierFilter == null) { tierFilter = null }
            TierFilterButton("Standard", tierFilter == ChipTier.STANDARD, Color(0, 150, 170)) {
                tierFilter = ChipTier.STANDARD
            }
            TierFilterButton("Mega", tierFilter == ChipTier.MEGA, Color(200, 110, 0)) {
                tierFilter = ChipTier.MEGA
            }
            TierFilterButton("Giga", tierFilter == ChipTier.GIGA, Color(150, 30, 140)) {
                tierFilter = ChipTier.GIGA
            }
        }
        Spacer(Modifier.height(8.dp))

        // Element filter row (scrolls sideways)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElementFilterButton("All", elementFilter == null, Color.Gray) { elementFilter = null }
            for (element in ChipElement.values()) {
                ElementFilterButton(
                    element.name.lowercase().replaceFirstChar { it.uppercase() },
                    elementFilter == element,
                    elementColor(element)
                ) { elementFilter = element }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Chip list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { chip ->
                ChipRow(chip = chip, onClick = { selectedChip = chip })
            }
        }
    }

    // Detail dialog
    selectedChip?.let { chip ->
        AlertDialog(
            onDismissRequest = { selectedChip = null },
            backgroundColor = Color(30, 10, 55),
            title = {
                Text(chip.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TierBadge(tier = chip.tier)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            chip.element.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = elementColor(chip.element),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    DetailLine("Damage", if (chip.hits > 1) "${chip.damage} x ${chip.hits} hits" else "${chip.damage}")
                    DetailLine("Codes", chip.codes.joinToString(" "))
                    DetailLine("Memory", "${chip.mb} MB")
                    DetailLine("Type", chip.effectKind.name.lowercase().replaceFirstChar { it.uppercase() })
                    Spacer(Modifier.height(4.dp))
                    Text(chip.description, color = Color(220, 220, 230), fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedChip = null }) {
                    Text("Close", color = Color.Cyan)
                }
            }
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row {
        Text("$label: ", color = Color.Gray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TierFilterButton(
    label: String,
    selected: Boolean,
    activeColor: Color = Color(0, 150, 200),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (selected) activeColor else Color(60, 50, 80)
        ),
        modifier = Modifier.height(40.dp)
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun ElementFilterButton(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (selected) color else Color(60, 50, 80)
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            label,
            color = if (selected && color == Color.Gray) Color.White else Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun TierBadge(tier: ChipTier) {
    Box(
        modifier = Modifier
            .background(tierColor(tier), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            tier.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChipRow(chip: BattleChip, onClick: () -> Unit) {
    Card(
        backgroundColor = Color(35, 15, 60),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    chip.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                TierBadge(tier = chip.tier)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    chip.element.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = elementColor(chip.element),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("DMG ${chip.damage}${if (chip.hits > 1) "x${chip.hits}" else ""}", color = Color.White, fontSize = 13.sp)
                Text("Codes ${chip.codes.joinToString(" ")}", color = Color.Cyan, fontSize = 13.sp)
                Text("${chip.mb} MB", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
