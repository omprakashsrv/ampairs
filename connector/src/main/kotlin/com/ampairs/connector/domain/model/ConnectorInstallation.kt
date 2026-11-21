package com.ampairs.connector.domain.model

import com.ampairs.connector.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table

/** Lifecycle state of a per-workspace connector installation. */
enum class InstallationStatus { NEEDS_CONFIG, ENABLED, PAUSED, ERROR, UNINSTALLED }

/**
 * A connector enabled for a specific workspace (tenant-scoped via [OwnableBaseDomain.ownerId]).
 * Soft-deleted via [active] = false on uninstall so offline clients reconcile the removal.
 */
@Entity(name = "connector_installation")
@NamedEntityGraph(name = "ConnectorInstallation.basic")
@Table(
    indexes = [
        Index(name = "idx_connector_installation_uid", columnList = "uid", unique = true),
        Index(name = "idx_connector_installation_owner", columnList = "owner_id"),
        Index(name = "idx_connector_installation_owner_type", columnList = "owner_id, connector_type"),
        Index(name = "idx_connector_installation_updated", columnList = "updated_at"),
    ]
)
class ConnectorInstallation : OwnableBaseDomain() {

    @Column(name = "connector_type", length = 64, nullable = false)
    var connectorType: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    var status: InstallationStatus = InstallationStatus.NEEDS_CONFIG

    @Column(name = "auto_start", nullable = false)
    var autoStart: Boolean = true

    @Column(name = "schedule_seconds")
    var scheduleSeconds: Int? = null

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    var lastErrorMessage: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.CONNECTOR_INSTALLATION_PREFIX
}
