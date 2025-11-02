package com.ampairs.file.service

import com.ampairs.core.service.ValidationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.MessageDigest
import javax.imageio.ImageIO

/**
 * Comprehensive file validation service for secure file uploads
 * Provides validation against malicious files, size limits, and content verification
 */
@Service
class FileValidationService(
    private val validationService: ValidationService,
) {

    private val logger = LoggerFactory.getLogger(FileValidationService::class.java)

    companion object {
        // File size limits (in bytes)
        const val MAX_IMAGE_SIZE = 10 * 1024 * 1024 // 10MB
        const val MAX_DOCUMENT_SIZE = 50 * 1024 * 1024 // 50MB
        const val MIN_FILE_SIZE = 1 // 1 byte minimum

        // Image dimensions
        const val MAX_IMAGE_WIDTH = 4096
        const val MAX_IMAGE_HEIGHT = 4096
        const val MIN_IMAGE_WIDTH = 1
        const val MIN_IMAGE_HEIGHT = 1

        // Magic number validation for file type verification
        private val MAGIC_NUMBERS = mapOf(
            // Images
            "image/jpeg" to listOf(
                byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
                byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()),
                byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte())
            ),
            "image/png" to listOf(
                byteArrayOf(
                    0x89.toByte(),
                    0x50.toByte(),
                    0x4E.toByte(),
                    0x47.toByte(),
                    0x0D.toByte(),
                    0x0A.toByte(),
                    0x1A.toByte(),
                    0x0A.toByte()
                )
            ),
            "image/gif" to listOf(
                byteArrayOf(0x47.toByte(), 0x49.toByte(), 0x46.toByte(), 0x38.toByte(), 0x37.toByte(), 0x61.toByte()),
                byteArrayOf(0x47.toByte(), 0x49.toByte(), 0x46.toByte(), 0x38.toByte(), 0x39.toByte(), 0x61.toByte())
            ),
            "image/webp" to listOf(
                byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte())
            ),

            // Documents
            "application/pdf" to listOf(
                byteArrayOf(0x25.toByte(), 0x50.toByte(), 0x44.toByte(), 0x46.toByte())
            ),

            // Microsoft Office
            "application/msword" to listOf(
                byteArrayOf(
                    0xD0.toByte(),
                    0xCF.toByte(),
                    0x11.toByte(),
                    0xE0.toByte(),
                    0xA1.toByte(),
                    0xB1.toByte(),
                    0x1A.toByte(),
                    0xE1.toByte()
                )
            ),
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to listOf(
                byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte())
            )
        )

        // Dangerous file signatures that should be blocked
        private val DANGEROUS_SIGNATURES = listOf(
            byteArrayOf(0x4D.toByte(), 0x5A.toByte()), // PE executable
            byteArrayOf(0x7F.toByte(), 0x45.toByte(), 0x4C.toByte(), 0x46.toByte()), // ELF executable
            byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte()), // Java class file
            byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCE.toByte()), // Mach-O executable
            byteArrayOf(0x3C.toByte(), 0x3F.toByte(), 0x70.toByte(), 0x68.toByte(), 0x70.toByte()), // PHP script
            byteArrayOf(0x3C.toByte(), 0x25.toByte()) // JSP/ASP script
        )
    }

    /**
     * Comprehensive file validation
     */
    fun validateFile(
        file: MultipartFile,
        allowedTypes: Set<String>? = null,
        maxSize: Long? = null,
    ): FileValidationResult {
        val errors = mutableListOf<String>()

        try {
            // Basic file checks
            if (file.isEmpty) {
                errors.add("File is empty")
                return FileValidationResult(false, errors)
            }

            val originalFilename = file.originalFilename
            if (originalFilename.isNullOrBlank()) {
                errors.add("Filename is required")
                return FileValidationResult(false, errors)
            }

            // Validate filename
            val sanitizedFilename = validationService.sanitizeFilename(originalFilename)
            if (sanitizedFilename != originalFilename) {
                logger.warn("Filename sanitized: {} -> {}", originalFilename, sanitizedFilename)
            }

            if (!validationService.isValidFileExtension(sanitizedFilename, allowedTypes)) {
                errors.add("Invalid file extension")
                return FileValidationResult(false, errors)
            }

            // Validate file size
            val fileSize = file.size
            val effectiveMaxSize = maxSize ?: getDefaultMaxSize(file.contentType)
            if (!validationService.isValidFileSize(fileSize, effectiveMaxSize)) {
                errors.add("File size exceeds maximum allowed limit")
                return FileValidationResult(false, errors)
            }

            if (fileSize < MIN_FILE_SIZE) {
                errors.add("File size is too small")
                return FileValidationResult(false, errors)
            }

            // Validate content type
            val contentType = file.contentType
            if (!validationService.isValidContentType(
                    contentType,
                    allowedTypes?.let { getContentTypesForExtensions(it) })
            ) {
                errors.add("Invalid content type")
                return FileValidationResult(false, errors)
            }

            // Get file bytes for advanced validation
            val fileBytes = file.bytes

            // Validate file signature (magic number)
            if (!validateFileSignature(fileBytes, contentType)) {
                errors.add("File content doesn't match declared type")
                return FileValidationResult(false, errors)
            }

            // Check for dangerous signatures
            if (containsDangerousSignature(fileBytes)) {
                errors.add("File contains dangerous content")
                logger.warn("Dangerous file signature detected in file: {}", sanitizedFilename)
                return FileValidationResult(false, errors)
            }

            // Additional validation for images
            if (contentType?.startsWith("image/") == true) {
                val imageValidation = validateImage(fileBytes)
                if (!imageValidation.isValid) {
                    errors.addAll(imageValidation.errors)
                    return FileValidationResult(false, errors)
                }
            }

            // Scan for embedded scripts or malicious content
            if (containsMaliciousContent(fileBytes)) {
                errors.add("File contains potentially malicious content")
                logger.warn("Malicious content detected in file: {}", sanitizedFilename)
                return FileValidationResult(false, errors)
            }

            return FileValidationResult(true, emptyList(), sanitizedFilename, fileSize, calculateChecksum(fileBytes))

        } catch (e: Exception) {
            logger.error("Error validating file: {}", e.message, e)
            errors.add("File validation failed due to internal error")
            return FileValidationResult(false, errors)
        }
    }

    private fun getDefaultMaxSize(contentType: String?): Long {
        return when {
            contentType?.startsWith("image/") == true -> MAX_IMAGE_SIZE.toLong()
            else -> MAX_DOCUMENT_SIZE.toLong()
        }
    }

    private fun getContentTypesForExtensions(extensions: Set<String>): Set<String> {
        val contentTypes = mutableSetOf<String>()

        extensions.forEach { ext ->
            when (ext.lowercase()) {
                "jpg", "jpeg" -> contentTypes.add("image/jpeg")
                "png" -> contentTypes.add("image/png")
                "gif" -> contentTypes.add("image/gif")
                "webp" -> contentTypes.add("image/webp")
                "svg" -> contentTypes.add("image/svg+xml")
                "pdf" -> contentTypes.add("application/pdf")
                "doc" -> contentTypes.add("application/msword")
                "docx" -> contentTypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                "xls" -> contentTypes.add("application/vnd.ms-excel")
                "xlsx" -> contentTypes.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                "txt" -> contentTypes.add("text/plain")
            }
        }

        return contentTypes
    }

    private fun validateFileSignature(fileBytes: ByteArray, contentType: String?): Boolean {
        if (contentType == null || fileBytes.isEmpty()) return false

        val magicNumbers = MAGIC_NUMBERS[contentType]
        if (magicNumbers == null) return true // No validation available for this type

        return magicNumbers.any { signature ->
            if (fileBytes.size >= signature.size) {
                signature.indices.all { i -> fileBytes[i] == signature[i] }
            } else {
                false
            }
        }
    }

    private fun containsDangerousSignature(fileBytes: ByteArray): Boolean {
        return DANGEROUS_SIGNATURES.any { signature ->
            if (fileBytes.size >= signature.size) {
                signature.indices.all { i -> fileBytes[i] == signature[i] }
            } else {
                false
            }
        }
    }

    private fun validateImage(imageBytes: ByteArray): FileValidationResult {
        val errors = mutableListOf<String>()

        try {
            val inputStream = ByteArrayInputStream(imageBytes)
            val image = ImageIO.read(inputStream)

            if (image == null) {
                errors.add("Invalid or corrupted image")
                return FileValidationResult(false, errors)
            }

            val width = image.width
            val height = image.height

            if (width < MIN_IMAGE_WIDTH || height < MIN_IMAGE_HEIGHT) {
                errors.add("Image dimensions too small")
                return FileValidationResult(false, errors)
            }

            if (width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
                errors.add("Image dimensions too large")
                return FileValidationResult(false, errors)
            }

            return FileValidationResult(true, emptyList())

        } catch (e: IOException) {
            logger.warn("Error reading image: {}", e.message)
            errors.add("Unable to process image file")
            return FileValidationResult(false, errors)
        }
    }

    private fun containsMaliciousContent(fileBytes: ByteArray): Boolean {
        // Convert bytes to string for pattern matching
        val contentString = String(fileBytes, Charsets.ISO_8859_1).lowercase()

        // Check for embedded scripts
        val maliciousPatterns = listOf(
            "<script", "</script>", "javascript:", "vbscript:",
            "<?php", "<%", "%>", "<jsp:", "<%@",
            "eval(", "exec(", "system(", "shell_exec(",
            "base64_decode", "gzinflate", "str_rot13"
        )

        return maliciousPatterns.any { pattern -> contentString.contains(pattern) }
    }

    private fun calculateChecksum(fileBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(fileBytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validate multiple files in a batch
     */
    fun validateFiles(
        files: List<MultipartFile>,
        allowedTypes: Set<String>? = null,
        maxSize: Long? = null,
    ): BatchValidationResult {
        val results = mutableListOf<FileValidationResult>()
        var allValid = true

        files.forEach { file ->
            val result = validateFile(file, allowedTypes, maxSize)
            results.add(result)
            if (!result.isValid) {
                allValid = false
            }
        }

        return BatchValidationResult(allValid, results)
    }

    data class FileValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val sanitizedFilename: String? = null,
        val fileSize: Long? = null,
        val checksum: String? = null,
    )

    data class BatchValidationResult(
        val allValid: Boolean,
        val results: List<FileValidationResult>,
    )
}
