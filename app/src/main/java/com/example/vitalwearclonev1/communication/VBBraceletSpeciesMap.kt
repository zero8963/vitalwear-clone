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
        // Ultimate (Perfect) — b9/stage from DIM roster (01._Impulse_City_14_amd3.bin,
        // DIM_PARSE_REFERENCE.txt). b13/b15 unknown → experimental write keeps
        // source bytes.
        "Boutmon" to SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Shootmon" to SpeciesBytes(b9 = 0x08, stage = 4, b13 = 0x03, b15 = 0x0F),
        "Divemon" to SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Tempomon" to SpeciesBytes(b9 = 0x0A, stage = 4, b13 = 0x02, b15 = 0x10),
        "Climbmon" to SpeciesBytes(b9 = 0x0B, stage = 4, b13 = 0x01, b15 = 0x10),
        "Pistmon" to SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        // Mega (Ultimate)
        // Kazuchimon: b13=Vaccine(03) from card game data; b15=0x11
        // DEVICE-CONFIRMED 2026-09-28: user wrote app-evolved Kazuchimon to
        // real Hero via Send Evolved — bracelet accepted and displayed correctly.
        "Kazuchimon" to SpeciesBytes(b9 = 0x0D, stage = 5, b13 = 0x03, b15 = 0x11, confirmed = true),
        "Shivamon" to SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Achillesmon" to SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        // Shroudmon: secret evolution (unlock=1 in DIM roster), hardest evo reqs
        // (7000 vitals / 20 trophies / 25 battles / 70% win rate).
        "Shroudmon" to SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
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
        // Rookie — BlackAgumon device-confirmed 2026-09-28 (Koromon evolution):
        // b9=0x03, stage 1->2, b13=0x01 (Virus), b15=0x1F->0x23. Bracelet shows
        // black sprite but names it "Agumon" in status (firmware quirk).
        // Rookie — Agumon (orange) device-confirmed 2026-09-28 (Koromon
        // evolution, 1200-vitals path): b9=0x01->0x02, stage 1->2, b13=0x00->0x03
        // (Vaccine), b15=0x1F->0x24.
        "Agumon" to SpeciesBytes(b9 = 0x02, stage = 2, b13 = 0x03, b15 = 0x24, confirmed = true),
        "BlackAgumon" to SpeciesBytes(b9 = 0x03, stage = 2, b13 = 0x01, b15 = 0x23, confirmed = true),
        // Champion — Monochromon device-confirmed 2026-09-28 (BlackAgumon
        // evolution, 0-vitals path; bracelet status names it MONOCHROMON):
        // b9=0x05, stage 2->3, b13=0x01->0x02, b15=0x23->0x27.
        // NOTE: idx 5 was mislabeled "Greymon (Blue)" — bracelet says Monochromon.
        "Greymon" to SpeciesBytes(b9 = 0x04, stage = 3, b13 = 0x03, b15 = null, confirmed = false),
        "Monochromon" to SpeciesBytes(b9 = 0x05, stage = 3, b13 = 0x02, b15 = 0x27, confirmed = true),
        // idx 6: blue Greymon variant (tool names it "Greymon"; Virus attr).
        // b15 unknown — needs device read.
        "Greymon (Blue)" to SpeciesBytes(b9 = 0x06, stage = 3, b13 = 0x01, b15 = null, confirmed = false),
        // idx 7: DarkTyrannomon — device-confirmed 2026-09-29 (user's bracelet
        // read): b9=0x07, stage 3, b13=0x01 (Virus), b15=0x28.
        "DarkTyrannomon" to SpeciesBytes(b9 = 0x07, stage = 3, b13 = 0x01, b15 = 0x28, confirmed = true),
        // Ultimate
        "MetalGreymon (Vaccine)" to SpeciesBytes(b9 = 0x08, stage = 4, b13 = 0x03, b15 = null, confirmed = false),
        "MetalGreymon (Virus)" to SpeciesBytes(b9 = 0x09, stage = 4, b13 = 0x02, b15 = null, confirmed = false),
        // Ultimate — SkullGreymon device-confirmed 2026-09-28 (Greymon Blue
        // evolution): b9=0x0A, stage 3->4, b13=0x02->0x01 (Virus), b15=0x27->0x2C.
        "SkullGreymon" to SpeciesBytes(b9 = 0x0A, stage = 4, b13 = 0x01, b15 = 0x2C, confirmed = true),
        // Ultimate — Blue MetalGreymon device-confirmed 2026-09-28 (Monochromon
        // evolution): b9=0x05->0x0B, stage 3->4, b13=0x02->0x01, b15=0x27->0x2D.
        // NOTE: idx 11 was mislabeled "Vermilimon" — device shows blue MetalGreymon.
        "MetalGreymon (Blue)" to SpeciesBytes(b9 = 0x0B, stage = 4, b13 = 0x01, b15 = 0x2D, confirmed = true),
        // Mega
        "WarGreymon" to SpeciesBytes(b9 = 0x0C, stage = 5, b13 = 0x03, b15 = null, confirmed = false),
        "VictoryGreymon" to SpeciesBytes(b9 = 0x0D, stage = 5, b13 = 0x03, b15 = null, confirmed = false),
        // Mega — BlackWarGreymon device-confirmed 2026-09-28 (SkullGreymon
        // evolution): b9=0x0E, stage 4->5, b13 stays 0x01 (Virus), b15=0x2C->0x31.
        "BlackWarGreymon" to SpeciesBytes(b9 = 0x0E, stage = 5, b13 = 0x01, b15 = 0x31, confirmed = true),
        // Mega — WarGreymon (orange) device-confirmed 2026-09-28 (Blue MetalGreymon
        // evolution): b9=0x0B->0x0C, stage 4->5, b13=0x01->0x03, b15=0x2D->0x31.
        // NOTE: shares b15=0x31 with BlackWarGreymon; b9 distinguishes them.
        "WarGreymon" to SpeciesBytes(b9 = 0x0C, stage = 5, b13 = 0x03, b15 = 0x31, confirmed = true),
        // Mega — Omegamon forms (from DIM file indices 15-16; b15 needs device read)
        // idx 15 = Omegamon (white, from WarGreymon) device-confirmed 2026-09-28:
        // b9=0x0C->0x0F, stage 5 (unchanged), b13=0x03 (Vaccine, unchanged),
        // b15=0x31->0x34.
        // idx 16 = Omegamon Black / Zwart (secret, unlocked via DIM edit)
        "Omegamon" to SpeciesBytes(b9 = 0x0F, stage = 5, b13 = 0x03, b15 = 0x34, confirmed = true),
        // idx 16 = Omegamon Black / Zwart (secret, unlocked via DIM edit)
        // device-confirmed 2026-09-28 (BlackWarGreymon evolution):
        // b9=0x0E->0x10, stage 5 unchanged, b13=0x01->0x03, b15=0x31->0x46.
        "Omegamon (Black)" to SpeciesBytes(b9 = 0x10, stage = 5, b13 = 0x03, b15 = 0x46, confirmed = true),
    )

    /**
     * Resolve the species map for a card name. Matches fuzzily (case/space/
     * punctuation-insensitive) because imported card names come from user-
     * edited filenames. Ordered specific-first so alternate/v2 cards win
     * over their base names (e.g. "fairimon48" before "fairimon").
     *
     * Returns null if the card isn't recognized.
     */
    private val BUILTIN_MATCHERS: List<Pair<List<String>, Map<String, SpeciesBytes>>> by lazy { listOf(
        listOf("impulse", "pulsecity") to IMPULSE_CITY,
        listOf("dinosaur", "dinosarroar") to DINOSAUR_ROAR,
        listOf("terriermon") to TERRIERMON,
        listOf("ryudamon") to RYUDAMON,
        // NOTE: "renamon" is deliberately last — the Renamon BEM ("renamonbem",
        // "renamon25") must match before the Renamon DIM does.
    ) }

    fun mapFor(cardName: String): Map<String, SpeciesBytes>? {
        val normalized = cardName.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (normalized == "zero") return VBBraceletSpeciesMapsGen.ZERO
        val all = BUILTIN_MATCHERS +
            VBBraceletSpeciesMapsGen.EXTRA_MATCHERS +
            listOf(listOf("renamon") to RENAMON)
        for ((keys, map) in all) {
            if (keys.any { it in normalized }) return map
        }
        return null
    }

    /**
     * Display name -> species map for EVERY reference card (the 5 hand-mapped
     * cards plus all generated ones). Used by pickers and the b15 research
     * name suggestions.
     */
    fun allCards(): LinkedHashMap<String, Map<String, SpeciesBytes>> =
        linkedMapOf(
            "Impulse City" to IMPULSE_CITY,
            "Dinosaur Roar" to DINOSAUR_ROAR,
            "Renamon" to RENAMON,
            "Terriermon" to TERRIERMON,
            "Ryudamon" to RYUDAMON,
        ).apply { putAll(VBBraceletSpeciesMapsGen.EXTRA_CARDS) }

    /**
     * Lookup species bytes by DIM card name and species name.
     * Returns null if the species isn't mapped.
     */
    fun lookup(cardName: String, speciesName: String): SpeciesBytes? =
        mapFor(cardName)?.get(speciesName)

    /**
     * True for "Slot 0xNN" placeholder entries: unmapped roster slots kept
     * selectable for experimental evolve-back writes. These are NOT real
     * species names — never present them as candidates or record them as such.
     */
    fun isPlaceholderSlot(speciesName: String): Boolean =
        speciesName.startsWith("Slot 0x")

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
        // -----
        // Unmapped roster slots: selectable as EXPERIMENTAL. b9/stage from the DIM
        // roster dump; b13/b15 unknown and kept from the source backup on write.
        // Names to be confirmed later — do not treat these labels as real species.
        "Slot 0x04" to SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),

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
        // -----
        // Unmapped roster slots: selectable as EXPERIMENTAL. b9/stage from the DIM
        // roster dump; b13/b15 unknown and kept from the source backup on write.
        // Names to be confirmed later — do not treat these labels as real species.
        "Slot 0x00" to SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),

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
        // -----
        // Unmapped roster slots: selectable as EXPERIMENTAL. b9/stage from the DIM
        // roster dump; b13/b15 unknown and kept from the source backup on write.
        // Names to be confirmed later — do not treat these labels as real species.
        "Slot 0x00" to SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),

    )
}
