package com.ampairs.core.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.regex.Pattern
import kotlin.reflect.KClass

/**
 * Custom validation annotations for common business patterns
 */

// Phone number validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PhoneValidator::class])
@MustBeDocumented
annotation class ValidPhone(
    val message: String = "Phone number must be 10 digits",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class PhoneValidator : ConstraintValidator<ValidPhone, String?> {
    private val phonePattern = Pattern.compile("^[0-9]{10}$")

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true // Let @NotNull handle null validation
        return phonePattern.matcher(value).matches()
    }
}

// Email validation (more strict than standard)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [EmailValidator::class])
@MustBeDocumented
annotation class ValidEmail(
    val message: String = "Invalid email format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class EmailValidator : ConstraintValidator<ValidEmail, String?> {
    private val emailPattern = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    )

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return value.length <= 254 && emailPattern.matcher(value).matches()
    }
}

// GSTIN validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [GstinValidator::class])
@MustBeDocumented
annotation class ValidGstin(
    val message: String = "Invalid GSTIN format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class GstinValidator : ConstraintValidator<ValidGstin, String?> {
    private val gstinPattern = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return gstinPattern.matcher(value).matches()
    }
}

// PAN validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PanValidator::class])
@MustBeDocumented
annotation class ValidPan(
    val message: String = "Invalid PAN format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class PanValidator : ConstraintValidator<ValidPan, String?> {
    private val panPattern = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return panPattern.matcher(value).matches()
    }
}

// Pincode validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PincodeValidator::class])
@MustBeDocumented
annotation class ValidPincode(
    val message: String = "Pincode must be 6 digits",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class PincodeValidator : ConstraintValidator<ValidPincode, String?> {
    private val pincodePattern = Pattern.compile("^[0-9]{6}$")

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return pincodePattern.matcher(value).matches()
    }
}

// Safe string validation (no malicious patterns)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [SafeStringValidator::class])
@MustBeDocumented
annotation class SafeString(
    val message: String = "Contains invalid or dangerous characters",
    val maxLength: Int = 1000,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class SafeStringValidator : ConstraintValidator<SafeString, String?> {

    @Autowired
    private lateinit var validationService: com.ampairs.core.service.ValidationService

    private var maxLength: Int = 1000

    override fun initialize(constraintAnnotation: SafeString) {
        this.maxLength = constraintAnnotation.maxLength
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true

        try {
            // This will throw SecurityException if malicious patterns are found
            validationService.validateAndSanitizeInput(value, "field", maxLength)
            return true
        } catch (e: SecurityException) {
            return false
        }
    }
}

// Alphanumeric validation with basic symbols
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AlphanumericValidator::class])
@MustBeDocumented
annotation class Alphanumeric(
    val message: String = "Can only contain letters, numbers, spaces, and basic symbols (._-)",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class AlphanumericValidator : ConstraintValidator<Alphanumeric, String?> {
    private val alphanumericPattern = Pattern.compile("^[a-zA-Z0-9\\s._-]+$")

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return alphanumericPattern.matcher(value).matches()
    }
}

// Country code validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [CountryCodeValidator::class])
@MustBeDocumented
annotation class ValidCountryCode(
    val message: String = "Country code must be between 1 and 9999",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class CountryCodeValidator : ConstraintValidator<ValidCountryCode, Int?> {
    override fun isValid(value: Int?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return false
        return value in 1..9999
    }
}

// Price validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidPriceValidator::class])
@MustBeDocumented
annotation class ValidPrice(
    val message: String = "Price must be a positive number with up to 2 decimal places",
    val min: Double = 0.0,
    val max: Double = 999999999.99,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class ValidPriceValidator : ConstraintValidator<ValidPrice, Double?> {
    private var min: Double = 0.0
    private var max: Double = 999999999.99

    override fun initialize(constraintAnnotation: ValidPrice) {
        this.min = constraintAnnotation.min
        this.max = constraintAnnotation.max
    }

    override fun isValid(value: Double?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true

        // Check range
        if (value < min || value > max) return false

        // Check decimal places (max 2)
        val decimalPlaces = value.toString().substringAfter('.', "").length
        return decimalPlaces <= 2
    }
}

// File extension validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FileExtensionValidator::class])
@MustBeDocumented
annotation class ValidFileExtension(
    val message: String = "Invalid file extension",
    val allowedExtensions: Array<String> = [],
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class FileExtensionValidator : ConstraintValidator<ValidFileExtension, String?> {

    @Autowired
    private lateinit var validationService: com.ampairs.core.service.ValidationService

    private var allowedExtensions: Set<String> = emptySet()

    override fun initialize(constraintAnnotation: ValidFileExtension) {
        this.allowedExtensions = constraintAnnotation.allowedExtensions.map { it.lowercase() }.toSet()
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true

        return if (allowedExtensions.isEmpty()) {
            validationService.isValidFileExtension(value)
        } else {
            validationService.isValidFileExtension(value, allowedExtensions)
        }
    }
}

// URL validation to prevent SSRF attacks
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidUrlValidator::class])
@MustBeDocumented
annotation class ValidUrl(
    val message: String = "Invalid or unsafe URL",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class ValidUrlValidator : ConstraintValidator<ValidUrl, String?> {

    @Autowired
    private lateinit var validationService: com.ampairs.core.service.ValidationService

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return validationService.isValidUrl(value)
    }
}

// Indian mobile number validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidIndianMobileValidator::class])
@MustBeDocumented
annotation class ValidIndianMobile(
    val message: String = "Invalid Indian mobile number",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class ValidIndianMobileValidator : ConstraintValidator<ValidIndianMobile, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        // Assuming country code is 91 for Indian mobile validation
        return value.matches(Regex("^[6-9]\\d{9}$"))
    }
}

