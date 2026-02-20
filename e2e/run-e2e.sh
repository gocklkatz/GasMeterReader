#!/usr/bin/env bash
# run-e2e.sh — Full cross-platform E2E test orchestrator
#
# What it does (in order):
#   1. Creates the AVD "GasMeterReader_API36" if avdmanager is available,
#      otherwise falls back to the first existing AVD in ~/.android/avd/
#   2. Starts the Android emulator if none is connected
#   3. Starts the Spring Boot backend on :8080
#   4. Starts the Angular dev server on :4200
#   5. Installs the emulatorDebug APK on the emulator
#   6. Runs UploadE2ETest (Android instrumented test)
#   7. Runs the Playwright web test
#   8. Cleans up background processes
#
# Usage:
#   cd GasMeterReader
#   ./e2e/run-e2e.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# ---- Environment setup -------------------------------------------------------
# Android SDK
: "${ANDROID_HOME:=$HOME/Library/Android/sdk}"
export ANDROID_HOME
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# Node.js via NVM (pick the highest installed version if NVM_NODE_VERSION unset)
if [[ -z "${NVM_NODE_VERSION:-}" ]]; then
    NVM_NODE_VERSION="$(ls "$HOME/.nvm/versions/node" 2>/dev/null | sort -V | tail -1)"
fi
if [[ -n "$NVM_NODE_VERSION" && -d "$HOME/.nvm/versions/node/$NVM_NODE_VERSION/bin" ]]; then
    export PATH="$HOME/.nvm/versions/node/$NVM_NODE_VERSION/bin:$PATH"
    echo ">>> Using Node $(node --version) from NVM ($NVM_NODE_VERSION)"
fi

# ---- State -------------------------------------------------------------------
BACKEND_PID=""
WEB_PID=""
EMULATOR_PID=""

# ---- Cleanup on exit ---------------------------------------------------------
cleanup() {
    echo ""
    echo ">>> Cleaning up..."
    [[ -n "$BACKEND_PID" ]] && kill "$BACKEND_PID" 2>/dev/null && echo "  Stopped backend (PID $BACKEND_PID)" || true
    [[ -n "$WEB_PID"     ]] && kill "$WEB_PID"     2>/dev/null && echo "  Stopped Angular (PID $WEB_PID)"   || true
    # Only stop the emulator if this script started it
    [[ -n "$EMULATOR_PID" ]] && kill "$EMULATOR_PID" 2>/dev/null && echo "  Stopped emulator (PID $EMULATOR_PID)" || true
}
trap cleanup EXIT

# ---- 1. Resolve which AVD to use ---------------------------------------------
AVD_NAME="GasMeterReader_API36"
ABI="$([ "$(uname -m)" = "arm64" ] && echo "arm64-v8a" || echo "x86_64")"
SYSTEM_IMAGE="system-images;android-36;google_apis;${ABI}"

if command -v avdmanager &>/dev/null; then
    echo ">>> Checking for AVD '$AVD_NAME'..."
    if ! avdmanager list avd 2>/dev/null | grep -q "Name: ${AVD_NAME}"; then
        echo ">>> Installing system image: $SYSTEM_IMAGE"
        sdkmanager "$SYSTEM_IMAGE"
        echo ">>> Creating AVD '$AVD_NAME' (API 36, ${ABI})"
        echo "no" | avdmanager create avd \
            --name    "$AVD_NAME" \
            --package "$SYSTEM_IMAGE" \
            --device  "pixel_6" \
            --force
        echo ">>> AVD '$AVD_NAME' created."
    else
        echo ">>> AVD '$AVD_NAME' already exists."
    fi
else
    # cmdline-tools not installed — find the first existing AVD
    echo ">>> avdmanager not found (cmdline-tools not installed)."
    echo ">>> Scanning ~/.android/avd/ for an existing AVD..."
    FOUND_AVD=""
    for ini in "$HOME/.android/avd/"*.ini; do
        [[ -f "$ini" ]] || continue
        FOUND_AVD="$(basename "$ini" .ini)"
        break
    done
    if [[ -z "$FOUND_AVD" ]]; then
        echo "Error: No AVD found and avdmanager is not available."
        echo "       Install 'Android SDK Command-line Tools' via"
        echo "       Android Studio → SDK Manager → SDK Tools."
        exit 1
    fi
    AVD_NAME="$FOUND_AVD"
    echo ">>> Using existing AVD: '$AVD_NAME'"
fi

