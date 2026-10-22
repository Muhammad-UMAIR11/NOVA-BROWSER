#!/usr/bin/env bash
set -e

# Change to the directory of this script
cd "$(dirname "$0")"

echo "====================================================================="
echo "              Video Browser - Android APK Builder"
echo "====================================================================="
echo ""

# 1. Check for Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java was not found in your PATH."
    echo "Please install Java JDK 17+ and set JAVA_HOME."
    exit 1
fi

echo "[*] Java version:"
java -version 2>&1 | head -n 1
echo ""

# 2. Select Gradle command
GRADLE_CMD=""
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    GRADLE_CMD="./gradlew"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
else
    echo "[ERROR] Neither ./gradlew nor system gradle was found."
    exit 1
fi

# 3. Assemble Debug APK
echo "[*] Compiling APK ($GRADLE_CMD assembleDebug)..."
$GRADLE_CMD assembleDebug

APK_SOURCE="app/build/outputs/apk/debug/app-debug.apk"
APK_DEST="VideoBrowser-debug.apk"

if [ -f "$APK_SOURCE" ]; then
    cp -f "$APK_SOURCE" "$APK_DEST"
    echo ""
    echo "====================================================================="
    echo "[SUCCESS] APK generated successfully!"
    echo "Location: $(pwd)/$APK_DEST"
    echo "Size: $(du -h "$APK_DEST" | cut -f1)"
    echo "====================================================================="
else
    echo "[WARNING] Expected output at $APK_SOURCE not found."
fi
