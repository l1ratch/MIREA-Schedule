#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Branch AI Reviewer & Diff Analyzer for MIREA-Schedule
Analyzes differences between a contributor branch and main,
runs heuristic & architectural checks for KMP/Compose,
and generates a deep Russian impact analysis.
"""

import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path
from urllib import request, error

def run_cmd(cmd, check=True):
    """Run shell command and return stdout."""
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding='utf-8', errors='replace', shell=False)
    if check and result.returncode != 0:
        raise RuntimeError(f"Command failed ({result.returncode}): {' '.join(cmd)}\\n{result.stderr}")
    return (result.stdout or '').strip()

def get_git_info(branch: str, base: str = 'main'):
    """Gather git commits, diff stats, and changed files."""
    for ref_prefix in ['', 'origin/']:
        test_ref = f"{ref_prefix}{branch}"
        check = subprocess.run(['git', 'rev-parse', '--verify', test_ref], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding='utf-8', errors='replace')
        if check.returncode == 0:
            target_ref = test_ref
            break
    else:
        target_ref = branch

    base_ref = base
    for ref_prefix in ['', 'origin/']:
        test_base = f"{ref_prefix}{base}"
        check = subprocess.run(['git', 'rev-parse', '--verify', test_base], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding='utf-8', errors='replace')
        if check.returncode == 0:
            base_ref = test_base
            break

    try:
        merge_base = run_cmd(['git', 'merge-base', base_ref, target_ref])
    except Exception:
        merge_base = base_ref

    commits_raw = run_cmd(['git', 'log', '--oneline', '--no-merges', f"{merge_base}..{target_ref}"], check=False)
    commits = [line.strip() for line in commits_raw.splitlines() if line.strip()]

    author_log = run_cmd(['git', 'log', '--format=%an <%ae>', f"{merge_base}..{target_ref}"], check=False)
    authors = list(set([line.strip() for line in author_log.splitlines() if line.strip()]))

    numstat_raw = run_cmd(['git', 'diff', '--numstat', f"{merge_base}...{target_ref}"], check=False)
    file_stats = []
    total_added = 0
    total_deleted = 0
    for line in numstat_raw.splitlines():
        parts = line.strip().split('\t')
        if len(parts) >= 3:
            add = int(parts[0]) if parts[0].isdigit() else 0
            delete = int(parts[1]) if parts[1].isdigit() else 0
            path = parts[2]
            total_added += add
            total_deleted += delete
            file_stats.append({'path': path, 'add': add, 'delete': delete})

    full_diff = run_cmd(['git', 'diff', f"{merge_base}...{target_ref}"], check=False)

    return {
        'branch': branch,
        'target_ref': target_ref,
        'base_ref': base_ref,
        'merge_base': merge_base,
        'commits': commits,
        'authors': authors,
        'file_stats': file_stats,
        'total_added': total_added,
        'total_deleted': total_deleted,
        'diff': full_diff
    }

def analyze_feature_impact(file_stats, diff):
    """Produce structured Russian domain analysis of what changes affect."""
    impacts = []
    testing_recommendations = []
    affected_modules = set()

    paths = [f['path'].replace('\\', '/') for f in file_stats]

    # Schedule module
    schedule_files = [p for p in paths if 'screens/schedule' in p or 'ScheduleRepository' in p or 'Lesson' in p or 'MireaICal' in p]
    if schedule_files:
        affected_modules.add('📅 **Расписание занятий (Главный экран)**')
        details = []
        if any('LessonCard' in p for p in schedule_files):
            details.append('карточки занятий (UI отображения пар, преподавателей, кабинетов)')
        if any('WeekCalendarStrip' in p for p in schedule_files):
            details.append('мини-календарь (переключение дней и недель)')
        if any('ScheduleRepository' in p or 'MireaICal' in p for p in schedule_files):
            details.append('загрузка и парсинг iCal-расписания с серверов университета')
        if not details:
            details.append('логика и отображение сетки расписания')
        impacts.append(f"- 📅 **Расписание пар:** Затронуты {', '.join(details)}. Файлы: {', '.join(f'`{Path(p).name}`' for p in schedule_files[:3])}.")
        testing_recommendations.append("Проверьте переключение между 1-й и 2-й неделей и открытие детальной информации о паре.")

    # Free Rooms module
    rooms_files = [p for p in paths if 'screens/rooms' in p or 'FreeRooms' in p]
    if rooms_files:
        affected_modules.add('🏢 **Свободные аудитории**')
        impacts.append(f"- 🏢 **Свободные аудитории:** Изменена логика поиска свободных кабинетов, фильтрации по дате, номеру пары или кампусам. Файлы: {', '.join(f'`{Path(p).name}`' for p in rooms_files[:3])}.")
        testing_recommendations.append("Проверьте поиск свободных аудиторий на текущую дату и на воскресенье (когда пары отсутствуют).")

    # Campus Maps module
    map_files = [p for p in paths if 'screens/map' in p or 'maps/' in p or 'CampusMap' in p or 'MapHtml' in p]
    if map_files:
        affected_modules.add('🗺️ **Интерактивные карты кампусов**')
        impacts.append(f"- 🗺️ **Карты кампусов:** Обновлены векторные схемы этажей, масштабирование WebView или маркеры аудиторий. Файлы: {', '.join(f'`{Path(p).name}`' for p in map_files[:3])}.")
        testing_recommendations.append("Откройте карты кампусов В-78, С-20 и В-86, проверьте плавность зума и переключение этажей на Android и iOS.")

    # Tasks and Subjects module
    tasks_files = [p for p in paths if 'screens/tasks' in p or 'TaskRepository' in p or 'TaskModel' in p]
    if tasks_files:
        affected_modules.add('📝 **Задачи и предметы**')
        impacts.append(f"- 📝 **Задачи и дедлайны:** Затронут трекер учебных заданий, баллов или конструктор предметов. Файлы: {', '.join(f'`{Path(p).name}`' for p in tasks_files[:3])}.")
        testing_recommendations.append("Создайте тестовую задачу с дедлайном и убедитесь, что она корректно сохраняется и отображается в списке.")

    # Storage and Cache
    storage_files = [p for p in paths if 'storage/' in p or 'UnifiedSyncManager' in p or 'DataAndCache' in p]
    if storage_files:
        affected_modules.add('💾 **Хранилище данных и кэширование**')
        impacts.append(f"- 💾 **Локальное хранилище:** Изменена сериализация или запись в `PlatformStorage` (`SharedPreferences` / `NSUserDefaults`). Опасность потери кэша при обновлении!")
        testing_recommendations.append("Проверьте холодный запуск приложения без интернета (офлайн-режим) — данные должны загружаться из локального кэша.")

    # Android specific
    android_files = [p for p in paths if 'androidapp/' in p or 'androidmain/' in p]
    if android_files:
        affected_modules.add('🤖 **Android Runtime**')
        impacts.append(f"- 🤖 **Android платформа:** Затронуты специфичные компоненты Android (`MainActivity`, системные отступы `WindowInsets`, манифест).")
        testing_recommendations.append("Обязательно соберите и протестируйте тестовый APK на реальном телефоне Android.")

    # iOS specific
    ios_files = [p for p in paths if 'iosapp/' in p or 'iosmain/' in p or 'swift' in p]
    if ios_files:
        affected_modules.add('🍏 **iOS Runtime**')
        impacts.append(f"- 🍏 **iOS платформа:** Затронуты Swift-код, проект Xcode или адаптеры Darwin runtime.")
        testing_recommendations.append("Установите тестовый IPA через AltStore / TrollStore / Scarlet и проверьте запуск на iPhone.")

    # Build & Dependencies
    build_files = [p for p in paths if 'gradle' in p or 'versions.toml' in p or 'build.gradle' in p]
    if build_files:
        affected_modules.add('📦 **Сборочная система (Gradle)**')
        impacts.append(f"- 📦 **Конфигурация сборки:** Изменены зависимости или настройки плагинов. Файлы: {', '.join(f'`{Path(p).name}`' for p in build_files[:3])}.")
        testing_recommendations.append("Убедитесь, что `./gradlew assembleDebug` и компиляция iOS проходят без ошибок депенденси-резолвинга.")

    # Other / Settings
    other_files = [p for p in paths if 'screens/other' in p or 'theme/' in p or 'components/' in p]
    if other_files:
        affected_modules.add('🎨 **Дизайн и компоненты интерфейса**')
        impacts.append(f"- 🎨 **Интерфейс:** Изменения в плавающем доке (`FloatingDock`), свайпе назад или темах оформления.")

    return {
        'modules': list(affected_modules),
        'impacts': impacts,
        'testing': testing_recommendations
    }

def run_heuristics(git_info):
    """Perform static checks on diff for common KMP / Compose pitfalls."""
    diff = git_info['diff']
    warnings = []
    notices = []
    tips = []

    # 1. Threading / Dispatchers check
    if 'Dispatchers.Main' in diff and ('http' in diff or 'download' in diff or 'parse' in diff or 'delay' in diff):
        warnings.append('⚠️ **Потенциальный фриз UI:** Обнаружены тяжелые операции рядом с `Dispatchers.Main`. Сетевой стек и парсинг iCal должны выполняться на `Dispatchers.IO` или `Dispatchers.Default`.')

    if 'runBlocking' in diff:
        warnings.append('⚠️ **Блокировка потока (`runBlocking`):** Использование `runBlocking` может приводить к зависанию UI или ANR. Рекомендуется использовать `viewModelScope`.')

    # 2. Hardcoded secrets check
    secret_patterns = [
        (r'(?i)(api[_-]?key|secret|token|password)\s*=\s*["\'][a-zA-Z0-9_\-]{16,}', 'Возможная утечка секрета или токена'),
        (r'https://[^/\s]+:[^@\s]+@', 'Учетные данные в URL')
    ]
    for pattern, desc in secret_patterns:
        if re.search(pattern, diff):
            warnings.append(f'🚨 **Безопасность:** В коде обнаружен подозрительный ключ или токен ({desc}). Проверьте добавленный код.')

    # 3. Crash / Exception safety
    if 'throw ' in diff or 'error(' in diff or 'TODO(' in diff:
        notices.append('💡 **TODO / Необработанные исключения:** В кодовой базе обнаружены вызовы исключений или заглушки `TODO()`. Убедитесь, что они защищены `try-catch`.')

    # 4. Debugging leftovers
    if 'println(' in diff or 'Log.d(' in diff:
        notices.append('🧹 **Отладочные логи:** В коде присутствуют вызовы `println` / `Log.d`. Рекомендуется удалить их перед слиянием в `main`.')

    # 5. Dependency bloat check
    gradle_changes = [f for f in git_info['file_stats'] if 'gradle' in f['path'].lower() or 'toml' in f['path'].lower()]
    if gradle_changes:
        tips.append('📦 **Изменение зависимостей:** Изменены сборочные скрипты. Проверьте совместимость библиотек с Kotlin 2.x.')

    # 6. Ponytail / Overengineering check
    if git_info['total_added'] > 500 and git_info['total_deleted'] < 20:
        tips.append('✂️ **Минимализм кода:** Добавлено много нового кода (>500 строк). Проверьте, нельзя ли упростить решение стандартными средствами Kotlin Multiplatform.')

    return {
        'warnings': warnings,
        'notices': notices,
        'tips': tips
    }

def query_ai_review(git_info):
    """Ask GitHub Models / Azure AI for automated review if token permits."""
    token = os.environ.get('GITHUB_TOKEN') or os.environ.get('GH_TOKEN')
    if not token:
        return None

    diff_snippet = git_info['diff'][:12000]
    if len(git_info['diff']) > 12000:
        diff_snippet += "\n\n[... diff truncated for AI review ...]"

    commits_text = "\n".join(f"- {c}" for c in git_info['commits'])

    system_prompt = """Ты — ведущий мобильный архитектор открытого проекта MIREA-Schedule (Kotlin Multiplatform + Compose Multiplatform для Android и iOS).
