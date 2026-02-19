# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew app:assembleDebug

# Install to connected device/emulator
./gradlew app:installDebug

# Run unit tests
./gradlew app:testDebugUnitTest

# Run a single unit test class
./gradlew app:testDebugUnitTest --tests "com.example.greetingcard.ExampleUnitTest"

# Run instrumentation tests (requires connected device/emulator)
./gradlew app:connectedAndroidTest

# Lint
./gradlew app:lint

# Clean build
./gradlew clean app:assembleDebug
```

## Architecture

This is a minimal single-module Android app using **Jetpack Compose** with a **single-activity** architecture.

- **Entry point:** `app/src/main/java/com/example/greetingcard/MainActivity.kt`
- **UI:** Declarative Compose composables; no separate ViewModels yet
- **Theme:** Centralized in `ui/theme/` — `Theme.kt` (GreetingCardTheme with dynamic color support for Android 12+), `Color.kt`, `Type.kt`
- **Package:** `com.example.greetingcard`

## Tech Stack

- Kotlin 2.0.21, AGP 9.0.1, Gradle 9.2.1
- Jetpack Compose with Material 3 (BOM 2024.09.00)
- Min SDK 24, Target/Compile SDK 36
- Java 11 source/target compatibility
- Dependencies managed via `gradle/libs.versions.toml` (version catalog)
- Testing: JUnit 4 (unit), Espresso + AndroidJUnit4 (instrumentation), Compose UI Test library included
