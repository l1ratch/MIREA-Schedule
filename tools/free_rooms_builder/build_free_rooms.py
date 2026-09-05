"""
Скрипт выгрузки расписаний аудиторий РТУ МИРЭА и построения матрицы занятости/свободных аудиторий.
Группирует данные:
  - по кампусам (В-78, В-86, С-20, СГ-22, МП-1 и др.)
  - по этажам (вычисляются по первой цифре/номеру аудитории)
  - по дням (на ближайший месяц / полмесяца)
  - по звонкам / парам (1-7 пары)
"""

import sys
import os
import re
import json
import time
import datetime
import urllib.request
import urllib.parse
import urllib.error
import concurrent.futures
from typing import Dict, List, Optional, Tuple, Set

# Настройки консоли для UTF-8
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_API_URL = "https://schedule-of.mirea.ru/schedule/api"
USER_AGENT = "university-app-schedule-fetcher/0.1"

CAMPUSES_ORDER = ["В-78", "В-86", "С-20", "СГ-22", "МП-1"]

BELL_SLOTS = [
    {"bell": 1, "start": "09:00", "end": "10:30"},
    {"bell": 2, "start": "10:40", "end": "12:10"},
    {"bell": 3, "start": "12:40", "end": "14:10"},
    {"bell": 4, "start": "14:20", "end": "15:50"},
    {"bell": 5, "start": "16:20", "end": "17:50"},
    {"bell": 6, "start": "18:00", "end": "19:30"},
    {"bell": 7, "start": "19:40", "end": "21:10"},
]

def parse_proxy_url(raw_proxy: str) -> Optional[str]:
    """
    Принимает строку прокси любого вида:
      - 'ip:port:user:pass' -> 'http://user:pass@ip:port'
      - 'user:pass@ip:port' -> 'http://user:pass@ip:port'
      - 'ip:port' -> 'http://ip:port'
      - 'http://user:pass@ip:port'
    """
    if not raw_proxy:
        return None
    s = raw_proxy.strip()
    if not s:
        return None
    if "://" in s:
        return s
    parts = s.split(":")
    if len(parts) == 4:
        ip, port, user, pwd = parts
        return f"http://{user}:{pwd}@{ip}:{port}"
    elif len(parts) == 2:
        ip, port = parts
        return f"http://{ip}:{port}"
    return f"http://{s}"

def setup_global_opener():
    """Настраивает urllib opener с поддержкой RU_PROXY при наличии."""
    raw_proxy = os.environ.get("RU_PROXY") or os.environ.get("HTTPS_PROXY") or os.environ.get("HTTP_PROXY")
    proxy_url = parse_proxy_url(raw_proxy)
    if proxy_url:
        masked = re.sub(r':([^:@]+)@', ':****@', proxy_url)
        print(f"[Proxy] Использование прокси: {masked}")
        proxy_handler = urllib.request.ProxyHandler({
            "http": proxy_url,
            "https": proxy_url,
        })
        opener = urllib.request.build_opener(proxy_handler)
        urllib.request.install_opener(opener)
    else:
        print("[Proxy] Прокси не задан, прямые сетевые запросы.")

def get_bell_number(hhmm: str) -> int:
    """Определение номера пары по времени начала занятия."""
    if hhmm < "10:35":
        return 1
    elif hhmm < "12:20":
        return 2
    elif hhmm < "14:15":
        return 3
    elif hhmm < "16:00":
        return 4
    elif hhmm < "18:00":
        return 5
    elif hhmm < "19:35":
        return 6
    else:
        return 7

def extract_floor_and_campus(full_title: str) -> Tuple[str, str, Optional[int]]:
    """
    Извлекает название кампуса, чистое имя аудитории и номер этажа.
    Примеры:
      'А-1 (В-78)' -> ('В-78', 'А-1', 1)
      '349 (В-78)' -> ('В-78', '349', 3)
      '287 (С-20)' -> ('С-20', '287', 2)
      'И-212-г (В-78)' -> ('В-78', 'И-212-г', 2)
      '1024 (В-78)' -> ('В-78', '1024', 10)
    """
    campus_match = re.search(r'\(([^)]+)\)', full_title)
    campus = campus_match.group(1).strip() if campus_match else "Неизвестно"
    
    # Очищаем имя от кампуса в скобках
    room_name = re.sub(r'\s*\([^)]*\)', '', full_title).strip()
    
    # Ищем последовательность цифр в названии
    m = re.search(r'\d+', room_name)
    floor = None
    if m:
        digits = m.group(0)
        if len(digits) >= 3:
            # 349 -> 3 этаж, 212 -> 2 этаж, 1024 -> 10 этаж
            floor = int(digits[:-2]) if len(digits) > 3 else int(digits[0])
        elif len(digits) in (1, 2):
            floor = 1
        elif len(digits) == 0:
            floor = 1

    return campus, room_name, floor

