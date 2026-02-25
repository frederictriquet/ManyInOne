#!/bin/bash
set -e

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"

# Pick physical device over emulator; fail if none found
DEVICE=$(adb devices | grep -v emulator | grep -w device | head -1 | awk '{print $1}')
if [ -z "$DEVICE" ]; then
    echo "FAIL: No physical device found"
    adb devices -l
    exit 1
fi
echo "Using device: $DEVICE"
ADB="adb -s $DEVICE"

echo "Building release APK (minified with R8)..."
if ./gradlew :app:assembleRelease; then
    echo "OK: Build successful"
else
    echo "FAIL: Build failed"
    exit 1
fi

echo "Installing on $DEVICE..."
if $ADB install -r app/build/outputs/apk/release/app-release.apk; then
    echo "OK: APK installed"
else
    echo "FAIL: APK install failed"
    exit 1
fi

echo "Stopping app..."
if $ADB shell am force-stop fr.triquet.manyinone; then
    echo "OK: App stopped"
else
    echo "WARN: Could not stop app (not running?)"
fi

echo "Starting app..."
if $ADB shell am start -n fr.triquet.manyinone/.MainActivity; then
    echo "OK: App started"
else
    echo "FAIL: Could not start app"
    exit 1
fi

echo "Done."
