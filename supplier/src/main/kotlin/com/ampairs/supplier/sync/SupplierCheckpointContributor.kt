package com.ampairs.supplier.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.supplier.repository.SupplierRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the supplier module's sync checkpoint (max `updatedAt`) for the current workspace.
 * The query is `@TenantId`-filtered, so it is automatically workspace-scoped.
 */
@Component
class SupplierCheckpointContributor(
    private val supplierRepository: SupplierRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "supplier" to supplierRepository.findMaxUpdatedAt(),
    )
}
