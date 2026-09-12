package com.ampairs.cb_store.sync

import com.ampairs.cb_store.repository.StoreRepository
import com.ampairs.cb_store.repository.ZonalOfficeRepository
import com.ampairs.core.sync.SyncCheckpointContributor
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes cb_store's sync checkpoints (max `updatedAt` per resource) for the current workspace.
 * `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class CbStoreCheckpointContributor(
    private val storeRepository: StoreRepository,
    private val zonalOfficeRepository: ZonalOfficeRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "cb_store" to storeRepository.findMaxUpdatedAt(),
        "cb_zonal_office" to zonalOfficeRepository.findMaxUpdatedAt(),
    )
}
