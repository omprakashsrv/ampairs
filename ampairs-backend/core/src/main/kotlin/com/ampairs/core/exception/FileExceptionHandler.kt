package com.ampairs.core.exception

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.core.domain.service.FileAccessException
import com.ampairs.core.domain.service.FileDeletionException
import com.ampairs.core.domain.service.FileNotFoundException
import com.ampairs.core.domain.service.FileUploadException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 20) // Execute before global handler
class FileExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFoundException(
        ex: FileNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.FILE_NOT_FOUND,
            message = "File not found",
            details = ex.message ?: "The requested file was not found",
            request = request,
            moduleName = "file"
        )
    }

    @ExceptionHandler(FileUploadException::class)
    fun handleFileUploadException(
        ex: FileUploadException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("File upload failed for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.FILE_UPLOAD_FAILED,
            message = "File upload failed",
            details = ex.message ?: "Failed to upload file",
            request = request,
            moduleName = "file"
        )
    }

    @ExceptionHandler(FileDeletionException::class)
    fun handleFileDeletionException(
        ex: FileDeletionException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("File deletion failed for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.FILE_DELETION_FAILED,
            message = "File deletion failed",
            details = ex.message ?: "Failed to delete file",
            request = request,
            moduleName = "file"
        )
    }

    @ExceptionHandler(FileAccessException::class)
    fun handleFileAccessException(
        ex: FileAccessException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("File access failed for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.FORBIDDEN,
            errorCode = ErrorCodes.FILE_ACCESS_DENIED,
            message = "File access denied",
            details = ex.message ?: "Access to the file is denied",
            request = request,
            moduleName = "file"
        )
    }

    @ExceptionHandler(NoSuchKeyException::class)
    fun handleNoSuchKeyException(
        ex: NoSuchKeyException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.FILE_NOT_FOUND,
            message = "File not found in storage",
            details = "The requested file does not exist in S3 storage",
            request = request,
            moduleName = "file"
        )
    }

    @ExceptionHandler(NoSuchBucketException::class)
    fun handleNoSuchBucketException(
        ex: NoSuchBucketException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("S3 bucket not found for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.FILE_UPLOAD_FAILED,
            message = "Storage bucket not found",
            details = "The configured storage bucket does not exist",
            request = request,
            moduleName = "file"
        )
    }

    @ExceptionHandler(S3Exception::class)
    fun handleS3Exception(
        ex: S3Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("S3 operation failed for request {}: {}", request.requestURI, ex.message, ex)

        val (httpStatus, errorCode, message) = when {
            ex.statusCode() == 403 -> Triple(
                HttpStatus.FORBIDDEN,
                ErrorCodes.FILE_ACCESS_DENIED,
                "Storage access denied"
            )

            ex.statusCode() == 404 -> Triple(
                HttpStatus.NOT_FOUND,
                ErrorCodes.FILE_NOT_FOUND,
                "File not found in storage"
            )

            ex.statusCode() >= 500 -> Triple(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCodes.FILE_UPLOAD_FAILED,
                "Storage service error"
            )

            else -> Triple(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.FILE_UPLOAD_FAILED,
                "Storage operation failed"
            )
        }

        return createErrorResponse(
            httpStatus = httpStatus,
            errorCode = errorCode,
            message = message,
            details = ex.message,
            request = request,
            moduleName = "file"
        )
    }

    @ExceptionHandler(InvalidFileTypeException::class)
    fun handleInvalidFileTypeException(
        ex: InvalidFileTypeException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.BAD_REQUEST,
            message = "Invalid file type",
            details = ex.message ?: "The uploaded file type is not allowed",
            request = request,
            moduleName = "file"
        )
    }

    @ExceptionHandler(FileSizeExceededException::class)
    fun handleFileSizeExceededException(
        ex: FileSizeExceededException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.PAYLOAD_TOO_LARGE,
            errorCode = ErrorCodes.FILE_SIZE_EXCEEDED,
            message = "File size exceeded",
            details = ex.message ?: "File size exceeds the maximum allowed limit",
            request = request,
            moduleName = "file"
        )
    }
}

// Additional file-related exceptions
class InvalidFileTypeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileSizeExceededException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)