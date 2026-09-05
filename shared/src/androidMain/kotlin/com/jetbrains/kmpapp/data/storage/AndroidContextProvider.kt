package com.jetbrains.kmpapp.data.storage

import android.content.Context

object AndroidContextProvider {
    var context: Context? = null
        get() {
            if (field != null) return field
            try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val method = activityThreadClass.getMethod("currentApplication")
                val app = method.invoke(null) as? Context
                if (app != null) {
                    field = app
                    return app
                }
            } catch (_: Throwable) {}
            return null
        }
}
