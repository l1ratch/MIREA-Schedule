package com.jetbrains.kmpapp.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class ScheduleTargetType(val id: Int, val pathName: String, val displayName: String) {
    GROUP(1, "Group", "Группа"),
    TEACHER(2, "Teacher", "Преподаватель"),
    AUDITORIUM(3, "Auditorium", "Аудитория");

    companion object {
        fun fromId(id: Int): ScheduleTargetType =
            entries.firstOrNull { it.id == id } ?: GROUP
    }
}

@Serializable
data class ScheduleTarget(
    val id: Int,
    val targetTitle: String,
    val fullTitle: String,
    val scheduleTarget: Int = 1
) {
    val type: ScheduleTargetType
        get() = ScheduleTargetType.fromId(scheduleTarget)
}

@Serializable
enum class LessonType(val displayName: String, val shortName: String) {
    LECTURE("Лекция", "ЛК"),
    PRACTICE("Практика", "ПР"),
    LAB("Лабораторная", "ЛАБ"),
    OTHER("Занятие", "ДР")
}

@Serializable
data class LessonBells(
    val number: Int,
    val startTime: String,
    val endTime: String
)

val defaultBells = listOf(
    LessonBells(1, "09:00", "10:30"),
    LessonBells(2, "10:40", "12:10"),
    LessonBells(3, "12:40", "14:10"),
    LessonBells(4, "14:20", "15:50"),
    LessonBells(5, "16:20", "17:50"),
    LessonBells(6, "18:00", "19:30"),
    LessonBells(7, "19:40", "21:10")
)

@Serializable
data class Lesson(
    val id: String,
    val subject: String,
    val lessonType: LessonType,
    val teachers: List<String>,
    val classrooms: List<String>,
    val bellNumber: Int,
    val startTime: String,
    val endTime: String,
    val date: LocalDate,
    val groups: List<String> = emptyList()
)

data class DaySchedule(
    val date: LocalDate,
    val lessons: List<Lesson>
)

data class SemesterWeekInfo(
    val weekNumber: Int,
    val isEven: Boolean
)

sealed class ScheduleSlot {
    abstract val bellNumber: Int
    abstract val startTime: String
    abstract val endTime: String

    data class Active(
        override val bellNumber: Int,
        override val startTime: String,
        override val endTime: String,
        val lessons: List<Lesson>
    ) : ScheduleSlot()

    data class Empty(
        override val bellNumber: Int,
        override val startTime: String,
        override val endTime: String
    ) : ScheduleSlot()
}

