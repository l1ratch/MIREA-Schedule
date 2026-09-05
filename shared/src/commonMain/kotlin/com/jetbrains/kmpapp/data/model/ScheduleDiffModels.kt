package com.jetbrains.kmpapp.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class LessonDiffType {
    ADDED,
    CANCELLED,
    MODIFIED
}

@Serializable
data class LessonDiffItem(
    val type: LessonDiffType,
    val date: LocalDate,
    val bellNumber: Int,
    val subject: String,
    val description: String
)

@Serializable
data class ScheduleDiff(
    val targetId: Int,
    val targetTitle: String,
    val items: List<LessonDiffItem>
)
