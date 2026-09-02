package com.ampairs.cb_maintenance.config

object Constants {
    const val TICKET_PREFIX = "TKT"
    const val PM_SCHEDULE_PREFIX = "PMS"
    const val PM_ENTRY_PREFIX = "PME"
    const val ASSET_CATEGORY_ALIAS_PREFIX = "ACA"

    /** Backend module code used for workspace module-enablement gating. */
    const val MODULE_CODE = "cb-maintenance"

    /** setting module keys (module = "cb_maintenance"). */
    const val SETTING_MODULE = "cb_maintenance"
    const val KEY_ESCALATION_THRESHOLD_DAYS = "escalation_threshold_days"
    const val KEY_PM_GENERATION_WINDOW_DAYS = "pm_generation_window_days"

    const val DEFAULT_ESCALATION_THRESHOLD_DAYS = 2
    const val DEFAULT_PM_GENERATION_WINDOW_DAYS = 30
}
