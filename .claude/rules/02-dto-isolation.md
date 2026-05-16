# DTO Isolation

- NEVER expose JPA entities directly in controller responses.
- All API inputs use Request DTOs; all API outputs use Response DTOs — both in `domain/dto/`.
- Entity ↔ DTO conversion via extension functions: `entity.asEntityResponse()`, `request.toEntity()`.
- Request DTOs must include validation annotations (`@field:NotBlank`, `@Valid`, etc.).
- Response DTOs expose only client-required fields — never internal IDs, flags, or audit columns.
