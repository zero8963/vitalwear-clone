package com.example.vitalwearclonev1.gridbattle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun programColor(c: ProgramColor): Color = when (c) {
    ProgramColor.RED -> Color(220, 70, 70)
    ProgramColor.BLUE -> Color(80, 150, 255)
    ProgramColor.YELLOW -> Color(240, 205, 70)
    ProgramColor.GREEN -> Color(85, 205, 125)
}

private fun styleColor(s: BattleStyle): Color = when (s) {
    BattleStyle.BLAZE -> Color(255, 110, 60)
    BattleStyle.AQUA -> Color(80, 170, 255)
    BattleStyle.GALE -> Color(255, 215, 80)
    BattleStyle.TERRA -> Color(90, 210, 130)
    BattleStyle.NONE -> Color.Gray
}

private fun styleBlurb(s: BattleStyle): String = when (s) {
    BattleStyle.BLAZE -> "Blaze style: your Digimon hits like a truck."
    BattleStyle.AQUA -> "Aqua style: your Digimon glides across the grid."
    BattleStyle.GALE -> "Gale style: your Digimon charges shots in a flash."
    BattleStyle.TERRA -> "Terra style: your Digimon is tough to take down."
    BattleStyle.NONE -> "No style yet — place programs of one color."
}

/**
 * NaviCust ("Program Grid") screen (2026-09-25).
 *
 * Place program parts on a 6x6 grid around the blocked core. Every part
 * must connect back to the core; same-colored parts that touch cause
 * glitches. RUN validates and shows bonuses; Save persists per Digimon.
 */
