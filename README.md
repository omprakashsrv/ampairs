# Ampairs Business Management Platform

Comprehensive multi-platform business management system with workspace-based multi-tenancy.

---

## Applications

| App | Repository | Technology | Purpose |
|-----|-----------|-----------|---------|
| **Backend** | this repo | Spring Boot 3.5 + Kotlin 2.2 + Java 25 | REST API, business logic, database |
| **Web** | [omprakashsrv/ampairs-web](https://github.com/omprakashsrv/ampairs-web) | Angular 20 + Material Design 3 | Browser client |
| **Mobile / Desktop** | [omprakashsrv/ampairs-app](https://github.com/omprakashsrv/ampairs-app) | Compose Multiplatform | Android, iOS, Desktop |

All clients consume REST APIs from the Spring Boot backend with JWT authentication and multi-tenant workspace isolation.

---

## Project Structure

```
ampairs/
├── ampairs_service/    # Main Spring Boot application (aggregates all modules)
├── core/               # Shared utilities, base entities, multi-tenancy, AWS
├── auth/               # JWT authentication, OTP, device-aware sessions
├── workspace/          # Workspace management, RBAC, memberships, invitations
├── product/            # Product catalog, inventory (multi-warehouse, batch/serial)
├── subscription/       # Subscription plans, billing, recurring charges
├── customer/           # CRM, contacts, GST-aware addressing
├── tax/                # GST configuration, HSN/SAC catalog, calculation engine
├── business/           # Business profile, legal details, multi-branch
├── order/              # Order lifecycle, pricing, discounts, status transitions
├── invoice/            # Invoice generation, GST compliance, PDF export
├── event/              # Domain event streaming, WebSocket/STOMP delivery
├── notification/       # Multi-channel notifications (SMS, email, push, WhatsApp)
├── unit/               # Unit of measure definitions and conversions
├── setting/            # Central workspace settings registry (cross-module toggles, offline bulk-sync)
├── form/               # Dynamic form builder, configurable entity schemas
├── file/               # File storage, upload/download, AWS S3 integration
│
├── specs/              # Feature specifications (spec → plan → tasks → impl)
├── scripts/            # Deployment and automation scripts
├── ansible/            # Infrastructure as code
├── .github/workflows/  # CI/CD pipelines
└── docker-compose.yml  # Local development dependencies
```

> Web and mobile apps live in their own repositories and consume the backend REST API.

---

## Quick Start

### Prerequisites

- Java 25+
- Docker (required for Testcontainers integration tests)
- MySQL 8.0+ or via `docker-compose.yml`
- Node.js 20+ (web frontend only)

### Backend

```bash
# Start local dependencies (MySQL, RabbitMQ, etc.)
docker-compose up -d

# Run the application
./gradlew :ampairs_service:bootRun

# Or use the dev script
./start-dev.sh
```

---

## Build & Test Commands

```bash
# From the project root
./gradlew buildAll      # Build all modules (produces ampairs_service/build/libs/*.jar)
./gradlew testAll       # Run all module tests (requires Docker)
./gradlew cleanAll      # Clean all build outputs
./gradlew ciBuild       # Full CI gate: tests + build

# Module-specific
./gradlew :<module>:build
./gradlew :<module>:test

# Database migrations
./gradlew :ampairs_service:flywayInfo      # Current migration status
./gradlew :ampairs_service:flywayValidate  # Verify checksums
./gradlew :ampairs_service:flywayMigrate   # Apply pending migrations
```

---

## Architecture

### Backend

- **Modular monolith**: 16 domain modules + `ampairs_service` aggregator
- **Multi-tenancy**: Workspace-based isolation via `@TenantId` on `OwnableBaseDomain`
- **Auth**: Device-aware JWT with concurrent multi-device login support
- **API**: Versioned REST endpoints (`/api/v1/{resource}`), all responses wrapped in `ApiResponse<T>`
- **Database**: Flyway-versioned migrations, `@EntityGraph` for efficient relationship loading
- **Events**: Spring ApplicationEvents captured by `event` module, delivered via WebSocket/STOMP

### Key Design Rules (enforced across all modules)

- `java.time.Instant` for all timestamps — never `LocalDateTime`
- DTO isolation — entities never exposed in API responses
- Global Jackson `SNAKE_CASE` — no `@JsonProperty` for standard fields
- Tenant context set at controller level, never in services
- `@EntityGraph` for relationship loading — no `JOIN FETCH` in JPQL

Full rules: `.claude/rules/` | Architecture standards: [CLAUDE.md](CLAUDE.md)

### Frontend & Mobile

Both live in separate repositories and consume this backend's REST API:

- **Web**: Angular 20 + Material Design 3
- **Mobile/Desktop**: Compose Multiplatform (Android, iOS, Desktop)

---

## Feature Development Workflow

Features follow a spec-driven process using the built-in speckit commands:

```
/speckit.specify   → create feature spec   (specs/###-feature/spec.md)
/speckit.clarify   → fill specification gaps
/speckit.plan      → generate implementation plan + data model
/speckit.tasks     → generate dependency-ordered task list
/speckit.analyze   → cross-artifact consistency check
/speckit.implement → execute tasks
```

Existing specs: `specs/002` (timezone), `003` (business), `004` (unit), `005` (backend).

---

## Deployment

Push to `main` triggers the CI/CD pipeline:

1. Build & compile verification
2. Automated test suite
3. JAR packaging
4. SSH deployment to production server
5. Service restart and health verification

```bash
# Manual JAR build
./gradlew :ampairs_service:bootJar
# Output: ampairs_service/build/libs/ampairs_service-1.0.0.jar
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for full production setup.

---

## Monitoring

```bash
# Application health
curl http://localhost:8080/actuator/health

# Service status (production)
systemctl status ampairs
journalctl -u ampairs -f
```

---

## Documentation

| Document | Purpose |
|----------|---------|
| [CLAUDE.md](CLAUDE.md) | Coding standards, architectural patterns |
| [docs/](docs/README.md) | All developer guides — API, deployment, migrations, features |
| `specs/###-feature/` | Per-feature specifications, plans, and API contracts |

---

## Contributing

1. Read [CLAUDE.md](CLAUDE.md) and `.claude/rules/` for coding standards
2. Create a feature branch: `git checkout -b ###-feature-name` (e.g. `006-payment-gateway`)
3. Use Conventional Commits: `feat(module):`, `fix(module):`, `refactor(module):` — subject ≤72 chars
4. Run `./gradlew ciBuild` and ensure it passes before opening a PR
5. For features > 30 min, start with `/speckit.specify` to generate a spec first

---

## License

**Source-available, noncommercial** — licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE.md).

You may use, modify, and share this software for any **noncommercial** purpose. **Commercial use is not permitted** — including use by or for a business to generate revenue, sell the software, or charge customers for products or services built with it. This is *not* an OSI-approved open-source license (it restricts commercial use).

For a commercial license, contact the copyright holder. Copyright © 2026 Om Prakash.
