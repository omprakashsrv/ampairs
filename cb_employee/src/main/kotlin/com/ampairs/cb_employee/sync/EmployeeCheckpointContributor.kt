package com.ampairs.cb_employee.sync

import com.ampairs.cb_employee.repository.EmployeeRepository
import com.ampairs.core.sync.SyncCheckpointContributor
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the cb_employee module's sync checkpoint (max `updatedAt`) for the current workspace.
 * `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class EmployeeCheckpointContributor(
    private val employeeRepository: EmployeeRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "cb_employee" to employeeRepository.findMaxUpdatedAt(),
    )
}
