package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type

/**
 * Configuration for customer fields to control visibility, mandatory status, and validations
 * Enables workspace-specific customization of customer fields based on business vertical
 */
@Entity(name = "customer_field_config")
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["owner_id", "field_name"])])
class CustomerFieldConfig : OwnableBaseDomain() {

    @Column(name = "field_name", nullable = false, length = 100)
    var fieldName: String = ""

    @Column(name = "display_name", nullable = false, length = 255)
    var displayName: String = ""

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true

    @Column(name = "mandatory", nullable = false)
    var mandatory: Boolean = false

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(name = "validation_type", length = 50)
    var validationType: String? = null // e.g., "REGEX", "LENGTH", "RANGE", "CUSTOM"

    @Type(JsonType::class)
    @Column(name = "validation_params", columnDefinition = "json")
    var validationParams: Map<String, Any>? = null // e.g., {"pattern": "^[0-9]+$", "min": 10, "max": 15}

    @Column(name = "placeholder", length = 255)
    var placeholder: String? = null

    @Column(name = "help_text", length = 500)
    var helpText: String? = null

    @Column(name = "default_value", length = 500)
    var defaultValue: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.CUSTOMER_FIELD_CONFIG_PREFIX
    }

    /**
     * Validate a value against configured validation rules
     */
    fun validateValue(value: Any?): ValidationResult {
        // Check mandatory
        if (mandatory && (value == null || value.toString().isBlank())) {
            return ValidationResult(false, "$displayName is mandatory")
        }

        // Skip validation if value is null/empty and not mandatory
        if (value == null || value.toString().isBlank()) {
            return ValidationResult(true)
        }

        // Apply validation type
        return when (validationType) {
            "REGEX" -> validateRegex(value.toString())
            "LENGTH" -> validateLength(value.toString())
            "RANGE" -> validateRange(value)
            "ENUM" -> validateEnum(value.toString())
            else -> ValidationResult(true)
        }
    }

    private fun validateRegex(value: String): ValidationResult {
        val pattern = validationParams?.get("pattern") as? String
            ?: return ValidationResult(true)

        return if (value.matches(Regex(pattern))) {
            ValidationResult(true)
        } else {
            val message = validationParams?.get("message") as? String
                ?: "$displayName format is invalid"
            ValidationResult(false, message)
        }
    }

    private fun validateLength(value: String): ValidationResult {
        val min = (validationParams?.get("min") as? Number)?.toInt()
        val max = (validationParams?.get("max") as? Number)?.toInt()

        if (min != null && value.length < min) {
            return ValidationResult(false, "$displayName must be at least $min characters")
        }
        if (max != null && value.length > max) {
            return ValidationResult(false, "$displayName must not exceed $max characters")
        }
        return ValidationResult(true)
    }

    private fun validateRange(value: Any): ValidationResult {
        val numValue = when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: return ValidationResult(false, "$displayName must be a number")
            else -> return ValidationResult(false, "$displayName must be a number")
        }

        val min = (validationParams?.get("min") as? Number)?.toDouble()
        val max = (validationParams?.get("max") as? Number)?.toDouble()

        if (min != null && numValue < min) {
            return ValidationResult(false, "$displayName must be at least $min")
        }
        if (max != null && numValue > max) {
            return ValidationResult(false, "$displayName must not exceed $max")
        }
        return ValidationResult(true)
    }

    private fun validateEnum(value: String): ValidationResult {
        @Suppress("UNCHECKED_CAST")
        val allowedValues = validationParams?.get("values") as? List<String>
            ?: return ValidationResult(true)

        return if (allowedValues.contains(value)) {
            ValidationResult(true)
        } else {
            ValidationResult(false, "$displayName must be one of: ${allowedValues.joinToString(", ")}")
        }
    }
}

/**
 * Result of field validation
 */
data class ValidationResult(
    val valid: Boolean,
    val message: String? = null
)
