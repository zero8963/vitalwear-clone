package com.example.vitalwearclonev1.communication

import timber.log.Timber
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

/**
 * Character-data crypto + blob model for the real Vital Bracelet transfer
 * protocol, reverse-engineered from the official Vital Bracelet Arena APK
 * (com.bandai.vitalbraceletarena v2.1.0 — jp/co/bandai/vb_nfc/a.java).
 * Verified against the decompiled source, not just the protocol report.
 *
 * Data region: tag pages 8–223 = 864 bytes (page 8 = blob offset 0).
 *
 * Encryption (exact mirror of the official app):
 *   h1     = HMAC-SHA256(key1, uid[0..6])
 *   s      = SBOX_NIBBLES(h1)              // VBBE table iff productId == 4
 *   h2     = HMAC-SHA256(key2, s)
 *   aesKey = h2[0..15]
 *   t1     = h2[24..31] ++ uid[0..6]       // 15 bytes
 *   t2     = h2[0..14]                     // 15 bytes
 *   iv     = (t1 XOR t2) zero-padded to 16 bytes
 *   data   = AES-CTR-NoPadding(aesKey, iv, data)   // symmetric: same fn both ways
 *
 * Checksums: for the 16-byte blocks starting at absolute pages
 * {8,16,24,32,40,48,52,56,60,64,68,72,76,80,84,104,192,200,208,216}:
 * block[15] = sum(block[0..14]) & 0xFF (signed-byte sum, same mod 256).
 *
 * IMPORTANT: the official app only ever patches ~10 known fields and got full
 * blobs from its (dead) server. We do strict read-modify-write: unknown blob
 * regions are preserved verbatim. Fresh blobs cannot be synthesized from zero.
 */
object VBBraceletData {

    const val DATA_FIRST_PAGE = 8
    const val DATA_LAST_PAGE = 223
    const val DATA_SIZE = 864

    /** Absolute pages whose 16-byte blocks carry a checksum byte. */
    private val CHECKSUM_PAGES = setOf(
        8, 16, 24, 32, 40, 48, 52, 56, 60, 64, 68, 72, 76, 80, 84,
        104, 192, 200, 208, 216
    )

    /** UID-bound AES-CTR. Symmetric: the same call encrypts and decrypts. */
    fun dataCrypt(uid: ByteArray, productId: Int, data: ByteArray): ByteArray {
        val (key, iv) = cryptParams(uid, productId)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.doFinal(data)
    }

    /**
     * Decrypt a single 16-byte block at [blockIndex] (0-based within the
     * 864-byte region) without touching the rest. AES-CTR blocks are
     * independent, so this is exact — used to verify the data-exists flag
     * after a transfer without re-reading all 864 bytes.
     */
    fun dataCryptBlock(uid: ByteArray, productId: Int, blockIndex: Int, cipherBlock: ByteArray): ByteArray {
        require(uid.size == 7) { "UID must be 7 bytes" }
        require(cipherBlock.size == 16) { "block must be 16 bytes" }
        val (key, iv) = cryptParams(uid, productId)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        // Advance the CTR counter past the preceding blocks (update() with
        // complete blocks only moves the counter, output discarded).
        if (blockIndex > 0) cipher.update(ByteArray(blockIndex * 16))
        return cipher.doFinal(cipherBlock)
    }

    /** Shared AES key + IV derivation for [dataCrypt]/[dataCryptBlock]. */
    private fun cryptParams(uid: ByteArray, productId: Int): Pair<SecretKeySpec, IvParameterSpec> {
        require(uid.size == 7) { "UID must be 7 bytes" }
        val (key1, key2) = VBBraceletAuth.getKeyPair(productId)
        val h1 = VBBraceletAuth.hmacSha256(key1, uid)
        val s = VBBraceletAuth.sboxNibbles(h1, VBBraceletAuth.sboxTableFor(productId))
        val h2 = VBBraceletAuth.hmacSha256(key2, s)
        val aesKey = h2.copyOfRange(0, 16)
        val t1 = h2.copyOfRange(24, 32) + uid // 15 bytes
        val t2 = h2.copyOfRange(0, 15)        // 15 bytes
        val iv = ByteArray(16) { i -> if (i < 15) (t1[i] xor t2[i]) else 0.toByte() }
        return SecretKeySpec(aesKey, "AES") to IvParameterSpec(iv)
    }

    /** Returns the list of absolute pages whose checksum FAILED (empty = all OK). */
    fun verifyChecksums(plain: ByteArray): List<Int> {
        require(plain.size == DATA_SIZE) { "blob must be $DATA_SIZE bytes" }
        val bad = mutableListOf<Int>()
        for (page in CHECKSUM_PAGES) {
            val off = (page - DATA_FIRST_PAGE) * 4
            var sum = 0
            for (i in 0 until 15) sum += plain[off + i]
            if ((sum and 0xFF).toByte() != plain[off + 15]) bad.add(page)
        }
        if (bad.isNotEmpty()) Timber.w("VBBraceletData checksum failures on pages $bad")
        return bad
    }

    /** Recompute the checksum byte of the 16-byte block containing [offset]. */
    fun recomputeBlockChecksum(plain: ByteArray, offset: Int) {
        val blockStart = (offset / 16) * 16
        var sum = 0
        for (i in 0 until 15) sum += plain[blockStart + i]
        plain[blockStart + 15] = (sum and 0xFF).toByte()
    }

    // ------------------------------------------------------------------
    // Character blob model
    // ------------------------------------------------------------------

