package com.ampairs.setting.domain.definition

import com.ampairs.setting.domain.SettingValueType

/**
 * Declares a single setting a module supports: its key, type, default and validation. Definitions
 * live in code (not the DB) and are aggregated from every [SettingDefinitionProvider]. The DB only
 * stores *overrides* of these defaults; the effective value is `default ⊕ stored override`.
 *
 * Definitions enforce the module boundary: only declared `(module, key)` pairs may be persisted,
 * and a pushed value must parse to [valueType] (and be in [allowedValues] for ENUM types).
 *
 * @property module the owning module code (e.g. "invoice", "order", "common").
 * @property key the setting key, unique within the module (e.g. "show_discount_options").
 * @property valueType how the stored string value is interpreted.
 * @property defaultValue the value used when no workspace override exists, as a string.
 * @property allowedValues for ENUM types, the permitted values; empty means unconstrained.
 * @property label human-readable label for a generic settings UI.
 * @property description optional longer help text.
 */
data class SettingDefinition(
    val module: String,
    val key: String,
    val valueType: SettingValueType,
    val defaultValue: String,
    val allowedValues: List<String> = emptyList(),
    val label: String,
    val description: String? = null,
) {
    /** Composite identity used to match a stored [com.ampairs.setting.domain.model.StoreSetting]. */
    fun identity(): String = "$module/$key"

    /** Validates a candidate raw value against this definition's type and allowed values. */
    fun accepts(raw: String): Boolean {
        if (!valueType.isValid(raw)) return false
        if (valueType == SettingValueType.ENUM && allowedValues.isNotEmpty() && raw !in allowedValues) return false
        return true
    }
}
