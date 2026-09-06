#!/usr/bin/env python3
"""
tools/publish_release.py
Automates GitHub Release publishing and build archiving.

Logic:
1. Parses AppVersion.kt to get VERSION_NAME, BUILD_NUMBER, CHANGELOG, GITHUB_REPO.
2. Formats release notes.
3. Prepares:
   - dist_latest/ (Schedule-MIREA.apk, Schedule-MIREA.ipa)
   - dist_archive/ (Schedule-MIREA-v{version}-b{build}.apk, Schedule-MIREA-v{version}-b{build}.ipa)
4. Uploads to pre-release 'build-archive' (keeps historical builds).
5. Uploads / overwrites in main release 'v{version}' (clean generic filenames).
"""

import os
import sys
import shutil
import argparse
import subprocess
from pathlib import Path

def parse_app_version(app_version_path: str):
    import re
    with open(app_version_path, 'r', encoding='utf-8') as f:
        content = f.read()

    version_name_match = re.search(r'const\s+val\s+VERSION_NAME\s*=\s*"([^"]+)"', content)
    build_num_match = re.search(r'const\s+val\s+BUILD_NUMBER\s*=\s*(\d+)', content)
    repo_match = re.search(r'const\s+val\s+GITHUB_REPO\s*=\s*"([^"]+)"', content)

    changelog_match = re.search(r'const\s+val\s+CHANGELOG\s*=\s*"""([\s\S]*?)"""', content)
    if not changelog_match:
        changelog_match = re.search(r'const\s+val\s+CHANGELOG\s*=\s*"([^"]*)"', content)

    version_name = version_name_match.group(1) if version_name_match else "1.0.0"
    build_number = int(build_num_match.group(1)) if build_num_match else 1
    repo = repo_match.group(1) if repo_match else "l1ratch/MIREA-Schedule"
    changelog = changelog_match.group(1).strip() if changelog_match else ""

    return {
        "version": version_name,
        "build": build_number,
        "repo": repo,
        "changelog": changelog
    }

