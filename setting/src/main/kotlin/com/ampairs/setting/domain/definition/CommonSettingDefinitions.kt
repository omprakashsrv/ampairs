package com.ampairs.setting.domain.definition

import com.ampairs.core.setting.SettingDefinition
import com.ampairs.core.setting.SettingDefinitionProvider
import com.ampairs.core.setting.SettingValueType
import org.springframework.stereotype.Component

/**
 * Cross-cutting ("common") setting definitions that aren't owned by a single domain module.
 *
 * Module-specific settings live in that module's own [SettingDefinitionProvider] (e.g.
 * `InvoiceSettingDefinitions`, `OrderSettingDefinitions`) — not here.
 */
@Component
class CommonSettingDefinitions : SettingDefinitionProvider {

    override fun definitions(): List<SettingDefinition> = listOf(
        SettingDefinition(
            module = "common",
            key = "prices_include_tax",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "false",
            label = "Prices include tax",
            description = "When enabled, product prices are treated as tax-inclusive across ordering and invoicing.",
        ),
    )
}
