# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm install
npm start                        # Dev server at http://localhost:4200
npm run build                    # Production build (output: dist/)
npm test                         # Run tests in watch mode
npm test -- --watch=false        # Run tests once
```

## Architecture

Angular 21 SPA with standalone components (no NgModules) and Angular signals for state. Three routes with a sticky navigation bar and "Sign out" button:

| Route | Component | Description |
|---|---|---|
| `/login` | `LoginComponent` | Username/password login form |
| `/` | `ReadingUploadComponent` | Upload one or more meter photos (guarded) |
| `/browse` | `ReadingsBrowseComponent` | Browse all uploaded readings (guarded) |

```
LoginComponent          →  AuthService      →  POST /auth/login  →  stores JWT in sessionStorage
ReadingUploadComponent  →  ReadingsService  →  POST /readings (multipart/form-data)
ReadingsBrowseComponent →  ReadingsService  →  GET  /readings
                                            →  GET  /images/{path}
authInterceptor — adds Authorization: Bearer <token> to every request; redirects to /login on 401
authGuard       — redirects to /login if not logged in
```

**Key design choices:**
- `AuthService` stores the JWT in `sessionStorage` (cleared on tab/browser close)
- `authInterceptor` is a functional `HttpInterceptorFn` registered in `app.config.ts` via `withInterceptors`
- `FileEntry` interface tracks per-file state (timestamp, preview, loading, result, error)
- Multiple files: each fires its own parallel request via `submitAll()`
- Timestamp auto-filled from filename when it matches `IMG_YYYYMMDD_HHMMSS`
- Duplicate files (same name + size + lastModified) are silently ignored
- Adding new files clears completed upload results; completed results persist until then

## Testing Strategy

| Test file | What it covers |
|---|---|
| `app.spec.ts` | Root component, router outlet, nav links, logout button visibility, logout() method |
| `auth/auth.guard.spec.ts` | Guard returns `true` when logged in; returns UrlTree to `/login` when not |
| `auth/auth.interceptor.spec.ts` | Bearer header injection, 401 → logout+redirect, error re-throw |
| `auth/auth.service.spec.ts` | Login POST, token storage, logout, sessionStorage init |
| `auth/login/login.spec.ts` | Form validation, submit, 401 vs generic errors, loading state |
| `readings.service.spec.ts` | POST payload, GET list, image URL construction, error propagation |
| `reading-upload.spec.ts` | File selection, drag-drop, filename parsing, submit, per-entry state |
| `readings-browse.spec.ts` | Init load, loading/error/empty states, imageUrl delegation |

## Tech Stack

- Angular 21, TypeScript 5.9, RxJS 7.8
- Vitest 4 + jsdom (via `@angular/build:unit-test`)
- SCSS
