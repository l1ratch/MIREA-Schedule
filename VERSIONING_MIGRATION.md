# 🔄 Система версионирования CalVer (YY.X.Z) — справочник

> Миграция со старой схемы (26.9.1, хардкод версий) завершена.
> Полные правила для агентов: `.agents/VERSIONING.md` (локально).

## Схема версий

| Канал | Формат | Пример | Создаётся | Фиды |
|---|---|---|---|---|
| **stable** | `YY.X.Z` | `26.0.0` | тег `v26.0.0` | `version.json`, `apps.json`, `apps-beta.json` |
| **beta** | `YY.X.Z-beta.N` | `26.0.0-beta.1` | тег `v26.0.0-beta.1` | `beta.json`, `apps-beta.json` |
| **rc** | `YY.X.Z-rc.N` | `26.0.0-rc.1` | тег `v26.0.0-rc.1` | `beta.json`, `apps-beta.json` |
| **dev** | `YY.X.Z-dev.N` | `26.0.0-dev.151` | push в `main` | только `apps-beta.json` |
| **contrib** | `YY.X.Z-contrib.N` | `26.0.0-contrib.5` | ручной workflow | ничего (только Artifacts) |

- `BUILD_NUMBER` (versionCode/CFBundleVersion) = **epoch-секунды начала запуска**
  (`github.run_started_at`, подставляет `tools/versioning.py --build-time`).
  Монотонный, влезает в Int32/Android versionCode. `github.run_id` НЕЛЬЗЯ (~34e9 > Int32).
- В репо хранится ТОЛЬКО `AppVersion.RELEASE_VERSION` (линия разработки).
  После стабильного релиза `v26.0.0` подними её до `26.1.0`.

## Фиды на gh-pages (генерирует `tools/update_version_feed.py`)

| Файл | Кто пишет | Кто читает |
|---|---|---|
| `version.json` | только stable | все обычные пользователи (автообновление) |
| `beta.json` | только beta/rc теги | приложение с включённым «Бета-каналом» в настройках |
| `apps.json` | только stable | GBox/AltStore-клиенты (стабильный источник) |
| `apps-beta.json` | **каждая** сборка (dev/beta/rc/stable) | GBox-источник «всегда последняя сборка» — цикл «собрал → обновил → протестил» |

Dev-сборки приложение **не проверяет и не видит** — только GBox через `apps-beta.json`.
В `apps-beta.json` в description дублируются версия и дата/время обновления.

GBox-источники:
- Стабильный: `https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/apps.json`
- Бета (rolling): `https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/apps-beta.json`
- Подключай **один** за раз (bundle id одинаковый).

## Как выпустить

| Что | Как |
|---|---|
| dev-сборка | `git push origin main` (при изменении кода приложения) |
| бета | `git tag -a v26.0.0-beta.1 -m "..." && git push origin v26.0.0-beta.1` |
| rc | `git tag -a v26.0.0-rc.1 -m "..." && git push origin v26.0.0-rc.1` |
| стабильный релиз | `git tag -a v26.0.0 -m "..." && git push origin v26.0.0` |
| contrib-сборка | Actions → `🧪 Contributor Test Build` → Run workflow → ветка |

## Скрипты (вызываются только из CI)

| Скрипт | Роль |
|---|---|
| `tools/versioning.py` | единственный генератор версий: `resolve` (печать) / `prepare` (патч `AppVersion.kt`, `build.gradle.kts`, `Info.plist`) |
| `tools/publish_release.py` | GitHub Releases: `preview` → rolling-замена файлов; stable/beta/rc → иммутабельно (повтор = ошибка) |
| `tools/update_version_feed.py` | фиды + GBox-источники на gh-pages (см. таблицу выше) |

## Проверка что всё живо

```bash
gh run list --limit 5                                  # сборки
gh release list --limit 5                              # релизы
curl .../gh-pages/version.json                         # стабильный фид
curl .../gh-pages/beta.json                            # бета-фид
curl .../gh-pages/apps-beta.json                       # GBox rolling-источник
```

## ⚠️ Критичные правила

- ❌ НЕ коммить изменения `VERSION_NAME`/`BUILD_NUMBER`/`BUILD_CHANNEL`/`COMMIT_SHA` — их патчит CI.
- ❌ НЕ используй `github.run_id` как BUILD_NUMBER — превышает Int32.
- ❌ НЕ редактируй фиды руками — только CI.
- ❌ НЕ пушь в main как «релиз» — main = только dev-сборка.
- ✅ Сравнение версий — только `VersionComparator` (+ `VersionComparatorTest` в CI).
