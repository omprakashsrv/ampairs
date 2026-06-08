package com.ampairs.order.config

import com.ampairs.core.setting.SettingDefinition
import com.ampairs.core.setting.SettingDefinitionProvider
import com.ampairs.core.setting.SettingValueType
import org.springframework.stereotype.Component

/** Order module's own workspace settings, aggregated by the `setting` module. */
@Component
class OrderSettingDefinitions : SettingDefinitionProvider {

    override fun definitions(): List<SettingDefinition> = listOf(
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
