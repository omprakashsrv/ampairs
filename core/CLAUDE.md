# core module

Shared infrastructure — consumed by all other modules. No business logic.

## Key classes
- `BaseDomain` — base entity: `uid` (prefixed nanoid), `createdAt: Instant`, `updatedAt: Instant`, `deleted: Boolean`
- `OwnableBaseDomain : BaseDomain` — adds `@TenantId ownerId`, `workspaceId`
- `ApiResponse<T>` — all endpoints return this: `{success, data, error, timestamp, path, traceId}`
- `PageResponse<T>` — paginated list wrapper
- `TenantContextHolder` — set/clear tenant on every workspace-scoped request
- `GlobalExceptionHandler` — all domain exceptions bubble here; never catch in controllers

## API endpoints
- `/core/v1/app-updates/**` — app version check + admin management
- `/core/v1/admin/api-keys/**` — API key CRUD
- `/core/v1/test` — dev/healthcheck endpoint

Note: `/api` prefix is set globally via `spring.mvc.servlet.path` in application.yml.

## Migrations
`V1.0.2`, `V1.0.17` (app_versions), `V1.0.18` (S3 streaming), `V1.0.19` (api_keys)

## Full docs
`docs/modules/core.md`
