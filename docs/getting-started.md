# Getting Started

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 25+ | Backend runtime |
| Docker | any | MySQL + RabbitMQ via docker-compose |
| Gradle | via wrapper (`./gradlew`) | Build system |
| Node.js | 20+ | Web frontend (separate repo) |

## 1. Clone the repository

```bash
git clone https://github.com/omprakashsrv/ampairs-backend.git
cd ampairs-backend
```

## 2. Start local dependencies

MySQL and RabbitMQ run via Docker Compose:

```bash
docker-compose up -d
```

Services started:
- MySQL: `localhost:3306`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ STOMP: `localhost:61613`
- RabbitMQ Management UI: `http://localhost:15672` (guest/guest)

## 3. Configure environment

Copy and edit the environment file:

```bash
cp .env.example .env   # if present, else set vars inline
```

Minimum required environment variables:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ampairs?serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

# JWT
JWT_SECRET=your_jwt_secret_min_256_bits

# AWS (for S3 file storage)
AWS_ACCESS_KEY_ID=your_key
AWS_SECRET_ACCESS_KEY=your_secret
AWS_S3_BUCKET=your_bucket
AWS_REGION=ap-south-1

# Firebase (for push notifications)
FIREBASE_SERVICE_ACCOUNT_KEY_PATH=/path/to/firebase-adminsdk.json
```

## 4. Run the application

**Development mode** (rate limiting and reCAPTCHA disabled):
```bash
./start-dev.sh
```

**Manual run with specific profile:**
```bash
export SPRING_PROFILES_ACTIVE=dev
export BUCKET4J_ENABLED=false
export RECAPTCHA_ENABLED=false
./gradlew :ampairs_service:bootRun
```

**Test mode** (OTP fixed to `123456` for E2E testing):
```bash
./start-test.sh
```

The server starts at `http://localhost:8080`.

## 5. Verify

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

Swagger UI: `http://localhost:8080/swagger-ui`
API Docs: `http://localhost:8080/api-docs`

## Spring Profiles

| Profile | Purpose | Key differences |
|---------|---------|----------------|
| `dev` (default) | Local development | Rate limiting off, reCAPTCHA off |
| `test` | E2E testing | OTP fixed to `123456` |
| `sandbox` | Staging / pre-prod | Close to production settings |
| `production` | Production | All security features on |

## Running tests

```bash
# All module tests (requires Docker running)
./gradlew testAll

# Single module
./gradlew :auth:test
./gradlew :product:test

# Full CI gate
./gradlew ciBuild
```

## Building the JAR

```bash
./gradlew :ampairs_service:bootJar
# Output: ampairs_service/build/libs/ampairs_service-1.0.0.jar
```

## Database migrations

Flyway runs automatically on startup. To manage manually:

```bash
./gradlew :ampairs_service:flywayInfo      # view current state
./gradlew :ampairs_service:flywayValidate  # verify checksums
./gradlew :ampairs_service:flywayMigrate   # apply pending
```

## Related repositories

| Repo | URL | Tech |
|------|-----|------|
| Web frontend | https://github.com/omprakashsrv/ampairs-web | Angular 20 + M3 |
| Mobile / Desktop | https://github.com/omprakashsrv/ampairs-app | Compose Multiplatform |
