package com.example.vitalwearclonev1.communication

/**
 * GENERATED species maps for every other DIM/BEM card file the user provided.
 *
 * Every roster slot is a "Slot 0xNN" placeholder: b9 = the card's roster index and
 * stage come from parsing the card file with the same VB-DIM-Reader library the app
 * uses at import (see /tmp/vbreader/rosters.txt for the raw dumps). b13/b15 are null and
 * confirmed=false, so every one of these flows through the Experimental Write dialog
 * (unknown bytes kept from the source backup, original backup preserved).
 *
 * Placeholder labels are NOT species names — do not record or present them as such.
 * Real names get filled in from device reads / community research over time.
 *
 * Regenerate: re-run the generator against the .bin files in ~/workspace/user/files if cards change.
 */
object VBBraceletSpeciesMapsGen {

    /** Ancient Warriors — 00._Ancient_Warriors_15_bwmz.bin: 17 roster slots, all experimental placeholders. */
    val ANCIENT_WARRIORS: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Agumon — 01._Agumon_16_f02x.bin: 17 roster slots, all experimental placeholders. */
    val AGUMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Black Roar — 01._Black_Roar_12_etp3.bin: 7 roster slots, all experimental placeholders. */
    val BLACK_ROAR: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Guilmon — 01._Guilmon_22_a6h1.bin: 8 roster slots, all experimental placeholders. */
    val GUILMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Gabumon — 02._Gabumon_17_dr73.bin: 17 roster slots, all experimental placeholders. */
    val GABUMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Impmon — 02._Impmon_23_91o8.bin: 8 roster slots, all experimental placeholders. */
    val IMPMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Shadow Howl — 02._Shadow_Howl_13_8lqm.bin: 7 roster slots, all experimental placeholders. */
    val SHADOW_HOWL: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Volcanic Beat — 02._Volcanic_Beat_19_xuo7.bin: 17 roster slots, all experimental placeholders. */
    val VOLCANIC_BEAT: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Blizzard Fang — 03._Blizzard_Fang_18_9lsx.bin: 17 roster slots, all experimental placeholders. */
    val BLIZZARD_FANG: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Monodramon — 03._Monodramon_24_6obc.bin: 8 roster slots, all experimental placeholders. */
    val MONODRAMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Infinite Tide — 04._Infinite_Tide_20_2z73.bin: 17 roster slots, all experimental placeholders. */
    val INFINITE_TIDE: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Renamon (BEM) — 04._Renamon_25_7z4h.bin: 8 roster slots, all experimental placeholders. */
    val RENAMON_BEM: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Titan of Dust — 05._Titan_of_Dust_21_2ymv.bin: 17 roster slots, all experimental placeholders. */
    val TITAN_OF_DUST: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Nu Metal Empire — 07._Nu_Metal_Empire_27_pcea.bin: 17 roster slots, all experimental placeholders. */
    val NU_METAL_EMPIRE: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Dynasty of the Evil — 08._Dynasty_of_The_Evil_18_yx5x.bin: 17 roster slots, all experimental placeholders. */
    val DYNASTY_OF_THE_EVIL: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Dynasty of the Evil (v2) — 08._Dynasty_of_The_Evil_28_avzx.bin: 17 roster slots, all experimental placeholders. */
    val DYNASTY_OF_THE_EVIL_V2: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Mad Black Roar — 09._Mad_Black_Roar_10_x4vv.bin: 17 roster slots, all experimental placeholders. */
    val MAD_BLACK_ROAR: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** True Shadow Howl — 10._True_Shadow_Howl_11_hbmg.bin: 17 roster slots, all experimental placeholders. */
    val TRUE_SHADOW_HOWL: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Gammamon — 11._Gammamon_30_u6xb.bin: 14 roster slots, all experimental placeholders. */
    val GAMMAMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Medarot — 12._Medarot_29_nxc2.bin: 13 roster slots, all experimental placeholders. */
    val MEDAROT: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Agnimon — AGNIMON_42_1ajx.bin: 17 roster slots, all experimental placeholders. */
    val AGNIMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Blitzmon — BLITZMON_43_r0xj.bin: 17 roster slots, all experimental placeholders. */
    val BLITZMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Chackmon — CHACKMON_44_sa2q.bin: 17 roster slots, all experimental placeholders. */
    val CHACKMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Fairimon — FAIRIMON_45_p37y.bin: 17 roster slots, all experimental placeholders. */
    val FAIRIMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Fairimon (v2) — FAIRIMON_48_c3ve.bin: 17 roster slots, all experimental placeholders. */
    val FAIRIMON_V2: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Guilmon EX2 — Guilmon_EX2_33_nq52.bin: 17 roster slots, all experimental placeholders. */
    val GUILMON_EX2: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Hermit in the Jungle — Hermit_in_the_jungle_26_8457.bin: 17 roster slots, all experimental placeholders. */
    val HERMIT_IN_THE_JUNGLE: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Jellymon — Jellymon_32_jxz3.bin: 14 roster slots, all experimental placeholders. */
    val JELLYMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Louwemon — LOUWEMON_46_zl16.bin: 17 roster slots, all experimental placeholders. */
    val LOUWEMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Wolfmon — WOLFMON_47_713s.bin: 17 roster slots, all experimental placeholders. */
    val WOLFMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Wolf Howl — Wolf_Howl_39_gbj7.bin: 17 roster slots, all experimental placeholders. */
    val WOLF_HOWL: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Angoramon — angoramon_31_uts0.bin: 14 roster slots, all experimental placeholders. */
    val ANGORAMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Espimon — espimon_40_cstx.bin: 17 roster slots, all experimental placeholders. */
    val ESPIMON: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Primeval Warriors — primeval-warriors_37_0ndn.bin: 17 roster slots, all experimental placeholders. */
    val PRIMEVAL_WARRIORS: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /** Zero — zero1_4_0xx6.bin: 17 roster slots, all experimental placeholders. */
    val ZERO: Map<String, VBBraceletSpeciesMap.SpeciesBytes> = mapOf(
        "Slot 0x00" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x00, stage = 0, b13 = null, b15 = null, confirmed = false),
        "Slot 0x01" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x01, stage = 1, b13 = null, b15 = null, confirmed = false),
        "Slot 0x02" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x02, stage = 2, b13 = null, b15 = null, confirmed = false),
        "Slot 0x03" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x03, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x04" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x04, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x05" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x05, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x06" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x06, stage = 3, b13 = null, b15 = null, confirmed = false),
        "Slot 0x07" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x07, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x08" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x08, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x09" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x09, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0A" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0A, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0B" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0B, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0C" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0C, stage = 4, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0D" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0D, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0E" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0E, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x0F" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x0F, stage = 5, b13 = null, b15 = null, confirmed = false),
        "Slot 0x10" to VBBraceletSpeciesMap.SpeciesBytes(b9 = 0x10, stage = 5, b13 = null, b15 = null, confirmed = false),
    )

    /**
     * Ordered specific-first matchers: (match keys, species map).
     * v2/alternate cards come before their base names so e.g. "fairimon48"
     * doesn't match plain "fairimon" first.
     */
    val EXTRA_MATCHERS: List<Pair<List<String>, Map<String, VBBraceletSpeciesMap.SpeciesBytes>>> = listOf(
        listOf("dynastyoftheevil28", "evil28avzx") to DYNASTY_OF_THE_EVIL_V2,
        listOf("dynastyoftheevil") to DYNASTY_OF_THE_EVIL,
        listOf("fairimon48") to FAIRIMON_V2,
        listOf("fairimon") to FAIRIMON,
        listOf("madblackroar") to MAD_BLACK_ROAR,
        listOf("blackroar") to BLACK_ROAR,
        listOf("trueshadowhowl") to TRUE_SHADOW_HOWL,
        listOf("shadowhowl") to SHADOW_HOWL,
        listOf("guilmonex") to GUILMON_EX2,
        listOf("guilmon") to GUILMON,
        listOf("renamonbem", "renamon25") to RENAMON_BEM,
        listOf("espimon") to ESPIMON,
        listOf("impmon") to IMPMON,
        listOf("agnimon") to AGNIMON,
        listOf("agumon") to AGUMON,
        listOf("ancientwarriors") to ANCIENT_WARRIORS,
        listOf("angoramon") to ANGORAMON,
        listOf("blitzmon") to BLITZMON,
        listOf("blizzardfang") to BLIZZARD_FANG,
        listOf("chackmon") to CHACKMON,
        listOf("gabumon") to GABUMON,
        listOf("gammamon") to GAMMAMON,
        listOf("hermitinthejungle") to HERMIT_IN_THE_JUNGLE,
        listOf("infinitetide") to INFINITE_TIDE,
        listOf("jellymon") to JELLYMON,
        listOf("louwemon") to LOUWEMON,
        listOf("medarot") to MEDAROT,
        listOf("monodramon") to MONODRAMON,
        listOf("numetalempire") to NU_METAL_EMPIRE,
        listOf("primevalwarriors") to PRIMEVAL_WARRIORS,
        listOf("titanofdust") to TITAN_OF_DUST,
        listOf("volcanicbeat") to VOLCANIC_BEAT,
        listOf("wolfmon") to WOLFMON,
        listOf("wolfhowl") to WOLF_HOWL,
        listOf("zero1") to ZERO,
    )

    /** Display name -> species map for every generated card. */
    val EXTRA_CARDS: LinkedHashMap<String, Map<String, VBBraceletSpeciesMap.SpeciesBytes>> = linkedMapOf(
        "Ancient Warriors" to ANCIENT_WARRIORS,
        "Agumon" to AGUMON,
        "Black Roar" to BLACK_ROAR,
        "Guilmon" to GUILMON,
        "Gabumon" to GABUMON,
        "Impmon" to IMPMON,
        "Shadow Howl" to SHADOW_HOWL,
        "Volcanic Beat" to VOLCANIC_BEAT,
        "Blizzard Fang" to BLIZZARD_FANG,
        "Monodramon" to MONODRAMON,
        "Infinite Tide" to INFINITE_TIDE,
        "Renamon (BEM)" to RENAMON_BEM,
        "Titan of Dust" to TITAN_OF_DUST,
        "Nu Metal Empire" to NU_METAL_EMPIRE,
        "Dynasty of the Evil" to DYNASTY_OF_THE_EVIL,
        "Dynasty of the Evil (v2)" to DYNASTY_OF_THE_EVIL_V2,
        "Mad Black Roar" to MAD_BLACK_ROAR,
        "True Shadow Howl" to TRUE_SHADOW_HOWL,
        "Gammamon" to GAMMAMON,
        "Medarot" to MEDAROT,
        "Agnimon" to AGNIMON,
        "Blitzmon" to BLITZMON,
        "Chackmon" to CHACKMON,
        "Fairimon" to FAIRIMON,
        "Fairimon (v2)" to FAIRIMON_V2,
        "Guilmon EX2" to GUILMON_EX2,
        "Hermit in the Jungle" to HERMIT_IN_THE_JUNGLE,
        "Jellymon" to JELLYMON,
        "Louwemon" to LOUWEMON,
        "Wolfmon" to WOLFMON,
        "Wolf Howl" to WOLF_HOWL,
        "Angoramon" to ANGORAMON,
        "Espimon" to ESPIMON,
        "Primeval Warriors" to PRIMEVAL_WARRIORS,
        "Zero" to ZERO,
    )
}
