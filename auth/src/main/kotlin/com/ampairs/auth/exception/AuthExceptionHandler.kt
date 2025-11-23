package com.ampairs.auth.exception

import com.ampairs.auth.service.JwtTokenGenerationException
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.core.exception.BaseExceptionHandler
import com.ampairs.user.service.ProfilePictureNotFoundException
import com.ampairs.user.service.ProfilePictureValidationException
import io.jsonwebtoken.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Execute before global handler
class AuthExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCodes.AUTHENTICATION_FAILED,
            message = "Authentication failed",
            details = ex.message ?: "Invalid credentials",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(
        ex: BadCredentialsException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCodes.INVALID_CREDENTIALS,
            message = "Invalid credentials",
            details = "Username or password is incorrect",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.FORBIDDEN,
            errorCode = ErrorCodes.ACCESS_DENIED,
            message = "Access denied",
            details = ex.message ?: "You don't have permission to access this resource",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredJwtException(
        ex: ExpiredJwtException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCodes.TOKEN_EXPIRED,
            message = "Token expired",
            details = "JWT token has expired. Please refresh your token.",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(UnsupportedJwtException::class)
    fun handleUnsupportedJwtException(
        ex: UnsupportedJwtException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCodes.TOKEN_INVALID,
            message = "Unsupported token",
            details = "JWT token format is not supported",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(MalformedJwtException::class)
    fun handleMalformedJwtException(
        ex: MalformedJwtException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCodes.TOKEN_INVALID,
            message = "Invalid token format",
            details = "JWT token is malformed",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(SignatureException::class)
    fun handleSignatureException(
        ex: SignatureException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCodes.TOKEN_INVALID,
            message = "Invalid token signature",
            details = "JWT token signature is invalid",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(JwtException::class)
    fun handleJwtException(
        ex: JwtException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCodes.TOKEN_INVALID,
            message = "Invalid token",
            details = ex.message ?: "JWT token is invalid",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(JwtTokenGenerationException::class)
    fun handleJwtTokenGenerationException(
        ex: JwtTokenGenerationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("JWT token generation failed for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.TOKEN_GENERATION_FAILED,
            message = "Token generation failed",
            details = "Unable to generate authentication token",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(
        ex: UserNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.NOT_FOUND,
            message = "User not found",
            details = ex.message ?: "The requested user was not found",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(InvalidSessionException::class)
    fun handleInvalidSessionException(
        ex: InvalidSessionException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCodes.AUTHENTICATION_FAILED,
            message = "Invalid session",
            details = ex.message ?: "Session is invalid or expired",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(InsufficientPermissionsException::class)
    fun handleInsufficientPermissionsException(
        ex: InsufficientPermissionsException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.FORBIDDEN,
            errorCode = ErrorCodes.INSUFFICIENT_PERMISSIONS,
            message = "Insufficient permissions",
            details = ex.message ?: "You don't have sufficient permissions for this action",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(RecaptchaValidationException::class)
    fun handleRecaptchaValidationException(
        ex: RecaptchaValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.warn("reCAPTCHA validation failed for request {}: {}", request.requestURI, ex.message)
        
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Security verification failed",
            details = "Please complete the security verification",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(RecaptchaTokenMissingException::class)
    fun handleRecaptchaTokenMissingException(
        ex: RecaptchaTokenMissingException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Security token required",
            details = ex.message ?: "Security verification token is required",
            request = request,
            moduleName = "auth"
        )
    }

    @ExceptionHandler(ProfilePictureValidationException::class)
    fun handleProfilePictureValidationException(
        ex: ProfilePictureValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Profile picture validation failed",
            details = ex.message ?: "Invalid profile picture",
            request = request,
            moduleName = "user"
        )
    }

    @ExceptionHandler(ProfilePictureNotFoundException::class)
    fun handleProfilePictureNotFoundException(
        ex: ProfilePictureNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.NOT_FOUND,
            message = "Profile picture not found",
            details = ex.message ?: "Profile picture not found",
            request = request,
            moduleName = "user"
        )
    }
}

// Custom auth exceptions
class UserNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidSessionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InsufficientPermissionsException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)