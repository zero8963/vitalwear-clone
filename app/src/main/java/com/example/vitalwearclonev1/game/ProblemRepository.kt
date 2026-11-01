package com.example.vitalwearclonev1.game

interface ProblemProvider {
    suspend fun getProblem(mode: String, grade: String, floor: Int, isAdjusted: Boolean): Problem?
    suspend fun getLesson(mode: String, grade: String, seenTitles: List<String>): Lesson?
}

/**
 * Convert a SpellingCard into a Lesson the existing lesson screen can
 * render unchanged. [typed] picks the practice path: typed SPELL
 * practice (empty options → the text-input branch, the first lessons
 * to use it) or multiple-choice PICK practice (options → buttons).
 * The lesson screen records typed correct as SPELL (levels mastery)
 * and multiple-choice correct as PICK (last-seen only), mirroring
 * the formula NAME/APPLY split.
 */
fun spellingCardToLesson(card: SpellingCard, typed: Boolean): Lesson = Lesson(
    title = card.word,
    explanation = "${card.partOfSpeech.replaceFirstChar { it.uppercase() }}. ${card.definition} Breakdown: ${card.breakdown}",
    steps = listOf(card.definition, "Breakdown: ${card.breakdown}"),
    example = card.exampleSentence,
    type = LessonType.PUNCTUATION_AND_SPELLING,
    practiceQuestion = if (typed) "Spell this word: ${card.definition}" else "Which spelling is correct for: ${card.definition}?",
    practiceAnswer = card.word,
    practiceOptions = if (typed) emptyList() else (listOf(card.word) + card.pickDistractors).shuffled(),
    solveSteps = listOf(card.exampleSentence, "Memory trick: ${card.memoryTrick}")
)

/**
 * Convert a HistoryCard into a Lesson the existing lesson screen can
 * render unchanged. [form] picks the practice path: NAME asks the
 * event from its description, DATE asks the date/era from the event
 * — both options-based. The lesson screen records lesson NAME/DATE
 * correct as last-seen refresh only (mirroring formula NAME), so
 * units are mastered through battles (ORDER/CAUSE), not re-reading.
 */
fun historyCardToLesson(card: HistoryCard, form: HistoryForm): Lesson = Lesson(
    title = card.eventName,
    explanation = "${card.eraLabel}. ${card.dateLabel}. ${card.description} Why it mattered: ${card.whyItMattered}",
    steps = listOf(card.description, "Why it mattered: ${card.whyItMattered}"),
    example = "Date: ${card.dateLabel}. Era: ${card.eraLabel}.",
    type = LessonType.SCIENCE_FACTS,
    practiceQuestion = if (form == HistoryForm.DATE) "When did this happen? ${card.eventName}" else "Which event is this? ${card.description}",
    practiceAnswer = if (form == HistoryForm.DATE) card.dateLabel else card.eventName,
    practiceOptions = if (form == HistoryForm.DATE) (listOf(card.dateLabel) + card.dateDistractors).shuffled() else (listOf(card.eventName) + ALL_HISTORY_CARDS.filter { it.band == card.band && it.id != card.id }.map { it.eventName }).shuffled(),
    solveSteps = listOf("Date: ${card.dateLabel}.", "Memory hook: ${card.memoryHook}")
)

/** Convert a FormulaCard into a Lesson the existing lesson screen can render unchanged. */
fun formulaCardToLesson(card: FormulaCard): Lesson = Lesson(
    title = card.name,
    explanation = "${card.formulaWords} Formula: ${card.formulaSymbols}",
    steps = listOf(card.formulaWords, "Formula: ${card.formulaSymbols}"),
    example = card.workedExample,
    type = LessonType.HOW_TO_SOLVE,
    practiceQuestion = "What is the formula for ${card.name}?",
    practiceAnswer = card.formulaSymbols,
    practiceOptions = (listOf(card.formulaSymbols) + card.nameDistractors).shuffled(),
    solveSteps = listOf(card.workedExample, "Memory trick: ${card.memoryTrick}")
)

