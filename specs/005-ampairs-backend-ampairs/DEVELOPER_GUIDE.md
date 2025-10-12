# Flyway Migration Developer Guide

This guide walks new contributors through the end-to-end workflow for adding Flyway migrations to the Ampairs backend.

## Getting Started
- Install Docker Desktop (or start your local Docker daemon).
- Install Java 25 and Gradle wrapper dependencies (`./gradlew --version` should succeed).
- Ensure MySQL client tooling is available (`mysql` CLI) for quick checks.
- Export environment variables for database access when running commands locally:
  ```bash
  export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ampairs?serverTimezone=UTC
  export SPRING_DATASOURCE_USERNAME=root
  export SPRING_DATASOURCE_PASSWORD=secret
  ```

## Creating Your First Migration
1. Review entity changes and confirm Kotlin annotations reflect the desired schema.
2. Determine the next migration number (`find ampairs-backend -path '*/db/migration/mysql/V*.sql' | sort | tail -n1`).
3. Copy the contract template from `specs/005-ampairs-backend-ampairs/contracts/migration-template.sql`.
4. Place the SQL file under the owning module (e.g., `ampairs-backend/customer/src/main/resources/db/migration/mysql/`).
5. Update the header (module name, version, description, author, dependencies).
6. Write DDL grouped by table, including indexes and foreign keys that match annotations.
7. Add inline comments for any intentional deviations (length mismatches, optional FK).
8. Run `./gradlew :ampairs_service:flywayValidate` against a local MySQL instance.
9. Commit with `feat(db): add migration V4_X create <module>` and push your branch.

## JPA to SQL Mapping Cheat Sheet
| Kotlin Type / Annotation | MySQL Column | Notes |
|--------------------------|--------------|-------|
| `String` (default) | `VARCHAR(255)` | Override with `@Column(length = N)` |
| `Int` / `Long` primary key | `BIGINT AUTO_INCREMENT` | Provided by `BaseDomain` |
| `Instant` | `TIMESTAMP` | Use `DEFAULT CURRENT_TIMESTAMP` and `ON UPDATE` for `updated_at` |
| `Boolean` | `BOOLEAN` | MySQL stores as `TINYINT(1)` but exposed as BOOLEAN |
| `Double` currency | `DOUBLE` or `DECIMAL(15,2)` | Prefer `DECIMAL` for money to avoid rounding |
| `@JdbcTypeCode(SqlTypes.JSON)` / `@Type(JsonType::class)` | `JSON` | Requires MySQL 5.7+ |
| `Point` (Spring Data) | `POINT` | Add SRID if geospatial queries needed |
| `@Enumerated(EnumType.STRING)` | `VARCHAR` (length sized for enum names) | Avoid ordinal enums |

## Testing Migrations Locally
1. Start disposable MySQL: `docker run -d --name ampairs-mysql -e MYSQL_ROOT_PASSWORD=root -p 3307:3306 mysql:8.0`.
2. Run migrations: `./gradlew :ampairs_service:flywayMigrate -Dspring.datasource.url=jdbc:mysql://localhost:3307/test_db?serverTimezone=UTC`.
3. Validate: `./gradlew :ampairs_service:flywayValidate`.
4. Run targeted tests: `./gradlew :ampairs_service:test --tests FlywayMigrationTest`.
5. Tear down container: `docker rm -f ampairs-mysql`.

## Common Pitfalls
- Forgetting audit columns (`created_at`, `updated_at`, `last_updated`) on new tables.
- Using `DATETIME` for `Instant` fields—always use `TIMESTAMP` with UTC semantics.
- Mismatched `VARCHAR` lengths between entity annotations and DDL.
- Missing indexes on foreign key columns leading to slow joins.
- Relying on `IF NOT EXISTS`—Flyway should fail fast instead.

## Debugging Failed Migrations
- Inspect `flyway_schema_history` for the last successful version.
- Review Flyway logs to identify the failing statement.
- Run the problematic SQL manually in MySQL to get exact error details.
- If a migration partially applied, fix the schema manually, then run `./gradlew :ampairs_service:flywayRepair`.
- Create a corrective migration rather than editing an existing script.

## IDE Setup Tips
- Enable SQL language injection in IntelliJ for `.sql` files to get syntax highlighting.
- Install the Flyway plugin (optional) to preview schema history within the IDE.
- Configure `Editor → Code Style → SQL` to align with project formatting (uppercase keywords, lowercase identifiers).
- Use the Database Tools window to connect to local Testcontainers or Docker MySQL instances for quick schema inspection.
