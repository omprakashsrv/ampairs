package com.ampairs.workspace.setting

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.setting.InstalledModulesProvider
import com.ampairs.workspace.repository.WorkspaceModuleRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Resolves the current workspace's enabled module codes for the `setting` module's definition
 * filtering. Implements the core [InstalledModulesProvider] SPI so `setting` stays decoupled from
 * `workspace`.
 */
@Component
class WorkspaceInstalledModulesProvider(
    private val workspaceModuleRepository: WorkspaceModuleRepository,
) : InstalledModulesProvider {

    @Transactional(readOnly = true)
    override fun enabledModuleCodes(): Set<String> {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return emptySet()
        return workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue(workspaceId)
            .map { it.masterModule.moduleCode }
            .filter { it.isNotBlank() }
            .toSet()
    }
}
