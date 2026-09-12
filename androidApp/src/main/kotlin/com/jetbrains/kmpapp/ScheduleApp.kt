package com.jetbrains.kmpapp

import android.app.Application
import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.jetbrains.kmpapp.data.analytics.AndroidAnalytics
import com.jetbrains.kmpapp.data.analytics.AppAnalytics
import com.jetbrains.kmpapp.data.notifications.NotificationsManager
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider
import com.jetbrains.kmpapp.di.initKoin
import com.jetbrains.kmpapp.notifications.AndroidNotificationsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextProvider.context = this
        initKoin()
        AppAnalytics.setEngine(AndroidAnalytics())
        // Движок напоминаний: секция «Уведомления» в настройках видна там,
        // где движок зарегистрирован (Android и iOS симметричны).
        NotificationsManager.setEngine(AndroidNotificationsEngine)

        // Pre-warm Android InputMethodManager and Compose text classes on main thread idle
        android.os.Looper.myQueue().addIdleHandler {
            try {
                getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                Class.forName("androidx.compose.ui.text.input.TextInputServiceAndroid")
                Class.forName("androidx.compose.foundation.text.selection.TextFieldSelectionManager")
                Class.forName("androidx.compose.ui.text.platform.AndroidParagraphHelper_androidKt")
                Class.forName("androidx.compose.foundation.text.BasicTextFieldKt")
                Class.forName("androidx.compose.material3.OutlinedTextFieldKt")
                Class.forName("androidx.compose.material3.TextFieldDefaults")
            } catch (_: Throwable) {}
            false // Run once
        }

        // Also pre-warm background reflection classes
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Class.forName("androidx.compose.foundation.text.input.internal.LegacyPlatformTextInputServiceAdapter")
                Class.forName("androidx.compose.ui.text.input.EditProcessor")
            } catch (_: Throwable) {}
        }
    }
}
