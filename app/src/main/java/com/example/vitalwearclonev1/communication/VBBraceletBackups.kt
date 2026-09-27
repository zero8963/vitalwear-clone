package com.example.vitalwearclonev1.communication

import android.content.Context
import timber.log.Timber
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App-private persistence for extracted bracelet character backups.
 *
 * Every successful bracelet character read is auto-saved here, so the
 * extracted data survives leaving the transfer screen. A backup is the
 * full decrypted 864-byte blob (Base64) plus metadata and the parsed
 * known fields at save time.
 *
 * Storage: single JSON file under filesDir/bracelet_backups/backups.json.
 * No new dependencies (org.json ships with Android).
 */
object VBBraceletBackups {

    private const val DIR = "bracelet_backups"
    private const val FILE = "backups.json"
    private const val MAX_BACKUPS = 50

    data class Backup(
        val id: String,
        val savedAt: Long,
        val uidHex: String,
        val productId: Int,
        /** Decrypted 864-byte blob. Unknown regions preserved verbatim. */
        val plain: ByteArray,
        /** fieldId -> value at save time (for list summaries). */
        val fields: Map<String, Int>
    ) {
        val productName: String get() = VBBraceletAuth.productName(productId)
        val uidShort: String get() = if (uidHex.length >= 8) uidHex.take(8) + "…" else uidHex
        val dateStr: String get() =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(savedAt))

        fun fieldSummary(): String {
            val parts = mutableListOf<String>()
            fields["mental"]?.let { parts.add("Mental $it") }
            fields["vital"]?.let { parts.add("Vital $it") }
            fields["originDimId"]?.let {
                parts.add("DIM " + if (it == 0xFFFF) "empty" else it.toString())
            }
            return if (parts.isEmpty()) "no fields parsed" else parts.joinToString(" · ")
        }
    }

    private fun storeFile(context: Context): File {
        val dir = File(context.filesDir, DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE)
    }

    private fun uidToHex(uid: ByteArray): String =
        uid.joinToString("") { "%02X".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) out[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        return out
    }

    private fun toJson(b: Backup): JSONObject = JSONObject().apply {
        put("id", b.id)
        put("savedAt", b.savedAt)
        put("uidHex", b.uidHex)
        put("productId", b.productId)
        put("blob", android.util.Base64.encodeToString(b.plain, android.util.Base64.NO_WRAP))
        val fj = JSONObject()
        for ((k, v) in b.fields) fj.put(k, v)
        put("fields", fj)
    }

    private fun fromJson(o: JSONObject): Backup? = try {
        val fj = o.optJSONObject("fields") ?: JSONObject()
        val fields = mutableMapOf<String, Int>()
        val keys = fj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            fields[k] = fj.optInt(k)
        }
        Backup(
            id = o.getString("id"),
            savedAt = o.getLong("savedAt"),
            uidHex = o.getString("uidHex"),
            productId = o.getInt("productId"),
            plain = android.util.Base64.decode(o.getString("blob"), android.util.Base64.NO_WRAP),
            fields = fields
        )
    } catch (e: Exception) {
        Timber.w(e, "VBBraceletBackups skipping corrupt entry")
        null
    }

    /** Load all backups, newest first. Never throws. */
    fun list(context: Context): List<Backup> = try {
        val f = storeFile(context)
        if (!f.exists()) return emptyList()
        val arr = JSONObject(f.readText()).optJSONArray("backups") ?: JSONArray()
        val out = mutableListOf<Backup>()
        for (i in 0 until arr.length()) {
            fromJson(arr.getJSONObject(i))?.let { out.add(it) }
        }
        out.sortedByDescending { it.savedAt }
    } catch (e: Exception) {
        Timber.e(e, "VBBraceletBackups list failed")
        emptyList()
    }

    fun get(context: Context, id: String): Backup? = list(context).firstOrNull { it.id == id }

    /**
     * Auto-save a freshly read character. Returns the saved backup.
     * Caps the store at [MAX_BACKUPS], dropping the oldest.
     */
    fun save(context: Context, character: VBBraceletData.BraceletCharacter): Backup {
        val backup = Backup(
            id = "bb_${System.currentTimeMillis()}",
            savedAt = System.currentTimeMillis(),
            uidHex = uidToHex(character.uid),
            productId = character.productId,
            plain = character.plain.copyOf(),
            fields = character.fields.associate { it.field.id to it.value }
        )
        val all = (list(context) + backup).sortedByDescending { it.savedAt }.take(MAX_BACKUPS)
        val arr = JSONArray()
        for (b in all) arr.put(toJson(b))
        storeFile(context).writeText(JSONObject().put("backups", arr).toString())
        Timber.d("VBBraceletBackups saved ${backup.id} (product ${backup.productId}, ${all.size} total)")
        return backup
    }

    /** Delete a backup. Returns true if it existed. */
    fun delete(context: Context, id: String): Boolean {
        val all = list(context)
        if (all.none { it.id == id }) return false
        val arr = JSONArray()
        for (b in all) if (b.id != id) arr.put(toJson(b))
        storeFile(context).writeText(JSONObject().put("backups", arr).toString())
        Timber.d("VBBraceletBackups deleted $id")
        return true
    }

    /** Rebuild a BraceletCharacter from a stored backup for viewing/editing. */
    fun toCharacter(backup: Backup): VBBraceletData.BraceletCharacter {
        val uid = hexToBytes(backup.uidHex)
        return VBBraceletData.BraceletCharacter(
            backup.productId,
            uid,
            backup.plain.copyOf(),
            VBBraceletData.parseFields(backup.plain, backup.productId)
        )
    }
}
