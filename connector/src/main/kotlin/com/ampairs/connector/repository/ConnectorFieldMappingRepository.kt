package com.ampairs.connector.repository

import com.ampairs.connector.domain.model.ConnectorFieldMapping
import org.springframework.data.repository.CrudRepository

interface ConnectorFieldMappingRepository : CrudRepository<ConnectorFieldMapping, Long> {
    fun findByInstallationUidAndActiveTrue(installationUid: String): List<ConnectorFieldMapping>
    fun findByInstallationUidAndEntityType(installationUid: String, entityType: String): ConnectorFieldMapping?
}
