package com.example.vitalwearclonev1.game

/**
 * Gamified Spelling Cards (2026-11-01, education item 2 of 3).
 *
 * Mirrors FormulaCard: one card is one word to memorize, with an
 * original definition, an original example sentence, a breakdown of
 * how the word is built, and one original memory trick. The word
 * itself and its standard meaning are facts, but every definition
 * sentence, example sentence, breakdown, and trick here is written
 * for this app — no dictionary definition, textbook example sentence,
 * published spelling list ordering, or published mnemonic is copied.
 *
 * This file is pure Kotlin (no Android types) so the deck, mastery,
 * and problem generation can be unit-tested on the JVM.
 */
enum class SpellingBand { GENTLE_3_5, UPPER_6_PLUS }

/** Battle prompt forms, plus SPELL for typed lesson practice. */
enum class SpellingForm { PICK, FILL, USE, SPELL }

data class SpellingCard(
    val id: String,
    val word: String,
    val partOfSpeech: String,
    /** Definition in original words, written for this app. */
    val definition: String,
    /** One original example sentence with the word in place. */
    val exampleSentence: String,
    /** How the word is built: sound chunks / prefix-root-suffix / tricky letters. */
    val breakdown: String,
    /** One original memory trick, written for this app. */
    val memoryTrick: String,
    val band: SpellingBand,
    /** Card ids that are worth learning before this one (deck order also encodes this). */
    val prerequisites: List<String> = emptyList(),
    /** Misspellings of this word, used for PICK padding and USE padding. */
    val misspellingDistractors: List<String> = emptyList(),
    /** The homophone/confusable of this word, if it has one (also a PICK distractor). */
    val homophone: String? = null,
    /** Other words in the same confusable set, for USE (meaning) questions. */
    val useDistractors: List<String> = emptyList(),
    /** Exactly 3 PICK distractors: misspellings, plus the homophone for confusable cards. */
    val pickDistractors: List<String> = emptyList(),
    /** Fill-in-the-blank prompt, e.g. "nece__ary". */
    val fillPrompt: String = "",
    /** Answer to the fill-in-the-blank prompt. */
    val fillAnswer: String = "",
    /** Distractors for the fill-in-the-blank prompt. */
    val fillDistractors: List<String> = emptyList()
)

data class SpellingUnit(
    val id: String,
    val name: String,
    val band: SpellingBand,
    val cardIds: List<String>
) {
    fun cards(): List<SpellingCard> = cardIds.mapNotNull { id -> ALL_SPELLING_CARDS.firstOrNull { it.id == id } }
}
