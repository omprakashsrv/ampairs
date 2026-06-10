package com.ampairs.form.domain.model.validation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue

/**
 * A structured, typed validation rule (FR-004) — never a free-form expression.
 * Stored as a JSON list on a field; interpreted identically by the client renderer and the backend.
 *
 * Only the fields relevant to [type] are populated:
 * - REQUIRED        — (no params)
 * - LENGTH_RANGE    — [min]/[max] character counts
 * - NUMBER_RANGE    — [minValue]/[maxValue]
 * - FORMAT          — [kind] from [FormatKind]
 * - ALLOWED_CHOICES — [values]
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ValidationRule(
    val type: ValidationRuleType,
    val min: Int? = null,
    val max: Int? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val kind: FormatKind? = null,
    val values: List<String>? = null,
)

enum class ValidationRuleType(@get:JsonValue val value: String) {
    REQUIRED("required"),
    LENGTH_RANGE("length_range"),
    NUMBER_RANGE("number_range"),
    FORMAT("format"),
    ALLOWED_CHOICES("allowed_choices");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): ValidationRuleType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown validation rule type: '$value'")
    }
}

/** Curated set of named formats — not arbitrary regex entered by admins. */
enum class FormatKind(@get:JsonValue val value: String) {
    EMAIL("email"),
    PHONE("phone"),
    GSTIN("gstin"),
    PAN("pan"),
    URL("url"),
    NUMERIC("numeric");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): FormatKind =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown format kind: '$value'")
    }
}
