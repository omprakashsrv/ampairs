package com.ampairs.core.domain.dto

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object ResponseHelper {

    fun <T> success(data: T, path: String? = null): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity.ok(ApiResponse.success(data, path))
    }

    fun <T> created(data: T, path: String? = null): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(data, path))
    }

    fun noContent(): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.noContent().build()
    }

    fun <T> accepted(data: T, path: String? = null): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(data, path))
    }

    fun acceptedNoContent(path: String? = null): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(Unit, path))
    }

    // For paginated responses
    fun <T> successPaginated(
        data: T,
        page: Int,
        size: Int,
        totalElements: Long,
        totalPages: Int,
        path: String? = null,
    ): ResponseEntity<PaginatedResponse<T>> {
        return ResponseEntity.ok(
            PaginatedResponse(
                success = true,
                data = data,
                pagination = PaginationInfo(
                    page = page,
                    size = size,
                    totalElements = totalElements,
                    totalPages = totalPages
                ),
                path = path
            )
        )
    }
}

data class PaginatedResponse<T>(
    val success: Boolean,
    val data: T,
    val pagination: PaginationInfo,
    val path: String? = null,
    val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now(),
)

data class PaginationInfo(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean = page < totalPages - 1,
    val hasPrevious: Boolean = page > 0,
)