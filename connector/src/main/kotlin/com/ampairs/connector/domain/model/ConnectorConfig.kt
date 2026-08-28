package com.ampairs.connector.domain.model

import com.ampairs.connector.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import java.time.Instant

/**
 * Per-installation connection configuration. Non-secret values (host, port) are stored as JSON;
 * secrets are stored encrypted (AES-GCM via `ConnectorSecretCipher`) and never returned to clients.
 * One row per installation.
 */
@Entity(name = "connector_config")
@NamedEntityGraph(name = "ConnectorConfig.basic")
@Table(
    indexes = [
        Index(name = "idx_connector_config_uid", columnList = "uid", unique = true),
        Index(name = "idx_connector_config_owner", columnList = "owner_id"),
        Index(name = "idx_connector_config_installation", columnList = "installation_uid", unique = true),
        Index(name = "idx_connector_config_updated", columnList = "updated_at"),
    ]
)
class ConnectorConfig : OwnableBaseDomain() {

    @Column(name = "installation_uid", length = 200, nullable = false)
    var installationUid: String = ""

    /** JSON object of non-secret connection values, e.g. {"host":"localhost","port":"9000"}. */
    @Column(name = "non_secret_values", columnDefinition = "TEXT")
    var nonSecretValues: String? = null

    /** Encrypted JSON object of secret values; never serialized to clients. */
    @Column(name = "secret_values_encrypted", columnDefinition = "TEXT")
    var secretValuesEncrypted: String? = null

    @Column(name = "last_validated_at")
    var lastValidatedAt: Instant? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.CONNECTOR_CONFIG_PREFIX
}
