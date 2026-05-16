# API Response & Exception Handling

- ALL controller endpoints return `ApiResponse<T>` from `com.ampairs.core.domain.dto.ApiResponse`.
- Success: `return ApiResponse.success(data)`
- Errors bubble to the global exception handler — it emits `ApiResponse` with error details.
- NEVER use try/catch in controllers for business logic exceptions.
- Paginated endpoints wrap in `ApiResponse.success(PageResponse.from(page))`.
- Standard shape: `{ success, data, error, timestamp, path, traceId }`.
