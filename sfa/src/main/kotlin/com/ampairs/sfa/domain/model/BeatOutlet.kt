package com.ampairs.sfa.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sfa.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Membership of a retail outlet (a `customer` record) in a [Beat], with its visit sequence
 * and optional scheduled day. Referenced by `customerUid` — no cross-module JPA relationship.
 */
@Entity
@Table(
    name = "beat_outlets",
    indexes = [
        Index(name = "idx_beat_outlet_owner", columnList = "owner_id"),
        Index(name = "idx_beat_outlet_beat", columnList = "beat_uid"),
        Index(name = "idx_beat_outlet_customer", columnList = "customer_uid"),
        Index(name = "idx_beat_outlet_updated_at", columnList = "updated_at"),
    ],
)
class BeatOutlet : OwnableBaseDomain() {

    @Column(name = "beat_uid", nullable = false, length = 40)
    var beatUid: String = ""

    /** The outlet — a `customer` uid in the distributor's own catalog. */
    @Column(name = "customer_uid", nullable = false, length = 40)
    var customerUid: String = ""

    @Column(name = "visit_sequence", nullable = false)
    var visitSequence: Int = 0

    @Column(name = "visit_day", length = 10)
    var visitDay: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.BEAT_OUTLET_PREFIX
}
