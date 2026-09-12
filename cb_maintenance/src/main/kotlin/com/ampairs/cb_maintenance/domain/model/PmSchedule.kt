package com.ampairs.cb_maintenance.domain.model

import com.ampairs.cb_maintenance.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * A preventive-maintenance task attached to an asset category. Applies to EVERY store automatically
 * — no per-store row (module plan §3). Changing a cadence chain-wide is one row edit.
 */
@Entity(name = "cb_pm_schedule")
@NamedEntityGraph(name = "CbPmSchedule.basic")
@Table(
    name = "pm_schedule",
    indexes = [
        Index(name = "idx_cb_pm_schedule_uid", columnList = "uid", unique = true),
        Index(name = "idx_cb_pm_schedule_owner", columnList = "owner_id"),
        Index(name = "idx_cb_pm_schedule_category", columnList = "asset_category"),
        Index(name = "idx_cb_pm_schedule_bucket", columnList = "ticket_bucket_id"),
    ]
)
class PmSchedule : OwnableBaseDomain() {

    /**
     * Taxonomy department this PM belongs to (kept denormalized alongside [assetCategory] for
     * grouping/reporting). The exact taxonomy leaf is [ticketBucketId].
     */
    @Column(name = "department", length = 100, nullable = false)
    var department: String = ""

    @Column(name = "asset_category", length = 100, nullable = false)
    var assetCategory: String = ""

    /**
     * The exact ticket-bucket taxonomy leaf this PM maps to (Department › Category › Issue
     * [› Issue-detail]) — the uid of a `ticket_bucket` row, same granularity a ticket carries.
     * Nullable for schedules created before the leaf link existed / when the catalog isn't seeded.
     * Lets reports join PM ↔ ticket on the identical classification.
     */
    @Column(name = "ticket_bucket_id", length = 200)
    var ticketBucketId: String? = null

    @Column(name = "task_name", length = 200, nullable = false)
    var taskName: String = ""

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist", columnDefinition = "jsonb")
    var checklist: List<String>? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_unit", length = 20, nullable = false)
    var frequencyUnit: FrequencyUnit = FrequencyUnit.MONTH

    /** MONTH + interval=3 == quarterly. */
    @Column(name = "frequency_interval", nullable = false)
    var frequencyInterval: Int = 1

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.PM_SCHEDULE_PREFIX
    }
}
