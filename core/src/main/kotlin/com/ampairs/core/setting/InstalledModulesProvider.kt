package com.ampairs.core.setting

/**
 * SPI for resolving which modules are installed/enabled for the current workspace.
 *
 * Implemented in the `workspace` module and consumed by the `setting` module so settings can be
 * filtered to the workspace's installed modules without `setting` depending on `workspace`. Relies
 * on the ambient tenant context (set per request), like the other core SPIs.
 */
interface InstalledModulesProvider {
    /** Enabled module codes (e.g. "invoice-billing", "order-management") for the current workspace. */
    fun enabledModuleCodes(): Set<String>
}
