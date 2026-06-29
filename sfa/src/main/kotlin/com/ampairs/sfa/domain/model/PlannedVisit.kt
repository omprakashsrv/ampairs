package com.ampairs.sfa.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sfa.config.Constants
import com.ampairs.sfa.domain.enums.PlannedVisitStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * An expected stop for a given day, derived from a [JourneyPlan]. Reconciled to VISITED/MISSED
 * by authored [Visit]s — the basis for beat adherence (FR-017).
 */
@Entity
@Table(
    name = "planned_visits",
    indexes = [
        Index(name = "idx_planned_visit_owner", columnList = "owner_id"),
        Index(name = "idx_planned_visit_rep", columnList = "rep_member_uid"),
        Index(name = "idx_planned_visit_date", columnList = "planned_date"),
        Index(name = "idx_planned_visit_updated_at", columnList = "updated_at"),
    ],
)
class PlannedVisit : OwnableBaseDomain() {

    @Column(name = "journey_plan_uid", length = 40)
    var journeyPlanUid: String? = null

    @Column(name = "beat_uid", length = 40)
    var beatUid: String? = null

    @Column(name = "customer_uid", nullable = false, length = 40)
    var customerUid: String = ""

    @Column(name = "rep_member_uid", nullable = false, length = 40)
    var repMemberUid: String = ""

    @Column(name = "planned_date", nullable = false)
    var plannedDate: Instant = Instant.EPOCH

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PlannedVisitStatus = PlannedVisitStatus.PENDING

    @Column(name = "visit_sequence", nullable = false)
    var visitSequence: Int = 0

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.PLANNED_VISIT_PREFIX
}
