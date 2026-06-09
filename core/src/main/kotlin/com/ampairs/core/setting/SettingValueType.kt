package com.ampairs.core.setting

/**
 * How a [SettingDefinition] value is interpreted. Values are always stored as a `String` and parsed
 * /validated against the declared type.
 */
enum class SettingValueType {
    BOOLEAN,
    INT,
    DECIMAL,
    STRING,
    ENUM,
    JSON;

    /** True when [raw] is a valid value for this type (allowed-value checks live in the definition). */
    fun isValid(raw: String): Boolean = when (this) {
        BOOLEAN -> raw == "true" || raw == "false"
        INT -> raw.toLongOrNull() != null
        DECIMAL -> raw.toBigDecimalOrNull() != null
        STRING, ENUM, JSON -> true
    }
}
