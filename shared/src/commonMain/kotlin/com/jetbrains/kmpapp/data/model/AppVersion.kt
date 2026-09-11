package com.jetbrains.kmpapp.data.model

object AppVersion {
    /**
     * Базовая версия текущей линии разработки в формате YY.X.Z (старт: 26.0.0).
     * CI подставляет полный VERSION_NAME по каналу:
     *  - тег v26.0.0 / v26.0.1      → stable
     *  - тег v26.0.0-beta.3 / -rc.1  → beta / rc (prerelease)
     *  - push в main                 → 26.X-dev.N (rolling preview)
     *  - contributor build           → 26.X-contrib.N
     * Линия 26.1.0 = выпуск сравнения расписаний, диалога согласия и аналитики;
     * после релиза v26.1.0 поднимается до 26.2.0.
     */
    const val RELEASE_VERSION = "26.1.0"
    const val VERSION_NAME = RELEASE_VERSION

    /** stable | beta | rc | dev | contrib — подставляет CI через tools/versioning.py */
    const val BUILD_CHANNEL = "stable"

    /**
     * Числовой код сборки. CI вычисляет ОДИН раз на запуск в resolve-джобе
     * (epoch-секунды) и раздаёт всем джобам через --build-id: монотонно во
     * всех каналах, влезает в Int32 / Android versionCode (max 2147483647).
     * github.run_id и github.run_started_at НЕ подходят.
     */
    const val BUILD_NUMBER = 32
    const val COMMIT_SHA = "local"

    /** Стабильный канал обновлений (обновляется только стабильными релизами). */
    const val UPDATE_FEED_URL = "https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/version.json"

    /** Канал бета-версий (beta/rc): проверяется только если включён «Бета-канал» в настройках. */
    const val BETA_FEED_URL = "https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/beta.json"

    const val IS_CRITICAL = false
    const val MIN_SUPPORTED_BUILD = 1
    const val CHANGELOG = "Сравнение расписаний: две и более групп рядом, подсветка различий, поиск по сохранённым целям. Статистика: при первом входе диалог согласия — приложение спрашивает разрешение на анонимную статистику падений и использования разделов (включается/отключается в настройках). Запуск: без белой вспышки в тёмной теме. Док: новый состав по умолчанию — Расписание, Задачи, Карта, Другое; вкладки по-прежнему настраиваются. Мелкие исправления стабильности."

    val isTestBuild: Boolean get() = BUILD_CHANNEL != "stable"

    const val APPLICATION_ID = "ru.l1ratch.mireaschedule"
    const val DISPLAY_VERSION = "Версия $VERSION_NAME (сборка $BUILD_NUMBER)"
    const val GITHUB_REPO = "l1ratch/MIREA-Schedule"
    const val GITHUB_REPO_URL = "https://github.com/l1ratch/MIREA-Schedule"
    const val GITHUB_ISSUES_URL = "https://github.com/l1ratch/MIREA-Schedule/issues"
    const val DEVELOPER_NAME = "l1ratch"

}
