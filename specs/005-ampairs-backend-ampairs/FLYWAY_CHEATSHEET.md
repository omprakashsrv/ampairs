# Flyway Quick Reference

## Core Commands
- `./gradlew :ampairs_service:flywayInfo` — Show applied, pending, and failed migrations.
- `./gradlew :ampairs_service:flywayValidate` — Check checksums and schema compatibility.
- `./gradlew :ampairs_service:flywayMigrate` — Apply pending migrations to the configured database.
- `./gradlew :ampairs_service:flywayBaseline -Dflyway.baselineVersion=0` — Manually baseline an existing schema.
- `./gradlew :ampairs_service:flywayRepair` — Repair failed migration entries after manual fixes.
- `./gradlew :ampairs_service:flywayClean` — **Development only**: drop all objects managed by Flyway.

## Handy SQL Snippets
- List migration history:
  ```sql
  SELECT installed_rank, version, description, success, installed_on
  FROM flyway_schema_history
  ORDER BY installed_rank;
  ```
- Show tables and row counts:
  ```sql
  SELECT table_name, table_rows
  FROM information_schema.tables
  WHERE table_schema = DATABASE();
  ```
- Inspect foreign keys for a table:
  ```sql
  SELECT constraint_name, column_name, referenced_table_name, referenced_column_name, delete_rule
  FROM information_schema.referential_constraints rc
  JOIN information_schema.key_column_usage kcu USING (constraint_name)
  WHERE rc.constraint_schema = DATABASE() AND rc.table_name = 'customer_order';
  ```
- List indexes on a table:
  ```sql
  SHOW INDEX FROM customer;
  ```

## Troubleshooting Matrix
| Error Message | Likely Cause | Resolution |
|---------------|-------------|------------|
| `Cannot add foreign key constraint` | Column type/length mismatch or missing parent index | Align column definitions, ensure parent column is indexed |
| `Checksum mismatch` | Migration file edited after apply | Revert file or create corrective migration, then `flywayRepair` |
| `Table already exists` | Migration re-runs against baselined schema | Confirm baseline version and remove duplicate `CREATE TABLE` |
| `Unknown column` during migrate | Entity and SQL out of sync | Update entity or migration to match and rerun |

## Test-driven Workflow
1. Write/modify migration script.
2. Run `./gradlew :ampairs_service:flywayValidate`.
3. Spin up disposable MySQL (`docker compose up mysql` or Testcontainers).
4. Execute `./gradlew :ampairs_service:flywayMigrate`.
5. Run integration tests (`./gradlew :ampairs_service:test --tests FlywayMigrationTest`).
6. Start application with `SPRING_PROFILES_ACTIVE=test` to ensure `ddl-auto:validate` passes.

Keep this cheat sheet nearby during reviews and on-call troubleshooting to shorten feedback loops.
