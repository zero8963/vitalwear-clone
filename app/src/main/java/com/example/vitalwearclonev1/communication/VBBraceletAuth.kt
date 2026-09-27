package com.example.vitalwearclonev1.communication

import android.nfc.tech.NfcA
import android.util.Base64
import timber.log.Timber
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * PWD_AUTH login probe for a real Vital Bracelet (BE family, including the
 * Vital Hero) in Connect -> App Loglink mode.
 *
 * Reverse-engineered from the official Vital Bracelet Arena APK
 * (com.bandai.vitalbraceletarena v2.1.0 — jp/co/bandai/vb_nfc/a.java and
 * MainActivity.createPass/passwordAuth), verified line-by-line against the
 * decompiled source. Everything is offline: the 4-byte password is derived
 * locally from a hardcoded master key, per-product encrypted key pairs, and
 * the tag's 7-byte UID. No server involved.
 *
 * Derivation (exact mirror of the official app):
 *   key1 = AES-256-CBC-decrypt(base64(encKey1), masterKey, iv)   // ASCII string
 *   key2 = AES-256-CBC-decrypt(base64(encKey2), masterKey, iv)   // ASCII string
 *   h1   = HMAC-SHA256(key1, uid[0..6])
 *   s    = SBOX_NIBBLES(h1)          // VBBE table when productId == 4
 *   h2   = HMAC-SHA256(key2, s)
 *   password = h2[28..31]
 *
 *   masterKey: "E2C56DB5DFFB48D2B060D0F5" -> AES key = 24 UTF-8 bytes + 8 zero
 *              bytes; IV = last 16 bytes of the UTF-8 encoding.
 *
 * Login: NfcA transceive(1B p0 p1 p2 p3). A 2-byte reply is the PACK
 * (success); anything else (1-byte NAK, timeout, exception) is failure.
 * PWD_AUTH does not alter tag memory.
 */
object VBBraceletAuth {

    private const val MASTER_KEY = "E2C56DB5DFFB48D2B060D0F5"

    /** productId -> (base64 encKey1, base64 encKey2), per the official app. */
    private val PRODUCT_KEYS = mapOf(
        2 to Pair("cX+3kmjEVevKSARuiLqkuQ==", "32tJnlfeP3ETQM4KdE/X6w=="),
        3 to Pair("0k1bpdBrjizR3Xrs1fNmtw==", "W7JeDXv1C7S9h1XggnsNBQ=="),
        4 to Pair("n98u7qRmbCHK4YyEcOdedg==", "ouurqHkOIuHGvMPoBWV+8g==")
    )

    private val SBOX_CLASSIC = intArrayOf(9, 14, 4, 8, 0, 10, 15, 6, 7, 13, 1, 12, 5, 11, 3, 2)
    private val SBOX_VBBE = intArrayOf(5, 2, 4, 6, 3, 15, 0, 14, 1, 12, 13, 7, 11, 8, 10, 9)

    fun productName(productId: Int): String = when (productId) {
        2 -> "Digimon (VB classic)"
        3 -> "CHARACTERS"
        4 -> "VBBE / BE family"
        else -> "unknown"
    }

    fun supportsProduct(productId: Int): Boolean = PRODUCT_KEYS.containsKey(productId)

    fun sboxTableFor(productId: Int): IntArray =
        if (productId == 4) SBOX_VBBE else SBOX_CLASSIC

    /** Cache of unwrapped (key1, key2) per product — the unwrap is expensive. */
    private val keyPairCache = mutableMapOf<Int, Pair<ByteArray, ByteArray>>()