def main():
    parser = argparse.ArgumentParser(description="Publish release and archive builds to GitHub Releases")
    parser.add_argument("--build-number", type=int, default=None, help="CI build run number")
    parser.add_argument("--apk", type=str, default=None, help="Path to built APK")
    parser.add_argument("--ipa", type=str, default=None, help="Path to built IPA")
    parser.add_argument("--app-version-file", default="shared/src/commonMain/kotlin/com/jetbrains/kmpapp/data/model/AppVersion.kt")
    parser.add_argument("--dry-run", action="store_true", help="Do not call gh CLI, only prepare assets")
    args = parser.parse_args()

    meta = parse_app_version(args.app_version_file)
    version = meta["version"]
    build_num = args.build_number if args.build_number is not None else meta["build"]
    repo = meta["repo"]
    changelog = meta["changelog"]

    print(f"==> Processing release for {repo}: Version {version}, Build #{build_num}")

    dist_latest = Path("dist_latest")
    dist_archive = Path("dist_archive")
    shutil.rmtree(dist_latest, ignore_errors=True)
    shutil.rmtree(dist_archive, ignore_errors=True)
    dist_latest.mkdir(parents=True, exist_ok=True)
    dist_archive.mkdir(parents=True, exist_ok=True)

    has_files = False

    # Check APK
    apk_src = None
    if args.apk and os.path.exists(args.apk):
        apk_src = args.apk
    elif os.path.exists("download_apk/Schedule-MIREA.apk"):
        apk_src = "download_apk/Schedule-MIREA.apk"

    if apk_src:
        print(f"Found APK: {apk_src}")
        shutil.copy2(apk_src, dist_latest / "Schedule-MIREA.apk")
        shutil.copy2(apk_src, dist_archive / f"Schedule-MIREA-v{version}-b{build_num}.apk")
        has_files = True

    # Check IPA
    ipa_src = None
    if args.ipa and os.path.exists(args.ipa):
        ipa_src = args.ipa
    elif os.path.exists("download_ipa/Schedule-MIREA.ipa"):
        ipa_src = "download_ipa/Schedule-MIREA.ipa"

    if ipa_src:
        print(f"Found IPA: {ipa_src}")
        shutil.copy2(ipa_src, dist_latest / "Schedule-MIREA.ipa")
        shutil.copy2(ipa_src, dist_archive / f"Schedule-MIREA-v{version}-b{build_num}.ipa")
        has_files = True

    if not has_files:
        print("No APK or IPA found to publish. Exiting.")
        return

    if args.dry_run:
        print("[DRY RUN] Latest assets:", list(dist_latest.glob('*')))
        print("[DRY RUN] Archive assets:", list(dist_archive.glob('*')))
        return

    # 1. Update / Create Pre-release 'build-archive'
    archive_files = [str(p) for p in dist_archive.glob("*")]
    if archive_files:
        print("==> Uploading to 'build-archive' Pre-release...")
        check_archive = subprocess.run(["gh", "release", "view", "build-archive"], capture_output=True, text=True)
        if check_archive.returncode == 0:
            print("Updating existing 'build-archive' release...")
            subprocess.run(["gh", "release", "upload", "build-archive", *archive_files, "--clobber"], check=True)
        else:
            print("Creating 'build-archive' pre-release...")
            archive_notes = (
                "### 🗄️ Архив сборок приложения MIREA Schedule\n\n"
                "В этом предрелизе автоматически сохраняются все промежуточные и исторические сборки приложения.\n\n"
                "💡 Актуальную стабильную версию всегда можно скачать на [странице последнего релиза](../../releases/latest).\n"
            )
            Path("archive_notes.md").write_text(archive_notes, encoding="utf-8")
            subprocess.run([
                "gh", "release", "create", "build-archive", *archive_files,
                "--title", "📦 Архив промежуточных сборок (Build Archive)",
                "-F", "archive_notes.md",
                "--prerelease"
            ], check=True)

    # 2. Update / Create Main Release 'v{version}'
    latest_files = [str(p) for p in dist_latest.glob("*")]
    if latest_files:
        tag = f"v{version}"
        title = f"MIREA Schedule v{version} (Сборка #{build_num})"

        notes_lines = [
            f"### 📱 MIREA Schedule v{version}",
            f"**Номер сборки:** `#{build_num}`",
            ""
        ]
        if changelog:
            notes_lines.append("#### 📝 Что нового:")
            notes_lines.append(changelog)
            notes_lines.append("")

        notes_lines.append("#### 📥 Установочные файлы:")
        if (dist_latest / "Schedule-MIREA.apk").exists():
            notes_lines.append(f"* **Android:** [`Schedule-MIREA.apk`](https://github.com/{repo}/releases/download/{tag}/Schedule-MIREA.apk)")
        if (dist_latest / "Schedule-MIREA.ipa").exists():
            notes_lines.append(f"* **iOS:** [`Schedule-MIREA.ipa`](https://github.com/{repo}/releases/download/{tag}/Schedule-MIREA.ipa)")

        notes_lines.append("")
        notes_lines.append("---")
        notes_lines.append(f"> 🗄️ Предыдущие и промежуточные сборки доступны в [Архиве сборок](https://github.com/{repo}/releases/tag/build-archive).")

        notes_content = "\n".join(notes_lines)
        Path("release_notes.md").write_text(notes_content, encoding="utf-8")

        print(f"==> Publishing Release '{tag}'...")
        check_release = subprocess.run(["gh", "release", "view", tag], capture_output=True, text=True)
        if check_release.returncode == 0:
            print(f"Release '{tag}' exists. Updating title, notes, and clobbering latest files...")
            subprocess.run([
                "gh", "release", "edit", tag,
                "--title", title,
                "-F", "release_notes.md",
                "--latest"
            ], check=True)
            subprocess.run(["gh", "release", "upload", tag, *latest_files, "--clobber"], check=True)
        else:
            print(f"Creating new release '{tag}'...")
            subprocess.run([
                "gh", "release", "create", tag, *latest_files,
                "--title", title,
                "-F", "release_notes.md",
                "--latest"
            ], check=True)

    print("==> Release publishing completed successfully!")

if __name__ == '__main__':
    main()