package com.ampairs.connector.domain.model

import com.ampairs.connector.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import java.time.Instant

/** Per-installation, per-entity, per-direction incremental watermark (FR-017). */
@Entity(name = "connector_sync_checkpoint")
@NamedEntityGraph(name = "ConnectorSyncCheckpoint.basic")
@Table(
    indexes = [
        Index(name = "idx_connector_checkpoint_uid", columnList = "uid", unique = true),
        Index(name = "idx_connector_checkpoint_owner", columnList = "owner_id"),
        Index(name = "idx_connector_checkpoint_key", columnList = "installation_uid, entity_type, direction", unique = true),
        Index(name = "idx_connector_checkpoint_updated", columnList = "updated_at"),
    ]
)
class ConnectorSyncCheckpoint : OwnableBaseDomain() {

    @Column(name = "installation_uid", length = 200, nullable = false)
    var installationUid: String = ""

    @Column(name = "entity_type", length = 64, nullable = false)
    var entityType: String = ""

    @Column(name = "direction", length = 16, nullable = false)
    var direction: String = "INBOUND"

    /** ISO-8601 timestamp or external cursor (e.g. Tally alterId). */
    @Column(name = "watermark", length = 255)
    var watermark: String? = null

    @Column(name = "last_synced_at")
    var lastSyncedAt: Instant? = null

    override fun obtainSeqIdPrefix(): String = Constants.CONNECTOR_CHECKPOINT_PREFIX
}
