package com.example.vitalwearclonev1.gridbattle

import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Real-time grid battle engine, player-vs-AI (2026-09-25).
 *
 * Pure Kotlin simulation — no Android/Compose. The UI drives it with
 * [update] on a ~60fps loop and renders [snapshot].
 *
 * Grid is 6 cols x 3 rows. The player owns cols 0-2, the AI owns cols 3-5.
 * All damage numbers are provisional starting values — every formula that
 * needs the playtest balance pass is marked `// TUNE:`.
 */
data class BattleConfig(
    val playerName: String,
    val playerMaxHp: Int,
    val atkStat: Int,
    /** NaviCust attack% (+ style) multiplier applied to player damage. */
    val effAtkMult: Float,
    val busterElement: ChipElement?,
    /** Null = default buster animation. */
    val busterAnimKey: String?,
    /** Null = default sword animation. */
    val swordAnimKey: String?,
    /** Chip ids; shuffled into the draw deck at fight start. */
    val playerDeck: List<Int>,
    val enemyName: String,
    val enemyMaxHp: Int,
    val enemyAtk: Int,
    /** NaviCust charge% speeds up buster charging. */
    val chargeRate: Float = 1f
)

data class SimProjectile(
    var x: Float,
    val y: Int,
    val vx: Float,
    val damage: Float,
    val fromPlayer: Boolean,
    val element: ChipElement?,
    val big: Boolean,
    val piercing: Boolean,
    var hitDone: Boolean = false,
    var dead: Boolean = false
)

data class SimMine(
    val x: Int,
    val y: Int,
    var timer: Float,
    val damage: Float,
    val fromPlayer: Boolean
)

data class FiredShot(
    val damage: Float,
    val animKey: String?,
    val element: ChipElement?,
    val charged: Boolean
)

data class SwordResult(val damageDealt: Float, val animKey: String?)

private data class PendingChip(val chip: BattleChip, var delay: Float)
private data class DelayedProj(val proj: SimProjectile, var delay: Float)

data class BattleSnapshot(
    val px: Int, val py: Int,
    val ex: Int, val ey: Int,
    val playerHp: Float, val playerMaxHp: Float,
    val enemyHp: Float, val enemyMaxHp: Float,
    val projectiles: List<SimProjectile>,
    val mines: List<SimMine>,
    val gauge: Float,
    val charge: Float,
    val charging: Boolean,
    val hand: List<BattleChip>,
    val selected: Set<Int>,
    val paused: Boolean,
    val winner: Int?,
    val lastPlayerHitAt: Float,
    val lastEnemyHitAt: Float,
    val time: Float,
    val canCustom: Boolean
)

class BattleEngine(val config: BattleConfig) {

    var px = 1; private set
    var py = 1; private set
    var ex = 4; private set
    var ey = 1; private set

    var playerHp = config.playerMaxHp.toFloat(); private set
    var enemyHp = config.enemyMaxHp.toFloat(); private set

    private val projectiles = mutableListOf<SimProjectile>()
    private val mines = mutableListOf<SimMine>()
    private val pendingChips = mutableListOf<PendingChip>()
    private val delayedProjs = mutableListOf<DelayedProj>()

    private val deck: MutableList<Int> = config.playerDeck.shuffled().toMutableList()
    private val discards = mutableListOf<Int>()
    private val hand = mutableListOf<BattleChip>()
    private val selected = mutableSetOf<Int>()

    var charge = 0f; private set
    var charging = false; private set
    var gauge = 0f; private set
    var paused = false; private set
    var winner: Int? = null; private set  // 0 = player, 1 = enemy

    private var time = 0f
    private var lastPlayerHitAt = -99f
    private var lastEnemyHitAt = -99f

    private var moveCd = 0f
    private var busterCd = 0f
    private var swordCd = 0f
    private var aiMoveCd = 0.5f
    private var aiBusterCd = 1.15f
    private var aiChipCd = 6f

