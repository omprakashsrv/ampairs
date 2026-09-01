package com.ampairs.connector.service

import com.ampairs.connector.domain.dto.SyncCheckpointDto
import com.ampairs.connector.domain.dto.SyncRunDto
import com.ampairs.connector.domain.dto.toDto
import com.ampairs.connector.domain.model.ConnectorSyncCheckpoint
import com.ampairs.connector.domain.model.ConnectorSyncRun
import com.ampairs.connector.repository.ConnectorSyncCheckpointRepository
import com.ampairs.connector.repository.ConnectorSyncRunRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Checkpoints (incremental watermarks) + run history, reported by the client and read back (FR-017/020). */
@Service
class ConnectorSyncStateService(
    private val checkpointRepository: ConnectorSyncCheckpointRepository,
    private val runRepository: ConnectorSyncRunRepository,
    private val installationService: ConnectorInstallationService,
) {
    fun listCheckpoints(installationUid: String): List<SyncCheckpointDto> {
        installationService.getInstallation(installationUid)
        return checkpointRepository.findByInstallationUid(installationUid).map { it.toDto() }
    }

    @Transactional
    fun upsertCheckpoint(installationUid: String, dto: SyncCheckpointDto): SyncCheckpointDto {
        installationService.getInstallation(installationUid)
        val direction = dto.direction.ifBlank { "INBOUND" }
        val checkpoint = checkpointRepository
            .findByInstallationUidAndEntityTypeAndDirection(installationUid, dto.entityType, direction)
            ?: ConnectorSyncCheckpoint().apply {
                this.installationUid = installationUid
                this.entityType = dto.entityType
                this.direction = direction
            }
        checkpoint.watermark = dto.watermark
        checkpoint.lastSyncedAt = dto.lastSyncedAt ?: Instant.now()
        return checkpointRepository.save(checkpoint).toDto()
    }

    fun listRuns(installationUid: String, pageable: Pageable): Page<SyncRunDto> {
        installationService.getInstallation(installationUid)
        return runRepository.findByInstallationUidOrderByStartedAtDesc(installationUid, pageable).map { it.toDto() }
    }

    @Transactional
    fun recordRun(installationUid: String, dto: SyncRunDto): SyncRunDto {
        installationService.getInstallation(installationUid)
        val run = ConnectorSyncRun().apply {
            this.installationUid = installationUid
            this.entityType = dto.entityType
            this.trigger = dto.trigger.ifBlank { "MANUAL" }
            this.startedAt = dto.startedAt ?: Instant.now()
            this.finishedAt = dto.finishedAt
            this.status = dto.status.ifBlank { "SUCCESS" }
            this.processed = dto.processed
            this.created = dto.created
            this.updated = dto.updated
            this.failed = dto.failed
            this.errorDetail = dto.errorDetail
        }
        return runRepository.save(run).toDto()
    }
}
