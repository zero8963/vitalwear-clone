package com.example.vitalwearclonev1.game

interface ProblemProvider {
    suspend fun getProblem(mode: String, grade: String, floor: Int, isAdjusted: Boolean): Problem?
    suspend fun getLesson(mode: String, grade: String, seenTitles: List<String>): Lesson?
}

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
