# 🔄 Переход на новую систему версионирования CalVer (YY.X.Z)

## ✅ Что уже работает (после пуша в main)

### Автоматически запустилось:
1. **`preview-main.yml`** — собирает `26.0.0-dev.N` сборку
   - Тестирует новую систему версий
   - Запускает все тесты (включая `VersionComparatorTest`)
   - Публикует rolling preview release
   - Обновляет `preview.json` и `apps-beta.json` на gh-pages

**Статус:** 🟡 В процессе (проверь: https://github.com/l1ratch/MIREA-Schedule/actions)

---

## 📋 Что делать дальше (ручные шаги)

### Шаг 1: Дождись завершения preview-main сборки
```bash
gh run list --limit 1
# Дождись статуса "completed ✓"
```

**Проверь:**
- ✅ Все тесты прошли (особенно `VersionComparatorTest`)
- ✅ APK и IPA собрались
- ✅ Rolling release `preview` обновился
- ✅ `preview.json` обновился на gh-pages

---

### Шаг 2: Создай первый стабильный релиз v26.0.0

**Вариант A: Через GitHub Actions UI**
1. Иди в: https://github.com/l1ratch/MIREA-Schedule/actions/workflows/build-mobile.yml
2. Нажми **"Run workflow"**
3. Введи тег: `v26.0.0`
4. Запусти

**Вариант B: Через тег (рекомендуется)**
```bash
# Локально
git tag -a v26.0.0 -m "Release 26.0.0 - CalVer migration"
git push origin v26.0.0
```

**Что произойдет:**
- `build-mobile.yml` соберет APK + IPA
- Создаст GitHub Release `v26.0.0` (marked latest)
- Обновит `version.json` и `apps.json` на gh-pages
- Обычные пользователи увидят обновление

---

### Шаг 3: Проверь что старая система больше не работает

**Файлы которые БОЛЬШЕ НЕ используются:**
- ❌ Хардкод версий в `AppVersion.kt` — теперь подставляет CI
- ❌ Ручное изменение `BUILD_NUMBER` — теперь epoch-секунды `github.run_started_at` (подставляет CI)
- ❌ Ручное редактирование `version.json` — обновляется только из stable релиза

**Все версии теперь через `tools/versioning.py`:**
```bash
# Проверь что все каналы работают
python tools/versioning.py resolve --tag v26.0.0 --build-id 123
python tools/versioning.py resolve --tag v26.0.0-beta.1 --build-id 124
python tools/versioning.py resolve --channel dev --run-number 10 --build-id 125
```

---

## 🔢 Схема версионирования (CalVer YY.X.Z)

### Форматы версий:
| Канал | Формат | Пример | Когда создается | Обновляет feeds |
|---|---|---|---|---|
| **stable** | `YY.X.Z` | `26.0.0`, `26.1.0` | Тег `v26.0.0` | `version.json`, `apps.json` |
| **beta** | `YY.X.Z-beta.N` | `26.0.0-beta.1` | Тег `v26.0.0-beta.1` | Нет |
| **rc** | `YY.X.Z-rc.N` | `26.0.0-rc.1` | Тег `v26.0.0-rc.1` | Нет |
| **dev** | `YY.X.Z-dev.N` | `26.0.0-dev.151` | Push в `main` | `preview.json`, `apps-beta.json` |
| **contrib** | `YY.X.Z-contrib.N` | `26.0.0-contrib.5` | Ручная сборка ветки | Нет |

### Правила инкремента:
- **Минорный релиз:** `26.0.0` → `26.1.0` (новые фичи)
- **Патч:** `26.1.0` → `26.1.1` (багфиксы)
- **Мажорный:** `26.X.X` → `27.0.0` (следующий год или breaking changes)

**После каждого стабильного релиза:**
1. Выпустил `v26.0.0` → успех!
2. Руками подними `RELEASE_VERSION` в `AppVersion.kt`: `26.0.0` → `26.1.0`
3. Закоммить: `git commit -am "chore: bump dev version to 26.1.0"`
4. Push в main → следующие dev-сборки будут `26.1.0-dev.N`

---

## 🧪 Тестирование каналов

### 1. Проверь dev-канал (уже работает)
```bash
# В отладочных настройках приложения включи "Тестовые обновления"
# Приложение начнет проверять preview.json вместо version.json
```

### 2. Проверь stable-канал (после создания v26.0.0)
```bash
# Обычные пользователи
# Приложение проверит version.json и увидит 26.0.0
```

### 3. Проверь beta-канал
```bash
# Создай тег v26.1.0-beta.1
git tag -a v26.1.0-beta.1 -m "Beta 1 for 26.1.0"
git push origin v26.1.0-beta.1
# Создастся prerelease, но НЕ обновит feeds
```

---

## 🚨 ЧТО НЕ ДЕЛАТЬ (частые ошибки)

❌ **НЕ коммить изменения в `AppVersion.kt`:**
- `VERSION_NAME`, `BUILD_NUMBER`, `BUILD_CHANNEL`, `COMMIT_SHA` подставляет CI
- Коммить только изменения `RELEASE_VERSION` и `CHANGELOG`

❌ **НЕ создавать теги вручную без push:**
```bash
# ПЛОХО:
git tag v26.0.0
# (релиз не создастся, CI не запустится)

# ХОРОШО:
git tag v26.0.0
git push origin v26.0.0
```

❌ **НЕ редактировать `version.json` вручную:**
- Обновляется ТОЛЬКО стабильным релизом через `update_version_feed.py`
- Ручное изменение перезапишется при следующем релизе

❌ **НЕ пушить в main для релиза:**
- Push в main = только dev-сборка (rolling preview)
- Стабильный релиз = только тег `v26.X.X`

---

## 📊 Мониторинг

### Проверь что все работает:
```bash
# 1. Последние запуски CI
gh run list --limit 5

# 2. Последние релизы
gh release list --limit 5

# 3. Проверь feeds на gh-pages
curl https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/version.json
curl https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/preview.json

# 4. Проверь что тесты прошли
gh run view <run_id> --log
```

---

## ✅ Чеклист миграции

- [x] Пуш в main с новой системой
- [ ] Preview-сборка завершилась успешно
- [ ] Все тесты прошли (9 тестов в VersionComparatorTest)
- [ ] Создан тег `v26.0.0` и стабильный релиз
- [ ] `version.json` обновился на gh-pages
- [ ] Приложение видит обновление до 26.0.0
- [ ] Dev-канал работает (preview.json)
- [ ] RELEASE_VERSION поднята до 26.1.0 для следующих dev-сборок

---

## 🆘 Если что-то пошло не так

### Preview-сборка упала
```bash
# Смотри логи
gh run view <run_id> --log-failed

# Частые причины:
# - Тесты не прошли → проверь VersionComparatorTest
# - Gradle ошибка → проверь shared/build.gradle.kts
# - Python ошибка → проверь tools/versioning.py
```

### Релиз не создался
```bash
# Проверь что тег правильный формат
git tag | grep v26

# Переделай тег если нужно
git tag -d v26.0.0
git push origin :refs/tags/v26.0.0
git tag -a v26.0.0 -m "Release 26.0.0"
git push origin v26.0.0
```

### version.json не обновился
```bash
# Проверь что релиз был stable (не beta/rc)
gh release view v26.0.0

# Вручную запусти update_version_feed.py
python tools/update_version_feed.py --channel stable --version 26.0.0 --build-number <BUILD> --out-dir dist_version
# Залей в gh-pages вручную
```

---

**Автор миграции:** Claude Opus 5 (Kiro AI)  
**Дата:** 2026-09-10  
**Версия документа:** 1.0
