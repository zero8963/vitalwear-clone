package com.example.vitalwearclonev1.communication.secrets

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

class ApkSecretsImporter {
    companion object {
        const val DEX_FILE = "classes.dex"
        
        const val VBDM_SUBSTITUTION_CIPHER_IDX = 1080145
        const val BE_SUBSTITUTION_CIPHER_IDX = 1080217
        const val VBDM_HMAC_KEY_2_IDX = 1249063
        const val VBDM_HMAC_KEY_1_IDX = 1494074
        const val BE_HMAC_KEY_1_IDX = 1580157
        const val BE_HMAC_KEY_2_IDX = 1593759
        const val AES_KEY_IDX = 1277527
    }

    fun importFromApk(inputStream: InputStream): SecretsManager.VitalSecrets {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == DEX_FILE) {
                    return readFromDex(zip)
                }
                entry = zip.nextEntry
            }
        }
        throw Exception("classes.dex not found in APK")
    }

    private fun readFromDex(inputStream: InputStream): SecretsManager.VitalSecrets {
        val dexData = inputStream.readBytes()
        val byteOrder = ByteOrder.BIG_ENDIAN
        
        val vbCipher = dexData.sliceArray(VBDM_SUBSTITUTION_CIPHER_IDX until VBDM_SUBSTITUTION_CIPHER_IDX + 64).toIntArray(byteOrder)
        val beCipher = dexData.sliceArray(BE_SUBSTITUTION_CIPHER_IDX until BE_SUBSTITUTION_CIPHER_IDX + 64).toIntArray(byteOrder)
        
        val aesKey = String(dexData.sliceArray(AES_KEY_IDX until AES_KEY_IDX + 24), StandardCharsets.UTF_8)
        val vbdmHmac1 = String(dexData.sliceArray(VBDM_HMAC_KEY_1_IDX until VBDM_HMAC_KEY_1_IDX + 24), StandardCharsets.UTF_8)
        val vbdmHmac2 = String(dexData.sliceArray(VBDM_HMAC_KEY_2_IDX until VBDM_HMAC_KEY_2_IDX + 24), StandardCharsets.UTF_8)
        val beHmac1 = String(dexData.sliceArray(BE_HMAC_KEY_1_IDX until BE_HMAC_KEY_1_IDX + 24), StandardCharsets.UTF_8)
        val beHmac2 = String(dexData.sliceArray(BE_HMAC_KEY_2_IDX until BE_HMAC_KEY_2_IDX + 24), StandardCharsets.UTF_8)
        
        return SecretsManager.VitalSecrets(
            vbCipher = vbCipher,
            beCipher = beCipher,
            aesKey = aesKey,
            vbdmHmac1 = vbdmHmac1,
            vbdmHmac2 = vbdmHmac2,
            beHmac1 = beHmac1,
            beHmac2 = beHmac2
        )
    }

    private fun ByteArray.toIntArray(byteOrder: ByteOrder): IntArray {
        val ints = IntArray(size / 4)
        ByteBuffer.wrap(this).order(byteOrder).asIntBuffer().get(ints)
        return ints
    }
}
