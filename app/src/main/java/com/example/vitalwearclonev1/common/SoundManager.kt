package com.example.vitalwearclonev1.common

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import timber.log.Timber
import kotlin.random.Random

/**
 * Game SFX, 2026-09-26.
 *
 * Chiptune-style sounds live in app/src/main/res/raw as sfx_*.wav — swap any
 * file there to change the sound (the kids can record their own and drop them
 * in with the same names). Played through SoundPool for low-latency game
 * audio; attack sounds get a slight random pitch wobble so repeated shots
 * don't sound robotic.
 *
 * Usage: SoundManager.init(context) once (safe to call repeatedly), then
 * SoundManager.play("buster") etc. from anywhere, including Compose.
 */
object SoundManager {
    private var pool: SoundPool? = null
    private val ids = mutableMapOf<String, Int>()
    private var enabled = true
    private var appContext: Context? = null

    /** Sound names; each maps to R.raw.sfx_<name>. */
    val ALL = listOf(
        "evo", "buster", "buster_charged", "sword", "projectile", "beam",
        "lob", "summon", "trap", "heal", "hit", "charge", "win", "lose", "ui"
    )

    /** BattleChip EffectKind -> sound name. */
    fun forEffectKind(kindName: String): String = when (kindName) {
        "PROJECTILE" -> "projectile"
        "SWORD", "MELEE" -> "sword"
        "LOB" -> "lob"
        "BEAM" -> "beam"
        "SUMMON" -> "summon"
        "TRAP" -> "trap"
        "SUPPORT" -> "heal"
        else -> "projectile"
    }

    @Synchronized
    fun init(context: Context) {
        appContext = context.applicationContext
        if (pool != null) return
        enabled = appContext!!.getSharedPreferences("vitalwear_prefs", Context.MODE_PRIVATE)
            .getBoolean("sfx_enabled", true)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
        val res = appContext!!.resources
        val pkg = appContext!!.packageName
        for (name in ALL) {
            val resId = res.getIdentifier("sfx_$name", "raw", pkg)
            if (resId != 0) {
                ids[name] = pool!!.load(appContext, resId, 1)
            } else {
                Timber.w("SoundManager: missing raw/sfx_$name")
            }
        }
    }

    fun setEnabled(on: Boolean) {
        enabled = on
        appContext?.getSharedPreferences("vitalwear_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("sfx_enabled", on)?.apply()
    }

    fun isEnabled(): Boolean = enabled

    /** Play a sound by name; rate 1f = normal pitch. */
    fun play(name: String, rate: Float = 1f, volume: Float = 1f) {
        if (!enabled) return
        val p = pool ?: return
        val id = ids[name] ?: return
        // TUNE: ±8% pitch wobble keeps repeat shots from sounding robotic.
        val wobble = if (name == "evo" || name == "win" || name == "lose" || name == "ui") 1f
        else 1f + (Random.nextFloat() - 0.5f) * 0.16f
        try {
            p.play(id, volume, volume, 1, 0, (rate * wobble).coerceIn(0.5f, 2f))
        } catch (e: Exception) {
            Timber.w(e, "SoundManager.play($name) failed")
        }
    }

    fun release() {
        try { pool?.release() } catch (_: Exception) {}
        pool = null
        ids.clear()
    }
}
