# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew app:assembleDebug                                                              # Build debug APK
./gradlew app:installDebug                                                               # Install to connected device/emulator
./gradlew app:testDebugUnitTest                                                          # Run unit tests
./gradlew app:testDebugUnitTest --tests "com.example.greetingcard.ExampleUnitTest"      # Run a single test class
./gradlew app:connectedAndroidTest                                                       # Instrumentation tests (requires device/emulator)
./gradlew app:lint                                                                       # Lint
./gradlew clean app:assembleDebug                                                        # Clean build
```

## Architecture

Single-activity app with Jetpack Compose Navigation (`NavHost`). Three screens: `LoginScreen`, `CameraScreen`, `HistoryScreen`.

**DI:** Hilt throughout (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@HiltWorker`).

**Auth:** JWT stored in `DataStore<Preferences>` (survives process death). Start destination determined at startup by checking for an existing token.

**Offline-capable upload queue:** `CameraViewModel` captures a photo → saves it to permanent internal storage → inserts a `PendingUpload` row into Room → enqueues a `UploadWorker` `OneTimeWorkRequest` with a `CONNECTED` network constraint and exponential backoff. Uploads survive network drops and process restarts.

**Image compression:** `UploadRepository.compressImage()` scales to max 1280 px on the longest edge and JPEG-compresses at 85% quality before uploading.

**Image loading:** Coil (`AsyncImage`) for displaying remote meter images in the history screen.

**Backend URL:** Hardcoded as `BuildConfig.BACKEND_URL = "http://10.0.2.2:8080/"` (Android emulator's loopback alias for the host machine). Change the `buildConfigField` in `app/build.gradle.kts` for a real device or production deployment.

**Key packages under `com.example.greetingcard`:**
- `auth/` — `LoginScreen`, `AuthViewModel`, `AuthRepository`, DataStore token persistence
- `camera/` — `CameraScreen`, `CameraViewModel`, `UploadRepository`, `UploadWorker`
- `data/` — Room database, `PendingUpload` entity and DAO
- `history/` — `HistoryScreen`, `HistoryViewModel`, `HistoryRepository`
- `network/` — Retrofit `ApiService`, Hilt network module
- `ui/theme/` — Material 3 theme

## Tech Stack

- Kotlin 2.0.21, AGP 9.0.1, Gradle 9.2.1
- Jetpack Compose + Material 3 (BOM 2024.09.00)
- Hilt 2.59.1 (DI), KSP 2.0.21-1.0.27 (annotation processing for Hilt + Room)
- CameraX 1.4.1 (camera preview & capture)
- Retrofit 2.11.0 + OkHttp 4.12.0 (HTTP client)
- DataStore Preferences 1.1.1 (persistent JWT storage)
- Navigation Compose 2.8.6
- WorkManager 2.9.1 (background upload queue)
- Room 2.7.0 (SQLite ORM for upload queue)
- Coil 2.7.0 (image loading)
- Accompanist Permissions 0.37.0 (Compose camera permission handling)
- Coroutines 1.9.0
- Min SDK 24, Target SDK 36
- Dependency versions in `gradle/libs.versions.toml`
