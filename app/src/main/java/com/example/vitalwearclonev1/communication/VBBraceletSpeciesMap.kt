package com.example.vitalwearclonev1.communication

/**
 * Species byte map for the Vital Bracelet (product 2).
 *
 * Mapped live on the user's real Hero via read→evolve→read diffs (2026-09-27/28).
 * - b9  (0x0049/0x0059): species ID = roster index in the DIM card.
 * - b12 (0x004C/0x005C): stage (0=Baby I, 1=Baby II, 2=Child, 3=Adult, 4=Ultimate, 5=Mega).
 * - b13 (0x004D/0x005D): attribute (01=Virus, 02=Data, 03=Vaccine). Species-linked, not stage-linked.
 * - b15 (0x004F/0x005F): species constant. Changes on evolution, exact meaning unresolved.
 *
 * DO NOT edit species bytes directly unless all four are known. Unknown b13/b15
 * will produce a corrupt character on the bracelet.
 */
object VBBraceletSpeciesMap {

    data class SpeciesBytes(
        val b9: Int,
        val stage: Int,
        val b13: Int?,
        val b15: Int?,
        /** True if b13/b15 are device-confirmed (not estimated). */
        val confirmed: Boolean = true
    )

    /**
     * Impulse City (Pulsemon) DIM species map.
     * Roster order from the DIM card; b9 = roster index.
     */
    val IMPULSE_CITY: Map<String, SpeciesBytes> = mapOf(
        // Baby I
        "Dokimon" to SpeciesBytes(b9 = 0x00, stage = 0, b13 = 0x00, b15 = 0x00),
        // Baby II
        "Bibimon" to SpeciesBytes(b9 = 0x01, stage = 1, b13 = 0x00, b15 = 0x02),
        // Child
        "Pulsemon" to SpeciesBytes(b9 = 0x02, stage = 2, b13 = 0x03, b15 = 0x07),
        // Adult
        "Bulkmon" to SpeciesBytes(b9 = 0x03, stage = 3, b13 = 0x01, b15 = 0x09),
        "Exermon" to SpeciesBytes(b9 = 0x04, stage = 3, b13 = 0x02, b15 = 0x09),
        "Runnermon" to SpeciesBytes(b9 = 0x05, stage = 3, b13 = 0x01, b15 = 0x09),
        "Namakemon" to SpeciesBytes(b9 = 0x06, stage = 3, b13 = 0x01, b15 = 0x0A),
        // Ultimate (Perfect)
        // Boutmon: b13/b15 not yet mapped (needs device read)
        "Shootmon" to SpeciesBytes(b9 = 0x08, stage = 4, b13 = 0x03, b15 = 0x0F),
        // Divemon: b13/b15 not yet mapped
        "Tempomon" to SpeciesBytes(b9 = 0x0A, stage = 4, b13 = 0x02, b15 = 0x10),
        "Climbmon" to SpeciesBytes(b9 = 0x0B, stage = 4, b13 = 0x01, b15 = 0x10),
        // Pistmon: b13/b15 not yet mapped
        // Mega (Ultimate)
        // Kazuchimon: b13=Vaccine(03) from card game data; b15=0x11
        // DEVICE-CONFIRMED 2026-09-28: user wrote app-evolved Kazuchimon to
        // real Hero via Send Evolved — bracelet accepted and displayed correctly.
        "Kazuchimon" to SpeciesBytes(b9 = 0x0D, stage = 5, b13 = 0x03, b15 = 0x11, confirmed = true),
        // Shivamon, Achillesmon, Shroudmon: not yet mapped
    )

    /**
     * Lookup species bytes by DIM card name and species name.
     * Returns null if the species isn't mapped.
     */
    fun lookup(cardName: String, speciesName: String): SpeciesBytes? {
        // Normalize card name: "Impulse City", "Pulse City", "Pulsecity" all match.
        val normalized = cardName.lowercase().replace(" ", "")
        return when {
            "impulse" in normalized || "pulse" in normalized -> IMPULSE_CITY[speciesName]
            else -> null
        }
    }
}
