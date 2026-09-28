package com.example.vitalwearclonev1.communication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
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
 * Byte-level diff of two saved bracelet backups (see VBBraceletDiff).
 *
 * Recipe for the species hunt: read the bracelet with Digimon A, swap the
 * Digimon, read again with Digimon B, then diff the two backups here. The
 * bytes that change with the species are its ID — short unknown ranges are
 * flagged as candidates at the top.
 */
class VBBraceletDiffActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BACKUP_ID_A = "diff_backup_a"
        const val EXTRA_BACKUP_ID_B = "diff_backup_b"
    }

    private var backupA by mutableStateOf<VBBraceletBackups.Backup?>(null)
    private var backupB by mutableStateOf<VBBraceletBackups.Backup?>(null)
    private var result by mutableStateOf<VBBraceletDiff.DiffResult?>(null)
    private var error by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val idA = intent.getStringExtra(EXTRA_BACKUP_ID_A)
        val idB = intent.getStringExtra(EXTRA_BACKUP_ID_B)
        try {
            val a = idA?.let { VBBraceletBackups.get(this, it) }
            val b = idB?.let { VBBraceletBackups.get(this, it) }
            if (a == null || b == null) {
                error = "One of the backups is gone — it may have been deleted."
            } else {
                backupA = a
                backupB = b
                result = VBBraceletDiff.diff(a, b)
            }
        } catch (e: Exception) {
            Timber.e(e, "diff failed")
            error = "Diff failed: ${e.message}"
        }
        setContent { DiffScreen() }
    }

    @androidx.compose.runtime.Composable
    private fun DiffScreen() {
        val a = backupA
        val b = backupB
        val r = result
        val err = error
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0, 20, 40)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Backup Diff",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                if (a != null && b != null) {
                    Text(
                        "A: ${a.dateStr} (${a.uidShort})\nB: ${b.dateStr} (${b.uidShort})",
                        color = Color(0xFF8A9BB5), fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (err != null) {
                    Text(err, color = Color(0xFFFF6B6B), fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace)
                }

                Spacer(Modifier.height(8.dp))

                if (r != null) {
                    // Summary card — fixed at top, ranges scroll below.
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF1E3A5F), elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${r.totalBytes} differing bytes in ${r.ranges.size} ranges",
                                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.height(6.dp))
                            if (r.fieldChanges.isEmpty()) {
                                Text("No known fields changed.",
                                    color = Color(0xFF8A9BB5), fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace)
                            } else {
                                Text("Known fields changed:",
                                    color = Color(0, 255, 200), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                for (fc in r.fieldChanges) {
                                    Text("• ${fc.field.label}: ${fc.aValue} → ${fc.bValue}",
                                        color = Color(0, 255, 200), fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace)
                                }
                            }
                            if (r.candidates.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text("★ Species-ID candidates (short unknown changes):",
                                    color = Color(0xFFFFD54F), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                for (c in r.candidates) {
                                    Text("• page ${c.startPage}, offset 0x%04X (%d byte%s)".format(
                                        c.start, c.length, if (c.length == 1) "" else "s"),
                                        color = Color(0xFFFFD54F), fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // Scrollable range list — takes all remaining space.
                    // Each range card scrolls horizontally for wide hex rows.
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(r.ranges) { range -> RangeCard(range) }
                    }
                } else {
                    // No result yet — spacer to keep Done button at bottom.
                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { finish() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))
                ) { Text("Done") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun RangeCard(range: VBBraceletDiff.DiffRange) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF16283F), elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    (if (range.isCandidate) "★ " else "") +
                            "page ${range.startPage} · offset 0x%04X–0x%04X (%d byte%s)".format(
                                range.start, range.endExclusive - 1,
                                range.length, if (range.length == 1) "" else "s"),
                    color = if (range.isCandidate) Color(0xFFFFD54F) else Color.White,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                for (f in range.fields) {
                    Text("touches known field: ${f.label} (${f.id})",
                        color = Color(0, 255, 200), fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(4.dp))
                // Hex rows scroll horizontally if wider than the screen.
                val hScroll = rememberScrollState()
                Column(modifier = Modifier.horizontalScroll(hScroll)) {
                    // 16 bytes per row.
                    var off = range.start
                    var ai = 0
                    while (ai < range.length) {
                        val n = minOf(16, range.length - ai)
                        Text(
                            VBBraceletDiff.formatRow(
                                off,
                                range.aBytes.copyOfRange(ai, ai + n),
                                range.bBytes.copyOfRange(ai, ai + n)
                            ),
                            color = Color(0xFF8A9BB5), fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        off += n
                        ai += n
                    }
                }
            }
        }
    }
}
