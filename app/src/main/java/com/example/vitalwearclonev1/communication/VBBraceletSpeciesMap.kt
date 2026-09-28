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
     * Dinosaur Roar (Agumon/BlackAgumon) DIM species map.
     * Roster order + attributes (b13) parsed directly from the DIM file
     * (Dinosaur_Roar_38_2i5j.DIM, character stats at 0x30000, bitwise-NOT encoded).
     * Attribute encoding: 0=None (Baby), 1=Virus, 2=Data, 3=Vaccine, 4=Free/Variable
     * (attr 4 confirmed real 2026-09-28: present in official DIMs — Frontier spirit
     * Digimon, Ancient Warriors, Primeval Warriors, Medarot; per cfogrady/VB-DIM-Reader
     * character-table layout. Exact bracelet display label needs device confirmation).
     * b15 NOT YET MAPPED — needs a real bracelet read from this DIM.
     */
    val DINOSAUR_ROAR: Map<String, SpeciesBytes> = mapOf(
        // Fresh — device-observed 2026-09-28: b9 read 0x1D (= DIM ID 29) as a
        // placeholder on fresh hatch, corrected to roster idx on evolution.
        // b15 was also 0x1D (placeholder); real Botamon b15 unknown.
        "Botamon" to SpeciesBytes(b9 = 0x00, stage = 0, b13 = 0x00, b15 = null, confirmed = false),
        // In-Training — device-confirmed 2026-09-28: b9 corrected to 0x01,
        // stage 0->1, b15 0x1D->0x1F on Botamon->Koromon evolution.
        "Koromon" to SpeciesBytes(b9 = 0x01, stage = 1, b13 = 0x00, b15 = 0x1F, confirmed = true),
        // Rookie
        "Agumon" to SpeciesBytes(b9 = 0x02, stage = 2, b13 = 0x03, b15 = null, confirmed = false),
        "BlackAgumon" to SpeciesBytes(b9 = 0x03, stage = 2, b13 = 0x01, b15 = null, confirmed = false),
        // Champion
        "Greymon" to SpeciesBytes(b9 = 0x04, stage = 3, b13 = 0x03, b15 = null, confirmed = false),
        "Greymon (Blue)" to SpeciesBytes(b9 = 0x05, stage = 3, b13 = 0x02, b15 = null, confirmed = false),
        "Monochromon" to SpeciesBytes(b9 = 0x06, stage = 3, b13 = 0x01, b15 = null, confirmed = false),
        "DarkTyrannomon" to SpeciesBytes(b9 = 0x07, stage = 3, b13 = 0x01, b15 = null, confirmed = false),
        // Ultimate
        "MetalGreymon (Vaccine)" to SpeciesBytes(b9 = 0x08, stage = 4, b13 = 0x03, b15 = null, confirmed = false),
        "MetalGreymon (Virus)" to SpeciesBytes(b9 = 0x09, stage = 4, b13 = 0x02, b15 = null, confirmed = false),
        "SkullGreymon" to SpeciesBytes(b9 = 0x0A, stage = 4, b13 = 0x01, b15 = null, confirmed = false),
        "Vermilimon" to SpeciesBytes(b9 = 0x0B, stage = 4, b13 = 0x01, b15 = null, confirmed = false),
        // Mega
        "WarGreymon" to SpeciesBytes(b9 = 0x0C, stage = 5, b13 = 0x03, b15 = null, confirmed = false),
        "VictoryGreymon" to SpeciesBytes(b9 = 0x0D, stage = 5, b13 = 0x03, b15 = null, confirmed = false),
        "BlackWarGreymon" to SpeciesBytes(b9 = 0x0E, stage = 5, b13 = 0x01, b15 = null, confirmed = false),
        // Jogress (from DIM file indices 15-16; names need confirmation)
        // "Omegamon" to SpeciesBytes(b9 = 0x0F, stage = 5, b13 = 0x03, b15 = null, confirmed = false),
        // "Omegamon Zwart" to SpeciesBytes(b9 = 0x10, stage = 5, b13 = 0x03, b15 = null, confirmed = false),
    )

    /**
     * Lookup species bytes by DIM card name and species name.
     * Returns null if the species isn't mapped.
     */
    fun lookup(cardName: String, speciesName: String): SpeciesBytes? {
        // Normalize card name: "Impulse City", "Pulse City", "Pulsecity" all match.
        val normalized = cardName.lowercase().replace(" ", "")
        return when {
            "impulse" in normalized || "pulsecity" in normalized -> IMPULSE_CITY[speciesName]
            "dinosaur" in normalized || "dinosarroar" in normalized -> DINOSAUR_ROAR[speciesName]
            "renamon" in normalized -> RENAMON[speciesName]
            "terriermon" in normalized -> TERRIERMON[speciesName]
            "ryudamon" in normalized -> RYUDAMON[speciesName]
            else -> null
        }
    }

    /**
     * Renamon DIM species map.
     * Roster order + attributes (b13) parsed directly from the DIM file
     * (DIM_Renamon_35_5mmd.bin, character stats at 0x30000, bitwise-NOT encoded).
     * Attribute encoding: 0=None (Baby), 1=Virus, 2=Data, 3=Vaccine, 4=Free/Variable
     * (attr 4 confirmed in official DIMs 2026-09-28, see note on IMPULSE_CITY).
     * b15 NOT YET MAPPED — needs a real bracelet read from this DIM.
     *
     * DIM file slots (b9/stage/b13):
     *  00: Fresh/0, 01: Baby II/0, 02: Rookie/Data(2), 03: Champion/Data(2),
     *  04: Champion/Data(2), 05: Champion/Virus(1), 06: Champion/attr4,
     *  07: Ultimate/Vaccine(3), 08: Ultimate/Vaccine(3), 09: Ultimate/Data(2),
     *  10: Ultimate/Data(2), 11: Ultimate/Data(2), 12: Ultimate/Virus(1),
     *  13: Mega/Data(2), 14: Mega/Virus(1), 15: Mega/Data(2), 16: Mega/Data(2, unlock=1)
     * Alternate path names (slots 04-16) need device confirmation.
     */
    val RENAMON: Map<String, SpeciesBytes> = mapOf(
        // Fresh
        "Relemon" to SpeciesBytes(b9 = 0x00, stage = 0, b13 = 0x00, b15 = null, confirmed = false),
        // In-Training
        "Viximon" to SpeciesBytes(b9 = 0x01, stage = 1, b13 = 0x00, b15 = null, confirmed = false),
        // Rookie
        "Renamon" to SpeciesBytes(b9 = 0x02, stage = 2, b13 = 0x02, b15 = null, confirmed = false),
        // Champion (main line)
        "Kyubimon" to SpeciesBytes(b9 = 0x03, stage = 3, b13 = 0x02, b15 = null, confirmed = false),
        // Ultimate (main line: Taomon is one of slots 07-12, needs device ID)
        // Mega (main line: Sakuyamon/Kuzuhamon/Sakuyamon Maid Mode are slots 13-16, needs device ID)
    )

    /**
     * Terriermon DIM species map.
     * Roster order + attributes (b13) parsed directly from the DIM file
     * (DIM_Terriermon_36_bmvp.bin, character stats at 0x30000, bitwise-NOT encoded).
     * b15 NOT YET MAPPED — needs a real bracelet read from this DIM.
     *
     * DIM file slots (b9/stage/b13):
     *  00: Fresh/0, 01: Baby II/0, 02: Rookie/Vaccine(3), 03: Champion/Vaccine(3),
     *  04: Champion/Data(2), 05: Champion/attr4, 06: Champion/Virus(1),
     *  07: Ultimate/Vaccine(3), 08: Ultimate/Data(2), 09: Ultimate/Data(2),
     *  10: Ultimate/Data(2), 11: Ultimate/Virus(1), 12: Ultimate/Virus(1),
     *  13: Mega/Vaccine(3), 14: Mega/Vaccine(3), 15: Mega/Vaccine(3), 16: Mega/Vaccine(3, unlock=1)
     * Alternate path names (slots 04-16) need device confirmation.
     */
    val TERRIERMON: Map<String, SpeciesBytes> = mapOf(
        // Fresh (Gummymon is Baby II per card game data; Baby I name unconfirmed)
        // In-Training
        "Gummymon" to SpeciesBytes(b9 = 0x01, stage = 1, b13 = 0x00, b15 = null, confirmed = false),
        // Rookie
        "Terriermon" to SpeciesBytes(b9 = 0x02, stage = 2, b13 = 0x03, b15 = null, confirmed = false),
        // Champion (main line)
        "Gargomon" to SpeciesBytes(b9 = 0x03, stage = 3, b13 = 0x03, b15 = null, confirmed = false),
        // Ultimate (main line: Rapidmon is one of slots 07-12, needs device ID)
        // Mega (main line: MegaGargomon/SaintGargomon are slots 13-16, needs device ID)
    )

    /**
     * Ryudamon DIM species map.
     * Roster order + attributes (b13) parsed directly from the DIM file
     * (DIM_Ryudamon_41_7x5x.bin, character stats at 0x30000, bitwise-NOT encoded).
     * b15 NOT YET MAPPED — needs a real bracelet read from this DIM.
     *
     * DIM file slots (b9/stage/b13):
     *  00: Fresh/0, 01: Baby II/0, 02: Rookie/Vaccine(3), 03: Rookie/Vaccine(3),
     *  04: Champion/Vaccine(3), 05: Champion/Vaccine(3), 06: Champion/Virus(1), 07: Champion/Virus(1),
     *  08: Ultimate/Vaccine(3), 09: Ultimate/Vaccine(3), 10: Ultimate/Vaccine(3),
     *  11: Ultimate/Virus(1), 12: Ultimate/Virus(1),
     *  13: Mega/Vaccine(3), 14: Mega/Vaccine(3), 15: Mega/Virus(1), 16: Mega/Virus(1)
     * Alternate path names need device confirmation.
     */
    val RYUDAMON: Map<String, SpeciesBytes> = mapOf(
        // Fresh (Fufumon per community data, unconfirmed)
        // In-Training (Kyokyomon per community data, unconfirmed)
        // Rookie (main line)
        "Ryudamon" to SpeciesBytes(b9 = 0x02, stage = 2, b13 = 0x03, b15 = null, confirmed = false),
        // Champion/Ultimate/Mega names need device confirmation
    )
}
