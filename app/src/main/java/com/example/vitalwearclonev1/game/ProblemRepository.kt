package com.example.vitalwearclonev1.game

interface ProblemProvider {
    suspend fun getProblem(mode: String, grade: String, floor: Int, isAdjusted: Boolean): Problem?
    suspend fun getLesson(mode: String, grade: String, seenTitles: List<String>): Lesson?
}

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
