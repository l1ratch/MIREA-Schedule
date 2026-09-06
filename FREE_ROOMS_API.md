# Спецификация API «Свободные аудитории» РТУ МИРЭА

Документация и техническое описание открытого API свободных аудиторий кампусов РТУ МИРЭА.

---

## 1. Общие сведения и архитектура

API предоставляет актуальную матрицу занятости и доступности учебных аудиторий по всем ключевым корпусам университета на 30 дней вперед.

- **Формат данных**: JSON (UTF-8, gzip-совместимый)
- **Точка доступа (CDN)**:
  ```http
  GET https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/free_rooms.json
  ```
  *(Зеркало GitHub Pages: `https://l1ratch.github.io/MIREA-Schedule/free_rooms.json`)*
- **Периодичность обновления**:
  - Автоматически 1-го и 15-го числа каждого месяца в 03:00 UTC (06:00 МСК) через GitHub Actions ([`.github/workflows/sync-free-rooms.yml`](.github/workflows/sync-free-rooms.yml)).
  - Охватывает более **700 аудиторий** пяти кампусов.
  - Средний размер payload: ~160–210 КБ.

---

## 2. Схема данных (`free_rooms.json`)

### Пример ответа
```json
{
  "updatedAt": "2026-09-05T17:17:55.449889+00:00",
  "dateRange": {
    "start": "2026-09-05",
    "end": "2026-10-05"
  },
  "campuses": [
    "В-78",
    "В-86",
    "С-20",
    "СГ-22",
    "МП-1"
  ],
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

### Описание полей

| Поле | Тип | Описание |
| :--- | :--- | :--- |
| `updatedAt` | `String` (ISO 8601) | Временная метка генерации набора данных |
| `dateRange` | `Object` | Диапазон дат (`start` / `end`), покрываемый выгрузкой |
| `campuses` | `Array<String>` | Доступные идентификаторы кампусов: `В-78`, `В-86`, `С-20`, `СГ-22`, `МП-1` |
| `bellSlots` | `Array<Object>` | Расписание звонков пар (`bell`: номер пары, `start`: время начала, `end`: время окончания) |
| `rooms` | `Array<Object>` | Реестр аудиторий и их график занятости |
| `rooms[].id` | `Int` | Уникальный числовой идентификатор аудитории в базе |
| `rooms[].name` | `String` | Номер/название аудитории (`А-1`, `214`, `349`) |
| `rooms[].fullTitle` | `String` | Полное отображаемое наименование с кампусом |
| `rooms[].campus` | `String` | Кампус расположения аудитории |
| `rooms[].floor` | `Int?` | Этаж расположения (`1`, `2`..), либо `null`, если этаж не определен |
| `rooms[].busy` | `Map<String, List<Int>>` | Словарь занятости: ключ — дата `"YYYY-MM-DD"`, значение — массив занятых пар (`[1, 2]`) |

---

## 3. Логика определения свободности аудитории

1. **Проверка конкретной пары**:
   - Если для даты `D` в `busy[D]` **нет** номера пары `B` (или даты `D` вообще нет в словаре `busy`) — аудитория **свободна** в пару `B`.
2. **Проверка на весь день**:
   - Аудитория свободна весь день, если ключ `D` отсутствует в `busy` или массив `busy[D]` пуст.
3. **Воскресенье**:
   - В воскресенье регулярные занятия не проводятся, все аудитории считаются свободными.

---

## 4. Клиентская интеграция (Kotlin Serialization)

```kotlin
@Serializable
data class FreeRoomsData(
    val updatedAt: String = "",
    val campuses: List<String> = emptyList(),
    val bellSlots: List<BellSlot> = emptyList(),
    val rooms: List<FreeRoomItem> = emptyList()
)

@Serializable
data class BellSlot(
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
    fun isFreeAt(dateIso: String, bell: Int): Boolean {
        val busyBells = busy[dateIso] ?: return true
        return bell !in busyBells
    }
}
```
