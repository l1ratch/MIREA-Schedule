package com.jetbrains.kmpapp.data.notifications

import com.jetbrains.kmpapp.data.model.Lesson
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Локальные напоминания о ближайшем занятии выбранного расписания.
 * Бронью: все пары на 7 дней вперёд (потолок 60 из 64 запросов iOS;
 * фактически ~35) — телефон, пролежавший запертым неделю, всё равно
 * получает напоминания. Перепланируется при любом изменении кэша/настроек.
 */
object NotificationsManager {
    /** iOS-движок: UNUserNotificationCenter. Идентификаторы "lesson-<date>-<bell>". */
    interface NotificationEngine {
        /** Запросить системное разрешение (вызывается при включении тумблера). */
        fun requestAuthorization()
        /** Запланировать уведомление с идентификатором; dateEpochMillis — время показа. */
        fun schedule(id: String, title: String, body: String, dateEpochMillis: Long)
        /** Снять все запланированные (в т.ч. предыдущую партию). */
        fun cancelAll()
    }

    /** Максимальный горизонт планирования. */
    const val DAYS_AHEAD = 7

    var engine: NotificationEngine? = null
        private set

    /** true только там, где платформа умеет показывать локальные уведомления. */
    val supportsNotifications: Boolean get() = engine != null

    fun setEngine(newEngine: NotificationEngine) {
        engine = newEngine
    }

    fun requestAuthorization() {
        engine?.requestAuthorization()
    }

    /**
     * Перепланировать напоминания: снять прошлую партию и забронировать все
     * занятия [lessons] на следующие [DAYS_AHEAD] дней, у которых время
     * показа (минус [minutesBefore]) ещё не наступило.
     */
    fun reschedule(lessons: List<Lesson>, minutesBefore: Int, formatter: (Lesson) -> String) {
        val eng = engine ?: return
        eng.cancelAll()
        if (minutesBefore <= 0) return

        val nowLocal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val horizonDate = nowLocal.date.plus(DatePeriod(days = DAYS_AHEAD))

        var scheduled = 0
        lessons.asSequence()
            .filter { it.date >= nowLocal.date && it.date <= horizonDate }
            .sortedWith(compareBy({ it.date }, { it.bellNumber }))
            .forEach { lesson ->
                if (scheduled >= 60) return@forEach // ponytail: потолок 64 iOS-запросов
                val fireAt = lessonStartMinus(lesson, minutesBefore) ?: return@forEach
                if (fireAt <= nowLocal) return@forEach // время показа уже прошло
                eng.schedule(
                    id = "lesson-${lesson.date}-${lesson.bellNumber}",
                    title = "Скоро занятие",
                    body = formatter(lesson),
                    dateEpochMillis = fireAt.toInstant(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds()
                )
                scheduled++
            }
    }

    /** Время показа уведомления = начало пары минус [minusMinutes]. */
    private fun lessonStartMinus(lesson: Lesson, minusMinutes: Int): LocalDateTime? {
        val parts = lesson.startTime.split(':')
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val total = hour * 60 + minute - minusMinutes
        if (total < 0) return null // уведомление должно было уйти до полуночи
        return LocalDateTime(lesson.date, LocalTime(total / 60, total % 60))
    }
}
