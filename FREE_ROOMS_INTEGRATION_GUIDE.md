# Инструкция по интеграции функционала «Свободные аудитории»

Данный документ описывает формат данных, источник, модели и практический пример внедрения функции поиска свободных аудиторий в приложение **MIREA-Schedule** (Kotlin Multiplatform / Compose Multiplatform).

---

## 1. Источник данных и обновление

* **Постоянный публичный URL (CDN)**:
  ```
  https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/free_rooms.json
  ```
* **Автоматическое обновление**:
  * Каждые полмесяца (1-го и 15-го числа в 03:00 UTC) через GitHub Actions воркфлоу [`.github/workflows/sync-free-rooms.yml`](.github/workflows/sync-free-rooms.yml).
  * Воркфлоу собирает расписание по **702 аудиториям** всех кампусов, парсит iCal, рассчитывает занятые слоты на 30 дней вперед и коммитит результат в ветку `gh-pages`.
  * Размер файла: ~150–200 КБ (скачивается за доли секунды и кэшируется локально).

---

## 2. Структура файла `free_rooms.json`

```json
{
  "updatedAt": "2026-09-05T17:17:55.449889+00:00",
  "dateRange": {
    "start": "2026-09-05",
    "end": "2026-10-05"
  },
  "campuses": ["В-78", "В-86", "С-20", "СГ-22", "МП-1"],
  "bellSlots": [
    { "bell": 1, "start": "09:00", "end": "10:30" },
    { "bell": 2, "start": "10:40", "end": "12:10" },
    { "bell": 3, "start": "12:40", "end": "14:10" },
    { "bell": 4, "start": "14:20", "end": "15:50" },
    { "bell": 5, "start": "16:20", "end": "17:50" },
    { "bell": 6, "start": "18:00", "end": "19:30" },
    { "bell": 7, "start": "19:40", "end": "21:10" }
  ],
  "rooms": [
    {
      "id": 66,
      "name": "А-1",
      "fullTitle": "А-1 (В-78)",
      "campus": "В-78",
      "floor": 1,
      "busy": {
        "2026-09-07": [1, 2],
        "2026-09-08": [3, 4]
      }
    }
  ]
}
```

### Пояснение полей:
* `campuses` — список основных кампусов РТУ МИРЭА.
* `bellSlots` — временные рамки стандартных пар (1–7 звонки).
* `rooms[].name` — чистое название кабинета (`349`, `А-1`, `287`).
* `rooms[].campus` — кампус расположения (`В-78`, `В-86`, `С-20`, `СГ-22`, `МП-1`).
* `rooms[].floor` — вычисленный этаж (например, `349` $\to$ `3`, `1024` $\to$ `10`, `А-1` $\to$ `1`). Если этаж не распознан, значение `null`.
* `rooms[].busy` — словарь, где ключ — дата (`"YYYY-MM-DD"`), а значение — массив **номеров занятых пар** (`[1, 2]`).
  * Если номер пары **отсутствует** в списке — аудитория в эту пару **свободна**!
  * Если для даты вообще нет ключа в `busy` — аудитория **свободна весь день**.

---

## 3. Kotlin Модели данных (`shared/.../model`)

```kotlin
package com.jetbrains.kmpapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FreeRoomsData(
    val updatedAt: String = "",
    val dateRange: FreeRoomsDateRange? = null,
    val campuses: List<String> = emptyList(),
    val bellSlots: List<FreeRoomBellSlot> = emptyList(),
    val rooms: List<FreeRoomItem> = emptyList()
)

@Serializable
data class FreeRoomsDateRange(
    val start: String,
    val end: String
)

@Serializable
data class FreeRoomBellSlot(
    val bell: Int,
    val start: String,
    val end: String
)

@Serializable
data class FreeRoomItem(
    val id: Int,
    val name: String,
    val fullTitle: String,
    val campus: String,
    val floor: Int? = null,
    val busy: Map<String, List<Int>> = emptyMap()
) {
    /**
     * Проверяет, свободна ли аудитория на конкретную дату и пару.
     */
    fun isFreeAt(dateIso: String, bellNumber: Int): Boolean {
        val busyBells = busy[dateIso] ?: return true
        return bellNumber !in busyBells
    }

    /**
     * Возвращает список свободных пар аудитории на указанный день (от 1 до 7).
     */
    fun getFreeBells(dateIso: String, maxBells: Int = 7): List<Int> {
        val busyBells = busy[dateIso]?.toSet() ?: emptySet()
        return (1..maxBells).filter { it !in busyBells }
    }
}
```

