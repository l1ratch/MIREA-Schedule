#!/usr/bin/env python3
"""
tools/update_version_feed.py — генерация канальных фидов и IPA-источников на gh-pages.

  --channel stable  : version.json (автообновление обычных пользователей)
                      + apps.json (AltStore-совместимый источник IPA)
  --channel preview : preview.json (opt-in тестовые обновления через отладочное меню)
                      + apps-beta.json (AltStore-совместимый источник тестовых IPA)

Вызывается ТОЛЬКО из релизного (stable) и preview-воркфлоу. Деплой на
gh-pages выполняет workflow (peaceiris/actions-gh-pages, keep_files).
Источники apps.json / apps-beta.json совместимы с AltStore-подобными
клиентами (SideStore, GBox и др.); один bundle id — подключай один
источник за раз.
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


def asset_urls(repo, channel):
    if channel == "stable":
        base = f"https://github.com/{repo}/releases"
        return {
            "download_url": f"{base}/latest",
            "apk_url": f"{base}/latest/download/Schedule-MIREA.apk",
            "ipa_url": f"{base}/latest/download/Schedule-MIREA.ipa",
        }
    base = f"https://github.com/{repo}/releases/download/preview"
    return {
        "download_url": f"{base}/MIREA-Schedule-preview.apk",
        "apk_url": f"{base}/MIREA-Schedule-preview.apk",
        "ipa_url": f"{base}/MIREA-Schedule-preview.ipa",
    }


def build_source(repo, channel, version, ipa_url):
    stable = channel == "stable"
    filename = "apps.json" if stable else "apps-beta.json"
    return {
        "name": "MIREA Schedule" + ("" if stable else " (Beta)"),
        "identifier": f"mirea-schedule-{'stable' if stable else 'beta'}",
        "sourceURL": f"https://raw.githubusercontent.com/{repo}/gh-pages/{filename}",
        "apps": [{
            "name": "Расписание МИРЭА",
            "bundleIdentifier": BUNDLE_ID,
            "developerName": "l1ratch",
            "localizedDescription": APP_DESCRIPTION,
            "iconURL": f"https://raw.githubusercontent.com/{repo}/main/shared/src/commonMain/composeResources/drawable/app_icon.png",
            "version": version,
            "versionDate": datetime.now(timezone.utc).isoformat(),
            "downloadURL": ipa_url,
            "tintColor": TINT_COLOR,
        }],
    }


def main():
    ap = argparse.ArgumentParser(description="Генерация фидов версий и IPA-источников")
    ap.add_argument("--channel", required=True, choices=["stable", "preview"])
    ap.add_argument("--version", required=True, help="Версия из tools/versioning.py")
    ap.add_argument("--build-number", type=int, required=True, help="github.run_id")
    ap.add_argument("--commit-sha", default="")
    ap.add_argument("--out-dir", default="dist_version")
    ap.add_argument("--app-version-file", default=APP_VERSION_FILE)
    args = ap.parse_args()

    repo, changelog, critical, min_supported = parse_app_version(args.app_version_file)
    urls = asset_urls(repo, args.channel)
    is_preview = args.channel == "preview"

    feed = {
        "version": args.version,
        "build": args.build_number,
        "critical": critical and not is_preview,
        "min_supported_build": min_supported if not is_preview else 1,
        "changelog": changelog,
        "download_url": urls["download_url"],
        "apk_url": urls["apk_url"],
        "ipa_url": urls["ipa_url"],
        "channel": args.channel,
        "prerelease": is_preview,
        "updated_at": datetime.now(timezone.utc).isoformat(),
    }
    if is_preview and args.commit_sha:
        feed["commit_sha"] = args.commit_sha

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    feed_name = "preview.json" if is_preview else "version.json"
    source_name = "apps-beta.json" if is_preview else "apps.json"
    (out_dir / feed_name).write_text(
        json.dumps(feed, indent=2, ensure_ascii=False), encoding="utf-8")
    (out_dir / source_name).write_text(
        json.dumps(build_source(repo, args.channel, args.version, urls["ipa_url"]),
                   indent=2, ensure_ascii=False), encoding="utf-8")

    print(f"Generated {out_dir / feed_name} and {out_dir / source_name}:")
    print(json.dumps(feed, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
