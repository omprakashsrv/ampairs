package com.ampairs.invoice.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.invoice.repository.InvoiceRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the invoice module's sync checkpoint (max `updatedAt`) for the current workspace.
 * `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class InvoiceCheckpointContributor(
    private val invoiceRepository: InvoiceRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "invoice" to invoiceRepository.findMaxUpdatedAt(),
    )
}
