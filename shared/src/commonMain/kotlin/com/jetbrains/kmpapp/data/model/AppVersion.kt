package com.jetbrains.kmpapp.data.model

object AppVersion {
    const val VERSION_NAME = "26.9.3"
    const val BUILD_NUMBER = 34
    const val IS_CRITICAL = false
    const val MIN_SUPPORTED_BUILD = 1
    const val CHANGELOG = "Расписание: индикатор оставшегося времени и прогресса текущей пары, магнитная автопрокрутка к текущему занятию или перемене при открытии, настройки в приложении."

    val isTestBuild: Boolean get() = BUILD_NUMBER >= 900000 || VERSION_NAME.contains("-test")

    const val APPLICATION_ID = "ru.l1ratch.mireaschedule"
    const val DISPLAY_VERSION = "Версия $VERSION_NAME (сборка $BUILD_NUMBER)"
    const val GITHUB_REPO = "l1ratch/MIREA-Schedule"
    const val GITHUB_REPO_URL = "https://github.com/l1ratch/MIREA-Schedule"
    const val GITHUB_ISSUES_URL = "https://github.com/l1ratch/MIREA-Schedule/issues"
    const val DEVELOPER_NAME = "l1ratch"

    const val VERSION_FEED_URL = "https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/version.json"
}
