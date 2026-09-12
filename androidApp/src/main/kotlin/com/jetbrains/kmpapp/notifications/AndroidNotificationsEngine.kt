package com.jetbrains.kmpapp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.jetbrains.kmpapp.data.notifications.NotificationsManager
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider

/**
 * Android-движок напоминаний: будильники AlarmManager, срабатывание в
 * LessonAlarmReceiver. Идентификаторы детерминированы из id (дата+номер
 * пары) — коллизий между неделями нет (LocalDate с годом), смена группы
 * перезаписывает те же слоты. Список запланированных id хранится в prefs:
 * он переживает рестарт процесса, поэтому cancelAll снимает всё.
 */
object AndroidNotificationsEngine : NotificationsManager.NotificationEngine {

    /** Ставится MainActivity'ом: разрешение POST_NOTIFICATIONS (API 33+) требует Activity. */
    var permissionRequester: (() -> Unit)? = null

    private val context: Context?
        get() = AndroidContextProvider.context

    override fun requestAuthorization() {
        permissionRequester?.invoke()
    }

    override fun schedule(id: String, title: String, body: String, dateEpochMillis: Long) {
        val ctx = context ?: return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dateEpochMillis, pendingIntent(ctx, id, title, body))
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dateEpochMillis, pendingIntent(ctx, id, title, body))
        }
        val p = prefs(ctx)
        val ids = (p.getStringSet(KEY_SCHEDULED_IDS, emptySet()) ?: emptySet()) + id
        p.edit().putStringSet(KEY_SCHEDULED_IDS, ids).apply()
    }

    override fun cancelAll() {
        val ctx = context ?: return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val p = prefs(ctx)
        (p.getStringSet(KEY_SCHEDULED_IDS, emptySet()) ?: emptySet()).forEach { id ->
            am.cancel(pendingIntent(ctx, id, "", ""))
        }
        p.edit().putStringSet(KEY_SCHEDULED_IDS, emptySet()).apply()
    }

    private fun pendingIntent(ctx: Context, id: String, title: String, body: String): PendingIntent {
        val intent = Intent(ctx, LessonAlarmReceiver::class.java).apply {
            action = "$ACTION_PREFIX$id"
            putExtra(LessonAlarmReceiver.EXTRA_ID, id)
            putExtra(LessonAlarmReceiver.EXTRA_TITLE, title)
            putExtra(LessonAlarmReceiver.EXTRA_BODY, body)
        }
        return PendingIntent.getBroadcast(
            ctx,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("lesson_notifications", Context.MODE_PRIVATE)

    private const val KEY_SCHEDULED_IDS = "scheduled_ids"
    private const val ACTION_PREFIX = "com.jetbrains.kmpapp.LESSON_ALARM."
}