@Composable
fun NaviCustScreen(ownerId: String, ownerName: String) {
    val context = LocalContext.current
    var placements by remember {
        mutableStateOf(NaviCustLoadout.load(context, ownerId).placements)
    }
    var selectedPartId by remember { mutableStateOf<Int?>(null) }
    var rotation by remember { mutableStateOf(0) }
    var removeMode by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<ValidationResult?>(null) }
    var saveMsg by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }

    val loadout = remember(placements) { NaviCustLoadout(placements) }
    // cell -> (part, isGlitched)
    val cellMap = remember(placements) {
        val glitched = loadout.validate().glitchedPartIds
        val map = mutableMapOf<Pair<Int, Int>, Pair<ProgramPart, Boolean>>()
        for (p in placements) {
            val part = ProgramParts.byId(p.partId) ?: continue
            for (cell in loadout.absoluteCells(p)) {
                map[cell] = part to (p.partId in glitched)
            }
        }
        map
    }

    fun onCellTap(r: Int, c: Int) {
        report = null
        saveMsg = null
        hint = null
        if (removeMode) {
            val idx = placements.indexOfFirst { p ->
                loadout.absoluteCells(p).contains(r to c)
            }
            if (idx >= 0) {
                placements = placements.toMutableList().also { it.removeAt(idx) }
            }
            return
        }
        val partId = selectedPartId
        if (partId == null) {
            hint = "Pick a program below first."
            return
        }
        if (placements.any { it.partId == partId }) {
            hint = "Already placed — remove it first to move it."
            return
        }
        placements = placements + Placement(partId, r, c, rotation)
        selectedPartId = null // placed — pick the next program
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(20, 0, 40))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Program Grid", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("$ownerName's loadout", color = Color.Cyan, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Parts must touch the core. Same colors touching = glitch! CORE parts change your buster & sword.",
            color = Color.Gray, fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))

        // --- 6x6 grid ---
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (r in 0 until NaviCustLoadout.GRID_SIZE) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (c in 0 until NaviCustLoadout.GRID_SIZE) {
                        val isCore = r to c in NaviCustLoadout.CORE_CELLS
                        val info = cellMap[r to c]
                        val bg = when {
                            isCore -> Color(70, 70, 80)
                            info != null -> {
                                val base = programColor(info.first.color)
                                if (info.second) base.copy(alpha = 0.35f) else base
                            }
                            else -> Color(12, 4, 26)
                        }
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(bg, RoundedCornerShape(4.dp))
                                .border(1.dp, Color(90, 70, 120), RoundedCornerShape(4.dp))
                                .clickable { onCellTap(r, c) },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isCore -> Text("CORE", color = Color(200, 200, 210), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                info != null && info.second -> Text("!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        hint?.let {
            Text(it, color = Color.Yellow, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(4.dp))
        }

        // --- controls ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { rotation = (rotation + 1) % 4 },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 110, 180)),
                modifier = Modifier.weight(1f)
            ) { Text("Rotate ⟳", color = Color.White, fontSize = 13.sp) }
            Button(
                onClick = { removeMode = !removeMode },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (removeMode) Color(180, 60, 60) else Color(70, 60, 90)
                ),
                modifier = Modifier.weight(1f)
            ) { Text(if (removeMode) "Removing: ON" else "Remove", color = Color.White, fontSize = 13.sp) }
            Button(
                onClick = { placements = emptyList(); report = null; saveMsg = null },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(70, 60, 90)),
                modifier = Modifier.weight(1f)
            ) { Text("Clear", color = Color.White, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(12.dp))

        // --- parts tray ---
        Text("Programs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(172.dp)
        ) {
            items(ProgramParts.all, key = { it.id }) { part ->
                val placed = placements.any { it.partId == part.id }
                val selected = selectedPartId == part.id
                PartCard(
                    part = part,
                    rotation = rotation,
                    placed = placed,
                    selected = selected,
                    onClick = {
                        selectedPartId = if (selected) null else part.id
                        removeMode = false
                        hint = null
                    }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // --- RUN ---
        Button(
            onClick = {
                report = loadout.validate()
                saveMsg = null
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 100)),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("RUN PROGRAMS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))

        // --- report ---
        report?.let { res ->
            Card(
                backgroundColor = Color(30, 12, 52),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (res.errors.isNotEmpty()) {
                        Text("Fix these first:", color = Color(255, 120, 120), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        res.errors.forEach { e ->
                            Text("• $e", color = Color(255, 150, 150), fontSize = 13.sp)
                        }
                    } else if (placements.isEmpty()) {
                        Text("Grid is empty — place some programs!", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        Text("All programs running clean!", color = Color(120, 255, 170), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    if (res.glitchedPartIds.isNotEmpty()) {
                        val names = res.glitchedPartIds.mapNotNull { ProgramParts.byId(it)?.name }
                        Text(
                            "Glitched (no bonus): ${names.joinToString(", ")}",
                            color = Color(255, 180, 80), fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Glitch HP penalty: -${res.glitchedPartIds.size * 10} HP in battle",
                            color = Color(255, 180, 80), fontSize = 13.sp
                        )
                    }

                    val b = loadout.totalBonuses()
                    Text(
                        "Bonuses: ATK +${b.attackPct}%   HP +${b.maxHpBonus}   SPD +${b.speedPct}%   CHG +${b.chargePct}%",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    val busterOv = loadout.busterOverride()
                    val swordOv = loadout.swordOverride()
                    Text(
                        "Buster: " + (busterOv?.let { "${it.element.name} \u2014 ${it.displayName()}" } ?: "(default)"),
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Sword: " + (swordOv?.let { "${it.element.name} \u2014 ${it.displayName()}" } ?: "(default)"),
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    val style = loadout.style()
                    Text(
                        "Style: ${style.name}",
                        color = styleColor(style), fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Text(styleBlurb(style), color = Color.Gray, fontSize = 12.sp)

                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            loadout.save(context, ownerId)
                            saveMsg = "Loadout saved for $ownerName!"
                        },
                        enabled = res.isOk,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0, 110, 180),
                            disabledBackgroundColor = Color(50, 50, 60)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Loadout", color = Color.White) }
                    saveMsg?.let { Text(it, color = Color(120, 255, 170), fontSize = 13.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PartCard(
    part: ProgramPart,
    rotation: Int,
    placed: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = remember(part.id, rotation) { rotatedCells(part.cells, rotation) }
    val maxR = (shape.maxOfOrNull { it.first } ?: 0) + 1
    val maxC = (shape.maxOfOrNull { it.second } ?: 0) + 1
    Card(
        backgroundColor = if (selected) Color(50, 90, 130) else Color(35, 15, 60),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(118.dp)
            .border(
                2.dp,
                when {
                    selected -> Color.Cyan
                    placed -> Color(100, 200, 120)
                    else -> Color.Transparent
                },
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(programColor(part.color), RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.height(4.dp))
            Text(
                part.name, color = Color.White, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            if (part.isCore()) {
                Text(
                    "CORE", color = Color(255, 200, 90), fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(4.dp))
            // shape preview
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                for (r in 0 until maxR) {
                    Row {
                        for (c in 0 until maxC) {
                            val filled = r to c in shape
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .padding(1.dp)
                                    .background(
                                        if (filled) programColor(part.color) else Color.Transparent,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                part.effectSummary(), color = Color.Cyan, fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            if (placed) {
                Text("PLACED", color = Color(100, 220, 130), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
