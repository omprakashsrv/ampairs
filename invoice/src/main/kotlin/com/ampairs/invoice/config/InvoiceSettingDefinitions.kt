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
        ),
    )
}
