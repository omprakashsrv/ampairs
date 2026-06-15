package com.ampairs.connector.domain.dto

import com.ampairs.connector.domain.model.ConnectorSyncCheckpoint
import com.ampairs.connector.domain.model.ConnectorSyncRun
import java.time.Instant

data class SyncCheckpointDto(
    val installationUid: String = "",
    val entityType: String = "",
    val direction: String = "INBOUND",
    val watermark: String? = null,
    val lastSyncedAt: Instant? = null,
)

fun ConnectorSyncCheckpoint.toDto(): SyncCheckpointDto = SyncCheckpointDto(
    installationUid = installationUid,
    entityType = entityType,
    direction = direction,
    watermark = watermark,
    lastSyncedAt = lastSyncedAt,
)

data class SyncRunDto(
    val uid: String? = null,
    val installationUid: String = "",
    val entityType: String? = null,
    val trigger: String = "MANUAL",
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val status: String = "RUNNING",
    val processed: Int = 0,
    val created: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    val errorDetail: String? = null,
)

fun ConnectorSyncRun.toDto(): SyncRunDto = SyncRunDto(
    uid = uid,
    installationUid = installationUid,
    entityType = entityType,
    trigger = trigger,
    startedAt = startedAt,
    finishedAt = finishedAt,
    status = status,
    processed = processed,
    created = created,
    updated = updated,
    failed = failed,
    errorDetail = errorDetail,
)
