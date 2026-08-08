package com.ampairs.subscription.exception

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.exception.BaseExceptionHandler
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 72) // Execute before global handler
class SubscriptionExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(SubscriptionException::class)
    fun handleSubscriptionException(
        ex: SubscriptionException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> = createErrorResponse(
        httpStatus = ex.status,
        errorCode = ex.errorCode,
        message = ex.message,
        request = request,
        moduleName = "subscription",
    )
}
