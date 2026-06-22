package com.ampairs.inventory.config

import com.ampairs.core.setting.SettingDefinition
import com.ampairs.core.setting.SettingDefinitionProvider
import com.ampairs.core.setting.SettingValueType
import org.springframework.stereotype.Component

/**
 * Inventory module's workspace settings, aggregated by the `setting` module (spec 014, R11 / FR-018).
 *
 * Replaces the dedicated `InventoryConfig` entity/table — inventory policy now lives in the central
 * settings registry under the `inventory` namespace. Effective values are read via
 * `SettingService.getBoolean("inventory", key)` (override falling back to the declared default here).
 *
 * All keys are gated by the installed module `inventory-management`, so they only surface for
 * workspaces that have inventory enabled.
 */
@Component
class InventorySettingDefinitions : SettingDefinitionProvider {

    override fun definitions(): List<SettingDefinition> = listOf(
        SettingDefinition(
            module = MODULE,
            key = KEY_AUTO_DEDUCT_ON_ORDER,
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Auto-deduct stock on sale",
            description = "When enabled, confirming an order/invoice automatically reduces stock for tracked items.",
            requiresModule = MODULE_CODE,
        ),
        SettingDefinition(
            module = MODULE,
            key = KEY_BLOCK_ORDERS_WHEN_OUT_OF_STOCK,
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "false",
            label = "Block sales when out of stock",
            description = "When enabled, a sale that would drive a tracked item below zero is rejected.",
            requiresModule = MODULE_CODE,
        ),
        SettingDefinition(
            module = MODULE,
            key = KEY_ALLOW_NEGATIVE_STOCK,
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "false",
            label = "Allow negative stock",
            description = "When enabled, sales may drive on-hand stock below zero.",
            requiresModule = MODULE_CODE,
        ),
        SettingDefinition(
            module = MODULE,
            key = KEY_ALLOW_MANUAL_OVERRIDE,
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Allow manual stock override",
            description = "When enabled, users may manually adjust stock outside automatic movements.",
            requiresModule = MODULE_CODE,
        ),
        SettingDefinition(
            module = MODULE,
            key = KEY_ENABLE_LOW_STOCK_ALERTS,
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Low-stock alerts",
            description = "When enabled, low-stock and out-of-stock alerts are generated for items.",
            requiresModule = MODULE_CODE,
        ),
    )

    companion object {
        /** Settings namespace for all inventory policy keys. */
        const val MODULE = "inventory"

        /** Installed-module code that gates these settings (matches the client ModuleRegistry code). */
        const val MODULE_CODE = "inventory-management"

        const val KEY_AUTO_DEDUCT_ON_ORDER = "auto_deduct_on_order"
        const val KEY_BLOCK_ORDERS_WHEN_OUT_OF_STOCK = "block_orders_when_out_of_stock"
        const val KEY_ALLOW_NEGATIVE_STOCK = "allow_negative_stock"
        const val KEY_ALLOW_MANUAL_OVERRIDE = "allow_manual_override"
        const val KEY_ENABLE_LOW_STOCK_ALERTS = "enable_low_stock_alerts"
    }
}
