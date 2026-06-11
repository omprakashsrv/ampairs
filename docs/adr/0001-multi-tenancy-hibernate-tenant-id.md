# 0001 — Row-level multi-tenancy via Hibernate `@TenantId`

- Status: Accepted
- Date: 2026-06-10 (documenting a pre-existing decision)

## Context

Ampairs is a multi-tenant SaaS: many workspaces share one database. Tenant data must never leak
across workspaces, and the protection has to hold for *every* query without each service
remembering to add a `where workspace_id = ?` clause (the most common multi-tenant bug).

Options considered:

1. **Database-per-tenant** — strongest isolation, but operationally heavy (migrations × N, connection
   sprawl) and overkill at the current scale.
2. **Schema-per-tenant** — moderate isolation; still multiplies migration/DDL work and complicates
   pooling.
3. **Shared schema with a discriminator column** enforced by the ORM — one schema, one migration
   set, isolation centralized in one place.

## Decision

Use a **shared-schema discriminator** enforced by Hibernate. Tenant-scoped entities extend
`OwnableBaseDomain`, which carries `@TenantId var ownerId`. A `CurrentTenantIdentifierResolver`
(wired via `MultiTenancyConfiguration`) resolves the active tenant from the Spring Security context,
falling back to a `TenantContextHolder` thread-local. Hibernate then:

- stamps `ownerId` from the resolver on persist, and
- appends `owner_id = :currentTenant` to every read automatically.

Tenant context is set **at the controller/filter layer** (`SessionUserFilter`, keyed off the
`X-Workspace-ID` header) inside a `try/finally`, never inside services. Deliberate cross-tenant
queries (admin, invitations) must use `nativeQuery = true` to opt out of the filter.

## Consequences

- **Positive:** one schema and one migration set; isolation is centralized and declarative; services
  stay tenant-unaware; the common "forgot the workspace filter" bug is impossible for `@TenantId`
  entities.
- **Negative / risks:** a misconfigured resolver or an unguarded `nativeQuery` can bypass isolation;
  the `default` tenant fallback must never be reachable in production paths.
- **Testing:** because isolation is invisible at the call site, it must be tested explicitly — see
  `customer/.../TenantIsolationIntegrationTest` (added 2026-06). Note the Session/transaction timing
  gotcha documented there: Hibernate binds the tenant when the Session opens, so a test-managed
  `@Transactional` opens before the tenant is set and binds `default`.
