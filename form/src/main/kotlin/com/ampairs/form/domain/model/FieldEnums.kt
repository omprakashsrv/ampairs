package com.ampairs.form.domain.model

/** Where a field's value is stored: a built-in entity column, or the entity's `attributes` JSON map. */
enum class FieldSource(val value: String) {
    STANDARD("standard"),
    CUSTOM("custom");

    companion object {
        fun fromValue(value: String): FieldSource =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported field source: '$value'")
    }
}

/** Input data type of a field. `CUSTOM` is the escape hatch (pairs with a `widgetKey`). */
enum class FieldDataType(val value: String) {
    TEXT("text"),
    TEXTAREA("textarea"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    DATE("date"),
    CHOICE("choice"),
    MULTI_CHOICE("multi_choice"),
    CUSTOM("custom");

    val isChoice: Boolean get() = this == CHOICE || this == MULTI_CHOICE

    companion object {
        fun fromValue(value: String): FieldDataType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported data_type: '$value'")
    }
}

/** For CHOICE / MULTI_CHOICE fields, the origin of the selectable options. */
enum class OptionSource(val value: String) {
    STATIC("static"),
    DYNAMIC("dynamic");

    companion object {
        fun fromValue(value: String): OptionSource =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported option_source: '$value'")
    }
}
