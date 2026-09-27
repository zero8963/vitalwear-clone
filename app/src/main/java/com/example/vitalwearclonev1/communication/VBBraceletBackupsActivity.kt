package com.example.vitalwearclonev1.communication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list, key = { it.id }) { backup ->
                            BackupCard(backup)
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
    }

    @androidx.compose.runtime.Composable
    private fun BackupCard(backup: VBBraceletBackups.Backup) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp,
            backgroundColor = Color(0xFF1E3A5F)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "${backup.productName} · UID ${backup.uidShort}",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
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
}
