package com.example.vitalwearclonev1.communication

import android.nfc.Tag
import android.nfc.tech.NfcA
import timber.log.Timber
import java.io.IOException

/**
 * The full bracelet character transfer session, reverse-engineered from the
 * official Vital Bracelet Arena APK (jp/co/bandai/vb_nfc/MainActivity).
 *
 * Phone is a plain NFC-A reader; the bracelet (Connect -> App Loglink)
 * presents a Mifare Ultralight-compatible tag. Raw commands only:
 *   READ      30 <page>   -> 16 bytes (4 pages)
 *   WRITE     A2 <page> <4 bytes> -> 1-byte ACK 0x0A
 *   PWD_AUTH  1B <4 bytes> -> 2-byte PACK on success
 *
 * NEVER writes pages 0-7 except the protocol's own page-6 status/operation
 * slot. (The retired VBNfcProtocol path wrote page 2's lock-byte area —
 * the official protocol never does.)
 *
 * Sessions:
 *   READ      (1 tap):  header -> op=1 -> PWD_AUTH -> read pages 8-223
 *                       (864 bytes) -> decrypt + checksum verify -> op=2
 *   WRITE tap1:        header -> capture 3-byte session ID -> op=1 ->
 *                       PWD_AUTH -> backup pages 8-223 -> op=3 CHECK_DIM
 *   WRITE tap2:        header -> session ID MUST match tap 1 -> op=1 ->
 *                       PWD_AUTH -> patch fields -> encrypt -> write pages
 *                       8-223 -> op=4 commit
 *
 * Page-6 status writes (exact official layout):
 *   classic (2/3): A2 06 h[8] h[9] <op> h[11]      (op at byte 2)
 *   VBBE    (4):   A2 06 h[8] <op> h[10] h[11]      (op at byte 1)
 *   CHECK_DIM carries the dimId: classic -> byte 1 (1 byte), VBBE -> bytes 2-3.
 */
object VBBraceletSession {

    const val DATA_FIRST_PAGE = 8
    const val DATA_LAST_PAGE = 223
    const val DATA_SIZE = 864
    private const val ACK: Byte = 0x0A
    private const val MAX_RETRIES = 5

    const val OP_SESSION_START = 1
    const val OP_READ_DONE = 2
    const val OP_CHECK_DIM = 3
    const val OP_WRITE_COMMIT = 4

    private val MAGIC = byteArrayOf(0x42, 0x41, 0x4E, 0x54) // "BANT"

    data class Header(
        val raw: ByteArray,
        val productId: Int,
        val deviceId: Int,
        val status: Int,
        val operation: Int,
        val dimId: Int,
        val sessionId: ByteArray
    )

    /** Parse + validate the 16-byte header (READ page 4). */
    fun parseHeader(raw: ByteArray): Header {
        require(raw.size >= 16) { "header too short: ${raw.size} bytes" }
        for (i in 0..3) require(raw[i] == MAGIC[i]) {
            "bad magic: ${raw.take(4).joinToString(" ") { "%02X".format(it) }}"
        }
        val productId = raw[5].toInt() and 0xFF
        require(productId == 2 || productId == 3 || productId == 4) {
            "unsupported product ID: $productId"
        }
        val deviceId = ((raw[6].toInt() and 0xFF) shl 8) or (raw[7].toInt() and 0xFF)
        val status = raw[8].toInt() and 0xFF
        val (operation, dimId) = if (productId == 4) {
            Pair(
                raw[9].toInt() and 0xFF,
                ((raw[10].toInt() and 0xFF) shl 8) or (raw[11].toInt() and 0xFF)
            )
        } else {
            Pair(raw[10].toInt() and 0xFF, raw[9].toInt() and 0xFF)
        }
        return Header(
            raw.copyOf(), productId, deviceId, status, operation, dimId,
            raw.copyOfRange(13, 16)
        )
    }

    // ------------------------------------------------------------------
    // Low-level commands (all with the official retry policy)
    // ------------------------------------------------------------------

