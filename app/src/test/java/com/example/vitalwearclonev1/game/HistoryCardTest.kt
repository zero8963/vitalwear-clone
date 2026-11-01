package com.example.vitalwearclonev1.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * JVM unit tests for History Cards (2026-11-01). Pure logic only:
 * no Android types are touched, so these run on the host JVM.
 */
class HistoryCardTest {

    @Test
    fun everyCardHasDateSortKeyWhatHappenedWhyHookAndDistractors() {
        assertEquals(8, ALL_HISTORY_CARDS.size)
        assertEquals(8, ALL_HISTORY_CARDS.map { it.id }.toSet().size)
        assertEquals(8, ALL_HISTORY_CARDS.map { it.eventName }.toSet().size)
        for (card in ALL_HISTORY_CARDS) {
            assertTrue("blank date label: ${card.id}", card.dateLabel.isNotBlank())
            assertTrue("blank description: ${card.id}", card.description.isNotBlank())
            assertTrue("description must not leak the event name: ${card.id}",
                !card.description.contains(card.eventName, ignoreCase = true))
            assertTrue("blank why-it-mattered: ${card.id}", card.whyItMattered.isNotBlank())
            assertTrue("blank hook: ${card.id}", card.memoryHook.isNotBlank())
            assertTrue("blank cause: ${card.id}", card.cause.isNotBlank())
            assertTrue("blank consequence: ${card.id}", card.consequence.isNotBlank())
            assertEquals("DATE needs 3 distractors: ${card.id}", 3, card.dateDistractors.size)
            assertTrue("date distractors must not contain the answer: ${card.id}",
                card.dateLabel !in card.dateDistractors)
            assertEquals("DATE distractors distinct: ${card.id}", 3, card.dateDistractors.toSet().size)
        }
    }

    @Test
    fun unitsCoverEveryCardExactlyOnceAndAreChronological() {
        assertEquals(2, HISTORY_UNITS.size)
        val covered = HISTORY_UNITS.flatMap { it.cardIds }
        assertEquals(8, covered.size)
        assertEquals(ALL_HISTORY_CARDS.map { it.id }.toSet(), covered.toSet())
        for (unit in HISTORY_UNITS) {
            assertTrue("unit size 4-6: ${unit.id}", unit.cardIds.size in 4..6)
            assertEquals(unit.cardIds.size, unit.cards().size)
            val keys = unit.cards().map { it.dateSortKey }
            assertEquals("sort keys strictly increasing in ${unit.id}", keys.sorted(), keys)
            assertEquals("sort keys distinct in ${unit.id}", keys.size, keys.toSet().size)
        }
    }

    @Test
    fun battleProblemsAreWellFormed() {
        val gen = HistoryCardProblemGenerator(isAdjusted = false, random = Random(42))
        for (unit in HISTORY_UNITS) {
            val cards = unit.cards()
            for (card in cards) {
                for (form in listOf(HistoryForm.NAME, HistoryForm.DATE, HistoryForm.CAUSE)) {
                    repeat(10) {
                        val problem = gen.generateHistoryProblem(card, form, cards)
                        assertEquals("options must be 4: ${card.id} $form", 4, problem.options.size)
                        assertEquals("options distinct: ${card.id} $form", 4, problem.options.toSet().size)
                        assertTrue("answer must be an option: ${card.id} $form", problem.correctAnswer in problem.options)
                    }
                }
                // DATE answer is the card's own date label.
                val date = gen.generateHistoryProblem(card, HistoryForm.DATE, cards)
                assertEquals("DATE answer: ${card.id}", card.dateLabel, date.correctAnswer)
                // CAUSE answers are the card's own consequence or cause.
                val cause = gen.generateHistoryProblem(card, HistoryForm.CAUSE, cards)
                assertTrue("CAUSE answer: ${card.id}",
                    cause.correctAnswer == card.consequence || cause.correctAnswer == card.cause)
            }
            // ORDER first/last answers are earliest/latest by sort key.
            val first = gen.generateHistoryProblem(cards[1], HistoryForm.ORDER, cards)
            // ORDER rotates first/last/before-after, so draw until a first/last question appears.
            var seenFirst = false
            var seenLast = false
            repeat(12) {
                val p = gen.generateHistoryProblem(cards[1], HistoryForm.ORDER, cards)
                if (p.question == "Which of these happened FIRST?") {
                    seenFirst = true
                    assertEquals("FIRST answer", cards.minByOrNull { it.dateSortKey }!!.eventName, p.correctAnswer)
                }
                if (p.question == "Which of these happened LAST?") {
                    seenLast = true
                    assertEquals("LAST answer", cards.maxByOrNull { it.dateSortKey }!!.eventName, p.correctAnswer)
                }
                if (p.question.startsWith("Did ")) {
                    assertEquals("before/after has 2 options", 2, p.options.size)
                    assertTrue("before/after answer", p.correctAnswer == "Before" || p.correctAnswer == "After")
                }
            }
            assertTrue("ORDER must produce a FIRST question", seenFirst)
            assertTrue("ORDER must produce a LAST question", seenLast)
            assertTrue("first ORDER draw is a valid ORDER question",
                first.question == "Which of these happened FIRST?" ||
                    first.question == "Which of these happened LAST?" ||
                    first.question.startsWith("Did "))
        }
    }

