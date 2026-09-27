package com.example.vitalwearclonev1.communication

import android.content.Context
import com.example.vitalwearclonev1.lab.StoredMonster
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Sync training to Bracelet": patch an adopted partner's app-side progress
 * back into its source bracelet backup, then hand the new backup to the
 * existing Write-to-Bracelet restore flow.
 *
 * Mapped training bytes (product 2, verified live on the real Hero
 * 2026-09-27 via read→battle→read diffs, checksums verified):
 *   0x0081/0x0091  mental  ← partner mood (0–100)
 *   0x0063/0x0073  wins    ← partner win count
 *   0x0065/0x0075  losses  ← partner loss count
 *   0x006A/0x007A  win rate % ← recomputed, truncated (wins*100/(wins+losses))
 *
 * Deliberately NOT touched (no verified mapping yet):
 *   vitality / stat bonuses / level / XP — the app's training bonuses live
 *   in app-side fields with no known bracelet counterpart. The UI says so
 *   plainly instead of inventing bytes.
 *
 * Trophies (0x006C/0x007C) are read for display but never written by sync:
 * missions are earned on the bracelet, so the counter is preserved verbatim
 * in the new backup.
 *
 * Safety: strict read-modify-write. The source backup is never altered — the
 * patched blob is saved as a NEW backup. Only the 8 mapped bytes change;
 * all four touched 16-byte blocks get fresh checksums. Pages 0–7, the
 * frozen reference pair (0x01D0/0x01E0), mission flags, and every unknown
 * byte are preserved verbatim.
 */
object VBBraceletSyncBack {

    const val PREFIX = "bracelet-adopt:"

    /** The source backup id for an adopted partner, or null if not adopted. */
    fun sourceBackupId(monster: StoredMonster): String? =
        monster.rawPayload
            ?.takeIf { it.startsWith(PREFIX) }
            ?.removePrefix(PREFIX)
            ?.takeIf { it.isNotBlank() }

    data class SyncPreview(
        val mental: Int,
        val wins: Int,
        val losses: Int,
        val winRate: Int,
        /**
         * Bracelet-side trophy count read from the source backup.
         * Display only: missions are earned on the bracelet, so sync never
         * writes trophies — the counter is preserved verbatim.
         */
        val trophies: Int?
    )

    /** What the sync would write, derived from the partner's app-side state. */
    fun preview(context: Context, monster: StoredMonster): SyncPreview {
        val wins = monster.currentWins.coerceIn(0, 255)
        val losses = monster.losses.coerceIn(0, 255)
        val total = wins + losses
        return SyncPreview(
            mental = monster.mood.coerceIn(0, 100),
            wins = wins,
            losses = losses,
            winRate = if (total > 0) wins * 100 / total else 0,
            trophies = sourceTrophies(context, monster)
        )
    }

    /**
     * Current trophy count on the partner's source backup (bracelet-side).
     * Null when the partner wasn't adopted from a bracelet backup or the
     * backup is unreadable.
     */
    fun sourceTrophies(context: Context, monster: StoredMonster): Int? {
        val backupId = sourceBackupId(monster) ?: return null
        val source = try {
            VBBraceletBackups.get(context, backupId)
        } catch (e: Exception) {
            null
        } ?: return null
        if (source.productId != 2 || source.plain.size != VBBraceletData.DATA_SIZE) return null
        return VBBraceletData.readField(
            source.plain, VBBraceletData.FIELDS.first { it.id == "braceletTrophies" }
        )
    }

    /**
     * Patch the mapped training bytes into [plain] IN PLACE. Returns
     * "label: old → new" lines for the confirm UI / log. Only the 8 mapped
     * bytes are touched; untouched bytes (and their blocks) are left alone.
     */
    fun patchTrainingBytes(plain: ByteArray, p: SyncPreview): List<String> {
        require(plain.size == VBBraceletData.DATA_SIZE) { "blob must be 864 bytes" }
        val changes = mutableListOf<String>()
        fun setU8(off: Int, value: Int, label: String) {
            val old = plain[off].toInt() and 0xFF
            if (old != value) {
                plain[off] = value.toByte()
                VBBraceletData.recomputeBlockChecksum(plain, off)
                changes.add("$label: $old → $value")
            }
        }
        setU8(0x81, p.mental, "Mental")
        setU8(0x91, p.mental, "Mental (mirror)")
        setU8(0x63, p.wins, "Wins")
        setU8(0x73, p.wins, "Wins (mirror)")
        setU8(0x65, p.losses, "Losses")
        setU8(0x75, p.losses, "Losses (mirror)")
        setU8(0x6A, p.winRate, "Win rate %")
        setU8(0x7A, p.winRate, "Win rate % (mirror)")
        return changes
    }

    data class SyncResult(
        val newBackup: VBBraceletBackups.Backup,
        val changes: List<String>,
        val preview: SyncPreview
    )

    /**
     * Build a NEW backup from the source backup with the partner's training
     * patched in. Throws IllegalArgumentException/IllegalStateException with
     * a user-readable message on any problem.
     */
    fun syncToNewBackup(context: Context, monster: StoredMonster): SyncResult {
        val backupId = sourceBackupId(monster)
            ?: throw IllegalArgumentException("This partner wasn't adopted from a bracelet backup.")
        val source = VBBraceletBackups.get(context, backupId)
            ?: throw IllegalStateException(
                "The source backup is gone (deleted?). " +
                        "Read the bracelet again to make a fresh backup first."
            )
        require(source.productId == 2) {
            "Training sync is mapped for VB classic (product 2) bracelets only."
        }
        require(source.plain.size == VBBraceletData.DATA_SIZE) {
            "Source backup blob is corrupt (${source.plain.size} bytes)."
        }

        val p = preview(context, monster)
        val patched = source.plain.copyOf()
        val changes = patchTrainingBytes(patched, p)

        val bad = VBBraceletData.verifyChecksums(patched)
        require(bad.isEmpty()) { "Checksum mismatch after patching (pages $bad) — aborting." }

        val displayName = monster.nickname ?: "Digimon"
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val character = VBBraceletData.BraceletCharacter(
            productId = source.productId,
            uid = VBBraceletBackups.hexToBytes(source.uidHex),
            plain = patched,
            fields = VBBraceletData.parseFields(patched, source.productId)
        )
        val newBackup = VBBraceletBackups.save(
            context, character, note = "$displayName — training synced $stamp"
        )
        Timber.d(
            "VBBraceletSyncBack: ${monster.nickname ?: monster.name} -> " +
                    "backup ${newBackup.id} (${changes.size} bytes changed)"
        )
        return SyncResult(newBackup, changes, p)
    }
}