def discover_all_classrooms() -> List[Dict]:
    """
    Находит все уникальные аудитории через поиск по ключевым символам.
    """
    search_queries = (
        CAMPUSES_ORDER + 
        [chr(c) for c in range(ord('А'), ord('Я') + 1)] + 
        [str(i) for i in range(10)]
    )
    
    rooms_by_id = {}
    print(f"Поиск аудиторий по {len(search_queries)} шаблонам...")

    last_err = None
    for q in search_queries:
        encoded_q = urllib.parse.quote(q)
        url = f"{BASE_API_URL}/search?match={encoded_q}&limit=100"
        req = urllib.request.Request(url, headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/json"
        })
        try:
            with urllib.request.urlopen(req, timeout=12) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                for item in data.get("data", []):
                    # scheduleTarget 3 означает Auditorium
                    if item.get("scheduleTarget") == 3:
                        rid = item["id"]
                        if rid not in rooms_by_id:
                            rooms_by_id[rid] = item
        except Exception as e:
            last_err = e
            continue

    if not rooms_by_id and last_err:
        print(f"[Внимание] Ошибка при поиске аудиторий: {last_err}")

    rooms_list = list(rooms_by_id.values())
    print(f"Найдено уникальных аудиторий: {len(rooms_list)}")
    return rooms_list

def unfold_ical_lines(raw_ical: str) -> List[str]:
    """Развертывает строки iCal согласно RFC 5545 (строки, начинающиеся с пробела/табуляции)."""
    unfolded = []
    for line in raw_ical.splitlines():
        if (line.startswith(" ") or line.startswith("\t")) and unfolded:
            unfolded[-1] += line[1:]
        else:
            unfolded.append(line)
    return unfolded

def parse_room_ical(raw_ical: str, start_range: datetime.date, end_range: datetime.date) -> Dict[str, List[int]]:
    """
    Парсит iCal расписание кабинета и возвращает словарь:
    { 'YYYY-MM-DD': [1, 2, ...] } со списком занятых номеров пар.
    """
    unfolded = unfold_ical_lines(raw_ical)
    busy: Dict[str, Set[int]] = {}
    in_event = False
    props: Dict[str, str] = {}

    for line in unfolded:
        line_s = line.strip()
        if line_s == "BEGIN:VEVENT":
            in_event = True
            props = {}
        elif line_s == "END:VEVENT":
            in_event = False
            summary = props.get("SUMMARY", "")
            if summary.endswith("неделя") or "дистанцион" in summary.lower():
                continue
            loc = props.get("LOCATION", "")
            if "дистанцион" in loc.lower():
                continue

            dtstart = props.get("DTSTART", "")
            if "T" not in dtstart:
                continue

            t_idx = dtstart.index("T")
            start_d_str = dtstart[t_idx-8:t_idx]
            start_t_str = f"{dtstart[t_idx+1:t_idx+3]}:{dtstart[t_idx+3:t_idx+5]}"
            bell = get_bell_number(start_t_str)

            try:
                event_date = datetime.date(int(start_d_str[:4]), int(start_d_str[4:6]), int(start_d_str[6:8]))
            except Exception:
                continue

            rrule = props.get("RRULE", "")
            exdate_raw = props.get("EXDATE", "")
            exdates = set()
            if exdate_raw:
                for part in exdate_raw.split(","):
                    if "T" in part:
                        idx = part.index("T")
                        exdates.add(part[idx-8:idx])
                    elif len(part) >= 8:
                        exdates.add(part[:8])

            if rrule:
                interval = 1
                for part in rrule.split(";"):
                    if part.startswith("INTERVAL="):
                        try:
                            interval = int(part.split("=")[1])
                        except Exception:
                            pass
                until_date = end_range
                for part in rrule.split(";"):
                    if part.startswith("UNTIL="):
                        u_str = part.split("=")[1]
                        if "T" in u_str:
                            u_str = u_str.split("T")[0]
                        u_str = u_str[:8]
                        if len(u_str) == 8:
                            try:
                                parsed_until = datetime.date(int(u_str[:4]), int(u_str[4:6]), int(u_str[6:8]))
                                until_date = min(until_date, parsed_until)
                            except Exception:
                                pass

                cur = event_date
                step = datetime.timedelta(days=7 * interval)
                while cur <= min(end_range, until_date):
                    if cur >= start_range:
                        d_str = cur.strftime("%Y%m%d")
                        if d_str not in exdates:
                            d_iso = cur.isoformat()
                            if d_iso not in busy:
                                busy[d_iso] = set()
                            busy[d_iso].add(bell)
                    cur += step
            else:
                if start_range <= event_date <= end_range:
                    d_str = event_date.strftime("%Y%m%d")
                    if d_str not in exdates:
                        d_iso = event_date.isoformat()
                        if d_iso not in busy:
                            busy[d_iso] = set()
                        busy[d_iso].add(bell)

        elif in_event and ":" in line_s:
            colon_idx = line_s.find(":")
            k = line_s[:colon_idx].split(";")[0].upper().strip()
            v = line_s[colon_idx+1:].strip()
            props[k] = v

    return {k: sorted(list(v)) for k, v in sorted(busy.items())}