    @Test
    fun masteryTransitionsAndUnlocks() {
        val state = HistoryMasteryState()
        val unit = HISTORY_UNITS[0]
        val card = unit.cards()[0]
        // NAME/DATE correct only refresh: no level gain.
        state.record(card.id, HistoryForm.NAME, true)
        state.record(card.id, HistoryForm.DATE, true)
        assertEquals(0, state.level(card.id))
        // Two consecutive ORDER/CAUSE corrects raise a level; mixed streak counts too.
        state.record(card.id, HistoryForm.ORDER, true)
        assertEquals(0, state.level(card.id))
        state.record(card.id, HistoryForm.CAUSE, true)
        assertEquals(1, state.level(card.id))
        // A miss drops a level.
        state.record(card.id, HistoryForm.ORDER, false)
        assertEquals(0, state.level(card.id))
        // Cap at 3.
        repeat(8) {
            state.record(card.id, HistoryForm.CAUSE, true)
            state.record(card.id, HistoryForm.ORDER, true)
        }
        assertEquals(3, state.level(card.id))
        // Unit mastered only when every card is >= 2.
        assertTrue(!state.isUnitMastered(unit))
        for (c in unit.cards()) {
            repeat(4) { state.record(c.id, HistoryForm.ORDER, true) }
        }
        assertTrue(state.isUnitMastered(unit))
        // Unlocks: gentle-only at first; upper opens on gentle mastery or upper entry.
        assertEquals(listOf(unit), computeUnlockedHistoryUnits(HistoryBand.GENTLE_3_5, emptySet()))
        assertEquals(HISTORY_UNITS, computeUnlockedHistoryUnits(HistoryBand.GENTLE_3_5, setOf(unit.id)))
        assertEquals(HISTORY_UNITS, computeUnlockedHistoryUnits(HistoryBand.UPPER_6_PLUS, emptySet()))
    }

    @Test
    fun missedCardsRequeueAsOrderAfterTwoOthers() {
        val gen = HistoryCardProblemGenerator(isAdjusted = false, random = Random(7))
        val cards = HISTORY_UNITS[0].cards()
        val missed = cards[0]
        gen.recordResult(missed, false)
        // Next two problems are normal picks (not the missed card as ORDER yet).
        val first = gen.nextProblem(cards)
        val second = gen.nextProblem(cards)
        assertTrue("requeue must wait 2", !(first.form == HistoryForm.ORDER && first.card.id == missed.id))
        assertTrue("requeue must wait 2", !(second.form == HistoryForm.ORDER && second.card.id == missed.id))
        // Third problem is the missed card, returning as ORDER.
        val third = gen.nextProblem(cards)
        assertEquals("missed card returns 3rd, as ORDER", missed.id, third.card.id)
        assertEquals(HistoryForm.ORDER, third.form)
    }
}
