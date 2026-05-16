# Repository Guidelines for AI Agents

## Project Structure

- Backend modules live at the **root level** — `core`, `auth`, `workspace`, `customer`, `product`, `order`, `invoice`, `tax`, `business`, `notification`, `event`, `form`, `file`, `unit`, `subscription`, and `ampairs_service` (the runnable aggregator).
- Web and mobile apps are in **separate repositories** — `ampairs-web/` and `ampairs-mp-app/` at the root contain only historical implementation docs, not source code.
- Specs live in `specs/{###-feature}/` — each feature has `spec.md`, `plan.md`, `tasks.md`, `data-model.md`, and `contracts/`.
- Tooling: `.github/workflows/`, `scripts/`, `ansible/`, `.claude/rules/`, `.specify/`.

## Build, Test, and Development Commands

- Build all: `./gradlew buildAll`
- Test all (requires Docker): `./gradlew testAll`
- CI gate: `./gradlew ciBuild`
- Run backend: `./gradlew :ampairs_service:bootRun`
- Package JAR: `./gradlew :ampairs_service:bootJar`
- Module-specific: `./gradlew :<module>:build` / `./gradlew :<module>:test`
- Migrations: `./gradlew :ampairs_service:flywayInfo` / `flywayMigrate` / `flywayValidate`

## Coding Standards

Enforced rules are in `.claude/rules/` (one file per concern). Key rules:

- **Timestamps**: always `java.time.Instant`, never `LocalDateTime`
- **DTOs**: never expose JPA entities in API responses — use Request/Response DTOs in `domain/dto/`
- **JSON**: global Jackson `SNAKE_CASE` config — no `@JsonProperty` for standard camelCase fields
- **API responses**: all endpoints return `ApiResponse<T>` wrapper
- **Exceptions**: no try/catch in controllers — let the global handler manage them
- **Tenant context**: set at controller level before repository access, cleared in `finally`
- **Data loading**: `@EntityGraph` for relationships — avoid N+1 queries

## Feature Development Workflow

All significant features use the speckit flow:

```
/speckit.specify → /speckit.clarify → /speckit.plan → /speckit.tasks → /speckit.analyze → /speckit.implement
```

Next spec number: `006`.

## Branching & Commits

- Branch naming: `###-feature-name` (e.g. `006-payment-gateway`)
- Conventional Commits: `feat(module):`, `fix(module):`, `refactor(module):` — subject ≤72 chars
- Reference issues in commit body when applicable

## Documentation

All developer docs are in `docs/`:

```
docs/
├── api/           — authentication, app updates, API key testing
├── deployment/    — deployment, CI/CD, production validation
├── guides/        — database migrations, configuration, security, file storage
└── features/      — account deletion and other feature-specific docs
```

Root-level docs: `README.md` (project overview), `CLAUDE.md` (coding standards), `AGENTS.md` (this file).
