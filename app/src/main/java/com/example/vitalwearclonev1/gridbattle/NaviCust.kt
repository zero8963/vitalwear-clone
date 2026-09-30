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

/**
 * Element + animation override for the fighter's basic (buster) or sword
 * attack, installed by special "Core" programs.
 *
 * ENGINE NOTE (2026-09-25): the grid battle engine must implement these
 * 8 animation keys exactly (animKey -> meaning):
 *   Buster: "shot_flame" (FIRE), "shot_tide" (WATER), "shot_volt" (ELEC), "shot_thorn" (WOOD)
 *   Sword:  "slash_cinder" (FIRE), "slash_reef" (WATER), "slash_storm" (ELEC), "slash_bramble" (WOOD)
 * The key picks the animation; the element picks damage typing/effects.
 */
data class AttackOverride(val element: ChipElement, val animKey: String) {
    /** Kid-friendly display name derived from the animation key, e.g. "Flame Shot". */
    fun displayName(): String = when (animKey) {
        "shot_flame" -> "Flame Shot"
        "shot_tide" -> "Tide Shot"
        "shot_volt" -> "Volt Shot"
        "shot_thorn" -> "Thorn Shot"
        "slash_cinder" -> "Cinder Slash"
        "slash_reef" -> "Reef Slash"
        "slash_storm" -> "Storm Slash"
        "slash_bramble" -> "Bramble Slash"
        else -> animKey
    }
}

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
    val description: String,
    /** Non-null for Core parts: rewires the buster's element + animation. */
    val busterOverride: AttackOverride? = null,
    /** Non-null for Core parts: rewires the sword's element + animation. */
    val swordOverride: AttackOverride? = null,
    /** % chance (0..1) each of your hits applies the burn coat (Net World). */
    val burnCoatChance: Float = 0f,
    /** Burn applied by the coat (dps scaled by ATK at hit time). */
    val burnCoat: HitStatus = HitStatus(),
    /** % chance (0..1) each of your hits applies the poison coat (Net World). */
    val poisonCoatChance: Float = 0f,
    /** Poison applied by the coat (dps scaled by ATK at hit time). */
    val poisonCoat: HitStatus = HitStatus(),
    /** Flat flinch resistance vs interrupts (Net World). */
    val hyperArmor: Int = 0,
    /** Bonus flinch chance (0..1) added to your hits (Net World). */
    val flinchBonus: Float = 0f,
    /** % of burn DoT ignored when you are burning (Net World). */
    val burnResistPct: Int = 0,
    /** % of poison DoT ignored when you are poisoned (Net World). */
    val poisonResistPct: Int = 0
) {
    /** True for the special Core parts that rewire basic attacks. */
    fun isCore(): Boolean = busterOverride != null || swordOverride != null

    fun effectSummary(): String {
        val bits = mutableListOf<String>()
        if (attackPct > 0) bits.add("ATK +$attackPct%")
        if (maxHpBonus > 0) bits.add("HP +$maxHpBonus")
        if (speedPct > 0) bits.add("SPD +$speedPct%")
        if (chargePct > 0) bits.add("CHG +$chargePct%")
        if (burnCoatChance > 0f) bits.add("\uD83D\uDD25 coat ${(burnCoatChance * 100).toInt()}%")
        if (poisonCoatChance > 0f) bits.add("☠ coat ${(poisonCoatChance * 100).toInt()}%")
        if (hyperArmor > 0) bits.add("HA +$hyperArmor")
        if (flinchBonus > 0f) bits.add("Flinch +${(flinchBonus * 100).toInt()}%")
        if (burnResistPct > 0) bits.add("\uD83D\uDD25Resist $burnResistPct%")
        if (poisonResistPct > 0) bits.add("☠Resist $poisonResistPct%")
        busterOverride?.let { bits.add("Buster: ${it.displayName()}") }
        swordOverride?.let { bits.add("Sword: ${it.displayName()}") }
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
    private val TETRA_SKEW = listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2)

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
            description = "A lifeline straight to victory. +40 max HP."),
        // --- CORE parts: rewire the buster's element + animation ---
        ProgramPart(25, "EmberCore", ProgramColor.RED, TETRA_T, attackPct = 5,
            busterOverride = AttackOverride(ChipElement.FIRE, "shot_flame"),
            description = "Rewires your buster into a blazing Flame Shot (FIRE)! +5% attack."),
        ProgramPart(26, "TideCore", ProgramColor.BLUE, SQUARE,
            busterOverride = AttackOverride(ChipElement.WATER, "shot_tide"),
            speedPct = 5,
            description = "Your buster becomes a splashing Tide Shot (WATER)! +5% move speed."),
        ProgramPart(27, "VoltCore", ProgramColor.YELLOW, TETRA_LINE,
            busterOverride = AttackOverride(ChipElement.ELEC, "shot_volt"),
            chargePct = 8,
            description = "Your buster crackles as a Volt Shot (ELEC)! +8% charge speed."),
        ProgramPart(28, "ThornCore", ProgramColor.GREEN, TETRA_SKEW,
            busterOverride = AttackOverride(ChipElement.WOOD, "shot_thorn"),
            maxHpBonus = 10,
            description = "Your buster bursts into a Thorn Shot (WOOD)! +10 max HP."),
        // --- CORE parts: rewire the sword's element + animation ---
        ProgramPart(29, "CinderEdge", ProgramColor.RED, TETRA_SKEW,
            swordOverride = AttackOverride(ChipElement.FIRE, "slash_cinder"),
            attackPct = 5,
            description = "Your sword becomes a Cinder Slash (FIRE) that leaves embers! +5% attack."),
        ProgramPart(30, "ReefEdge", ProgramColor.BLUE, TETRA_T,
            swordOverride = AttackOverride(ChipElement.WATER, "slash_reef"),
            speedPct = 5,
            description = "Your sword crashes like a Reef Slash (WATER) wave! +5% move speed."),
        ProgramPart(31, "StormEdge", ProgramColor.YELLOW, SQUARE,
            swordOverride = AttackOverride(ChipElement.ELEC, "slash_storm"),
            chargePct = 8,
            description = "Your sword strikes as a lightning Storm Slash (ELEC)! +8% charge speed."),
        ProgramPart(32, "BrambleEdge", ProgramColor.GREEN, TETRA_LINE,
            swordOverride = AttackOverride(ChipElement.WOOD, "slash_bramble"),
            maxHpBonus = 10,
            description = "Your sword tangles foes in a Bramble Slash (WOOD)! +10 max HP."),
        // --- STATUS parts (2026-09-30): coatings, hyper armor, resists (Net World) ---
        ProgramPart(33, "EmberCoat", ProgramColor.RED, TRI_L,
            burnCoatChance = 0.25f, burnCoat = HitStatus(burnDps = 8f, burnSecs = 3f),
            description = "Your hits may ignite foes in the Net World (25% burn)."),
        ProgramPart(34, "InfernoWeave", ProgramColor.RED, TETRA_SKEW,
            burnCoatChance = 0.4f, burnCoat = HitStatus(burnDps = 15f, burnSecs = 4f),
            description = "Woven wildfire: your hits often ignite foes in the Net World (40% burn)."),
        ProgramPart(35, "ImpactEdge", ProgramColor.RED, DOMINO,
            flinchBonus = 0.2f,
            description = "Heavy hits stagger foes in the Net World (+20% flinch)."),
        ProgramPart(36, "VenomCoat", ProgramColor.GREEN, TRI_L,
            poisonCoatChance = 0.25f, poisonCoat = HitStatus(poisonDps = 6f, poisonSecs = 5f),
            description = "Your hits may poison foes in the Net World (25%)."),
        ProgramPart(37, "PlagueWeave", ProgramColor.GREEN, TETRA_SKEW,
            poisonCoatChance = 0.4f, poisonCoat = HitStatus(poisonDps = 12f, poisonSecs = 6f),
            description = "Woven plague: your hits often poison foes in the Net World (40%)."),
        ProgramPart(38, "IronStance", ProgramColor.GREEN, SQUARE,
            hyperArmor = 2,
            description = "Shrug off interrupts in the Net World (+2 hyper armor)."),
        ProgramPart(39, "CoolantVeil", ProgramColor.BLUE, TRI_LINE,
            burnResistPct = 50, poisonResistPct = 50,
            description = "Halves burn and poison damage you take in the Net World."),
        ProgramPart(40, "TitanStance", ProgramColor.YELLOW, TETRA_T,
            hyperArmor = 4,
            description = "An unmovable stance in the Net World (+4 hyper armor).")
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
    val chargePct: Int,
    /** Summed coat chance (capped at 100%); coat potency = strongest part. */
    val burnCoatChance: Float = 0f,
    val burnCoat: HitStatus = HitStatus(),
    val poisonCoatChance: Float = 0f,
    val poisonCoat: HitStatus = HitStatus(),
    val hyperArmor: Int = 0,
    val flinchBonus: Float = 0f,
    val burnResistPct: Int = 0,
    val poisonResistPct: Int = 0
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

        // Core limits: at most one INSTALLED buster core and one sword core.
        // Glitched cores don't count as installed.
        val busterCores = placements.mapNotNull { p ->
            val part = ProgramParts.byId(p.partId)
            if (part?.busterOverride != null && p.partId !in glitched) part.name else null
        }
        if (busterCores.size > 1) {
            errors.add("Only one Buster Core fits \u2014 remove ${busterCores.joinToString(" or ")}.")
        }
        val swordCores = placements.mapNotNull { p ->
            val part = ProgramParts.byId(p.partId)
            if (part?.swordOverride != null && p.partId !in glitched) part.name else null
        }
        if (swordCores.size > 1) {
            errors.add("Only one Sword Core fits \u2014 remove ${swordCores.joinToString(" or ")}.")
        }

        return ValidationResult(errors.distinct(), glitched)
    }

    /** 10 HP per glitched part, applied in battle. */
    fun glitchPenaltyHp(): Int = validate().glitchedPartIds.size * 10

    /** Summed bonuses over NON-glitched parts only. */
    fun totalBonuses(): TotalBonuses {
        val glitched = validate().glitchedPartIds
        var atk = 0; var hp = 0; var spd = 0; var chg = 0
        var burnChance = 0f; var burnDps = 0f; var burnSecs = 0f
        var poisonChance = 0f; var poisonDps = 0f; var poisonSecs = 0f
        var ha = 0; var flinch = 0f; var burnRes = 0; var poisonRes = 0
        for (p in placements) {
            if (p.partId in glitched) continue
            val part = ProgramParts.byId(p.partId) ?: continue
            atk += part.attackPct
            hp += part.maxHpBonus
            spd += part.speedPct
            chg += part.chargePct
            burnChance += part.burnCoatChance
            if (part.burnCoat.burnDps > burnDps) burnDps = part.burnCoat.burnDps
            if (part.burnCoat.burnSecs > burnSecs) burnSecs = part.burnCoat.burnSecs
            poisonChance += part.poisonCoatChance
            if (part.poisonCoat.poisonDps > poisonDps) poisonDps = part.poisonCoat.poisonDps
            if (part.poisonCoat.poisonSecs > poisonSecs) poisonSecs = part.poisonCoat.poisonSecs
            ha += part.hyperArmor
            flinch += part.flinchBonus
            burnRes += part.burnResistPct
            poisonRes += part.poisonResistPct
        }
        return TotalBonuses(
            atk, hp, spd, chg,
            burnCoatChance = burnChance.coerceAtMost(1f),
            burnCoat = HitStatus(burnDps = burnDps, burnSecs = burnSecs),
            poisonCoatChance = poisonChance.coerceAtMost(1f),
            poisonCoat = HitStatus(poisonDps = poisonDps, poisonSecs = poisonSecs),
            hyperArmor = ha,
            flinchBonus = flinch.coerceAtMost(1f),
            burnResistPct = burnRes.coerceAtMost(90),
            poisonResistPct = poisonRes.coerceAtMost(90)
        )
    }

    /** Installed (non-glitched) parts only — glitched cores grant nothing. */
    private fun activeParts(): List<ProgramPart> {
        val glitched = validate().glitchedPartIds
        return placements.mapNotNull { p ->
            if (p.partId in glitched) null else ProgramParts.byId(p.partId)
        }
    }

    /** The installed buster override, or null for the default buster. */
    fun busterOverride(): AttackOverride? =
        activeParts().mapNotNull { it.busterOverride }.firstOrNull()

    /** The installed sword override, or null for the default sword. */
    fun swordOverride(): AttackOverride? =
        activeParts().mapNotNull { it.swordOverride }.firstOrNull()

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
