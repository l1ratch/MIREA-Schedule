package com.jetbrains.kmpapp.data.parser

import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.LessonType
import com.jetbrains.kmpapp.data.model.defaultBells
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

object MireaICalParser {

    fun parse(rawIcal: String): List<Lesson> {
        val unfolded = unfoldLines(rawIcal)
        val lessons = mutableListOf<Lesson>()

        var inEvent = false
        val currentProps = mutableMapOf<String, MutableList<String>>()

        for (line in unfolded) {
            val trimmed = line.trim()
            if (trimmed == "BEGIN:VEVENT") {
                inEvent = true
                currentProps.clear()
            } else if (trimmed == "END:VEVENT") {
                inEvent = false
                parseEvent(currentProps)?.let { lessons.addAll(it) }
            } else if (inEvent && ':' in trimmed) {
                val colonIdx = trimmed.indexOf(':')
                val rawKey = trimmed.substring(0, colonIdx)
                val value = trimmed.substring(colonIdx + 1).trim()
                val key = rawKey.split(';')[0].uppercase()
                currentProps.getOrPut(key) { mutableListOf() }.add(value)
            }
        }

        return lessons.sortedWith(compareBy({ it.date }, { it.bellNumber }))
    }

    private fun unfoldLines(raw: String): List<String> {
        val result = mutableListOf<String>()
        val lines = raw.lines()
        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (result.isNotEmpty()) {
                    val last = result.removeAt(result.size - 1)
                    result.add(last + line.substring(1))
                }
            } else {
                result.add(line)
            }
        }
        return result
    }

    private fun parseEvent(props: Map<String, List<String>>): List<Lesson>? {
        if (props["TRANSP"]?.firstOrNull()?.uppercase() == "TRANSPARENT") {
            return null
        }

        val subject = props["X-META-DISCIPLINE"]?.firstOrNull()
            ?: props["SUMMARY"]?.firstOrNull()
            ?: return null

        val rawType = props["X-META-LESSON_TYPE"]?.firstOrNull()
            ?: props["X-META-FULL_LESSON_TYPE"]?.firstOrNull() ?: ""
        val lessonType = when {
            rawType.contains("лк", ignoreCase = true) || rawType.contains("лек", ignoreCase = true) -> LessonType.LECTURE
            rawType.contains("пр", ignoreCase = true) || rawType.contains("прак", ignoreCase = true) -> LessonType.PRACTICE
            rawType.contains("лаб", ignoreCase = true) -> LessonType.LAB
            else -> LessonType.OTHER
        }

        val teachers = props["X-META-TEACHER"] ?: emptyList()
        val classrooms = props["X-META-AUDITORIUM"] ?: props["LOCATION"] ?: emptyList()
        val groups = props["X-META-GROUP"] ?: emptyList()

        val dtStartRaw = props["DTSTART"]?.firstOrNull() ?: return null
        val dtEndRaw = props["DTEND"]?.firstOrNull() ?: ""

        val startDate = parseDate(dtStartRaw) ?: return null
        val startTime = parseTime(dtStartRaw)
        val endTime = parseTime(dtEndRaw)

        val bellNumber = determineBellNumber(startTime)
        val bell = defaultBells.firstOrNull { it.number == bellNumber }
        val finalStart = if (startTime.isNotBlank()) startTime else (bell?.startTime ?: "09:00")
        val finalEnd = if (endTime.isNotBlank()) endTime else (bell?.endTime ?: "10:30")

        val dates = mutableListOf<LocalDate>()
        val rrule = props["RRULE"]?.firstOrNull()
        val exdates = props["EXDATE"]?.flatMap { it.split(',') }?.mapNotNull { parseDate(it) }?.toSet() ?: emptySet()

        if (rrule != null) {
            val interval = parseRruleInterval(rrule)
            val untilDate = parseRruleUntil(rrule)
            var current = startDate
            val limit = untilDate ?: startDate.plus(DatePeriod(months = 5))

            while (current <= limit) {
                if (current !in exdates) {
                    dates.add(current)
                }
                current = current.plus(DatePeriod(days = 7 * interval))
            }
        } else {
            if (startDate !in exdates) {
                dates.add(startDate)
            }
        }

        val uid = props["UID"]?.firstOrNull() ?: subject

        return dates.map { date ->
            Lesson(
                id = "${uid}_$date",
                subject = subject,
                lessonType = lessonType,
                teachers = teachers.filter { it.isNotBlank() },
                classrooms = classrooms.filter { it.isNotBlank() },
                bellNumber = bellNumber,
                startTime = finalStart,
                endTime = finalEnd,
                date = date,
                groups = groups.filter { it.isNotBlank() }
            )
        }
    }

    private fun parseDate(raw: String): LocalDate? {
        val clean = raw.trim().replace("T", "")
        if (clean.length < 8) return null
        val y = clean.substring(0, 4).toIntOrNull() ?: return null
        val m = clean.substring(4, 6).toIntOrNull() ?: return null
        val d = clean.substring(6, 8).toIntOrNull() ?: return null
        return try {
            LocalDate(y, m, d)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTime(raw: String): String {
        val tIdx = raw.indexOf('T')
        if (tIdx == -1 || raw.length < tIdx + 5) return ""
        val timePart = raw.substring(tIdx + 1)
        val hh = timePart.substring(0, 2)
        val mm = timePart.substring(2, 4)
        return "$hh:$mm"
    }

    private fun determineBellNumber(time: String): Int {
        return when {
            time.startsWith("09:") || time.startsWith("08:") -> 1
            time.startsWith("10:") || time.startsWith("11:") -> 2
            time.startsWith("12:") || time.startsWith("13:") -> 3
            time.startsWith("14:") || time.startsWith("15:") -> 4
            time.startsWith("16:") || time.startsWith("17:") -> 5
            time.startsWith("18:") -> 6
            time.startsWith("19:") || time.startsWith("20:") -> 7
            else -> 1
        }
    }

    private fun parseRruleInterval(rrule: String): Int {
        val parts = rrule.split(';')
        for (part in parts) {
            if (part.startsWith("INTERVAL=", ignoreCase = true)) {
                return part.substring(9).toIntOrNull() ?: 1
            }
        }
        return 1
    }

    private fun parseRruleUntil(rrule: String): LocalDate? {
        val parts = rrule.split(';')
        for (part in parts) {
            if (part.startsWith("UNTIL=", ignoreCase = true)) {
                return parseDate(part.substring(6))
            }
        }
        return null
    }
}
