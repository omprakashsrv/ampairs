# Database Migrations

- All schema changes ship via Flyway — NEVER enable `ddl-auto: create/update` outside isolated tests.
- Migration paths (write BOTH — Flyway resolves `db/migration/{vendor}` and the dev/runtime DB is PostgreSQL):
  - `{module}/src/main/resources/db/migration/mysql/`
  - `{module}/src/main/resources/db/migration/postgresql/`
  - A migration that exists only under `mysql/` will silently not run on Postgres (table missing at schema validation).
- New modules must be added to `migrationModules` in `ampairs_service/build.gradle.kts` (used by the Gradle flyway tasks).
- Naming: `V{semver}__description.sql` (e.g. `V1.0.42__add_product_barcode.sql`)
- NEVER modify an applied migration — write a new version.
- Check `./gradlew :ampairs_service:flywayInfo` before choosing the next version number.
- Document no-op changes in `NO_MIGRATION_NEEDED.md`.
