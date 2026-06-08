package com.ampairs.core.setting

/**
 * Declares a single setting a module supports: its key, type, default and validation. Definitions
 * live in code (not the DB) and are aggregated from every [SettingDefinitionProvider]. The DB only
 * stores *overrides* of these defaults; the effective value is `default ⊕ stored override`.
 *
 * Definitions enforce the module boundary: only declared `(module, key)` pairs may be persisted,
 * and a pushed value must parse to [valueType] (and be in [allowedValues] for ENUM types).
 *
 * Lives in `core` (like `SyncCheckpointContributor`) so any domain module can declare its own
 * settings without depending on the `setting` module.
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
    /** Composite identity used to match a stored setting row. */
    fun identity(): String = "$module/$key"

    /** Validates a candidate raw value against this definition's type and allowed values. */
    fun accepts(raw: String): Boolean {
        if (!valueType.isValid(raw)) return false
        if (valueType == SettingValueType.ENUM && allowedValues.isNotEmpty() && raw !in allowedValues) return false
        return true
    }
}
