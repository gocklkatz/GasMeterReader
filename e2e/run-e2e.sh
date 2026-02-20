#!/usr/bin/env bash
# run-e2e.sh — Full cross-platform E2E test orchestrator
#
# What it does (in order):
#   1. Creates the AVD "GasMeterReader_API36" if it doesn't exist
#   2. Starts the Android emulator if none is connected
#   3. Starts the Spring Boot backend on :8080
#   4. Starts the Angular dev server on :4200
#   5. Installs the debug APK on the emulator
#   6. Runs UploadE2ETest (Android instrumented test)
#   7. Runs the Playwright web test
#   8. Cleans up background processes
#
# Prerequisites:
#   - ANDROID_HOME set (e.g. ~/Library/Android/sdk)
#   - avdmanager, sdkmanager, emulator, adb in PATH or under ANDROID_HOME
#   - Java 21 (for the backend Maven build)
#   - Node.js ≥ 18 (for Playwright)
#   - npm install + npx playwright install run at least once in e2e/
#
# Usage:
#   cd GasMeterReader
#   ./e2e/run-e2e.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AVD_NAME="GasMeterReader_API36"

# Detect host architecture to pick the right ABI
if [[ "$(uname -m)" == "arm64" ]]; then
    ABI="arm64-v8a"
else
    ABI="x86_64"
fi
SYSTEM_IMAGE="system-images;android-36;google_apis;${ABI}"

BACKEND_PID=""
WEB_PID=""
EMULATOR_PID=""

# ---- Cleanup on exit ----------------------------------------------------------
cleanup() {
    echo ""
    echo ">>> Cleaning up..."
    [[ -n "$BACKEND_PID" ]] && kill "$BACKEND_PID" 2>/dev/null && echo "  Stopped backend (PID $BACKEND_PID)" || true
    [[ -n "$WEB_PID"     ]] && kill "$WEB_PID"     2>/dev/null && echo "  Stopped Angular (PID $WEB_PID)"   || true
    # Leave emulator running if it was already running before this script started
    [[ -n "$EMULATOR_PID" ]] && kill "$EMULATOR_PID" 2>/dev/null && echo "  Stopped emulator (PID $EMULATOR_PID)" || true
}
trap cleanup EXIT

# ---- Helper: resolve SDK tool paths ------------------------------------------
sdk_tool() {
    local tool="$1"
    # Try PATH first, then common ANDROID_HOME locations
    if command -v "$tool" &>/dev/null; then
        echo "$tool"
    elif [[ -x "${ANDROID_HOME}/cmdline-tools/latest/bin/${tool}" ]]; then
        echo "${ANDROID_HOME}/cmdline-tools/latest/bin/${tool}"
    elif [[ -x "${ANDROID_HOME}/tools/bin/${tool}" ]]; then
        echo "${ANDROID_HOME}/tools/bin/${tool}"
    else
        echo "Error: '$tool' not found. Set ANDROID_HOME or add it to PATH." >&2
        exit 1
    fi
}

AVDMANAGER="$(sdk_tool avdmanager)"
SDKMANAGER="$(sdk_tool sdkmanager)"
EMULATOR="${ANDROID_HOME}/emulator/emulator"
[[ -x "$EMULATOR" ]] || { echo "Error: emulator not found at $EMULATOR"; exit 1; }

# ---- 1. Create AVD if it doesn't exist ---------------------------------------
echo ">>> Checking for AVD '$AVD_NAME'..."
if ! "$AVDMANAGER" list avd 2>/dev/null | grep -q "Name: ${AVD_NAME}"; then
    echo ">>> AVD not found. Installing system image: $SYSTEM_IMAGE"
    "$SDKMANAGER" "$SYSTEM_IMAGE"

    echo ">>> Creating AVD '$AVD_NAME' (Pixel 6, API 36, ${ABI})"
    echo "no" | "$AVDMANAGER" create avd \
        --name    "$AVD_NAME" \
        --package "$SYSTEM_IMAGE" \
        --device  "pixel_6" \
        --force
    echo ">>> AVD '$AVD_NAME' created."
else
    echo ">>> AVD '$AVD_NAME' already exists."
fi

# ---- 2. Start emulator if no emulator device is connected --------------------
EMULATOR_WAS_RUNNING=false
if adb devices 2>/dev/null | grep -qE "^emulator-[0-9]+[[:space:]]+device$"; then
    echo ">>> Emulator already running — skipping emulator start."
    EMULATOR_WAS_RUNNING=true
else
    echo ">>> Starting emulator '$AVD_NAME'..."
    "$EMULATOR" -avd "$AVD_NAME" -no-audio -no-snapshot-load &
    EMULATOR_PID=$!
    echo "    Emulator PID: $EMULATOR_PID"

    echo ">>> Waiting for emulator device to come online..."
    adb wait-for-device

    echo ">>> Waiting for full boot (sys.boot_completed=1)..."
    until adb shell getprop sys.boot_completed 2>/dev/null | grep -q "^1$"; do
        sleep 3
    done
    echo ">>> Emulator ready."
fi

# ---- 3. Start Spring Boot backend --------------------------------------------
echo ">>> Starting Spring Boot backend..."
cd "$REPO_ROOT/backend"
./mvnw spring-boot:run > /tmp/gas-meter-backend.log 2>&1 &
BACKEND_PID=$!
echo "    Backend PID: $BACKEND_PID  (log: /tmp/gas-meter-backend.log)"

# ---- 4. Start Angular dev server ---------------------------------------------
echo ">>> Starting Angular dev server..."
cd "$REPO_ROOT/frontend-web"
npm start > /tmp/gas-meter-web.log 2>&1 &
WEB_PID=$!
echo "    Angular PID: $WEB_PID  (log: /tmp/gas-meter-web.log)"

# ---- 5. Wait for backend to be ready -----------------------------------------
echo ">>> Waiting for backend on http://localhost:8080 ..."
until curl -sf -o /dev/null http://localhost:8080/readings 2>/dev/null; do
    sleep 2
done
echo ">>> Backend is ready."

# ---- 6. Wait for Angular to be ready -----------------------------------------
echo ">>> Waiting for Angular on http://localhost:4200 ..."
until curl -sf -o /dev/null http://localhost:4200 2>/dev/null; do
    sleep 2
done
echo ">>> Angular is ready."

# ---- 7. Build and install the emulator APK -----------------------------------
echo ">>> Installing emulatorDebug APK on emulator..."
cd "$REPO_ROOT/frontend-mobile"
./gradlew app:installEmulatorDebug
echo ">>> APK installed."

# ---- 8. Run the Android instrumented E2E test --------------------------------
echo ">>> Running UploadE2ETest on emulator..."
cd "$REPO_ROOT/frontend-mobile"
./gradlew app:connectedEmulatorDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.example.greetingcard.UploadE2ETest
echo ">>> Android E2E test passed."

# ---- 9. Run the Playwright web test ------------------------------------------
echo ">>> Running Playwright web test..."
cd "$REPO_ROOT/e2e"
# Install dependencies and browsers on first run
npm install
npx playwright install --with-deps chromium
npx playwright test
echo ">>> Playwright test passed."

echo ""
echo "✓ All E2E tests passed!"
