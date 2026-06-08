package com.ampairs.setting.domain.definition

import com.ampairs.setting.domain.SettingValueType
import org.springframework.stereotype.Component

/**
 * Initial built-in setting definitions (display / business-logic toggles shared across modules).
 *
 * As features grow, each owning module should register its **own** [SettingDefinitionProvider] bean
 * rather than adding keys here — this provider only seeds the first cross-cutting settings so the
 * module is useful out of the box.
 */
@Component
class CoreSettingDefinitions : SettingDefinitionProvider {

    override fun definitions(): List<SettingDefinition> = listOf(
        SettingDefinition(
            module = "common",
            key = "prices_include_tax",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "false",
            label = "Prices include tax",
            description = "When enabled, product prices are treated as tax-inclusive across ordering and invoicing.",
        ),
        SettingDefinition(
            module = "invoice",
            key = "show_discount_options",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Show discount options on invoices",
            description = "Controls whether discount fields are displayed while creating an invoice.",
        ),
        SettingDefinition(
            module = "order",
            key = "show_discount_options",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Show discount options on orders",
            description = "Controls whether discount fields are displayed while creating an order.",
        ),
    )
}
