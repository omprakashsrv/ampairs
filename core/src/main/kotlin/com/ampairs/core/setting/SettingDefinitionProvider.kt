package com.ampairs.core.setting

/**
 * SPI implemented by any module that contributes setting definitions. The `setting` module
 * aggregates all providers (Spring injects them as a `List<SettingDefinitionProvider>`) into one
 * catalog used for push-time validation and default resolution.
 *
 * A module keeps ownership of its settings by registering its own `@Component` provider bean — the
 * `setting` module never hard-codes another module's keys. This mirrors `SyncCheckpointContributor`.
 */
interface SettingDefinitionProvider {
    fun definitions(): List<SettingDefinition>
}
