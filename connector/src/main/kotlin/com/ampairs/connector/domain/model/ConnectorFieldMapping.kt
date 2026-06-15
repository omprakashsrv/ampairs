package com.ampairs.connector.domain.model

import com.ampairs.connector.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table

/**
 * Per-installation, per-entity field mapping. [rules] is a JSON array of mapping rules
 * (externalField → ampairsField, unmapped flag, optional transform). One row per
 * (installation, entityType). The set of mapped `ampairsField`s is the sparse-upsert allowlist.
 */
@Entity(name = "connector_field_mapping")
@NamedEntityGraph(name = "ConnectorFieldMapping.basic")
@Table(
    indexes = [
        Index(name = "idx_connector_mapping_uid", columnList = "uid", unique = true),
        Index(name = "idx_connector_mapping_owner", columnList = "owner_id"),
        Index(name = "idx_connector_mapping_install_entity", columnList = "installation_uid, entity_type", unique = true),
        Index(name = "idx_connector_mapping_updated", columnList = "updated_at"),
    ]
)
class ConnectorFieldMapping : OwnableBaseDomain() {

    @Column(name = "installation_uid", length = 200, nullable = false)
    var installationUid: String = ""

    @Column(name = "entity_type", length = 64, nullable = false)
    var entityType: String = ""

    /** JSON array: [{"externalField":"name","ampairsField":"name","unmapped":false,"transform":null}, ...] */
    @Column(name = "rules", columnDefinition = "TEXT")
    var rules: String? = null

    @Column(name = "version", nullable = false)
    var version: Int = 1

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.CONNECTOR_MAPPING_PREFIX
}
