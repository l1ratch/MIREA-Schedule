#!/usr/bin/env python3
"""
tools/publish_release.py — публикация GitHub Release по каналу.

  --channel stable  : иммутабельный релиз v{version} (marked latest), файлы НЕ перезаписываются
  --channel beta|rc : иммутабельный prerelease v{version}
  --channel preview : один rolling prerelease с тегом `preview`, файлы заменяются (--clobber)

Архив сборок (build-archive) удалён: история живёт в Actions, тестовые
сборки — в rolling preview, релизы — в тегах. Вызывается ТОЛЬКО из
релизного и preview воркфлоу, никогда — из contributor-сборщика.
"""

import argparse
import os
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

APP_VERSION_FILE = "shared/src/commonMain/kotlin/com/jetbrains/kmpapp/data/model/AppVersion.kt"


def parse_repo(app_version_file):
    content = Path(app_version_file).read_text(encoding="utf-8")
    m = re.search(r'const\s+val\s+GITHUB_REPO\s*=\s*"([^"]+)"', content)
    return m.group(1) if m else "l1ratch/MIREA-Schedule"


def parse_changelog(app_version_file):
    content = Path(app_version_file).read_text(encoding="utf-8")
    m = (re.search(r'const\s+val\s+CHANGELOG\s*=\s*"""([\s\S]*?)"""', content)
         or re.search(r'const\s+val\s+CHANGELOG\s*=\s*"([^"]*)"', content))
    return m.group(1).strip() if m else ""


def gh(*args):
    return subprocess.run(["gh", *args], capture_output=True, text=True)


def collect_files(apk, ipa):
    files = [p for p in (apk, ipa) if p and os.path.exists(p)]
    for p in files:
        print(f"Found: {p}")
    return files


def build_preview_notes(version, build_number, commit_sha, date):
    lines = [
        f"### 🚧 Rolling-сборка ветки `main`",
        "",
        f"**Версия:** `{version}` · **сборка:** `#{build_number}` · **коммит:** `{commit_sha or '—'}` · {date}",
        "",
        "Файлы **заменяются** при каждом push в `main` — здесь всегда последняя тестовая сборка.",
        "Обычным пользователям этот канал не виден: автообновление читает только стабильный `version.json`.",
        "Opt-in проверка тестовых обновлений — через отладочное меню приложения.",
    ]
    return "\n".join(lines)


def build_release_notes(version, build_number, date, repo, tag, files, changelog):
    lines = [
        f"### 📱 MIREA Schedule v{version}",
        f"**Номер сборки:** `#{build_number}` · {date}",
        ""
    ]
    if changelog:
        lines += ["#### 📝 Что нового:", changelog, ""]
    lines.append("#### 📥 Установочные файлы:")
    for p in files:
        name = os.path.basename(p)
        platform = "Android" if p.endswith(".apk") else "iOS"
        lines.append(
            f"* **{platform}:** "
            f"[`{name}`](https://github.com/{repo}/releases/download/{tag}/{name})"
        )
    return "\n".join(lines)


def publish_preview(args, files, date):
    tag = "preview"
    title = f"🧪 Тестовая сборка {args.version} (сборка #{args.build_number})"
    notes = build_preview_notes(args.version, args.build_number, args.commit_sha, date)
    Path("release_notes.md").write_text(notes, encoding="utf-8")

    if args.dry_run:
        print(f"[DRY RUN] preview release '{tag}' с файлами {files}")
        return

    print(f"==> Publishing rolling preview release '{tag}'...")
    existing = gh("release", "view", tag)
    if existing.returncode == 0:
        subprocess.run([
            "gh", "release", "edit", tag,
            "--title", title, "-F", "release_notes.md", "--prerelease"
        ], check=True)
        subprocess.run([
            "gh", "release", "upload", tag, *files, "--clobber"
        ], check=True)
    else:
        subprocess.run([
            "gh", "release", "create", tag, *files,
            "--title", title, "-F", "release_notes.md", "--prerelease"
        ], check=True)


def publish_immutable(args, files, date):
    tag = f"v{args.version}"
    title = f"MIREA Schedule v{args.version} (сборка #{args.build_number})"
    prerelease_flag = "--prerelease" if args.channel != "stable" else "--latest"

    repo = parse_repo(args.app_version_file)
    changelog = parse_changelog(args.app_version_file)
    notes = build_release_notes(args.version, args.build_number, date, repo, tag, files, changelog)
    Path("release_notes.md").write_text(notes, encoding="utf-8")

    if args.dry_run:
        print(f"[DRY RUN] release '{tag}' ({args.channel}) с файлами {files}")
        return

    existing = gh("release", "view", tag)
    if existing.returncode == 0:
        sys.exit(
            f"ERROR: Release '{tag}' already exists. Stable/beta/rc releases "
            f"are immutable: bump the version or delete the release manually."
        )
    print(f"==> Publishing release '{tag}' ({args.channel})...")
    subprocess.run([
        "gh", "release", "create", tag, *files,
        "--title", title, "-F", "release_notes.md", prerelease_flag
    ], check=True)


def parse_args():
    ap = argparse.ArgumentParser(description="Публикация GitHub Release по каналу")
    ap.add_argument("--channel", required=True,
                    choices=["stable", "beta", "rc", "preview"])
    ap.add_argument("--version", required=True, help="Версия из tools/versioning.py")
    ap.add_argument("--build-number", required=True, help="github.run_id")
    ap.add_argument("--commit-sha", default="")
    ap.add_argument("--apk", default=None, help="Путь к APK")
    ap.add_argument("--ipa", default=None, help="Путь к IPA")
    ap.add_argument("--app-version-file", default=APP_VERSION_FILE)
    ap.add_argument("--dry-run", action="store_true")
    return ap.parse_args()


def main():
    args = parse_args()
    files = collect_files(args.apk, args.ipa)
    if not files:
        print("Нет APK/IPA для публикации — выходим без ошибок.")
        return

    date = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    if args.channel == "preview":
        publish_preview(args, files, date)
    else:
        publish_immutable(args, files, date)

    print("==> Release publishing completed successfully!")


if __name__ == "__main__":
    main()