// IFSC code validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidIfscValidator::class])
@MustBeDocumented
annotation class ValidIfsc(
    val message: String = "Invalid IFSC code format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class ValidIfscValidator : ConstraintValidator<ValidIfsc, String?> {

    @Autowired
    private lateinit var validationService: com.ampairs.core.service.ValidationService

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return validationService.isValidIfscCode(value)
    }
}

// Bank account number validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidBankAccountValidator::class])
@MustBeDocumented
annotation class ValidBankAccount(
    val message: String = "Invalid bank account number",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class ValidBankAccountValidator : ConstraintValidator<ValidBankAccount, String?> {

    @Autowired
    private lateinit var validationService: com.ampairs.core.service.ValidationService

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return validationService.isValidBankAccount(value)
    }
}

// Credit card format validation (for PCI DSS compliance)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidCreditCardValidator::class])
@MustBeDocumented
annotation class ValidCreditCard(
    val message: String = "Invalid credit card format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class ValidCreditCardValidator : ConstraintValidator<ValidCreditCard, String?> {

    @Autowired
    private lateinit var validationService: com.ampairs.core.service.ValidationService

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return validationService.isValidCreditCardFormat(value)
    }
}

// Enhanced amount validation for financial transactions
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidAmountValidator::class])
@MustBeDocumented
annotation class ValidAmount(
    val message: String = "Invalid amount",
    val min: Double = 0.01,
    val max: Double = 10000000.0,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class ValidAmountValidator : ConstraintValidator<ValidAmount, Double?> {
    private var min: Double = 0.01
    private var max: Double = 10000000.0

    override fun initialize(constraintAnnotation: ValidAmount) {
        this.min = constraintAnnotation.min
        this.max = constraintAnnotation.max
    }

    override fun isValid(value: Double?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true

        // Check range
        if (value < min || value > max) return false

        // Check decimal places (max 2 for currency)
        val decimalPlaces = value.toString().substringAfter('.', "").length
        return decimalPlaces <= 2
    }
}

// No malicious content validation (comprehensive)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NoMaliciousContentValidator::class])
@MustBeDocumented
annotation class NoMaliciousContent(
    val message: String = "Content contains potentially malicious patterns",
    val maxLength: Int = 10000,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class NoMaliciousContentValidator : ConstraintValidator<NoMaliciousContent, String?> {

    @Autowired
    private lateinit var validationService: com.ampairs.core.service.ValidationService

    private var maxLength: Int = 10000

    override fun initialize(constraintAnnotation: NoMaliciousContent) {
        this.maxLength = constraintAnnotation.maxLength
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true

        // Check length
        if (value.length > maxLength) return false

        // Check for SQL injection
        if (validationService.containsSqlInjection(value)) return false

        // Check for XSS
        if (validationService.containsXss(value)) return false

        return true
    }
}

// Search query validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidSearchQueryValidator::class])
@MustBeDocumented
annotation class ValidSearchQuery(
    val message: String = "Invalid search query",
    val maxLength: Int = 100,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class ValidSearchQueryValidator : ConstraintValidator<ValidSearchQuery, String?> {

    @Autowired
    private lateinit var validationService: com.ampairs.core.service.ValidationService

    private var maxLength: Int = 100

    override fun initialize(constraintAnnotation: ValidSearchQuery) {
        this.maxLength = constraintAnnotation.maxLength
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true

        // Use validation service to sanitize and validate
        val sanitized = validationService.sanitizeSearchQuery(value)
        return sanitized.isNotEmpty() && sanitized.length <= maxLength && !validationService.containsMaliciousPatterns(
            sanitized
        )
    }
}

// Add extension function to ValidationService for malicious patterns check
private fun com.ampairs.core.service.ValidationService.containsMaliciousPatterns(input: String): Boolean {
    return containsSqlInjection(input) || containsXss(input)
}