package com.ampairs.setting.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.setting.config.Constants
import com.ampairs.setting.repository.StoreSettingRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the setting module's sync checkpoint (max `updatedAt`) for the current workspace.
 * `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class SettingCheckpointContributor(
    private val repository: StoreSettingRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        Constants.SYNC_ENTITY_CODE to repository.findMaxUpdatedAt(),
    )
}