    // TUNE: converts the ATK stat into chip damage scale.
    private fun dmgScale(): Float = (0.5f + config.atkStat / 150f) * config.effAtkMult

    // ---------- player intents ----------

    /** TUNE: 180ms step cooldown. */
    fun movePlayer(dx: Int, dy: Int) {
        if (paused || winner != null || moveCd > 0f) return
        val nx = (px + dx).coerceIn(0, 2)
        val ny = (py + dy).coerceIn(0, 2)
        if (nx != px || ny != py) {
            px = nx; py = ny
            moveCd = 0.14f
        }
    }

    fun setCharging(c: Boolean) {
        if (paused || winner != null) return
        charging = c
    }

    /** Fires the buster; returns shot info for the UI FX, or null on cooldown. */
    fun releaseBuster(): FiredShot? {
        if (paused || winner != null || busterCd > 0f) return null
        // TUNE: buster base + charge scaling.
        val dmg = (8f + config.atkStat / 10f) * config.effAtkMult * (1f + charge * 1.6f)
        val charged = charge > 0.7f
        projectiles.add(
            SimProjectile(px + 0.6f, py, 4.2f, dmg, true, config.busterElement, big = charged, piercing = false)
        )
        busterCd = 0.2f
        charge = 0f
        charging = false
        return FiredShot(dmg, config.busterAnimKey, config.busterElement, charged)
    }

    /** Instant sword arc; returns result for the UI FX, or null on cooldown. */
    fun playerSword(): SwordResult? {
        if (paused || winner != null || swordCd > 0f) return null
        swordCd = 0.5f
        // TUNE: sword hits 1.5x an uncharged buster.
        val dmg = (8f + config.atkStat / 10f) * config.effAtkMult * 1.5f
        var dealt = 0f
        if (ex == px + 1 && abs(ey - py) <= 1) {
            damageEnemy(dmg)
            dealt = dmg
        }
        return SwordResult(dealt, config.swordAnimKey)
    }

    // ---------- chip custom ----------

    /** Opens chip select (pauses the sim) and draws up to 5 chips. */
    fun openCustom() {
        if (paused || winner != null || gauge < 1f) return
        if (deck.size < 5) {
            deck.addAll(discards.shuffled())
            discards.clear()
        }
        if (deck.isEmpty()) return
        repeat(minOf(5, deck.size)) {
            ChipLibrary.byId(deck.removeAt(0))?.let { hand.add(it) }
        }
        selected.clear()
        paused = true
    }

    fun cancelCustom() {
        if (!paused) return
        deck.addAll(0, hand.map { it.id })
        hand.clear()
        selected.clear()
        paused = false
    }

    /**
     * Code-matching: the first pick sets the code; later picks must share it
     * (or be the same chip). '*' is a wildcard on either side.
     */
    fun toggleChipSelect(index: Int) {
        if (!paused || index !in hand.indices) return
        if (index in selected) {
            selected.remove(index)
            return
        }
        val chip = hand[index]
        if (selected.isEmpty()) {
            selected.add(index)
            return
        }
        val first = hand[selected.first()]
        val reqCode: Char? = first.codes.firstOrNull { it != '*' }
        val ok = selected.any { hand[it].id == chip.id } ||
            '*' in chip.codes || '*' in first.codes ||
            reqCode == null || reqCode in chip.codes
        if (ok) selected.add(index)
    }

    fun fireSelected() {
        if (!paused || selected.isEmpty()) return
        val ordered = selected.sorted()
        hand.forEachIndexed { i, c -> if (i !in selected) deck.add(c.id) }
        discards.addAll(ordered.map { hand[it].id })
        // TUNE: 0.45s between queued chips.
        ordered.forEachIndexed { qi, hi -> pendingChips.add(PendingChip(hand[hi], qi * 0.3f)) }
        hand.clear()
        selected.clear()
        paused = false
        gauge = 0f
    }

