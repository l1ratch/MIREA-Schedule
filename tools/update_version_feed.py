#!/usr/bin/env python3
"""
tools/update_version_feed.py
Extracts version metadata from AppVersion.kt and generates version.json
for hosting on the gh-pages branch.
"""

import os
import re
import sys
import json
import argparse
from datetime import datetime, timezone

def parse_app_version(app_version_path: str):
    with open(app_version_path, 'r', encoding='utf-8') as f:
        content = f.read()

    version_name_match = re.search(r'const\s+val\s+VERSION_NAME\s*=\s*"([^"]+)"', content)
    build_num_match = re.search(r'const\s+val\s+BUILD_NUMBER\s*=\s*(\d+)', content)
    is_critical_match = re.search(r'const\s+val\s+IS_CRITICAL\s*=\s*(true|false)', content)
    min_supported_match = re.search(r'const\s+val\s+MIN_SUPPORTED_BUILD\s*=\s*(\d+)', content)
    repo_match = re.search(r'const\s+val\s+GITHUB_REPO\s*=\s*"([^"]+)"', content)

    # Changelog can be multiline or single line string
    changelog_match = re.search(r'const\s+val\s+CHANGELOG\s*=\s*"""([\s\S]*?)"""', content)
    if not changelog_match:
        changelog_match = re.search(r'const\s+val\s+CHANGELOG\s*=\s*"([^"]*)"', content)

    version_name = version_name_match.group(1) if version_name_match else "1.1.0"
    build_number = int(build_num_match.group(1)) if build_num_match else 1
    is_critical = is_critical_match.group(1) == 'true' if is_critical_match else False
    min_supported = int(min_supported_match.group(1)) if min_supported_match else 1
    repo = repo_match.group(1) if repo_match else "l1ratch/MIREA-Schedule"
    changelog = changelog_match.group(1).strip() if changelog_match else ""

    return {
        "version": version_name,
        "build": build_number,
        "critical": is_critical,
        "min_supported_build": min_supported,
        "repo": repo,
        "changelog": changelog
    }

def main():
    parser = argparse.ArgumentParser(description="Generate version.json from AppVersion.kt")
    parser.add_argument("--build-number", type=int, default=None, help="Override build number (e.g. from CI run_number)")
    parser.add_argument("--out-dir", default="dist_version", help="Output directory for version.json")
    parser.add_argument("--app-version-file", default="shared/src/commonMain/kotlin/com/jetbrains/kmpapp/data/model/AppVersion.kt")
    args = parser.parse_args()

    meta = parse_app_version(args.app_version_file)
    if args.build_number is not None:
        meta["build"] = args.build_number
    elif os.environ.get("GITHUB_RUN_NUMBER"):
        try:
            meta["build"] = int(os.environ["GITHUB_RUN_NUMBER"])
        except ValueError:
            pass

    repo = meta.pop("repo")
    meta["download_url"] = f"https://github.com/{repo}/releases/latest"
    meta["updated_at"] = datetime.now(timezone.utc).isoformat()

    os.makedirs(args.out_dir, exist_ok=True)
    out_path = os.path.join(args.out_dir, "version.json")

    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2, ensure_ascii=False)

    print(f"Generated version feed at {out_path}:")
    print(json.dumps(meta, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
