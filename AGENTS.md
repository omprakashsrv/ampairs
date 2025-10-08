# Repository Guidelines

## Project Structure & Module Organization
- `ampairs-backend/` — Kotlin + Spring Boot modules by domain (`core`, `auth`, `order`, etc.); the runnable service sits in `ampairs_service/src/main/kotlin`.
- `ampairs-web/` — Angular 20 subproject with features in `src/app`, shared UI in `src/app/shared`, assets in `public/`.
- `ampairs-mp-app/` — Compose Multiplatform subproject; shared logic in `shared/`, platform launchers in `androidApp`, `desktopApp`, `iosApp`.
- Tooling lives in `.github/workflows/`, `scripts/`, `templates/`; generated files land in `build/` and `logs/`.

## Build, Test, and Development Commands
- Root orchestration: `./gradlew buildAll`, `./gradlew testAll`, `./gradlew ciBuild` (tests run first).
- Backend: `cd ampairs-backend && ./gradlew bootRun`; add `SPRING_PROFILES_ACTIVE=test` for E2E; package with `./gradlew :ampairs_service:bootJar`.
- Web: `cd ampairs-web && npm install && npm start`; production bundles via `npm run build:prod`; lint with `npm run lint`.
- Multiplatform: `cd ampairs-mp-app && ./gradlew run`, `./gradlew installDebug`, `./gradlew package`.

## Coding Style & Naming Conventions
- Kotlin uses JetBrains defaults (4 spaces, `UpperCamelCase` types, `lowerCamelCase` members); group Spring stereotypes per module and prefer constructor injection with immutable DTOs (`*Dto`, `*Entity`).
- Angular components co-locate `.ts/.html/.scss`; selectors use the `amp-` prefix, classes stay `PascalCase`, and ESLint (`npm run lint`) enforces formatting.
- Compose shared UI stays in `shared/src/commonMain`; platform overrides belong in each launcher’s `src` tree.

## Testing Guidelines
- Backend: `cd ampairs-backend && ./gradlew test`; suites rely on JUnit 5 + Testcontainers, so keep Docker running.
- Web: `npm test` runs Karma/Jasmine; `npm run test:e2e:headless` covers Cypress flows before merging UI or API changes.
- Multiplatform: `cd ampairs-mp-app && ./gradlew check`; add `desktopTest` or `iosTest` when touching shared UI or native bridges. Aim for >80% backend coverage and add regression specs with fixes.

## Commit & Pull Request Guidelines
- Use Conventional Commits (`feat:`, `fix:`, `refactor:`); keep subjects ≤72 characters and link issues (`AMP-123`) in the body.
- PRs outline scope, affected modules, and validation commands; attach screenshots or API diffs when behavior or payloads shift.
- Request reviewers from the owning squad (backend, web, mobile) and wait for CI (`./gradlew ciBuild`, Angular/Cypress, multiplatform checks) to pass before merging.

## Security & Configuration Tips
- Keep secrets out of Git; rely on env vars (`SPRING_PROFILES_ACTIVE`, `RECAPTCHA_ENABLED`, `BUCKET4J_ENABLED`) and leave only redacted samples in `keys/`.
- Use `docker-compose.yml` for local dependencies and extend scripts in `scripts/` or `templates/` instead of introducing ad-hoc tooling.
