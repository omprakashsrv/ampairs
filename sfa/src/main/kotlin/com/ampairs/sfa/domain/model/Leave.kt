package com.ampairs.sfa.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sfa.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * A manager-marked excused absence for a rep on a given day. Excused days are not counted "absent"
 * in attendance summaries and their planned visits are excused (not "missed") in adherence.
 */
@Entity
@Table(
    name = "leaves",
    indexes = [
        Index(name = "idx_leave_owner", columnList = "owner_id"),
        Index(name = "idx_leave_rep", columnList = "rep_member_uid"),
        Index(name = "idx_leave_date", columnList = "leave_date"),
        Index(name = "idx_leave_updated_at", columnList = "updated_at"),
    ],
)
class Leave : OwnableBaseDomain() {

    @Column(name = "rep_member_uid", nullable = false, length = 40)
    var repMemberUid: String = ""

    @Column(name = "leave_date", nullable = false)
    var leaveDate: Instant = Instant.EPOCH

    @Column(name = "reason", length = 500)
    var reason: String? = null

    /** Workspace member uid of the manager who marked the leave. */
    @Column(name = "marked_by", length = 40)
    var markedBy: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.LEAVE_PREFIX
}
