package com.jetbrains.kmpapp

import android.app.Application
import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider
import com.jetbrains.kmpapp.di.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MuseumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextProvider.context = this
        initKoin()

        // Pre-warm Android InputMethodManager and text classes to eliminate first-tap keyboard lag
        CoroutineScope(Dispatchers.Default).launch {
            try {
                getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                Class.forName("androidx.compose.ui.text.input.TextInputServiceAndroid")
                Class.forName("androidx.compose.foundation.text.BasicTextFieldKt")
                Class.forName("androidx.compose.ui.text.platform.AndroidParagraphHelper_androidKt")
            } catch (_: Throwable) {}
        }
    }
}
