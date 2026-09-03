package com.ampairs.cb_store.domain.model

import com.ampairs.cb_store.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * An outlet. Kept separate from `product.Warehouse` — a store here needs zone
 * routing, not inventory semantics (module plan §2).
 */
@Entity(name = "cb_store")
@NamedEntityGraph(name = "CbStore.basic")
@Table(
    name = "store",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cb_store_owner_code", columnNames = ["owner_id", "code"]),
    ],
    indexes = [
        Index(name = "idx_cb_store_uid", columnList = "uid", unique = true),
        Index(name = "idx_cb_store_owner", columnList = "owner_id"),
        Index(name = "idx_cb_store_zone", columnList = "zonal_office_id"),
    ]
)
class Store : OwnableBaseDomain() {

    /** 3-letter outlet code, e.g. "ARK". */
    @Column(name = "code", length = 20, nullable = false)
    var code: String = ""

    @Column(name = "name", length = 200, nullable = false)
    var name: String = ""

    @Column(name = "city", length = 100, nullable = false)
    var city: String = ""

    /** FK -> ZonalOffice.uid (same module). */
    @Column(name = "zonal_office_id", length = 200, nullable = false)
    var zonalOfficeId: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.STORE_PREFIX
    }
}
