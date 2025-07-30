package com.ampairs.company.exception

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
class CompanyExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(CompanyNotFoundException::class)
    fun handleCompanyNotFoundException(
        ex: CompanyNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.COMPANY_NOT_FOUND,
            message = "Company not found",
            details = ex.message ?: "The requested company was not found",
            request = request,
            moduleName = "company"
        )
    }

    @ExceptionHandler(DuplicateCompanyException::class)
    fun handleDuplicateCompanyException(
        ex: DuplicateCompanyException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.DUPLICATE_ENTRY,
            message = "Duplicate company",
            details = ex.message ?: "A company with the same details already exists",
            request = request,
            moduleName = "company"
        )
    }

    @ExceptionHandler(InvalidCompanyDataException::class)
    fun handleInvalidCompanyDataException(
        ex: InvalidCompanyDataException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid company data",
            details = ex.message ?: "The provided company data is invalid",
            request = request,
            moduleName = "company"
        )
    }

    @ExceptionHandler(CompanyAccessDeniedException::class)
    fun handleCompanyAccessDeniedException(
        ex: CompanyAccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.FORBIDDEN,
            errorCode = ErrorCodes.ACCESS_DENIED,
            message = "Company access denied",
            details = ex.message ?: "You don't have permission to access this company",
            request = request,
            moduleName = "company"
        )
    }

    @ExceptionHandler(UserCompanyAssociationException::class)
    fun handleUserCompanyAssociationException(
        ex: UserCompanyAssociationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.CONSTRAINT_VIOLATION,
            message = "User company association error",
            details = ex.message ?: "Failed to manage user-company association",
            request = request,
            moduleName = "company"
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
            details = ex.message ?: "The specified role is not valid for this company",
            request = request,
            moduleName = "company"
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
            moduleName = "company"
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
            moduleName = "company"
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
            moduleName = "company"
        )
    }
}

// Custom company exceptions
class CompanyNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class DuplicateCompanyException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidCompanyDataException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class CompanyAccessDeniedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class UserCompanyAssociationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidRoleException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TenantNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TenantAccessDeniedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidTenantContextException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)