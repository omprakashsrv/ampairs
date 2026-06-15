package com.ampairs.connector.service

import com.ampairs.connector.config.Constants
import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.domain.model.ConnectorInstallation
import com.ampairs.connector.domain.model.InstallationStatus
import com.ampairs.connector.exception.ConnectorErrors
import com.ampairs.connector.repository.ConnectorInstallationRepository
import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.sync.EntityChangePublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Install / uninstall / lifecycle of per-workspace connector installations. Tenant context is set by
 * `SessionUserFilter` from `X-Workspace-ID`; `@TenantId` scopes all reads/writes automatically.
 */
@Service
class ConnectorInstallationService(
    private val repository: ConnectorInstallationRepository,
    private val registry: ConnectorCatalogueRegistry,
    private val entityChangePublisher: EntityChangePublisher,
) {
    fun listInstallations(): List<ConnectorInstallation> = repository.findByActiveTrue()

    fun getInstallation(uid: String): ConnectorInstallation =
        repository.findByUid(uid)?.takeIf { it.active }
            ?: throw NotFoundException("Connector installation not found: $uid")

    @Transactional
    fun install(connectorType: String): ConnectorInstallation {
        val connector = registry.require(connectorType) // 404 if unknown type
        if (!connector.multipleInstancesAllowed) {
            repository.findByConnectorTypeAndActiveTrue(connectorType)?.let {
                throw ConnectorErrors.alreadyInstalled(connectorType) // FR-005
            }
        }
        val installation = ConnectorInstallation().apply {
            this.connectorType = connectorType
            this.status = InstallationStatus.NEEDS_CONFIG
            this.autoStart = true
            this.active = true
        }
        val saved = repository.save(installation)
        entityChangePublisher.created(Constants.SYNC_ENTITY_CODE, saved.uid)
        return saved
    }

    @Transactional
    fun uninstall(uid: String) {
        val installation = getInstallation(uid)
        installation.active = false
        installation.status = InstallationStatus.UNINSTALLED
        repository.save(installation)
        entityChangePublisher.deleted(Constants.SYNC_ENTITY_CODE, installation.uid)
    }
}
