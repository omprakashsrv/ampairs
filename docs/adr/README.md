# Architecture Decision Records (ADRs)

Short, dated records of significant architectural decisions: the context, the decision, and the
consequences. They make the *why* behind the codebase legible to future maintainers (and reviewers)
instead of living only in people's heads.

Format: one file per decision, `NNNN-kebab-title.md`, status `Accepted | Superseded | Deprecated`.

| # | Title | Status |
|---|-------|--------|
| [0001](0001-multi-tenancy-hibernate-tenant-id.md) | Row-level multi-tenancy via Hibernate `@TenantId` | Accepted |
| [0002](0002-unified-offline-sync-contract.md) | One canonical `/sync` contract for all syncable entities | Accepted |
| [0003](0003-flyway-vendor-parity-postgres-primary.md) | Flyway dual-vendor migrations; PostgreSQL is primary | Accepted |
| [0004](0004-otp-recaptcha-secure-defaults.md) | Secure-by-default OTP bypass and reCAPTCHA enforcement | Accepted |
