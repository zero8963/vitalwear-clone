package com.example.vitalwearclonev1.game

import kotlinx.serialization.Serializable

@Serializable
enum class LessonType {
    HOW_TO_SOLVE,
    PUNCTUATION_AND_SPELLING,
    SCIENCE_FACTS
}

@Serializable
data class Lesson(
    val title: String,
    val explanation: String,
    val steps: List<String> = emptyList(),
    val example: String? = null,
    val type: LessonType,
    val practiceQuestion: String? = null,
    val practiceAnswer: String? = null,
    val practiceOptions: List<String> = emptyList(),
    val solveSteps: List<String> = emptyList()
)

@Serializable
data class Problem(
    val question: String,
    val correctAnswer: String,
    val options: List<String>
)
