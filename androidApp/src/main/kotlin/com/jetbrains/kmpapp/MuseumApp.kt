package com.jetbrains.kmpapp

import android.app.Application
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider
import com.jetbrains.kmpapp.di.initKoin

class MuseumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextProvider.context = this
        initKoin()
    }
}
