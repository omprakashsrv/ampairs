package com.ampairs.setting.domain.definition

/**
 * Implemented by any module that contributes setting definitions. The setting module aggregates all
 * providers (Spring injects them as a `List<SettingDefinitionProvider>`) into one catalog used for
 * push-time validation and default resolution.
 *
 * A module keeps ownership of its settings by registering its own provider bean — the setting module
 * never hard-codes another module's keys.
 */
interface SettingDefinitionProvider {
    fun definitions(): List<SettingDefinition>
}
