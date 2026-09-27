package com.example.vitalwearclonev1.communication

import timber.log.Timber

/**
 * Byte-level diff of two saved bracelet character backups.
 *
 * Goal: the user reads the bracelet with Digimon A, reads again with
 * Digimon B, and diffs the two decrypted 864-byte blobs. The bytes that
 * change with the species are its ID field — this tool surfaces them.
 *
 * Differing offsets are grouped into maximal contiguous ranges; each range
 * is annotated with any known [VBBraceletData.CharField]s it overlaps, and
 * short unknown ranges (<= 4 bytes, no known field) are flagged as
 * species-ID candidates.
 */
object VBBraceletDiff {

    data class DiffRange(
        /** Blob offset of the first differing byte. */
        val start: Int,
        /** Blob offset one past the last differing byte. */
        val endExclusive: Int,
        val aBytes: ByteArray,
        val bBytes: ByteArray,
        /** Known fields overlapping this range (may be empty). */
        val fields: List<VBBraceletData.CharField>
    ) {
        val length: Int get() = endExclusive - start
        /** Short unknown change — the prime species-ID candidate shape. */
        val isCandidate: Boolean get() = fields.isEmpty() && length <= 4
        /** Absolute tag page of [start] (page 8 = blob offset 0). */
        val startPage: Int get() = 8 + start / 4
    }

    data class FieldChange(
        val field: VBBraceletData.CharField,
        val aValue: Int,
        val bValue: Int
    )

    data class DiffResult(
        val ranges: List<DiffRange>,
        val totalBytes: Int,
        val fieldChanges: List<FieldChange>,
        /** Short unknown ranges — check these first for the species ID. */
        val candidates: List<DiffRange>
    )

    fun diff(a: VBBraceletBackups.Backup, b: VBBraceletBackups.Backup): DiffResult {
        require(a.productId == b.productId) {
            "cannot diff different products (${a.productId} vs ${b.productId})"
        }
        require(a.plain.size == VBBraceletData.DATA_SIZE && b.plain.size == VBBraceletData.DATA_SIZE) {
            "backups must hold full ${VBBraceletData.DATA_SIZE}-byte blobs"
        }
        val productId = a.productId
        val fields = VBBraceletData.FIELDS.filter { productId in it.products }

        // Maximal contiguous runs of differing offsets.
        val ranges = mutableListOf<DiffRange>()
        var i = 0
        while (i < VBBraceletData.DATA_SIZE) {
            if (a.plain[i] != b.plain[i]) {
                val start = i
                while (i < VBBraceletData.DATA_SIZE && a.plain[i] != b.plain[i]) i++
                val overlapping = fields.filter { f ->
                    val size = if (f.type == VBBraceletData.FieldType.U8) 1 else 2
                    f.offset < i && f.offset + size > start
                }
                ranges.add(
                    DiffRange(
                        start, i,
                        a.plain.copyOfRange(start, i),
                        b.plain.copyOfRange(start, i),
                        overlapping
                    )
                )
            } else {
                i++
            }
        }

        val fieldChanges = fields.mapNotNull { f ->
            val av = VBBraceletData.readField(a.plain, f)
            val bv = VBBraceletData.readField(b.plain, f)
            if (av != bv) FieldChange(f, av, bv) else null
        }

        val result = DiffResult(
            ranges = ranges,
            totalBytes = ranges.sumOf { it.length },
            fieldChanges = fieldChanges,
            candidates = ranges.filter { it.isCandidate }
        )
        Timber.d(
            "VBBraceletDiff ${a.id} vs ${b.id}: ${result.totalBytes} bytes " +
                    "in ${ranges.size} ranges, ${fieldChanges.size} known fields, " +
                    "${result.candidates.size} candidates"
        )
        return result
    }

    /** Compact hex for one row of a range: "0x00A0: A: …  B: …". */
    fun formatRow(offset: Int, a: ByteArray, b: ByteArray): String {
        val sb = StringBuilder("0x%04X: ".format(offset))
        sb.append("A: ")
        for (x in a) sb.append("%02X ".format(x))
        sb.append(" B: ")
        for (x in b) sb.append("%02X ".format(x))
        return sb.toString().trim()
    }
}
