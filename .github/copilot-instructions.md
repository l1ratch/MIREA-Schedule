# GitHub Copilot Instructions for MIREA-Schedule

## О проекте
**MIREA-Schedule** — кроссплатформенное мобильное приложение расписания для студентов и преподавателей РТУ МИРЭА на базе **Kotlin Multiplatform (KMP)** и **Compose Multiplatform** для Android и iOS.

## Архитектура и технологии
- **Язык**: Kotlin 2.x
- **UI Framework**: Compose Multiplatform (Material 3)
- **Dependency Injection**: Koin 4.x (`koinViewModel()`, `koinInject()`, `singleOf`, `factoryOf`)
- **Сетевой стек**: Ktor 3.x Client (OkHttp engine на Android, Darwin engine на iOS)
- **Сериализация**: `kotlinx.serialization` (JSON с `ignoreUnknownKeys = true`)
- **Работа с датами**: `kotlinx.datetime` и `kotlin.time.Clock`
- **Асинхронность**: Kotlin Coroutines & StateFlow / SharedFlow
- **Хранилище**: `PlatformStorage` (`SharedPreferences` на Android, `NSUserDefaults` на iOS)

## Структура кодовой базы
- `shared/src/commonMain/kotlin/com/jetbrains/kmpapp/`:
  - `data/`: Репозитории (`ScheduleRepository`, `TaskRepository`, `FreeRoomsRepository`), API (`MireaScheduleApi`), парсер iCal (`MireaICalParser`), модели данных (`Lesson`, `StudyTask`, `Subject`, `ScheduleTarget`), система кэширования (`UnifiedSyncManager`).
  - `screens/`:
    - `schedule/`: Главный экран расписания, карточки занятий (`LessonCard`), детальный просмотр (`LessonDetailScreen`), миникалендарь (`WeekCalendarStrip`).
    - `tasks/`: Трекер учебных задач, конструктор предметов, дедлайны.
    - `rooms/`: Поиск свободных аудиторий по кампусам и этажам с интерактивным выбором даты.
    - `map/`: Карта корпусов кампусов.
    - `other/`: Настройки дока, выбор тем оформления, о программе, статистика памяти.
    - `components/`: Плавающий док (`FloatingDock`), свайп назад (`PlatformBackHandler`).
- `androidApp/`: Точка входа Android (`ScheduleApp.kt`, `MainActivity.kt`), конфигурация сборки и подписи.
- `iosApp/`: Xcode-проект и запуск iOS-приложения.

## Правила разработки и решения Issue
1. **Безопасность потоков и старта**:
   - Все сетевые запросы и парсинг файлов ДОЛЖНЫ выполняться строго на `Dispatchers.IO` или `Dispatchers.Default`.
   - В конструкторах и блоках `init` ViewModels ЗАПРЕЩЕНО выполнять блокирующие сетевые запросы.
   - Любые операции с хранилищем и парсингом должны быть защищены `try-catch (_: Throwable)`.
2. **UI и дизайн**:
   - Поддержка темного и светлого оформления Material 3 + секретная тема Сакуры.
   - Корректные отступы под системные панели (WindowInsets: `navigationBarsPadding()`, `statusBarsPadding()`).
3. **Локализация**:
   - Весь интерфейс и текстовые сообщения пользователю составляются на русском языке.
