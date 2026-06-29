package com.ampairs.sfa.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sfa.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * A named route ("beat") of retail outlets that a field rep walks on scheduled day(s).
 * Outlets are members (see [BeatOutlet]); a beat may be assigned to a rep.
 */
@Entity
@Table(
    name = "beats",
    indexes = [
        Index(name = "idx_beat_owner", columnList = "owner_id"),
        Index(name = "idx_beat_rep", columnList = "rep_member_uid"),
        Index(name = "idx_beat_updated_at", columnList = "updated_at"),
    ],
)
class Beat : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 150)
    var name: String = ""

    @Column(name = "description", length = 500)
    var description: String? = null

    /** Workspace member uid of the rep assigned to this beat (null = unassigned). */
    @Column(name = "rep_member_uid", length = 40)
    var repMemberUid: String? = null

    /** Comma-separated scheduled weekdays, e.g. "MON,WED,FRI". */
    @Column(name = "scheduled_days", length = 100)
    var scheduledDays: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.BEAT_PREFIX
}
