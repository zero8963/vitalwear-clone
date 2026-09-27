package com.example.vitalwearclonev1.communication

import android.content.Context
import android.nfc.tech.MifareUltralight
import com.example.vitalwearclonev1.common.util.Endian
import com.example.vitalwearclonev1.common.util.getUInt16
import com.example.vitalwearclonev1.common.util.getUInt32
import com.example.vitalwearclonev1.common.util.toByteArray
import timber.log.Timber
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * The tag-memory protocol a real Vital Bracelet (BE family, including the
 * Vital Hero) speaks when the user puts it in Connect -> App Loglink mode.
 * In that mode the bracelet's own firmware presents itself as a Mifare
 * Ultralight tag, so the phone only needs plain NFC-reader commands —
 * no HCE, no auth, no custom cipher.
 *
 * Ported from 457R0/VitalWear's VBNfcData.kt (release tag "companion"),
 * which is proven against real hardware. Command bytes, page layout and
 * op codes are mirrored exactly.
 *
 * Page-4 READ (16 bytes = pages 4-7):
 *   bytes 0-3 : magic (UInt32 BE)
 *   bytes 4-5 : itemId (UInt16 BE; 4 = BE card family)
 *   bytes 6-7 : itemNumber (UInt16 BE)
 *   byte  8   : status (bit0 = ready, bit1 = dim-ready)
 *   BE layout : byte 9 = operation, bytes 10-11 = dimId (UInt16 BE)
 *   classic   : byte 9 = dimId (1 byte), byte 10 = operation
 *
 * Validation is a two-tap handshake:
 *   tap 1 -> writeCardCheck(): phone writes [READY, CHECK_DIM, dimId] to page 6
 *   (user inserts the physical card into the bracelet)
 *   tap 2 -> wasCardIdValidated(): phone re-reads page 4 and confirms the
 *   bracelet reports the same dimId with operation == READY and both status flags.
 */
class VBBraceletTag(nfc: MifareUltralight) {

    companion object {
        private const val ITEM_ID_BE: UShort = 4u
        private const val STATUS_READY_FLAG: Byte = 0b00000001
        private const val STATUS_DIM_READY_FLAG: Byte = 0b00000010
        private val STATUS_DIM_IS_READY = STATUS_READY_FLAG or STATUS_DIM_READY_FLAG
        private const val OPERATION_READY: Byte = 1
        private const val OPERATION_CHECK_DIM: Byte = 3

        /** Mifare Ultralight READ command starting at page 4 (returns pages 4-7). */
        private val CMD_READ_PAGE_4 = byteArrayOf(0x30, 0x04)

        /** Ultralight page the bracelet watches for the CHECK_DIM request. */
        private const val CHECK_PAGE = 6
    }

    /** Raw 16 bytes returned by the page-4 read, kept for diagnostics. */
    val raw: ByteArray
    val magic: UInt
    val itemId: UShort
    val itemNumber: UShort
    val status: Byte
    val operation: Byte
    val dimId: UShort

    init {
        raw = nfc.transceive(CMD_READ_PAGE_4)
        Timber.d("VBBraceletTag page-4 raw: ${raw.joinToString(" ") { "%02X".format(it) }}")
        magic = raw.getUInt32(0, Endian.Big)
        itemId = raw.getUInt16(4, Endian.Big)
        itemNumber = raw.getUInt16(6, Endian.Big)
        status = raw[8]
        if (itemId == ITEM_ID_BE) {
            operation = raw[9]
            dimId = raw.getUInt16(10, Endian.Big)
        } else {
            dimId = raw[9].toUShort()
            operation = raw[10]
        }
        Timber.d("VBBraceletTag parsed: magic=$magic itemId=$itemId itemNumber=$itemNumber " +
                "status=${"%02X".format(status)} operation=$operation dimId=$dimId")
    }

    /**
     * Tap 1: ask the bracelet to check [dimId]. The bracelet shows the
     * insert-card icon; the user inserts the physical card, then taps again.
     */
    fun writeCardCheck(nfc: MifareUltralight, dimId: UShort) {
        val page: ByteArray = if (itemId == ITEM_ID_BE) {
            val dimData = dimId.toByteArray(endian = Endian.Big)
            byteArrayOf(STATUS_READY_FLAG, OPERATION_CHECK_DIM, dimData[0], dimData[1])
        } else {
            byteArrayOf(STATUS_READY_FLAG, dimId.toByte(), OPERATION_CHECK_DIM, 0)
        }
        Timber.d("VBBraceletTag writing page $CHECK_PAGE: ${page.joinToString(" ") { "%02X".format(it) }}")
        nfc.writePage(CHECK_PAGE, page)
    }

    /**
     * Tap 2: true when the bracelet confirms it is holding the requested card:
     * same dimId, operation == READY, and both status ready flags set.
     */
    fun wasCardIdValidated(dimId: UShort): Boolean {
        val ok = dimId == this.dimId &&
                operation == OPERATION_READY &&
                (status and STATUS_DIM_IS_READY == STATUS_DIM_IS_READY)
        Timber.d("VBBraceletTag validation: want=$dimId got=${this.dimId} op=$operation " +
                "status=${"%02X".format(status)} -> $ok")
        return ok
    }
}

/**
 * Persists which DIM card IDs have been verified against the real bracelet,
 * so a card only needs the two-tap check once.
 */
object BraceletValidationStore {
    private const val PREFS = "bracelet_validation_prefs"
    private const val KEY_VALIDATED = "validated_dim_ids"

    fun isValidated(context: Context, dimId: Int): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_VALIDATED, emptySet())
            ?.contains(dimId.toString()) == true

    fun markValidated(context: Context, dimId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_VALIDATED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (updated.add(dimId.toString())) {
            prefs.edit().putStringSet(KEY_VALIDATED, updated).apply()
            Timber.d("BraceletValidationStore: DIM $dimId marked bracelet-verified")
        }
    }
}
