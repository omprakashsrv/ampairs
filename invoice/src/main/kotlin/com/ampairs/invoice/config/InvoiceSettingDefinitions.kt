package com.ampairs.invoice.config

import com.ampairs.core.setting.SettingDefinition
import com.ampairs.core.setting.SettingDefinitionProvider
import com.ampairs.core.setting.SettingValueType
import org.springframework.stereotype.Component

/** Invoice module's own workspace settings, aggregated by the `setting` module. */
@Component
class InvoiceSettingDefinitions : SettingDefinitionProvider {

    override fun definitions(): List<SettingDefinition> = listOf(
        SettingDefinition(
            module = "invoice",
            key = "show_discount_options",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Show discount options on invoices",
            description = "Controls whether discount fields are displayed while creating an invoice.",
            requiresModule = "invoice-billing",
        ),
        SettingDefinition(
            module = "invoice",
            key = "series_prefix",
            valueType = SettingValueType.STRING,
            defaultValue = "",
            label = "Invoice series prefix",
            description = "Prefix for client-assigned invoice numbers (e.g. MH27). Blank uses the default INV series.",
            requiresModule = "invoice-billing",
        ),
        SettingDefinition(
            module = "invoice",
            key = "financial_year",
            valueType = SettingValueType.STRING,
            defaultValue = "",
            label = "Financial year segment",
            description = "Optional financial-year segment appended to the series prefix (e.g. 26-27 → MH27/26-27/0001). Ignored when the prefix is blank.",
            requiresModule = "invoice-billing",
        ),
    )
}
