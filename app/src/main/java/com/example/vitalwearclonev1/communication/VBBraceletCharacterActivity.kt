package com.example.vitalwearclonev1.communication

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

/**
 * Bracelet character backup / transfer screen (Phase 2).
 *
 *   1. "Read Character" (one tap): runs the official read session
 *      (op=1 -> PWD_AUTH -> read 864 bytes -> op=2), decrypts the
 *      UID-bound blob, verifies checksums and shows the known fields.
 *   2. Edit the known fields, then "Write Back" (two taps): tap 1 backs
 *      the bracelet up again, captures the anti-replay session ID and
 *      issues CHECK_DIM; tap 2 verifies the session, patches ONLY the
 *      edited fields into the fresh backup, re-encrypts, writes all
 *      pages and commits (op=4). A confirm dialog lists every change
 *      before anything is written. Unknown blob regions are preserved
 *      verbatim — strict read-modify-write.
 *
 * Writes are strictly user-initiated. Pages 0-7 are never touched apart
 * from the protocol's own page-6 status/operation slot.
 */
class VBBraceletCharacterActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    companion object {
        /** Open the screen with a saved backup loaded (see VBBraceletBackups). */
        const val EXTRA_BACKUP_ID = "bracelet_backup_id"
        /** When true (with EXTRA_BACKUP_ID), arm the write confirm immediately. */
        const val EXTRA_ARM_WRITE = "bracelet_arm_write"
    }

    private enum class UiState {
        Idle, ReadArmed, Reading, ReadDone,
        WriteTap1Armed, WriteTap1, WriteTap2Armed, WriteTap2, Finished
    }

    private var nfcAdapter: NfcAdapter? = null
    private val uiState = mutableStateOf(UiState.Idle)
    private val statusFlow = MutableStateFlow("Tap “Read Character”, then tap the bracelet (App Loglink mode).")
    private val errorFlow = MutableStateFlow<String?>(null)
    private val charState = mutableStateOf<VBBraceletData.BraceletCharacter?>(null)
    /** fieldId -> raw typed text (only entries the user changed). */
    private var editedText by mutableStateOf(mapOf<String, String>())
    private var tap1State: VBBraceletSession.Tap1State? = null
    private val showConfirm = mutableStateOf(false)
    private val confirmLines = mutableStateOf(listOf<String>())
    private val doneMessage = mutableStateOf<String?>(null)
    /** Non-null when editing a saved backup: write-back uses it as payload base. */
    private var backupPayload: VBBraceletData.BraceletCharacter? = null
    private var backupId: String? = null
    private val backupCount = mutableStateOf(0)
    /** The DIM number the bracelet must have active at tap 2 (set in the confirm dialog). */
    private var expectedDimId: Int? = null
    private val dimText = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "No NFC on this device!", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val loadId = intent.getStringExtra(EXTRA_BACKUP_ID)
        if (loadId != null) {
            val backup = VBBraceletBackups.get(this, loadId)
            if (backup != null) {
                backupPayload = VBBraceletBackups.toCharacter(backup)
                backupId = backup.id
                charState.value = backupPayload
                editedText = emptyMap()
                uiState.value = UiState.ReadDone
                statusFlow.value = "Loaded backup ${backup.dateStr} " +
                        "(${backup.productName}, UID ${backup.uidShort}). " +
                        "Edit fields, then Write Back to restore it to a bracelet."
                if (intent.getBooleanExtra(EXTRA_ARM_WRITE, false)) {
                    // Defer one frame so Compose is ready for the dialog.
                    window.decorView.post { armWrite() }
                }
            } else {
                statusFlow.value = "Backup not found — it may have been deleted."
            }
        }
        setContent { CharacterScreen() }
    }

    override fun onResume() {
        super.onResume()
        backupCount.value = try {
            VBBraceletBackups.list(this).size
        } catch (e: Exception) { 0 }
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) {
            Toast.makeText(this, "NFC must be enabled", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        } else {
            val options = Bundle()
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
            adapter.enableReaderMode(
                this, this,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                options
            )
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    // ------------------------------------------------------------------
    // NFC dispatch
    // ------------------------------------------------------------------

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        when (uiState.value) {
            UiState.ReadArmed -> doRead(tag)
            UiState.WriteTap1Armed -> doWriteTap1(tag)
            UiState.WriteTap2Armed -> doWriteTap2(tag)
            else -> Unit // not armed: ignore stray taps
        }
    }

    private fun withNfc(tag: Tag, block: (NfcA) -> Unit) {
        val nfcA = NfcA.get(tag)
        if (nfcA == null) {
            errorFlow.value = "Tag is not NFC-A — tap the bracelet in App Loglink mode."
            uiState.value = UiState.Idle
            return
        }
        try {
            nfcA.connect()
            nfcA.timeout = 10000
            nfcA.use(block)
        } catch (e: Exception) {
            Timber.e(e, "NFC connection failed")
            errorFlow.value = "NFC connection failed: ${e.message}"
            uiState.value = UiState.Idle
        }
    }

    private fun progress(text: String) {
        statusFlow.value = text
    }

    private fun doRead(tag: Tag) {
        uiState.value = UiState.Reading
        errorFlow.value = null
        doneMessage.value = null
        withNfc(tag) { nfc ->
            when (val r = VBBraceletSession.readCharacter(nfc, tag, ::progress)) {
                is VBBraceletSession.ReadResult.Ok -> {
                    charState.value = r.character
                    editedText = emptyMap()
                    backupPayload = null
                    backupId = null
                    uiState.value = UiState.ReadDone
                    val present = VBBraceletData.hasCharacterData(r.character.plain, r.character.productId)
                    val saved = try {
                        val b = VBBraceletBackups.save(this@VBBraceletCharacterActivity, r.character)
                        val hm = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(b.savedAt))
                        "\nBackup saved ✓ $hm — find it in DigiLab → Bracelet Backups."
                    } catch (e: Exception) {
                        Timber.e(e, "auto-save backup failed")
                        "\n⚠ Backup auto-save failed: ${e.message}"
                    }
                    // op=2 is the official "transfer complete" signal; the Hero's
                    // firmware clears the character slot (anti-dupe). Say so plainly.
                    val slotMsg = when (r.slotEmpty) {
                        true -> "\n⚠ Bracelet slot is now EMPTY — the Hero cleared it on " +
                                "transfer (anti-dupe). Your Digimon now lives only in " +
                                "this backup. Write it back to restore it to the bracelet."
                        false -> "\nBracelet slot still reports character data present."
                        null -> "\n(Slot re-check failed — op=2 was ACKed, so assume the " +
                                "Digimon moved to this app backup.)"
                    }
                    statusFlow.value = "Read OK — ${r.character.fields.size} known fields parsed " +
                            "(character present: $present).$slotMsg$saved"
                }
                is VBBraceletSession.ReadResult.Err -> {
                    errorFlow.value = "Read failed [${r.step}]: ${r.message}"
                    uiState.value = UiState.Idle
                    statusFlow.value = "Read failed — see error above. You can try again."
                }
            }
        }
    }

    private fun doWriteTap1(tag: Tag) {
        uiState.value = UiState.WriteTap1
        errorFlow.value = null
        // Writing from a saved backup: the bracelet may legitimately be empty.
        val fromBackup = backupPayload != null
        val dim = expectedDimId
        if (dim == null) {
            errorFlow.value = "No DIM number set — open Write Back again and enter one."
            uiState.value = UiState.ReadDone
            return
        }
        withNfc(tag) { nfc ->
            when (val r = VBBraceletSession.writeTap1(
                nfc, tag, ::progress,
                requireCharacterData = !fromBackup,
                expectedDimId = dim
            )) {
                is VBBraceletSession.Tap1Result.Ok -> {
                    tap1State = r.state
                    uiState.value = UiState.WriteTap2Armed
                    statusFlow.value = "Tap 1 OK — backup taken, session locked. " +
                            "Insert DIM #$dim into the bracelet, wait for it to load, " +
                            "then press “Tap 2: Write” and tap the bracelet again."
                }
                is VBBraceletSession.Tap1Result.Err -> {
                    errorFlow.value = "Write tap 1 failed [${r.step}]: ${r.message}"
                    uiState.value = UiState.ReadDone
                    statusFlow.value = "Tap 1 failed — nothing was written. You can retry Write Back."
                }
            }
        }
    }

    private fun doWriteTap2(tag: Tag) {
        val tap1 = tap1State
        if (tap1 == null) {
            errorFlow.value = "Lost tap-1 state — start Write Back again."
            uiState.value = UiState.ReadDone
            return
        }
        val edits = currentEdits() ?: return // validation errors already shown
        uiState.value = UiState.WriteTap2
        errorFlow.value = null
        // Restoring a saved backup: write the saved blob (with edits on top),
        // re-encrypted for this bracelet's UID — not the tap-1 backup.
        val payloadBase = backupPayload?.plain
        withNfc(tag) { nfc ->
            when (val r = VBBraceletSession.writeTap2(nfc, tag, tap1, edits, ::progress, payloadBase = payloadBase)) {
                is VBBraceletSession.WriteResult.Ok -> {
                    uiState.value = UiState.Finished
                    tap1State = null
                    expectedDimId = null
                    editedText = emptyMap()
                    val lines = if (r.changes.isEmpty()) "no field values actually changed"
                    else r.changes.joinToString("\n")
                    doneMessage.value = "✓ Write complete — ${r.pagesWritten} pages written.\n$lines"
                    statusFlow.value = "Done. The bracelet now holds the updated character."
                }
                is VBBraceletSession.WriteResult.Err -> {
                    // DIM problems are retryable in place: the user inserts the
                    // DIM and taps again without redoing tap 1.
                    val retryableDim = r.step == "dim"
                    errorFlow.value = "Write tap 2 failed [${r.step}] after ${r.pagesWritten} pages: ${r.message}\n" +
                            if (retryableDim) {
                                "Nothing was written (the check runs before any page " +
                                        "writes). Insert the DIM, wait for it to load, then tap again."
                            } else {
                                "The commit did NOT run, so the bracelet should still hold its previous data. " +
                                        "Re-run Write Back with no edits to restore the backup exactly."
                            }
                    uiState.value = if (retryableDim) UiState.WriteTap2Armed else UiState.ReadDone
                    statusFlow.value = if (retryableDim) {
                        "Tap 2 armed — insert the DIM, wait for it to load, then tap the bracelet."
                    } else {
                        "Write failed — nothing was committed. See error above."
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Edit handling
    // ------------------------------------------------------------------

    /** Parse + validate the user's typed edits. Null = invalid (error shown). */
    private fun currentEdits(): Map<String, Int>? {
        val ch = charState.value ?: return emptyMap()
        val out = mutableMapOf<String, Int>()
        for ((id, text) in editedText) {
            val field = VBBraceletData.FIELDS.firstOrNull { it.id == id } ?: continue
            val v = text.toIntOrNull()
            if (v == null) {
                errorFlow.value = "“${field.label}” is not a number."
                return null
            }
            val max = if (field.type == VBBraceletData.FieldType.U8) 255 else 65535
            if (v !in 0..max) {
                errorFlow.value = "“${field.label}” must be 0–$max."
                return null
            }
            if (field.cap != null && v > field.cap) {
                errorFlow.value = "“${field.label}” max is ${field.cap}."
                return null
            }
            val current = ch.fields.firstOrNull { it.field.id == id }?.value
            if (current != v) out[id] = v
        }
        return out
    }

    private fun armWrite() {
        val edits = currentEdits() ?: return
        val fromBackup = backupPayload != null
        if (edits.isEmpty() && !fromBackup) {
            errorFlow.value = "No fields changed — edit a value first."
            return
        }
        val ch = charState.value ?: return
        // Prefill the DIM number from the character's origin DIM when sane.
        val origin = ch.fields.firstOrNull { it.field.id == "originDimId" }?.value
        val maxDim = if (ch.productId == 4) 65534 else 254
        dimText.value = if (origin != null && origin in 0..maxDim) origin.toString() else ""
        confirmLines.value = if (edits.isEmpty()) {
            listOf("Restore the saved backup exactly (no field changes).")
        } else edits.map { (id, v) ->
            val field = VBBraceletData.FIELDS.first { it.id == id }
            val old = ch.fields.first { it.field.id == id }.value
            "${field.label}: $old → $v"
        }
        errorFlow.value = null
        showConfirm.value = true
    }

    /** Validate the dialog's DIM input; null = invalid (error shown). */
    private fun parseDimInput(productId: Int): Int? {
        val v = dimText.value.trim().toIntOrNull()
        val maxDim = if (productId == 4) 65534 else 254
        if (v == null || v !in 0..maxDim) {
            errorFlow.value = "Enter the DIM card number you will insert (0–$maxDim)."
            return null
        }
        return v
    }

    // ------------------------------------------------------------------
    // UI
    // ------------------------------------------------------------------

    @Composable
    private fun CharacterScreen() {
        val state by uiState
        val status by statusFlow.collectAsState()
        val error by errorFlow.collectAsState()
        val ch = charState.value
        val done = doneMessage.value
        val confirm by showConfirm
        val busy = state == UiState.Reading || state == UiState.WriteTap1 || state == UiState.WriteTap2

        Box(
            modifier = Modifier.fillMaxSize().background(Color(0, 20, 40)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Bracelet Character",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Backup a bracelet character, edit known fields, write it back. Read-modify-write only.",
                    color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                if (busy) {
                    CircularProgressIndicator(color = Color.Cyan)
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    status,
                    color = Color(0, 255, 200), fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace, textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error!!,
                        color = Color(0xFFFF6B6B), fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace, textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (done != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        done,
                        color = Color(0xFF9DFF9D), fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace, textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (ch != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Character fields (product ${ch.productId} — ${VBBraceletAuth.productName(ch.productId)})",
                        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    for (fv in ch.fields) {
                        FieldRow(fv, ch.productId)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Decrypted blob (864 bytes, hex):",
                        color = Color.Gray, fontSize = 12.sp
                    )
                    Text(
                        VBBraceletData.hexDump(ch.plain),
                        color = Color(0xFF8A9BB5), fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))
                when (state) {
                    UiState.Idle, UiState.Finished -> {
                        Button(
                            onClick = {
                                errorFlow.value = null
                                uiState.value = UiState.ReadArmed
                                statusFlow.value = "Armed — tap the bracelet now."
                            }
                        ) { Text("Read Character from Bracelet") }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                startActivity(Intent(this@VBBraceletCharacterActivity, VBBraceletBackupsActivity::class.java))
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6A4C93))
                        ) { Text("Saved Backups (${backupCount.value})") }
                    }
                    UiState.ReadArmed -> {
                        Button(
                            onClick = { uiState.value = UiState.Idle },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555))
                        ) { Text("Cancel") }
                    }
                    UiState.Reading -> Unit
                    UiState.ReadDone -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { armWrite() }) { Text("Write Back to Bracelet") }
                            Button(
                                onClick = {
                                    charState.value = null
                                    backupPayload = null
                                    backupId = null
                                    expectedDimId = null
                                    editedText = emptyMap()
                                    uiState.value = UiState.Idle
                                    statusFlow.value = "Tap “Read Character”, then tap the bracelet."
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555))
                            ) { Text("Discard") }
                        }
                        Text(
                            "Write Back = 2 taps. Tap 1 backs up + asks for the DIM; " +
                                    "tap 2 needs that DIM inserted and loaded, then writes + commits.",
                            color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    UiState.WriteTap1Armed -> {
                        Text("Tap 1 armed — tap the bracelet.", color = Color.Yellow, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { uiState.value = UiState.ReadDone },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555))
                        ) { Text("Cancel") }
                    }
                    UiState.WriteTap1 -> Unit
                    UiState.WriteTap2Armed -> {
                        Button(onClick = { armTap2() }) { Text("Tap 2: Write to Bracelet") }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                tap1State = null
                                uiState.value = UiState.ReadDone
                                statusFlow.value = "Write cancelled before tap 2 — nothing was written."
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555))
                        ) { Text("Cancel Write") }
                    }
                    UiState.WriteTap2 -> Unit
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { finish() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))
                ) { Text("Done") }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (confirm) {
            val dimInput = dimText.value
            AlertDialog(
                onDismissRequest = { showConfirm.value = false },
                title = { Text("Write to bracelet?") },
                text = {
                    Column {
                        Text("This writes the following changes to your bracelet (tap 1 backs up first):")
                        Spacer(Modifier.height(8.dp))
                        for (line in confirmLines.value) {
                            Text("• $line", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "The bracelet needs a DIM inserted to receive a Digimon. " +
                                    "Enter the number on the DIM card you will insert:",
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        TextField(
                            value = dimInput,
                            onValueChange = { dimText.value = it.filter { c -> c.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            placeholder = { Text("DIM number") },
                            modifier = Modifier.width(140.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val productId = ch?.productId ?: 2
                        val dim = parseDimInput(productId) ?: return@TextButton
                        expectedDimId = dim
                        showConfirm.value = false
                        uiState.value = UiState.WriteTap1Armed
                        statusFlow.value = "Tap 1 armed — tap the bracelet."
                    }) { Text("Write (2 taps)") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm.value = false }) { Text("Cancel") }
                }
            )
        }
    }

    private fun armTap2() {
        // No-op: the tap-2 arm is the state itself; this keeps the button honest.
        uiState.value = UiState.WriteTap2Armed
        statusFlow.value = "Tap 2 armed — tap the bracelet now."
    }

    @Composable
    private fun FieldRow(fv: VBBraceletData.FieldValue, productId: Int) {
        val field = fv.field
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(field.label, color = Color.White, fontSize = 14.sp)
                if (field.hint.isNotEmpty()) {
                    Text(field.hint, color = Color.Gray, fontSize = 11.sp)
                }
            }
            if (field.editable) {
                val text = editedText[field.id] ?: fv.value.toString()
                TextField(
                    value = text,
                    onValueChange = { editedText = editedText + (field.id to it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(110.dp)
                )
            } else {
                Text(
                    fv.value.toString(),
                    color = Color(0xFF8A9BB5), fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(110.dp))
            }
        }
    }
}
