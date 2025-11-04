package com.ampairs.product.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 50) // Execute before global handler
class ProductExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleProductNotFoundException(
        ex: ProductNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.PRODUCT_NOT_FOUND,
            message = "Product not found",
            details = ex.message ?: "The requested product was not found",
            request = request,
            moduleName = "product"
        )
    }

    @ExceptionHandler(InsufficientInventoryException::class)
    fun handleInsufficientInventoryException(
        ex: InsufficientInventoryException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.CONSTRAINT_VIOLATION,
            message = "Insufficient inventory",
            details = ex.message ?: "Not enough product quantity available in inventory",
            request = request,
            moduleName = "product"
        )
    }

    @ExceptionHandler(DuplicateProductException::class)
    fun handleDuplicateProductException(
        ex: DuplicateProductException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.DUPLICATE_ENTRY,
            message = "Duplicate product",
            details = ex.message ?: "A product with the same details already exists",
            request = request,
            moduleName = "product"
        )
    }

    @ExceptionHandler(InvalidProductDataException::class)
    fun handleInvalidProductDataException(
        ex: InvalidProductDataException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid product data",
            details = ex.message ?: "The provided product data is invalid",
            request = request,
            moduleName = "product"
        )
    }

    @ExceptionHandler(ProductCategoryNotFoundException::class)
    fun handleProductCategoryNotFoundException(
        ex: ProductCategoryNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.NOT_FOUND,
            message = "Product category not found",
            details = ex.message ?: "The requested product category was not found",
            request = request,
            moduleName = "product"
        )
    }

    @ExceptionHandler(InventoryUpdateException::class)
    fun handleInventoryUpdateException(
        ex: InventoryUpdateException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Inventory update failed for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.INTERNAL_SERVER_ERROR,
            message = "Inventory update failed",
            details = ex.message ?: "Failed to update product inventory",
            request = request,
            moduleName = "product"
        )
    }

    @ExceptionHandler(InvalidPriceException::class)
    fun handleInvalidPriceException(
        ex: InvalidPriceException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid price",
            details = ex.message ?: "The provided price is invalid",
            request = request,
            moduleName = "product"
        )
    }
}

// Custom product exceptions
class ProductNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InsufficientInventoryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class DuplicateProductException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidProductDataException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class ProductCategoryNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InventoryUpdateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidPriceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)