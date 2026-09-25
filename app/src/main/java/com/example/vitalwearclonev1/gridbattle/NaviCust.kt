package com.example.vitalwearclonev1.gridbattle

import android.content.Context

/**
 * NaviCust ("Program Grid") for Grid Battle mode (2026-09-25).
 *
 * Pure Kotlin + android.content.Context for save/load only — no Compose.
 * Players place polyomino program parts on a 6x6 grid around a blocked
 * 2x2 core. Connected parts grant stat bonuses; same-colored parts that
 * touch cause GLITCHES (part gives nothing + HP penalty); floating parts
 * are errors. The dominant color picks the battle style.
 */

enum class ProgramColor { RED, BLUE, YELLOW, GREEN }

data class ProgramPart(
    val id: Int,
    val name: String,
    val color: ProgramColor,
    /** Polyomino shape as (row, col) offsets from the origin cell. */
    val cells: List<Pair<Int, Int>>,
    val attackPct: Int = 0,
    val maxHpBonus: Int = 0,
    val speedPct: Int = 0,
    val chargePct: Int = 0,
    val description: String
) {
    fun effectSummary(): String {
        val bits = mutableListOf<String>()
        if (attackPct > 0) bits.add("ATK +$attackPct%")
        if (maxHpBonus > 0) bits.add("HP +$maxHpBonus")
        if (speedPct > 0) bits.add("SPD +$speedPct%")
        if (chargePct > 0) bits.add("CHG +$chargePct%")
        return bits.joinToString(", ")
    }
}

object ProgramParts {
    // Shapes
    private val DOMINO = listOf(0 to 0, 0 to 1)
    private val TRI_LINE = listOf(0 to 0, 0 to 1, 0 to 2)
    private val TRI_L = listOf(0 to 0, 1 to 0, 1 to 1)
    private val SQUARE = listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
    private val TETRA_LINE = listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3)
    private val TETRA_T = listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1)

    val all: List<ProgramPart> = listOf(
        // RED = attack
        ProgramPart(1, "BrawlBit", ProgramColor.RED, DOMINO, attackPct = 5,
            description = "A scrappy little fighter program. +5% attack."),
        ProgramPart(2, "WarEdge", ProgramColor.RED, TRI_LINE, attackPct = 7,
            description = "A honed battle edge. +7% attack."),
        ProgramPart(3, "RageNode", ProgramColor.RED, TRI_L, attackPct = 8,
            description = "Hums with fighting spirit. +8% attack."),
        ProgramPart(4, "StrikeLine", ProgramColor.RED, TETRA_LINE, attackPct = 10,
            description = "A straight line of pure punch. +10% attack."),
        ProgramPart(5, "FuryBlock", ProgramColor.RED, SQUARE, attackPct = 12,
            description = "Dense with battle rage. +12% attack."),
        ProgramPart(6, "TitanCore", ProgramColor.RED, TETRA_T, attackPct = 15,
            description = "The heart of a brawler. +15% attack."),
        // BLUE = speed
        ProgramPart(7, "DashBit", ProgramColor.BLUE, DOMINO, speedPct = 5,
            description = "Light on its feet. +5% move speed."),
        ProgramPart(8, "GlideEdge", ProgramColor.BLUE, TRI_LINE, speedPct = 6,
            description = "Slides across the grid. +6% move speed."),
        ProgramPart(9, "SwiftNode", ProgramColor.BLUE, TRI_L, speedPct = 8,
            description = "Thinks fast, moves faster. +8% move speed."),
        ProgramPart(10, "AeroBlock", ProgramColor.BLUE, SQUARE, speedPct = 10,
            description = "Catches every tailwind. +10% move speed."),
        ProgramPart(11, "ZephyrLine", ProgramColor.BLUE, TETRA_LINE, speedPct = 12,
            description = "A breeze you can program. +12% move speed."),
        ProgramPart(12, "CycloneCore", ProgramColor.BLUE, TETRA_T, speedPct = 15,
            description = "Spins up a storm of speed. +15% move speed."),
        // YELLOW = charge
        ProgramPart(13, "SparkBit", ProgramColor.YELLOW, DOMINO, chargePct = 10,
            description = "A tiny crackle of energy. +10% charge speed."),
        ProgramPart(14, "ZapEdge", ProgramColor.YELLOW, TRI_LINE, chargePct = 12,
            description = "Keeps the buster humming. +12% charge speed."),
        ProgramPart(15, "VoltNode", ProgramColor.YELLOW, TRI_L, chargePct = 15,
            description = "Stores up a shocking surprise. +15% charge speed."),
        ProgramPart(16, "StormCore", ProgramColor.YELLOW, TETRA_T, chargePct = 18,
            description = "A thunderhead in a box. +18% charge speed."),
        ProgramPart(17, "SurgeBlock", ProgramColor.YELLOW, SQUARE, chargePct = 20,
            description = "Power flows through every corner. +20% charge speed."),
        ProgramPart(18, "ChargeLine", ProgramColor.YELLOW, TETRA_LINE, chargePct = 25,
            description = "A lightning rod for buster shots. +25% charge speed."),
        // GREEN = HP / support
        ProgramPart(19, "HeartBit", ProgramColor.GREEN, DOMINO, maxHpBonus = 15,
            description = "A warm little heartbeat. +15 max HP."),
        ProgramPart(20, "CureEdge", ProgramColor.GREEN, TRI_LINE, maxHpBonus = 18,
            description = "Patches up scratches fast. +18 max HP."),
        ProgramPart(21, "MendNode", ProgramColor.GREEN, TRI_L, maxHpBonus = 20,
            description = "Knits wounds while you fight. +20 max HP."),
        ProgramPart(22, "GuardianCore", ProgramColor.GREEN, TETRA_T, maxHpBonus = 25,
            description = "A steadfast protector core. +25 max HP."),
        ProgramPart(23, "VitalBlock", ProgramColor.GREEN, SQUARE, maxHpBonus = 30,
            description = "Solid as a hospital wall. +30 max HP."),
        ProgramPart(24, "LifeLine", ProgramColor.GREEN, TETRA_LINE, maxHpBonus = 40,
            description = "A lifeline straight to victory. +40 max HP.")
    )

    fun byId(id: Int): ProgramPart? = all.find { it.id == id }
    fun byColor(color: ProgramColor): List<ProgramPart> = all.filter { it.color == color }
}

