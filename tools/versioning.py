#!/usr/bin/env python3
"""
tools/versioning.py — единый генератор версий для всех CI-каналов.

Каналы и форматы:
  stable   тег v26.10 / v26.10.1     → версия 26.10 / 26.10.1
  beta|rc  тег v26.10-beta.1 / -rc.1 → версия 26.10-beta.1 (prerelease)
  dev      push в main               → {RELEASE_VERSION}-dev.<run_number> (rolling preview)
  contrib  ручная сборка ветки      → {RELEASE_VERSION}-contrib.<run_number>

Числовой BUILD_NUMBER (versionCode / CFBundleVersion) = github.run_id:
один и монотонный для всех каналов и воркфлоу, поэтому любая свежая
сборка всегда устанавливается поверх любой старой.

Команды:
  resolve — вычислить версию и вывести в $GITHUB_OUTPUT (файлы не трогает)
  prepare — resolve + патч AppVersion.kt / androidApp/build.gradle.kts / Info.plist

Версия в репозитории живёт ТОЛЬКО в AppVersion.RELEASE_VERSION (линия
разработки, YY.X). Полные версии с суффиксами в репо не коммитятся — их
подставляет CI. Никогда не хардкодь версии в workflow напрямую.
"""

import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

APP_VERSION_FILE = "shared/src/commonMain/kotlin/com/jetbrains/kmpapp/data/model/AppVersion.kt"
GRADLE_FILE = "androidApp/build.gradle.kts"
PLIST_FILE = "iosApp/iosApp/Info.plist"

# v26.10 | v26.10.1 | v26.10-beta.1 | v26.10-rc.2
TAG_RE = re.compile(r"^v(\d+\.\d+(?:\.\d+)?)(?:-(beta|rc)\.(\d+))?$")


def read_release_version():
    content = Path(APP_VERSION_FILE).read_text(encoding="utf-8")
    m = (re.search(r'const\s+val\s+RELEASE_VERSION\s*=\s*"([^"]+)"', content)
         or re.search(r'const\s+val\s+VERSION_NAME\s*=\s*"([^"]+)"', content))
    if not m:
        sys.exit(f"Не найдена версия в {APP_VERSION_FILE}")
    return m.group(1).strip()


def short_sha():
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"], text=True
        ).strip()
    except Exception:
        sha = os.environ.get("GITHUB_SHA", "local")
        return sha[:8] if sha else "local"


def resolve(channel, tag, run_number, run_id):
    if tag:
        m = TAG_RE.match(tag.strip())
        if not m:
            sys.exit(
                f"Некорректный тег '{tag}'. Ожидается v26.10, v26.10.1, "
                f"v26.10-beta.1 или v26.10-rc.1"
            )
        version = m.group(1)
        channel = m.group(2) or "stable"
        if m.group(2):
            version += f"-{m.group(2)}.{m.group(3)}"
    elif channel == "dev":
        version = f"{read_release_version()}-dev.{run_number}"
    elif channel == "contrib":
        version = f"{read_release_version()}-contrib.{run_number}"
    else:
        sys.exit("Для канала stable/beta/rc нужен --tag, для dev/contrib --channel")

    return {
        "version": version,
        "channel": channel,
        "prerelease": "true" if channel != "stable" else "false",
        "build_id": str(run_id),
        "commit_sha": short_sha(),
    }


def numeric_core(version):
    """26.10-dev.151 → 26.10 (для CFBundleShortVersionString, iOS любит числовые)."""
    return version.split("+")[0].split("-")[0]


def patch_files(info):
    # 1) AppVersion.kt: полный VERSION_NAME + канал + числовой код + sha
    path = Path(APP_VERSION_FILE)
    c = path.read_text(encoding="utf-8")
    c = re.sub(
        r'const val VERSION_NAME = (?:RELEASE_VERSION|"[^"]*")',
        f'const val VERSION_NAME = "{info["version"]}"', c)
    c = re.sub(r'const val BUILD_CHANNEL = "[^"]*"',
               f'const val BUILD_CHANNEL = "{info["channel"]}"', c)
    c = re.sub(r'const val BUILD_NUMBER = \d+',
               f'const val BUILD_NUMBER = {info["build_id"]}', c)
    c = re.sub(r'const val COMMIT_SHA = "[^"]*"',
               f'const val COMMIT_SHA = "{info["commit_sha"]}"', c)
    path.write_text(c, encoding="utf-8")
    print(f"patched {APP_VERSION_FILE}")

    # 2) androidApp/build.gradle.kts: versionName (versionCode берётся из env BUILD_NUMBER)
    path = Path(GRADLE_FILE)
    g = path.read_text(encoding="utf-8")
    g = re.sub(r'versionName = "[^"]*"',
               f'versionName = "{info["version"]}"', g)
    path.write_text(g, encoding="utf-8")
    print(f"patched {GRADLE_FILE}")

    # 3) Info.plist: числовая база версии + числовой код сборки
    path = Path(PLIST_FILE)
    p = path.read_text(encoding="utf-8")
    p = re.sub(r'(<key>CFBundleShortVersionString</key>\s*<string>)[^<]+(</string>)',
               rf'\g<1>{numeric_core(info["version"])}\g<2>', p)
    p = re.sub(r'(<key>CFBundleVersion</key>\s*<string>)[^<]+(</string>)',
               rf'\g<1>{info["build_id"]}\g<2>', p)
    path.write_text(p, encoding="utf-8")
    print(f"patched {PLIST_FILE}")


def emit(info):
    out_file = os.environ.get("GITHUB_OUTPUT")
    if out_file:
        with open(out_file, "a", encoding="utf-8") as f:
            for k, v in info.items():
                f.write(f"{k}={v}\n")
    print("==========================================")
    print("VERSION RESOLVED")
    print(json.dumps(info, indent=2, ensure_ascii=False))
    print("==========================================")


def main():
    ap = argparse.ArgumentParser(description="Единый генератор версий CI")
    ap.add_argument("command", choices=["resolve", "prepare"])
    ap.add_argument("--channel", default=None,
                    help="dev | contrib (для тегов не нужен — канал выводится из тега)")
    ap.add_argument("--tag", default=None,
                    help="Тег релиза: v26.10, v26.10.1, v26.10-beta.1, v26.10-rc.1")
    ap.add_argument("--run-number", type=int, default=0,
                    help="github.run_number (номер pre/contrib-сборки)")
    ap.add_argument("--build-id", type=int,
                    default=int(os.environ.get("GITHUB_RUN_ID", "32")),
                    help="github.run_id — монотонный числовой код сборки")
    args = ap.parse_args()

    if not args.tag and not args.channel:
        sys.exit("Нужен --tag или --channel")

    info = resolve(args.channel, args.tag, args.run_number, args.build_id)
    if args.command == "prepare":
        patch_files(info)
    emit(info)


if __name__ == "__main__":
    main()
