package com.ampairs.sfa.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sfa.config.Constants
import com.ampairs.sfa.domain.enums.AttendanceStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * A rep's daily attendance — check-in and check-out events, each capturing location and time.
 */
@Entity
@Table(
    name = "attendance",
    indexes = [
        Index(name = "idx_attendance_owner", columnList = "owner_id"),
        Index(name = "idx_attendance_rep", columnList = "rep_member_uid"),
        Index(name = "idx_attendance_check_in", columnList = "check_in_at"),
        Index(name = "idx_attendance_updated_at", columnList = "updated_at"),
    ],
)
class Attendance : OwnableBaseDomain() {

    @Column(name = "rep_member_uid", nullable = false, length = 40)
    var repMemberUid: String = ""

    @Column(name = "check_in_at")
    var checkInAt: Instant? = null

    @Column(name = "check_in_latitude")
    var checkInLatitude: Double? = null

    @Column(name = "check_in_longitude")
    var checkInLongitude: Double? = null

    @Column(name = "check_out_at")
    var checkOutAt: Instant? = null

    @Column(name = "check_out_latitude")
    var checkOutLatitude: Double? = null

    @Column(name = "check_out_longitude")
    var checkOutLongitude: Double? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: AttendanceStatus = AttendanceStatus.OPEN

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.ATTENDANCE_PREFIX
}
