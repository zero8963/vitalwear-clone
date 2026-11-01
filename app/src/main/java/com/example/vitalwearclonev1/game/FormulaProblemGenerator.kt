package com.example.vitalwearclonev1.game

import kotlin.random.Random

/**
 * Formula battle problems (2026-11-01). Pure Kotlin, no Android types.
 *
 * Three prompt forms rotate per enemy turn:
 * - NAME: pick the formula in symbols from multiple choice.
 * - FILL: fill in the missing piece of a formula.
 * - APPLY: plug procedurally generated numbers into the formula and
 *   compute. Distractors target the classic confusions: area vs
 *   perimeter, diameter vs radius, squared vs doubled, s² vs s³.
 *
 * Paced Learning on means smaller numbers and deck order; off means
 * shuffled cards and larger numbers. Pi is 3.14 throughout, matching
 * the deck's worked examples.
 */
data class FormulaProblem(
    val card: FormulaCard,
    val form: FormulaForm,
    val problem: Problem
)

fun formatFormulaNumber(value: Double): String {
    if (value == value.toInt().toDouble()) return value.toInt().toString()
    return "%.2f".format(value).trimEnd('0').trimEnd('.')
}

class FormulaProblemGenerator(
    private val isAdjusted: Boolean,
    private val random: Random = Random(System.currentTimeMillis())
) {
    private var deckIndex = 0
    private var formIndex = 0
    private var lastCard: FormulaCard? = null

    private data class QueuedMiss(val card: FormulaCard, var wait: Int)
    private val missed = mutableListOf<QueuedMiss>()

    /** Next problem for a formula battle, drawing from [cards] only (one unit's deck). */
    fun nextProblem(cards: List<FormulaCard>): FormulaProblem {
        require(cards.isNotEmpty()) { "Formula battle needs a non-empty unit deck" }
        // A missed card returns only after at least 2 other questions:
        // it is queued with wait=2, decremented on each normal pick, and
        // returned as APPLY once its wait reaches 0.
        val due = missed.firstOrNull()?.takeIf { it.wait <= 0 }
        if (due != null) {
            missed.remove(due)
            return makeProblem(due.card, FormulaForm.APPLY)
        }
        for (queued in missed) queued.wait--
        val card = if (isAdjusted) {
            cards[deckIndex % cards.size].also { deckIndex++ }
        } else {
            val others = cards.filter { it.id != lastCard?.id }
            (if (others.isNotEmpty()) others.random(random) else cards.random(random))
        }
        lastCard = card
        val form = FormulaForm.values()[formIndex % FormulaForm.values().size].also { formIndex++ }
        return makeProblem(card, form)
    }

    /** Feed a battle answer back so missed cards are re-queued (and cleared on a later correct answer). */
    fun recordResult(card: FormulaCard, correct: Boolean) {
        if (!correct) {
            if (missed.none { it.card.id == card.id }) missed.add(QueuedMiss(card, 2))
        } else {
            missed.removeAll { it.card.id == card.id }
        }
    }

    fun generateFormulaProblem(card: FormulaCard, form: FormulaForm): Problem =
        makeProblem(card, form).problem

    private fun makeProblem(card: FormulaCard, form: FormulaForm): FormulaProblem {
        return when (form) {
            FormulaForm.NAME -> FormulaProblem(
                card, form,
                Problem(
                    question = "What is the formula for ${card.name}?",
                    correctAnswer = card.formulaSymbols,
                    options = (listOf(card.formulaSymbols) + card.nameDistractors).shuffled(random)
                )
            )
            FormulaForm.FILL -> FormulaProblem(
                card, form,
                Problem(
                    question = "Fill in the missing part: ${card.fillPrompt}",
                    correctAnswer = card.fillAnswer,
                    options = (listOf(card.fillAnswer) + card.fillDistractors).shuffled(random)
                )
            )
            FormulaForm.APPLY -> {
                val (question, answer, distractors) = applyCase(card)
                FormulaProblem(
                    card, form,
                    Problem(
                        question = question,
                        correctAnswer = answer,
                        options = distinctOptions(answer, distractors)
                    )
                )
            }
        }
    }

    private fun distinctOptions(answer: String, candidates: List<String>): List<String> {
        val options = mutableListOf(answer)
        for (candidate in candidates) {
            if (candidate !in options) options.add(candidate)
        }
        var delta = 1
        val base = answer.toDoubleOrNull()
        while (options.size < 4) {
            val filler = if (base != null) formatFormulaNumber(base + delta) else "$answer?"
            if (filler !in options) options.add(filler)
            delta++
        }
        return options.take(4).shuffled(random)
    }

    private fun intRange(paced: IntRange, normal: IntRange): Int =
        if (isAdjusted) paced.random(random) else normal.random(random)

    /** Returns (question, correctAnswer, distractors) for an APPLY prompt. Numbers in the question are in the order the test recomputes them. */
    private fun applyCase(card: FormulaCard): Triple<String, String, List<String>> {
        return when (card.id) {
            "rect_area" -> {
                var l = intRange(2..6, 3..15)
                var w = intRange(2..5, 3..12)
                if (l == w) w += 1
                Triple(
                    "A rectangle is $l long and $w wide. What is its area?",
                    (l * w).toString(),
                    listOf((2 * (l + w)).toString(), (l + w).toString(), (l * 2).toString())
                )
            }
            "rect_perimeter" -> {
                var l = intRange(2..6, 3..15)
                var w = intRange(2..5, 3..12)
                if (l == w) w += 1
                Triple(
                    "A rectangle is $l long and $w wide. What is its perimeter?",
                    (2 * (l + w)).toString(),
                    listOf((l * w).toString(), (l + w).toString(), (2 * l + w).toString())
                )
            }
            "triangle_area", "gentle_triangle_area" -> {
                val b = if (isAdjusted) listOf(2, 4, 6, 8).random(random) else listOf(4, 6, 8, 10, 12, 14).random(random)
                val h = intRange(2..6, 3..12)
                Triple(
                    "A triangle has a base of $b and a height of $h. What is its area?",
                    ((b * h) / 2).toString(),
                    listOf((b * h).toString(), (b + h).toString(), (2 * (b + h)).toString())
                )
            }
            "circle_area" -> {
                val r = intRange(2..5, 3..12)
                Triple(
                    "A circle has a radius of $r. What is its area? Use pi = 3.14.",
                    formatFormulaNumber(3.14 * r * r),
                    listOf(
                        formatFormulaNumber(2 * 3.14 * r),
                        formatFormulaNumber(3.14 * r),
                        (2 * r).toString()
                    )
                )
            }
            "circle_circumference" -> {
                val r = intRange(2..5, 3..12)
                Triple(
                    "A circle has a radius of $r. What is its circumference? Use pi = 3.14.",
                    formatFormulaNumber(2 * 3.14 * r),
                    listOf(
                        formatFormulaNumber(3.14 * r * r),
                        formatFormulaNumber(3.14 * r),
                        (2 * r).toString()
                    )
                )
            }
            "cube_volume" -> {
                val s = intRange(2..4, 2..9)
                Triple(
                    "A cube has a side of $s. What is its volume?",
                    (s * s * s).toString(),
                    listOf((s * s).toString(), (6 * s * s).toString(), (6 * s).toString())
                )
            }
            "cube_surface_area" -> {
                val s = intRange(2..4, 2..9)
                Triple(
                    "A cube has a side of $s. What is its surface area?",
                    (6 * s * s).toString(),
                    listOf((s * s * s).toString(), (s * s).toString(), (6 * s).toString())
                )
            }
            "pythagorean" -> {
                val (a, b, c) = if (isAdjusted) {
                    Triple(3, 4, 5)
                } else {
                    listOf(Triple(3, 4, 5), Triple(6, 8, 10), Triple(5, 12, 13), Triple(9, 12, 15)).random(random)
                }
                Triple(
                    "A right triangle has legs $a and $b. What is the longest side?",
                    c.toString(),
                    listOf((a + b).toString(), (c - 1).toString(), (c + 1).toString())
                )
            }
            "gentle_perimeter" -> {
                var a = intRange(2..6, 2..9)
                var b = intRange(2..6, 2..9)
                if (a == b) b += 1
                Triple(
                    "A shape has sides $a, $b, $a, and $b. What is its perimeter?",
                    (2 * (a + b)).toString(),
                    listOf((a * b).toString(), (a + b).toString(), (2 * a).toString())
                )
            }
            "gentle_area_rows" -> {
                var rows = intRange(2..5, 2..9)
                var cols = intRange(2..5, 2..9)
                if (rows == cols) cols += 1
                Triple(
                    "A grid has $rows rows with $cols tiles in each row. How many tiles in all?",
                    (rows * cols).toString(),
                    listOf((rows + cols).toString(), (2 * rows).toString(), (rows * cols * 2).toString())
                )
            }
            "gentle_rectangle" -> {
                var l = intRange(2..5, 3..9)
                var w = intRange(2..4, 2..8)
                if (l == w) w += 1
                Triple(
                    "A rectangle is $l long and $w wide. What is its area?",
                    (l * w).toString(),
                    listOf((2 * (l + w)).toString(), (l + w).toString(), (l * 2).toString())
                )
            }
            else -> {
                // Fallback for any future card without an APPLY case: never crash a battle.
                Triple(
                    "What is the formula for ${card.name}?",
                    card.formulaSymbols,
                    card.nameDistractors
                )
            }
        }
    }
}