---

## 4. Репозиторий и кэширование (`FreeRoomsRepository`)

```kotlin
package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.model.FreeRoomsData
import com.jetbrains.kmpapp.data.model.FreeRoomItem
import com.jetbrains.kmpapp.data.storage.PlatformStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.Json

class FreeRoomsRepository(
    private val client: HttpClient,
    private val storage: PlatformStorage
) {
    private val cdnUrl = "https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/free_rooms.json"
    private val cacheKey = "cached_free_rooms_json"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Получение данных: сначала из памяти/кэша, при наличии интернета — фоновое обновление.
     */
    suspend fun getFreeRooms(forceRefresh: Boolean = false): FreeRoomsData {
        if (!forceRefresh) {
            val cached = storage.getString(cacheKey)
            if (!cached.isNullOrEmpty()) {
                try {
                    return json.decodeFromString<FreeRoomsData>(cached)
                } catch (_: Exception) {}
            }
        }

        return try {
            val responseText: String = client.get(cdnUrl).body()
            storage.putString(cacheKey, responseText)
            json.decodeFromString<FreeRoomsData>(responseText)
        } catch (e: Exception) {
            val fallback = storage.getString(cacheKey)
            if (!fallback.isNullOrEmpty()) {
                json.decodeFromString<FreeRoomsData>(fallback)
            } else {
                FreeRoomsData()
            }
        }
    }

    /**
     * Фильтрация свободных аудиторий по 4 критериям.
     */
    fun filterFreeRooms(
        allRooms: List<FreeRoomItem>,
        campus: String,
        floor: Int?,
        dateIso: String,
        bellNumber: Int,
        searchQuery: String = ""
    ): List<FreeRoomItem> {
        val query = searchQuery.trim().lowercase()
        return allRooms.filter { room ->
            val matchCampus = room.campus.equals(campus, ignoreCase = true)
            val matchFloor = floor == null || room.floor == floor
            val matchFree = room.isFreeAt(dateIso, bellNumber)
            val matchQuery = query.isEmpty() || room.name.lowercase().contains(query)
            matchCampus && matchFloor && matchFree && matchQuery
        }
    }
}
```

---

## 5. Алгоритм «Свободно прямо сейчас»

Для определения текущей пары в приложении:

```kotlin
fun getCurrentBellNumber(currentTimeHHMM: String): Int {
    return when {
        currentTimeHHMM < "10:35" -> 1 // 09:00 - 10:30
        currentTimeHHMM < "12:20" -> 2 // 10:40 - 12:10
        currentTimeHHMM < "14:15" -> 3 // 12:40 - 14:10
        currentTimeHHMM < "16:00" -> 4 // 14:20 - 15:50
        currentTimeHHMM < "18:00" -> 5 // 16:20 - 17:50
        currentTimeHHMM < "19:35" -> 6 // 18:00 - 19:30
        currentTimeHHMM < "21:15" -> 7 // 19:40 - 21:10
        else -> 1 // вне учебного времени или до начала занятий
    }
}
```

---

## 6. Рекомендации по UI (Compose Multiplatform)

1. **Верхний фильтр**:
   * Горизонтальный скролл чипов **Кампуса**: `В-78` (активен по умолчанию), `В-86`, `С-20`, `СГ-22`, `МП-1`.
   * Горизонтальный скролл чипов **Этажей**: `Все`, `1 этаж`, `2 этаж`, `3 этаж` и т.д.
2. **Селектор времени**:
   * Чипы пар: `Сейчас (2 пара)`, `1 пара`, `2 пара`... `7 пара`.
3. **Список аудиторий**:
   * `LazyVerticalGrid(columns = GridCells.Adaptive(110.dp))`.
   * Каждая карточка аудитории отображает:
     * Номер кабинета крупно (например, **349** или **И-212-г**).
     * Бейдж этажа (`3 этаж`).
     * Статус: «Свободно до 14:10» (если следующая пара занята) или «Свободно до конца дня».
