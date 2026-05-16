# Timestamps

- ALWAYS use `java.time.Instant` for all timestamp fields — entities, DTOs, and API responses.
- NEVER use `LocalDateTime` — it drops timezone context and causes DST bugs.
- Database columns must be `TIMESTAMP` (MySQL) or `TIMESTAMPTZ` (Postgres).
- JDBC connection strings must include `?serverTimezone=UTC`.
- JSON output is automatically ISO-8601 UTC (e.g. `"2025-01-09T14:30:00Z"`) when using `Instant`.
