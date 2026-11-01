package com.example.vitalwearclonev1.game

/**
 * Gamified Formula Cards (2026-11-01, education item 1 of 3).
 *
 * A FormulaCard is one fact to memorize: the formula in symbols, the same
 * formula read in words, one worked example with real numbers, and one
 * original memory trick. All teaching text here is written for this app;
 * the formulas themselves are mathematical facts, but no textbook wording,
 * worked example, mnemonic, or published curriculum sequence is copied.
 *
 * This file is pure Kotlin (no Android types) so the deck, mastery, and
 * problem generation can be unit-tested on the JVM.
 */
enum class FormulaBand { GENTLE_3_5, UPPER_6_PLUS }

enum class FormulaForm { NAME, FILL, APPLY }

data class FormulaCard(
    val id: String,
    val name: String,
    /** Formula in symbols, e.g. "A = l × w". */
    val formulaSymbols: String,
    /** Formula read in words, e.g. "Area equals length times width". */
    val formulaWords: String,
    /** One worked example with numbers, written for this app. */
    val workedExample: String,
    /** One original memory trick, written for this app. */
    val memoryTrick: String,
    val band: FormulaBand,
    /** Card ids that are worth learning before this one (deck order also encodes this). */
    val prerequisites: List<String> = emptyList(),
    /** Symbol multiple-choice distractors for the NAME prompt form. */
    val nameDistractors: List<String> = emptyList(),
    /** Fill-in-the-blank prompt, e.g. "A = ___ × w". */
    val fillPrompt: String = "",
    /** Answer to the fill-in-the-blank prompt. */
    val fillAnswer: String = "",
    /** Distractors for the fill-in-the-blank prompt. */
    val fillDistractors: List<String> = emptyList()
)

data class FormulaUnit(
    val id: String,
    val name: String,
    val band: FormulaBand,
    val cardIds: List<String>
) {
    fun cards(): List<FormulaCard> = cardIds.mapNotNull { id -> ALL_FORMULA_CARDS.firstOrNull { it.id == id } }
}
