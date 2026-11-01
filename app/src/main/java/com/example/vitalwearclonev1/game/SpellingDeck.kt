package com.example.vitalwearclonev1.game

/**
 * The Slice 1 spelling decks (2026-11-01). Two bands, because the kids
 * this is built for are in 4th, 8th, and 10th grade:
 * - GENTLE_3_5: the gentle deck for the youngest (4th grade).
 * - UPPER_6_PLUS: the grades 6+ deck for the two older kids.
 *
 * Slice 1 leads with confusables for the upper unit (the most visible
 * classroom pain, and already half-taught by the existing Reading
 * lesson) alongside tricky everyday words for the gentle unit.
 * All definitions, example sentences, breakdowns, and tricks are
 * original to this app. Units are 4–6 cards each. A unit is mastered
 * when every card in it is at mastery level 2 or higher (level 3 is
 * the gold "Mastered" card).
 */

/** Gentle grades 3–5 deck for the youngest (4th grade): tricky everyday words. */
val GENTLE_SPELLING_CARDS: List<SpellingCard> = listOf(
    SpellingCard(
        id = "spell_necessary",
        word = "necessary",
        partOfSpeech = "adjective",
        definition = "Needed in order to get something done; you cannot skip it.",
        exampleSentence = "A good warm-up is necessary before you lift anything heavy.",
        breakdown = "ne-ce-ssa-ry: say it in four beats, and hold the double s in the middle.",
        memoryTrick = "The middle of the word has two s's standing together, like a pair of shoes you need for the game.",
        band = SpellingBand.GENTLE_3_5,
        misspellingDistractors = listOf("neccessary", "necesary", "neccesary"),
        pickDistractors = listOf("neccessary", "necesary", "neccesary"),
        fillPrompt = "nece__ary",
        fillAnswer = "ss",
        fillDistractors = listOf("s", "c", "ssc")
    ),
    SpellingCard(
        id = "spell_separate",
        word = "separate",
        partOfSpeech = "verb",
        definition = "To split one group into two or more parts.",
        exampleSentence = "Please separate the clean towels from the dirty ones.",
        breakdown = "se-pa-rate: three beats, and the middle beat is spelled with an a.",
        memoryTrick = "Stretch it out: sep-AAA-rate. The stretched sound is the letter a, right in the middle.",
        band = SpellingBand.GENTLE_3_5,
        prerequisites = listOf("spell_necessary"),
        misspellingDistractors = listOf("seperate", "separete", "seperete"),
        pickDistractors = listOf("seperate", "separete", "seperete"),
        fillPrompt = "sepa_ate",
        fillAnswer = "r",
        fillDistractors = listOf("rr", "e", "t")
    ),
    SpellingCard(
        id = "spell_definitely",
        word = "definitely",
        partOfSpeech = "adverb",
        definition = "Without any doubt; certainly.",
        exampleSentence = "I will definitely bring water to practice tomorrow.",
        breakdown = "de-fi-nite-ly: build it from 'definite' and add 'ly' at the end.",
        memoryTrick = "Start from 'finite', a word you already know, put 'de' in front and 'ly' at the end, and do not let an extra a sneak in.",
        band = SpellingBand.GENTLE_3_5,
        prerequisites = listOf("spell_separate"),
        misspellingDistractors = listOf("definately", "definitly", "definetly"),
        pickDistractors = listOf("definately", "definitly", "definetly"),
        fillPrompt = "definite_y",
        fillAnswer = "l",
        fillDistractors = listOf("ll", "e", "ly")
    ),
    SpellingCard(
        id = "spell_beautiful",
        word = "beautiful",
        partOfSpeech = "adjective",
        definition = "Very pleasing to look at.",
        exampleSentence = "The sunset over the field was beautiful tonight.",
        breakdown = "beau-ti-ful: the first beat is spelled 'beau', which is the tricky part.",
        memoryTrick = "The first beat sounds like 'bow' but is spelled b-e-a-u — picture a bow on a gift, then add 'tiful'.",
        band = SpellingBand.GENTLE_3_5,
        prerequisites = listOf("spell_definitely"),
        misspellingDistractors = listOf("beautifull", "beautifel", "beutiful"),
        pickDistractors = listOf("beautifull", "beautifel", "beutiful"),
        fillPrompt = "beauti__l",
        fillAnswer = "fu",
        fillDistractors = listOf("f", "ful", "v")
    )
)

