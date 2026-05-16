# Database Migrations

- All schema changes ship via Flyway — NEVER enable `ddl-auto: create/update` outside isolated tests.
- Migration path: `{module}/src/main/resources/db/migration/mysql/`
- Naming: `V{semver}__description.sql` (e.g. `V1.0.42__add_product_barcode.sql`)
- NEVER modify an applied migration — write a new version.
- Check `./gradlew :ampairs_service:flywayInfo` before choosing the next version number.
- Document no-op changes in `NO_MIGRATION_NEEDED.md`.
