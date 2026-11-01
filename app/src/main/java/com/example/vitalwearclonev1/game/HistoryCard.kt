package com.example.vitalwearclonev1.game

/**
 * Gamified History Cards (2026-11-01, education item 3 of 3).
 *
 * Mirrors FormulaCard/SpellingCard: one card is one event to learn,
 * with an original account of what happened, an original account of
 * why it mattered (cause and consequence, not just a date to
 * memorize), and one original memory hook. Dates and facts are
 * facts, but every explanation sentence, consequence sentence, and
 * hook here is written for this app — no textbook wording, published
 * timeline selection/ordering, or published mnemonic is copied.
 *
 * dateSortKey is the field the old history code lacks: an integer
 * year used for ORDER questions (negative for BCE). Units keep
 * strictly increasing sort keys, so "which happened first/last" is
 * always unambiguous. For eras given as ranges, the sort key is the
 * range's start year and ORDER questions are phrased as "which
 * began first".
 *
 * Sensitivity rule: factual and neutral, no present-day political
 * slant; peoples are named specifically; "reached the Americas" is
 * used instead of "discovered" framing; where a topic has genuinely
 * multiple perspectives, whyItMattered carries consequences from
 * more than one side at an age-appropriate level.
 *
 * This file is pure Kotlin (no Android types) so the deck, mastery,
 * and problem generation can be unit-tested on the JVM.
 */
enum class HistoryBand { GENTLE_3_5, UPPER_6_PLUS }

/** Battle/lesson prompt forms. ORDER and CAUSE are the performance forms. */
enum class HistoryForm { NAME, DATE, ORDER, CAUSE }

data class HistoryCard(
    val id: String,
    val eventName: String,
    val eraLabel: String,
    /** Display date, e.g. "1776", "1914–1918", "c. 1300–1600". */
    val dateLabel: String,
    /** Integer year for ordering; negative for BCE. */
    val dateSortKey: Int,
    /** What happened, in original words, with no event name and no date in it. */
    val description: String,
    /** Why it mattered: cause and consequence, in original words. */
    val whyItMattered: String,
    /** One original memory hook, written for this app. */
    val memoryHook: String,
    /** Short cause string, used for "What caused X?" questions. */
    val cause: String,
    /** Short consequence string, used for "What did X lead to?" questions. */
    val consequence: String,
    val band: HistoryBand,
    /** Card ids that are worth learning before this one (deck order also encodes this). */
    val prerequisites: List<String> = emptyList(),
    /** Exactly 3 close date distractors, from the same unit or nearby years — never random pool answers. */
    val dateDistractors: List<String> = emptyList(),
    /** Era labels of other cards, used for era questions. */
    val eraDistractors: List<String> = emptyList()
)

data class HistoryUnit(
    val id: String,
    val name: String,
    val band: HistoryBand,
    val cardIds: List<String>
) {
    fun cards(): List<HistoryCard> = cardIds.mapNotNull { id -> ALL_HISTORY_CARDS.firstOrNull { it.id == id } }
}
