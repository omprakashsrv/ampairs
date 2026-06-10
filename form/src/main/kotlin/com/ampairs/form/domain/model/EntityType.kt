package com.ampairs.form.domain.model

/**
 * The validated set of entity types a form schema can target (FR-024).
 * Replaces the previous free-form `entityType` strings — unknown values are rejected at the API
 * boundary. The canonical wire/storage value is the lowercase [value] (e.g. "customer").
 */
enum class EntityType(val value: String) {
    CUSTOMER("customer"),
    PRODUCT("product"),
    ORDER("order"),
    INVOICE("invoice"),
    BUSINESS("business");

    companion object {
        fun fromValue(value: String): EntityType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported entity_type: '$value'")

        fun isValid(value: String): Boolean = entries.any { it.value.equals(value, ignoreCase = true) }
    }
}
