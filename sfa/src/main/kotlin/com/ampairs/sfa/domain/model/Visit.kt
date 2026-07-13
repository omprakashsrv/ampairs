package com.ampairs.sfa.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sfa.config.Constants
import com.ampairs.sfa.domain.enums.GeoFenceStatus
import com.ampairs.sfa.domain.enums.VisitOutcome
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * An actual rep stop at an outlet — captured offline with location, time, outcome, optional order,
 * and an informational geo-fence flag. A planned stop references [PlannedVisit]; an ad-hoc stop does not.
 */
@Entity
@Table(
    name = "visits",
    indexes = [
        Index(name = "idx_visit_owner", columnList = "owner_id"),
        Index(name = "idx_visit_rep", columnList = "rep_member_uid"),
        Index(name = "idx_visit_customer", columnList = "customer_uid"),
        Index(name = "idx_visit_visited_at", columnList = "visited_at"),
        Index(name = "idx_visit_updated_at", columnList = "updated_at"),
    ],
)
class Visit : OwnableBaseDomain() {

    @Column(name = "customer_uid", nullable = false, length = 40)
    var customerUid: String = ""

    @Column(name = "rep_member_uid", nullable = false, length = 40)
    var repMemberUid: String = ""

    /** Set when this visit fulfils a planned stop; null for ad-hoc visits. */
    @Column(name = "planned_visit_uid", length = 40)
    var plannedVisitUid: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    var outcome: VisitOutcome = VisitOutcome.NO_ORDER

    @Column(name = "latitude")
    var latitude: Double? = null

    @Column(name = "longitude")
    var longitude: Double? = null

    /** Distance (metres) from the captured location to the outlet, when computable. */
    @Column(name = "distance_meters")
    var distanceMeters: Double? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "geo_fence_status", nullable = false, length = 20)
    var geoFenceStatus: GeoFenceStatus = GeoFenceStatus.NO_LOCATION

    @Column(name = "ad_hoc", nullable = false)
    var adHoc: Boolean = false

    @Column(name = "notes", length = 1000)
    var notes: String? = null

    /** The order taken at the counter (an `order` module uid), if any. */
    @Column(name = "order_uid", length = 40)
    var orderUid: String? = null

    @Column(name = "visited_at", nullable = false)
    var visitedAt: Instant = Instant.EPOCH

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.VISIT_PREFIX
}
