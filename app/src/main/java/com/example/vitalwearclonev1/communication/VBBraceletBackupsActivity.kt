package com.example.vitalwearclonev1.communication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Saved bracelet character backups (see VBBraceletBackups).
 *
 * Reachable from DigiLab ("Bracelet Backups") and from the Bracelet
 * Character screen. Every entry is a full decrypted 864-byte blob captured
 * by a successful read — it survives leaving the transfer screen.
 *
 * Per entry: View (open on the character screen for editing),
 * Write to Bracelet (open with the write confirm pre-armed; the two-tap
 * session still runs for session ID / login / commit), Delete (confirm).
 */
class VBBraceletBackupsActivity : ComponentActivity() {

    private var backups by mutableStateOf(listOf<VBBraceletBackups.Backup>())
    private var deleteTarget by mutableStateOf<VBBraceletBackups.Backup?>(null)
    private var adoptTarget by mutableStateOf<VBBraceletBackups.Backup?>(null)
    /** Compare mode: tap two cards to select them, then diff. */
    private var compareMode by mutableStateOf(false)
    private var selectedIds by mutableStateOf(listOf<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BackupsScreen() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        backups = VBBraceletBackups.list(this)
    }

    private fun openBackup(backup: VBBraceletBackups.Backup, armWrite: Boolean) {
        val intent = Intent(this, VBBraceletCharacterActivity::class.java).apply {
            putExtra(VBBraceletCharacterActivity.EXTRA_BACKUP_ID, backup.id)
            putExtra(VBBraceletCharacterActivity.EXTRA_ARM_WRITE, armWrite)
        }
        startActivity(intent)
    }

    private fun deleteBackup(backup: VBBraceletBackups.Backup) {
        try {
            VBBraceletBackups.delete(this, backup.id)
        } catch (e: Exception) {
            Timber.e(e, "delete backup failed")
        }
        deleteTarget = null
        refresh()
    }

