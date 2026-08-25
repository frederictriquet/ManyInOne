#!/usr/bin/env python3
"""Fails when R8 stripped a ViewModel constructor from the release APK.

ViewModelProvider instantiates ViewModels reflectively, so R8 sees no caller for
their constructors and may remove them while keeping the class. The app then dies
with NoSuchMethodException the first time the screen is opened -- a crash that no
debug build can reproduce.

Usage: check_release_viewmodels.py <apk> [source-root]
"""

import re
import subprocess
import sys
from pathlib import Path

CLASS_RE = re.compile(
    r"^\s*(?:internal\s+|public\s+|private\s+)?class\s+(\w+)\s*(?:@\w+\s+constructor\s*)?\("
    r"[^)]*\)\s*:\s*(?:\w+\.)*(?:Android)?ViewModel\b",
    re.MULTILINE,
)
CTOR_RE = re.compile(r"name\s+:\s+'<init>'\s*\n\s*type\s+:\s+'([^']+)'")


def find_dexdump() -> str:
    roots = [Path(p) for p in (sys.argv[3:] or []) if p]
    for env in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        import os

        if os.environ.get(env):
            roots.append(Path(os.environ[env]))
    roots.append(Path.home() / "Library/Android/sdk")
    roots.append(Path("/usr/local/lib/android/sdk"))
    for root in roots:
        found = sorted((root / "build-tools").glob("*/dexdump"))
        if found:
            return str(found[-1])
    sys.exit("ERROR: dexdump not found; set ANDROID_HOME to the Android SDK.")


def source_viewmodels(source_root: Path) -> dict[str, str]:
    """Maps simple class name -> fully qualified name, for every ViewModel found."""
    result = {}
    for kt in source_root.rglob("*.kt"):
        text = kt.read_text(encoding="utf-8", errors="replace")
        package = re.search(r"^package\s+([\w.]+)", text, re.MULTILINE)
        if not package:
            continue
        for name in CLASS_RE.findall(text):
            result[name] = f"{package.group(1)}.{name}"
    return result


def main() -> int:
    apk = Path(sys.argv[1] if len(sys.argv) > 1 else "app/build/outputs/apk/release/app-release.apk")
    source_root = Path(sys.argv[2] if len(sys.argv) > 2 else "app/src/main")

    if not apk.is_file():
        sys.exit(f"ERROR: APK not found: {apk}")

    viewmodels = source_viewmodels(source_root)
    if not viewmodels:
        sys.exit(f"ERROR: no ViewModel found under {source_root}; the detection regex is stale.")

    print(f"Checking {len(viewmodels)} ViewModel(s) in {apk}")

    dump = subprocess.run(
        [find_dexdump(), "-d", str(apk)],
        capture_output=True,
        text=True,
        errors="replace",
    )
    if dump.returncode != 0:
        sys.exit(f"ERROR: dexdump failed:\n{dump.stderr[-2000:]}")

    blocks = {}
    for block in dump.stdout.split("Class descriptor  : "):
        if block.startswith("'L"):
            descriptor = block.split("'")[1]
            blocks[descriptor[1:-1].replace("/", ".")] = block

    failures = []
    for simple, fqn in sorted(viewmodels.items()):
        block = blocks.get(fqn)
        if block is None:
            failures.append(f"{fqn}: class absent from the release dex (obfuscated or removed)")
            continue
        ctors = CTOR_RE.findall(block)
        if not ctors:
            failures.append(f"{fqn}: no constructor left in the release dex")
            continue
        print(f"  OK  {fqn}  ({len(ctors)} constructor(s))")

    if failures:
        print("\nFAIL: R8 stripped ViewModel metadata needed at runtime:", file=sys.stderr)
        for f in failures:
            print(f"  - {f}", file=sys.stderr)
        print(
            "\nThe app would crash with NoSuchMethodException when the screen opens.\n"
            "Check the -keep rule for androidx.lifecycle.ViewModel in app/proguard-rules.pro.",
            file=sys.stderr,
        )
        return 1

    print("\nAll ViewModels survived R8 with their constructors.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
