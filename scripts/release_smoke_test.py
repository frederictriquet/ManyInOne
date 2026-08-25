#!/usr/bin/env python3
"""Installs the release APK on a connected device and walks its bottom tabs.

Debug builds cannot reproduce R8-only failures (stripped constructors, shrunk
resources, broken reflection), so the minified artifact has to be exercised for
real. Any crash, or the process disappearing, fails the run.

Usage: release_smoke_test.py <apk> [--serial SERIAL] [--package NAME]
"""

import argparse
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = "fr.triquet.manyinone"
ACTIVITY = ".MainActivity"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk")
    parser.add_argument("--serial")
    parser.add_argument("--package", default=PACKAGE)
    args = parser.parse_args()

    def adb(*cmd, check=True, timeout=180):
        base = ["adb"] + (["-s", args.serial] if args.serial else [])
        res = subprocess.run(base + list(cmd), capture_output=True, text=True,
                             errors="replace", timeout=timeout)
        if check and res.returncode != 0:
            sys.exit(f"ERROR: adb {' '.join(cmd)} failed:\n{res.stderr.strip()}")
        return res.stdout

    def pid():
        return adb("shell", "pidof", args.package, check=False).strip()

    def crash_log():
        return adb("logcat", "-b", "crash", "-d", check=False).strip()

    def fail(message):
        print(f"\nFAIL: {message}", file=sys.stderr)
        log = crash_log()
        if log:
            print("\n--- crash buffer ---", file=sys.stderr)
            print("\n".join(log.splitlines()[:60]), file=sys.stderr)
        else:
            print("(crash buffer empty; process died without a Java exception)", file=sys.stderr)
        return 1

    print(f"Installing {args.apk}")
    # Emulators are slow to write a 30 MB APK; be generous with the timeout.
    out = adb("install", "-r", "-d", args.apk, timeout=900)
    if "Success" not in out:
        sys.exit(f"ERROR: install failed:\n{out}")

    adb("logcat", "-c", "-b", "all", check=False)
    adb("shell", "am", "force-stop", args.package, check=False)
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    # Without this the app starts behind the lock screen and nothing is tappable.
    adb("shell", "wm", "dismiss-keyguard", check=False)
    time.sleep(1)
    adb("shell", "am", "start", "-n", f"{args.package}/{ACTIVITY}")
    time.sleep(8)

    launch_pid = pid()
    if not launch_pid:
        return fail("the app died during startup")
    print(f"Started, pid={launch_pid}")

    tabs, foreground = discover_tabs(adb, args.package)
    if not foreground:
        sys.exit(
            f"ERROR: '{args.package}' is not the foreground app; the UI dump belongs to "
            "something else. A locked screen with a PIN is the usual cause -- unlock the "
            "device, or run this on an emulator without a keyguard."
        )
    if len(tabs) < 2:
        sys.exit(
            f"ERROR: found {len(tabs)} bottom-bar tab(s); the smoke test would pass "
            "without navigating anywhere. Check the UI dump."
        )
    print(f"Tabs: {', '.join(label for label, _, _ in tabs)}")

    # Two passes: the first creates each ViewModel, the second re-enters them.
    for round_index in (1, 2):
        for label, x, y in tabs:
            adb("shell", "input", "tap", str(x), str(y), check=False)
            time.sleep(3)
            current = pid()
            if not current:
                return fail(f"tab '{label}' (pass {round_index}) killed the process")
            if current != launch_pid:
                return fail(
                    f"tab '{label}' (pass {round_index}) restarted the process "
                    f"({launch_pid} -> {current})"
                )
            print(f"  pass {round_index}: {label} OK")

    if crash_log():
        return fail("the crash buffer is not empty after navigation")

    print("\nSmoke test passed: every tab opened without crashing.")
    return 0


def discover_tabs(adb, package):
    """Returns ([(label, x, y)], foreground) for the bottom navigation bar.

    Tab items carry neither text nor content-desc: the label sits on a child node,
    and the selected item is not even clickable. So the bar is located through its
    clickable children, then every sibling in that container is treated as a tab.
    """
    adb("shell", "uiautomator", "dump", "/sdcard/smoke.xml", check=False)
    xml = adb("shell", "cat", "/sdcard/smoke.xml", check=False)
    adb("shell", "rm", "-f", "/sdcard/smoke.xml", check=False)

    start = xml.find("<?xml")
    if start == -1:
        sys.exit("ERROR: could not read the UI hierarchy dump.")
    try:
        root = ET.fromstring(xml[start:])
    except ET.ParseError as exc:
        sys.exit(f"ERROR: malformed UI dump: {exc}")

    parents = {child: parent for parent in root.iter() for child in parent}

    def box(node):
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
        return tuple(map(int, m.groups())) if m else None

    screen_height = max((box(n)[3] for n in root.iter("node") if box(n)), default=0)
    foreground = any(n.get("package") == package for n in root.iter("node"))
    if not screen_height:
        return [], foreground

    # Clickable nodes sitting in the bottom eighth of the screen: the tab items.
    anchors = [
        n for n in root.iter("node")
        if n.get("clickable") == "true" and n.get("package") == package
        and box(n) and (box(n)[1] + box(n)[3]) / 2 >= screen_height * 0.85
    ]
    if not anchors:
        return [], foreground

    bar = parents.get(anchors[0])
    if bar is None:
        return [], foreground

    def label_of(node):
        for descendant in node.iter("node"):
            text = (descendant.get("text") or descendant.get("content-desc") or "").strip()
            if text:
                return text
        return None

    tabs = []
    for item in bar:
        bounds = box(item)
        if not bounds:
            continue
        x1, y1, x2, y2 = bounds
        tabs.append((label_of(item) or f"tab@{(x1 + x2) // 2}", (x1 + x2) // 2, (y1 + y2) // 2))
    return tabs, foreground


if __name__ == "__main__":
    sys.exit(main())
