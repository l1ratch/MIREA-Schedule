#!/usr/bin/env python3
"""
tools/update_version_feed.py — генерация канальных фидов и IPA-источников на gh-pages.

Каналы и файлы:
  --channel stable   : version.json (автообновление обычных пользователей)
                       + apps.json (AltStore-совместимый источник стабильных IPA)
                       + apps-beta.json (GBox-источник «всегда последняя сборка»)
  --channel beta|rc  : beta.json (opt-in бета-канал в настройках приложения)
                       + apps-beta.json
  --channel preview  : apps-beta.json только (rolling dev-сборка main)

apps-beta.json — «самая свежая сборка» (dev/beta/rc/stable): кто последний
собрался, тот и записан. Используется владельцем для цикла
«собрал → обновил в GBox → протестил». В description дублируются версия
и дата/время, чтобы не запутаться.

Вызывается ТОЛЬКО из релизного и preview-воркфлоу. Деплой на gh-pages
выполняет workflow (peaceiris/actions-gh-pages, keep_files). Источники
apps.json / apps-beta.json совместимы с AltStore-подобными клиентами
(SideStore, GBox и др.); один bundle id — подключай один источник за раз.
"""

import argparse
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

APP_VERSION_FILE = "shared/src/commonMain/kotlin/com/jetbrains/kmpapp/data/model/AppVersion.kt"
BUNDLE_ID = "ru.l1ratch.mireaschedule"
TINT_COLOR = "4F46E5"
APP_DESCRIPTION = (
    "Расписание пар РТУ МИРЭА: поиск свободных аудиторий, интерактивные "
    "карты корпусов, задачи и офлайн-кеш."
)


def parse_app_version(path):
    content = Path(path).read_text(encoding="utf-8")
    m = re.search(r'const\s+val\s+GITHUB_REPO\s*=\s*"([^"]+)"', content)
    repo = m.group(1) if m else "l1ratch/MIREA-Schedule"
    m = (re.search(r'const\s+val\s+CHANGELOG\s*=\s*"""([\s\S]*?)"""', content)
         or re.search(r'const\s+val\s+CHANGELOG\s*=\s*"([^"]*)"', content))
    changelog = m.group(1).strip() if m else ""
    m = re.search(r'const\s+val\s+IS_CRITICAL\s*=\s*(true|false)', content)
    critical = (m.group(1) == "true") if m else False
    m = re.search(r'const\s+val\s+MIN_SUPPORTED_BUILD\s*=\s*(\d+)', content)
    min_supported = int(m.group(1)) if m else 1
    return repo, changelog, critical, min_supported


def asset_urls(repo, channel, version):
    if channel == "stable":
        base = f"https://github.com/{repo}/releases"
        return {
            "download_url": f"{base}/latest",
            "apk_url": f"{base}/latest/download/Schedule-MIREA.apk",
            "ipa_url": f"{base}/latest/download/Schedule-MIREA.ipa",
        }
    if channel in ("beta", "rc"):
        base = f"https://github.com/{repo}/releases/download/v{version}"
        return {
            "download_url": base,
            "apk_url": f"{base}/Schedule-MIREA-v{version}.apk",
            "ipa_url": f"{base}/Schedule-MIREA-v{version}.ipa",
        }
    # preview (rolling dev)
    base = f"https://github.com/{repo}/releases/download/preview"
    return {
        "download_url": f"{base}/MIREA-Schedule-preview.apk",
        "apk_url": f"{base}/MIREA-Schedule-preview.apk",
        "ipa_url": f"{base}/MIREA-Schedule-preview.ipa",
    }


def build_source(repo, channel, version, ipa_url):
    """AltStore-совместимый источник. apps-beta.json всегда несёт последнюю сборку."""
    stable = channel == "stable"
    filename = "apps.json" if stable else "apps-beta.json"
    now = datetime.now(timezone.utc)
    # В description дублируем версию и время — так в GBox видно, что установлено.
    description = (
        f"{APP_DESCRIPTION}\n\n"
        f"Сборка: {version}\n"
        f"Обновлено: {now.strftime('%Y-%m-%d %H:%M UTC')}"
    )
    return {
        "name": "MIREA Schedule" + ("" if stable else " (Beta)"),
        "identifier": f"mirea-schedule-{'stable' if stable else 'beta'}",
        "sourceURL": f"https://raw.githubusercontent.com/{repo}/gh-pages/{filename}",
        "apps": [{
            "name": "Расписание МИРЭА",
            "bundleIdentifier": BUNDLE_ID,
            "developerName": "l1ratch",
            "localizedDescription": description,
            "iconURL": f"https://raw.githubusercontent.com/{repo}/main/shared/src/commonMain/composeResources/drawable/app_icon.png",
            "version": version,
            "versionDate": now.isoformat(),
            "downloadURL": ipa_url,
            "tintColor": TINT_COLOR,
        }],
    }


def main():
    ap = argparse.ArgumentParser(description="Генерация фидов версий и IPA-источников")
    ap.add_argument("--channel", required=True, choices=["stable", "beta", "rc", "preview"])
    ap.add_argument("--version", required=True, help="Версия из tools/versioning.py")
    ap.add_argument("--build-number", type=int, required=True, help="epoch-код сборки")
    ap.add_argument("--commit-sha", default="")
    ap.add_argument("--out-dir", default="dist_version")
    ap.add_argument("--app-version-file", default=APP_VERSION_FILE)
    args = ap.parse_args()

    repo, changelog, critical, min_supported = parse_app_version(args.app_version_file)
    channel = args.channel
    is_stable = channel == "stable"
    is_preview = channel == "preview"
    # beta и rc пишут один и тот же фид: бета-канал приложения читает beta.json
    is_beta = channel in ("beta", "rc")
    urls = asset_urls(repo, channel, args.version)

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    written = []

    # 1) Канальные фиды обновлений приложения
    if is_stable or is_beta:
        feed = {
            "version": args.version,
            "build": args.build_number,
            "critical": critical and is_stable,
            "min_supported_build": min_supported if is_stable else 1,
            "changelog": changelog,
            "download_url": urls["download_url"],
            "apk_url": urls["apk_url"],
            "ipa_url": urls["ipa_url"],
            "channel": "stable" if is_stable else channel,
            "prerelease": not is_stable,
            "updated_at": datetime.now(timezone.utc).isoformat(),
        }
        if args.commit_sha:
            feed["commit_sha"] = args.commit_sha
        feed_name = "version.json" if is_stable else "beta.json"
        (out_dir / feed_name).write_text(
            json.dumps(feed, indent=2, ensure_ascii=False), encoding="utf-8")
        written.append(feed_name)

    # 2) GBox-источники: apps.json — стабильный, apps-beta.json — всегда последняя сборка
    if is_stable:
        (out_dir / "apps.json").write_text(
            json.dumps(build_source(repo, channel, args.version, urls["ipa_url"]),
                       indent=2, ensure_ascii=False), encoding="utf-8")
        written.append("apps.json")
    # apps-beta.json пишется ВСЕМИ каналами — «самая свежая сборка» для GBox
    (out_dir / "apps-beta.json").write_text(
        json.dumps(build_source(repo, channel, args.version, urls["ipa_url"]),
                   indent=2, ensure_ascii=False), encoding="utf-8")
    written.append("apps-beta.json")

    print(f"Generated in {out_dir}: {', '.join(written)}")


if __name__ == "__main__":
    main()
