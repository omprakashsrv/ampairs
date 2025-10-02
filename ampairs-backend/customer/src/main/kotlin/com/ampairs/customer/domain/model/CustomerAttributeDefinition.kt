package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type

/**
 * Predefined custom attribute definitions for customers
 * Enables workspace-specific custom fields based on business vertical
 * Example: Jewelry - preferred_metal, Kirana - preferred_delivery_time
 */
@Entity(name = "customer_attribute_definition")
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["owner_id", "attribute_key"])])
class CustomerAttributeDefinition : OwnableBaseDomain() {

    @Column(name = "attribute_key", nullable = false, length = 100)
    var attributeKey: String = ""

    @Column(name = "display_name", nullable = false, length = 255)
    var displayName: String = ""

    @Column(name = "data_type", nullable = false, length = 50)
    var dataType: String = "STRING" // STRING, NUMBER, BOOLEAN, DATE, ENUM, JSON

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true

    @Column(name = "mandatory", nullable = false)
    var mandatory: Boolean = false

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(name = "category", length = 100)
    var category: String? = null // e.g., "PREFERENCES", "BUSINESS_INFO", "DELIVERY"

    @Column(name = "default_value", length = 500)
    var defaultValue: String? = null

    @Column(name = "validation_type", length = 50)
    var validationType: String? = null // REGEX, LENGTH, RANGE, ENUM

    @Type(JsonType::class)
    @Column(name = "validation_params", columnDefinition = "json")
    var validationParams: Map<String, Any>? = null

    @Type(JsonType::class)
    @Column(name = "enum_values", columnDefinition = "json")
    var enumValues: List<String>? = null // For ENUM data type

    @Column(name = "placeholder", length = 255)
    var placeholder: String? = null

    @Column(name = "help_text", length = 500)
    var helpText: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.CUSTOMER_ATTRIBUTE_DEF_PREFIX
    }

    /**
     * Validate an attribute value against this definition
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

        // Validate data type
        val dataTypeResult = validateDataType(value)
        if (!dataTypeResult.valid) {
            return dataTypeResult
        }

        // Apply additional validation rules
        return when (validationType) {
            "REGEX" -> validateRegex(value.toString())
            "LENGTH" -> validateLength(value.toString())
            "RANGE" -> validateRange(value)
            "ENUM" -> validateEnum(value.toString())
            else -> ValidationResult(true)
        }
    }

    private fun validateDataType(value: Any): ValidationResult {
        return when (dataType) {
            "STRING" -> ValidationResult(true)
            "NUMBER" -> {
                val numValue = when (value) {
                    is Number -> value
                    is String -> value.toDoubleOrNull()
                    else -> null
                }
                if (numValue == null) {
                    ValidationResult(false, "$displayName must be a valid number")
                } else {
                    ValidationResult(true)
                }
            }
            "BOOLEAN" -> {
                val boolValue = when (value) {
                    is Boolean -> value
                    is String -> value.toBooleanStrictOrNull()
                    else -> null
                }
                if (boolValue == null) {
                    ValidationResult(false, "$displayName must be true or false")
                } else {
                    ValidationResult(true)
                }
            }
            "DATE" -> {
                try {
                    java.time.LocalDate.parse(value.toString())
                    ValidationResult(true)
                } catch (e: Exception) {
                    ValidationResult(false, "$displayName must be a valid date (YYYY-MM-DD)")
                }
            }
            "ENUM" -> {
                if (enumValues.isNullOrEmpty()) {
                    ValidationResult(true)
                } else {
                    validateEnum(value.toString())
                }
            }
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
        val allowedValues = enumValues ?: (validationParams?.get("values") as? List<String>)
            ?: return ValidationResult(true)

        return if (allowedValues.contains(value)) {
            ValidationResult(true)
        } else {
            ValidationResult(false, "$displayName must be one of: ${allowedValues.joinToString(", ")}")
        }
    }

    /**
     * Get the default value parsed according to data type
     */
    fun getParsedDefaultValue(): Any? {
        if (defaultValue == null) return null

        return when (dataType) {
            "STRING" -> defaultValue
            "NUMBER" -> defaultValue?.toDoubleOrNull()
            "BOOLEAN" -> defaultValue?.toBooleanStrictOrNull()
            "DATE" -> defaultValue
            "ENUM" -> if (enumValues?.contains(defaultValue) == true) defaultValue else null
            else -> defaultValue
        }
    }
}
