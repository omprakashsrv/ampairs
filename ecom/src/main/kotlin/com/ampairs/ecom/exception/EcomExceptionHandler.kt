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
        createErrorResponse(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Storefront not found", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(StorefrontSlugConflictException::class)
    fun handleSlugConflict(ex: StorefrontSlugConflictException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.DUPLICATE_ENTRY, "Slug already taken", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(StorefrontAlreadyExistsException::class)
    fun handleStorefrontAlreadyExists(ex: StorefrontAlreadyExistsException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.DUPLICATE_ENTRY, "Storefront already exists", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(ProductUnavailableException::class)
    fun handleProductUnavailable(ex: ProductUnavailableException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.CONSTRAINT_VIOLATION, "Product unavailable", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.CONSTRAINT_VIOLATION, "Insufficient stock", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(CartExpiredException::class)
    fun handleCartExpired(ex: CartExpiredException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Cart not found or expired", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(EmptyCartException::class)
    fun handleEmptyCart(ex: EmptyCartException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Cart is empty", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(InvalidDeliveryAddressException::class)
    fun handleInvalidDeliveryAddress(ex: InvalidDeliveryAddressException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid delivery address", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(EcomOrderNotFoundException::class)
    fun handleEcomOrderNotFound(ex: EcomOrderNotFoundException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Order not found", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(InvalidOrderStatusTransitionException::class)
    fun handleInvalidStatusTransition(ex: InvalidOrderStatusTransitionException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.CONSTRAINT_VIOLATION, "Invalid status transition", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(StoreAccessDeniedException::class)
    fun handleStoreAccessDenied(ex: StoreAccessDeniedException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.FORBIDDEN, "STORE_ACCESS_DENIED", "Access denied", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(StoreUnauthenticatedException::class)
    fun handleStoreUnauthenticated(ex: StoreUnauthenticatedException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.FORBIDDEN, "STORE_UNAUTHENTICATED", "Authentication required", ex.message, request, moduleName = "ecom")

    @ExceptionHandler(EcomNotLinkedException::class)
    fun handleEcomNotLinked(ex: EcomNotLinkedException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.FORBIDDEN, "ECOM_NOT_LINKED", "Not linked to a distributor", ex.message, request, moduleName = "ecom")
}

// Ecom domain exceptions
class StorefrontNotFoundException(message: String) : RuntimeException(message)
class StorefrontSlugConflictException(message: String) : RuntimeException(message)
class StorefrontAlreadyExistsException(message: String) : RuntimeException(message)
class ProductUnavailableException(message: String) : RuntimeException(message)
class InsufficientStockException(message: String, val availableQuantity: Int) : RuntimeException(message)
class CartExpiredException(message: String) : RuntimeException(message)
class EmptyCartException(message: String) : RuntimeException(message)
class InvalidDeliveryAddressException(message: String) : RuntimeException(message)
class EcomOrderNotFoundException(message: String) : RuntimeException(message)
class InvalidOrderStatusTransitionException(message: String) : RuntimeException(message)
class StoreAccessDeniedException(message: String) : RuntimeException(message)
class StoreUnauthenticatedException(message: String) : RuntimeException(message)

/**
 * Thrown at checkout when the storefront buyer is not linked to any workspace distributor account.
 * Ordering is blocked until the workspace owner links the buyer (an explicit contact, or a CRM
 * customer created with the buyer's phone). Surfaces as HTTP 403 with code `ECOM_NOT_LINKED`.
 */
class EcomNotLinkedException(message: String) : RuntimeException(message)
