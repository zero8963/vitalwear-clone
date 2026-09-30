package com.example.vitalwearclonev1.communication

import android.content.Context
import android.os.Build
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * b15 crowdsourcing (2026-09-30): lets Vital Bracelet owners contribute
 * their character's species bytes to the community b15 mapping effort.
 *
 * The b15 species constant lives at blob offset 0x4F/0x5F and can only be
 * learned from real hardware. Each contributor's bracelet already holds the
 * answer for whatever species is on it — this ships just those bytes
 * (plus the DIM/species identity needed to file them) to the owner's
 * Google Sheet via a free Apps Script endpoint. Nothing else leaves the
 * phone: no nicknames, no vitals, no battle records.
 *
 * Strictly opt-in: the contributor previews the exact payload and confirms
 * the species name against their bracelet's display before anything is sent.
 */
object VBBraceletResearch {

    // Blob offsets of the species bytes (mirror pair; primary copy used).
    private const val OFF_B9 = 0x49
    private const val OFF_B12 = 0x4C
    private const val OFF_B13 = 0x4D
    private const val OFF_B15 = 0x4F

    private const val PREFS = "b15_research_prefs"
    private const val KEY_ENDPOINT = "research_endpoint_url"

    val STAGE_NAMES = mapOf(
        0 to "Baby I", 1 to "Baby II", 2 to "Child",
        3 to "Adult", 4 to "Ultimate", 5 to "Mega"
    )

    data class ResearchSubmission(
        val dimName: String,
        val speciesName: String,
        val stage: Int,
        val b9: Int,
        val b12: Int,
        val b13: Int,
        val b15: Int,
        val productId: Int,
        val productName: String,
        val appVersion: String,
        val deviceModel: String
    ) {
        fun stageName(): String = STAGE_NAMES[stage] ?: "Stage $stage"

        fun toJson(): JSONObject = JSONObject()
            .put("dimName", dimName)
            .put("speciesName", speciesName)
            .put("stage", stage)
            .put("stageName", stageName())
            .put("b9", "0x" + b9.toString(16).uppercase().padStart(2, '0'))
            .put("b12", "0x" + b12.toString(16).uppercase().padStart(2, '0'))
            .put("b13", "0x" + b13.toString(16).uppercase().padStart(2, '0'))
            .put("b15", "0x" + b15.toString(16).uppercase().padStart(2, '0'))
            .put("productId", productId)
            .put("productName", productName)
            .put("appVersion", appVersion)
            .put("deviceModel", deviceModel)

        /** One-line human preview of exactly what will be transmitted. */
        fun previewLines(): List<String> = listOf(
            "DIM: $dimName",
            "Species: $speciesName (${stageName()})",
            "b9=${hex(b9)}  b12=${hex(b12)}  b13=${hex(b13)}  b15=${hex(b15)}",
            "Bracelet: $productName (product $productId)",
            "App: $appVersion on $deviceModel"
        )

        private fun hex(v: Int): String = "0x" + v.toString(16).uppercase().padStart(2, '0')
    }

    private fun u8(blob: ByteArray, off: Int): Int = blob[off].toInt() and 0xFF

    /** Build a submission from a decrypted bracelet character. */
    fun buildSubmission(
        context: Context,
        ch: VBBraceletData.BraceletCharacter,
        dimName: String,
        speciesName: String
    ): ResearchSubmission {
        val b9 = u8(ch.plain, OFF_B9)
        val b12 = u8(ch.plain, OFF_B12)
        return ResearchSubmission(
            dimName = dimName.trim(),
            speciesName = speciesName.trim(),
            stage = b12,
            b9 = b9,
            b12 = b12,
            b13 = u8(ch.plain, OFF_B13),
            b15 = u8(ch.plain, OFF_B15),
            productId = ch.productId,
            productName = VBBraceletAuth.productName(ch.productId),
            appVersion = appVersion(context),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }

    /** Species-name candidates for a b9 roster index across every mapped card. */
    fun speciesCandidates(b9: Int): List<Pair<String, String>> {
        return VBBraceletSpeciesMap.allCards().flatMap { (card, map) ->
            map.entries
                .filter { it.value.b9 == b9 }
                .filterNot { (name, _) -> VBBraceletSpeciesMap.isPlaceholderSlot(name) }
                .map { (name, _) -> name to card }
        }
    }

    fun getEndpoint(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENDPOINT, "") ?: ""

    fun saveEndpoint(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ENDPOINT, url.trim())
            .apply()
    }

    private fun appVersion(context: Context): String = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        pi.versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    /**
     * POST the submission JSON to the Apps Script endpoint on a background
     * thread. [onResult] runs on the calling thread — the caller is
     * responsible for hopping to the UI thread.
     */
    fun submit(
        endpointUrl: String,
        submission: ResearchSubmission,
        onResult: (ok: Boolean, message: String) -> Unit
    ) {
        Thread {
            try {
                val body = submission.toJson().toString().toByteArray(Charsets.UTF_8)
                val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                conn.outputStream.use { it.write(body) }
                val code = conn.responseCode
                // Apps Script returns 302 on success when not using
                // follow-redirects carefully; accept any 2xx/3xx.
                if (code in 200..399) {
                    Timber.i("b15 research submission accepted (HTTP $code)")
                    onResult(true, "Contribution sent — thank you!")
                } else {
                    Timber.w("b15 research submission failed: HTTP $code")
                    onResult(false, "Send failed (HTTP $code) — check the endpoint URL.")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Timber.w(e, "b15 research submission failed")
                onResult(false, "Send failed: ${e.message}")
            }
        }.start()
    }
}
