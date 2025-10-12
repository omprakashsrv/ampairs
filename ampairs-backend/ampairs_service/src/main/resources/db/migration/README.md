# Flyway Migration Conventions

This project stores all relational schema changes in `ampairs-backend/ampairs_service/src/main/resources/db/migration/`. The module aggregates entities from every backend Gradle module, so Flyway migrations are the single source of truth for MySQL DDL.

## Versioning Scheme
- Format: `V{major}_{minor}__{description}.sql` (e.g. `V4_3__create_customer_module_tables.sql`)
- Major version increments for large feature sets or production milestones.
- Minor version increments sequentially per feature branch; do not reuse or reorder numbers once merged.
- Existing history: `V3_1` (tax module), `V3_2` (event system). Retail management baseline begins at `V4_1`.

## Naming Conventions
- Keep descriptions lowercase with underscores, imperative verbs, and module grouping: `create_customer_module_tables`, `add_order_indexes`.
- Place vendor specific SQL under `db/migration/` (no `{vendor}` suffixes in filenames).
- Prefix module-specific content inside the script with comment banners (`-- =====================================================`).

### File Name Examples
- ✅ `V4_8__create_payment_tables.sql`
- ✅ `V5_1__add_order_shipment_columns.sql`
- ❌ `v4_8__CreatePaymentTables.sql` (lowercase `v`, camel case description)
- ❌ `V4.8__create-payment-tables.sql` (dot separator, hyphenated description)
- ❌ `V04_08__create_payment_tables.sql` (padded numbers cause lexical ordering issues)

## Migration Header Template
Each SQL file must begin with:
```
-- {Module} Database Migration Script
-- Version: {major.minor}
-- Description: {short summary}
-- Author: {engineer}
-- Date: {YYYY-MM-DD}
-- Dependencies: {comma-separated prior migrations}
```

## Determining the Next Version
1. Run `ls V*.sql | sort` to inspect the latest merged version.
2. Reserve the next available minor number on your feature branch (e.g. if `V4_7` exists, start at `V4_8`).
3. Document reserved ranges in pull requests for parallel branches (e.g. "Reserving V4_8–V4_9 for AMP-123").
4. If a merge conflict occurs, rename your migration with the next free number and update the header.

## Module Organization
- Group related tables per migration to keep files reviewable (e.g. one migration per bounded context).
- Order migrations according to dependency chain: core → unit → customer → product → inventory → order → invoice → form.
- Add inline comments when referential integrity cannot be enforced (length mismatches, optional domains).

## Validation Checklist
- File name matches `^V\d+_\d+__.+\.sql$`.
- Header present and accurate.
- All tables use `ENGINE=InnoDB`, `utf8mb4` charset, and include audit columns (`created_at`, `updated_at`, `last_updated`).
- Foreign keys and indexes mirror entity annotations; if deviations are required, explain them in comments.
- No `IF EXISTS`/`IF NOT EXISTS` statements (migrations must fail fast on drift).

## Useful Commands
- `./gradlew :ampairs_service:flywayInfo` — displays pending/applied migrations.
- `./gradlew :ampairs_service:flywayValidate` — validates checksums and schema.
- `./gradlew :ampairs_service:flywayMigrate` — applies migrations to the configured database.

### Naming Regex Guard (optional)
```bash
find ampairs-backend/ampairs_service/src/main/resources/db/migration -maxdepth 1 -name 'V*.sql' \
  | grep -Ev '^.*/V[0-9]+_[0-9]+__[a-z0-9_]+\.sql$' && echo '⚠️  Invalid migration filename detected'
```
Wire this command into a pre-commit hook or CI check to enforce the naming pattern when the list is non-empty.

## Git Commit Guidelines
- Use Conventional Commits: `feat(db): add migration V4_8 create delivery tables`.
- Reference tickets in the body (`AMP-123`).
- Never amend an already merged migration; create a new higher version to correct issues.

## Troubleshooting
- **Checksum mismatch**: rename the file or run `flyway repair` only after confirming with the team.
- **Out-of-order error**: enable `flyway.out-of-order=true` only locally to recover, then renumber migrations.
- **Duplicate version**: resolve by renaming before pushing; Flyway rejects duplicates at runtime.
- **Validation failure (length/type mismatch)**: adjust the SQL to match entity metadata or update the entity, then regenerate.

## Review Checklist
- SQL executes cleanly in MySQL 8.0 (no syntax errors or unsupported features).
- Column types, lengths, and nullability align with Kotlin entity annotations.
- Indexes cover unique constraints, foreign keys, and high-frequency search columns.
- Foreign keys use `CASCADE`, `RESTRICT`, or `SET NULL` deliberately and include inline comments for exceptions.
- Header metadata (version, author, dependencies) is complete and accurate.

## Common Review Findings
- `DATETIME` instead of `TIMESTAMP` for `Instant` properties (use `TIMESTAMP` with UTC semantics).
- Missing `NOT NULL` on columns annotated with `nullable = false`.
- VARCHAR lengths that do not match entity `@Column(length = ...)` values.
- Foreign keys omitted without an explanatory comment.
- JSON columns missing explicit `JSON` type declaration.

## Rollback Procedures
- Flyway Community Edition does **not** support automatic down migrations.
- Manual rollback involves dropping tables in reverse dependency order (form → invoice → order → inventory → product → customer → unit → core).
- Always capture production schema backups before applying corrective migrations.
- Use `flywayUndo` only in local development (enterprise feature) or execute targeted `DROP TABLE` / `ALTER TABLE DROP COLUMN` statements manually.

## Emergency Hotfix Migrations
- Reserve spare version numbers (gaps) when planning large feature migrations (e.g. leave `V4_10` open).
- If an emergency fix must run between existing scripts, create a new migration with a higher minor (e.g. `V4_10__add_missing_index.sql`) and document the dependency chain.
- Avoid renaming existing files—introduce a new version and communicate the ordering in the header `Dependencies` line.