def fetch_room_schedule(room: Dict, start_range: datetime.date, end_range: datetime.date) -> Optional[Dict]:
    """Загружает iCal одной аудитории и рассчитывает занятость."""
    rid = room["id"]
    full_title = room.get("fullTitle", "")
    campus, room_name, floor = extract_floor_and_campus(full_title)

    # Игнорируем виртуальные аудитории
    if "дистанционно" in full_title.lower() or "сдо" in full_title.lower() or "дистант" in full_title.lower():
        return None

    url = f"{BASE_API_URL}/ical/3/{rid}"
    req = urllib.request.Request(url, headers={
        "User-Agent": USER_AGENT,
        "Accept": "text/calendar, application/json, */*"
    })

    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=12) as resp:
                raw_ical = resp.read().decode("utf-8")
                busy_schedule = parse_room_ical(raw_ical, start_range, end_range)
                return {
                    "id": rid,
                    "name": room_name,
                    "fullTitle": full_title,
                    "campus": campus,
                    "floor": floor,
                    "busy": busy_schedule
                }
        except urllib.error.HTTPError as e:
            if e.code == 404:
                # Аудитория без расписания (всегда свободна)
                return {
                    "id": rid,
                    "name": room_name,
                    "fullTitle": full_title,
                    "campus": campus,
                    "floor": floor,
                    "busy": {}
                }
            time.sleep(0.5 * (attempt + 1))
        except Exception:
            time.sleep(0.5 * (attempt + 1))

    return {
        "id": rid,
        "name": room_name,
        "fullTitle": full_title,
        "campus": campus,
        "floor": floor,
        "busy": {}
    }

def main():
    print("=== Начало построения базы свободных аудиторий ===")
    setup_global_opener()

    today = datetime.date.today()
    # Расписание на следующие 30 дней вперед
    start_range = today
    end_range = today + datetime.timedelta(days=30)
    print(f"Период расчета: с {start_range} по {end_range}")

    rooms_meta = discover_all_classrooms()
    if not rooms_meta:
        print("[Ошибка] Не удалось получить аудитории от сервера МИРЭА (возможна гео-блокировка или недоступность сети).")
        sys.exit(1)

    processed_rooms = []
    print(f"Загрузка iCal и расчет занятости для {len(rooms_meta)} аудиторий (concurrency=8)...")
    start_time = time.time()

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        futures = {
            executor.submit(fetch_room_schedule, r, start_range, end_range): r 
            for r in rooms_meta
        }
        done_count = 0
        for future in concurrent.futures.as_completed(futures):
            res = future.result()
            if res:
                processed_rooms.append(res)
            done_count += 1
            if done_count % 100 == 0 or done_count == len(rooms_meta):
                print(f"Обработано {done_count}/{len(rooms_meta)} аудиторий...")

    elapsed = time.time() - start_time
    print(f"Все аудитории обработаны за {elapsed:.2f} сек. Итоговых аудиторий: {len(processed_rooms)}")

    # Сортировка: сначала известные кампусы, затем этаж, затем имя
    def sort_key(r):
        c = r["campus"]
        c_idx = CAMPUSES_ORDER.index(c) if c in CAMPUSES_ORDER else 99
        fl = r["floor"] if r["floor"] is not None else 99
        return (c_idx, c, fl, r["name"])

    processed_rooms.sort(key=sort_key)

    # Список уникальных обнаруженных кампусов
    campuses = []
    for c in CAMPUSES_ORDER:
        if any(r["campus"] == c for r in processed_rooms):
            campuses.append(c)
    other_campuses = sorted(list(set(r["campus"] for r in processed_rooms if r["campus"] not in CAMPUSES_ORDER)))
    campuses.extend(other_campuses)

    result_payload = {
        "updatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "dateRange": {
            "start": start_range.isoformat(),
            "end": end_range.isoformat()
        },
        "campuses": campuses,
        "bellSlots": BELL_SLOTS,
        "rooms": processed_rooms
    }

    out_dir = os.path.join(os.getcwd(), "dist_free_rooms")
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, "free_rooms.json")

    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(result_payload, f, ensure_ascii=False, indent=None, separators=(',', ':'))

    size_kb = os.path.getsize(out_file) / 1024
    print(f"Файл успешно сформирован: {out_file} ({size_kb:.1f} КБ)")

if __name__ == "__main__":
    main()
