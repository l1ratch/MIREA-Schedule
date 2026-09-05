package com.jetbrains.kmpapp.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TaskPriority(
    val displayName: String,
    val order: Int
) {
    CRITICAL("Критичный", 0),
    HIGH("Высокий", 1),
    MEDIUM("Средний", 2),
    LOW("Низкий", 3);
}

@Serializable
enum class TaskCategory(
    val displayName: String
) {
    LAB("Лабораторная"),
    PRACTICE("Практика"),
    HOMEWORK("Домашнее задание"),
    TERM_PROJECT("Курсовая работа"),
    SEMINAR("Семинар / Доклад"),
    TEST_PREP("Подготовка к зачету"),
    CUSTOM("Другое");
}

@Serializable
enum class TaskStatus(
    val displayName: String
) {
    PENDING("Не начато"),
    IN_PROGRESS("В процессе"),
    SUBMITTED("На проверке"),
    COMPLETED("Зачтено"),
    NEEDS_FIX("Требует доработки");

    val isFinished: Boolean
        get() = this == COMPLETED
}

@Serializable
data class Subtask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

@Serializable
data class StudyTask(
    val id: String,
    val subjectTitle: String,
    val title: String,
    val taskDescription: String = "",
    val category: TaskCategory = TaskCategory.LAB,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val subtasks: List<Subtask> = emptyList(),
    val dueDateIso: String? = null,
    val pointsEarned: Double? = null,
    val maxPoints: Double? = null,
    val createdAtIso: String = ""
) {
    val completionRatio: Double
        get() {
            if (status == TaskStatus.COMPLETED) return 1.0
            if (subtasks.isEmpty()) {
                return when (status) {
                    TaskStatus.IN_PROGRESS -> 0.5
                    TaskStatus.SUBMITTED -> 0.8
                    TaskStatus.NEEDS_FIX -> 0.3
                    TaskStatus.PENDING -> 0.0
                    TaskStatus.COMPLETED -> 1.0
                }
            }
            val completed = subtasks.count { it.isCompleted }
            return completed.toDouble() / subtasks.size
        }

    val progressPercentage: Int
        get() = (completionRatio * 100).toInt()
}