    // ---------- simulation ----------

    fun update(dt: Float) {
        if (winner != null) return
        time += dt
        if (paused) return

        moveCd = maxOf(0f, moveCd - dt)
        busterCd = maxOf(0f, busterCd - dt)
        swordCd = maxOf(0f, swordCd - dt)

        // TUNE: buster reaches full charge in 1.2s (faster with chargeRate).
        if (charging && busterCd <= 0f) charge = min(1f, charge + dt / 0.9f * config.chargeRate)
        // TUNE: chip gauge fills in ~5s.
        if (hand.isEmpty()) gauge = min(1f, gauge + dt / 5f)

        val pci = pendingChips.iterator()
        while (pci.hasNext()) {
            val p = pci.next()
            p.delay -= dt
            if (p.delay <= 0f) {
                applyChip(p.chip)
                pci.remove()
            }
        }

        val dpi = delayedProjs.iterator()
        while (dpi.hasNext()) {
            val d = dpi.next()
            d.delay -= dt
            if (d.delay <= 0f) {
                projectiles.add(d.proj)
                dpi.remove()
            }
        }

        for (pr in projectiles) pr.x += pr.vx * dt

        for (pr in projectiles) {
            if (pr.dead || pr.hitDone) continue
            if (pr.fromPlayer) {
                // TUNE: hit window vs the enemy.
                if (abs(pr.x - ex) < 0.45f && pr.y == ey) {
                    damageEnemy(pr.damage)
                    pr.hitDone = true
                    if (!pr.piercing) pr.dead = true
                }
            } else {
                if (abs(pr.x - px) < 0.45f && pr.y == py) {
                    damagePlayer(pr.damage)
                    pr.hitDone = true
                    pr.dead = true
                }
            }
        }
        projectiles.removeAll { it.dead || it.x < -0.6f || it.x > 6.6f }

        val mi = mines.iterator()
        while (mi.hasNext()) {
            val m = mi.next()
            m.timer -= dt
            if (m.timer <= 0f) {
                // TUNE: mines hit their whole column on detonation.
                if (m.fromPlayer) {
                    if (ex == m.x) damageEnemy(m.damage)
                } else {
                    if (px == m.x) damagePlayer(m.damage)
                }
                mi.remove()
            }
        }

        enemyAi(dt)
    }

    private fun applyChip(chip: BattleChip) {
        val s = dmgScale()
        when (chip.effectKind) {
            EffectKind.PROJECTILE -> {
                // TUNE: multi-hit chips stagger their shots 0.12s apart.
                repeat(chip.hits.coerceAtLeast(1)) { i ->
                    delayedProjs.add(
                        DelayedProj(
                            SimProjectile(px + 0.6f, py, 3.6f, chip.damage * s, true, chip.element, big = chip.tier != ChipTier.STANDARD, piercing = false),
                            i * 0.12f
                        )
                    )
                }
            }
            EffectKind.SWORD, EffectKind.MELEE -> {
                if (ex == px + 1 && abs(ey - py) <= 1) damageEnemy(chip.damage * chip.hits * s)
            }
            EffectKind.LOB -> {
                // TUNE: lobbed shots are slow but hit 1.2x.
                delayedProjs.add(
                    DelayedProj(SimProjectile(px + 0.6f, py, 2.1f, chip.damage * 1.2f * s, true, chip.element, big = true, piercing = false), 0f)
                )
            }
            EffectKind.BEAM -> {
                // TUNE: beams pierce and hit 1.5x.
                delayedProjs.add(
                    DelayedProj(SimProjectile(px + 0.6f, py, 5.5f, chip.damage * 1.5f * s, true, chip.element, big = true, piercing = true), 0f)
                )
            }
            EffectKind.SUMMON -> {
                // TUNE: summons spray 3 rows at 0.6x each.
                listOf(py - 1, py, py + 1).map { it.coerceIn(0, 2) }.distinct().forEachIndexed { i, row ->
                    delayedProjs.add(
                        DelayedProj(SimProjectile(px + 0.6f, row, 3.1f, chip.damage * 0.6f * s, true, chip.element, big = false, piercing = false), i * 0.15f)
                    )
                }
            }
            EffectKind.TRAP -> {
                mines.add(SimMine(ex, ey, 1.2f, chip.damage * s, fromPlayer = true))
            }
            EffectKind.SUPPORT -> {
                // TUNE: support chips carry damage=0 in the library, so heal from stats.
                playerHp = min(config.playerMaxHp.toFloat(), playerHp + 80f + config.atkStat * 0.3f)
            }
        }
    }

