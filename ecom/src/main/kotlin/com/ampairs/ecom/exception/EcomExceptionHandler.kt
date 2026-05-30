package com.ampairs.ecom.exception

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

@RestControllerAdvice(basePackages = ["com.ampairs.ecom"])
@Order(Ordered.HIGHEST_PRECEDENCE + 80)
class EcomExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(StorefrontNotFoundException::class)
    fun handleStorefrontNotFound(ex: StorefrontNotFoundException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Storefront not found", ex.message ?: "Storefront not found", request, "ecom")

    @ExceptionHandler(StorefrontSlugConflictException::class)
    fun handleSlugConflict(ex: StorefrontSlugConflictException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.DUPLICATE_ENTRY, "Slug already taken", ex.message ?: "A storefront with this slug already exists", request, "ecom")

    @ExceptionHandler(StorefrontAlreadyExistsException::class)
    fun handleStorefrontAlreadyExists(ex: StorefrontAlreadyExistsException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.DUPLICATE_ENTRY, "Storefront already exists", ex.message ?: "This workspace already has a storefront", request, "ecom")

    @ExceptionHandler(ProductUnavailableException::class)
    fun handleProductUnavailable(ex: ProductUnavailableException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.CONSTRAINT_VIOLATION, "Product unavailable", ex.message ?: "Product is not available", request, "ecom")

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> {
        val details = mapOf("available_quantity" to ex.availableQuantity, "message" to (ex.message ?: "Insufficient stock"))
        return createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.CONSTRAINT_VIOLATION, "Insufficient stock", details.toString(), request, "ecom")
    }

    @ExceptionHandler(CartExpiredException::class)
    fun handleCartExpired(ex: CartExpiredException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Cart not found or expired", ex.message ?: "Cart session has expired", request, "ecom")

    @ExceptionHandler(EmptyCartException::class)
    fun handleEmptyCart(ex: EmptyCartException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Cart is empty", ex.message ?: "Cannot checkout with an empty cart", request, "ecom")

    @ExceptionHandler(EcomOrderNotFoundException::class)
    fun handleEcomOrderNotFound(ex: EcomOrderNotFoundException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Order not found", ex.message ?: "Ecom order not found", request, "ecom")

    @ExceptionHandler(InvalidOrderStatusTransitionException::class)
    fun handleInvalidStatusTransition(ex: InvalidOrderStatusTransitionException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.CONSTRAINT_VIOLATION, "Invalid status transition", ex.message ?: "Order status transition is not allowed", request, "ecom")
}

// Ecom domain exceptions
class StorefrontNotFoundException(message: String) : RuntimeException(message)
class StorefrontSlugConflictException(message: String) : RuntimeException(message)
class StorefrontAlreadyExistsException(message: String) : RuntimeException(message)
class ProductUnavailableException(message: String) : RuntimeException(message)
class InsufficientStockException(message: String, val availableQuantity: Int) : RuntimeException(message)
class CartExpiredException(message: String) : RuntimeException(message)
class EmptyCartException(message: String) : RuntimeException(message)
class EcomOrderNotFoundException(message: String) : RuntimeException(message)
class InvalidOrderStatusTransitionException(message: String) : RuntimeException(message)
