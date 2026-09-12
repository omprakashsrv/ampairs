package com.ampairs.cb_store.domain.model

import com.ampairs.cb_store.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table

/**
 * City-level maintenance office grouping a set of outlets. No `leadEmployeeId` field — the
 * escalation target is derived in `cb_employee` (walk the `reportsTo` chain), never stored
 * redundantly here, so it can't drift or be null for thin zones (module plan §2).
 */
@Entity(name = "cb_zonal_office")
@NamedEntityGraph(name = "CbZonalOffice.basic")
@Table(
    name = "zonal_office",
    indexes = [
        Index(name = "idx_cb_zonal_office_uid", columnList = "uid", unique = true),
        Index(name = "idx_cb_zonal_office_owner", columnList = "owner_id"),
    ]
)
class ZonalOffice : OwnableBaseDomain() {

    @Column(name = "name", length = 150, nullable = false)
    var name: String = ""

    @Column(name = "city", length = 100, nullable = false)
    var city: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.ZONAL_OFFICE_PREFIX
    }
}