    private fun enemyAi(dt: Float) {
        aiMoveCd -= dt
        if (aiMoveCd <= 0f) {
            // TUNE: enemy decision rate.
            aiMoveCd = 0.5f
            val threat = projectiles.any { it.fromPlayer && !it.dead && it.vx > 0 && it.y == ey && it.x < ex && ex - it.x < 2.5f }
            // TUNE: dodge chance vs incoming shots.
            if (threat && Random.nextFloat() < 0.75f) {
                ey = if (ey >= 2) ey - 1 else if (ey <= 0) ey + 1 else ey + (if (Random.nextBoolean()) 1 else -1)
                ey = ey.coerceIn(0, 2)
            } else if (Random.nextFloat() < 0.4f) {
                ex = (ex + Random.nextInt(-1, 2)).coerceIn(3, 5)
            }
        }
        aiBusterCd -= dt
        if (aiBusterCd <= 0f) {
            // TUNE: enemy fire rate.
            aiBusterCd = 1.15f
            if (abs(ey - py) <= 1) {
                // TUNE: enemy buster damage.
                projectiles.add(SimProjectile(ex - 0.6f, ey, -3.1f, 6f + config.enemyAtk / 12f, false, null, big = false, piercing = false))
            }
        }
        aiChipCd -= dt
        if (aiChipCd <= 0f) {
            // TUNE: enemy chip rate + simple chip kit.
            aiChipCd = 6f
            when (Random.nextInt(3)) {
                0 -> projectiles.add(
                    SimProjectile(ex - 0.6f, ey, -2.6f, 40f * (0.5f + config.enemyAtk / 150f), false, ChipElement.ELEC, big = true, piercing = false)
                )
                1 -> if (abs(ex - px) <= 2 && abs(ey - py) <= 1) damagePlayer(50f)
                else -> enemyHp = min(config.enemyMaxHp.toFloat(), enemyHp + 70f)
            }
        }
    }

    private fun damagePlayer(d: Float) {
        if (winner != null) return
        playerHp -= d
        lastPlayerHitAt = time
        if (playerHp <= 0f) {
            playerHp = 0f
            winner = 1
        }
    }

    private fun damageEnemy(d: Float) {
        if (winner != null) return
        enemyHp -= d
        lastEnemyHitAt = time
        if (enemyHp <= 0f) {
            enemyHp = 0f
            winner = 0
        }
    }

    fun snapshot(): BattleSnapshot = BattleSnapshot(
        px = px, py = py, ex = ex, ey = ey,
        playerHp = playerHp, playerMaxHp = config.playerMaxHp.toFloat(),
        enemyHp = enemyHp, enemyMaxHp = config.enemyMaxHp.toFloat(),
        projectiles = projectiles.toList(),
        mines = mines.toList(),
        gauge = gauge, charge = charge, charging = charging,
        hand = hand.toList(), selected = selected.toSet(),
        paused = paused, winner = winner,
        lastPlayerHitAt = lastPlayerHitAt, lastEnemyHitAt = lastEnemyHitAt,
        time = time, canCustom = gauge >= 1f && hand.isEmpty() && winner == null
    )
}
