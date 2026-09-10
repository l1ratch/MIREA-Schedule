# MIREA Schedule

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-3DDC84.svg?logo=android&logoColor=white)](https://github.com/l1ratch/MIREA-Schedule/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.12.0-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-GPL_v3-blue.svg)](LICENSE)

Кроссплатформенное мобильное приложение: расписание учебных занятий, поиск свободных аудиторий и схемы корпусов РТУ МИРЭА.

> [!NOTE]
> Приложение не является официальным продуктом РТУ МИРЭА. Расписание и схемы корпусов загружаются из открытых источников университета.

**Скачать:** [APK (Android, стабильная версия)](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.apk) · [IPA (iOS, без подписи)](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.ipa) · [Все релизы](https://github.com/l1ratch/MIREA-Schedule/releases)

---

> [!CAUTION]
> Если установлена версия **26.9.0–26.9.4**: нумерация версий изменилась, автоматическое обновление не сработает. Установите свежую версию вручную: [APK](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.apk) · [IPA](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.ipa).

## Возможности

**Расписание**
- Поиск и отображение расписания групп, преподавателей и аудиторий.
- Нумерация недель семестра, чётные/нечётные недели.
- Индикация текущего занятия и прогресс до его конца.
- Сохранение нескольких расписаний с переключением между ними.
- Подсветка изменений при обновлении расписания.
- Локальный кэш: расписание доступно без сети.

**Свободные аудитории**
- Список аудиторий, свободных на выбранной паре или в интервале.
- Фильтрация по кампусам. ([API](FREE_ROOMS_API.md))

**Карты корпусов**
- Векторные схемы этажей (SVG, WebView, pan/zoom): В-78, В-86, С-20, МП-1.
- Схемы построены на официальных источниках и могут содержать неточности; не используйте их как единственный ориентир при эвакуации.

**Задачи**
- Учёт задач по предметам: категории (лабораторные, практики, домашние задания, курсовые и др.), приоритеты, статусы, чеклисты подзадач.

**Интерфейс**
- Темы: светлая, тёмная, системная.
- Настраиваемый плавающий док: порядок и видимость разделов.
- Встроенная проверка обновлений.

---

## Установка

### Android
1. Скачайте [Schedule-MIREA.apk](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.apk) из последнего стабильного релиза.
2. Установите, разрешив установку из неизвестных источников.
3. Дальше приложение проверяет обновления само и предлагает установить новый APK.

### iOS
IPA собирается без подписи (unsigned) — для установки переподпишите его любым инструментом sideload.

Источник приложений (AltStore-совместимый формат):

```
https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/apps.json
```

---

## Сборка из исходников

Требования: JDK 21, Android SDK (compileSdk 37, minSdk 24, targetSdk 37), Xcode 16+ (для iOS). Gradle 9.6.1 подключается через wrapper (`gradlew`).

```bash
git clone https://github.com/l1ratch/MIREA-Schedule.git
cd MIREA-Schedule

# Android (debug APK)
./gradlew assembleDebug
# результат: androidApp/build/outputs/apk/debug/androidApp-debug.apk

# iOS (unsigned, на macOS)
cd iosApp
xcodebuild -scheme iosApp -configuration Release -sdk iphoneos \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build
```

Тесты общего кода: `./gradlew :shared:allTests`.

Стек: Kotlin Multiplatform, Compose Multiplatform, Ktor Client, kotlinx.serialization, Room/SQLite, Multiplatform Settings.

---

## Источники данных

- **Расписание:** публичный API расписания РТУ МИРЭА.
- **Свободные аудитории:** собственная база, собираемая CI из расписания ([FREE_ROOMS_API.md](FREE_ROOMS_API.md)).
- **Карты корпусов:** официальные схемы [pulse.mirea.ru](https://pulse.mirea.ru/services/maps) и векторные схемы проекта [university-app](https://github.com/0niel/university-app) ([0niel](https://github.com/0niel)).
- Приложение не отправляет данные пользователей: сетевые запросы — только чтение (API расписания и GitHub: проверка обновлений, база свободных аудиторий, список контрибьюторов).

## Лицензия

Код проекта распространяется по [GNU GPL v3](LICENSE). 
Права на использованные данные и материалы остаются за их правообладателями: расписание и сведения о занятиях — РТУ МИРЭА; векторные схемы корпусов — [pulse.mirea.ru](https://pulse.mirea.ru/services/maps) и проект [university-app](https://github.com/0niel/university-app) ([0niel](https://github.com/0niel)).
