# GasMeterReader

Monorepo for a gas meter reading tracker. Photograph your gas meter each morning; the backend stores the image for later OCR and graphing. Three frontends share one backend API.

## Structure

```
GasMeterReader/
├── api/              # OpenAPI specification (source of truth for all clients)
├── backend/          # Spring Boot backend (Java/Maven)  →  backend/README.md
├── frontend-web/     # Angular web frontend              →  frontend-web/README.md
└── frontend-mobile/  # Android mobile frontend           →  frontend-mobile/README.md
```

## Quickstart

```bash
# Terminal 1 — backend (http://localhost:8080)
cd backend && ./mvnw spring-boot:run

# Terminal 2 — web frontend (http://localhost:4200)
cd frontend-web && npm install && npm start
```

Open `http://localhost:4200` and sign in (`admin` / `changeme` by default).

For the Android app, open `frontend-mobile/` in Android Studio and run on an emulator. The emulator automatically reaches the backend at `http://10.0.2.2:8080/`.
