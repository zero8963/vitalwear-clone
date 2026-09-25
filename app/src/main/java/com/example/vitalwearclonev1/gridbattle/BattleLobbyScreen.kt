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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Pre-fight lobby for Grid Battle mode (2026-09-25).
 *
 * Shows the Digimon's computed battle stats (base + training + NaviCust),
 * battle style, buster/sword rewires, and chip-folder readiness.
 * FIGHT is only enabled with a battle-ready 30-chip folder.
 */
@Composable
fun BattleLobbyScreen(
    ownerId: String,
    monster: PhoneMonsterManager.MonsterState,
    onFight: (BattleSetup) -> Unit
) {
    val context = LocalContext.current

    val loadout = remember(ownerId) {
        val l = NaviCustLoadout.load(context, ownerId)
        if (l.validate().errors.isEmpty()) l else NaviCustLoadout(emptyList())
    }
    val folder = remember(ownerId) { ChipFolder.load(context, ownerId) }
    val problems = remember(folder) { folder.validate() }
    val bonuses = remember(loadout) { loadout.totalBonuses() }
    val style = remember(loadout) { loadout.style() }
    val busterOv = remember(loadout) { loadout.busterOverride() }
    val swordOv = remember(loadout) { loadout.swordOverride() }

    val displayName = monster.nickname?.takeIf { it.isNotBlank() } ?: monster.cardName
    val atkStat = monster.baseAp + monster.attackBonus
    // TUNE: enemy scaling vs the player.
    val playerMaxHp = monster.baseHp + monster.healthBonus + bonuses.maxHpBonus - loadout.glitchPenaltyHp()
    val effAtkMult = (1f + bonuses.attackPct / 100f) * (if (style == BattleStyle.BLAZE) 1.1f else 1f)
    val chargeRate = 1f + bonuses.chargePct / 100f

    // Pick a random wild opponent card (original flavor: a rogue virus).
    var enemyPick by remember { mutableStateOf<Pair<String, Int>?>(null) }
    LaunchedEffect(ownerId) {
        withContext(Dispatchers.IO) {
            try {
                val cm = CardManager(context)
                val names = cm.listCards().filter { it.isNotBlank() }
                if (names.isNotEmpty()) {
                    val name = names.random()
                    val card = cm.getCard(name)
                    val count = try {
                        card?.characterStats?.characterEntries?.size ?: 1
                    } catch (e: Exception) { 1 }
                    enemyPick = name to Random.nextInt(maxOf(1, count))
                }
            } catch (e: Exception) {
                enemyPick = null
            }
        }
    }

    fun statRow(label: String, value: String, color: Color = Color.White) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.Gray, fontSize = 14.sp)
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            Text("GRID BATTLE", color = Color.Yellow, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("vs the wild viruses", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            Text(displayName, color = Color.Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            statRow("HP", "$playerMaxHp")
            statRow("ATK", "$atkStat${if (bonuses.attackPct > 0) " (+${bonuses.attackPct}%)" else ""}")
            statRow(
                "Style",
                if (style == BattleStyle.NONE) "—" else style.name,
                color = if (style == BattleStyle.NONE) Color.White else Color.Cyan
            )
            statRow("Buster", busterOv?.displayName() ?: "Default")
            statRow("Sword", swordOv?.displayName() ?: "Default")
            if (bonuses.speedPct > 0) statRow("Shot speed", "+${bonuses.speedPct}%")
            if (bonuses.chargePct > 0) statRow("Charge rate", "+${bonuses.chargePct}%")
            val glitch = loadout.glitchPenaltyHp()
            if (glitch > 0) statRow("Glitch strain", "-$glitch HP", color = Color(0xFFFF8A80))

            Spacer(Modifier.height(16.dp))

            if (problems.isEmpty()) {
                Text("✔ Chip folder battle-ready (${folder.chipIds.size}/30)", color = Color(0xFF7BFF6B), fontSize = 14.sp)
            } else {
                Text("✘ Chip folder not ready:", color = Color(0xFFFF8A80), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                problems.take(3).forEach { p ->
                    Text("• $p", color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
                Text("Open the Folder tab to build it.", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(Modifier.height(24.dp))

            val ready = problems.isEmpty() && enemyPick != null
            Button(
                onClick = {
                    val (eName, eChar) = enemyPick ?: return@Button
                    // TUNE: wild virus scaling — 85% of player HP, 90% of player ATK.
                    val eHp = (playerMaxHp * 0.85f).toInt().coerceAtLeast(50)
                    val eAtk = (atkStat * 0.9f).toInt().coerceAtLeast(5)
                    val config = BattleConfig(
                        playerName = displayName,
                        playerMaxHp = playerMaxHp.coerceAtLeast(50),
                        atkStat = atkStat,
                        effAtkMult = effAtkMult,
                        busterElement = busterOv?.element,
                        busterAnimKey = busterOv?.animKey,
                        swordAnimKey = swordOv?.animKey,
                        playerDeck = folder.chipIds,
                        enemyName = "Wild ${eName.take(18)}",
                        enemyMaxHp = eHp,
                        enemyAtk = eAtk,
                        chargeRate = chargeRate
                    )
                    onFight(
                        BattleSetup(
                            ownerId = ownerId,
                            playerCardName = monster.cardName,
                            playerCharId = monster.characterId,
                            displayName = displayName,
                            config = config,
                            enemyCardName = eName,
                            enemyCharId = eChar
                        )
                    )
                },
                enabled = ready,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0, 140, 0),
                    disabledBackgroundColor = Color(60, 60, 60)
                ),
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Text(
                    if (enemyPick == null) "FINDING OPPONENT..." else "FIGHT!",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
