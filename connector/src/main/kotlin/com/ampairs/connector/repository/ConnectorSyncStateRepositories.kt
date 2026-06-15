package com.ampairs.connector.repository

import com.ampairs.connector.domain.model.ConnectorSyncCheckpoint
import com.ampairs.connector.domain.model.ConnectorSyncRun
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface ConnectorSyncCheckpointRepository : CrudRepository<ConnectorSyncCheckpoint, Long> {
    fun findByInstallationUidAndEntityTypeAndDirection(
        installationUid: String, entityType: String, direction: String,
    ): ConnectorSyncCheckpoint?

    fun findByInstallationUid(installationUid: String): List<ConnectorSyncCheckpoint>
}

interface ConnectorSyncRunRepository :
    CrudRepository<ConnectorSyncRun, Long>, PagingAndSortingRepository<ConnectorSyncRun, Long> {
    fun findByInstallationUidOrderByStartedAtDesc(installationUid: String, pageable: Pageable): Page<ConnectorSyncRun>
}
