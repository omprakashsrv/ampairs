package com.ampairs.cb_maintenance.sync

import com.ampairs.cb_maintenance.repository.AssetCategoryAliasRepository
import com.ampairs.cb_maintenance.repository.PmEntryRepository
import com.ampairs.cb_maintenance.repository.PmScheduleRepository
import com.ampairs.cb_maintenance.repository.TicketRepository
import com.ampairs.core.sync.SyncCheckpointContributor
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes cb_maintenance's sync checkpoints (max `updatedAt` per resource) for the current
 * workspace. `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class CbMaintenanceCheckpointContributor(
    private val ticketRepository: TicketRepository,
    private val pmScheduleRepository: PmScheduleRepository,
    private val pmEntryRepository: PmEntryRepository,
    private val assetCategoryAliasRepository: AssetCategoryAliasRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "cb_ticket" to ticketRepository.findMaxUpdatedAt(),
        "cb_pm_schedule" to pmScheduleRepository.findMaxUpdatedAt(),
        "cb_pm_entry" to pmEntryRepository.findMaxUpdatedAt(),
        "cb_asset_alias" to assetCategoryAliasRepository.findMaxUpdatedAt(),
    )
}
