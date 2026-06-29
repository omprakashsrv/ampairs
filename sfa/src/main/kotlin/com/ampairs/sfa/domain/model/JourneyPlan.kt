package com.ampairs.sfa.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sfa.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * A rep's recurring weekly assignment of a [Beat] (the Permanent Journey Plan / PJP).
 * Drives the rep's planned outlets for a given day.
 */
@Entity
@Table(
    name = "journey_plans",
    indexes = [
        Index(name = "idx_journey_plan_owner", columnList = "owner_id"),
        Index(name = "idx_journey_plan_rep", columnList = "rep_member_uid"),
        Index(name = "idx_journey_plan_updated_at", columnList = "updated_at"),
    ],
)
class JourneyPlan : OwnableBaseDomain() {

    @Column(name = "rep_member_uid", nullable = false, length = 40)
    var repMemberUid: String = ""

    @Column(name = "beat_uid", nullable = false, length = 40)
    var beatUid: String = ""

    /** Recurring weekday this plan applies to, e.g. "MON". */
    @Column(name = "weekday", length = 10)
    var weekday: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.JOURNEY_PLAN_PREFIX
}
