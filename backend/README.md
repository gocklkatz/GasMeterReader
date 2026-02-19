# GasMeterAPI

A contract-first Spring Boot REST API for tracking daily natural gas consumption, with an Angular frontend for uploading photos directly from the browser.

Upload a photo of your gas meter each morning; the backend stores the image and exposes it for later processing (OCR, graphing, etc.).

## Quickstart

```bash
./mvnw spring-boot:run
```

Sign in with the configured credentials (default: `admin` / `changeme`). See [frontend-web](../frontend-web/README.md) or [frontend-mobile](../frontend-mobile/README.md) for client setup.

## API

All endpoints except `/auth/login` and `GET /images/**` require a valid JWT in the `Authorization: Bearer <token>` header.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/auth/login` | — | Obtain a JWT token |
| `POST` | `/readings` | required | Upload a meter photo with a timestamp |
| `GET` | `/readings` | required | List all readings |
| `GET` | `/readings/{id}` | required | Get a single reading by ID |
| `GET` | `/images/{path}` | — | Serve a stored image (local storage only) |

The OpenAPI specification in [`src/main/resources/api.yaml`](src/main/resources/api.yaml) is the source of truth. Java interfaces and models are generated from it at build time — do not edit the files under `target/generated-sources/`.

### Authenticate (curl)

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"changeme"}' | jq -r .token)
```

### Upload a reading (curl)

```bash
curl -X POST "http://localhost:8080/readings" \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@/path/to/meter.jpg" \
  -F "timestamp=2026-02-19T08:22:00Z"
```

### List all readings

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/readings
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `app.image-storage.base-path` | `/data/images` | Base directory for local image storage |
| `app.image-storage.backend` | `local` | Storage backend: `local` or `s3` |
| `app.image-storage.s3.bucket` | — | S3 bucket name (required when backend is `s3`) |
| `app.image-storage.s3.region` | — | AWS region (required when backend is `s3`) |
| `spring.servlet.multipart.max-file-size` | `20MB` | Maximum size per uploaded file |
| `app.cors.allowed-origins` | `http://localhost:4200` | Comma-separated list of allowed CORS origins |
| `app.security.jwt.secret` | *(insecure default)* | JWT signing secret — must be ≥ 32 chars |
| `app.security.jwt.expiration-ms` | `86400000` | Token lifetime in milliseconds (24 h) |
| `app.security.users[n].username` | `admin` | Username for user *n* |
| `app.security.users[n].password` | `{noop}changeme` | Password for user *n* (supports `{bcrypt}` prefix) |

Images are stored under `{base-path}/{year}/{month}/{day}/reading_{uuid}.jpg`. The storage directory is cleared on every startup.

### Switching to S3

```properties
app.image-storage.backend=s3
app.image-storage.s3.bucket=your-bucket-name
app.image-storage.s3.region=eu-central-1
```

AWS credentials are resolved via the standard SDK credential chain (environment variables, IAM role, `~/.aws/credentials`).

## Security

### Before deploying to production

**1. Change the JWT secret**

Generate a cryptographically random secret of at least 64 characters:

```bash
openssl rand -base64 48
```

Set it in `application.properties` or as an environment variable:

```bash
APP_SECURITY_JWT_SECRET=<your-random-secret> ./mvnw spring-boot:run
```

**2. Hash all passwords with BCrypt**

Generate a BCrypt hash (cost factor 12):

```bash
htpasswd -bnBC 12 "" yourpassword | tr -d ':\n'
```

Store the result in `application.properties`:

```properties
app.security.users[0].password={bcrypt}$2a$12$...
```

**3. Set the correct CORS origin**

```properties
app.cors.allowed-origins=https://your-domain.com
```

**4. Run behind HTTPS**

Expose the application through a reverse proxy (Nginx, Caddy, AWS ALB) that terminates TLS. Do not expose port 8080 directly to the internet.

**5. Restrict the image base path**

Point `app.image-storage.base-path` to a directory outside the web root and ensure the process user has minimal permissions.

### Additional hardening recommendations

- **Rate-limit `/auth/login`** at the reverse proxy level (e.g., Nginx `limit_req`) to slow brute-force attempts.
- **Set a short JWT expiration** and implement token refresh if needed. Tokens are invalidated only on expiry (no server-side revocation).
- **Enable HTTP Strict Transport Security (HSTS)** in the reverse proxy.
- **Keep dependencies updated** — run `./mvnw versions:display-dependency-updates` and `npm outdated` regularly.
- **Audit uploaded files** — only `image/jpeg`, `image/png`, `image/webp`, and `image/gif` are accepted server-side. The client-side `accept="image/*"` is advisory only.

## Build & test

```bash
./mvnw spring-boot:run          # Run with defaults
./mvnw test                     # Run all tests
./mvnw package                  # Build executable JAR
./mvnw generate-sources         # Regenerate after api.yaml changes
```

## Tech stack

- Java 21, Spring Boot 4.0.2, Spring Security 6
- JJWT 0.12.6 (JWT)
- OpenAPI Generator 7.12.0 (Maven plugin)
- AWS SDK for Java v2 (S3)
- JUnit 5, Mockito, AssertJ, spring-security-test
