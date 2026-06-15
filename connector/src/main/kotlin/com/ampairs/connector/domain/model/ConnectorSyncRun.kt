package com.ampairs.connector.domain.model

import com.ampairs.connector.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import java.time.Instant

/** Auditable record of one sync execution (FR-020). */
@Entity(name = "connector_sync_run")
@NamedEntityGraph(name = "ConnectorSyncRun.basic")
@Table(
    indexes = [
        Index(name = "idx_connector_run_uid", columnList = "uid", unique = true),
        Index(name = "idx_connector_run_owner", columnList = "owner_id"),
        Index(name = "idx_connector_run_installation", columnList = "installation_uid"),
        Index(name = "idx_connector_run_updated", columnList = "updated_at"),
    ]
)
class ConnectorSyncRun : OwnableBaseDomain() {

    @Column(name = "installation_uid", length = 200, nullable = false)
    var installationUid: String = ""

    @Column(name = "entity_type", length = 64)
    var entityType: String? = null

    @Column(name = "trigger_type", length = 16, nullable = false)
    var trigger: String = "MANUAL"

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now()

    @Column(name = "finished_at")
    var finishedAt: Instant? = null

    @Column(name = "status", length = 16, nullable = false)
    var status: String = "RUNNING"

    @Column(name = "processed", nullable = false)
    var processed: Int = 0

    @Column(name = "created", nullable = false)
    var created: Int = 0

    @Column(name = "updated", nullable = false)
    var updated: Int = 0

    @Column(name = "failed", nullable = false)
    var failed: Int = 0

    @Column(name = "error_detail", columnDefinition = "TEXT")
    var errorDetail: String? = null

    override fun obtainSeqIdPrefix(): String = Constants.CONNECTOR_RUN_PREFIX
}
