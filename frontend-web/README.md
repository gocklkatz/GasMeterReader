# GasMeterReader — Web Frontend

Angular 21 SPA for uploading and browsing gas meter photos. Connects to the [backend](../backend/README.md).

## Setup

```bash
npm install
npm start       # Dev server at http://localhost:4200
npm run build   # Production build (output: dist/)
```

## Features

**Login (`/login`)** — username/password form; on success a JWT is stored in `sessionStorage`.

**Upload (`/`)** — drag-and-drop or browse for one or more images, adjust timestamps, and upload in parallel. Timestamp is auto-filled from filenames matching `IMG_YYYYMMDD_HHMMSS`. Adding new images clears previous results.

**Browse (`/browse`)** — all uploaded readings as an image grid with timestamps.

## Configuration

The backend URL is hardcoded as `http://localhost:8080` in `src/app/readings.service.ts` and `src/app/auth/auth.service.ts` (`apiBase` field in each service).