    enum class FieldType { U8, U16_BE }

    /**
     * A known field of the decrypted character blob. [page]/[byte] are
     * ABSOLUTE tag page / byte-in-page (page 8 = blob offset 0).
     */
    data class CharField(
        val id: String,
        val label: String,
        val page: Int,
        val byte: Int,
        val type: FieldType,
        val cap: Int?,
        val products: Set<Int>,
        val editable: Boolean,
        val hint: String = ""
    ) {
        val offset: Int get() = (page - DATA_FIRST_PAGE) * 4 + byte
    }

    val FIELDS = listOf(
        CharField("originDimId", "Origin DIM ID", 26, 2, FieldType.U16_BE, null,
            setOf(2, 3, 4), true, "FFFF = empty slot"),
        CharField("dataExists", "Character present", 32, 0, FieldType.U16_BE, null,
            setOf(2, 3), false, "non-zero = data exists (classic)"),
        CharField("mental", "Mental", 40, 1, FieldType.U8, 100,
            setOf(2, 3, 4), true, "cap 100"),
        CharField("vital", "Vital value", 41, 0, FieldType.U16_BE, null,
            setOf(2, 3, 4), true),
        CharField("nextTimer", "Next timer", 43, 1, FieldType.U16_BE, null,
            setOf(2, 3, 4), true),
        CharField("hpPlus", "HP+", 72, 0, FieldType.U16_BE, 999,
            setOf(4), true, "VBBE only, cap 999"),
        CharField("apPlus", "AP+", 72, 2, FieldType.U16_BE, 999,
            setOf(4), true, "VBBE only, cap 999"),
        CharField("bpPlus", "BP+", 73, 0, FieldType.U16_BE, 999,
            setOf(4), true, "VBBE only, cap 999"),
        CharField("trainingLimit", "Training limit", 74, 2, FieldType.U16_BE, 6000,
            setOf(4), true, "VBBE only, cap 6000"),
        CharField("abilityRarity", "Ability rarity", 80, 2, FieldType.U8, null,
            setOf(4), true, "VBBE only"),
        CharField("abilityId", "Ability ID", 81, 2, FieldType.U16_BE, null,
            setOf(4), true, "VBBE only"),
        CharField("swVersion", "Software version", 103, 0, FieldType.U16_BE, null,
            setOf(4), false, "VBBE only")
    )

    data class FieldValue(val field: CharField, val value: Int)

    /** A decrypted character blob plus its parsed known fields. */
    data class BraceletCharacter(
        val productId: Int,
        /** UID the blob was decrypted with (needed if re-encrypting). */
        val uid: ByteArray,
        /** 864-byte DECRYPTED blob. Unknown regions preserved verbatim. */
        val plain: ByteArray,
        val fields: List<FieldValue>
    )

    fun readField(plain: ByteArray, field: CharField): Int = when (field.type) {
        FieldType.U8 -> plain[field.offset].toInt() and 0xFF
        FieldType.U16_BE -> ((plain[field.offset].toInt() and 0xFF) shl 8) or
                (plain[field.offset + 1].toInt() and 0xFF)
    }

    fun parseFields(plain: ByteArray, productId: Int): List<FieldValue> =
        FIELDS.filter { productId in it.products }.map { FieldValue(it, readField(plain, it)) }

    /** Mirrors the official app's data-exists check. */
    fun hasCharacterData(plain: ByteArray, productId: Int): Boolean =
        if (productId == 4) {
            readField(plain, FIELDS.first { it.id == "originDimId" }) != 0xFFFF
        } else {
            readField(plain, FIELDS.first { it.id == "dataExists" }) != 0
        }

    /**
     * Patch [edits] (fieldId -> new value) into [plain] IN PLACE, recomputing
     * the checksum of every touched 16-byte block. Returns human-readable
     * "label: old → new" lines for the confirm dialog / log.
     */
    fun applyEdits(plain: ByteArray, productId: Int, edits: Map<String, Int>): List<String> {
        val changed = mutableListOf<String>()
        for ((id, value) in edits) {
            val field = FIELDS.firstOrNull { it.id == id } ?: continue
            require(field.editable) { "${field.label} is read-only" }
            require(productId in field.products) { "${field.label} not applicable to this bracelet" }
            val max = when (field.type) {
                FieldType.U8 -> 255
                FieldType.U16_BE -> 65535
            }
            require(value in 0..max) { "${field.label} out of range 0..$max" }
            if (field.cap != null) require(value <= field.cap) { "${field.label} max is ${field.cap}" }
            val old = readField(plain, field)
            if (old != value) {
                when (field.type) {
                    FieldType.U8 -> plain[field.offset] = value.toByte()
                    FieldType.U16_BE -> {
                        plain[field.offset] = (value shr 8).toByte()
                        plain[field.offset + 1] = value.toByte()
                    }
                }
                recomputeBlockChecksum(plain, field.offset)
                changed.add("${field.label}: $old → $value")
                Timber.d("VBBraceletData patched ${field.id} $old -> $value (page ${field.page})")
            }
        }
        return changed
    }

    fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    fun hexDump(bytes: ByteArray, bytesPerLine: Int = 16): String {
        val sb = StringBuilder()
        var off = 0
        while (off < bytes.size) {
            val end = minOf(off + bytesPerLine, bytes.size)
            sb.append("%04X: ".format(off))
            for (i in off until end) sb.append("%02X ".format(bytes[i]))
            sb.append("\n")
            off = end
        }
        return sb.toString()
    }
}
