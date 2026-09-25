package com.example.vitalwearclonev1.gridbattle

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.card.BemCard
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.card.SpriteBitmapHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Attack FX picker: choose any card/character whose base (DIM-programmed)
 * attack animations your partner should borrow. Shared with Adventure mode —
 * a NaviCust Core program still wins when one is installed.
 */
private data class FxCandidate(
    val cardName: String,
    val charId: Int,
    val sprite: Bitmap?
)

@Composable
fun AttackFxPickerScreen(ownerId: String) {
    val context = LocalContext.current
    var current by remember(ownerId) { mutableStateOf(AttackFxOverrides.load(context, ownerId)) }
    var candidates by remember { mutableStateOf<List<FxCandidate>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(ownerId) {
        loading = true
        withContext(Dispatchers.IO) {
            val list = mutableListOf<FxCandidate>()
            try {
                val cm = CardManager(context)
                for (name in cm.listCards().filter { it.isNotBlank() }) {
                    try {
                        val card = cm.getCard(name) ?: continue
                        val count = try {
                            card.characterStats.characterEntries.size
                        } catch (e: Exception) { 0 }
                        if (count <= 0) continue
                        val isBem = card is BemCard
                        val sprites = try { card.spriteData.sprites } catch (e: Exception) { null }
                        for (i in 0 until count) {
                            val bmp = try {
                                val base = charBaseIndex(i, isBem)
                                sprites?.getOrNull(base + 1)?.let { SpriteBitmapHandler.getBitmap(it) }
                            } catch (e: Exception) { null }
                            list.add(FxCandidate(name, i, bmp))
                        }
                    } catch (e: Exception) { /* skip unreadable card */ }
                }
            } catch (e: Exception) { /* no cards */ }
            candidates = list
            loading = false
        }
    }

    fun pick(c: FxCandidate?) {
        val ov = c?.let { AttackFxOverride(it.cardName, it.charId) }
        AttackFxOverrides.save(context, ownerId, ov)
        current = ov
    }

    val shown = remember(candidates, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) candidates
        else candidates.filter { it.cardName.lowercase().contains(q) }
    }

    Column(
        Modifier.fillMaxSize().background(Color(10, 0, 25)).padding(12.dp)
    ) {
        Text(
            "ATTACK FX",
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
        )
        Text(
            "Borrow another Digimon's base attack animations. Your partner keeps its own stats — only the animation changes. A NaviCust Core program still takes priority when installed.",
            color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        // Current selection
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(30, 10, 60))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cur = current
            if (cur != null) {
                val bmp = candidates.firstOrNull { it.cardName == cur.cardName && it.charId == cur.charId }?.sprite
                if (bmp != null) {
                    Image(
                        bmp.asImageBitmap(), contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0, 0, 0, 120))
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Now using:", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        "${cur.cardName} · #${cur.charId}",
                        color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
                Button(
                    onClick = { pick(null) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(120, 30, 30))
                ) { Text("RESET", color = Color.White, fontSize = 12.sp) }
            } else {
                Text(
                    "Using your partner's own attack animations.",
                    color = Color.Gray, fontSize = 13.sp, modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search cards…", color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.White,
                backgroundColor = Color(25, 25, 45),
                focusedIndicatorColor = Color(0xFF7B61FF),
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(Modifier.height(8.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading cards…", color = Color.Gray)
            }
        } else if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matches.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(shown, key = { "${it.cardName}#${it.charId}" }) { c ->
                    val selected = current?.cardName == c.cardName && current?.charId == c.charId
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(60, 40, 120) else Color(22, 22, 40))
                            .border(
                                2.dp,
                                if (selected) Color(0xFFFFD54F) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { pick(c) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (c.sprite != null) {
                            Image(
                                c.sprite.asImageBitmap(), contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Color(0, 0, 0, 120))
                            )
                        } else {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Color(50, 50, 70)),
                                contentAlignment = Alignment.Center
                            ) { Text("?", color = Color.Gray, fontSize = 20.sp) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.cardName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Character #${c.charId}", color = Color.Gray, fontSize = 12.sp)
                        }
                        if (selected) Text("✓", color = Color(0xFFFFD54F), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
