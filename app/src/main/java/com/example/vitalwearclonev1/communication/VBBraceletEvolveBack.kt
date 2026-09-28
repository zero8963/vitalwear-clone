package com.example.vitalwearclonev1.communication

import android.content.Context
import com.example.vitalwearclonev1.lab.StoredMonster
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Creates a NEW bracelet backup with an app-evolved partner's species bytes.
 *
 * Use when a bracelet-adopted partner digivolves IN THE APP (not on the bracelet)
 * and the user wants to send the evolved form back to the bracelet.
 *
 * This updates:
 * - b9  (0x0049/0x0059): species ID
 * - b12 (0x004C/0x005C): stage
 * - b13 (0x004D/0x005D): attribute
 * - b15 (0x004F/0x005F): species constant
 * - Timer reset to 1440 (post-evolution standard)
 * - Checksums recalculated
 *
 * It does NOT update the species history log (0x00D0) or evolution event log
 * (0x00E0) — those are best-effort and not critical for display. Future
 * bracelet-side evolutions from this backup may need a fresh read.
 *
 * The species bytes MUST be in [VBBraceletSpeciesMap]. If b13/b15 are unknown
 * (unconfirmed), the function throws — do not guess.
 */
object VBBraceletEvolveBack {

    const val PREFIX = "bracelet-adopt:"

    data class EvolvePreview(
        val speciesName: String,
        val b9: Int,
        val stage: Int,
        val b13: Int?,
        val b15: Int?,
        val confirmed: Boolean,
        val warnings: List<String>
    )

    data class EvolveResult(
        val newBackup: VBBraceletBackups.Backup,
        val changes: List<String>,
        val preview: EvolvePreview
    )

    /** The backup ID this partner was adopted from, or null. */
    fun sourceBackupId(monster: StoredMonster): String? {
        val raw = monster.rawPayload ?: return null
        return if (raw.startsWith(PREFIX)) raw.removePrefix(PREFIX) else null
    }

    /**
     * Preview the evolved-form write. Throws if the species isn't mapped
     * or if b13/b15 are unconfirmed.
     */
    fun preview(
        context: Context,
        monster: StoredMonster,
        targetSpecies: String
    ): EvolvePreview {
        val cardName = monster.name // DIM card name (e.g. "Impulse City")
        val species = VBBraceletSpeciesMap.lookup(cardName, targetSpecies)
            ?: throw IllegalArgumentException(
                "Species '$targetSpecies' not mapped for card '$cardName'. " +
                        "Cannot write unmapped species bytes to the bracelet."
            )

        val warnings = mutableListOf<String>()
        if (!species.confirmed) {
            warnings.add(
                "b15 for $targetSpecies is ESTIMATED (not device-confirmed). " +
                        "The bracelet may reject this. Test on a throwaway slot first."
            )
        }
        if (species.b13 == null || species.b15 == null) {
            throw IllegalArgumentException(
                "Species '$targetSpecies' has unknown b13/b15. " +
                        "Cannot write incomplete species bytes."
            )
        }

        return EvolvePreview(
            speciesName = targetSpecies,
            b9 = species.b9,
            stage = species.stage,
            b13 = species.b13,
            b15 = species.b15,
            confirmed = species.confirmed,
            warnings = warnings
        )
    }

    /**
     * Create a new backup with the evolved species bytes.
     * The original backup is preserved.
     */
    fun evolveToNewBackup(
        context: Context,
        monster: StoredMonster,
        targetSpecies: String
    ): EvolveResult {
        val backupId = sourceBackupId(monster)
            ?: throw IllegalArgumentException("This partner wasn't adopted from a bracelet backup.")
        val source = VBBraceletBackups.get(context, backupId)
            ?: throw IllegalStateException(
                "The source backup is gone (deleted?). " +
                        "Read the bracelet again to make a fresh backup first."
            )
        require(source.productId == 2) {
            "Evolved-form write is mapped for VB classic (product 2) bracelets only."
        }
        require(source.plain.size == VBBraceletData.DATA_SIZE) {
            "Source backup blob is corrupt (${source.plain.size} bytes)."
        }

        val p = preview(context, monster, targetSpecies)
        val patched = source.plain.copyOf()
        val changes = mutableListOf<String>()

        // Update species bytes (primary at 0x0040, mirror at 0x0050).
        // b9=species, b12=stage, b13=attribute, b15=species constant.
        val b9 = p.b9
        val stage = p.stage
        val b13 = p.b13!!
        val b15 = p.b15!!

        fun setByte(off: Int, value: Int) {
            patched[off] = value.toByte()
            VBBraceletData.recomputeBlockChecksum(patched, off)
        }

        // Primary block (0x0040)
        setByte(0x49, b9)
        setByte(0x4C, stage)
        setByte(0x4D, b13)
        setByte(0x4F, b15)
        // Mirror block (0x0050)
        setByte(0x59, b9)
        setByte(0x5C, stage)
        setByte(0x5D, b13)
        setByte(0x5F, b15)
        changes.add("species b9=$b9 stage=$stage b13=$b13 b15=$b15")

        // Reset Next timer to 1440 (post-evolution standard).
        // Timer at 0x008D-0x008E (U16 BE), mirror at 0x009D.
        setByte(0x8D, 0x05) // 1440 = 0x05A0
        setByte(0x8E, 0xA0)
        setByte(0x9D, 0x05)
        setByte(0x9E, 0xA0)
        changes.add("timer reset to 1440")

        // Post-evolution: vitals → 0 (from device data 2026-09-27).
        // Vitals at 0x0084-0x0085 (U16 BE), mirror at 0x0094.
        setByte(0x84, 0x00)
        setByte(0x85, 0x00)
        setByte(0x94, 0x00)
        setByte(0x95, 0x00)
        changes.add("vitals reset to 0")

        // Verify checksums.
        val bad = VBBraceletData.verifyChecksums(patched)
        require(bad.isEmpty()) { "Checksum mismatch after patching (pages $bad) — aborting." }

        val displayName = monster.nickname ?: targetSpecies
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val character = VBBraceletData.BraceletCharacter(
            productId = source.productId,
            uid = VBBraceletBackups.hexToBytes(source.uidHex),
            plain = patched,
            fields = VBBraceletData.parseFields(patched, source.productId)
        )
        val newBackup = VBBraceletBackups.save(
            context, character, note = "$displayName — evolved to $targetSpecies $stamp"
        )
        Timber.d(
            "VBBraceletEvolveBack: ${monster.nickname ?: monster.name} -> " +
                    "$targetSpecies, backup ${newBackup.id}"
        )
        return EvolveResult(newBackup, changes, p)
    }
}
