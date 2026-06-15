package com.ampairs.connector.service

import com.ampairs.connector.domain.catalogue.CatalogueConnector
import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.repository.ConnectorInstallationRepository
import org.springframework.stereotype.Service

/**
 * Lists the connectors available to the current workspace and flags which are already installed.
 *
 * NOTE: subscription-tier / installed-module gating (FR-002) is not yet wired here — see task T011.
 * Today every catalogued connector is returned; gating will filter by `requiredModule`/`requiredTier`
 * against the workspace's installed modules and tier.
 */
@Service
class ConnectorCatalogueService(
    private val registry: ConnectorCatalogueRegistry,
    private val installationRepository: ConnectorInstallationRepository,
) {
    fun listAvailable(): List<Pair<CatalogueConnector, Boolean>> {
        val installedTypes = installationRepository.findByActiveTrue().map { it.connectorType }.toSet()
        return registry.all().map { it to (it.type in installedTypes) }
    }
}