/** Grades 6+ deck for the two older kids: homophones and confusables, both sets complete. */
val UPPER_SPELLING_CARDS: List<SpellingCard> = listOf(
    SpellingCard(
        id = "spell_their",
        word = "their",
        partOfSpeech = "possessive adjective",
        definition = "Belonging to them.",
        exampleSentence = "The players left their gear in the dugout after practice.",
        breakdown = "the-ir: it starts like 'the' and ends with 'ir'.",
        memoryTrick = "Their shows ownership, and it hides the word 'heir' inside — an heir is someone who owns what is passed down.",
        band = SpellingBand.UPPER_6_PLUS,
        misspellingDistractors = listOf("thier", "ther"),
        homophone = "there",
        useDistractors = listOf("there", "they're"),
        pickDistractors = listOf("thier", "ther", "there"),
        fillPrompt = "the_r",
        fillAnswer = "i",
        fillDistractors = listOf("ie", "y", "e")
    ),
    SpellingCard(
        id = "spell_there",
        word = "there",
        partOfSpeech = "adverb",
        definition = "In that place, not here.",
        exampleSentence = "Put the bag over there by the fence.",
        breakdown = "the-re: it starts like 'the' and ends with 're'.",
        memoryTrick = "There points to a place, and 'here' is hiding inside it — here and there are both places.",
        band = SpellingBand.UPPER_6_PLUS,
        prerequisites = listOf("spell_their"),
        misspellingDistractors = listOf("thare", "thear"),
        homophone = "their",
        useDistractors = listOf("their", "they're"),
        pickDistractors = listOf("thare", "thear", "their"),
        fillPrompt = "the_e",
        fillAnswer = "r",
        fillDistractors = listOf("i", "re", "ar")
    ),
    SpellingCard(
        id = "spell_theyre",
        word = "they're",
        partOfSpeech = "contraction",
        definition = "Short for 'they are'.",
        exampleSentence = "They're heading to the field after lunch.",
        breakdown = "they-'re: two words squeezed together, with an apostrophe marking the missing a.",
        memoryTrick = "If you cannot swap in 'they are', it is not they're — try the swap every time and the apostrophe earns its place.",
        band = SpellingBand.UPPER_6_PLUS,
        prerequisites = listOf("spell_there"),
        misspellingDistractors = listOf("theyre", "thier"),
        homophone = "their",
        useDistractors = listOf("their", "there"),
        pickDistractors = listOf("theyre", "thier", "their"),
        fillPrompt = "they__e",
        fillAnswer = "'r",
        fillDistractors = listOf("r", "re", "s")
    ),
    SpellingCard(
        id = "spell_your",
        word = "your",
        partOfSpeech = "possessive adjective",
        definition = "Belonging to you.",
        exampleSentence = "Your turn to run the drill starts now.",
        breakdown = "yo-ur: four letters, no apostrophe, no extra letters.",
        memoryTrick = "Your shows ownership and never has an apostrophe — owned things do not need a squeezed-together word.",
        band = SpellingBand.UPPER_6_PLUS,
        prerequisites = listOf("spell_theyre"),
        misspellingDistractors = listOf("yor", "yore"),
        homophone = "you're",
        useDistractors = listOf("you're"),
        pickDistractors = listOf("yor", "yore", "you're"),
        fillPrompt = "yo_r",
        fillAnswer = "u",
        fillDistractors = listOf("ou", "o", "ur")
    ),
    SpellingCard(
        id = "spell_youre",
        word = "you're",
        partOfSpeech = "contraction",
        definition = "Short for 'you are'.",
        exampleSentence = "You're up next after the water break.",
        breakdown = "you-'re: two words squeezed together, with an apostrophe marking the missing a.",
        memoryTrick = "Swap in 'you are' — if the sentence still makes sense, you're is the one you want.",
        band = SpellingBand.UPPER_6_PLUS,
        prerequisites = listOf("spell_your"),
        misspellingDistractors = listOf("youre", "yore"),
        homophone = "your",
        useDistractors = listOf("your"),
        pickDistractors = listOf("youre", "your", "yore"),
        fillPrompt = "you__e",
        fillAnswer = "'r",
        fillDistractors = listOf("r", "re", "are")
    )
)

val ALL_SPELLING_CARDS: List<SpellingCard> = GENTLE_SPELLING_CARDS + UPPER_SPELLING_CARDS

/** Units of 4–6 cards each. Order is the unlock order within and across bands. */
val SPELLING_UNITS: List<SpellingUnit> = listOf(
    SpellingUnit(
        id = "gentle_tricky_words",
        name = "Tricky Everyday Words",
        band = SpellingBand.GENTLE_3_5,
        cardIds = listOf("spell_necessary", "spell_separate", "spell_definitely", "spell_beautiful")
    ),
    SpellingUnit(
        id = "upper_homophones_confusables",
        name = "Homophones & Confusables",
        band = SpellingBand.UPPER_6_PLUS,
        cardIds = listOf("spell_their", "spell_there", "spell_theyre", "spell_your", "spell_youre")
    )
)

/** Which band's deck shows first for a grade picker value. */
fun spellingBandForGrade(grade: String): SpellingBand {
    val level = if (grade == "K") 0 else grade.toIntOrNull() ?: 1
    return if (level >= 6) SpellingBand.UPPER_6_PLUS else SpellingBand.GENTLE_3_5
}

fun spellingCardsForBand(band: SpellingBand): List<SpellingCard> =
    ALL_SPELLING_CARDS.filter { it.band == band }

fun spellingCardById(id: String): SpellingCard? = ALL_SPELLING_CARDS.firstOrNull { it.id == id }

fun spellingCardByWord(word: String): SpellingCard? = ALL_SPELLING_CARDS.firstOrNull { it.word == word }

/**
 * Global unlock rule (not per grade), mirroring formulas: the gentle
 * unit is always open. The first upper unit opens when the gentle unit
 * is mastered OR the entry band is already upper. Each later unit
 * opens when the previous unit is mastered. [masteredUnitIds] are unit
 * ids whose every card is at mastery level 2 or higher.
 */
fun computeUnlockedSpellingUnits(
    entryBand: SpellingBand,
    masteredUnitIds: Set<String>
): List<SpellingUnit> {
    val unlocked = mutableListOf<SpellingUnit>()
    for ((index, unit) in SPELLING_UNITS.withIndex()) {
        val open = when (index) {
            0 -> true
            1 -> "gentle_tricky_words" in masteredUnitIds || entryBand == SpellingBand.UPPER_6_PLUS
            else -> SPELLING_UNITS[index - 1].id in masteredUnitIds
        }
        if (open) unlocked.add(unit)
    }
    return unlocked
}
