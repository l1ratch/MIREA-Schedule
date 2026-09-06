# 🎓 MIREA Schedule & Campus App

<div align="center">

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-3DDC84.svg?logo=android&logoColor=white)](https://github.com/l1ratch/MIREA-Schedule)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.7.1-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Build Mobile](https://github.com/l1ratch/MIREA-Schedule/actions/workflows/build-mobile.yml/badge.svg)](https://github.com/l1ratch/MIREA-Schedule/actions/workflows/build-mobile.yml)
[![License](https://img.shields.io/badge/License-GPL_v3-blue.svg)](LICENSE)

**Современный, быстрый и автономный мобильный клиент для студентов и преподавателей РТУ МИРЭА.**  
*Расписание занятий, интерактивные векторные карты корпусов, трекер академических задач и мониторинг свободных аудиторий.*

<p align="center">
  <a href="https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.apk">
    <img src="https://img.shields.io/badge/Скачать-APK%20(Android)-2ea44f?style=for-the-badge&logo=android&logoColor=white" alt="Скачать APK" />
  </a>
  &nbsp;
  <a href="https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.ipa">
    <img src="https://img.shields.io/badge/Скачать-IPA%20(iOS)-007AFF?style=for-the-badge&logo=apple&logoColor=white" alt="Скачать IPA" />
  </a>
</p>

[📦 Все стабильные релизы](https://github.com/l1ratch/MIREA-Schedule/releases) • [🗄️ Архив сборок (Pre-release)](https://github.com/l1ratch/MIREA-Schedule/releases/tag/build-archive) • [Документация API аудиторий](FREE_ROOMS_API.md)

</div>

---

## ✨ Основные возможности

### 📅 1. Умное расписание занятий
* **Поддержка любых целей**: поиск и отображение расписания учебных групп, преподавателей и аудиторий.
* **100% Offline-First**: мгновенная загрузка из локального кэша, работа без доступа к интернету.
* **Интеллектуальный календарь**:
  * Чётные и нечётные недели с определением текущей недели семестра.
  * Индикатор текущей пары в реальном времени с прогрессом до конца занятия.
  * Цветовая индикация типов занятий (лекции, практики, лабораторные).
* **История и избранное**: сохранение нескольких расписаний с быстрым переключением между ними.
* **Diff-контроль**: автоматическое отслеживание изменений в расписании и подсветка обновлённых пар.

### 🗺️ 2. Векторные карты корпусов РТУ МИРЭА
* **Векторные схемы** ключевых кампусов университета(Могут быть неточности):
  * **В-78** (пр-т Вернадского, 78 — все 4 этажа).
  * **В-86** (пр-т Вернадского, 86 — все корпуса и этажи).
  * **С-20** (ул. Стромынка, 20 — все 4 этажа).
  * **МП-1** (ул. Малая Пироговская, 1).
* **Интерактивность**:
  * Плавное жестовое масштабирование (Pinch-to-zoom) и панорамирование на 60/120 FPS.
  * Тап по любой аудитории открывает подробную карточку с номером, этажом, корпусом и назначением.
  * Полноценная поддержка светлой и тёмной темы для всех планов этажей.

### 🏢 3. Свободные аудитории в реальном времени
* Узнавайте, какие кабинеты свободны прямо сейчас или на выбранной паре.
* Фильтрация по кампусам и временным интервалам.

### 📋 4. Академический трекер задач (Tasks)
* **Subject-Centric архитектура**: организация дедлайнов по дисциплинам с прогресс-барами («Завершено N / Всего M»).
* **Типизация задач**: Лабораторные, Практики, Домашние задания, Курсовые, Экзамены и Зачёты.
* Приоритеты, дедлайны с напоминаниями и гибкая сортировка.

### 🎨 5. Кастомизация и эргономика
* **Темы оформления**: Системная, Светлая, Тёмная и специальная пастельная тема «Цветение Сакуры» (Sakura).
* **Floating Dock (плавающая навигация)**: настройка порядка и видимости разделов приложения.

---

## 🛠️ Стек технологий и архитектура

Проект построен на современном стеке **Kotlin Multiplatform** с общим кодом для Android и iOS:

* **UI & Графика**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Material Design 3)
* **Сетевой стек**: [Ktor Client](https://ktor.io/) + ContentNegotiation
* **Сериализация**: [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
* **Асинхронность**: Kotlin Coroutines + StateFlow / SharedFlow
* **Внедрение зависимостей**: [Koin](https://insert-koin.io/)
* **Кэширование изображений**: [Coil 3](https://github.com/coil-kt/coil)
* **Картография**: Оптимизированный векторный движок на базе WebView (WKWebView на iOS / Android WebView)
* **Хранилище**: Кроссплатформенное абстрагированное локальное хранилище (`PlatformStorage`)

---

## 📲 Установка приложения

### 🤖 Android
1. Скачайте [**Schedule-MIREA.apk**](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.apk).
2. Откройте скачанный файл на устройстве и разрешите установку при запросе системы.
3. Приложение оснащено встроенным авто-обновлением: при появлении новой версии в разделе «Другое» появится уведомление.

### 🍏 iOS
Пакет [**Schedule-MIREA.ipa**](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.ipa) собирается в CI без цифровой подписи (Unsigned). Для установки на iPhone/iPad используйте любой привычный способ:
* **Без компьютера**: [SideStore](https://sidestore.io), [TrollStore](https://github.com/opa334/TrollStore) (если поддерживается вашей версией iOS) или [Scarlet](https://usescarlet.com).
* **С компьютера (Mac/PC)**: [AltStore](https://altstore.io) или [Sideloadly](https://sideloadly.io).

---

## ⚙️ Сборка и запуск

### Требования к окружению
* **JDK**: 21 (рекомендуется Azul Zulu или Eclipse Temurin)
* **Android SDK**: API level 35, Build Tools 35.0.0
* **Xcode**: 16.0+ (для сборки iOS)
* **Gradle**: 8.11+ (поставляется через `gradlew`)

### Сборка Android приложения
```bash
# Клонирование репозитория
git clone https://github.com/l1ratch/MIREA-Schedule.git
cd MIREA-Schedule

# Сборка Debug APK
./gradlew assembleDebug

# Готовый APK будет расположен в:
# androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Сборка iOS приложения
Сборка iOS доступна на macOS:
```bash
# Открытие проекта в Xcode
open iosApp/iosApp.xcodeproj

# Или сборка через командную строку (Unsigned Release)
cd iosApp
xcodebuild -scheme iosApp -configuration Release -sdk iphoneos \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build
```

---

## 🤝 Контрибьюторы и благодарности

* **Автор проекта**: [l1ratch](https://github.com/l1ratch)
* **Карты корпусов**: Векторные схемы [pulse.mirea.ru](https://pulse.mirea.ru/services/maps) и проект [university-app](https://github.com/0niel/university-app) от [0niel](https://github.com/0niel).
* **API расписания**: Сервисы [Mirea Ninja](https://mirea.ninja) и официальные API РТУ МИРЭА.

---

## 📜 Лицензия

Проект распространяется под свободной лицензией с открытым исходным кодом. Подробнее см. в файле [LICENSE](LICENSE).