# ---- 2. Start emulator if no emulator device is connected --------------------
EMULATOR="${ANDROID_HOME}/emulator/emulator"
[[ -x "$EMULATOR" ]] || { echo "Error: emulator binary not found at $EMULATOR"; exit 1; }

if "$ANDROID_HOME/platform-tools/adb" devices 2>/dev/null | grep -qE "^emulator-[0-9]+[[:space:]]+device$"; then
    echo ">>> Emulator already running — skipping start."
else
    echo ">>> Starting emulator '$AVD_NAME' (no audio, no snapshot)..."
    "$EMULATOR" -avd "$AVD_NAME" -no-audio -no-snapshot-load -no-snapshot-save &
    EMULATOR_PID=$!
    echo "    Emulator PID: $EMULATOR_PID"

    echo ">>> Waiting for emulator to come online..."
    "$ANDROID_HOME/platform-tools/adb" wait-for-device

    echo ">>> Waiting for full boot (sys.boot_completed=1)..."
    until "$ANDROID_HOME/platform-tools/adb" shell getprop sys.boot_completed 2>/dev/null | grep -q "^1$"; do
        sleep 3
    done
    echo ">>> Emulator fully booted."
fi

# ---- 3. Start Spring Boot backend --------------------------------------------
echo ">>> Starting Spring Boot backend..."
cd "$REPO_ROOT/backend"
./mvnw spring-boot:run > /tmp/gas-meter-backend.log 2>&1 &
BACKEND_PID=$!
echo "    PID: $BACKEND_PID  |  log: /tmp/gas-meter-backend.log"

# ---- 4. Start Angular dev server ---------------------------------------------
echo ">>> Starting Angular dev server..."
cd "$REPO_ROOT/frontend-web"
npm start > /tmp/gas-meter-web.log 2>&1 &
WEB_PID=$!
echo "    PID: $WEB_PID  |  log: /tmp/gas-meter-web.log"

# ---- 5. Wait for backend to be ready -----------------------------------------
# Use curl without -f so that any HTTP response (incl. 401) counts as "up"
echo ">>> Waiting for backend on http://localhost:8080 ..."
DEADLINE=$(( $(date +%s) + 120 ))
until curl -s -o /dev/null http://localhost:8080/readings 2>/dev/null; do
    [[ $(date +%s) -ge $DEADLINE ]] && { echo "Error: Backend did not start within 120 s. Check /tmp/gas-meter-backend.log"; exit 1; }
    sleep 2
done
echo ">>> Backend is ready."

# ---- 6. Wait for Angular to be ready -----------------------------------------
echo ">>> Waiting for Angular on http://localhost:4200 ..."
DEADLINE=$(( $(date +%s) + 120 ))
until curl -s -o /dev/null http://localhost:4200 2>/dev/null; do
    [[ $(date +%s) -ge $DEADLINE ]] && { echo "Error: Angular did not start within 120 s. Check /tmp/gas-meter-web.log"; exit 1; }
    sleep 2
done
echo ">>> Angular is ready."

# ---- 7. Run the Android instrumented E2E test --------------------------------
# Uninstall any stale package first to avoid install-commit failures.
echo ">>> Cleaning up stale packages on emulator (if any)..."
"$ANDROID_HOME/platform-tools/adb" uninstall com.example.greetingcard      2>/dev/null || true
"$ANDROID_HOME/platform-tools/adb" uninstall com.example.greetingcard.test 2>/dev/null || true
sleep 5

# Install the app APK first so we can grant camera permission before the test
# runs (the subsequent connectedEmulatorDebugAndroidTest does an upgrade install
# which preserves runtime permissions on Android 6+).
echo ">>> Installing app APK..."
cd "$REPO_ROOT/frontend-mobile"
./gradlew app:installEmulatorDebug

echo ">>> Granting camera permission (avoids system dialog during UI test)..."
"$ANDROID_HOME/platform-tools/adb" shell pm grant com.example.greetingcard android.permission.CAMERA

echo ">>> Running UploadE2ETest on emulator..."
./gradlew app:connectedEmulatorDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.example.greetingcard.UploadE2ETest
echo ">>> Android E2E test passed."

# ---- 8. Install Playwright deps and run web test -----------------------------
echo ">>> Running Playwright web test..."
cd "$REPO_ROOT/e2e"
npm install
npx playwright install --with-deps chromium
npx playwright test
echo ">>> Playwright test passed."

echo ""
echo "✓ All E2E tests passed!"
