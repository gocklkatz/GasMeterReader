# GasMeterReader — Android App

Kotlin/Jetpack Compose Android app for capturing and uploading gas meter photos. Connects to the [backend](../backend/README.md).

## Requirements

- Android Studio (latest stable)
- Android device or emulator (min SDK 24)
- Backend running at `http://localhost:8080` (reachable from the emulator as `http://10.0.2.2:8080/`)

## Setup

Open `frontend-mobile/` in Android Studio, let Gradle sync, then run via **Run > Run 'app'**.

Or from the command line:

```bash
./gradlew app:installDebug
```

## Configuration

The backend URL is hardcoded as `http://10.0.2.2:8080/` (the Android emulator's loopback alias for the host machine). To point at a real server, change the `buildConfigField` value in `app/build.gradle.kts`.

## Features

- **Login** — JWT-based authentication; token persists in DataStore and survives app restarts.
- **Camera** — Capture a photo with CameraX; the upload is queued in WorkManager and retried automatically if the network is unavailable.
- **History** — Browse all uploaded readings fetched from the backend.
