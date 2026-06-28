package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * A recurring send definition. `nextRunAt` is a UTC instant computed from the wall-clock
 * (`timeOfDay` + day selectors) in the workspace business `timezone`. The sweeper fires the schedule
 * when `nextRunAt <= now`, materializes one request, and advances `nextRunAt`.
 */
@Entity(name = "communication_schedule")
@Table(
    indexes = [
        Index(name = "idx_schedule_owner", columnList = "owner_id"),
        Index(name = "idx_schedule_due", columnList = "paused,next_run_at"),
    ]
)
class CommunicationSchedule : OwnableBaseDomain() {

    @Column(name = "name", length = 200, nullable = false)
    var name: String = ""

    @Column(name = "template_uid", length = 200, nullable = false)
    var templateUid: String = ""

    @Column(name = "channels", length = 120, nullable = false)
    var channels: String = ""

    @Column(name = "audience_type", length = 20, nullable = false)
    var audienceType: String = "SEGMENT"

    @Column(name = "audience_ref", length = 200)
    var audienceRef: String? = null

    @Column(name = "variables_json", columnDefinition = "TEXT")
    var variablesJson: String? = null

    @Column(name = "frequency", length = 20, nullable = false)
    var frequency: String = "MONTHLY"

    /** Every N periods. Column is `interval_count` (`interval` is a SQL reserved word). */
    @Column(name = "interval_count", nullable = false)
    var interval: Int = 1

    @Column(name = "day_of_week")
    var dayOfWeek: Int? = null

    @Column(name = "day_of_month")
    var dayOfMonth: Int? = null

    @Column(name = "time_of_day", length = 5, nullable = false)
    var timeOfDay: String = "09:00"

    /** IANA business timezone the wall-clock is evaluated in (e.g. Asia/Kolkata). */
    @Column(name = "timezone", length = 64, nullable = false)
    var timezone: String = "UTC"

    @Column(name = "start_date", length = 10)
    var startDate: String? = null

    @Column(name = "end_date", length = 10)
    var endDate: String? = null

    @Column(name = "paused", nullable = false)
    var paused: Boolean = false

    @Column(name = "next_run_at")
    var nextRunAt: Instant? = null

    @Column(name = "last_run_at")
    var lastRunAt: Instant? = null

    @Column(name = "last_occurrence_key", length = 64)
    var lastOccurrenceKey: String? = null

    @Column(name = "claim_version", nullable = false)
    var claimVersion: Long = 0

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.SCHEDULE_PREFIX
}
