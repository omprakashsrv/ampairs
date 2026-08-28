package com.ampairs.connector.service

import com.ampairs.connector.domain.catalogue.CatalogueConnector
import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.repository.ConnectorInstallationRepository
import com.ampairs.core.setting.InstalledModulesProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

/**
 * Lists the connectors available to the current workspace and flags which are already installed.
 *
 * Gating (FR-002): a connector with a `requiredModule` is only offered when that module is enabled for
 * the workspace, via the core [InstalledModulesProvider] SPI (implemented in `workspace`). Injected as
 * an [ObjectProvider] so connector-only test contexts that lack the provider bean still load. (Tier
 * gating by `requiredTier` is metadata today; enforcing it needs a current-tier SPI — follow-up.)
 */
@Service
class ConnectorCatalogueService(
    private val registry: ConnectorCatalogueRegistry,
    private val installationRepository: ConnectorInstallationRepository,
    private val installedModulesProvider: ObjectProvider<InstalledModulesProvider>,
) {
    fun listAvailable(): List<Pair<CatalogueConnector, Boolean>> {
        val installedTypes = installationRepository.findByActiveTrue().map { it.connectorType }.toSet()
        val enabledModules = installedModulesProvider.ifAvailable?.enabledModuleCodes() ?: emptySet()
        return registry.all()
            .filter { it.requiredModule == null || it.requiredModule in enabledModules }
            .map { it to (it.type in installedTypes) }
    }
}
