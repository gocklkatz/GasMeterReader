# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A **Gas Meter API** — contract-first Spring Boot backend with an Angular frontend for uploading gas meter reading photos.

---

## Backend

### Build & Run

```bash
./mvnw clean compile        # Compile
./mvnw spring-boot:run      # Run on http://localhost:8080
./mvnw package              # Build executable JAR
```

### Test Commands

```bash
./mvnw test                                                    # Run all tests
./mvnw test -Dtest=ReadingControllerTest                       # Run specific test class
./mvnw test -Dtest=ReadingControllerTest#methodName            # Run specific test method
```

### Architecture

**Contract-first** Spring Boot REST API. The OpenAPI spec (`src/main/resources/api.yaml`) is the source of truth. The Maven OpenAPI Generator plugin generates Java interfaces and models into `target/generated-sources/openapi/`.

**Request flow:**
```
HTTP Request
  → JwtAuthenticationFilter (validates Bearer token)
  → ReadingController (implements generated ReadingsApi)
  → ReadingService / ReadingServiceImpl (validates image content-type)
  → ImageStorageService (local filesystem or S3)
  → ReadingRepository / ReadingRepositoryImpl (in-memory HashMap)
  → Reading (generated model)
```

**Key design choices:**
- All major components are interface-backed (`ReadingService`, `ReadingRepository`, `ImageStorageService`) to support mocking
- `ReadingController` implements the generated `ReadingsApi` interface — do not manually define endpoints; update `api.yaml` and regenerate
- The repository is in-memory only (no database)
- Constructor injection throughout (no `@Autowired` field injection)
- CORS and security configured in `SecurityConfig`; allowed origins read from `app.cors.allowed-origins`
- `GlobalExceptionHandler` maps `IllegalArgumentException` → 400 Bad Request

**Image storage backends** (controlled by `app.image-storage.backend`):
- `local` (default) — stores files under a local base path, cleared on startup
- `s3` — uploads to AWS S3; requires `app.image-storage.s3.region` and `app.image-storage.s3.bucket`

**Authentication:**
- Stateless JWT via JJWT 0.12.x; Bearer token in `Authorization` header
- `POST /auth/login` and `GET /images/**` are public; all other endpoints require a valid token
- `POST /auth/login` with `{username, password}` JSON body returns `{token}`
- Users configured in `application.properties` under `app.security.users[n]`
- Passwords support `{noop}plaintext` (dev) or `{bcrypt}$2a$...` (production) prefixes
- `app.security.jwt.secret` must be ≥ 32 characters

### Code Generation

After modifying `api.yaml`, regenerate sources:
```bash
./mvnw generate-sources
```

Generated files live in `target/generated-sources/openapi/src/main/java/com/example/` and must not be edited manually.

### Testing Strategy

| Test class | Type | What it covers |
|---|---|---|
| `HelloOpenApiApplicationTests` | Spring integration | Context loads |
| `ReadingControllerTest` | `@WebMvcTest` + `@WithMockUser` | HTTP layer, request validation |
| `WebConfigTest` | `@WebMvcTest` + `@Import(SecurityConfig)` | CORS allowed/disallowed origins |
| `AuthControllerTest` | `@WebMvcTest` + `@Import(SecurityConfig)` | Login success/failure |
| `JwtServiceTest` | Unit | Token generation, extraction, validation, expiry |
| `ReadingServiceImplTest` | Unit | Service logic, content-type validation |
| `ReadingRepositoryImplTest` | Unit | In-memory store, ID auto-increment |
| `ImageStorageServiceLocalTest` | Unit (`@TempDir`) | Local file storage |
| `S3ImageStorageServiceLocalTest` | Unit (mocked S3) | S3 upload logic |

**`@WebMvcTest` notes with security:**
- Add `@WithMockUser` (class-level) to controller tests — security checks are active
- Add `@MockitoBean JwtService jwtService` — the JWT filter is loaded as a Filter and needs it
- Tests that need CORS or `PasswordEncoder` must `@Import(SecurityConfig.class)`

### Tech Stack

- Java 21, Spring Boot 4.0.2, Spring Security 6, Spring Web MVC, Spring Validation
- JJWT 0.12.6 (JWT generation and validation)
- OpenAPI Generator 7.12.0 (Maven plugin)
- JUnit 5, Mockito, AssertJ, spring-security-test