class OfflineProblemProvider(
    private val mathGen: MathProblemGenerator,
    private val readingGen: ReadingProblemGenerator,
    private val historyGen: HistoryProblemGenerator,
    private val scienceGen: ScienceProblemGenerator,
    private val geometryGen: GeometryProblemGenerator,
    private val lifeScienceGen: LifeScienceProblemGenerator
) : ProblemProvider {
    override suspend fun getProblem(mode: String, grade: String, floor: Int, isAdjusted: Boolean): Problem {
        return when (mode) {
            // Formula battles use FormulaProblemGenerator directly with a
            // unit deck; this path only serves callers that ask the
            // repository for a plain formula problem.
            "FORMULA" -> {
                val cards = formulaCardsForBand(formulaBandForGrade(grade))
                FormulaProblemGenerator(isAdjusted)
                    .generateFormulaProblem(cards.first(), FormulaForm.NAME)
            }
            // Spelling battles use SpellingProblemGenerator directly with a
            // unit deck; this path only serves callers that ask the
            // repository for a plain spelling problem.
            "SPELLING" -> {
                val cards = spellingCardsForBand(spellingBandForGrade(grade))
                SpellingProblemGenerator(isAdjusted)
                    .generateSpellingProblem(cards.first(), SpellingForm.PICK)
            }
            // History Card battles use HistoryCardProblemGenerator
            // directly with a unit deck; this path only serves callers
            // that ask the repository for a plain history problem.
            // Mode is HISTORY_CARDS, not HISTORY (already the old subject).
            "HISTORY_CARDS" -> {
                val cards = historyCardsForBand(historyBandForGrade(grade))
                HistoryCardProblemGenerator(isAdjusted)
                    .generateHistoryProblem(cards.first(), HistoryForm.NAME, cards)
            }
            "MATH" -> mathGen.generate()
            "READING" -> readingGen.generate()
            "HISTORY" -> historyGen.generate()
            "GEOMETRY" -> geometryGen.generate()
            "LIFESCIENCE" -> lifeScienceGen.generate()
            else -> scienceGen.generate()
        }
    }

    override suspend fun getLesson(mode: String, grade: String, seenTitles: List<String>): Lesson {
        return when (mode) {
            "FORMULA" -> {
                val cards = formulaCardsForBand(formulaBandForGrade(grade))
                val unseen = cards.filter { it.name !in seenTitles }
                formulaCardToLesson(unseen.firstOrNull() ?: cards.first())
            }
            "SPELLING" -> {
                val cards = spellingCardsForBand(spellingBandForGrade(grade))
                val unseen = cards.filter { it.word !in seenTitles }
                val card = unseen.firstOrNull() ?: cards.first()
                // Alternate practice paths across lessons so learners get
                // both typed SPELL practice and multiple-choice PICK practice.
                spellingCardToLesson(card, typed = seenTitles.size % 2 == 0)
            }
            "HISTORY_CARDS" -> {
                val cards = historyCardsForBand(historyBandForGrade(grade))
                val unseen = cards.filter { it.eventName !in seenTitles }
                val card = unseen.firstOrNull() ?: cards.first()
                // Alternate NAME and DATE practice across lessons (like
                // spelling alternates typed/PICK).
                historyCardToLesson(card, if (seenTitles.size % 2 == 0) HistoryForm.NAME else HistoryForm.DATE)
            }
            "MATH" -> mathGen.generateLesson(seenTitles)
            "READING" -> readingGen.generateLesson(seenTitles)
            "HISTORY" -> historyGen.generateLesson(seenTitles)
            "GEOMETRY" -> geometryGen.generateLesson(seenTitles)
            "LIFESCIENCE" -> lifeScienceGen.generateLesson(seenTitles)
            else -> scienceGen.generateLesson(seenTitles)
        }
    }
}

class ProblemRepository(
    private val offlineProvider: ProblemProvider
) {
    suspend fun getProblem(mode: String, grade: String, floor: Int, isAdjusted: Boolean): Problem {
        return offlineProvider.getProblem(mode, grade, floor, isAdjusted)!!
    }

    suspend fun getLesson(mode: String, grade: String, seenTitles: List<String>): Lesson {
        return offlineProvider.getLesson(mode, grade, seenTitles)!!
    }
}