    @androidx.compose.runtime.Composable
    private fun BackupsScreen() {
        val list = backups
        val confirmDelete = deleteTarget
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0, 20, 40)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Bracelet Backups",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Auto-saved on every successful bracelet read. " +
                            "Write restores the saved character to a bracelet (2 taps).",
                    color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))

                if (list.isEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "No backups yet.\nRead a character from your bracelet\nand it will appear here automatically.",
                        color = Color(0xFF8A9BB5), fontSize = 14.sp,
                        textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                compareMode = !compareMode
                                selectedIds = emptyList()
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (compareMode) Color(0xFF6A4C93) else Color(0xFF444444)
                            )
                        ) { Text(if (compareMode) "Cancel Compare" else "Compare Two", fontSize = 12.sp) }
                        if (compareMode) {
                            Text(
                                "${selectedIds.size}/2 selected",
                                color = Color(0xFFFFD54F), fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (selectedIds.size == 2) {
                                Button(
                                    onClick = {
                                        startActivity(
                                            Intent(this@VBBraceletBackupsActivity, VBBraceletDiffActivity::class.java).apply {
                                                putExtra(VBBraceletDiffActivity.EXTRA_BACKUP_ID_A, selectedIds[0])
                                                putExtra(VBBraceletDiffActivity.EXTRA_BACKUP_ID_B, selectedIds[1])
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                                ) { Text("Show Diff", fontSize = 12.sp) }
                            }
                        }
                    }
                    if (compareMode) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap two backups to compare them byte-for-byte.",
                            color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list, key = { it.id }) { backup ->
                            BackupCard(
                                backup,
                                selectable = compareMode,
                                selectedOrder = selectedIds.indexOf(backup.id).let { if (it < 0) null else it },
                                onToggleSelect = {
                                    selectedIds = if (selectedIds.contains(backup.id)) {
                                        selectedIds - backup.id
                                    } else if (selectedIds.size < 2) {
                                        selectedIds + backup.id
                                    } else {
                                        selectedIds
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { finish() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))
                ) { Text("Done") }
                Spacer(Modifier.height(12.dp))
            }
        }

        if (confirmDelete != null) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Delete backup?") },
                text = {
                    Text(
                        "Delete the backup from ${confirmDelete.dateStr} " +
                                "(${confirmDelete.productName})? This cannot be undone.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { deleteBackup(confirmDelete) }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                }
            )
        }

        val adopt = adoptTarget
        if (adopt != null) {
            AdoptDialog(backup = adopt, onClose = { adoptTarget = null })
        }
    }

    @androidx.compose.runtime.Composable
    private fun BackupCard(
        backup: VBBraceletBackups.Backup,
        selectable: Boolean = false,
        selectedOrder: Int? = null,
        onToggleSelect: () -> Unit = {}
    ) {
        var cardModifier = Modifier.fillMaxWidth()
        if (selectable) cardModifier = cardModifier.clickable(onClick = onToggleSelect)
        Card(
            modifier = cardModifier,
            elevation = 4.dp,
            backgroundColor = if (selectedOrder != null) Color(0xFF3A5A8F) else Color(0xFF1E3A5F)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedOrder != null) {
                        Text(
                            if (selectedOrder == 0) "Ⓐ " else "Ⓑ ",
                            color = Color(0xFFFFD54F), fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "${backup.productName} · UID ${backup.uidShort}",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    backup.dateStr,
                    color = Color(0xFF8A9BB5), fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    backup.fieldSummary(),
                    color = Color(0, 255, 200), fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { openBackup(backup, armWrite = false) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                    ) { Text("View", fontSize = 12.sp) }
                    Button(
                        onClick = { adoptTarget = backup },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6A4C93))
                    ) { Text("Adopt", fontSize = 12.sp) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { openBackup(backup, armWrite = true) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0))
                    ) { Text("Write to Bracelet", fontSize = 12.sp) }
                    Button(
                        onClick = { deleteTarget = backup },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF777777))
                    ) { Text("Delete", fontSize = 12.sp) }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun AdoptDialog(backup: VBBraceletBackups.Backup, onClose: () -> Unit) {
        val context = LocalContext.current
        var candidates by remember { mutableStateOf(listOf<VBBraceletAdopt.AdoptCandidate>()) }
        var loading by remember { mutableStateOf(true) }
        var query by remember { mutableStateOf("") }
        var selected by remember { mutableStateOf<VBBraceletAdopt.AdoptCandidate?>(null) }
        var nickname by remember { mutableStateOf("") }
        var stage by remember { mutableStateOf(2) }

        LaunchedEffect(backup.id) {
            loading = true
            val list = withContext(Dispatchers.IO) { VBBraceletAdopt.loadCandidates(context) }
            candidates = list
            VBBraceletAdopt.rememberedChoice(context, backup.id)?.let { (cardName, charId) ->
                list.firstOrNull { it.cardName == cardName && it.charId == charId }?.let { pick ->
                    selected = pick
                    stage = pick.stage
                }
            }
            loading = false
        }

        val shown = remember(candidates, query) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) candidates
            else candidates.filter {
                it.cardName.lowercase().contains(q) ||
                        it.label.lowercase().contains(q)
            }
        }

        Dialog(onDismissRequest = onClose) {
            Card(backgroundColor = Color(0xFF1E3A5F), elevation = 8.dp) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Adopt as Partner",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "The bracelet doesn't say which Digimon this is — " +
                                "pick the species from your imported cards. " +
                                "It joins the DigiLab fresh: level 1, no training, " +
                                "mood from the bracelet.",
                        color = Color(0xFF8A9BB5), fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search cards…", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        loading -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) { CircularProgressIndicator(color = Color(0xFFFFD54F)) }
                        }
                        candidates.isEmpty() -> {
                            Text(
                                "No DIM/BEM cards imported yet.\nImport one in Manage Cards first.",
                                color = Color(0xFFFFB74D), fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().height(240.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(shown, key = { "${it.cardName}#${it.charId}" }) { c ->
                                    val isSel = selected?.cardName == c.cardName &&
                                            selected?.charId == c.charId
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable {
                                                selected = c
                                                stage = c.stage
                                            }
                                            .background(
                                                if (isSel) Color(0xFF3A5A8F) else Color(0xFF14273F),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val bmp = c.sprite
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = c.label,
                                                modifier = Modifier.size(44.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.size(44.dp)
                                                    .background(Color(0xFF0A1A2F)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("?", color = Color.Gray, fontSize = 18.sp)
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                c.cardName, color = Color.White,
                                                fontSize = 13.sp, fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Character #${c.charId} · " +
                                                        VBBraceletAdopt.STAGE_NAMES.getOrElse(c.stage) { "Stage ${c.stage}" },
                                                color = Color(0xFF8A9BB5), fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        if (isSel) {
                                            Text(
                                                "✓", color = Color(0xFF00E5A0),
                                                fontSize = 18.sp, fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    TextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        placeholder = { Text("Nickname (optional)", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Stage", color = Color(0xFF8A9BB5), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0 until 2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (i in row * 3 until row * 3 + 3) {
                                    val label = VBBraceletAdopt.STAGE_NAMES[i]
                                    Button(
                                        onClick = { stage = i },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = if (stage == i) Color(0xFF6A4C93)
                                            else Color(0xFF333333)
                                        )
                                    ) {
                                        Text(label, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onClose,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555))
                        ) { Text("Cancel", fontSize = 13.sp) }
                        Button(
                            onClick = {
                                val pick = selected ?: return@Button
                                try {
                                    VBBraceletAdopt.adopt(
                                        context, backup, pick, nickname.trim(), stage
                                    )
                                } catch (e: Exception) {
                                    Timber.e(e, "adopt failed")
                                    Toast.makeText(
                                        context,
                                        "Adopt failed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                onClose()
                            },
                            enabled = selected != null,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6A4C93))
                        ) { Text("Adopt!", fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}
