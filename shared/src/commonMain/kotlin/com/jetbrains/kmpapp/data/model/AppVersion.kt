package com.jetbrains.kmpapp.data.model

object AppVersion {
    const val VERSION_NAME = "26.9.1"
    const val BUILD_NUMBER = 32
    const val IS_CRITICAL = false
    const val MIN_SUPPORTED_BUILD = 1
    const val CHANGELOG = "Оптимизация энергопотребления на iOS и Android: устранение расхода аккумулятора при выключенном экране, поддержка режима энергосбережения и адаптивные анимации"

    const val APPLICATION_ID = "ru.l1ratch.mireaschedule"
    const val DISPLAY_VERSION = "Версия $VERSION_NAME (сборка $BUILD_NUMBER)"
    const val GITHUB_REPO = "l1ratch/MIREA-Schedule"
    const val GITHUB_REPO_URL = "https://github.com/l1ratch/MIREA-Schedule"
    const val GITHUB_ISSUES_URL = "https://github.com/l1ratch/MIREA-Schedule/issues"
    const val DEVELOPER_NAME = "l1ratch"

    const val VERSION_FEED_URL = "https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/version.json"
}
