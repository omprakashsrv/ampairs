package com.ampairs.cb_maintenance.config

import com.ampairs.core.setting.SettingDefinition
import com.ampairs.core.setting.SettingDefinitionProvider
import com.ampairs.core.setting.SettingValueType
import org.springframework.stereotype.Component

/** cb_maintenance workspace settings, aggregated + gated by the `setting` module. */
@Component
class CbMaintenanceSettingDefinitions : SettingDefinitionProvider {

    override fun definitions(): List<SettingDefinition> = listOf(
        SettingDefinition(
            module = Constants.SETTING_MODULE,
            key = Constants.KEY_ESCALATION_THRESHOLD_DAYS,
            valueType = SettingValueType.INT,
            defaultValue = Constants.DEFAULT_ESCALATION_THRESHOLD_DAYS.toString(),
            label = "Overdue escalation threshold (days)",
            description = "Days a PM may be overdue before the assignee's manager is notified.",
            requiresModule = Constants.MODULE_CODE,
        ),
        SettingDefinition(
            module = Constants.SETTING_MODULE,
            key = Constants.KEY_PM_GENERATION_WINDOW_DAYS,
            valueType = SettingValueType.INT,
            defaultValue = Constants.DEFAULT_PM_GENERATION_WINDOW_DAYS.toString(),
            label = "PM generation window (days)",
            description = "How far ahead the nightly job rolls preventive-maintenance entries.",
            requiresModule = Constants.MODULE_CODE,
        ),
    )
}
