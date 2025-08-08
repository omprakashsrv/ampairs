package com.ampairs.core.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Pattern

/**
 * Comprehensive validation and sanitization service for input security
 * Provides defense against injection attacks, XSS, and malicious input
 */
@Service
class ValidationService {

    private val logger = LoggerFactory.getLogger(ValidationService::class.java)

    companion object {
        // Common patterns for validation
        private val EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        )

        private val PHONE_PATTERN = Pattern.compile("^[0-9]{10}$")
        private val COUNTRY_CODE_PATTERN = Pattern.compile("^[0-9]{1,4}$")

        private val GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        private val PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")

        private val PINCODE_PATTERN = Pattern.compile("^[0-9]{6}$")
        private val ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s._-]+$")

        // Dangerous patterns that should be blocked
        private val SQL_INJECTION_PATTERNS = listOf(
            Pattern.compile("(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute).*"),
            Pattern.compile("(?i).*(script|javascript|vbscript|onload|onerror|onclick).*"),
            Pattern.compile(".*['\";].*--.*"),
            Pattern.compile(".*(/\\*|\\*/).*"),
            Pattern.compile(".*(<|>|&lt|&gt).*script.*"),
            Pattern.compile(".*\\b(xp_|sp_).*")
        )

        private val XSS_PATTERNS = listOf(
            Pattern.compile("(?i).*<script.*"),
            Pattern.compile("(?i).*</script>.*"),
            Pattern.compile("(?i).*javascript:.*"),
            Pattern.compile("(?i).*vbscript:.*"),
            Pattern.compile("(?i).*on(load|error|click|focus|blur|change|submit|reset)\\s*=.*"),
            Pattern.compile(".*<iframe.*"),
            Pattern.compile(".*<object.*"),
            Pattern.compile(".*<embed.*"),
            Pattern.compile(".*<link.*"),
            Pattern.compile(".*<meta.*http-equiv.*")
        )

        // File upload patterns
        private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "svg")
        private val ALLOWED_DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx", "txt")
        private val DANGEROUS_FILE_EXTENSIONS = setOf(
            "exe", "bat", "cmd", "com", "scr", "pif", "vbs", "js", "jar", "php", "asp", "jsp"
        )

        // Content type validation
        private val ALLOWED_IMAGE_CONTENT_TYPES = setOf(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
        )
        private val ALLOWED_DOCUMENT_CONTENT_TYPES = setOf(
            "application/pdf", "application/msword", "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
        )
    }

    /**
     * Sanitize string input by removing potentially dangerous characters
     */
    fun sanitizeString(input: String?, maxLength: Int = 1000): String {
        if (input.isNullOrBlank()) return ""

        var sanitized = input.trim()

        // Remove control characters (except tab, newline, carriage return)
        sanitized = sanitized.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")

        // Remove null bytes and other dangerous characters
        sanitized = sanitized.replace("\u0000", "")
        sanitized = sanitized.replace("\uFFFE", "")
        sanitized = sanitized.replace("\uFFFF", "")

        // Limit length to prevent buffer overflow attacks
        if (sanitized.length > maxLength) {
            sanitized = sanitized.substring(0, maxLength)
        }

        return sanitized
    }

    /**
     * Sanitize HTML by encoding dangerous characters
     */
    fun sanitizeHtml(input: String?): String {
        if (input.isNullOrBlank()) return ""

        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }

    /**
     * Check for SQL injection patterns
     */
    fun containsSqlInjection(input: String?): Boolean {
        if (input.isNullOrBlank()) return false

        val lowercaseInput = input.lowercase()
        return SQL_INJECTION_PATTERNS.any { pattern ->
            pattern.matcher(lowercaseInput).matches()
        }
    }

    /**
     * Check for XSS patterns
     */
    fun containsXss(input: String?): Boolean {
        if (input.isNullOrBlank()) return false

        val lowercaseInput = input.lowercase()
        return XSS_PATTERNS.any { pattern ->
            pattern.matcher(lowercaseInput).matches()
        }
    }

    /**
     * Validate and sanitize input against malicious patterns
     */
    fun validateAndSanitizeInput(input: String?, fieldName: String, maxLength: Int = 1000): String {
        if (input.isNullOrBlank()) return ""

        // Check for malicious patterns first
        if (containsSqlInjection(input)) {
            logger.warn("SQL injection attempt detected in field '{}': {}", fieldName, input.take(100))
            throw SecurityException("Invalid input detected in field: $fieldName")
        }

        if (containsXss(input)) {
            logger.warn("XSS attempt detected in field '{}': {}", fieldName, input.take(100))
            throw SecurityException("Invalid input detected in field: $fieldName")
        }

        return sanitizeString(input, maxLength)
    }

    /**
     * Validate email format
     */
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return EMAIL_PATTERN.matcher(email).matches() && email.length <= 254
    }

    /**
     * Validate phone number format
     */
    fun isValidPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        return PHONE_PATTERN.matcher(phone).matches()
    }

    /**
     * Validate country code format
     */
    fun isValidCountryCode(countryCode: Int?): Boolean {
        if (countryCode == null) return false
        return countryCode in 1..9999 && COUNTRY_CODE_PATTERN.matcher(countryCode.toString()).matches()
    }

    /**
     * Validate GSTIN format
     */
    fun isValidGstin(gstin: String?): Boolean {
        if (gstin.isNullOrBlank()) return true // GSTIN is optional
        return GSTIN_PATTERN.matcher(gstin).matches()
    }

    /**
     * Validate PAN format
     */
    fun isValidPan(pan: String?): Boolean {
        if (pan.isNullOrBlank()) return true // PAN is optional
        return PAN_PATTERN.matcher(pan).matches()
    }

    /**
     * Validate pincode format
     */
    fun isValidPincode(pincode: String?): Boolean {
        if (pincode.isNullOrBlank()) return true // Pincode is optional
        return PINCODE_PATTERN.matcher(pincode).matches()
    }

    /**
     * Validate alphanumeric string with common special characters
     */
    fun isValidAlphanumeric(input: String?, allowEmpty: Boolean = false): Boolean {
        if (input.isNullOrBlank()) return allowEmpty
        return ALPHANUMERIC_PATTERN.matcher(input).matches()
    }

    /**
     * Validate file extension
     */
    fun isValidFileExtension(filename: String?, allowedTypes: Set<String>? = null): Boolean {
        if (filename.isNullOrBlank()) return false

        val extension = filename.substringAfterLast('.', "").lowercase()

        // Block dangerous extensions regardless of allowed types
        if (DANGEROUS_FILE_EXTENSIONS.contains(extension)) {
            logger.warn("Dangerous file extension detected: {}", extension)
            return false
        }

        // If specific allowed types are provided, use them
        if (allowedTypes != null) {
            return allowedTypes.contains(extension)
        }

        // Default allowed extensions
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension) ||
                ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)
    }

    /**
     * Validate content type
     */
    fun isValidContentType(contentType: String?, expectedTypes: Set<String>? = null): Boolean {
        if (contentType.isNullOrBlank()) return false

        val normalizedContentType = contentType.lowercase().split(';')[0] // Remove charset info

        if (expectedTypes != null) {
            return expectedTypes.contains(normalizedContentType)
        }

        return ALLOWED_IMAGE_CONTENT_TYPES.contains(normalizedContentType) ||
                ALLOWED_DOCUMENT_CONTENT_TYPES.contains(normalizedContentType)
    }

    /**
     * Validate file size
     */
    fun isValidFileSize(fileSize: Long?, maxSizeBytes: Long = 10 * 1024 * 1024): Boolean { // 10MB default
        if (fileSize == null || fileSize <= 0) return false
        return fileSize <= maxSizeBytes
    }

    /**
     * Sanitize filename by removing dangerous characters
     */
    fun sanitizeFilename(filename: String?): String {
        if (filename.isNullOrBlank()) return ""

        // Remove path separators and dangerous characters
        var sanitized = filename
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .replace(Regex("\\.+"), ".") // Replace multiple dots with single dot
            .replace(Regex("^\\.|\\.$"), "") // Remove leading/trailing dots

        // Limit filename length
        if (sanitized.length > 255) {
            val extension = sanitized.substringAfterLast('.', "")
            val nameWithoutExtension = sanitized.substringBeforeLast('.', sanitized)
            val maxNameLength = 255 - extension.length - 1 // -1 for the dot
            sanitized = "${nameWithoutExtension.take(maxNameLength)}.$extension"
        }

        return sanitized
    }

    /**
     * Validate numeric range
     */
    fun isValidNumericRange(value: Number?, min: Number? = null, max: Number? = null): Boolean {
        if (value == null) return false

        val doubleValue = value.toDouble()

        if (min != null && doubleValue < min.toDouble()) return false
        if (max != null && doubleValue > max.toDouble()) return false

        return true
    }

    /**
     * Validate string length range
     */
    fun isValidStringLength(input: String?, minLength: Int = 0, maxLength: Int = Int.MAX_VALUE): Boolean {
        if (input == null) return minLength == 0
        return input.length in minLength..maxLength
    }

    /**
     * Comprehensive validation for customer data
     */
    fun validateCustomerData(
        name: String?,
        phone: String?,
        email: String?,
        gstin: String?,
        address: String?,
    ): List<String> {
        val errors = mutableListOf<String>()

        // Name validation
        if (name.isNullOrBlank()) {
            errors.add("Name is required")
        } else {
            try {
                val sanitizedName = validateAndSanitizeInput(name, "name", 100)
                if (sanitizedName.length < 2) {
                    errors.add("Name must be at least 2 characters long")
                }
            } catch (e: SecurityException) {
                errors.add("Name contains invalid characters")
            }
        }

        // Phone validation
        if (!phone.isNullOrBlank() && !isValidPhone(phone)) {
            errors.add("Phone number must be 10 digits")
        }

        // Email validation
        if (!email.isNullOrBlank() && !isValidEmail(email)) {
            errors.add("Invalid email format")
        }

        // GSTIN validation
        if (!isValidGstin(gstin)) {
            errors.add("Invalid GSTIN format")
        }

        // Address validation
        if (!address.isNullOrBlank()) {
            try {
                validateAndSanitizeInput(address, "address", 500)
            } catch (e: SecurityException) {
                errors.add("Address contains invalid characters")
            }
        }

        return errors
    }

    /**
     * Comprehensive validation for product data
     */
    fun validateProductData(
        name: String?,
        code: String?,
        price: Double?,
        description: String?,
    ): List<String> {
        val errors = mutableListOf<String>()

        // Name validation
        if (name.isNullOrBlank()) {
            errors.add("Product name is required")
        } else {
            try {
                val sanitizedName = validateAndSanitizeInput(name, "product_name", 200)
                if (sanitizedName.length < 2) {
                    errors.add("Product name must be at least 2 characters long")
                }
            } catch (e: SecurityException) {
                errors.add("Product name contains invalid characters")
            }
        }

        // Product code validation
        if (!code.isNullOrBlank()) {
            if (!isValidAlphanumeric(code)) {
                errors.add("Product code can only contain letters, numbers, and basic symbols")
            }
            if (code.length > 50) {
                errors.add("Product code cannot exceed 50 characters")
            }
        }

        // Price validation
        if (price != null && !isValidNumericRange(price, 0.0, 999999999.99)) {
            errors.add("Price must be between 0 and 999,999,999.99")
        }

        // Description validation
        if (!description.isNullOrBlank()) {
            try {
                validateAndSanitizeInput(description, "description", 1000)
            } catch (e: SecurityException) {
                errors.add("Description contains invalid characters")
            }
        }

        return errors
    }

    /**
     * Advanced sanitization for database queries - prevents SQL injection
     */
    fun sanitizeForDatabase(input: String?): String {
        if (input.isNullOrBlank()) return ""

        var sanitized = input.trim()

        // Escape single quotes
        sanitized = sanitized.replace("'", "''")

        // Remove null bytes and control characters
        sanitized = sanitized.replace("\u0000", "")

        // Remove or escape dangerous characters
        sanitized = sanitized.replace("--", "\\--")
        sanitized = sanitized.replace("/*", "\\/*")
        sanitized = sanitized.replace("*/", "\\*/")

        return sanitized
    }

    /**
     * Sanitize JSON input to prevent injection attacks
     */
    fun sanitizeJsonString(input: String?): String {
        if (input.isNullOrBlank()) return ""

        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
    }

    /**
     * Sanitize for logging to prevent log injection
     */
    fun sanitizeForLogging(input: String?, maxLength: Int = 1000): String {
        if (input.isNullOrBlank()) return ""

        var sanitized = input.trim()

        // Remove or replace line breaks and control characters
        sanitized = sanitized.replace(Regex("[\r\n\t]"), " ")
        sanitized = sanitized.replace(Regex("[\\x00-\\x1F\\x7F]"), "")

        // Limit length to prevent log flooding
        if (sanitized.length > maxLength) {
            sanitized = sanitized.substring(0, maxLength - 3) + "..."
        }

        return sanitized
    }

    /**
     * Validate URLs to prevent SSRF attacks
     */
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        try {
            val parsedUrl = java.net.URL(url)
            val protocol = parsedUrl.protocol.lowercase()

            // Only allow HTTP and HTTPS
            if (protocol !in listOf("http", "https")) {
                return false
            }

            // Block private/internal IP ranges
            val host = parsedUrl.host.lowercase()
            if (isPrivateIp(host) || isInternalHost(host)) {
                logger.warn("Blocked access to private/internal host: {}", host)
                return false
            }

            return true

        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Check if host is a private IP address
     */
    private fun isPrivateIp(host: String): Boolean {
        if (host.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))) {
            val parts = host.split(".")
            if (parts.size == 4) {
                val first = parts[0].toIntOrNull() ?: return false
                val second = parts[1].toIntOrNull() ?: return false

                return when (first) {
                    10 -> true
                    172 -> second in 16..31
                    192 -> second == 168
                    127 -> true // localhost
                    else -> false
                }
            }
        }
        return false
    }

    /**
     * Check if host is internal/reserved
     */
    private fun isInternalHost(host: String): Boolean {
        val internalHosts = setOf(
            "localhost", "127.0.0.1", "::1",
            "metadata.google.internal",
            "169.254.169.254" // AWS metadata service
        )

        return internalHosts.contains(host) ||
                host.endsWith(".internal") ||
                host.endsWith(".local")
    }

    /**
     * Sanitize CSV data to prevent CSV injection
     */
    fun sanitizeCsvCell(input: String?): String {
        if (input.isNullOrBlank()) return ""

        var sanitized = input.trim()

        // If cell starts with dangerous characters, prefix with single quote
        if (sanitized.matches(Regex("^[=+@-].*"))) {
            sanitized = "'$sanitized"
        }

        // Escape quotes in the content
        sanitized = sanitized.replace("\"", "\"\"")

        // Wrap in quotes if contains special characters
        if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r")) {
            sanitized = "\"$sanitized\""
        }

        return sanitized
    }

    /**
     * Validate and sanitize search query
     */
    fun sanitizeSearchQuery(query: String?): String {
        if (query.isNullOrBlank()) return ""

        var sanitized = query.trim()

        // Remove dangerous patterns
        sanitized = sanitized.replace(Regex("[<>\"'%;()&+]"), " ")

        // Normalize whitespace
        sanitized = sanitized.replace(Regex("\\s+"), " ")

        // Limit length
        if (sanitized.length > 100) {
            sanitized = sanitized.substring(0, 100)
        }

        return sanitized
    }

    /**
     * Validate credit card number (basic format validation only)
     */
    fun isValidCreditCardFormat(cardNumber: String?): Boolean {
        if (cardNumber.isNullOrBlank()) return false

        // Remove spaces and hyphens
        val cleaned = cardNumber.replace(Regex("[\\s-]"), "")

        // Check if all digits and length between 13-19
        if (!cleaned.matches(Regex("^\\d{13,19}$"))) {
            return false
        }

        // Basic Luhn algorithm check
        return isValidLuhn(cleaned)
    }

    /**
     * Luhn algorithm for credit card validation
     */
    private fun isValidLuhn(cardNumber: String): Boolean {
        var sum = 0
        var alternate = false

        for (i in cardNumber.length - 1 downTo 0) {
            var digit = cardNumber[i].toString().toInt()

            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit = digit / 10 + digit % 10
                }
            }

            sum += digit
            alternate = !alternate
        }

        return sum % 10 == 0
    }

    /**
     * Validate Indian mobile number with country code
     */
    fun isValidIndianMobile(phone: String?, countryCode: Int?): Boolean {
        if (phone.isNullOrBlank() || countryCode == null) return false

        // India country code is 91
        if (countryCode != 91) return false

        // Indian mobile numbers are 10 digits starting with 6-9
        return phone.matches(Regex("^[6-9]\\d{9}$"))
    }

    /**
     * Sanitize and validate bank account number
     */
    fun isValidBankAccount(accountNumber: String?): Boolean {
        if (accountNumber.isNullOrBlank()) return false

        // Remove spaces and special characters
        val cleaned = accountNumber.replace(Regex("[^\\d]"), "")

        // Indian bank account numbers are typically 9-18 digits
        return cleaned.matches(Regex("^\\d{9,18}$"))
    }

    /**
     * Validate Indian IFSC code
     */
    fun isValidIfscCode(ifsc: String?): Boolean {
        if (ifsc.isNullOrBlank()) return false

        // IFSC format: 4 letters + 7 characters (letters/numbers)
        return ifsc.matches(Regex("^[A-Za-z]{4}[A-Za-z0-9]{7}$"))
    }

    /**
     * Comprehensive validation for financial data
     */
    fun validateFinancialData(
        amount: Double?,
        accountNumber: String?,
        ifscCode: String?,
        description: String?,
    ): List<String> {
        val errors = mutableListOf<String>()

        // Amount validation
        if (amount == null) {
            errors.add("Amount is required")
        } else if (!isValidNumericRange(amount, 0.01, 10000000.0)) {
            errors.add("Amount must be between 0.01 and 10,000,000")
        }

        // Account number validation
        if (!accountNumber.isNullOrBlank() && !isValidBankAccount(accountNumber)) {
            errors.add("Invalid bank account number format")
        }

        // IFSC validation
        if (!ifscCode.isNullOrBlank() && !isValidIfscCode(ifscCode)) {
            errors.add("Invalid IFSC code format")
        }

        // Description validation
        if (!description.isNullOrBlank()) {
            try {
                validateAndSanitizeInput(description, "transaction_description", 500)
            } catch (e: SecurityException) {
                errors.add("Description contains invalid characters")
            }
        }

        return errors
    }
}