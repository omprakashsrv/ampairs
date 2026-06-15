package com.ampairs.connector.repository

import com.ampairs.connector.domain.model.ConnectorConfig
import org.springframework.data.repository.CrudRepository

interface ConnectorConfigRepository : CrudRepository<ConnectorConfig, Long> {
    fun findByInstallationUid(installationUid: String): ConnectorConfig?
    fun findByUid(uid: String?): ConnectorConfig?
}
