package com.example.vitalwearclonev1.communication

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

/**
 * Diagnostic tap-reader for a real Vital Bracelet in Connect -> App Loglink
 * mode. Shows the raw tag bytes the bracelet presents plus the parsed
 * protocol fields, so odd bracelet behavior (timeouts, "failed" prompts)
 * can be diagnosed from what the tag actually said instead of guessed at.
 *
 * Reads pages 0-3 (UID/header area, raw only) and pages 4-7 (the protocol
 * window parsed by [VBBraceletTag]). Read-only: never writes to the tag.
 * Stays open and re-reads on every tap so reads can be compared
 * (e.g. before vs after inserting a card into the bracelet).
 */
class VBBraceletTagReaderActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private val reportFlow = MutableStateFlow("Waiting for a tap…")
    private val tapCount = mutableStateOf(0)
    private val authArmed = mutableStateOf(false)
    private val authReportFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "No NFC on this device!", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContent { ReaderScreen() }
    }

    override fun onResume() {
        super.onResume()
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

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        if (authArmed.value) {
            authArmed.value = false
            runAuthProbe(tag)
            return
        }
        try {
            val nfc = MifareUltralight.get(tag)
            if (nfc == null) {
                runOnUiThread {
                    Toast.makeText(this, "That's not the bracelet — tap your Hero in App Loglink mode.", Toast.LENGTH_LONG).show()
                }
                return
            }
            nfc.connect()
            val report = nfc.use { readReport(it) }
            tapCount.value = tapCount.value + 1
            reportFlow.value = "Tap #${tapCount.value}\n\n$report"
        } catch (e: Exception) {
            Timber.e(e, "NFC error during bracelet tag read")
            runOnUiThread {
                Toast.makeText(this, "NFC error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Phase-1 login probe: read the UID + header over NfcA, derive the
     * official-app PWD_AUTH password, send the single 0x1B auth command and
     * report the bracelet's answer. Read-only apart from the auth command.
     */
    private fun runAuthProbe(tag: Tag) {
        authReportFlow.value = "Probing…"
        val sb = StringBuilder()
        try {
            val nfcA = NfcA.get(tag)
            if (nfcA == null) {
                authReportFlow.value = "Tag is not NFC-A — cannot test login."
                return
            }
            nfcA.connect()
            nfcA.use { nfc ->
                val uid = tag.id
                sb.append("UID (${uid.size} bytes): ${uid.toHex()}\n")
                if (uid.size != 7) {
                    sb.append("UID is not 7 bytes — password derivation needs exactly 7.\n")
                    authReportFlow.value = sb.toString()
                    return@use
                }
                val header = nfc.transceive(byteArrayOf(0x30, 0x04))
                if (header.size < 16) {
                    sb.append("Header read returned ${header.size} bytes (expected 16).\n")
                    authReportFlow.value = sb.toString()
                    return@use
                }
                val productId = header[5].toInt() and 0xFF
                sb.append("Product ID: $productId (${VBBraceletAuth.productName(productId)})\n")
                if (!VBBraceletAuth.supportsProduct(productId)) {
                    sb.append("No known login keys for this product — not attempting login.\n")
                    authReportFlow.value = sb.toString()
                    return@use
                }
                sb.append("Sending PWD_AUTH (0x1B)…\n")
                when (val r = VBBraceletAuth.pwdAuth(nfc, uid, productId)) {
                    is VBBraceletAuth.PwdAuthResult.Success -> {
                        sb.append("\n\u2713 LOGIN ACCEPTED — PACK: ${r.pack.toHex()}\n")
                        sb.append("The bracelet speaks the transfer protocol.\n")
                        if (!r.ackOk) sb.append("(note: first PACK byte was not 0x0A)\n")
                    }
                    is VBBraceletAuth.PwdAuthResult.Nak -> {
                        sb.append("\n\u2717 LOGIN REJECTED — NAK: ${r.response.toHex()}\n")
                        sb.append("The bracelet does not accept this login.\n")
                    }
                    is VBBraceletAuth.PwdAuthResult.Error ->
                        sb.append("\n\u2717 ERROR: ${r.message}\n")
                }
                authReportFlow.value = sb.toString()
            }
        } catch (e: Exception) {
            Timber.e(e, "Auth probe failed")
            sb.append("\n\u2717 ERROR: ${e.message}\n")
            authReportFlow.value = sb.toString()
        }
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    private fun readReport(nfc: MifareUltralight): String {
        // Pages 0-3: UID + lock bytes (raw only — standard Ultralight layout).
        val header = nfc.transceive(byteArrayOf(0x30, 0x00))
        // Pages 4-7: the bracelet protocol window (parsed by VBBraceletTag).
        val proto = VBBraceletTag(nfc)
        val productId = proto.productId
        val sb = StringBuilder()
        sb.append("Product ID: $productId (${VBBraceletAuth.productName(productId)})\n")
        sb.append("Login keys: ${if (VBBraceletAuth.supportsProduct(productId)) "available" else "NOT available"}\n")
        sb.append("\nPages 0-3 (UID/header):\n")
        sb.append(hexDump(header, 0))
        sb.append("\nPages 4-7 (protocol):\n")
        sb.append(hexDump(proto.raw, 4))
        sb.append("\nParsed:\n")
        sb.append("  magic      = 0x${proto.magic.toString(16).uppercase()} (${proto.magic})\n")
        sb.append("  itemId     = ${proto.itemId} ${if (proto.itemId.toInt() == 4) "(BE family)" else "(classic)"}\n")
        sb.append("  itemNumber = ${proto.itemNumber}\n")
        sb.append("  status     = 0x${"%02X".format(proto.status)} ${describeStatus(proto.status)}\n")
        sb.append("  operation  = ${proto.operation} ${describeOperation(proto.operation)}\n")
        sb.append("  dimId      = ${proto.dimId}\n")
        return sb.toString()
    }

    private fun hexDump(bytes: ByteArray, firstPage: Int): String {
        val sb = StringBuilder()
        for (page in 0 until bytes.size / 4) {
            sb.append("  p${firstPage + page}: ")
            for (i in 0 until 4) {
                sb.append("%02X".format(bytes[page * 4 + i]))
                if (i < 3) sb.append(" ")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun describeStatus(status: Byte): String {
        val flags = mutableListOf<String>()
        if (status.toInt() and 0x01 != 0) flags.add("ready")
        if (status.toInt() and 0x02 != 0) flags.add("dim-ready")
        return if (flags.isEmpty()) "(no flags)" else "(${flags.joinToString(", ")})"
    }

    private fun describeOperation(op: Byte): String = when (op.toInt()) {
        1 -> "(READY)"
        3 -> "(CHECK_DIM request pending)"
        else -> "(unknown)"
    }

    @Composable
    private fun ReaderScreen() {
        val report by reportFlow.collectAsState()
        val taps by tapCount
        val armed by authArmed
        val authReport by authReportFlow.collectAsState()
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0, 20, 40)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Bracelet Tag Reader",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Put the bracelet in Connect → App Loglink, then tap it to the phone. Read-only — nothing is written.",
                    color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                if (taps == 0 && authReport == null) {
                    CircularProgressIndicator(color = Color.Cyan)
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    report,
                    color = Color(0, 255, 200),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxSize()
                )
                if (authReport != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Bracelet Login Test",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        authReport!!,
                        color = Color(0xFFFFD54F),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            authArmed.value = true
                            authReportFlow.value = "Armed — tap the bracelet now (App Loglink mode)."
                        },
                        enabled = !armed
                    ) {
                        Text(if (armed) "Tap the bracelet…" else "Test Bracelet Login")
                    }
                    Button(onClick = { finish() }) {
                        Text("Done")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        startActivity(Intent(this, VBBraceletCharacterActivity::class.java))
                    }
                ) {
                    Text("Character Backup / Transfer")
                }
            }
        }
    }
}
