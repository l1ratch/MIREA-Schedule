# MIREA Schedule

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-3DDC84.svg?logo=android&logoColor=white)](https://github.com/l1ratch/MIREA-Schedule/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.12.0-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-GPL_v3-blue.svg)](LICENSE)

Кроссплатформенное мобильное приложение для просмотра расписания учебных занятий, поиска свободных аудиторий и навигации по корпусам РТУ МИРЭА.

> **Проект неофициальный.** Не аффилирован с РТУ МИРЭА и не одобрен университетом. Название вуза и все данные (расписание, схемы корпусов) принадлежат их правообладателям; приложение только отображает их и ссылается на официальные источники.

**Скачать:** [APK (Android, стабильная)](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.apk) · [IPA (iOS, unsigned)](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.ipa) · [Все релизы](https://github.com/l1ratch/MIREA-Schedule/releases)

---

## Возможности

**Расписание**
- Поиск и отображение расписания групп, преподавателей и аудиторий.
- Нумерация недель семестра, чётные/нечётные недели.
- Индикация текущего занятия и прогресса до его конца.
- Сохранение нескольких расписаний с переключением между ними.
- Подсветка изменений при обновлении расписания.
- Локальный кеш: расписание доступно без сети.

**Свободные аудитории**
- Список аудиторий, свободных на выбранной паре или в интервале.
- Фильтрация по кампусам. ([API](FREE_ROOMS_API.md))

**Карты корпусов**
- Векторные схемы этажей (SVG, WebView, pan/zoom): В-78, В-86, С-20, МП-1.
- Схемы построены на официальных источниках и **могут содержать неточности**; не используйте их как единственный ориентир при эвакуации.

**Задачи**
- Учёт задач по предметам: категории (лабораторные, практики, домашние задания, курсовые и др.), приоритеты, статусы, подзадачи.
- Генератор списка лабораторных работ.

**Интерфейс**
- Темы: светлая, тёмная, системная.
- Настраиваемый плавающий док (порядок и видимость разделов).
- Встроенная проверка обновлений.

---

## Установка

### Android
1. Скачайте [Schedule-MIREA.apk](https://github.com/l1ratch/MIREA-Schedule/releases/latest/download/Schedule-MIREA.apk) (последний стабильный релиз).
2. Установите, разрешив установку из неизвестных источников.
3. Обновления: приложение проверяет их само и предлагает скачать новый APK.

### iOS
IPA собирается без подписи (unsigned). Установка — сторонними инструментами, например: AltStore, SideStore, GBox, Sideloadly. Инструмент подписывает пакет собственным сертификентом.

**Источники приложений (AltStore-совместимый формат, для GBox и аналогов):**

| Источник | Содержимое |
|---|---|
| `https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/apps.json` | Стабильные релизы |
| `https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/apps-beta.json` | Последняя сборка любого канала (dev/beta/rc/stable) |

Подключайте один источник за раз: `bundleIdentifier` у обоих одинаковый.

---

## Каналы обновлений

| Канал | Файл фида | Проверяет приложение |
|---|---|---|
| Стабильный | `version.json` | всегда, все пользователи |
| Бета / RC | `beta.json` | только при включённом тумблере «Бета-канал обновлений» в настройках |
| Dev (сборки из `main`) | — | не проверяется приложением; доступен через GBox-источник выше |

Dev-сборки публикуются в rolling-релизе [`preview`](https://github.com/l1ratch/MIREA-Schedule/releases/tag/preview).

## Версионирование

Формат **CalVer**: `YY.RELEASE.PATCH` — год, номер релиза, патч (например `26.0.0`, `26.0.1`).

Каналы сборки: `26.0.0` (стабильный), `26.0.0-beta.N`, `26.0.0-rc.N`, `26.0.0-dev.N` (тестовые сборки `main`), `26.0.0-contrib.N` (сборки контрибьюторов, не публикуются).

> **Для установок со старой нумерацией (26.9.1 и ниже):** сравнение версий изменилось, приложение предложит обновление только до более нового номера. Если обновление не предлагается, установите стабильную версию вручную по ссылке выше.

---

## Сборка из исходников

Требования: JDK 21, Android SDK (compileSdk 37, minSdk 24, targetSdk 37), Xcode 16+ (для iOS). Gradle 9.6.1 поставляется через wrapper.

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

Тесты (общий код): `./gradlew :shared:allTests`.

Стек: Kotlin Multiplatform, Compose Multiplatform, Ktor Client, kotlinx.serialization, Room/SQLite, Multiplatform Settings.

## CI/CD

- Push в `main` → тестовая dev-сборка (`26.0.0-dev.N`, rolling-релиз `preview`).
- Тег `v26.0.0` → стабильный релиз; `v26.0.0-beta.N` / `-rc.N` → иммутабельный prerelease.
- Теги `v*` и только они обновляют стабильный фид обновлений. Подробности: [CI_GUIDE.md](.github/CI_GUIDE.md).

---

## Источники данных и права

- **Расписание:** публичный API РТУ МИРЭА. Данные принадлежат университету.
- **Карты корпусов:** на основе официальных схем [pulse.mirea.ru](https://pulse.mirea.ru/services/maps) и векторных схем проекта [university-app](https://github.com/0niel/university-app) ([0niel](https://github.com/0niel)).
- Приложение не отправляет данные пользователей: сетевые запросы — только чтение, к API университета (`schedule-of.mirea.ru`) и GitHub (фиды обновлений, база свободных аудиторий, список контрибьюторов).

## Лицензия

Код распространяется по [GNU GPL v3](LICENSE). На данные РТУ МИРЭА и использованные схемы корпусов действие лицензии не распространяется — они остаются собственностью их правообладателей и используются здесь в информационных целях со ссылкой на источник.