    private fun tx(nfc: NfcA, cmd: ByteArray, expectLen: Int, what: String): ByteArray {
        var last: IOException? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val r = nfc.transceive(cmd)
                if (expectLen >= 0 && r.size != expectLen)
                    throw IOException("$what: reply ${r.size} bytes, expected $expectLen")
                return r
            } catch (e: IOException) {
                last = e
                Timber.w("VBBraceletSession $what attempt ${attempt + 1}/$MAX_RETRIES: ${e.message}")
            }
        }
        throw IOException("$what failed after $MAX_RETRIES attempts: ${last?.message}", last)
    }

    /** READ 4 pages starting at [page] -> 16 bytes. */
    private fun read4(nfc: NfcA, page: Int): ByteArray =
        tx(nfc, byteArrayOf(0x30, page.toByte()), 16, "READ page $page")

    /** WRITE one page -> expects single-byte ACK 0x0A. */
    private fun writePage(nfc: NfcA, page: Int, data: ByteArray, what: String) {
        require(data.size == 4) { "page write needs exactly 4 bytes" }
        val r = tx(
            nfc,
            byteArrayOf(0xA2.toByte(), page.toByte(), data[0], data[1], data[2], data[3]),
            1, what
        )
        if (r[0] != ACK) throw IOException("$what: no ACK (got ${"%02X".format(r[0])})")
    }

    /** Official page-6 status/operation write. */
    private fun page6(nfc: NfcA, hdr: Header, op: Int, dimId: Int = -1, what: String) {
        val h = hdr.raw
        val payload = if (hdr.productId == 4) {
            if (op == OP_CHECK_DIM && dimId >= 0)
                byteArrayOf(h[8], op.toByte(), (dimId shr 8).toByte(), dimId.toByte())
            else byteArrayOf(h[8], op.toByte(), h[10], h[11])
        } else {
            if (op == OP_CHECK_DIM && dimId >= 0)
                byteArrayOf(h[8], dimId.toByte(), op.toByte(), h[11])
            else byteArrayOf(h[8], h[9], op.toByte(), h[11])
        }
        Timber.d("VBBraceletSession page6 op=$op payload=${payload.toHex()}")
        writePage(nfc, 6, payload, what)
    }

    /** PWD_AUTH with the derived password; NAK fails fast, I/O errors retry. */
    private fun pwdAuth(nfc: NfcA, uid: ByteArray, productId: Int) {
        var lastErr: String? = null
        repeat(3) { attempt ->
            when (val r = VBBraceletAuth.pwdAuth(nfc, uid, productId)) {
                is VBBraceletAuth.PwdAuthResult.Success -> {
                    Timber.d("VBBraceletSession PWD_AUTH ok, PACK=${r.pack.toHex()}")
                    return
                }
                is VBBraceletAuth.PwdAuthResult.Nak ->
                    throw IOException("bracelet rejected login (NAK ${r.response.toHex()})")
                is VBBraceletAuth.PwdAuthResult.Error -> {
                    lastErr = r.message
                    Timber.w("VBBraceletSession PWD_AUTH attempt ${attempt + 1}/3: ${r.message}")
                }
            }
        }
        throw IOException("login failed: $lastErr")
    }

    /** Read the full 864-byte encrypted blob (54 x 16-byte reads). */
    private fun readData(nfc: NfcA, progress: (String) -> Unit): ByteArray {
        val out = ByteArray(DATA_SIZE)
        var page = DATA_FIRST_PAGE
        var i = 0
        while (page <= DATA_LAST_PAGE) {
            val chunk = read4(nfc, page)
            System.arraycopy(chunk, 0, out, i * 16, 16)
            i++
            if (i % 12 == 0) progress("Reading… ${i * 16}/$DATA_SIZE bytes")
            page += 4
        }
        return out
    }

    // ------------------------------------------------------------------
    // Public sessions
    // ------------------------------------------------------------------

    sealed interface ReadResult {
        data class Ok(val character: VBBraceletData.BraceletCharacter) : ReadResult
        data class Err(val step: String, val message: String) : ReadResult
    }

    /** Single-tap backup/read session. */
    fun readCharacter(nfc: NfcA, tag: Tag, progress: (String) -> Unit): ReadResult {
        try {
            val uid = tag.id
            if (uid.size != 7) return ReadResult.Err("uid", "UID is ${uid.size} bytes, need 7")
            progress("Reading header…")
            val hdr = parseHeader(read4(nfc, 4))
            if (hdr.status and 0x01 == 0)
                return ReadResult.Err("header", "bracelet not ready (status ${"%02X".format(hdr.status)})")
            progress("Starting session…")
            page6(nfc, hdr, OP_SESSION_START, what = "op=1 session start")
            progress("Logging in…")
            pwdAuth(nfc, uid, hdr.productId)
            progress("Reading character data…")
            val enc = readData(nfc, progress)
            progress("Decrypting + verifying…")
            val plain = VBBraceletData.dataCrypt(uid, hdr.productId, enc)
            val bad = VBBraceletData.verifyChecksums(plain)
            if (bad.isNotEmpty())
                return ReadResult.Err("checksum", "checksum error on pages ${bad.joinToString(", ")}")
            page6(nfc, hdr, OP_READ_DONE, what = "op=2 read done")
            val ch = VBBraceletData.BraceletCharacter(
                hdr.productId, uid.copyOf(), plain,
                VBBraceletData.parseFields(plain, hdr.productId)
            )
            Timber.d("VBBraceletSession read ok (product ${hdr.productId})")
            return ReadResult.Ok(ch)
        } catch (e: Exception) {
            Timber.e(e, "VBBraceletSession.readCharacter failed")
            return ReadResult.Err("error", e.message ?: e.toString())
        }
    }

    data class Tap1State(
        val uid: ByteArray,
        val productId: Int,
        val deviceId: Int,
        val dimId: Int,
        val sessionId: ByteArray,
        /** Decrypted 864-byte backup taken on tap 1 — the patch base. */
        val backupPlain: ByteArray
    )

    sealed interface Tap1Result {
        data class Ok(val state: Tap1State) : Tap1Result
        data class Err(val step: String, val message: String) : Tap1Result
    }

    /** Write tap 1: backup the bracelet, capture the session ID, ask for the DIM. */
    fun writeTap1(nfc: NfcA, tag: Tag, progress: (String) -> Unit): Tap1Result {
        try {
            val uid = tag.id
            if (uid.size != 7) return Tap1Result.Err("uid", "UID is ${uid.size} bytes, need 7")
            progress("Reading header…")
            val hdr = parseHeader(read4(nfc, 4))
            if (hdr.status and 0x01 == 0)
                return Tap1Result.Err("header", "bracelet not ready (status ${"%02X".format(hdr.status)})")
            progress("Starting session…")
            page6(nfc, hdr, OP_SESSION_START, what = "op=1 session start")
            progress("Logging in…")
            pwdAuth(nfc, uid, hdr.productId)
            progress("Backing up bracelet data…")
            val enc = readData(nfc, progress)
            val plain = VBBraceletData.dataCrypt(uid, hdr.productId, enc)
            val bad = VBBraceletData.verifyChecksums(plain)
            if (bad.isNotEmpty())
                return Tap1Result.Err("checksum", "backup checksum error on pages ${bad.joinToString(", ")}")
            if (!VBBraceletData.hasCharacterData(plain, hdr.productId))
                return Tap1Result.Err("backup", "no character data on the bracelet to modify")
            progress("Requesting DIM check…")
            page6(nfc, hdr, OP_CHECK_DIM, hdr.dimId, "op=3 CHECK_DIM")
            val state = Tap1State(
                uid.copyOf(), hdr.productId, hdr.deviceId, hdr.dimId,
                hdr.sessionId.copyOf(), plain
            )
            Timber.d("VBBraceletSession tap1 ok (sessionId=${hdr.sessionId.toHex()} dimId=${hdr.dimId})")
            return Tap1Result.Ok(state)
        } catch (e: Exception) {
            Timber.e(e, "VBBraceletSession.writeTap1 failed")
            return Tap1Result.Err("error", e.message ?: e.toString())
        }
    }

    sealed interface WriteResult {
        data class Ok(val pagesWritten: Int, val changes: List<String>) : WriteResult
        data class Err(val step: String, val message: String, val pagesWritten: Int) : WriteResult
    }

    /**
     * Write tap 2: verify the session, patch ONLY [edits] into the tap-1
     * backup, re-encrypt for this tap's UID, write all pages, commit.
     */
    fun writeTap2(
        nfc: NfcA,
        tag: Tag,
        tap1: Tap1State,
        edits: Map<String, Int>,
        progress: (String) -> Unit
    ): WriteResult {
        var pagesWritten = 0
        try {
            val uid = tag.id
            if (uid.size != 7) return WriteResult.Err("uid", "UID is ${uid.size} bytes, need 7", 0)
            progress("Reading header…")
            val hdr = parseHeader(read4(nfc, 4))
            if (hdr.productId != tap1.productId)
                return WriteResult.Err("header", "product changed between taps", 0)
            if (hdr.deviceId != tap1.deviceId)
                return WriteResult.Err("header", "a different bracelet was tapped", 0)
            if (!hdr.sessionId.contentEquals(tap1.sessionId))
                return WriteResult.Err("session", "session ID mismatch — start tap 1 again", 0)
            if (hdr.dimId != tap1.dimId)
                return WriteResult.Err("header", "DIM changed (was ${tap1.dimId}, now ${hdr.dimId})", 0)
            if (hdr.status and 0x01 == 0)
                return WriteResult.Err("header", "bracelet not ready", 0)
            progress("Patching ${edits.size} field(s)…")
            val plain = tap1.backupPlain.copyOf()
            val changes = VBBraceletData.applyEdits(plain, tap1.productId, edits)
            progress("Logging in…")
            page6(nfc, hdr, OP_SESSION_START, what = "op=1 session start")
            pwdAuth(nfc, uid, tap1.productId)
            val enc = VBBraceletData.dataCrypt(uid, tap1.productId, plain)
            progress("Writing character data…")
            var page = DATA_FIRST_PAGE
            var i = 0
            while (page <= DATA_LAST_PAGE) {
                writePage(nfc, page, enc.copyOfRange(i * 4, i * 4 + 4), "write page $page")
                pagesWritten++
                i++
                if (i % 24 == 0) progress("Writing… $pagesWritten/216 pages")
                page++
            }
            page6(nfc, hdr, OP_WRITE_COMMIT, what = "op=4 commit")
            Timber.d("VBBraceletSession write ok ($pagesWritten pages)")
            return WriteResult.Ok(pagesWritten, changes)
        } catch (e: Exception) {
            Timber.e(e, "VBBraceletSession.writeTap2 failed")
            return WriteResult.Err("error", e.message ?: e.toString(), pagesWritten)
        }
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}
