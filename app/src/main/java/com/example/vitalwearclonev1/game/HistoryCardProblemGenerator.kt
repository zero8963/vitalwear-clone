package com.example.vitalwearclonev1.game

import kotlin.random.Random

/**
 * History Card battle problems (2026-11-01). Pure Kotlin, no Android types.
 *
 * Named HistoryCardProblemGenerator (not HistoryProblemGenerator)
 * because that name is already the OLD history subject generator
 * (grade/floor-based, used by rememberProblemRepository) — the two
 * must not collide, and the old HISTORY mode stays untouched.
 *
 * Battles stay options-only (GamePlayScreen answers are options), so
 * ORDER is implemented options-compatibly — no drag-to-order UI in
 * Slice 1. Four prompt forms rotate per enemy turn:
 * - NAME: "Which event is this?" — description (no name, no date)
 *   vs. the event name plus other unit events as distractors.
 * - DATE: "When did this happen?" — event vs. its own close date
 *   distractors from the same unit, never random pool answers.
 * - ORDER: "Which of these happened FIRST?" / "LAST?" with the
 *   unit's events as options (answer by dateSortKey), or "Did X
 *   happen before or after Y?" with Before/After options. For
 *   first/last questions the problem's card is the answer event
 *   (the event the learner identified); for before/after it is the
 *   subject event.
 * - CAUSE: "What did X lead to?" (consequence vs. other cards'
 *   consequences) or "What helped cause X?" (cause vs. other
 *   cards' causes).
 *
 * Paced Learning on means deck order — which for history units IS
 * chronological order, so the deck itself teaches the timeline;
 * off means shuffled cards. No audio is assumed: every prompt is
 * description/date text.
 */
data class HistoryProblem(
    val card: HistoryCard,
    val form: HistoryForm,
    val problem: Problem
)

class HistoryCardProblemGenerator(
    private val isAdjusted: Boolean,
    private val random: Random = Random(System.currentTimeMillis())
) {
    private var deckIndex = 0
    private var formIndex = 0
    private var orderVariantIndex = 0
    private var causeVariantIndex = 0
    private var lastCard: HistoryCard? = null

    private data class QueuedMiss(val card: HistoryCard, var wait: Int)
    private val missed = mutableListOf<QueuedMiss>()

    /** Next problem for a history battle, drawing from [cards] only (one unit's deck). */
    fun nextProblem(cards: List<HistoryCard>): HistoryProblem {
        require(cards.isNotEmpty()) { "History battle needs a non-empty unit deck" }
        // A missed card returns only after at least 2 other questions:
        // it is queued with wait=2, decremented on each normal pick, and
        // returned as ORDER once due.
        val due = missed.firstOrNull()?.takeIf { it.wait <= 0 }
        if (due != null) {
            missed.remove(due)
            return makeProblem(due.card, HistoryForm.ORDER, cards)
        }
        for (queued in missed) queued.wait--
        val card = if (isAdjusted) {
            cards[deckIndex % cards.size].also { deckIndex++ }
        } else {
            val others = cards.filter { it.id != lastCard?.id }
            (if (others.isNotEmpty()) others.random(random) else cards.random(random))
        }
        lastCard = card
        val forms = listOf(HistoryForm.NAME, HistoryForm.DATE, HistoryForm.ORDER, HistoryForm.CAUSE)
        val form = forms[formIndex % forms.size].also { formIndex++ }
        return makeProblem(card, form, cards)
    }

    /** Feed a battle answer back so missed cards are re-queued (and cleared on a later correct answer). */
    fun recordResult(card: HistoryCard, correct: Boolean) {
        if (!correct) {
            if (missed.none { it.card.id == card.id }) missed.add(QueuedMiss(card, 2))
        } else {
            missed.removeAll { it.card.id == card.id }
        }
    }

    fun generateHistoryProblem(card: HistoryCard, form: HistoryForm, cards: List<HistoryCard>): Problem =
        makeProblem(card, form, cards).problem

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

    private fun makeProblem(card: HistoryCard, form: HistoryForm, cards: List<HistoryCard>): HistoryProblem {
        val others = cards.filter { it.id != card.id }
        return when (form) {
            HistoryForm.NAME -> HistoryProblem(
                card, form,
                Problem(
                    question = "Which event is this? ${card.description}",
                    correctAnswer = card.eventName,
                    options = distinct4(card.eventName, others.map { it.eventName })
                )
            )
            HistoryForm.DATE -> HistoryProblem(
                card, form,
                Problem(
                    question = "When did this happen? ${card.eventName} (${card.eraLabel})",
                    correctAnswer = card.dateLabel,
                    options = distinct4(card.dateLabel, card.dateDistractors)
                )
            )
            HistoryForm.ORDER -> {
                when (orderVariantIndex++ % 3) {
                    0 -> {
                        // Which happened FIRST? The answer event is the problem's card.
                        val earliest = cards.minByOrNull { it.dateSortKey }!!
                        HistoryProblem(
                            earliest, form,
                            Problem(
                                question = "Which of these happened FIRST?",
                                correctAnswer = earliest.eventName,
                                options = distinct4(earliest.eventName, cards.filter { it.id != earliest.id }.map { it.eventName })
                            )
                        )
                    }
                    1 -> {
                        // Which happened LAST? The answer event is the problem's card.
                        val latest = cards.maxByOrNull { it.dateSortKey }!!
                        HistoryProblem(
                            latest, form,
                            Problem(
                                question = "Which of these happened LAST?",
                                correctAnswer = latest.eventName,
                                options = distinct4(latest.eventName, cards.filter { it.id != latest.id }.map { it.eventName })
                            )
                        )
                    }
                    else -> {
                        // Before/after pair: subject card stays the problem's card.
                        val other = others.random(random)
                        val answer = if (card.dateSortKey < other.dateSortKey) "Before" else "After"
                        HistoryProblem(
                            card, form,
                            Problem(
                                question = "Did ${card.eventName} happen before or after ${other.eventName}?",
                                correctAnswer = answer,
                                options = listOf("Before", "After").shuffled(random)
                            )
                        )
                    }
                }
            }
            HistoryForm.CAUSE -> {
                if (causeVariantIndex++ % 2 == 0) {
                    HistoryProblem(
                        card, form,
                        Problem(
                            question = "What did ${card.eventName} lead to?",
                            correctAnswer = card.consequence,
                            options = distinct4(card.consequence, others.map { it.consequence })
                        )
                    )
                } else {
                    HistoryProblem(
                        card, form,
                        Problem(
                            question = "What helped cause ${card.eventName}?",
                            correctAnswer = card.cause,
                            options = distinct4(card.cause, others.map { it.cause })
                        )
                    )
                }
            }
        }
    }
}
