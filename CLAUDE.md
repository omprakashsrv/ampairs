# CLAUDE.md

## Project

Ampairs — business management SaaS. Backend (Spring Boot 3.5 + Kotlin 2.2 + Java 25), Web (Angular 20 + M3, separate repo), Mobile (Compose Multiplatform, separate repo).

- 15 domain modules at root + `ampairs_service` aggregator
- Package convention: `com.ampairs.{module}.{domain|repository|service|controller}`
- Each module has its own `CLAUDE.md` — read it when working in that directory
- Full module docs: `docs/modules/`

## Non-negotiable rules (apply everywhere)

### 1. Timestamps — always `Instant`, never `LocalDateTime`
```kotlin
var createdAt: Instant = Instant.now()  // ✅
```
DB: `TIMESTAMP` (MySQL) / `TIMESTAMPTZ` (Postgres). JDBC URL: `?serverTimezone=UTC`.

### 2. DTO isolation — never expose JPA entities in API responses
```kotlin
fun Product.asProductResponse(): ProductResponse = ProductResponse(uid, name, ...)
```
Request/Response DTOs in `domain/dto/`. Validation with `@field:NotBlank`, `@Valid`.

### 3. JSON — global `SNAKE_CASE`, no `@JsonProperty` for standard fields
```kotlin
var countryCode: Int = 91   // → "country_code" automatically
```

### 4. API responses — always `ApiResponse<T>` wrapper
```kotlin
return ApiResponse.success(data)  // exceptions bubble to GlobalExceptionHandler
```

### 5. No try/catch in controllers for business exceptions
Let them bubble. Controllers handle HTTP only.

### 6. Tenant context — set at controller level, never in services
```kotlin
TenantContextHolder.setCurrentTenant(workspaceId)
try { ... } finally { TenantContextHolder.clear() }
```
Every workspace-scoped request requires `X-Workspace-ID` header.

### 7. `@EntityGraph` for relationships — prevent N+1
Define `@NamedEntityGraph` on entity. Use `@EntityGraph("Entity.graph")` on repo method.

### 8. Spring Data derived queries over `@Query`
Use `@Query` only when method name cannot express intent.

## Feature development workflow (speckit)
```
/speckit.specify → /speckit.clarify → /speckit.plan → /speckit.tasks → /speckit.analyze → /speckit.implement
```
Specs in `specs/{###-feature}/`. Next number: `006`.

## Build commands
```bash
./gradlew buildAll          # build all
./gradlew testAll           # test all (needs Docker)
./gradlew ciBuild           # CI gate
./gradlew :ampairs_service:bootRun
./gradlew :ampairs_service:flywayInfo
```

## Flyway migrations
- Path: `{module}/src/main/resources/db/migration/mysql/`
- Naming: `V{semver}__description.sql`
- Never modify applied migrations — write a new version

## Related repos
- Web: https://github.com/omprakashsrv/ampairs-web
- Mobile: https://github.com/omprakashsrv/ampairs-app