Оцени изменения из ветки контрибьютора относительно main на русском языке.

Формат ответа:
### 🎯 Суть изменений и решаемая задача
(Кратко в 2-3 предложениях)

### 🔬 Архитектурная оценка и платформенное влияние
- **Android**: влияние на запуск, UI и память.
- **iOS**: поведение на iOS/Darwin runtime.
- **Качество кода**: читаемость, безопасность потоков, отсутствие дублирования.

### 🏁 Итоговый вердикт ревьюера
(✅ Готово к слиянию (LGTM) / ⚠️ Нужны небольшие доработки / 🛑 Требуется пересмотр)"""

    user_prompt = f"""Ветка: `{git_info['branch']}`
Коммиты:
{commits_text}

Измененные файлы ({len(git_info['file_stats'])}):
{json.dumps([f['path'] for f in git_info['file_stats'][:25]], indent=2)}

Фрагмент Git Diff:
```diff
{diff_snippet}
```"""

    try:
        url = 'https://models.inference.ai.azure.com/chat/completions'
        payload = json.dumps({
            'model': 'gpt-4o-mini',
            'messages': [
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': user_prompt}
            ],
            'temperature': 0.2,
            'max_tokens': 750
        }).encode('utf-8')

        headers = {
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {token}'
        }

        req = request.Request(url, data=payload, headers=headers, method='POST')
        with request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            ai_text = data.choices[0]['message']['content'].strip()
            return ai_text
    except Exception as e:
        print(f"Note: AI API call not active ({e}), using deep heuristic review.", file=sys.stderr)
        return None

def generate_report(git_info, domain_analysis, heuristics, ai_review):
    """Compose the full Markdown review report in Russian."""
    lines = []
    branch = git_info['branch']
    commits_count = len(git_info['commits'])
    files_count = len(git_info['file_stats'])
    added = git_info['total_added']
    deleted = git_info['total_deleted']

    lines.append(f"# 🔍 Экспертный аудит ветки `{branch}` относительно `main`")
    lines.append("")
    lines.append("| Ветка | Коммитов | Изменено файлов | Добавлено строк | Удалено строк |")
    lines.append("| :--- | :--- | :--- | :--- | :--- |")
    lines.append(f"| `{branch}` | **{commits_count}** | **{files_count}** | <span style='color:green'>+{added}</span> | <span style='color:red'>-{deleted}</span> |")
    lines.append("")

    if git_info['authors']:
        lines.append(f"> 👤 **Авторы изменений:** {', '.join(f'`{a}`' for a in git_info['authors'])}")
        lines.append("")

    # If completely empty or in sync
    if not git_info['commits'] and files_count == 0:
        lines.append("> ℹ️ **Ветка полностью синхронизирована с `main`:** Новых коммитов или изменённых файлов относительно `main` не обнаружено.")
        lines.append("")
        return "\n".join(lines)

    # 1. AI Review section (if available)
    if ai_review:
        lines.append("## 🤖 Заключение AI-архитектора (GitHub Copilot)")
        lines.append(ai_review)
        lines.append("")

    # 2. Detailed Russian Feature Impact Analysis (ALWAYS PRESENT!)
    lines.append("## 🎯 На что повлияли правки относительно `main`")
    if domain_analysis['modules']:
        lines.append(f"**Затронутые разделы приложения:** {', '.join(domain_analysis['modules'])}")
        lines.append("")
        for imp in domain_analysis['impacts']:
            lines.append(imp)
        lines.append("")
    else:
        lines.append("Правки затрагивают служебные конфигурации, скрипты автоматизаций или документацию без изменения логики экранов приложения.")
        lines.append("")

    # 3. Testing recommendations
    if domain_analysis['testing']:
        lines.append("### 🧪 Чеклист для проверки на устройстве (Что тестировать):")
        for t in domain_analysis['testing']:
            lines.append(f"- [ ] {t}")
        lines.append("")

    # 4. Warnings / Heuristics
    if heuristics['warnings']:
        lines.append("### ⚠️ Архитектурные риски и замечания:")
        for w in heuristics['warnings']:
            lines.append(f"- {w}")
        lines.append("")

    if heuristics['notices']:
        lines.append("### 📌 Замечания по чистоте кода:")
        for n in heuristics['notices']:
            lines.append(f"- {n}")
        lines.append("")

    if heuristics['tips']:
        lines.append("### 💡 Советы по оптимизации:")
        for t in heuristics['tips']:
            lines.append(f"- {t}")
        lines.append("")

    # 5. Commits list
    if git_info['commits']:
        lines.append("### 📜 История коммитов ветки:")
        for c in git_info['commits']:
            lines.append(f"- `{c}`")
        lines.append("")

    # 6. Verdict summary
    lines.append("## 🏁 Резюме аудитора")
    if heuristics['warnings']:
        lines.append("> ⚠️ **Внимание:** Рекомендуется устранить указанные архитектурные риски перед мерджем в `main`.")
    elif domain_analysis['testing']:
        lines.append("> 🧪 **Рекомендация:** Код выглядит чисто. Скачайте тестовый APK / IPA из вкладки Actions и подтвердите корректную работу на телефоне перед слиянием с `main`.")
    else:
        lines.append("> ✅ **LGTM:** Изменения готовы к слиянию с основной веткой `main`.")
    lines.append("")

    lines.append("---")
    lines.append("*💡 Сгенерировано автоматически инструментом Branch AI Reviewer для проекта MIREA-Schedule.*\n")

    return "\n".join(lines)

def main():
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    if hasattr(sys.stderr, 'reconfigure'):
        sys.stderr.reconfigure(encoding='utf-8')

    parser = argparse.ArgumentParser(description="Review and evaluate branch diff vs main")
    parser.add_argument('--branch', required=True, help="Target branch name (e.g. fix_p_m)")
    parser.add_argument('--base', default='main', help="Base branch to compare against (default: main)")
    parser.add_argument('--pr-number', type=int, default=None, help="Pull Request number to post comment to")
    parser.add_argument('--post-comment', action='store_true', help="Post comment to GitHub PR via gh cli")
    parser.add_argument('--output', default='review_report.md', help="Output markdown file path")
    parser.add_argument('--step-summary', action='store_true', help="Write to GITHUB_STEP_SUMMARY")
    args = parser.parse_args()

    print(f"==> Analyzing branch '{args.branch}' against '{args.base}'...")
    git_info = get_git_info(args.branch, args.base)
    domain_analysis = analyze_feature_impact(git_info['file_stats'], git_info['diff'])
    heuristics = run_heuristics(git_info)
    ai_review = query_ai_review(git_info)

    report = generate_report(git_info, domain_analysis, heuristics, ai_review)

    out_path = Path(args.output)
    out_path.write_text(report, encoding='utf-8')
    print(f"==> Report saved to {out_path.resolve()}")

    if args.step_summary or os.environ.get('GITHUB_STEP_SUMMARY'):
        summary_env = os.environ.get('GITHUB_STEP_SUMMARY')
        if summary_env:
            with open(summary_env, 'a', encoding='utf-8') as f:
                f.write(report + '\n')
            print("==> Appended to GITHUB_STEP_SUMMARY")

    pr_num = args.pr_number
    if not pr_num and args.post_comment:
        try:
            pr_out = run_cmd(['gh', 'pr', 'list', '--head', args.branch, '--state', 'open', '--json', 'number', '--jq', '.[0].number'], check=False)
            if pr_out.isdigit():
                pr_num = int(pr_out)
        except Exception:
            pass

    if args.post_comment and pr_num:
        print(f"==> Posting review comment to Pull Request #{pr_num}...")
        try:
            run_cmd(['gh', 'pr', 'comment', str(pr_num), '--body-file', str(out_path)])
            print(f"==> Comment posted to PR #{pr_num}!")
        except Exception as e:
            print(f"Warning: Could not post comment to PR #{pr_num}: {e}", file=sys.stderr)

    print("\n" + report)

if __name__ == '__main__':
    main()
