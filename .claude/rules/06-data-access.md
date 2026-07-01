# Data Access Patterns

- Prefer Spring Data JPA derived query methods (`findByActiveTrueOrderByName()`).
- Use `@Query` only when the method name cannot express the intent.
- Use `@EntityGraph` with `@NamedEntityGraph` to prevent N+1 queries — avoid `JOIN FETCH` in JPQL.
- Map entities → DTOs inside the service (session open), never in controllers. Paged + collection
  queries use `@BatchSize` + in-service mapping, not a paged `@EntityGraph`. See `11-lazy-loading.md`.
- Repositories are persistence-only — no business logic.
- Cross-module access goes through public service interfaces, never direct repository injection.
