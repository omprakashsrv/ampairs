package com.ampairs.cb_employee.domain.model

/**
 * Maintenance-org roles for the workspace. Ordered loosely from field staff up to HQ.
 * `MAINTENANCE_LEADER` (the HQ maintenance leader) sits at the top of the escalation chain and is
 * the one blanket zone exemption — see cb_maintenance access control.
 */
enum class MaintenanceRole {
    EXECUTIVE,
    SENIOR_EXECUTIVE,
    ASSISTANT_MANAGER,
    MAINTENANCE_INCHARGE,
    MAINTENANCE_LEADER,
}
