#!/bin/bash
set -e

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

echo "Building release APK (minified with R8)..."
./gradlew :app:assembleRelease

echo "Installing on connected device..."
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
adb install -r app/build/outputs/apk/release/app-release.apk

echo "Done."