    /**
     * Unwrapped per-product (key1, key2) ASCII key bytes, via the master-key
     * AES-256-CBC unwrap. Shared by the PWD_AUTH derivation and the
     * UID-bound AES-CTR data crypto. Throws on unknown product.
     */
    @Synchronized
    fun getKeyPair(productId: Int): Pair<ByteArray, ByteArray> {
        keyPairCache[productId]?.let { return it }
        val (encKey1, encKey2) = PRODUCT_KEYS[productId]
            ?: throw IllegalArgumentException("unknown product ID: $productId")
        // Master key -> AES-256 key (UTF-8 bytes zero-padded to 32), IV = last 16 bytes.
        val masterBytes = MASTER_KEY.toByteArray(Charsets.UTF_8)
        val aesKey = ByteArray(32).also { System.arraycopy(masterBytes, 0, it, 0, masterBytes.size) }
        val iv = masterBytes.copyOfRange(masterBytes.size - 16, masterBytes.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        fun unwrap(b64: String): ByteArray {
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(iv))
            return cipher.doFinal(Base64.decode(b64, Base64.DEFAULT))
        }
        val pair = Pair(unwrap(encKey1), unwrap(encKey2))
        keyPairCache[productId] = pair
        Timber.d("VBBraceletAuth unwrapped key pair for product $productId")
        return pair
    }

    /**
     * Derive the 4-byte PWD_AUTH password for [uid] (must be 7 bytes) and
     * [productId] (2, 3 or 4). Throws on unknown product or bad UID length.
     */
    fun derivePassword(uid: ByteArray, productId: Int): ByteArray {
        require(uid.size == 7) { "UID must be 7 bytes, was ${uid.size}" }
        val (key1, key2) = getKeyPair(productId)

        val h1 = hmacSha256(key1, uid)
        val s = sboxNibbles(h1, sboxTableFor(productId))
        val h2 = hmacSha256(key2, s)
        val password = h2.copyOfRange(28, 32)
        Timber.d("VBBraceletAuth derived password (product=$productId uid=${uid.toHex()}): ${password.toHex()}")
        return password
    }

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    /** Nibble substitution, mirroring the official app's SBOX loop exactly. */
    fun sboxNibbles(input: ByteArray, table: IntArray): ByteArray =
        ByteArray(input.size) { i ->
            val b = input[i].toInt() and 0xFF
            var out = 0
            for (shift in intArrayOf(0, 4)) {
                out = out or (table[(b shr shift) and 0x0F] shl shift)
            }
            out.toByte()
        }

    sealed interface PwdAuthResult {
        /** 2-byte PACK reply: the bracelet accepted the login. */
        data class Success(val pack: ByteArray, val productId: Int, val ackOk: Boolean) : PwdAuthResult
        /** 1-byte reply: the bracelet actively rejected the login (NAK). */
        data class Nak(val response: ByteArray, val productId: Int) : PwdAuthResult
        /** No usable reply: timeout, tag lost, derivation failure, etc. */
        data class Error(val message: String) : PwdAuthResult
    }

    /**
     * Send PWD_AUTH (0x1B) with the derived password over an already-connected
     * [nfc]. Read-only probe apart from the auth command itself.
     */
    fun pwdAuth(nfc: NfcA, uid: ByteArray, productId: Int): PwdAuthResult {
        val password = try {
            derivePassword(uid, productId)
        } catch (e: Exception) {
            Timber.e(e, "VBBraceletAuth password derivation failed")
            return PwdAuthResult.Error("password derivation failed: ${e.message}")
        }
        val cmd = byteArrayOf(0x1B, password[0], password[1], password[2], password[3])
        Timber.d("VBBraceletAuth sending PWD_AUTH for product $productId")
        return try {
            val resp = nfc.transceive(cmd)
            Timber.d("VBBraceletAuth PWD_AUTH reply (${resp.size} bytes): ${resp.toHex()}")
            when {
                resp.size == 2 -> PwdAuthResult.Success(resp, productId, resp[0] == 0x0A.toByte())
                resp.size == 1 -> PwdAuthResult.Nak(resp, productId)
                else -> PwdAuthResult.Error("unexpected reply length ${resp.size}: ${resp.toHex()}")
            }
        } catch (e: IOException) {
            Timber.w(e, "VBBraceletAuth PWD_AUTH transceive failed")
            PwdAuthResult.Error("no reply (tag rejected or lost): ${e.message}")
        }
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}
