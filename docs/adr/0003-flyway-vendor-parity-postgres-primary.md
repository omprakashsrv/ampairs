# 0003 — Flyway dual-vendor migrations; PostgreSQL is the primary vendor

- Status: Accepted
- Date: 2026-06-10

## Context

Schema changes ship as Flyway migrations under `{module}/src/main/resources/db/migration/{vendor}/`,
where `{vendor}` resolves to `mysql` or `postgresql`. Over time the two vendor trees drifted: ~15
migrations existed for only one vendor, `ecom` had **zero** MySQL migrations, and Flyway could not
even run against MySQL because `flyway-mysql` was not on the classpath. The dev/CI/production runtime
is PostgreSQL (PostGIS), so the drift was invisible until a MySQL target was attempted.

Two ad-hoc `fix-*.sql` scripts also sat at the repo root — one repairing a checksum after an *applied*
migration had been edited in place (a process violation), one backfilling data — neither under
Flyway's control.

## Decision

- **PostgreSQL is the primary, always-current vendor.** It is what dev, CI, and production run, and
  the codebase uses Postgres-specific features (PostGIS geometry, `tsvector` full-text, `jsonb`
  operators) that have no portable equivalent.
- **MySQL is kept as a supported secondary vendor at the schema level.** `flyway-mysql` is now a
  dependency and the Gradle `db*` tasks select the vendor directory from the JDBC URL. The missing
  MySQL migrations were authored so the two trees are at parity for DDL.
- **Known limitation:** a few repositories use Postgres-only *queries* (FTS, jsonb containment).
  MySQL schema parity does not make those queries run; converting them (`MATCH…AGAINST`,
  `JSON_CONTAINS`) is tracked in `NO_MIGRATION_NEEDED.md` and is out of scope until a MySQL
  deployment is actually required.
- **No more in-place edits or root `fix-*.sql`.** Repairs ship as new forward migrations; data fixes
  ship as migrations; the only sanctioned checksum repair is `./gradlew :ampairs_service:dbRepair`.
  A new PR CI job applies all migrations to a fresh Postgres and validates them, catching version
  collisions and parity breaks before merge.

## Consequences

- **Positive:** migrations apply cleanly on a fresh DB (gated in CI); the dual-vendor rule is real
  again rather than aspirational; migration history is immutable.
- **Negative:** every schema change now costs two files (one per vendor). MySQL migrations are
  currently authored by translation and are **not yet exercised by an integration test** — if MySQL
  becomes a real target, add a MySQL Testcontainers run to CI and port the Postgres-only queries.
