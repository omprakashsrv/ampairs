package com.ampairs.workspace.sync

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.workspace.repository.WorkspaceModuleRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the workspace module-enablement checkpoint (max `updatedAt` of the workspace's
 * installed modules) so the mobile client's connect/reconnect/hourly bootstrap re-pulls the
 * installed-module list when another device installs, uninstalls, or reorders a module.
 *
 * `WorkspaceModule` uses an explicit `workspaceId` column (not `@TenantId`), so the current tenant
 * is read from the ambient context and passed to the query explicitly.
 */
@Component
class WorkspaceModuleCheckpointContributor(
    private val workspaceModuleRepository: WorkspaceModuleRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return mapOf("module" to null)
        return mapOf("module" to workspaceModuleRepository.findMaxUpdatedAtByWorkspaceId(workspaceId))
    }
}
