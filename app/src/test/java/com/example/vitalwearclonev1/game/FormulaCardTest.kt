package com.example.vitalwearclonev1.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * JVM unit tests for Formula Cards (2026-11-01). Pure logic only:
 * no Android types are touched, so these run on the host JVM.
 */
class FormulaCardTest {

    @Test
    fun everyCardHasFormulaExampleAndTrick() {
        assertEquals(12, ALL_FORMULA_CARDS.size)
        assertEquals(12, ALL_FORMULA_CARDS.map { it.id }.toSet().size)
        for (card in ALL_FORMULA_CARDS) {
            assertTrue("blank formula: ${card.id}", card.formulaSymbols.isNotBlank())
            assertTrue("blank words: ${card.id}", card.formulaWords.isNotBlank())
            assertTrue("blank example: ${card.id}", card.workedExample.isNotBlank())
            assertTrue("blank trick: ${card.id}", card.memoryTrick.isNotBlank())
            assertEquals("NAME needs 3 distractors: ${card.id}", 3, card.nameDistractors.size)
            assertTrue("blank fill: ${card.id}", card.fillPrompt.isNotBlank() && card.fillAnswer.isNotBlank())
            assertEquals("FILL needs 3 distractors: ${card.id}", 3, card.fillDistractors.size)
        }
    }

    @Test
    fun unitsCoverEveryCardExactlyOnce() {
        assertEquals(3, FORMULA_UNITS.size)
        val covered = FORMULA_UNITS.flatMap { it.cardIds }
        assertEquals(12, covered.size)
        assertEquals(ALL_FORMULA_CARDS.map { it.id }.toSet(), covered.toSet())
        for (unit in FORMULA_UNITS) {
            assertTrue("unit size 4-6: ${unit.id}", unit.cardIds.size in 4..6)
            assertEquals(unit.cardIds.size, unit.cards().size)
        }
    }

    @Test
    fun applyAnswersMatchTheirFormulas() {
        val gen = FormulaProblemGenerator(isAdjusted = false, random = Random(42))
        for (card in ALL_FORMULA_CARDS) {
            repeat(30) {
                val problem = gen.generateFormulaProblem(card, FormulaForm.APPLY)
                assertEquals("options must be 4: ${card.id}", 4, problem.options.size)
                assertEquals("options distinct: ${card.id}", 4, problem.options.toSet().size)
                assertTrue("answer must be an option: ${card.id}", problem.correctAnswer in problem.options)
                assertEquals("recomputed answer: ${card.id} :: ${problem.question}", recomputeApply(card, problem.question), problem.correctAnswer)
            }
        }
    }

    /** Recompute an APPLY answer from the numbers printed in the question. */
    private fun recomputeApply(card: FormulaCard, question: String): String {
        val nums = Regex("\\d+(\\.\\d+)?").findAll(question).map { it.value.toDouble() }.toList()
        return when (card.id) {
            "rect_area", "gentle_rectangle" -> formatFormulaNumber(nums[0] * nums[1])
            "rect_perimeter" -> formatFormulaNumber(2 * (nums[0] + nums[1]))
            "triangle_area", "gentle_triangle_area" -> formatFormulaNumber(0.5 * nums[0] * nums[1])
            "circle_area" -> formatFormulaNumber(3.14 * nums[0] * nums[0])
            "circle_circumference" -> formatFormulaNumber(2 * 3.14 * nums[0])
            "cube_volume" -> formatFormulaNumber(nums[0] * nums[0] * nums[0])
            "cube_surface_area" -> formatFormulaNumber(6 * nums[0] * nums[0])
            "pythagorean" -> {
                // Legs are the first two numbers; the answer is the integer hypotenuse.
                val a = nums[0]; val b = nums[1]
                formatFormulaNumber(kotlin.math.sqrt(a * a + b * b))
            }
            "gentle_perimeter" -> formatFormulaNumber(2 * (nums[0] + nums[1]))
            "gentle_area_rows" -> formatFormulaNumber(nums[0] * nums[1])
            else -> throw AssertionError("no recompute for ${card.id}")
        }
    }

    @Test
    fun masteryLevelsAndMissedRequeue() {
        val state = FormulaMasteryState()
        val card = "rect_area"
        // One correct APPLY is not enough to level up.
        state.record(card, FormulaForm.APPLY, true, 1000L)
        assertEquals(0, state.level(card))
        // Two consecutive correct APPLYs raise a level.
        state.record(card, FormulaForm.APPLY, true, 2000L)
        assertEquals(1, state.level(card))
        // A NAME correct does not level.
        state.record(card, FormulaForm.NAME, true, 3000L)
        assertEquals(1, state.level(card))
        // A miss lowers a level.
        state.record(card, FormulaForm.APPLY, false, 4000L)
        assertEquals(0, state.level(card))
        // Level caps at 3.
        repeat(6) { state.record(card, FormulaForm.APPLY, true, 5000L + it) }
        assertEquals(3, state.level(card))

        // Missed-card requeue: after a miss, the card returns as APPLY
        // only after at least 2 other questions.
        val gen = FormulaProblemGenerator(isAdjusted = true, random = Random(7))
        val deck = listOf(
            formulaCardById("rect_area")!!,
            formulaCardById("rect_perimeter")!!,
            formulaCardById("triangle_area")!!,
            formulaCardById("circle_area")!!
        )
        val first = gen.nextProblem(deck)
        gen.recordResult(first.card, false)
        val second = gen.nextProblem(deck)
        val third = gen.nextProblem(deck)
        assertTrue("second question must not be the missed card", second.card.id != first.card.id)
        assertTrue("third question must not be the missed card", third.card.id != first.card.id)
        val fourth = gen.nextProblem(deck)
        assertEquals("missed card returns fourth, as APPLY", first.card.id, fourth.card.id)
        assertEquals(FormulaForm.APPLY, fourth.form)
    }

    @Test
    fun unlockLogicIsGlobalNotPerGrade() {
        // No mastery, gentle entry: only the gentle unit is open.
        assertEquals(
            listOf("gentle_perimeter_area"),
            computeUnlockedUnits(FormulaBand.GENTLE_3_5, emptySet()).map { it.id }
        )
        // Gentle mastered: first upper unit opens even for a gentle entry.
        assertEquals(
            listOf("gentle_perimeter_area", "upper_area_perimeter"),
            computeUnlockedUnits(FormulaBand.GENTLE_3_5, setOf("gentle_perimeter_area")).map { it.id }
        )
        // Upper entry: first upper unit is open from the start.
        assertEquals(
            listOf("gentle_perimeter_area", "upper_area_perimeter"),
            computeUnlockedUnits(FormulaBand.UPPER_6_PLUS, emptySet()).map { it.id }
        )
        // All units mastered: everything open.
        assertEquals(
            3,
            computeUnlockedUnits(
                FormulaBand.GENTLE_3_5,
                setOf("gentle_perimeter_area", "upper_area_perimeter", "upper_circles_cubes_triangles")
            ).size
        )
        // Grade bands: 4th grade starts gentle, 8th and 10th start upper.
        assertEquals(FormulaBand.GENTLE_3_5, formulaBandForGrade("4"))
        assertEquals(FormulaBand.UPPER_6_PLUS, formulaBandForGrade("8"))
        assertEquals(FormulaBand.UPPER_6_PLUS, formulaBandForGrade("10"))
    }
}
