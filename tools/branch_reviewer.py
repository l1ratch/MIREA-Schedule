#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Branch AI Reviewer & Diff Analyzer for MIREA-Schedule
Analyzes differences between a contributor branch and main,
runs heuristic & architectural checks for KMP/Compose,
and queries GitHub Models / Copilot for deep AI review.
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
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, shell=False)
    if check and result.returncode != 0:
        raise RuntimeError(f"Command failed ({result.returncode}): {' '.join(cmd)}\n{result.stderr}")
    return result.stdout.strip()

def get_git_info(branch: str, base: str = 'main'):
    """Gather git commits, diff stats, and changed files."""
    for ref_prefix in ['', 'origin/']:
        test_ref = f"{ref_prefix}{branch}"
        check = subprocess.run(['git', 'rev-parse', '--verify', test_ref], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        if check.returncode == 0:
            target_ref = test_ref
            break
    else:
        target_ref = branch

    base_ref = base
    for ref_prefix in ['', 'origin/']:
        test_base = f"{ref_prefix}{base}"
        check = subprocess.run(['git', 'rev-parse', '--verify', test_base], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
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

def categorize_files(file_stats):
    """Group modified files into architectural domains."""
    categories = {
        'ui': [],
        'data': [],
        'android': [],
        'ios': [],
        'ci': [],
        'docs': [],
        'other': []
    }

    for item in file_stats:
        p = item['path'].lower()
        if 'shared' in p and ('screen' in p or 'component' in p or 'theme' in p):
            categories['ui'].append(item)
        elif 'shared' in p and ('data' in p or 'model' in p or 'repository' in p or 'api' in p or 'parser' in p):
            categories['data'].append(item)
        elif 'androidapp' in p or 'android' in p:
            categories['android'].append(item)
        elif 'iosapp' in p or 'ios' in p or 'swift' in p:
            categories['ios'].append(item)
        elif '.github' in p or 'tools/' in p:
            categories['ci'].append(item)
        elif p.endswith('.md') or 'docs/' in p or 'license' in p:
            categories['docs'].append(item)
        else:
            categories['other'].append(item)

    return categories

def run_heuristics(git_info):
    """Perform static checks on diff for common KMP / Compose pitfalls."""
    diff = git_info['diff']
    warnings = []
    notices = []
    tips = []

    # 1. Threading / Dispatchers check
    if 'Dispatchers.Main' in diff and ('http' in diff or 'download' in diff or 'parse' in diff or 'delay' in diff):
        warnings.append('⚠️ **Потенциальный фриз UI:** Обнаружены тяжелые операции или сетевые запросы рядом с `Dispatchers.Main`. Сетевой стек и парсинг iCal должны работать строго на `Dispatchers.IO` или `Dispatchers.Default`.')

    if 'runBlocking' in diff:
        warnings.append('⚠️ **Блокировка потока (`runBlocking`):** Использование `runBlocking` может приводить к ANR на Android или зависанию UI-раннера на iOS. Рекомендуется использовать корутинный скоуп (`viewModelScope`, `rememberCoroutineScope`).')

    # 2. Hardcoded secrets check
    secret_patterns = [
        (r'(?i)(api[_-]?key|secret|token|password)\s*=\s*[\'"][a-zA-Z0-9_\-]{16,}', 'Возможная утечка секрета или токена'),
        (r'https://[^/\s]+:[^@\s]+@', 'Учетные данные в URL')
    ]
    for pattern, desc in secret_patterns:
        if re.search(pattern, diff):
            warnings.append(f'🚨 **Безопасность:** В коде обнаружен подозрительный захардкоженный ключ или токен ({desc}). Проверьте добавленный код.')

    # 3. Crash / Exception safety
    if 'throw ' in diff or 'error(' in diff or 'TODO(' in diff:
        notices.append('💡 **TODO / Необработанные исключения:** В ветке добавлены вызовы исключений или заглушки `TODO()`. Убедитесь, что они не могут быть вызваны в рабочем сценарии пользователя.')

    # 4. Debugging leftovers
    if 'println(' in diff or 'Log.d(' in diff:
        notices.append('🧹 **Отладочные логи:** В коде присутствуют вызовы `println` / `Log.d`. Рекомендуется удалить их перед слиянием в `main`.')

    # 5. Dependency bloat check
    gradle_changes = [f for f in git_info['file_stats'] if 'gradle' in f['path'].lower() or 'toml' in f['path'].lower()]
    if gradle_changes:
        tips.append('📦 **Изменение зависимостей:** Изменены файлы конфигурации сборки (`libs.versions.toml` или `build.gradle.kts`). Убедитесь, что новые библиотеки действительно необходимы и компилируются как под Android, так и под iOS.')

    # 6. Ponytail / Overengineering check
    if git_info['total_added'] > 500 and git_info['total_deleted'] < 20:
        tips.append('✂️ **Минимализм кода:** Добавлено много нового кода (>500 строк). Проверьте, нельзя ли упростить решение стандартными средствами Kotlin Multiplatform или существующими утилитами проекта.')

    return {
        'warnings': warnings,
        'notices': notices,
        'tips': tips
    }

def query_ai_review(git_info):
    """Ask GitHub Models / Azure AI for automated review."""
    token = os.environ.get('GITHUB_TOKEN')
    if not token:
        return None

    diff_snippet = git_info['diff'][:12000]
    if len(git_info['diff']) > 12000:
        diff_snippet += "\n\n[... diff truncated for AI review ...]"

    commits_text = "\n".join(f"- {c}" for c in git_info['commits'])

    system_prompt = """Ты — ведущий мобильный архитектор и ревьюер открытого проекта MIREA-Schedule (Kotlin Multiplatform + Compose Multiplatform для Android и iOS).
Твоя задача — объективно, доброжелательно и конструктивно оценить изменения из ветки контрибьютора относительно main.

Структура ответа (на русском языке в GitHub Flavored Markdown):
### 🎯 Суть изменений
(1-2 предложения, что делает эта ветка)

### 🔬 Архитектурная оценка и влияние
- **Android**: влияние на стабильность, жизненный цикл и запуск.
- **iOS**: влияние на Darwin runtime, память и отображение.
- **Чистота и стиль кода**: читаемость, минимализм, отсутствие бойлерплейта.

### 💡 Рекомендации контрибьютору
(Конкретные пункты, если есть замечания или что стоит проверить)

### 🏁 Резюме ревью
(Выбери одно: ✅ **Готово к слиянию (LGTM)** / ⚠️ **Требуются небольшие правки** / 🛑 **Требуется доработка и пересмотр архитектуры**)"""

    user_prompt = f"""Ветка для анализа: `{git_info['branch']}`
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
        with request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            ai_text = data.choices[0]['message']['content'].strip()
            return ai_text
    except Exception as e:
        print(f"AI review call skipped or failed: {e}", file=sys.stderr)
        return None

def generate_report(git_info, categories, heuristics, ai_review):
    """Compose the full Markdown review report."""
    lines = []
    branch = git_info['branch']
    commits_count = len(git_info['commits'])
    files_count = len(git_info['file_stats'])
    added = git_info['total_added']
    deleted = git_info['total_deleted']

    lines.append(f"# 🔍 Отчёт об аудите ветки `{branch}` относительно `main`")
    lines.append("")
    lines.append("| Ветка | Коммитов | Изменено файлов | Добавлено строк | Удалено строк |")
    lines.append("| :--- | :--- | :--- | :--- | :--- |")
    lines.append(f"| `{branch}` | **{commits_count}** | **{files_count}** | <span style='color:green'>+{added}</span> | <span style='color:red'>-{deleted}</span> |")
    lines.append("")

    if git_info['authors']:
        lines.append(f"> 👤 **Авторы изменений:** {', '.join(f'`{a}`' for a in git_info['authors'])}")
        lines.append("")

    if heuristics['warnings']:
        lines.append("### ⚠️ Обратите внимание на риски:")
        for w in heuristics['warnings']:
            lines.append(f"- {w}")
        lines.append("")

    if heuristics['notices']:
        lines.append("### 📌 Замечания и рекомендации по коду:")
        for n in heuristics['notices']:
            lines.append(f"- {n}")
        lines.append("")

    if heuristics['tips']:
        lines.append("### 💡 Советы:")
        for t in heuristics['tips']:
            lines.append(f"- {t}")
        lines.append("")

    if ai_review:
        lines.append("## 🤖 Анализ GitHub AI Agent")
        lines.append(ai_review)
        lines.append("")
    else:
        lines.append("## 📊 Структурный обзор изменений")
        if not git_info['commits']:
            lines.append("> ℹ️ Ветка синхронизирована с `main` или не содержит новых уникальных коммитов.")
        else:
            lines.append("**Коммиты в ветке:**")
            for c in git_info['commits']:
                lines.append(f"- `{c}`")
        lines.append("")

    lines.append("## 📂 Затронутые компоненты")
    cat_names = [
        ('ui', '🎨 Пользовательский интерфейс (UI / Compose)'),
        ('data', '💾 Данные, API, парсер и модели'),
        ('android', '🤖 Android специфичный код'),
        ('ios', '🍏 iOS специфичный код'),
        ('ci', '🛠️ CI / GitHub Actions / Скрипты'),
        ('docs', '📖 Документация'),
        ('other', '📦 Прочие файлы')
    ]

    for cat_id, title in cat_names:
        items = categories.get(cat_id, [])
        if items:
            lines.append(f"<details><summary><b>{title}</b> ({len(items)} файлов)</summary>\n")
            lines.append("| Файл | + Добавлено | - Удалено |")
            lines.append("| :--- | :--- | :--- |")
            for item in items:
                lines.append(f"| `{item['path']}` | `+{item['add']}` | `-{item['delete']}` |")
            lines.append("\n</details>\n")

    lines.append("---")
    lines.append("*💡 Сгенерировано автоматически с помощью Branch AI Reviewer для проекта MIREA-Schedule.*\n")

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
    categories = categorize_files(git_info['file_stats'])
    heuristics = run_heuristics(git_info)
    ai_review = query_ai_review(git_info)

    report = generate_report(git_info, categories, heuristics, ai_review)

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
