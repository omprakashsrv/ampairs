package com.ampairs.setting.domain

/**
 * The data type a [com.ampairs.setting.domain.model.StoreSetting] value is interpreted as.
 *
 * Values are always stored as a `String` in the DB (TEXT) and parsed/validated against the type
 * declared in the matching [com.ampairs.setting.domain.definition.SettingDefinition].
 */
enum class SettingValueType {
    BOOLEAN,
    INT,
    DECIMAL,
    STRING,
    ENUM,
    JSON;

    /** Returns true when [raw] is a valid value for this type (allowed-value checks live in the definition). */
    fun isValid(raw: String): Boolean = when (this) {
        BOOLEAN -> raw == "true" || raw == "false"
        INT -> raw.toLongOrNull() != null
        DECIMAL -> raw.toBigDecimalOrNull() != null
        STRING, ENUM, JSON -> true
    }
}