enum class BattleStyle { NONE, BLAZE, AQUA, GALE, TERRA }

data class Placement(
    val partId: Int,
    val originRow: Int,
    val originCol: Int,
    /** 0-3, number of 90-degree clockwise rotations. */
    val rotation: Int
)

/** Rotate a shape 90° clockwise [rotation] times, normalized to origin. */
fun rotatedCells(cells: List<Pair<Int, Int>>, rotation: Int): List<Pair<Int, Int>> {
    var cur = cells
    repeat(((rotation % 4) + 4) % 4) {
        cur = cur.map { (r, c) -> Pair(c, -r) }
        val minR = cur.minOf { it.first }
        val minC = cur.minOf { it.second }
        cur = cur.map { (r, c) -> Pair(r - minR, c - minC) }
    }
    return cur
}

data class TotalBonuses(
    val attackPct: Int,
    val maxHpBonus: Int,
    val speedPct: Int,
    val chargePct: Int
)

data class ValidationResult(
    val errors: List<String>,
    val glitchedPartIds: Set<Int>
) {
    val isOk: Boolean get() = errors.isEmpty()
}

data class NaviCustLoadout(val placements: List<Placement>) {

    companion object {
        const val GRID_SIZE = 6
        /** Blocked 2x2 core: rows 2-3, cols 2-3. */
        val CORE_CELLS: Set<Pair<Int, Int>> = setOf(
            2 to 2, 2 to 3, 3 to 2, 3 to 3
        )
        private const val PREFS = "navicust_prefs"

        private fun key(ownerId: String) =
            "loadout_" + ownerId.replace(Regex("[^A-Za-z0-9_]"), "_")

        fun load(context: Context, ownerId: String): NaviCustLoadout {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(key(ownerId), "") ?: ""
            if (raw.isBlank()) return NaviCustLoadout(emptyList())
            val list = raw.split(";").mapNotNull { tok ->
                val p = tok.split(",")
                if (p.size != 4) return@mapNotNull null
                try {
                    val partId = p[0].toInt()
                    if (ProgramParts.byId(partId) == null) return@mapNotNull null
                    Placement(partId, p[1].toInt(), p[2].toInt(), p[3].toInt())
                } catch (e: Exception) {
                    null
                }
            }
            return NaviCustLoadout(list)
        }
    }

    fun save(context: Context, ownerId: String) {
        val s = placements.joinToString(";") {
            "${it.partId},${it.originRow},${it.originCol},${it.rotation}"
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(ownerId), s)
            .apply()
    }

