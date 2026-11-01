package com.example.vitalwearclonev1.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * JVM unit tests for Spelling Cards (2026-11-01). Pure logic only:
 * no Android types are touched, so these run on the host JVM.
 */
class SpellingCardTest {

    @Test
    fun everyCardHasDefinitionExampleBreakdownTrickAndDistractors() {
        assertEquals(9, ALL_SPELLING_CARDS.size)
        assertEquals(9, ALL_SPELLING_CARDS.map { it.id }.toSet().size)
        assertEquals(9, ALL_SPELLING_CARDS.map { it.word }.toSet().size)
        for (card in ALL_SPELLING_CARDS) {
            assertTrue("blank definition: ${card.id}", card.definition.isNotBlank())
            assertTrue("blank example: ${card.id}", card.exampleSentence.isNotBlank())
            assertTrue("example must contain the word: ${card.id}",
                card.exampleSentence.contains(card.word, ignoreCase = true))
            assertTrue("blank breakdown: ${card.id}", card.breakdown.isNotBlank())
            assertTrue("blank trick: ${card.id}", card.memoryTrick.isNotBlank())
            assertEquals("PICK needs 3 distractors: ${card.id}", 3, card.pickDistractors.size)
            assertTrue("blank fill: ${card.id}", card.fillPrompt.isNotBlank() && card.fillAnswer.isNotBlank())
            assertTrue("fill prompt must contain _: ${card.id}", "_" in card.fillPrompt)
            // The fill answer must be a chunk of the word: putting it back
            // into the prompt (replacing the first __ run) rebuilds the word.
            val rebuilt = card.fillPrompt.replaceFirst(Regex("_+"), card.fillAnswer)
            assertEquals("fill answer must rebuild the word: ${card.id}", card.word, rebuilt)
            assertEquals("FILL needs 3 distractors: ${card.id}", 3, card.fillDistractors.size)
        }
    }

    @Test
    fun unitsCoverEveryCardExactlyOnce() {
        assertEquals(2, SPELLING_UNITS.size)
        val covered = SPELLING_UNITS.flatMap { it.cardIds }
        assertEquals(9, covered.size)
        assertEquals(ALL_SPELLING_CARDS.map { it.id }.toSet(), covered.toSet())
        for (unit in SPELLING_UNITS) {
            assertTrue("unit size 4-6: ${unit.id}", unit.cardIds.size in 4..6)
            assertEquals(unit.cardIds.size, unit.cards().size)
        }
    }

    @Test
    fun battleProblemsAreWellFormed() {
        val gen = SpellingProblemGenerator(isAdjusted = false, random = Random(42))
        for (card in ALL_SPELLING_CARDS) {
            for (form in listOf(SpellingForm.PICK, SpellingForm.FILL, SpellingForm.USE)) {
                repeat(10) {
                    val problem = gen.generateSpellingProblem(card, form)
                    assertEquals("options must be 4: ${card.id} $form", 4, problem.options.size)
                    assertEquals("options distinct: ${card.id} $form", 4, problem.options.toSet().size)
                    assertTrue("answer must be an option: ${card.id} $form", problem.correctAnswer in problem.options)
                }
            }
            // USE blanks the word out of its own sentence (no leak:
            // the blank is there and the word itself is gone).
            val use = gen.generateSpellingProblem(card, SpellingForm.USE)
            assertTrue("USE must blank the word: ${card.id}", "_____" in use.question)
            assertTrue("USE must not leak the word: ${card.id}", card.word !in use.question)
        }
    }

    @Test
    fun masteryTransitionsAndRequeue() {
        val state = SpellingMasteryState()
        val card = ALL_SPELLING_CARDS.first()
        // Two consecutive correct SPELLs raise a level; a miss drops it.
        state.record(card.id, SpellingForm.SPELL, true)
        assertEquals(0, state.level(card.id))
        state.record(card.id, SpellingForm.SPELL, true)
        assertEquals(1, state.level(card.id))
        // Other correct forms only refresh last-seen, never level.
        state.record(card.id, SpellingForm.PICK, true)
        assertEquals(1, state.level(card.id))
        state.record(card.id, SpellingForm.USE, true)
        assertEquals(1, state.level(card.id))
        state.record(card.id, SpellingForm.FILL, false)
        assertEquals(0, state.level(card.id))
        // Level caps at 3.
        repeat(6) { state.record(card.id, SpellingForm.SPELL, true) }
        assertEquals(3, state.level(card.id))

        // Missed card re-queues after at least 2 other questions, as PICK, never sooner.
        val gen = SpellingProblemGenerator(isAdjusted = true, random = Random(7))
        val unit = SPELLING_UNITS.first()
        val cards = unit.cards()
        val first = gen.nextProblem(cards)
        gen.recordResult(first.card, false)
        val second = gen.nextProblem(cards)
        assertTrue("miss must not return immediately", second.card.id != first.card.id)
        val third = gen.nextProblem(cards)
        assertTrue("miss must not return after 1 other question", third.card.id != first.card.id)
        val fourth = gen.nextProblem(cards)
        assertEquals("miss returns 4th, as PICK", first.card.id, fourth.card.id)
        assertEquals(SpellingForm.PICK, fourth.form)
    }

    @Test
    fun unlockLogic() {
        val state = SpellingMasteryState()
        // Gentle-only entry: only the gentle unit open at first.
        assertEquals(
            listOf("gentle_tricky_words"),
            computeUnlockedSpellingUnits(SpellingBand.GENTLE_3_5, state.masteredUnitIds(SPELLING_UNITS)).map { it.id }
        )
        // Upper entry opens gentle + first upper.
        assertEquals(
            listOf("gentle_tricky_words", "upper_homophones_confusables"),
            computeUnlockedSpellingUnits(SpellingBand.UPPER_6_PLUS, state.masteredUnitIds(SPELLING_UNITS)).map { it.id }
        )
        // Mastering gentle unlocks upper for a gentle entrant too.
        for (cardId in SPELLING_UNITS.first().cardIds) {
            repeat(4) { state.record(cardId, SpellingForm.SPELL, true) }
        }
        assertTrue(state.isUnitMastered(SPELLING_UNITS.first()))
        assertEquals(
            listOf("gentle_tricky_words", "upper_homophones_confusables"),
            computeUnlockedSpellingUnits(SpellingBand.GENTLE_3_5, state.masteredUnitIds(SPELLING_UNITS)).map { it.id }
        )
    }
}
