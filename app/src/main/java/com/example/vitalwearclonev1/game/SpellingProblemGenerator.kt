package com.example.vitalwearclonev1.game

import kotlin.random.Random

/**
 * Spelling battle problems (2026-11-01). Pure Kotlin, no Android types.
 *
 * Battles stay options-only (GamePlayScreen answers are options);
 * typed spelling lives in lessons. Three prompt forms rotate per
 * enemy turn:
 * - PICK: "Which spelling is correct?" — the word vs. its own
 *   misspellings, plus its homophone for confusable cards.
 * - FILL: the word with a missing chunk — pick the missing letters.
 * - USE: the example sentence with the word blanked — pick the
 *   word that fits the meaning (this is where homophones and
 *   confusables are tested for meaning, not just letter order).
 *
 * Paced Learning on means deck order; off means shuffled cards.
 * No audio is assumed: every prompt is definition/sentence text.
 */
data class SpellingProblem(
    val card: SpellingCard,
    val form: SpellingForm,
    val problem: Problem
)

class SpellingProblemGenerator(
    private val isAdjusted: Boolean,
    private val random: Random = Random(System.currentTimeMillis())
) {
    private var deckIndex = 0
    private var formIndex = 0
    private var lastCard: SpellingCard? = null

    private data class QueuedMiss(val card: SpellingCard, var wait: Int)
    private val missed = mutableListOf<QueuedMiss>()

    /** Next problem for a spelling battle, drawing from [cards] only (one unit's deck). */
    fun nextProblem(cards: List<SpellingCard>): SpellingProblem {
        require(cards.isNotEmpty()) { "Spelling battle needs a non-empty unit deck" }
        // A missed card returns only after at least 2 other questions:
        // it is queued with wait=2, decremented on each normal pick, and
        // returned as PICK (against its own misspellings) once due.
        val due = missed.firstOrNull()?.takeIf { it.wait <= 0 }
        if (due != null) {
            missed.remove(due)
            return makeProblem(due.card, SpellingForm.PICK)
        }
        for (queued in missed) queued.wait--
        val card = if (isAdjusted) {
            cards[deckIndex % cards.size].also { deckIndex++ }
        } else {
            val others = cards.filter { it.id != lastCard?.id }
            (if (others.isNotEmpty()) others.random(random) else cards.random(random))
        }
        lastCard = card
        val forms = listOf(SpellingForm.PICK, SpellingForm.FILL, SpellingForm.USE)
        val form = forms[formIndex % forms.size].also { formIndex++ }
        return makeProblem(card, form)
    }

    /** Feed a battle answer back so missed cards are re-queued (and cleared on a later correct answer). */
    fun recordResult(card: SpellingCard, correct: Boolean) {
        if (!correct) {
            if (missed.none { it.card.id == card.id }) missed.add(QueuedMiss(card, 2))
        } else {
            missed.removeAll { it.card.id == card.id }
        }
    }

    fun generateSpellingProblem(card: SpellingCard, form: SpellingForm): Problem =
        makeProblem(card, form).problem

    private fun distinct4(answer: String, candidates: List<String>): List<String> {
        val options = mutableListOf(answer)
        for (candidate in candidates) {
            if (candidate !in options) options.add(candidate)
        }
        // Last-resort padding: never crash a battle on a short deck.
        var n = 1
        while (options.size < 4) {
            val filler = "$answer?"
            if ("$filler$n" !in options) options.add("$filler$n")
            n++
        }
        return options.take(4).shuffled(random)
    }

    private fun makeProblem(card: SpellingCard, form: SpellingForm): SpellingProblem {
        return when (form) {
            SpellingForm.PICK -> SpellingProblem(
                card, form,
                Problem(
                    question = "Which spelling is correct for: ${card.definition}",
                    correctAnswer = card.word,
                    options = distinct4(card.word, card.pickDistractors)
                )
            )
            SpellingForm.FILL -> SpellingProblem(
                card, form,
                Problem(
                    question = "Fill in the missing letters: ${card.fillPrompt}",
                    correctAnswer = card.fillAnswer,
                    options = distinct4(card.fillAnswer, card.fillDistractors)
                )
            )
            SpellingForm.USE -> {
                // Blank the word's first occurrence in its own sentence
                // (case-insensitive, so sentence-initial "Your"/"They're" work).
                val blanked = card.exampleSentence.replaceFirst(card.word, "_____", ignoreCase = true)
                SpellingProblem(
                    card, form,
                    Problem(
                        question = "Fill in the blank: $blanked",
                        correctAnswer = card.word,
                        options = distinct4(
                            card.word,
                            card.useDistractors + card.misspellingDistractors + card.pickDistractors
                        )
                    )
                )
            }
            SpellingForm.SPELL -> {
                // Typed form lives in lessons, not battles; keep it total anyway.
                SpellingProblem(
                    card, form,
                    Problem(
                        question = "Spell this word: ${card.definition}",
                        correctAnswer = card.word,
                        options = emptyList()
                    )
                )
            }
        }
    }
}