    /** Absolute grid cells for a placement. */
    fun absoluteCells(p: Placement): List<Pair<Int, Int>> {
        val part = ProgramParts.byId(p.partId) ?: return emptyList()
        return rotatedCells(part.cells, p.rotation)
            .map { (r, c) -> Pair(p.originRow + r, p.originCol + c) }
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        val absPerPlacement = placements.map { absoluteCells(it) }

        // Bounds, core overlap, cell overlap.
        val cellOwner = mutableMapOf<Pair<Int, Int>, Int>() // cell -> placement index
        placements.forEachIndexed { idx, p ->
            val part = ProgramParts.byId(p.partId)
            if (part == null) {
                errors.add("Unknown program #${p.partId}.")
                return@forEachIndexed
            }
            for (cell in absPerPlacement[idx]) {
                val (r, c) = cell
                when {
                    r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE ->
                        errors.add("\"${part.name}\" hangs off the grid.")
                    cell in CORE_CELLS ->
                        errors.add("\"${part.name}\" overlaps the core.")
                    cellOwner.containsKey(cell) -> {
                        val otherName = ProgramParts.byId(placements[cellOwner.getValue(cell)].partId)?.name ?: "?"
                        errors.add("\"${part.name}\" overlaps \"$otherName\".")
                    }
                    else -> cellOwner[cell] = idx
                }
            }
        }

        // Connectivity: every part must touch the core network orthogonally.
        // BFS from the core through placed cells.
        val visited = mutableSetOf<Pair<Int, Int>>().apply { addAll(CORE_CELLS) }
        val queue = ArrayDeque(CORE_CELLS)
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            for ((dr, dc) in dirs) {
                val n = Pair(r + dr, c + dc)
                if (n in cellOwner && n !in visited) {
                    visited.add(n)
                    queue.add(n)
                }
            }
        }
        placements.forEachIndexed { idx, p ->
            val part = ProgramParts.byId(p.partId) ?: return@forEachIndexed
            val cells = absPerPlacement[idx].filter { (r, c) -> r in 0 until GRID_SIZE && c in 0 until GRID_SIZE }
            if (cells.isNotEmpty() && cells.none { it in visited }) {
                errors.add("\"${part.name}\" is not connected to the core.")
            }
        }

        // Glitches: same-colored parts touching orthogonally (not overlapping).
        val glitched = mutableSetOf<Int>()
        val colorOf = placements.map { ProgramParts.byId(it.partId)?.color }
        for (i in placements.indices) {
            for (j in i + 1 until placements.size) {
                if (colorOf[i] == null || colorOf[i] != colorOf[j]) continue
                val aCells = absPerPlacement[i].toSet()
                val bCells = absPerPlacement[j].toSet()
                val touching = aCells.any { (r, c) ->
                    dirs.any { (dr, dc) -> Pair(r + dr, c + dc) in bCells }
                }
                if (touching) {
                    glitched.add(placements[i].partId)
                    glitched.add(placements[j].partId)
                }
            }
        }

        return ValidationResult(errors.distinct(), glitched)
    }

    /** 10 HP per glitched part, applied in battle. */
    fun glitchPenaltyHp(): Int = validate().glitchedPartIds.size * 10

    /** Summed bonuses over NON-glitched parts only. */
    fun totalBonuses(): TotalBonuses {
        val glitched = validate().glitchedPartIds
        var atk = 0; var hp = 0; var spd = 0; var chg = 0
        for (p in placements) {
            if (p.partId in glitched) continue
            val part = ProgramParts.byId(p.partId) ?: continue
            atk += part.attackPct
            hp += part.maxHpBonus
            spd += part.speedPct
            chg += part.chargePct
        }
        return TotalBonuses(atk, hp, spd, chg)
    }

    /** Dominant color among non-glitched parts picks the style. */
    fun style(): BattleStyle {
        val glitched = validate().glitchedPartIds
        val counts = mutableMapOf<ProgramColor, Int>()
        for (p in placements) {
            if (p.partId in glitched) continue
            val color = ProgramParts.byId(p.partId)?.color ?: continue
            counts[color] = (counts[color] ?: 0) + 1
        }
        if (counts.isEmpty()) return BattleStyle.NONE
        return when (counts.maxByOrNull { it.value }!!.key) {
            ProgramColor.RED -> BattleStyle.BLAZE
            ProgramColor.BLUE -> BattleStyle.AQUA
            ProgramColor.YELLOW -> BattleStyle.GALE
            ProgramColor.GREEN -> BattleStyle.TERRA
        }
    }
}
