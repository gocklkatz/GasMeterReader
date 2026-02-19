# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Monorepo Structure

```
GasMeterReader/
├── api/              # OpenAPI 3.0 spec — single source of truth for all clients
├── backend/          # Spring Boot REST API (Java 21, Maven)     → backend/CLAUDE.md
├── frontend-web/     # Angular 21 SPA (TypeScript)               → frontend-web/CLAUDE.md
└── frontend-mobile/  # Android app (Kotlin, Jetpack Compose)     → frontend-mobile/CLAUDE.md
```

## API Contract

`api/api.yaml` is the canonical OpenAPI spec. **All API changes start here.** After editing it, regenerate backend sources:

```bash
cd backend && ./mvnw generate-sources
```

Generated Java interfaces/models land in `target/generated-sources/openapi/` and must not be edited manually. `ReadingController` implements the generated `ReadingsApi` interface — endpoints are never manually declared.

## System Overview

All three clients authenticate via `POST /auth/login` (returns a JWT), then upload meter photos via `POST /readings` (multipart/form-data).

```
Angular web ─────────┐
                      ├──→ JwtAuthenticationFilter → ReadingController → ImageStorageService (local / S3)
Android app ─────────┘                                                 → ReadingRepositoryImpl (in-memory)
```

The backend has no database — readings and local image storage are lost on every restart.

## Cross-Cutting Design Decisions

**JWT storage** differs by client: web uses `sessionStorage` (tab-scoped, cleared on close); Android uses `DataStore<Preferences>` (persistent across process death). Both send `Authorization: Bearer <token>` on every request.

**Upload resilience**: The Android app queues uploads in Room + WorkManager with a `CONNECTED` constraint and exponential backoff, so uploads survive network drops and process restarts. The web app fires parallel direct HTTP requests with no retry.

**Image compression**: The Android `UploadRepository` scales images to max 1280 px on the longest edge and JPEG-compresses at 85% before uploading. The web app sends the raw file unchanged.

**Mobile backend URL**: Hardcoded as `http://10.0.2.2:8080/` (Android emulator's loopback alias for the host machine). Change the `buildConfigField` in `frontend-mobile/app/build.gradle.kts` for a real device or production deployment.
