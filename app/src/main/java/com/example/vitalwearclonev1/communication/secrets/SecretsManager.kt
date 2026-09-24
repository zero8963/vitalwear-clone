package com.example.vitalwearclonev1.communication.secrets

import android.content.Context
import android.content.SharedPreferences

class SecretsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vital_secrets", Context.MODE_PRIVATE)

    data class VitalSecrets(
        val vbCipher: IntArray? = null,
        val beCipher: IntArray? = null,
        val aesKey: String? = null,
        val vbdmHmac1: String? = null,
        val vbdmHmac2: String? = null,
        val beHmac1: String? = null,
        val beHmac2: String? = null
    )

    fun saveSecrets(secrets: VitalSecrets) {
        val editor = prefs.edit()
        secrets.vbCipher?.let { editor.putString("vb_cipher", it.joinToString(",")) }
        secrets.beCipher?.let { editor.putString("be_cipher", it.joinToString(",")) }
        secrets.aesKey?.let { editor.putString("aes_key", it) }
        secrets.vbdmHmac1?.let { editor.putString("vbdm_hmac1", it) }
        secrets.vbdmHmac2?.let { editor.putString("vbdm_hmac2", it) }
        secrets.beHmac1?.let { editor.putString("be_hmac1", it) }
        secrets.beHmac2?.let { editor.putString("be_hmac2", it) }
        editor.apply()
    }

    fun getSecrets(): VitalSecrets {
        val vbCipherStr = prefs.getString("vb_cipher", null)
        val beCipherStr = prefs.getString("be_cipher", null)
        
        return VitalSecrets(
            vbCipher = vbCipherStr?.split(",")?.map { it.toInt() }?.toIntArray(),
            beCipher = beCipherStr?.split(",")?.map { it.toInt() }?.toIntArray(),
            aesKey = prefs.getString("aes_key", null),
            vbdmHmac1 = prefs.getString("vbdm_hmac1", null),
            vbdmHmac2 = prefs.getString("vbdm_hmac2", null),
            beHmac1 = prefs.getString("be_hmac1", null),
            beHmac2 = prefs.getString("be_hmac2", null)
        )
    }

    fun hasSecrets(): Boolean {
        return prefs.contains("vb_cipher")
    }
}
