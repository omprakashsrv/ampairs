package com.ampairs.workspace.exception

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.core.exception.BaseExceptionHandler
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 80) // Execute before global handler
class WorkspaceExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(WorkspaceNotFoundException::class)
    fun handleWorkspaceNotFoundException(
        ex: WorkspaceNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.WORKSPACE_NOT_FOUND,
            message = "Workspace not found",
            details = ex.message ?: "The requested workspace was not found",
            request = request,
            moduleName = "workspace"
        )
    }

    @ExceptionHandler(DuplicateWorkspaceException::class)
    fun handleDuplicateWorkspaceException(
        ex: DuplicateWorkspaceException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.DUPLICATE_ENTRY,
            message = "Duplicate workspace",
            details = ex.message ?: "A workspace with the same details already exists",
            request = request,
            moduleName = "workspace"
        )
    }

    @ExceptionHandler(InvalidWorkspaceDataException::class)
    fun handleInvalidWorkspaceDataException(
        ex: InvalidWorkspaceDataException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid workspace data",
            details = ex.message ?: "The provided workspace data is invalid",
            request = request,
            moduleName = "workspace"
        )
    }

    @ExceptionHandler(WorkspaceAccessDeniedException::class)
    fun handleWorkspaceAccessDeniedException(
        ex: WorkspaceAccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.FORBIDDEN,
            errorCode = ErrorCodes.ACCESS_DENIED,
            message = "Workspace access denied",
            details = ex.message ?: "You don't have permission to access this workspace",
            request = request,
            moduleName = "workspace"
        )
    }

    @ExceptionHandler(UserWorkspaceAssociationException::class)
    fun handleUserWorkspaceAssociationException(
        ex: UserWorkspaceAssociationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.CONSTRAINT_VIOLATION,
            message = "User workspace association error",
            details = ex.message ?: "Failed to manage user-workspace association",
            request = request,
            moduleName = "workspace"
        )
    }

    @ExceptionHandler(InvalidRoleException::class)
    fun handleInvalidRoleException(
        ex: InvalidRoleException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid role",
            details = ex.message ?: "The specified role is not valid for this workspace",
            request = request,
            moduleName = "workspace"
        )
    }

    @ExceptionHandler(TenantNotFoundException::class)
    fun handleTenantNotFoundException(
        ex: TenantNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.TENANT_NOT_FOUND,
            message = "Tenant not found",
            details = ex.message ?: "The requested tenant was not found",
            request = request,
            moduleName = "workspace"
        )
    }

    @ExceptionHandler(TenantAccessDeniedException::class)
    fun handleTenantAccessDeniedException(
        ex: TenantAccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.FORBIDDEN,
            errorCode = ErrorCodes.TENANT_ACCESS_DENIED,
            message = "Tenant access denied",
            details = ex.message ?: "You don't have permission to access this tenant",
            request = request,
            moduleName = "workspace"
        )
    }

    @ExceptionHandler(InvalidTenantContextException::class)
    fun handleInvalidTenantContextException(
        ex: InvalidTenantContextException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.INVALID_TENANT_CONTEXT,
            message = "Invalid tenant context",
            details = ex.message ?: "The tenant context is invalid or missing",
            request = request,
            moduleName = "workspace"
        )
    }
}

// Custom workspace exceptions
class WorkspaceNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class DuplicateWorkspaceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidWorkspaceDataException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class WorkspaceAccessDeniedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class UserWorkspaceAssociationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidRoleException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TenantNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TenantAccessDeniedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidTenantContextException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)