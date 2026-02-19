# GasMeterReader

Monorepo for the GasMeterReader project.

## Structure

```
GasMeterReader/
├── api/              # OpenAPI specification (source of truth for all clients)
├── backend/          # Spring Boot backend (Java/Maven)
├── frontend-web/     # Angular web frontend
└── frontend-mobile/  # Android mobile frontend (Kotlin/Compose)
```

## Getting started

### Backend
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend web
```bash
cd frontend-web
npm install
ng serve
```

### Frontend mobile
Open `frontend-mobile/` in Android Studio, then run the app on a device or emulator.
