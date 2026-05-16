# Multi-Tenancy

- Tenant-scoped entities extend `OwnableBaseDomain` (provides `@TenantId ownerId`).
- Set tenant context at the CONTROLLER level, before any repository access, using try/finally:
  ```kotlin
  TenantContextHolder.setCurrentTenant(workspaceId)
  try { ... } finally { TenantContextHolder.clear() }
  ```
- Services MUST NOT call `TenantContextHolder.setCurrentTenant()`.
- Cross-tenant queries (admin, invite flows) MUST use `nativeQuery = true` to bypass auto-filtering.
- Every workspace-scoped request requires the `X-Workspace-ID` header — `SessionUserFilter` enforces this.
- Never use both `@TenantId` filtering AND explicit `workspaceId` parameter in the same query.
