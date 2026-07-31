package com.ampairs.analytics.config

import com.ampairs.core.setting.SettingDefinition
import com.ampairs.core.setting.SettingDefinitionProvider
import com.ampairs.core.setting.SettingValueType
import org.springframework.stereotype.Component

/**
 * Analytics workspace settings, aggregated by the `setting` module (feature 022, T050). The dashboard
 * layout is an ordered, comma-separated list of enabled KPI tile keys — a UI preference that rides the
 * standard `SyncEntity.STORE` sync so add/remove/reorder propagates across a workspace's devices.
 *
 * Stored as a CSV STRING (mirroring `payment.aging_buckets`) rather than JSON to avoid a serializer
 * dependency; the mobile client decodes the keys against its `DashboardTile` enum. `requiresModule` is
 * null — the dashboard is always available, not a toggleable installed module.
 */
@Component
class AnalyticsSettingDefinitions : SettingDefinitionProvider {

    override fun definitions(): List<SettingDefinition> = listOf(
        SettingDefinition(
            module = "analytics",
            key = "dashboard_layout",
            valueType = SettingValueType.STRING,
            defaultValue = DEFAULT_LAYOUT,
            label = "Dashboard tile layout",
            description = "Comma-separated ordered list of enabled dashboard KPI tile keys.",
        ),
    )

    private companion object {
        const val DEFAULT_LAYOUT =
            "gross_sales,net_sales,tax,invoices,avg_invoice,collections,stock_value,low_stock,outstanding,inventory_turns"
    }
}
