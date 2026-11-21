package com.ampairs.connector.service

import com.ampairs.connector.config.Constants
import com.ampairs.connector.config.ConnectorJson
import com.ampairs.connector.domain.catalogue.CatalogueConnector
import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.domain.dto.FieldMappingRuleDto
import com.ampairs.connector.domain.model.ConnectorFieldMapping
import com.ampairs.connector.domain.model.ConnectorInstallation
import com.ampairs.connector.domain.model.InstallationStatus
import com.ampairs.connector.exception.ConnectorErrors
import com.ampairs.connector.repository.ConnectorConfigRepository
import com.ampairs.connector.repository.ConnectorFieldMappingRepository
import com.ampairs.connector.repository.ConnectorInstallationRepository
import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.sync.EntityChangePublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Install / uninstall / lifecycle of per-workspace connector installations. Tenant context is set by
 * `SessionUserFilter` from `X-Workspace-ID`; `@TenantId` scopes all reads/writes automatically.
 */
@Service
class ConnectorInstallationService(
    private val repository: ConnectorInstallationRepository,
    private val registry: ConnectorCatalogueRegistry,
    private val configRepository: ConnectorConfigRepository,
    private val mappingRepository: ConnectorFieldMappingRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {
    fun listInstallations(): List<ConnectorInstallation> = repository.findByActiveTrue()

    /** Incremental pull for client mirroring — includes inactive (uninstalled) rows so deletes propagate. */
    fun syncInstallations(lastSync: String?, pageable: Pageable): Page<ConnectorInstallation> {
        val since = lastSync?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.EPOCH
        return repository.findByUpdatedAtAfter(since, pageable)
    }

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
        seedDefaultMappings(saved.uid, connector)
        entityChangePublisher.created(Constants.SYNC_ENTITY_CODE, saved.uid)
        return saved
    }

    /**
     * Seed the connector's default mapping template (FR-011) as editable per-entity mapping rows so
     * the workspace starts with a working mapping (e.g. the current Tally field mappings) instead of
     * a blank slate. The admin can then customise them.
     */
    private fun seedDefaultMappings(installationUid: String, connector: CatalogueConnector) {
        connector.defaultMapping.forEach { entityMapping ->
            if (mappingRepository.findByInstallationUidAndEntityType(installationUid, entityMapping.entityType) != null) {
                return@forEach
            }
            val rules = entityMapping.rules.map {
                FieldMappingRuleDto(
                    externalField = it.externalField,
                    ampairsField = it.ampairsField,
                    unmapped = false,
                    transform = it.transform,
                )
            }
            mappingRepository.save(
                ConnectorFieldMapping().apply {
                    this.installationUid = installationUid
                    this.entityType = entityMapping.entityType
                    this.rules = ConnectorJson.mapper.writeValueAsString(rules)
                    this.version = 1
                }
            )
        }
    }

    /** Promote NEEDS_CONFIG/ERROR → ENABLED once a config and at least one mapping exist (FR-006). */
    @Transactional
    fun markEnabledIfReady(installationUid: String) {
        val installation = repository.findByUid(installationUid)?.takeIf { it.active } ?: return
        if (installation.status != InstallationStatus.NEEDS_CONFIG && installation.status != InstallationStatus.ERROR) return
        val hasConfig = configRepository.findByInstallationUid(installationUid) != null
        val hasMapping = mappingRepository.findByInstallationUidAndActiveTrue(installationUid).isNotEmpty()
        if (hasConfig && hasMapping) {
            installation.status = InstallationStatus.ENABLED
            installation.lastErrorMessage = null
            repository.save(installation)
            entityChangePublisher.updated(Constants.SYNC_ENTITY_CODE, installation.uid)
        }
    }

    @Transactional
    fun pause(uid: String): ConnectorInstallation {
        val installation = getInstallation(uid)
        if (installation.status == InstallationStatus.ENABLED) {
            installation.status = InstallationStatus.PAUSED
            repository.save(installation)
            entityChangePublisher.updated(Constants.SYNC_ENTITY_CODE, installation.uid)
        }
        return installation
    }

    @Transactional
    fun resume(uid: String): ConnectorInstallation {
        val installation = getInstallation(uid)
        if (installation.status == InstallationStatus.PAUSED) {
            installation.status = InstallationStatus.ENABLED
            repository.save(installation)
            entityChangePublisher.updated(Constants.SYNC_ENTITY_CODE, installation.uid)
        }
        return installation
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
